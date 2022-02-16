/*
 * Copyright (c)  Hoddmimes Solution AB 2022.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hoddmimes.te.cryptogwy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.TXIDFactory;
import com.hoddmimes.te.common.TeException;
import com.hoddmimes.te.common.db.TEDB;
import com.hoddmimes.te.common.interfaces.TeIpcServices;
import com.hoddmimes.te.common.ipc.IpcRequestCallbackInterface;

import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.InsufficientMoneyException;

import java.util.List;
import java.util.UUID;


public class CryptoGateway implements IpcRequestCallbackInterface {
	private static Logger cLog = LogManager.getLogger( CryptoGateway.class );
	private JsonObject mCryptoGwyConfig;
	private JsonObject mTeConfiguration;
	private BitcoinGwy mBitcoinGwy;
	private TEDB mDb;
	private ConfirmationCleanerThread mCleanerThread;



	public CryptoGateway( JsonObject pTeConfiguration ) {
		mTeConfiguration = pTeConfiguration;
		mCryptoGwyConfig = AuxJson.navigateObject( mTeConfiguration,"TeConfiguration/cryptoGateway");
		mDb = TeAppCntx.getInstance().getDb();
		TeAppCntx.getInstance().getIpcService().registerComponent( TeIpcServices.CryptoGwy, 0, this );
		TeAppCntx.getInstance().setCryptoGateway( this );

		mBitcoinGwy = new BitcoinGwy( mCryptoGwyConfig, mDb);


	}

	public BitcoinGwy getBitcoinGwy() {
		return mBitcoinGwy;
	}

	public BitcoinGwy getEthereumGwy() {
		return null;
	}

	public TEDB getDb() {
		return mDb;
	}

	static String getConfirmationId() {
		return  String.valueOf(TXIDFactory.getId()) + "-" + UUID.randomUUID().toString();
	}


	public String sendCoins( CryptoReDrawRequest pCryptoReDrawRequest ) throws TeException {
		String txid = null;

		// Validate that the destination address has been defined as a destination for the user
		// and that the PaymentEntry has been confirmed
		DbCryptoPaymentEntry tPaymentEntry = (DbCryptoPaymentEntry) TEDB.dbEntryFound(mDb.findPaymentEntryByAddressAndCoinType( pCryptoReDrawRequest.getAddress().get(), pCryptoReDrawRequest.getCoin().get()));
		if (tPaymentEntry == null) {
			cLog.error("Payment entry not found redraw, account: " + pCryptoReDrawRequest.getAccountId() + " coin: " + pCryptoReDrawRequest.getCoin().get() + " address: " + pCryptoReDrawRequest.getAddress().get());
			throw new TeException( StatusMessageBuilder.error("Payment entry not found for address: " + pCryptoReDrawRequest.getAddress().get(), null));
		}
		if (!tPaymentEntry.getConfirmed().get()) {
			cLog.error("Payment entry not confirmed redraw, account: " + pCryptoReDrawRequest.getAccountId() + " coin: " + pCryptoReDrawRequest.getCoin().get() + " address: " + pCryptoReDrawRequest.getAddress().get());
			throw new TeException(StatusMessageBuilder.error("Payment entry not confirmed for address: " + pCryptoReDrawRequest.getAddress().get(), null));
		}


		if (TEDB.CoinType.BTC.name().contentEquals( pCryptoReDrawRequest.getCoin().get())) {
			try {
				return mBitcoinGwy.sendCoins( pCryptoReDrawRequest );
			}
			catch (InsufficientMoneyException e) {
				cLog.error("InsufficientMoneyException when redrawing coins from TE wallet to address: " + pCryptoReDrawRequest.getAddress().get());
				throw new TeException(0, StatusMessageBuilder.error("Internal wallet transfer error", null), e);
			}
		}
		if (TEDB.CoinType.BTC.name().contentEquals( pCryptoReDrawRequest.getCoin().get())) {
			throw new TeException(StatusMessageBuilder.error("Redraw for Etherreum not yet available", null));
		}

		return null;
	}



	public MessageInterface setRedrawAddressEntry( SetReDrawEntryRequest pPaymentEntryRequest ) {
		if  (pPaymentEntryRequest.getCoin().get().contentEquals(TEDB.CoinType.BTC.name())) {
			return mBitcoinGwy.setPaymentEntry(pPaymentEntryRequest);
		} else {
			return StatusMessageBuilder.error("failed to get SetReDrawEntryRequest coin type is not implemented", null);
		}
	}

	public MessageInterface setDepositAddressEntry( GetDepositEntryRequest pPaymentEntryRequest ) {
		if  (pPaymentEntryRequest.getCoin().get().contentEquals(TEDB.CoinType.BTC.name())) {
			return mBitcoinGwy.getPaymentEntry(pPaymentEntryRequest);
		} else {
			return StatusMessageBuilder.error("failed to get SetReDrawEntryRequest coin type is not implemented", null);
		}
	}

	@Override
	public MessageInterface ipcRequest(MessageInterface pIpcRequest) {
		if (pIpcRequest instanceof MgmtGetWalletRequest) {
			String tCoin = ((MgmtGetWalletRequest)pIpcRequest).getCoin().get().toUpperCase();
			MgmtGetWalletResponse tRsp = new MgmtGetWalletResponse().setRef( ((MgmtGetWalletRequest)pIpcRequest).getRef().get());
			if (tCoin.contentEquals(TEDB.CoinType.BTC.name())) {
				tRsp.setWalletData(mBitcoinGwy.walletToString());
			} else {
				return StatusMessageBuilder.error("failed to get MgmtGetWalletRequest coin type is not implemented", null);
			}
			return tRsp;
		}
		if (pIpcRequest instanceof MgmtGetCryptoDepositAccountsRequest) {
			MgmtGetCryptoDepositAccountsRequest tRequest = (MgmtGetCryptoDepositAccountsRequest) pIpcRequest;
			MgmtGetCryptoDepositAccountsResponse tResponse = new MgmtGetCryptoDepositAccountsResponse().setRef(tRequest.getRef().get());
			List<DbCryptoDeposit> tDbCryptoDepositList = mDb.findAllDbCryptoDeposit();
			tResponse.addAccounts( tDbCryptoDepositList );
			return tResponse;
		}
		if (pIpcRequest instanceof MgmtGetCryptoAccountsAddressesRequest) {
			MgmtGetCryptoAccountsAddressesRequest tRequest = (MgmtGetCryptoAccountsAddressesRequest) pIpcRequest;
			MgmtGetCryptoAccountsAddressesResponse tResponse = new MgmtGetCryptoAccountsAddressesResponse().setRef(tRequest.getRef().get());
			List<DbCryptoPaymentEntry> tPaymentEntries = mDb.findPaymentEntryByAccountId( tRequest.getAccountId().get());
			tResponse.addPaymentEntries( tPaymentEntries );
			return tResponse;
		}
		if (pIpcRequest instanceof MgmtGetCryptoPaymentsRequest) {
			MgmtGetCryptoPaymentsRequest tRequest = (MgmtGetCryptoPaymentsRequest) pIpcRequest;
			MgmtGetCryptoPaymentsResponse tResponse = new MgmtGetCryptoPaymentsResponse().setRef(tRequest.getRef().get());
			List<DbCryptoPayment> tPayments = mDb.findDbCryptoPaymentByAccountId( tRequest.getAccountId().get());
			tResponse.addPayments( tPayments );
			return tResponse;
		}
		throw new RuntimeException("no management entry for : " + pIpcRequest.getMessageName());

	}

	class ConfirmationCleanerThread extends Thread
	{
		ConfirmationCleanerThread() {
		}

		public void run() {
	      setName("ConfirmationCleanerThread");
		  try { Thread.sleep( 300000L);}
		  catch( InterruptedException e) {}
		  while( true ) {
			 mDb.cleanConfirmations();
			setName("ConfirmationCleanerThread");
			try { Thread.sleep( 3600000L);}
			catch( InterruptedException e) {}
		  }
		}
	}
}

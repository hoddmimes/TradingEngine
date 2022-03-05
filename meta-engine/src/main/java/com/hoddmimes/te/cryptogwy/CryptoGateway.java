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
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.TeCoreService;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.Crypto;
import com.hoddmimes.te.common.TeException;
import com.hoddmimes.te.common.ipc.IpcService;
import com.hoddmimes.te.messages.DbCryptoDeposit;
import com.hoddmimes.te.common.db.TEDB;
import com.hoddmimes.te.common.interfaces.TeService;
import com.hoddmimes.te.common.ipc.IpcRequestCallbackInterface;

import com.hoddmimes.te.instrumentctl.SymbolX;
import com.hoddmimes.te.messages.DbCryptoHolding;
import com.hoddmimes.te.messages.SID;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.Coin;

import java.util.List;


public class CryptoGateway extends TeCoreService {
	private static Logger cLog = LogManager.getLogger( CryptoGateway.class );
	private BitcoinGwy  mBitcoinGwy = null;
	private EthereumGwy mEthereumGwy = null;
	private JsonObject mCryptoGatewayConfig;
	private TEDB mDb;

	private ConfirmationCleanerThread mCleanerThread;



	public CryptoGateway(JsonObject pTeConfiguration, IpcService pIpcService)
	{
		super( pTeConfiguration, pIpcService);
		mTeConfiguration = pTeConfiguration;
		mCryptoGatewayConfig = AuxJson.navigateObject( pTeConfiguration,"TeConfiguration/cryptoGateway");

		if (isEnabled( Crypto.CoinType.BTC)) {
			mBitcoinGwy = new BitcoinGwy(mTeConfiguration, mDb);
		}
		if (isEnabled( Crypto.CoinType.ETH)) {
			mEthereumGwy = new EthereumGwy(mTeConfiguration, mDb);
		}


		TeAppCntx.getInstance().registerService( this );
	}

	@Override
	public TeService getServiceId() {
		return TeService.CryptoGwy;
	}


	public boolean isEnabled( Crypto.CoinType pCoinType) {

		if (!AuxJson.navigateBoolean(mTeConfiguration,"TeConfiguration/cryptoGateway/enable")) {
			return false;
		}

		if (pCoinType == Crypto.CoinType.BTC) {
			return AuxJson.navigateBoolean(mTeConfiguration,"TeConfiguration/cryptoGateway/bitcoin/enable");
		}
		if (pCoinType == Crypto.CoinType.ETH) {
			return AuxJson.navigateBoolean(mTeConfiguration,"TeConfiguration/cryptoGateway/ethereum/enable");
		}

	    return false;
	}

	public long getEstimatedTxFeeNA( Crypto.CoinType pCoinType ) {
		long tEstimatedTxFee = 0L;
		try {
			tEstimatedTxFee = getCoinGateway(pCoinType).getEstimatedTxFeeNA();
		}
		catch( TeException e) {
			cLog.error("failed to get gateway for coin: " + pCoinType.name(), e);
		}
		return tEstimatedTxFee;
	}


	private CoinGatewayInterface getCoinGateway( Crypto.CoinType pCoinType) throws TeException{

		if (!isEnabled( pCoinType)) {
			throw new TeException( StatusMessageBuilder.error("Coin gateway \"" +pCoinType.name() + "\" is not enabled", null));
		}

		if (pCoinType == Crypto.CoinType.BTC) {
			return mBitcoinGwy;
		}
		if (pCoinType == Crypto.CoinType.ETH) {
			return mEthereumGwy;
		}

		throw new TeException( StatusMessageBuilder.error("No implementation found for coin \"" +pCoinType.name() + "\"", null));
	}

	public String sendCoins( CryptoRedrawRequest pCryptoReDrawRequest ) throws TeException {
		String txid = null;

		// Validate that the destination address has been defined as a destination for the user
		// and that the PaymentEntry has been confirmed
		Crypto.CoinType tCoinType = Crypto.CoinType.valueOf(pCryptoReDrawRequest.getCoin().get());
		DbCryptoPaymentEntry tPaymentEntry = (DbCryptoPaymentEntry) TEDB.dbEntryFound(mDb.findPaymentEntryByAddressAndCoinType(pCryptoReDrawRequest.getAddress().get(), tCoinType));
		if (tPaymentEntry == null) {
			cLog.error("Payment entry not found redraw, account: " + pCryptoReDrawRequest.getAccountId() + " coin: " + pCryptoReDrawRequest.getCoin().get() + " address: " + pCryptoReDrawRequest.getAddress().get());
			throw new TeException(StatusMessageBuilder.error("Redraw payment entry not found for address: " + pCryptoReDrawRequest.getAddress().get(), null));
		}
		if (!tPaymentEntry.getConfirmed().get()) {
			cLog.error("Payment entry not confirmed redraw, account: " + pCryptoReDrawRequest.getAccountId() + " coin: " + pCryptoReDrawRequest.getCoin().get() + " address: " + pCryptoReDrawRequest.getAddress().get());
			throw new TeException(StatusMessageBuilder.error("Redraw payment entry not confirmed for address: " + pCryptoReDrawRequest.getAddress().get(), null));
		}

		CoinGatewayInterface tCoinGateway = this.getCoinGateway(tCoinType);
		return tCoinGateway.sendCoins(pCryptoReDrawRequest);
	}


	/**
	 * Invoked when a client would like to add redraw payment account
	 * @param pSetRedrawEntryRequest
	 * @return
	 */

	public MessageInterface addRedrawEntry( SetRedrawEntryRequest pSetRedrawEntryRequest ) {
		try {
			CoinGatewayInterface tCoinGwy = getCoinGateway(Crypto.CoinType.valueOf(pSetRedrawEntryRequest.getCoin().get()));
			return tCoinGwy.addRedrawEntry(pSetRedrawEntryRequest);
		} catch (TeException e) {
			return e.getStatusMessage();
		}
	}

	/**
	 * Invoked when a client adds woukd like to get a new deposit address entry
	 * @param pPaymentEntryRequest, request
	 * @return GetDepositEntryRespone
	 */
	public MessageInterface addDepositEntry( GetDepositEntryRequest pPaymentEntryRequest ) {
		try {
			CoinGatewayInterface tCoinGwy = getCoinGateway(Crypto.CoinType.valueOf(pPaymentEntryRequest.getCoin().get()));
			return tCoinGwy.addDepositEntry(pPaymentEntryRequest);
		} catch (TeException e) {
			return e.getStatusMessage();
		}
	}

	@Override
	public MessageInterface ipcRequest(MessageInterface pIpcRequest) {
		if (pIpcRequest instanceof MgmtGetWalletRequest) {
			String tCoin = ((MgmtGetWalletRequest)pIpcRequest).getCoin().get().toUpperCase();
			MgmtGetWalletResponse tRsp = new MgmtGetWalletResponse().setRef( ((MgmtGetWalletRequest)pIpcRequest).getRef().get());
			if (tCoin.contentEquals(Crypto.CoinType.BTC.name())) {
				if (!isEnabled(Crypto.CoinType.BTC)) {
					tRsp.setWalletData("{status: \"BitCoin Gateway is not enabled and started\"}");
				} else {
					tRsp.setWalletData(mBitcoinGwy.walletToString());
				}
			} else if (tCoin.contentEquals(Crypto.CoinType.ETH.name())) {
				if (!isEnabled(Crypto.CoinType.ETH)) {
					tRsp.setWalletData("{status: \"Ethereum Gateway is not enabled and started\"}");
				} else {
					tRsp.setWalletData(mEthereumGwy.walletToString());
				}
			} else {
				tRsp.setWalletData("{status: \"Unknown coin (" + tCoin + ") no gateway available \"}");
			}
			return tRsp;
		}
		if (pIpcRequest instanceof MgmtGetCryptoDepositAccountsRequest) {
			MgmtGetCryptoDepositAccountsRequest tRequest = (MgmtGetCryptoDepositAccountsRequest) pIpcRequest;
			MgmtGetCryptoDepositAccountsResponse tResponse = new MgmtGetCryptoDepositAccountsResponse().setRef(tRequest.getRef().get());

			List<DbCryptoDeposit> tDbCryptoDepositList = TeAppCntx.getPositionController().getCryptoDeposits();
			for( DbCryptoDeposit cad : tDbCryptoDepositList) {
				if (cad.getHoldings().isPresent()) {
					// todo: go via CryptoDeposit
					for (DbCryptoHolding dh : cad.getHoldings().get()) {
						SymbolX tSymbol = TeAppCntx.getInstance().getInstrumentContainer().getSymbol(dh.getSid().get());
						long tAmount = tSymbol.scaleToOutsideNotation(dh.getQuantity().get());
						SID tSID = new SID(tSymbol.getId());
						MgmtGetCryptoDepositAccount ch = new MgmtGetCryptoDepositAccount().setAccountId(cad.getAccountId().get()).setCoin(tSID.getSymbol()).setAmount(friendlyString(tSymbol.getSid().get(), tAmount));
						tResponse.addAccounts(ch);
					}
				}
			}
			return tResponse;
		}
		if (pIpcRequest instanceof MgmtGetCryptoAccountsAddressesRequest) {
			List<DbCryptoPaymentEntry> tPaymentEntries;
			MgmtGetCryptoAccountsAddressesRequest tRequest = (MgmtGetCryptoAccountsAddressesRequest) pIpcRequest;
			MgmtGetCryptoAccountsAddressesResponse tResponse = new MgmtGetCryptoAccountsAddressesResponse().setRef(tRequest.getRef().get());
			if (tRequest.getAccountId().isPresent()) {
				tPaymentEntries = mDb.findPaymentEntryByAccountId(tRequest.getAccountId().get());
			} else {
				tPaymentEntries = mDb.findAllDbCryptoPaymentEntry();
			}
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

	private String friendlyString( String pSid, long pAmount ) {
		SID tSID = new SID( pSid );
		if ( tSID.getSymbol().contentEquals(Crypto.CoinType.BTC.name())) {
			Coin tCoin = Coin.valueOf(pAmount);
			return tCoin.toFriendlyString();
		}
		throw new RuntimeException("coin friendly string not yet implemented for " + pSid );
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

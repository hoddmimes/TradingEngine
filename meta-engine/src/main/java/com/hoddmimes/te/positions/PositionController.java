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

package com.hoddmimes.te.positions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.TeException;
import com.hoddmimes.te.common.db.TEDB;
import com.hoddmimes.te.common.interfaces.TeIpcServices;
import com.hoddmimes.te.engine.InternalTrade;
import com.hoddmimes.te.common.ipc.IpcRequestCallbackInterface;
import com.hoddmimes.te.common.ipc.IpcComponentInterface;
import com.hoddmimes.te.instrumentctl.InstrumentContainer;
import com.hoddmimes.te.management.gui.mgmt.PrcFmt;
import com.hoddmimes.te.messages.MgmtMessageRequest;
import com.hoddmimes.te.messages.MgmtMessageResponse;
import com.hoddmimes.te.messages.SID;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import com.hoddmimes.te.trades.TradeX;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class PositionController implements IpcRequestCallbackInterface
{
	private Logger mLog = LogManager.getLogger( PositionController.class);
	private HashMap<String, AccountPosition> mAccountMap;
	private JsonObject mPositionConfig;
	private boolean mPreTradingValidation;

	public PositionController( JsonObject pTeConfigurationFile )
	{
		mPositionConfig = AuxJson.navigateObject(pTeConfigurationFile,"TeConfiguration/positionConfiguration");
		mAccountMap = new HashMap<>();
		mPreTradingValidation = AuxJson.navigateBoolean( mPositionConfig,"preTradingValidation");
		mLog.info("pre-trade validation is [" + String.valueOf( mPreTradingValidation) + "]");
		if (mPreTradingValidation) {
			loadPositions();
		}
		TeAppCntx.getInstance().setPositionController( this );
		IpcComponentInterface tMgmt = TeAppCntx.getInstance().getIpcService().registerComponent( TeIpcServices.PositionData, 0, this );
	}

	public boolean isPreTradingValidationEnabled() {
		return mPreTradingValidation;
	}

	private void loadPositions() {
		try {
			String tDataStore = AuxJson.navigateString(mPositionConfig, "dataStore");
			JsonElement jElement = AuxJson.loadAndParseFile(tDataStore).get(0);
			JsonObject tPositionConfig = jElement.getAsJsonObject();
			adjustCashValues( tPositionConfig );
			JsonArray jAccountArray = tPositionConfig.get("Accounts").getAsJsonArray();
			for (int i = 0; i < jAccountArray.size(); i++)
			{
				JsonObject jAccount = jAccountArray.get(i).getAsJsonObject();
				JsonArray jPositions = jAccount.getAsJsonArray("positions");

				AccountPosition tAccountPosition = new AccountPosition( jAccount.get("account").getAsString(), jAccount.get("cash").getAsLong());
				for (int j = 0; j < jPositions.size(); j++) {
					JsonObject jPosition = jPositions.get(j).getAsJsonObject();
					SID tSID = new SID( jPosition.get("market").getAsInt(), jPosition.get("symbol").getAsString());
					tAccountPosition.setPosition( tSID.toString(), jPosition.get("position").getAsLong());
				}
				if (mAccountMap.containsKey( tAccountPosition.getAccount())) {
					mLog.warn("Position for account " + tAccountPosition.getAccount() + " is already defined, duplicates?");
				}
				mAccountMap.put(tAccountPosition.getAccount(), tAccountPosition);
			}
			mLog.info("Loaded positions for " + mAccountMap.size() + " accounts");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}




	private void adjustCashValues( JsonObject pPositionConfig  ) {
		JsonArray jAccountArray = pPositionConfig.get("Accounts").getAsJsonArray();
		for (int i = 0; i < jAccountArray.size(); i++) {
			JsonObject jAccount = jAccountArray.get(i).getAsJsonObject();
			AuxJson.adjustPriceValue( jAccount, "cash", TeAppCntx.PRICE_MULTIPLER);
		}
	}

	public AccountPosition getAccount( String pAccountId ) {
		return mAccountMap.get( pAccountId );
	}

	/**
	 * Invoked from the Trade Container as part of trades are loaded
	 * as part of the startup.
	 * @param pTradeX
	 */
	public synchronized void tradeExcution( TradeX pTradeX ) {
		if (!mPreTradingValidation) {
			return;
		}

		AccountPosition tBuyAccountPos = getAccount(pTradeX.getBuyer().get());
		if (tBuyAccountPos != null) {
			tBuyAccountPos.execution(pTradeX);
		}
		AccountPosition tSellAccountPos = getAccount(pTradeX.getSeller().get());
		if (tSellAccountPos != null) {
			tSellAccountPos.execution(pTradeX);
		}
	}

	/**
	 * Invoked from the matching engine as of a execution.
	 * @param pInternalTrade
	 */
	public synchronized void tradeExcution( InternalTrade pInternalTrade ) {
		if (!mPreTradingValidation) {
			return;
		}

		AccountPosition tBuyAccountPos = getAccount( pInternalTrade.getBuyOrder().getAccountId());
		if (tBuyAccountPos != null) {
			tBuyAccountPos.execution( pInternalTrade );
		}
		AccountPosition tSellAccountPos = getAccount( pInternalTrade.getSellOrder().getAccountId());
		if (tSellAccountPos != null) {
			tSellAccountPos.execution( pInternalTrade );
		}
	}

	public synchronized void updateHolding( String pAccountId, int pMarket, String pSymbol, long pDeltaPosition, long pTxNo )
	{
		AccountPosition tPosition = mAccountMap.get( pAccountId );
		if (tPosition == null) {
			tPosition = new AccountPosition( pAccountId, 0L);
			mAccountMap.put( pAccountId, tPosition );
		}
		SID tSid = new SID( pMarket, pSymbol );
		tPosition.updatePosition( tSid.toString(), pDeltaPosition, pTxNo );
	}

	public synchronized void updateCash( String pAccountId, double pAmount ) {
        AccountPosition tPosition = mAccountMap.get( pAccountId );
		if (tPosition == null) {
			tPosition = new AccountPosition( pAccountId, PrcFmt.convert(pAmount));
			mAccountMap.put( pAccountId, tPosition );
		} else {
			tPosition.updateCashPosition( PrcFmt.convert(pAmount));
		}
	}

	public synchronized void initialSyncCrypto( List<DbCryptoDeposit> pCryptoDeposits ) {
		for( DbCryptoDeposit tCryptoDeposit : pCryptoDeposits) {
			AccountPosition tPosition = mAccountMap.get( tCryptoDeposit.getAccountId().get() );
			if (tPosition == null) {
				tPosition = new AccountPosition( tCryptoDeposit.getAccountId().get(), 0L);
				mAccountMap.put( tCryptoDeposit.getAccountId().get(), tPosition );
			}

			for(DbDepositHolding tCryptoHolding : tCryptoDeposit.getHoldings().get()) {
				tPosition.setPosition( tCryptoHolding.getSid().get(), tCryptoHolding.getHolding().get());
			}
		}
	}

	public synchronized MessageInterface redrawCrypto( CryptoReDrawRequest pReDrawRqst, long pSellOrderExposure )
	{
		String txid = null;

		String tAccountId =  pReDrawRqst.getAccountId().get();
		String tCoinSid = pReDrawRqst.getCoin().get();

		// Verify that the Posion controller has an entry and position for the Account / coin
		AccountPosition tAccPos = mAccountMap.get( tAccountId );
		if (tAccPos == null) {
			mLog.warn(" (redrawCrypto) No account position found for account: " + tAccountId );
			return StatusMessageBuilder.error("No account position found", null);
		}

		HoldingEntry tAccHolding = tAccPos.getHoldingPosition( tCoinSid );
		if (tAccHolding == null) {
			mLog.warn(" (redrawCrypto) No account holding found for coin: " + tCoinSid + " (account: " + tAccountId + ")");
			return StatusMessageBuilder.error("No account holding found for coin: " + tCoinSid + " (account: " + tAccountId + ")", null);
		}

		// Verify that there is a crypto deposit entry in the database for the account / coin
		TEDB tDb = TeAppCntx.getInstance().getDb();
		DbCryptoDeposit tCryptDeposit = (DbCryptoDeposit) TEDB.dbEntryFound( tDb.findDbCryptoDepositByAccountId(tAccountId));
		if (tCryptDeposit == null) {
			mLog.warn(" (redrawCrypto) No crypto deposit account found (account: " + tAccountId + " )");
			return StatusMessageBuilder.error("No crypto deposit account found", null);
		}
		DbDepositHolding tCryptoHolding = tCryptDeposit.findHolding( tCoinSid );
		if (tCryptoHolding == null) {
			mLog.warn(" (redrawCrypto) No crypto holding found for coin: " + tCoinSid + " (account: " + tAccountId + " )");
			return StatusMessageBuilder.error("No crypto holding found for  coin: " + tCoinSid , null);
		}

		// Validate holdings current position and value of outstanding sell order
		// This SHOULD never happen !!!!
		if (tAccHolding.getHolding() != tCryptoHolding.getHolding().get()) {
				mLog.error(" (redrawCrypto) coin: " + tCoinSid + " holding different between PositionController( " + tAccHolding.getHolding() + " )" +
						" and CryptoDeposit( " + tCryptoHolding.getHolding().get() + " )");
		}

		// Veify that the account has enough with coins available

		if ((tAccHolding.getHolding() - pSellOrderExposure) < pReDrawRqst.getAmount().get()) {
			mLog.error(" (redrawCrypto) coin: " + tCoinSid + " insufficent holdings account-holding: " + tAccHolding.getHolding() + " )" +
					" sell-exposure: " + tCryptoHolding.getHolding().get() + " redraw-amount: " + pReDrawRqst.getAmount().get());
			return StatusMessageBuilder.error("insufficent holdings account-holding: " + tAccHolding.getHolding() + " )" +
					" sell-exposure: " + tCryptoHolding.getHolding().get() + " redraw-amount: " + pReDrawRqst.getAmount().get(), null);
		}

		// Update the databse holding
		try {
			txid = TeAppCntx.getInstance().getCryptoGateway().sendCoins( pReDrawRqst );
			CryptoReDrawResponse tResponse = new CryptoReDrawResponse().setRef( pReDrawRqst.getRef().get());
			tResponse.setRemaingCoins( tAccHolding.getHolding() - pReDrawRqst.getAmount().get()); // todo: needs to be adjusted for tx fee
			tResponse.setCoin( pReDrawRqst.getCoin().get());
			tResponse.setTxid( txid );


			// Update holding
			tAccHolding.updateHolding( (-1L * pReDrawRqst.getAmount().get()), 0);
			tCryptoHolding.updateHolding( (-1L * pReDrawRqst.getAmount().get()));
			tDb.updateDbCryptoDeposit( tCryptDeposit, false  );

			return tResponse;
		}
		catch( TeException te) {
		  mLog.error( te.getStatusMessage().toJson().toString());
		  return te.getStatusMessage();
		}
	}



	@Override
	public synchronized MessageInterface ipcRequest(MessageInterface pMgmtRequest) {
		if (pMgmtRequest instanceof MgmtGetAccountPositionsRequest) {
			return mgmtGetPositionData((MgmtGetAccountPositionsRequest) pMgmtRequest );
		}
		throw new RuntimeException("mgmt request not supported " + pMgmtRequest.getMessageName());
	}

	private MgmtGetAccountPositionsResponse mgmtGetPositionData(MgmtGetAccountPositionsRequest pRequest ) {
		MgmtGetAccountPositionsResponse tResponse = new MgmtGetAccountPositionsResponse().setRef( pRequest.getRef().get());
		String tAccountId = pRequest.getAccount().orElse("");
		AccountPosition tAccountPosition = mAccountMap.get( tAccountId );
		if (tAccountPosition == null) {
			tResponse.setIsDefined( false );
			return tResponse;
		}
		tResponse.setIsDefined( true );
		tResponse.setAccountId( tAccountId );
		tResponse.setCash( tAccountPosition.getCashPosition());
		tResponse.setPositions( tAccountPosition.getPositionsForMgmt());
		return tResponse;
	}

}

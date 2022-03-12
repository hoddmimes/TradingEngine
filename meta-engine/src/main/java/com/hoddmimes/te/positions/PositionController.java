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
import com.hoddmimes.te.TeCoreService;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.Crypto;
import com.hoddmimes.te.common.TeException;
import com.hoddmimes.te.common.db.CryptoDepositUpdateEvent;
import com.hoddmimes.te.common.db.TEDB;
import com.hoddmimes.te.common.interfaces.TeService;
import com.hoddmimes.te.common.ipc.IpcService;
import com.hoddmimes.te.cryptogwy.CryptoDepositReadOnly;
import com.hoddmimes.te.cryptogwy.CryptoDepository;
import com.hoddmimes.te.cryptogwy.CryptoGateway;
import com.hoddmimes.te.engine.InternalTrade;
import com.hoddmimes.te.engine.Order;
import com.hoddmimes.te.instrumentctl.InstrumentContainer;
import com.hoddmimes.te.instrumentctl.SymbolX;
import com.hoddmimes.te.management.gui.mgmt.PrcFmt;
import com.hoddmimes.te.messages.DbCryptoDeposit;
import com.hoddmimes.te.messages.DbCryptoHolding;
import com.hoddmimes.te.messages.SID;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * The Position Controller is in memory controller managing client holdings/position.
 * The position/holding are changed updated when
 * 1) at startup the initial positions for none crypto assets are loaded, currently load from file ./configuration/Positions.json
 *    For crypto assets the current positions are loaded from the CryptoDeposit (i.e. from the database ):
 *
 *    Normally crypto assets are synchronized with repective crypto network. And the current position / holding
 *    for a client is stored in the DB (i.e. DBCryptoDeposit). Note! there is only one wallet per crypto asset.
 *    And the Crypto Deposit keep track on what each user / account is holding
 *
 *
 * 2) at startup any trade done from a previous session and current day will be replayed.
 *    This also applies for the crypto assets. The CryptoDeposit class must validate what
 *    trades has been updated the DbCryptoDeposit records.
 *
 * 3) during continues trading trades done will update the holdings/position
 *
 * 4) when a crypto deposit is done, the holding will be updated via the callback cryptoDeposit.
 */

public class PositionController extends TeCoreService
{
	private Logger mLog = LogManager.getLogger( PositionController.class);
	private HashMap<String, AccountPosition> mAccountMap;
	private JsonObject mPositionConfig;
	private boolean mPreTradingValidation;
	private TEDB mDb;
	private InstrumentContainer mInstrumentContainer;
	private CryptoDepository mCryptoDepository;

	public PositionController(JsonObject pTeConfiguration, IpcService pIpcService, TEDB pDatabase)
	{
		super( pTeConfiguration, pIpcService );
		mDb = pDatabase;
		mCryptoDepository = new CryptoDepository( pTeConfiguration, mDb );
		mInstrumentContainer = (InstrumentContainer) TeAppCntx.getInstance().getService( TeService.InstrumentData );
		mPositionConfig = AuxJson.navigateObject(pTeConfiguration,"TeConfiguration/positionConfiguration");
		mAccountMap = new HashMap<>();
		mPreTradingValidation = AuxJson.navigateBoolean( mPositionConfig,"preTradingValidation");
		mLog.info("pre-trade validation is [" + String.valueOf( mPreTradingValidation) + "]");
		if (mPreTradingValidation) {
			loadPositions();
		}
		TeAppCntx.getInstance().registerService( this );
	}

	public boolean isPreTradingValidationEnabled() {
		return mPreTradingValidation;
	}

	public CryptoDepositReadOnly getCryptoDeposit() {
		return mCryptoDepository;
	}


	private void loadPositions() {
		loadNoneCryptoPositions();
		loadCryptoPositions();
	}

	public boolean hasPosition( String pAccountId, String pSid ) {
		AccountPosition tAccountPosition = mAccountMap.get(pAccountId);
		if (tAccountPosition == null) {
			return false;
		}
		return true;
	}

	private void loadCryptoPositions() {
		int tHoldinsCount = 0;
		List<DbCryptoDeposit>  tCryptoDeposits = mCryptoDepository.getCryptoPositions();
		for( DbCryptoDeposit tDeposit : tCryptoDeposits) {
			if (tDeposit.getHoldings().isPresent()) {

				for (DbCryptoHolding tHolding : tDeposit.getHoldings().get()) {
					tHoldinsCount++;
					AccountPosition tAccountPosition = getAccount( tDeposit.getAccountId().get());
					tAccountPosition.setPosition( tHolding.getSid().get(), tHolding.getQuantity().get());
				}
			}
		}
		mLog.info("Loaded " + tCryptoDeposits.size() + " accounts having " + tHoldinsCount + " crypto assets holdings");
	}

	private void loadNoneCryptoPositions() {

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

				// Crypto assets are not suposed to be loaded / sychronized via a the position load file
				// any how we have func for setting the DbCrypto deposit the hard way, should not really happen.
				for( HoldingEntry tHolding : tAccountPosition.getHoldings()) {
					if (mInstrumentContainer.isCryptoMarket(tHolding.getSid())) {
						mLog.warn("Loading crypto position (" + tHolding.getSid() + " from: " + tDataStore + " This should not happen!!!") ;
						//mCryptoDepository.set(tAccountPosition.getAccount(), tHolding.getSid(), tHolding.getHolding());
					}
				}
			}
			mLog.info("Loaded (none crypto) positions for " + mAccountMap.size() + " accounts");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Any time the holdings change for an account this is the method to be invoked.
	 * No other code must change the position for the account, nor the real-time position or
	 * the DB crypto position.
	 *
	 *
	 * Note! that the database updates are queued in a separate thread to minimize the latency
	 *
	 * @param pAccountId
	 * @param pSid
	 * @param pDeltaPosition
	 * @param pTxNo
	 */
	public synchronized void updateHolding( String pAccountId, String pSid, long pDeltaPosition, long pTxNo ) {
		/**
		 * Update the real-time holding position, this applies to all assets
		 */
		AccountPosition tPosition = getAccount( pAccountId );
		tPosition.updatePosition( pSid, pDeltaPosition, pTxNo );

		/**
		 * If the asset is a crypto asset update DB deposit
		 */
		if (mInstrumentContainer.isCryptoMarket( pSid)) {
			mCryptoDepository.queueCryptoUpdate( new CryptoDepositUpdateEvent( pAccountId, pSid, pDeltaPosition, pTxNo ));
		}
	}

	public synchronized void updateHolding( String pAccountId, String pSid, long pDeltaPosition, String  pTxid ) {
		/**
		 * Update the real-time holding position, this applies to all assets
		 */
		AccountPosition tPosition = getAccount( pAccountId );
		tPosition.updatePosition( pSid, pDeltaPosition );

		/**
		 * If the asset is a crypto asset update DB deposit
		 */
		if (mInstrumentContainer.isCryptoMarket( pSid)) {
			mCryptoDepository.queueCryptoUpdate( new CryptoDepositUpdateEvent( pAccountId, pSid, pDeltaPosition, pTxid ));
		}
	}



	public void updateUpdateCryptoPosition( String pAccountId, String pSid, long pDeltaPositionNormalized, String pTxid) {
		mCryptoDepository.queueCryptoUpdate( new CryptoDepositUpdateEvent( pAccountId, pSid, pDeltaPositionNormalized, pTxid ));
	}

	private void adjustCashValues( JsonObject pPositionConfig  ) {
		JsonArray jAccountArray = pPositionConfig.get("Accounts").getAsJsonArray();
		for (int i = 0; i < jAccountArray.size(); i++) {
			JsonObject jAccount = jAccountArray.get(i).getAsJsonObject();
			AuxJson.adjustPriceValue( jAccount, "cash", TeAppCntx.PRICE_MULTIPLER);
		}
	}

	 public synchronized AccountPosition getAccount( String pAccountId ) {

		AccountPosition tAccountPosition = mAccountMap.get( pAccountId );
		if (tAccountPosition == null) {
			tAccountPosition = new AccountPosition( pAccountId, 0L);
			mAccountMap.put( pAccountId, tAccountPosition );
		}
		return tAccountPosition.clone();
	}

	/**
	 * Invoked from the Trade Container as part of trades are loaded (replayed)
	 * and during continues trading when a trade execution has occured
	 * @param pTrade
	 */
	public synchronized void tradeExcution( InternalTrade pTrade ) {
		if (!mPreTradingValidation) {
			return;
		}

		// Process Buy side
		AccountPosition tBuyAccountPos = getAccount(pTrade.getBuyOrder().getAccountId());
		tBuyAccountPos.execution( pTrade.getSid(),
				                  Order.Side.BUY,
				                  pTrade.getPrice(),
				                  pTrade.getQuantity(),
								  pTrade.getTeBuySeqno());

		if (mInstrumentContainer.isCryptoMarket(pTrade.getSid())) {
			mCryptoDepository.queueCryptoUpdate(new CryptoDepositUpdateEvent( pTrade.getBuyOrder().getAccountId(),
																			  pTrade.getSid(),
																			  pTrade.getQuantity(),
																			  pTrade.getTeBuySeqno()));
		}

		// Process Sell side
		AccountPosition tSellAccountPos = getAccount(pTrade.getSellOrder().getAccountId());
		tSellAccountPos.execution(pTrade.getSid(),
								 Order.Side.SELL,
				                 pTrade.getPrice(),
				                 pTrade.getQuantity(),
				                 pTrade.getTeSellSeqno());

		if (mInstrumentContainer.isCryptoMarket(pTrade.getSid())) {
			mCryptoDepository.queueCryptoUpdate(new CryptoDepositUpdateEvent( pTrade.getSellOrder().getAccountId(),
					                                                          pTrade.getSid(),
																			  (-1L * pTrade.getQuantity()),
					                                                          pTrade.getTeSellSeqno()));
		}
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

	public boolean validateBuyOrder(Order pOrder, long pExposure ) {
		return getAccount(pOrder.getAccountId() ).validateBuyOrder( pOrder, pExposure );
	}

	public boolean validateSellOrder(Order pOrder, int pMarketPos ) {
		return getAccount(pOrder.getAccountId()).validateSellOrder( pOrder, pMarketPos );
	}

	/**
	 *
	 * @param pRedrawRqst
	 * @param pSellOrderExposure
	 * @return
	 */
	public synchronized MessageInterface redrawCrypto(CryptoRedrawRequest pRedrawRqst, long pSellOrderExposure) {
		String txid = null;
		long tEstimatedTxFeeNA = 0L;

		SymbolX tCryptoInst = mInstrumentContainer.getCryptoInstrument(pRedrawRqst.getCoin().get());
		if (tCryptoInst == null) {
			return StatusMessageBuilder.error("No such crypto asset found", null);
		}

		// Just a sanity check, the possition holdings should be the same as the crypto deposit holdings
		long tCryptoDepositHolding = mCryptoDepository.getCryptoHolding(pRedrawRqst.getAccountId().get(), tCryptoInst.getSid().get());
		long tAccountPositionHolding = getAccount(pRedrawRqst.getAccountId().get()).getHolding(tCryptoInst.getSid().get());
		if (tCryptoDepositHolding != tAccountPositionHolding) {
			mLog.warn("(redrawCrypto) account-holding (" + tAccountPositionHolding + ")  != crypto-deposit-holding (" + tCryptoDepositHolding + ")");
		}

		// Verify that the account has enough with coins available. Besides the amount the client would like to redraw
		// potential transaction fee must also be considered, checks will be done against the Postion controller holdings
		CryptoGateway tCryptoGwy = (CryptoGateway) TeAppCntx.getCryptoGateway();
		tEstimatedTxFeeNA = tCryptoGwy.getEstimatedTxFeeNA(Crypto.CoinType.valueOf(pRedrawRqst.getCoin().get())); // Normalized amount

		if (tAccountPositionHolding < (pRedrawRqst.getAmount().get() + pSellOrderExposure + tEstimatedTxFeeNA)) {
			mLog.warn("failed redraw (accounposition)" + pRedrawRqst.getCoin().get() + " amount: " + pRedrawRqst.getAmount().get() + " sell-exposure: " + pSellOrderExposure + " txFee: " + tEstimatedTxFeeNA);
			return StatusMessageBuilder.error("Insufficent holdings", null);
		}
		if (tCryptoDepositHolding < (pRedrawRqst.getAmount().get() + pSellOrderExposure + tEstimatedTxFeeNA)) {
			mLog.warn("failed redraw (depositposition)" + pRedrawRqst.getCoin().get() + " amount: " + pRedrawRqst.getAmount().get() + " sell-exposure: " + pSellOrderExposure + " txFee: " + tEstimatedTxFeeNA);
			return StatusMessageBuilder.error("Insufficent holdings", null);
		}


		try {
			txid = tCryptoGwy.sendCoins(pRedrawRqst);
			directPositionAdjustment(pRedrawRqst.getAccountId().get(), tCryptoInst.getSid().get(), (-1L * (pRedrawRqst.getAmount().get() + tEstimatedTxFeeNA)));

			CryptoRedrawResponse tResponse = new CryptoRedrawResponse().setRef(pRedrawRqst.getRef().get());
			tResponse.setRemaingCoins(mCryptoDepository.getCryptoHolding(pRedrawRqst.getAccountId().get(), tCryptoInst.getSid().get()));
			tResponse.setTxid(txid);

			// The transaction has been sent to the crypto network and it's assumed to go through and be confirmed
			// however it is not guaranteed that it will go through it may be rejected by the network.
			// It will be the responsibility of each crypto gateway to monitor the TX and correct the positions
			// in case a tx is rejected. So for the time being update holding as the tx had been confirmed.
			//this.updateCryptoPosition( tAccountId, tCoinSid, (-1L * (pRedrawRqst.getAmount().get() + tEstimatedTxFeeNA )));

			return tResponse;
		} catch (TeException te) {
			mLog.error(te.getStatusMessage().toJson().toString());
			return te.getStatusMessage();
		}
	}

	private void directPositionAdjustment( String pAccountId, String pSid, long pDeltaAmountNormalized ) {
		AccountPosition tAccountPosition = this.getAccount( pAccountId );
		tAccountPosition.updatePosition( pSid, pDeltaAmountNormalized );

		mCryptoDepository.directPositionAdjustment(pAccountId,  pSid,  pDeltaAmountNormalized );
	}



	public List<DbCryptoDeposit> getCryptoPositions() {
		return mCryptoDepository.getCryptoPositions();
	}


	public List<DbCryptoPayment> getCryptoPayments() {
		return mCryptoDepository.getCryptoPayments();
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


	@Override
	public TeService getServiceId() {
		return TeService.PositionData;
	}
}

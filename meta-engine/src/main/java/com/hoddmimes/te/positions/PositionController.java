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
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.interfaces.TeMgmtServices;
import com.hoddmimes.te.engine.InternalTrade;
import com.hoddmimes.te.management.service.MgmtCmdCallbackInterface;
import com.hoddmimes.te.management.service.MgmtComponentInterface;
import com.hoddmimes.te.messages.MgmtMessageRequest;
import com.hoddmimes.te.messages.MgmtMessageResponse;
import com.hoddmimes.te.messages.SID;
import com.hoddmimes.te.messages.generated.*;
import com.hoddmimes.te.trades.TradeX;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;

public class PositionController implements MgmtCmdCallbackInterface
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
		MgmtComponentInterface tMgmt = TeAppCntx.getInstance().getMgmtService().registerComponent( TeMgmtServices.PositionData, 0, this );
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
					tAccountPosition.addPosition( tSID.toString(), jPosition.get("position").getAsInt());
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

	@Override
	public synchronized MgmtMessageResponse mgmtRequest(MgmtMessageRequest pMgmtRequest) {
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

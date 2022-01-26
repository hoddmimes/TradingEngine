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

import com.hoddmimes.te.engine.InternalTrade;
import com.hoddmimes.te.engine.Order;
import com.hoddmimes.te.messages.generated.MgmtPositionEntry;
import com.hoddmimes.te.trades.TradeX;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;


public class AccountPosition
{
	static Logger cLog = LogManager.getLogger("AccountPosition");

	private String mAccount;
	private     HashMap<String, Integer> mPosition;
	private long mCash;

	public AccountPosition(String pAccount, long pCash  ) {
		mAccount = pAccount;
		mCash = pCash;
		mPosition = new HashMap<>();
	}

	public long getCashPosition() { return mCash; }
	public String getAccount() {
		return mAccount;
	}

	public void addPosition( String pSid, int pPosition )
	{
		mPosition.put( pSid, pPosition );
	}

	public boolean validateBuyOrder(Order pOrder, long pExposure ) {
		// if a buy validate that the money is there.
		if (pOrder.getSide() != Order.Side.BUY) {
			throw new RuntimeException("Invalid Holding Validation");
		}
		long tNet = (mCash - pExposure) - (pOrder.getPrice() * pOrder.getQuantity());

		if (tNet < 0) {
			cLog.warn("Buy order exceeds the cash limit for  account: " + mAccount + " and sid: " + pOrder.getSid() +
					"\n   deposit cash: " + mCash + " market buy exposure: " + pExposure + " order value: " + (pOrder.getPrice() * pOrder.getQuantity()));
			return false;
		}
	    return true;
	}

	public boolean validateSellOrder( Order pOrder, int pMarketPosition ) {
		// if a buy validate that the money is there.
		if (pOrder.getSide() != Order.Side.SELL) {
			throw new RuntimeException("Invalid Holding Validation");
		}
		Integer tDepositPosition = mPosition.get( pOrder.getSid());
		if (tDepositPosition == null) {
			cLog.warn("No selling position defined for account: " + mAccount + " and sid: " + pOrder.getSid() + "\n order: " + pOrder.toString());
			return false;
		}

		int tNet = (tDepositPosition - pMarketPosition - pOrder.getQuantity());
		if (tNet < 0) {
			cLog.warn("Selling position for account: " + mAccount + " and sid: " + pOrder.getSid() + " is exceeded \n" +
					" deposit position: " + tDepositPosition + " market exposure: " + pMarketPosition + " order size: " + pOrder.getQuantity());
			return false;
		}
		return true;
	}


	// Method invoked when starting and  loading trades from a previous instansiating, same day.
	public void execution( TradeX pTradeX )
	{
		if (pTradeX.getBuyer().get().contentEquals(mAccount)) {
			mCash -= (pTradeX.getPrice().get() * pTradeX.getQuantity().get());
			if (mCash < 0) {
				cLog.warn("load of old trades resulted in account " + mAccount + " now has exceeded the cash limit, cash holding: " + mCash );
			}


			Integer tNewDepositPosition  = (mPosition.containsKey(pTradeX.getSid().get())) ?
					(mPosition.get(pTradeX.getSid().get()) + pTradeX.getQuantity().get()) :
					pTradeX.getQuantity().get();

			mPosition.put( pTradeX.getSid().get(), tNewDepositPosition);
		}

		if (pTradeX.getSeller().get().contentEquals(mAccount)) {
			mCash += (pTradeX.getPrice().get() * pTradeX.getQuantity().get());

			if (!mPosition.containsKey(pTradeX.getSid().get())) {
				throw new RuntimeException( mAccount + " sold " + pTradeX.getSid().get() + " for which it had no position");
			}

			Integer tNewDepositPosition  = (mPosition.get(pTradeX.getSid().get()) - pTradeX.getQuantity().get());
			if (tNewDepositPosition < 0) {
				throw new RuntimeException( mAccount + " sold " + pTradeX.getSid().get() + " and deposit is now negative (" + tNewDepositPosition +")");
			}
			if (tNewDepositPosition < 0) {
				cLog.warn("load of old trades resulted in account " + mAccount + " now has exceeded the position limit for sid: " +
						pTradeX.getSid().get() + " position: " + tNewDepositPosition  );
			}
			mPosition.put( pTradeX.getSid().get(), tNewDepositPosition);
		}
	}


	// Executed when a trade is done.
	public void execution( InternalTrade pInternalTrade )
	{
		if (pInternalTrade.isOnBuySide(mAccount)) {
			mCash -= (pInternalTrade.getPrice() * pInternalTrade.getQuantity());
			if (mCash < 0) {
				cLog.error( mAccount + " bougth sid:" + pInternalTrade.getSid() + " and now got a negative cash position, cash: " + mCash );
			}

			Integer tNewDepositPosition  = (mPosition.containsKey(pInternalTrade.getSid())) ?
					(mPosition.get(pInternalTrade.getSid()) + pInternalTrade.getQuantity()) :
					pInternalTrade.getQuantity();

			mPosition.put( pInternalTrade.getSid(), tNewDepositPosition);
		}

		if (pInternalTrade.isOnSellSide(mAccount)) {
			mCash += (pInternalTrade.getPrice() * pInternalTrade.getQuantity());

			if (!mPosition.containsKey(pInternalTrade.getSid())) {
				cLog.error( mAccount + " sold " + pInternalTrade.getSid() + " for which it had no position");
			}

			Integer tNewDepositPosition  = (mPosition.get(pInternalTrade.getSid()) - pInternalTrade.getQuantity());
			if (tNewDepositPosition < 0) {
				cLog.error( mAccount + " sold " + pInternalTrade.getSid() + " and deposit is now negative (" + tNewDepositPosition +")");
			}
			mPosition.put( pInternalTrade.getSid(), tNewDepositPosition);
		}
	}

	public List<MgmtPositionEntry> getPositionsForMgmt() {
		List<MgmtPositionEntry> tPositions = new ArrayList<>();
		 Iterator<Map.Entry<String,Integer>> tPosItr = mPosition.entrySet().iterator();
		 while( tPosItr.hasNext()) {
			 Map.Entry<String,Integer> tEntry = tPosItr.next();
			 tPositions.add( new MgmtPositionEntry().setSid(tEntry.getKey()).setPosition(tEntry.getValue()));
		 }
		 return tPositions;
	}
}

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

	private String  mAccount;
	private         HashMap<String, HoldingEntry> mPosition;
	private long    mCash;

	public AccountPosition(String pAccount, long pCash  ) {
		mAccount = pAccount;
		mCash = pCash;
		mPosition = new HashMap<>();
	}

	public void updatePosition( String pSid, long pDeltaPosition, long pTxNo ) {
		HoldingEntry tHolding = mPosition.get( pSid );
		if (tHolding == null)  {
			tHolding = new HoldingEntry(pSid, pDeltaPosition, pTxNo );
			mPosition.put( pSid, tHolding);
		} else {
			tHolding.updateHolding( pDeltaPosition, pTxNo );
		}
	}


	public void updateCashPosition( long pDeltaCash ) {
		mCash += pDeltaCash;
	}
	public long getCashPosition() { return mCash; }
	public HoldingEntry getHoldingPosition( String pSid ) {
		return mPosition.get( pSid );
	}
	public String getAccount() {
		return mAccount;
	}

	public void setPosition( String pSid, long pHolding ) {
		HoldingEntry tHolding = mPosition.get(pSid);
		if (tHolding == null) {
			tHolding = new HoldingEntry(pSid, pHolding, 0);
			mPosition.put( pSid, tHolding);
		} else {
			tHolding.setHolding(pHolding, 0);
		}
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
		HoldingEntry tHolding = mPosition.get( pOrder.getSid());
		if (tHolding == null) {
			cLog.warn("No selling position defined for account: " + mAccount + " and sid: " + pOrder.getSid() + "\n order: " + pOrder.toString());
			return false;
		}

		long tNet = (tHolding.getHolding() - pMarketPosition - pOrder.getQuantity());
		if (tNet < 0) {
			cLog.warn("Selling position for account: " + mAccount + " and sid: " + pOrder.getSid() + " is exceeded \n" +
					" deposit position: " + tHolding.getHolding() + " market exposure: " + pMarketPosition + " order size: " + pOrder.getQuantity());
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

			HoldingEntry tHolding = mPosition.get( pTradeX.getSid().get());
			if (tHolding == null) {
				tHolding.setHolding(pTradeX.getQuantity().get(), pTradeX.getTradeId().get());
			} else {
				if (pTradeX.getTradeId().get() > tHolding.getTxNo()) {
					tHolding.addHolding(pTradeX.getQuantity().get(), pTradeX.getTradeId().get());
				}
			}
		}

		if (pTradeX.getSeller().get().contentEquals(mAccount)) {
			mCash += (pTradeX.getPrice().get() * pTradeX.getQuantity().get());

			HoldingEntry tHolding = mPosition.get( pTradeX.getSid().get());
			if (tHolding == null) {
				throw new RuntimeException( mAccount + " sold " + pTradeX.getSid().get() + " and deposit is now negative (" + (-1 * pTradeX.getTradeId().get()) +")");
			} else {
				if (pTradeX.getTradeId().get() > tHolding.getTxNo()) {
					tHolding.addHolding((-1 * pTradeX.getQuantity().get()), pTradeX.getTradeId().get());
				}
			}
		}
	}


	// Executed when a trade is done.
	public void execution( InternalTrade pInternalTrade )
	{
		if (pInternalTrade.isOnBuySide(mAccount)) {
			mCash -= (pInternalTrade.getPrice() * pInternalTrade.getQuantity());
			if (mCash < 0) {
				cLog.error(mAccount + " bougth sid:" + pInternalTrade.getSid() + " and now got a negative cash position, cash: " + mCash);
			}

			HoldingEntry tHolding = mPosition.get(pInternalTrade.getSid());
			if (tHolding == null) {
				tHolding.setHolding(pInternalTrade.getQuantity(), pInternalTrade.getTradeNo());
			} else {
				if (pInternalTrade.getTradeNo() > tHolding.getTxNo()) {
					tHolding.addHolding(pInternalTrade.getQuantity(), pInternalTrade.getTradeNo());
				}
			}
		}

		if (pInternalTrade.isOnSellSide(mAccount)) {
			mCash += (pInternalTrade.getPrice() * pInternalTrade.getQuantity());

			HoldingEntry tHolding = mPosition.get( pInternalTrade.getSid());
			if (tHolding == null) {
				throw new RuntimeException( mAccount + " sold " + pInternalTrade.getSid() + " and deposit is now negative (" + (-1 * pInternalTrade.getQuantity()) +")");
			} else {
				if (pInternalTrade.getTradeNo()> tHolding.getTxNo()) {
					tHolding.addHolding((-1 * pInternalTrade.getQuantity()), pInternalTrade.getTradeNo());
				}
			}
		}
	}

	public List<MgmtPositionEntry> getPositionsForMgmt() {
		List<MgmtPositionEntry> tPositions = new ArrayList<>();
		 Iterator<Map.Entry<String,HoldingEntry>> tPosItr = mPosition.entrySet().iterator();
		 while( tPosItr.hasNext()) {
			 Map.Entry<String,HoldingEntry> tEntry = tPosItr.next();
			 tPositions.add( new MgmtPositionEntry().setSid(tEntry.getKey()).setPosition(tEntry.getValue().getHolding()));
		 }
		 return tPositions;
	}
}

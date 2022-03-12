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

import com.hoddmimes.te.engine.Order;
import com.hoddmimes.te.messages.generated.MgmtPositionEntry;
import com.hoddmimes.te.messages.generated.Position;
import com.hoddmimes.te.messages.generated.QueryPositionResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;


public class AccountPosition implements Cloneable
{
	static Logger cLog = LogManager.getLogger("AccountPosition");

	private String  mAccount;
	private         HashMap<String, HoldingEntry> mPosition;
	private long    mCash;

	 AccountPosition(String pAccount, long pCash  ) {
		mAccount = pAccount;
		mCash = pCash;
		mPosition = new HashMap<>();
	}

	@Override
	public AccountPosition clone()
	{
		AccountPosition ap = new AccountPosition( this.mAccount, this.mCash );
		if (mPosition != null) {
			ap.mPosition = new HashMap<>();
			for( HoldingEntry tHoldingEntry : mPosition.values()) {
				HoldingEntry he = tHoldingEntry.clone();
				ap.mPosition.put( he.getSid(), he);
			}
		}
		return ap;
	}


	 void updateCashPosition( long pDeltaCash ) {
		mCash += pDeltaCash;
	}
	 long getCashPosition() { return mCash; }


	private HoldingEntry getHoldingPositionEntry( String pSid ) {
		HoldingEntry tHolding = mPosition.get(pSid);
		if (tHolding == null) {
			tHolding = new HoldingEntry(pSid, 0L, 0L);
			mPosition.put(pSid, tHolding);
		}
		return tHolding;
	}

	 List<HoldingEntry> getHoldings() {
		return mPosition.values().stream().toList();
	}


	 String getAccount() {
		return mAccount;
	}

	 long getHolding( String pSid ) {
		HoldingEntry tHoldingEntry = getHoldingPositionEntry(pSid);
		return tHoldingEntry.getHolding();
	}

	 void updatePosition( String pSid, long pDeltaPosition, long pTxNo ) {
		HoldingEntry tHolding = getHoldingPositionEntry( pSid );
		tHolding.updateHolding( pDeltaPosition, pTxNo );
	}

	 void updatePosition( String pSid, long pDeltaPosition ) {
		HoldingEntry tHolding = getHoldingPositionEntry( pSid );
		tHolding.updateHolding( pDeltaPosition, 0L );
	}

	 void setPosition( String pSid, long pHolding ) {
		HoldingEntry tHolding = getHoldingPositionEntry(pSid);
		tHolding.setHolding(pHolding, 0);
	}


	 boolean validateBuyOrder(Order pOrder, long pExposure ) {
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

	 boolean validateSellOrder( Order pOrder, int pMarketPosition ) {
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

	void execution(String pSid, Order.Side pSide, long pPrice, long pQuantity, long pTeTradeTxid)
	{
		long tQuantityChange = (pSide == Order.Side.BUY) ? pQuantity : (-1L * pQuantity);


		HoldingEntry tHolding = getHoldingPositionEntry( pSid );
		if (tHolding.getTxNo() < pTeTradeTxid) {
			mCash += (pSide == Order.Side.BUY) ? (-1L * pPrice * pQuantity) : (pPrice * pQuantity);
			tHolding.updateHolding(tQuantityChange, pTeTradeTxid );
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

	public QueryPositionResponse toQueryPositionResponse( String pRef ) {
		 QueryPositionResponse tQryPosResp = new QueryPositionResponse().setRef( pRef );
		 tQryPosResp.setCash( mCash );
		 if (mPosition != null) {
			 for (HoldingEntry tHoldingEntry : mPosition.values()) {
				 tQryPosResp.addPositions(new Position().setSid(tHoldingEntry.getSid()).setPosition(tHoldingEntry.getHolding()));
			 }
		 } else {
			 tQryPosResp.setPositions( new ArrayList<>());
		 }
		 return tQryPosResp;
	}


}

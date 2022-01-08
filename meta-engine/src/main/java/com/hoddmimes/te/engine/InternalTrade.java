/*
 * Copyright (c)  Hoddmimes Solution AB 2021.
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

package com.hoddmimes.te.engine;

import com.hoddmimes.te.common.TXIDFactory;
import com.hoddmimes.te.messages.generated.BdxOwnTrade;
import com.hoddmimes.te.messages.generated.BdxTrade;

import java.text.SimpleDateFormat;

public class InternalTrade
{
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private String      mSid;
    private int         mMarketId;
    private long        mTradeTime;
    private long        mTradeNo;
    private long        mPrice;
    private int         mQuantity;
    private Order       mBuyOrder;
    private Order       mSellOrder;


    public InternalTrade(String pSid, int pMarketId, long pPrice, int pQuantity, Order pOrder1, Order pOrder2 ) {
        mSid = pSid;
        mMarketId = pMarketId;
        mTradeTime = System.currentTimeMillis();
        mTradeNo = TXIDFactory.getId();
        mPrice = pPrice;
        mQuantity = pQuantity;
        mBuyOrder = (pOrder1.getSide() == Order.Side.BUY) ? pOrder1 : pOrder2;
        mSellOrder = (pOrder1.getSide() == Order.Side.SELL) ? pOrder1 : pOrder2;
    }

    @Override
    public String toString() {
        return "Inst: " + getBuyOrder().getSid() + " price: " + getPrice() + " volume: " + getQuantity() + " time: " + SDF.format(getTradeTime()) +
                " trdno: " + Long.toHexString(getTradeNo()) + " buyorder: " + Long.toHexString(getBuyOrder().getOrderId()) +
                " sellorder: " + Long.toHexString(getSellOrder().getOrderId()) + " buyref: " + getBuyOrder().getUserRef() + " sellref: " + getSellOrder().getUserRef();
    }

    public  int getMarketId() {
        return mMarketId;
    }

    public boolean isOnSellSide( String pUserId ) {
        return pUserId.contentEquals(this.getSellOrder().getAccountId());
    }

    public boolean isOnBuySide( String pUserId ) {
        return pUserId.contentEquals(this.getBuyOrder().getAccountId());
    }

    public BdxOwnTrade toOwnBdxTrade( Order.Side pSide) {
        BdxOwnTrade tBdx = new BdxOwnTrade();
        tBdx.setSid(this.getBuyOrder().getSid());
        if (pSide == Order.Side.BUY) {
            tBdx.setOrderId(Long.toHexString(this.getBuyOrder().getOrderId()));
            tBdx.setSide( Order.Side.BUY.name());
            tBdx.setOrderRef( this.getBuyOrder().getUserRef());
        }
        if (pSide == Order.Side.SELL) {
            tBdx.setOrderId(Long.toHexString(this.getSellOrder().getOrderId()));
            tBdx.setSide( Order.Side.SELL.name());
            tBdx.setOrderRef( this.getSellOrder().getUserRef());
        }
        tBdx.setTradeId( String.valueOf(this.getTradeNo()));
        tBdx.setPrice(this.getPrice());
        tBdx.setQuantity(this.getQuantity());
        tBdx.setTime( SDF.format( this.getTradeTime()));

        return tBdx;
    }


    public String getSid() {
        return mSid;
    }

    public long getTradeTime() {
        return mTradeTime;
    }

    public long getTradeNo() {
        return mTradeNo;
    }

    public long getPrice() {
        return mPrice;
    }

    public int getQuantity() {
        return mQuantity;
    }

    public Order getBuyOrder() {
        return mBuyOrder;
    }

    public Order getSellOrder() {
        return mSellOrder;
    }
}

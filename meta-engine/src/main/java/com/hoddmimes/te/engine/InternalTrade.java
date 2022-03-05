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

import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.JsonDecoder;
import com.hoddmimes.jsontransform.JsonEncoder;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.TXIDFactory;
import com.hoddmimes.te.messages.generated.BdxOwnTrade;
import com.hoddmimes.te.messages.generated.TradeExecution;

import java.text.SimpleDateFormat;

public class InternalTrade implements MessageInterface
{
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private String      mSid;
    private int         mMarketId;
    private long        mTradeTime;
    private long        mTradeNo;
    private long        mPrice;
    private long        mQuantity;
    private Order       mBuyOrder;
    private Order       mSellOrder;




    public InternalTrade(String pSid, int pMarketId, long pPrice, long pQuantity, Order pOrder1, Order pOrder2 ) {
        mSid = pSid;
        mMarketId = pMarketId;
        mTradeTime = System.currentTimeMillis();
        mTradeNo = TXIDFactory.getId();
        mPrice = pPrice;
        mQuantity = pQuantity;
        mBuyOrder = (pOrder1.getSide() == Order.Side.BUY) ? pOrder1 : pOrder2;
        mSellOrder = (pOrder1.getSide() == Order.Side.SELL) ? pOrder1 : pOrder2;
    }

    public InternalTrade( String pJsonString ) {
        JsonDecoder tDecoder = new JsonDecoder( pJsonString );
        this.decode( tDecoder );
    }

    @Override
    public String getMessageName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void encode(JsonEncoder pJsonEncoder) {
        pJsonEncoder.add("sid", mSid);
        pJsonEncoder.add("marketId", mMarketId);
        pJsonEncoder.add("tradeTime", mTradeTime);
        pJsonEncoder.add("tradeNo", mTradeNo);
        pJsonEncoder.add("price", mPrice);
        pJsonEncoder.add("quantity", mQuantity);
        pJsonEncoder.add("buyOrder", mBuyOrder);
        pJsonEncoder.add( "sellOrder", mSellOrder);
    }

    @Override
    public void decode(JsonDecoder pJsonDecoder) {
        mSid = pJsonDecoder.readString("sid");
        mMarketId = pJsonDecoder.readInteger("marketId");
        mTradeTime = pJsonDecoder.readLong("tradeTime");
        mTradeNo = pJsonDecoder.readLong("tradeNo");
        mPrice = pJsonDecoder.readLong("price");
        mQuantity = pJsonDecoder.readLong("quantity");
        mBuyOrder = (Order) pJsonDecoder.readMessage("buyOrder", Order.class);
        mSellOrder = (Order) pJsonDecoder.readMessage("sellOrder", Order.class);
    }

    public JsonObject toJson() {
        JsonEncoder tEncoder = new JsonEncoder();
        this.encode( tEncoder );
        return tEncoder.toJson();
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

    public long getQuantity() {
        return mQuantity;
    }

    public Order getBuyOrder() {
        return mBuyOrder;
    }

    public Order getSellOrder() {
        return mSellOrder;
    }

    public long getTeBuySeqno() {return (mTradeNo << 1);}
    public long getTeSellSeqno() {
        return (mTradeNo << 1) + 1;
    }

    public TradeExecution toTradeExecution() {
        TradeExecution te = new TradeExecution();
        te.setSid( mSid );
        te.setMarketId( mMarketId );
        te.setBuyer(  mBuyOrder.getAccountId() );
        te.setSeller( mSellOrder.getAccountId() );
        te.setBuyerOrderId( mBuyOrder.getOrderId());
        te.setSellerOrderId(mSellOrder.getOrderId());
        te.setBuyerOrderRef(mBuyOrder.getUserRef());
        te.setSellerOrderRef(mSellOrder.getUserRef());
        te.setPrice( mPrice );
        te.setQuantity( mQuantity);
        te.setTradeId( mTradeNo);
        te.setTradeTime( mTradeTime);
        return te;
    }
}

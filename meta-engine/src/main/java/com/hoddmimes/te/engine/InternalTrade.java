package com.hoddmimes.te.engine;

import com.hoddmimes.te.common.TXIDFactory;
import com.hoddmimes.te.messages.generated.BdxOwnTrade;
import com.hoddmimes.te.messages.generated.BdxTrade;

import java.text.SimpleDateFormat;

public class InternalTrade
{
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private String      mSymbol;
    private long        mTradeTime;
    private long        mTradeNo;
    private double      mPrice;
    private int         mQuantity;
    private Order       mBuyOrder;
    private Order       mSellOrder;


    public InternalTrade(double pPrice, int pQuantity, Order pOrder1, Order pOrder2 ) {
        mTradeTime = System.currentTimeMillis();
        mTradeNo = TXIDFactory.getId();
        mPrice = pPrice;
        mQuantity = pQuantity;
        mBuyOrder = (pOrder1.getSide() == Order.Side.BUY) ? pOrder1 : pOrder2;
        mSellOrder = (pOrder1.getSide() == Order.Side.SELL) ? pOrder1 : pOrder2;
        mSymbol = pOrder1.getSymbol();
    }

    @Override
    public String toString() {
        return "Inst: " + getBuyOrder().getSymbol() + " price: " + getPrice() + " volume: " + getQuantity() + " time: " + SDF.format(getTradeTime()) +
                " trdno: " + Long.toHexString(getTradeNo()) + " buyorder: " + Long.toHexString(getBuyOrder().getOrderId()) +
                " sellorder: " + Long.toHexString(getSellOrder().getOrderId()) + " buyref: " + getBuyOrder().getUserRef() + " sellref: " + getSellOrder().getUserRef();
    }

    public boolean isOnSellSide( String pUserId ) {
        return pUserId.contentEquals(this.getSellOrder().getAccountId());
    }

    public boolean isOnBuySide( String pUserId ) {
        return pUserId.contentEquals(this.getBuyOrder().getAccountId());
    }

    public BdxOwnTrade toOwnBdxTrade( Order.Side pSide) {
        BdxOwnTrade tBdx = new BdxOwnTrade();
        tBdx.setSymbol(this.getBuyOrder().getSymbol());
        if (pSide == Order.Side.BUY) {
            tBdx.setOrderId(Long.toHexString(this.getBuyOrder().getOrderId()));
            tBdx.setRef(this.getBuyOrder().getUserRef());
            tBdx.setSide( Order.Side.BUY.name());
        }
        if (pSide == Order.Side.SELL) {
            tBdx.setRef(Long.toHexString(this.getSellOrder().getOrderId()));
            tBdx.setRef(this.getSellOrder().getUserRef());
            tBdx.setSide( Order.Side.SELL.name());
        }
        tBdx.setTradeId( String.valueOf(this.getTradeNo()));
        tBdx.setPrice(this.getPrice());
        tBdx.setVolume(this.getQuantity());

        return tBdx;
    }

    public BdxTrade toBdxTrade() {
        BdxTrade tBdx = new BdxTrade();
        tBdx.setSymbol(this.getBuyOrder().getSymbol());
        tBdx.setBuyOrderId( Long.toHexString(this.getBuyOrder().getOrderId()));
        tBdx.setSellOrderId( Long.toHexString(this.getSellOrder().getOrderId()));
        tBdx.setPrice(this.getPrice());
        tBdx.setVolume(this.getQuantity());
        return tBdx;
    }

    public String getSymbol() {
        return mSymbol;
    }

    public long getTradeTime() {
        return mTradeTime;
    }

    public long getTradeNo() {
        return mTradeNo;
    }

    public double getPrice() {
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

package com.hoddmimes.te.engine;

import com.hoddmimes.te.common.TXIDFactory;
import com.hoddmimes.te.messages.generated.BdxTrade;

import java.text.SimpleDateFormat;

public class Trade
{
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    long        mTradeTime;
    long        mTradeNo;
    double      mPrice;
    int         mVolume;
    Order       mBuyOrder;
    Order       mSellOrder;


    public Trade(double pPrice, int pVolume, Order pOrder1, Order pOrder2 ) {
        mTradeTime = System.currentTimeMillis();
        mTradeNo = TXIDFactory.getId();
        mPrice = pPrice;
        mVolume = pVolume;
        mBuyOrder = (pOrder1.mSide == Order.Side.BUY) ? pOrder1 : pOrder2;
        mSellOrder = (pOrder1.mSide == Order.Side.SELL) ? pOrder1 : pOrder2;
    }

    @Override
    public String toString() {
        return "Inst: " + mBuyOrder.mSymbol + " price: " + mPrice + " volume: " + mVolume + " time: " + SDF.format( mTradeTime ) +
                " trdno: " + Long.toHexString( mTradeNo) + " buyorder: " + Long.toHexString( mBuyOrder.mOrderId ) +
                " sellorder: " + Long.toHexString( mSellOrder.mOrderId ) + " buyref: " + mBuyOrder.mUserRef + " sellref: " + mSellOrder.mUserRef;
    }


    public BdxTrade toBdxBuyTrade() {
        BdxTrade tBdx = new BdxTrade();
        tBdx.setSymbol( this.mBuyOrder.mSymbol );
        tBdx.setOrderId( Long.toHexString(this.mBuyOrder.mOrderId));
        tBdx.setPrice( this.mPrice );
        tBdx.setVolume( this.mVolume );
        return tBdx;
    }

    public BdxTrade toBdxSellTrade() {
        BdxTrade tBdx = new BdxTrade();
        tBdx.setSymbol( this.mSellOrder.mSymbol );
        tBdx.setOrderId( Long.toHexString(this.mSellOrder.mOrderId));
        tBdx.setPrice( this.mPrice );
        tBdx.setVolume( this.mVolume );
        return tBdx;
    }
}

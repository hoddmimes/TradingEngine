package com.hoddmimes.te.engine;


import com.hoddmimes.jsontransform.MessageInterface;

import com.hoddmimes.te.instrumentctl.Symbol;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.AddOrderResponse;
import com.hoddmimes.te.messages.generated.BdxPriceLevel;
import com.hoddmimes.te.messages.generated.DeleteOrderResponse;
import com.hoddmimes.te.messages.generated.PriceLevel;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Orderbook
{
    private Logger                              mLog;
    private Symbol                              mSymbol;
    private TreeMap<Double,LinkedList<Order>>   mBuySide;
    private TreeMap<Double,LinkedList<Order>>   mSellSide;
    private EngineInterface                     mEngineCallbackIf;
    private double                              mLastMatchPrice;
    private long                                mSeqNo;
    private BdxPriceLevel                       mPriceLevels;

    public Orderbook(Symbol pSymbol, Logger pLogger, EngineInterface pEngineCallbackIf ) {
        mSeqNo = 0;
        mSymbol = pSymbol;
        mBuySide = new TreeMap<>();
        mSellSide = new TreeMap<>();
        mLog = pLogger;
        mLastMatchPrice = 0.0d;
        mEngineCallbackIf = pEngineCallbackIf;
        mPriceLevels = null;
    }


    public  MessageInterface addOrder(Order pOrder, MeRqstCntx pRqstCntx) {
        try{ validateOrder( pOrder ); }
        catch( Exception e ) {
            return StatusMessageBuilder.error("invalid order", pOrder.mUserRef, e);
        }

        if (pOrder.mSide == Order.Side.BUY) {
            return matchBook( mSellSide, pOrder, pRqstCntx);
        } else {
            return matchBook( mBuySide, pOrder, pRqstCntx);
        }
    }



    public  MessageInterface deleteOrder(String pRqstRef, long pOrderId, MeRqstCntx pRqstCntx) {
        Order tRemovedOrder = null;


        Iterator<LinkedList<Order>> tOrderListItr = mBuySide.values().iterator();
        while( tOrderListItr.hasNext() && (tRemovedOrder == null)) {
            LinkedList<Order> tOrderList = tOrderListItr.next();
            Iterator<Order> tItr = tOrderList.iterator();
            while( tItr.hasNext() ) {
                Order tOrder = tItr.next();
                if (tOrder.mOrderId == pOrderId) {
                    tRemovedOrder = tOrder;
                    tItr.remove();
                    mEngineCallbackIf.orderbookChanged( mSymbol.getId() );
                    if (tOrderList.size() == 0) {
                        tOrderListItr.remove();
                    }
                    break;
                }
            }
        }

        tOrderListItr = mSellSide.values().iterator();
        while( tOrderListItr.hasNext() && (tRemovedOrder == null)) {
            LinkedList<Order> tOrderList = tOrderListItr.next();
            Iterator<Order> tItr = tOrderList.iterator();
            while( tItr.hasNext() ) {
                Order tOrder = tItr.next();
                if (tOrder.mOrderId == pOrderId) {
                    tRemovedOrder = tOrder;
                    tItr.remove();
                    mEngineCallbackIf.orderbookChanged( mSymbol.getId() );
                    if (tOrderList.size() == 0) {
                        tOrderListItr.remove();
                    }
                    break;
                }
            }
        }


        pRqstCntx.timestamp("remove deleted order from book");
        if (tRemovedOrder != null) {
            mEngineCallbackIf.orderRemoved( tRemovedOrder, pRqstCntx, ++mSeqNo );
            DeleteOrderResponse tResponse = new DeleteOrderResponse();
            tResponse.setOrderId( Long.toHexString(tRemovedOrder.mOrderId));
            tResponse.setRef(pRqstRef);
            tResponse.setRemaining( tRemovedOrder.mVolume );
            pRqstCntx.timestamp("build delete order success response");
            return tResponse;
        } else {
            MessageInterface tMsg = StatusMessageBuilder.error("Order not deleted, order " + pOrderId +  " not found", pRqstRef);
            pRqstCntx.timestamp("buld delete order failure, not found");
            return tMsg;
        }
    }


    private void addOrderToBook( Order pOrder ) {
        TreeMap<Double,LinkedList<Order>> tBookSide = (pOrder.mSide == Order.Side.BUY) ? mBuySide : mSellSide;
        LinkedList<Order> tOrderList = tBookSide.get( pOrder.mPrice );
        if (tOrderList == null) {
            tOrderList = new LinkedList<>();
            tBookSide.put(pOrder.mPrice, tOrderList);
        }
    }



    private MessageInterface matchBook(TreeMap<Double,LinkedList<Order>> pBook, Order pNewOrder, MeRqstCntx pRqstCntx ) {
        int tTotMatched = 0;
        boolean tEndOfMatch = false;


        // Loop as long as we can match
        pRqstCntx.timestamp("Starting matching order");
        Iterator<LinkedList<Order>> tOrderListItr = pBook.values().iterator();

        while(tOrderListItr.hasNext() && (pNewOrder.mVolume > 0) && (!tEndOfMatch)) {
            LinkedList<Order> tOrderList = tOrderListItr.next();
            Iterator<Order> tItr = tOrderList.iterator();
            while (tItr.hasNext()) {
                Order tBookOrder = tItr.next();
                pRqstCntx.timestamp("find orderbook");
                if (pNewOrder.match(tBookOrder)) {
                    pRqstCntx.timestamp("match orderbook");
                    int tMatchedSize = Math.min(tBookOrder.mVolume, pNewOrder.mVolume);
                    mEngineCallbackIf.trade(new Trade(tBookOrder.mPrice, tMatchedSize, pNewOrder, tBookOrder), pRqstCntx);
                    pRqstCntx.timestamp("create and process trade");
                    tBookOrder.mVolume -= tMatchedSize;
                    pNewOrder.mVolume -= tMatchedSize;
                    tTotMatched += tMatchedSize;
                    if (tBookOrder.mVolume == 0) {
                        mEngineCallbackIf.orderRemoved(tBookOrder,  pRqstCntx, ++mSeqNo);
                        tItr.remove();
                        if (tOrderList.isEmpty()) {
                            tOrderListItr.remove();  // remove prece level
                        }
                        pRqstCntx.timestamp("remove matched order");
                    }
                } else {
                    // Order does not match what is in the book
                    tEndOfMatch = true;
                    break;
                }
            }
        }

        // If remaining volume after match insert order in the book
        if (pNewOrder.mVolume > 0) {
            mEngineCallbackIf.orderAdded( pNewOrder, pRqstCntx, ++mSeqNo );
            pRqstCntx.timestamp("insert new order");
        } else {
            mEngineCallbackIf.newOrderMatched( pNewOrder, pRqstCntx, mSeqNo );
            pRqstCntx.timestamp("new order matched");
        }

        mEngineCallbackIf.orderbookChanged( mSymbol.getId() );

        AddOrderResponse tResponse = new AddOrderResponse();
        tResponse.setInserted( (pNewOrder.mVolume > 0) ? true : false );
        tResponse.setMatched( tTotMatched );
        tResponse.setOrderId( Long.toHexString(pNewOrder.mOrderId));
        tResponse.setRef(pNewOrder.mUserRef );
        pRqstCntx.timestamp("build order response");
        return tResponse;
    }

    public BdxPriceLevel buildPriceLevelBdx( int pLevels ) {
        BdxPriceLevel tBdx = new BdxPriceLevel();
        tBdx.setLevels( pLevels );
        tBdx.setSymbol( mSymbol.getId());
        tBdx.addBuySide( buildPriceLevelSide( this.mBuySide, pLevels ));
        tBdx.addSellSide( buildPriceLevelSide( this.mSellSide, pLevels ));
        return tBdx;
    }

    private List<PriceLevel> buildPriceLevelSide(TreeMap<Double,LinkedList<Order>> pSide, int pMaxLevels ) {
        int i = 0;
        ArrayList<PriceLevel> tPriceLevelList = new ArrayList<>();
        Iterator<Map.Entry<Double,LinkedList<Order>>> tItrLevels = pSide.entrySet().stream().iterator();
        while( (tItrLevels.hasNext()) && (i < pMaxLevels)) {
            Map.Entry<Double,LinkedList<Order>> tPriceEntry = tItrLevels.next();
            List<Order> tOrderList = tPriceEntry.getValue();
            int tVolume = 0;
            for( Order tOrder : tOrderList) {
                tVolume += tOrder.mVolume;
            }
            PriceLevel pl = new PriceLevel().setPrice(tPriceEntry.getKey()).setVolume( tVolume );
            tPriceLevelList.add( pl );
            i++;
        }
        return tPriceLevelList;
    }



    private void validateOrder(Order pNewOrder  ) throws Exception {
        // Validate Order Price
        mSymbol.validate( pNewOrder.mPrice, mLastMatchPrice );
    }


}

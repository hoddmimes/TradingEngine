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
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.interfaces.RequestContextInterface;
import com.hoddmimes.te.instrumentctl.SymbolX;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Orderbook
{
    private Logger                              mLog;
    private SymbolX                             mSymbol;
    private OrderMapInterface                   mBuySide;
    private OrderMapInterface                   mSellSide;
    private MatchingEngineCallback              mEngineCallbackIf;
    private long                                mLastTradePrice;
    private long                                mLastTradeTime;
    private long                                mSeqNo;
    private BdxBBO                              mBBO;
    private int                                 mOrderCount;
    private int                                 mExecutionCount;



    public Orderbook(SymbolX pSymbol, Logger pLogger, MatchingEngineCallback pEngineCallbackIf ) {
        mSeqNo = 0;
        mOrderCount = 0;
        mExecutionCount = 0;

        mSymbol = pSymbol;
        mBuySide = new OrderMap( Order.Side.BUY );
        mSellSide = new OrderMap( Order.Side.SELL );
        mLog = pLogger;
        mLastTradePrice = 0L;
        mEngineCallbackIf = pEngineCallbackIf;
        mBBO = new BdxBBO().setSid( mSymbol.getSid().get());
    }

    public int getMarketId() {
        return mSymbol.getMarketId().get();
    }

    public  MessageInterface addOrder(Order pOrder, RequestContextInterface  pRqstCntx) {
        try{ validateOrder( pOrder ); }
        catch( Exception e ) {
            return StatusMessageBuilder.error("invalid order", pOrder.getUserRef(), e);
        }
        pRqstCntx.timestamp("validate order");

        if (pOrder.getSide() == Order.Side.BUY) {
            return matchBook( mSellSide, pOrder, pRqstCntx);
        } else {
            return matchBook( mBuySide, pOrder, pRqstCntx);
        }

    }


    public int getAccountPosition( String pAccount, Order.Side pSide ) {
        int mPosition = 0;
        Order tOrder = null;

        Iterator<Order> tItr = (pSide == Order.Side.BUY) ? mBuySide.iterator() : mSellSide.iterator();
        while(tItr.hasNext()) {
            tOrder = tItr.next();
            if (tOrder.getAccountId().contentEquals(pAccount)) {
                mPosition += tOrder.getQuantity();
            }
        }
        return mPosition;
    }

    public int getAccountPositionExposure( String pAccount, Order.Side pSide ) {
        int tPositionExposure = 0;
        Order tOrder = null;

        Iterator<Order> tItr = (pSide == Order.Side.BUY) ? mBuySide.iterator() : mSellSide.iterator();
        while(tItr.hasNext()) {
            tOrder = tItr.next();
            if (tOrder.getAccountId().contentEquals(pAccount)) {
                tPositionExposure += tOrder.getQuantity();
            }
        }
        return tPositionExposure;
    }

    public int getAccountExposure( String pAccount, Order.Side pSide ) {
        int mExposure = 0;
        Order tOrder = null;

        Iterator<Order> tItr = (pSide == Order.Side.BUY) ? mBuySide.iterator() : mSellSide.iterator();
        while(tItr.hasNext()) {
            tOrder = tItr.next();
            if (tOrder.getAccountId().contentEquals(pAccount)) {
                mExposure += (tOrder.getQuantity() * tOrder.getPrice());
            }
        }
        return mExposure;
    }

    public void updateBBO() {
        Iterator<Order> tItr = null;
        BdxBBO bbo = new BdxBBO().setSid( mSymbol.getSid().get());
        if (mBuySide.size() > 0) {
            long tPrice = mBuySide.peek().getPrice();
            bbo.setBid( tPrice );
            long tBuyQty = mBuySide.getOrders().stream().filter( o -> (o.getPrice() == tPrice)).mapToLong( o -> o.getQuantity()).sum();
            bbo.setBidQty(tBuyQty);
        }
        if (mSellSide.size() > 0) {
            long tPrice = mSellSide.peek().getPrice();
            bbo.setOffer( tPrice );
            long tSellQty = mSellSide.getOrders().stream().filter( o -> (o.getPrice() == tPrice)).mapToLong( o -> o.getQuantity()).sum();
            bbo.setOfferQty(tSellQty);
        }
        if (!mBBO.same( bbo )) {
            mBBO = bbo;
            TeAppCntx.getMarketDistributor().queueBdxPublic( mBBO );
        }
    }

    public String getSymbolId() {
        return mSymbol.getSid().get();
    }

    public MessageInterface amendOrder( long pOrderId, AmendOrderRequest pAmendRqst, RequestContextInterface pRqstCntx ) {
        AmendOrderResponse tAmendRsp;
        // Check that there is something to amend
        if (pAmendRqst.getSide().isEmpty() && pAmendRqst.getPrice().isEmpty() && pAmendRqst.getDeltaQuantity().isEmpty()) {
            return StatusMessageBuilder.error("nothing to amend", pAmendRqst.getRef().get());
        }

        // Check that the order exists
        Order tOrderToAmend = findOrder(pOrderId);
        if (tOrderToAmend == null) {
            return StatusMessageBuilder.error("order not found", pAmendRqst.getRef().get());
        }

        boolean tCleanAmend = true;
        // Only decreasing the volume will performe a clean amend, otherwise
        // the order is delete and a new one is created
        if ((!pAmendRqst.getSide().isEmpty()) && (!pAmendRqst.getSide().get().contentEquals(tOrderToAmend.getSide().name()))) {
            tCleanAmend = false;
        }
        if ((!pAmendRqst.getDeltaQuantity().isEmpty()) && (pAmendRqst.getDeltaQuantity().get() > 0)) {
            tCleanAmend = false;
        }
        if (!pAmendRqst.getPrice().isEmpty()) {
            if ((tOrderToAmend.getSide() == Order.Side.BUY) && (pAmendRqst.getPrice().get() > tOrderToAmend.getPrice())) {
                tCleanAmend = false;
            }
            if ((tOrderToAmend.getSide() == Order.Side.SELL) && (pAmendRqst.getPrice().get() < tOrderToAmend.getPrice())) {
                tCleanAmend = false;
            }
        }

        // If clean amend update the existing order
        if (tCleanAmend) {
            long tNewVolume = (!pAmendRqst.getDeltaQuantity().isEmpty()) ? (tOrderToAmend.getQuantity() + pAmendRqst.getDeltaQuantity().get()) : tOrderToAmend.getQuantity();
            long tNewPrice = (!pAmendRqst.getPrice().isEmpty()) ? (pAmendRqst.getPrice().get()) : tOrderToAmend.getPrice();

            if (tNewVolume <= 0) {
                this.deleteOrder(tOrderToAmend.getOrderId(), pAmendRqst.getRef().get(), pRqstCntx);
                tAmendRsp = new AmendOrderResponse();
                return tAmendRsp.setInserted(false).setOrderId(Long.toHexString(tOrderToAmend.getOrderId())).
                        setRef(pAmendRqst.getRef().get()).setMatched(0L);
            } else {
                tOrderToAmend.setQuantity(tNewVolume);
                if (tOrderToAmend.getPrice() != tNewPrice) {
                    tOrderToAmend.setPrice(tNewPrice);
                    reshuffelOrder(tOrderToAmend);
                }

                mEngineCallbackIf.orderbookChanged(mSymbol.getSid().get());
                mEngineCallbackIf.orderModified(tOrderToAmend, pRqstCntx.getSessionContext(), ++mSeqNo);
                updateBBO();
                tAmendRsp = new AmendOrderResponse();
                return tAmendRsp.setInserted(false).setOrderId(Long.toHexString(tOrderToAmend.getOrderId())).
                        setRef(pAmendRqst.getRef().get()).setMatched(0L);
            }
        }

        // Amend require the existing order to be deleted and possibly a new to be inserted
        long tVolume = (!pAmendRqst.getDeltaQuantity().isEmpty()) ? (tOrderToAmend.getQuantity() + pAmendRqst.getDeltaQuantity().get()) : tOrderToAmend.getQuantity();
        String tSide = (!pAmendRqst.getSide().isEmpty()) ? pAmendRqst.getSide().get() : tOrderToAmend.getSide().name();
        long tPrice = (!pAmendRqst.getPrice().isEmpty()) ? pAmendRqst.getPrice().get() : tOrderToAmend.getPrice();

        // Always delete the existing one
        this.deleteOrder(tOrderToAmend.getOrderId(), pAmendRqst.getRef().get(), pRqstCntx);

        if (tVolume <= 0) {
            tAmendRsp = new AmendOrderResponse();
            return tAmendRsp.setInserted(false).setOrderId(Long.toHexString(tOrderToAmend.getOrderId())).
                    setRef(pAmendRqst.getRef().get()).setMatched(0L);
        } else {
            AddOrderRequest tAddOrderRqst = new AddOrderRequest().setPrice(tPrice).setRef(pAmendRqst.getRef().get()).setQuantity(tVolume).setSid(this.mSymbol.getSid().get()).setSide(tSide);
            Order tNewOrder = new Order(pRqstCntx.getAccountId(), tAddOrderRqst);

            MessageInterface tResponse = this.addOrder(tNewOrder, pRqstCntx);

            if (tResponse instanceof StatusMessage) {
                mLog.warn("failed to amend order when replacing, reason: " + ((StatusMessage) tResponse).getStatusMessage().get());
                return tResponse;
            }
            AddOrderResponse tAddOrderRsp = (AddOrderResponse) tResponse;
            tAmendRsp = new AmendOrderResponse();
            return tAmendRsp.setInserted(tAddOrderRsp.getInserted().get()).setOrderId(tAddOrderRsp.getOrderId().get()).
                    setRef(pAmendRqst.getRef().get()).setMatched(tAddOrderRsp.getMatched().get());
        }
    }

    private void reshuffelOrder( Order pOrder ) {
        Order tOrder = removeOrderFromBook(pOrder.getOrderId() );
        addOrderToBook( tOrder );
    }


    List<OwnOrder> getOrdersForAccount( String pAccount ) {
        List<OwnOrder> tAccountOrders = new ArrayList<>();

        Iterator<Order> tOrderItr = mBuySide.iterator();
        while (tOrderItr.hasNext()) {
            Order tOrder = tOrderItr.next();
            if (tOrder.getAccountId().toUpperCase().contentEquals(pAccount)) {
                tAccountOrders.add(tOrder.toOwnOrder());
            }
        }

        tOrderItr = mSellSide.iterator();
        while (tOrderItr.hasNext()) {
            Order tOrder = tOrderItr.next();
            if (tOrder.getAccountId().toUpperCase().contentEquals(pAccount)) {
                tAccountOrders.add(tOrder.toOwnOrder());
            }
        }

        return tAccountOrders;
    }


    private Order findOrder(long pOrderId) {
        Iterator<Order> tOrderItr = (OrderId.isBuyOrder(pOrderId)) ? mBuySide.iterator() : mSellSide.iterator();

        while (tOrderItr.hasNext()) {
            Order tOrder = tOrderItr.next();
            if (tOrder.getOrderId() == pOrderId) {
                return tOrder;
            }
        }
        return null;
    }


    public MessageInterface queryOwnOrders( String pRef, RequestContextInterface pRqstCntx) {
        InternalOwnOrdersResponse tRspMsg = new InternalOwnOrdersResponse();
        tRspMsg.setOrders( getOrdersForAccount(pRqstCntx.getAccountId()));
        return tRspMsg;
    }



    public BBO getBBO() {
        JsonObject jBBO = AuxJson.getMessageBody( mBBO.toJson() );
        BBO bbo = new BBO( jBBO.toString());
        return bbo;
    }


    public int deleteOrders(  String pAccountId, RequestContextInterface pRqstCntx ) {
         int tOrdersDeleted = 0;
         Order tOrder;

        Iterator<Order> tOrderItr = mBuySide.iterator();
        while (tOrderItr.hasNext()) {
            tOrder = tOrderItr.next();
            if (tOrder.getAccountId().contentEquals( pAccountId )) {
                tOrderItr.remove();
                tOrdersDeleted++;
                mEngineCallbackIf.orderRemoved(tOrder, pRqstCntx.getSessionContext(), ++mSeqNo);
                mEngineCallbackIf.orderbookChanged(mSymbol.getSid().get());
            }
        }

        tOrderItr = mSellSide.iterator();
        while (tOrderItr.hasNext()) {
            tOrder = tOrderItr.next();
            if (tOrder.getAccountId().contentEquals( pAccountId )) {
                tOrderItr.remove();
                tOrdersDeleted++;
                mEngineCallbackIf.orderRemoved(tOrder, pRqstCntx.getSessionContext(), ++mSeqNo);
                mEngineCallbackIf.orderbookChanged(mSymbol.getSid().get());
            }
        }

        updateBBO();
        return tOrdersDeleted;
    }

    private Order removeOrderFromBook(long pOrderId ) {
        return (OrderId.isBuyOrder(pOrderId)) ? mBuySide.remove( pOrderId ) : mSellSide.remove( pOrderId );
    }

    public  MessageInterface deleteOrder(long pOrderId, String pUserRef, RequestContextInterface pRqstCntx) {

        Order tRemovedOrder = removeOrderFromBook( pOrderId );

        pRqstCntx.timestamp("remove deleted order from book");
        if (tRemovedOrder != null) {
            mEngineCallbackIf.orderRemoved( tRemovedOrder, pRqstCntx.getSessionContext(), ++mSeqNo );
            pRqstCntx.timestamp("order remove callback");
            mEngineCallbackIf.orderbookChanged( mSymbol.getSid().get()  );
            pRqstCntx.timestamp("order change callback");
            DeleteOrderResponse tResponse = new DeleteOrderResponse();
            tResponse.setOrderId( Long.toHexString(tRemovedOrder.getOrderId()));
            tResponse.setRef(pUserRef);
            tResponse.setRemaining(tRemovedOrder.getQuantity());
            pRqstCntx.timestamp("build delete order success response");
            updateBBO();
            return tResponse;
        } else {
            MessageInterface tMsg = StatusMessageBuilder.error("Order not deleted, order " + pOrderId +  " not found", pUserRef);
            pRqstCntx.timestamp("build delete order failure, not found");
            return tMsg;
        }
    }


    private void addOrderToBook( Order pOrder ) {
        if (pOrder.getSide() == Order.Side.BUY) {
            mBuySide.add(pOrder);
        } else {
            mSellSide.add(pOrder);
        }
    }

    public InternalTrade revertTrade( TradeExecution pTrade,  RequestContextInterface pRqstCntx ) {
        // Reverse
        Order tBuyOrder = new Order( pTrade.getSeller().get(),
                                     pTrade.getSid().get(),
                                     pTrade.getPrice().get(),
                                     pTrade.getQuantity().get(),
                                     Order.Side.BUY,
                                     "reverse",
                                     System.currentTimeMillis());

        tBuyOrder.setOrderId( pTrade.getSellerOrderId().get());

        Order tSellOrder = new Order( pTrade.getBuyer().get(),
                pTrade.getSid().get(),
                pTrade.getPrice().get(),
                pTrade.getQuantity().get(),
                Order.Side.SELL,
                "reverse",
                System.currentTimeMillis());

        tSellOrder.setOrderId( pTrade.getBuyerOrderId().get());



        InternalTrade tInternalTrade = new InternalTrade(
                pTrade.getSid().get(),
                mSymbol.getMarketId().get(),
                pTrade.getPrice().get(),
                pTrade.getQuantity().get(),
                tBuyOrder, tSellOrder);

        mEngineCallbackIf.trade(tInternalTrade, pRqstCntx.getSessionContext());
        return tInternalTrade;
    }


    private MessageInterface matchBook(OrderMapInterface pBook, Order pNewOrder, RequestContextInterface pRqstCntx ) {
        long tTotMatched = 0;
        boolean tEndOfMatch = false;

        mOrderCount++;

        // Loop as long as we can match
        pRqstCntx.timestamp("Starting matching order");
        Iterator<Order> tOrderItr = pBook.iterator();

        while(tOrderItr.hasNext() && (pNewOrder.getQuantity() > 0) && (!tEndOfMatch)) {
            while (tOrderItr.hasNext()) {
                Order tBookOrder = tOrderItr.next();
                pRqstCntx.timestamp("find price level");
                if (pNewOrder.match(tBookOrder)) {
                    pRqstCntx.timestamp("match first order on price level");
                    long tMatchedSize = Math.min(tBookOrder.getQuantity(), pNewOrder.getQuantity());

                    mLastTradePrice = tBookOrder.getPrice();
                    mLastTradeTime = System.currentTimeMillis();

                    mEngineCallbackIf.trade(new InternalTrade( mSymbol.getSid().get() , mSymbol.getMarketId().get(), tBookOrder.getPrice(), tMatchedSize, pNewOrder, tBookOrder), pRqstCntx.getSessionContext());
                    pRqstCntx.timestamp("create and process trade");

                    tBookOrder.setQuantity(tBookOrder.getQuantity() - tMatchedSize);
                    pNewOrder.setQuantity(pNewOrder.getQuantity() - tMatchedSize);
                    tTotMatched += tMatchedSize;

                    mExecutionCount++;

                    if (tBookOrder.getQuantity() == 0) {
                        mEngineCallbackIf.orderRemoved(tBookOrder,  pRqstCntx.getSessionContext(), ++mSeqNo);
                        tOrderItr.remove();
                        pRqstCntx.timestamp("remove matched order");
                    }
                } else {
                    // Order does not match against the book
                    tEndOfMatch = true;
                    break;
                }
            }
        }

        // If remaining volume after match insert order in the book
        if (pNewOrder.getQuantity() > 0) {
            addOrderToBook( pNewOrder );
            mEngineCallbackIf.orderAdded( pNewOrder, pRqstCntx.getSessionContext(), ++mSeqNo );
            pRqstCntx.timestamp("insert new order");
        } else {
            mEngineCallbackIf.newOrderMatched( pNewOrder, pRqstCntx.getSessionContext(), mSeqNo );
            pRqstCntx.timestamp("new order matched");
        }

        mEngineCallbackIf.orderbookChanged( mSymbol.getSid().get()  );
        pRqstCntx.timestamp("orderbook change callback");
        updateBBO();
        pRqstCntx.timestamp("update BBO");

        AddOrderResponse tResponse = new AddOrderResponse();
        tResponse.setInserted( (pNewOrder.getQuantity() > 0) ? true : false );
        tResponse.setMatched( tTotMatched );
        tResponse.setOrderId( Long.toHexString(pNewOrder.getOrderId()));
        tResponse.setRef(pNewOrder.getUserRef());
        pRqstCntx.timestamp("build order response (Add Order Complete)");
        return tResponse;
    }

    public BdxPriceLevel buildPriceLevelBdx( int pLevels ) {
        BdxPriceLevel tBdx = new BdxPriceLevel();
        tBdx.setLevels( pLevels );
        tBdx.setSid(mSymbol.getSid().get() );
        tBdx.addBuySide( buildPriceLevelSide( this.mBuySide, pLevels ));
        tBdx.addSellSide( buildPriceLevelSide( this.mSellSide, pLevels ));
        return tBdx;
    }

    private List<PriceLevel> buildPriceLevelSide(OrderMapInterface pSide, int pMaxLevels ) {
        int i = 0;
        ArrayList<PriceLevel> tPriceLevelList = new ArrayList<>();

        if (pSide.size() == 0) {
            return tPriceLevelList;
        }

        int tLevel = 0;
        long tPrice = pSide.peek().getPrice();
        long tQty = 0;
        Iterator<Order> tOrderItr = pSide.iterator();

        while( (tOrderItr.hasNext()) && (tLevel < pMaxLevels)) {
           Order tOrder = tOrderItr.next();
           if (tOrder.getPrice() == tPrice) {
               tQty += tOrder.getQuantity();
           } else {
               // Price level shift
               PriceLevel pl = new PriceLevel().setPrice(tPrice).setQuantity(tQty);
               tPriceLevelList.add(pl);
               tPrice = tOrder.getPrice();
               tQty = tOrder.getQuantity();
               tLevel++;
           }
        }
        return tPriceLevelList;
    }


    public QueryOrderbookResponse orderbookSnapshot( QueryOrderbookRequest pRqst ) {
        QueryOrderbookResponse tResponse = new QueryOrderbookResponse();
        tResponse.setRef( pRqst.getRef().get());
        tResponse.setSid( mSymbol.getSid().get() );
        tResponse.setObSeqNo( this.mSeqNo );


        Iterator<Order> tOrderItr = mBuySide.iterator();
        while ( tOrderItr.hasNext() ) {
                Order o = tOrderItr.next();
                tResponse.addBuyOrders( o.toMsgOrder());
        }
        tOrderItr = mSellSide.iterator();
        while ( tOrderItr.hasNext() ) {
                Order o = tOrderItr.next();;
                tResponse.addSellOrders( o.toMsgOrder());
        }
        return tResponse;
    }

    private void validateOrder(Order pNewOrder  ) throws Exception {
        // Validate Order Price
        mSymbol.validate( pNewOrder.getQuantity(), pNewOrder.getPrice(), mLastTradePrice );
    }

    MgmtSymbolMatcherEntry getStatistics() {
        MgmtSymbolMatcherEntry sme = new MgmtSymbolMatcherEntry();
        sme.setSid( this.mSymbol.getSid().get());
        sme.setBuyOrders( mBuySide.size());
        sme.setSellOrders( mSellSide.size());
        sme.setOrders( mOrderCount );
        sme.setExecutions( mExecutionCount );
        if (mBuySide.size() > 0) {
            sme.setBuyPrice( mBuySide.peek().getPrice());
        }
        if (mSellSide.size() > 0) {
            sme.setSellPrice( mSellSide.peek().getPrice());
        }
        return sme;
    }

    static class PriceComarator implements Comparator<Double>
    {
        private Order.Side mSide;

        PriceComarator( Order.Side pSide) {
            mSide = pSide;
        }

        @Override
        public int compare(Double p1, Double p2) {
            return (mSide == Order.Side.BUY) ? Double.compare(p2,p1) : Double.compare(p1,p2);
        }
    }
}

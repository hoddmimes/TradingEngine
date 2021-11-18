package com.hoddmimes.te.engine;


import com.hoddmimes.jsontransform.MessageInterface;

import com.hoddmimes.te.common.interfaces.RequestContextInterface;
import com.hoddmimes.te.instrumentctl.SymbolX;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Orderbook
{
    private Logger                              mLog;
    private SymbolX mSymbol;
    private TreeMap<Double,LinkedList<Order>>   mBuySide;
    private TreeMap<Double,LinkedList<Order>>   mSellSide;
    private MatchingEngineCallback              mEngineCallbackIf;
    private double                              mLastTradePrice;
    private long                                mLastTradeTime;
    private long                                mSeqNo;
    private BdxPriceLevel                       mPriceLevels;

    public Orderbook(SymbolX pSymbol, Logger pLogger, MatchingEngineCallback pEngineCallbackIf ) {
        mSeqNo = 0;
        mSymbol = pSymbol;
        mBuySide = new TreeMap<>(new PriceComarator( Order.Side.BUY));
        mSellSide = new TreeMap<>(new PriceComarator( Order.Side.SELL));
        mLog = pLogger;
        mLastTradePrice = 0.0d;
        mLastTradePrice = 0L;
        mEngineCallbackIf = pEngineCallbackIf;
        mPriceLevels = null;
    }


    public  MessageInterface addOrder(Order pOrder, RequestContextInterface  pRqstCntx) {
        try{ validateOrder( pOrder ); }
        catch( Exception e ) {
            return StatusMessageBuilder.error("invalid order", pOrder.getUserRef(), e);
        }

        if (pOrder.getSide() == Order.Side.BUY) {
            return matchBook( mSellSide, pOrder, pRqstCntx);
        } else {
            return matchBook( mBuySide, pOrder, pRqstCntx);
        }
    }

    public String getSymbolName() {
        return mSymbol.getId();
    }

    public MessageInterface amendOrder( long pOrderId, AmendOrderRequest pAmendRqst, RequestContextInterface pRqstCntx ) {
        AmendOrderResponse tAmendRsp;
        // Check that there is something to amend
        if (pAmendRqst.getSide().isEmpty() && pAmendRqst.getPrice().isEmpty() && pAmendRqst.getDeltaVolume().isEmpty()) {
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
        if ((!pAmendRqst.getDeltaVolume().isEmpty()) && (pAmendRqst.getDeltaVolume().get() > 0)) {
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

        // If clean ammend update the existing order
        if (tCleanAmend) {
            int tNewVolume = (!pAmendRqst.getDeltaVolume().isEmpty()) ? (tOrderToAmend.getVolume() + pAmendRqst.getDeltaVolume().get()) : tOrderToAmend.getVolume();
            double tNewPrice = (!pAmendRqst.getPrice().isEmpty()) ? (pAmendRqst.getPrice().get()) : tOrderToAmend.getPrice();

            if (tNewVolume <= 0) {
                this.deleteOrder(tOrderToAmend.getOrderId(), pAmendRqst.getRef().get(), pRqstCntx);
                tAmendRsp = new AmendOrderResponse();
                return tAmendRsp.setInserted(false).setOrderId(Long.toHexString(tOrderToAmend.getOrderId())).
                        setRef(pAmendRqst.getRef().get()).setMatched(0);
            } else {
                tOrderToAmend.setVolume(tNewVolume);
                tOrderToAmend.setPrice(tNewPrice);

                mEngineCallbackIf.orderbookChanged(mSymbol.getId());
                mEngineCallbackIf.orderModified(tOrderToAmend, pRqstCntx.getSessionContext(), ++mSeqNo);
                tAmendRsp = new AmendOrderResponse();
                return tAmendRsp.setInserted(false).setOrderId(Long.toHexString(tOrderToAmend.getOrderId())).
                        setRef(pAmendRqst.getRef().get()).setMatched(0);
            }
        }

        // Amend require the existing order to be deleted and possibly a new to be inserted
        int tVolume = (!pAmendRqst.getDeltaVolume().isEmpty()) ? (tOrderToAmend.getVolume() + pAmendRqst.getDeltaVolume().get()) : tOrderToAmend.getVolume();
        String tSide = (!pAmendRqst.getSide().isEmpty()) ? pAmendRqst.getSide().get() : tOrderToAmend.getSide().name();
        double tPrice = (!pAmendRqst.getPrice().isEmpty()) ? pAmendRqst.getPrice().get() : tOrderToAmend.getPrice();

        // Always delete the existing one
        this.deleteOrder(tOrderToAmend.getOrderId(), pAmendRqst.getRef().get(), pRqstCntx);

        if (tVolume <= 0) {
            tAmendRsp = new AmendOrderResponse();
            return tAmendRsp.setInserted(false).setOrderId(Long.toHexString(tOrderToAmend.getOrderId())).
                    setRef(pAmendRqst.getRef().get()).setMatched(0);
        } else {
            AddOrderRequest tAddOrderRqst = new AddOrderRequest().setPrice(tPrice).setRef(pAmendRqst.getRef().get()).setVolume(tVolume).setSymbol(this.mSymbol.getId()).setSide(tSide);
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



    private Order findOrder( long pOrderId ) {

        Iterator<LinkedList<Order>> tOrderListItr = mBuySide.values().iterator();
        while( tOrderListItr.hasNext()) {
            LinkedList<Order> tOrderList = tOrderListItr.next();
            Iterator<Order> tItr = tOrderList.iterator();
            while (tItr.hasNext()) {
                Order tOrder = tItr.next();
                if (tOrder.getOrderId() == pOrderId) {
                    return tOrder;
                }
            }
        }

        tOrderListItr = mSellSide.values().iterator();
        while( tOrderListItr.hasNext()) {
            LinkedList<Order> tOrderList = tOrderListItr.next();
            Iterator<Order> tItr = tOrderList.iterator();
            while (tItr.hasNext()) {
                Order tOrder = tItr.next();
                if (tOrder.getOrderId() == pOrderId) {
                    return tOrder;
                }
            }
        }

        return null;

    }

    public MessageInterface queryOwnOrders( String pRef, RequestContextInterface pRqstCntx) {
        InternalOwnOrdersResponse tRspMsg = new InternalOwnOrdersResponse();

        Iterator<LinkedList<Order>> tOrderListItr = mBuySide.values().iterator();
        while( tOrderListItr.hasNext()) {
            LinkedList<Order> tOrderList = tOrderListItr.next();
            Iterator<Order> tItr = tOrderList.iterator();
            while( tItr.hasNext() ) {
                Order tOrder = tItr.next();
                if (tOrder.getAccountId().contentEquals( pRqstCntx.getAccountId())) {
                    tRspMsg.addOrders( tOrder.toOwnOrder());
                }
            }
        }

        tOrderListItr = mSellSide.values().iterator();
        while( tOrderListItr.hasNext()) {
            LinkedList<Order> tOrderList = tOrderListItr.next();
            Iterator<Order> tItr = tOrderList.iterator();
            while( tItr.hasNext() ) {
                Order tOrder = tItr.next();
                if (tOrder.getAccountId().contentEquals( pRqstCntx.getAccountId())) {
                    tRspMsg.addOrders( tOrder.toOwnOrder());
                }
            }
        }
        return tRspMsg;
    }

    public MessageInterface deleteOrders( String pUserRef, RequestContextInterface pRqstCntx ) {

        Iterator<LinkedList<Order>> tOrderListItr = mBuySide.values().iterator();
        while( tOrderListItr.hasNext()) {
            LinkedList<Order> tOrderList = tOrderListItr.next();
            Iterator<Order> tItr = tOrderList.iterator();
            while( tItr.hasNext() ) {
                Order tOrder = tItr.next();
                if (tOrder.getAccountId().contentEquals( pRqstCntx.getAccountId())) {
                    tItr.remove();
                    mEngineCallbackIf.orderRemoved( tOrder, pRqstCntx.getSessionContext(), ++mSeqNo );
                    mEngineCallbackIf.orderbookChanged( mSymbol.getId() );
                    if (tOrderList.size() == 0) {
                        tOrderListItr.remove();
                    }
                }
            }
        }

        tOrderListItr = mSellSide.values().iterator();
        while( tOrderListItr.hasNext()) {
            LinkedList<Order> tOrderList = tOrderListItr.next();
            Iterator<Order> tItr = tOrderList.iterator();
            while( tItr.hasNext() ) {
                Order tOrder = tItr.next();
                if (tOrder.getAccountId().contentEquals( pRqstCntx.getAccountId())) {
                    tItr.remove();
                    mEngineCallbackIf.orderRemoved( tOrder, pRqstCntx.getSessionContext(), ++mSeqNo );
                    mEngineCallbackIf.orderbookChanged( mSymbol.getId() );
                    if (tOrderList.size() == 0) {
                        tOrderListItr.remove();
                    }
                }
            }
        }

        return StatusMessageBuilder.success( pUserRef );
    }

    public  MessageInterface deleteOrder(long pOrderId, String pUserRef, RequestContextInterface pRqstCntx) {
        Order tRemovedOrder = null;

        Iterator<LinkedList<Order>> tOrderListItr = mBuySide.values().iterator();
        while( tOrderListItr.hasNext() && (tRemovedOrder == null)) {
            LinkedList<Order> tOrderList = tOrderListItr.next();
            Iterator<Order> tItr = tOrderList.iterator();
            while( tItr.hasNext() ) {
                Order tOrder = tItr.next();
                if (tOrder.getOrderId() == pOrderId) {
                    tRemovedOrder = tOrder;
                    tItr.remove();
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
                if (tOrder.getOrderId() == pOrderId) {
                    tRemovedOrder = tOrder;
                    tItr.remove();
                    if (tOrderList.size() == 0) {
                        tOrderListItr.remove();
                    }
                    break;
                }
            }
        }


        pRqstCntx.timestamp("remove deleted order from book");
        if (tRemovedOrder != null) {
            mEngineCallbackIf.orderRemoved( tRemovedOrder, pRqstCntx.getSessionContext(), ++mSeqNo );
            mEngineCallbackIf.orderbookChanged( mSymbol.getId() );
            DeleteOrderResponse tResponse = new DeleteOrderResponse();
            tResponse.setOrderId( Long.toHexString(tRemovedOrder.getOrderId()));
            tResponse.setRef(pUserRef);
            tResponse.setRemaining(tRemovedOrder.getVolume());
            pRqstCntx.timestamp("build delete order success response");
            return tResponse;
        } else {
            MessageInterface tMsg = StatusMessageBuilder.error("Order not deleted, order " + pOrderId +  " not found", pUserRef);
            pRqstCntx.timestamp("buld delete order failure, not found");
            return tMsg;
        }
    }


    private void addOrderToBook( Order pOrder ) {
        TreeMap<Double,LinkedList<Order>> tBookSide = (pOrder.getSide() == Order.Side.BUY) ? mBuySide : mSellSide;
        LinkedList<Order> tOrderList = tBookSide.get(pOrder.getPrice());
        if (tOrderList == null) {
            tOrderList = new LinkedList<>();
            tBookSide.put(pOrder.getPrice(), tOrderList);
        }
        tOrderList.add( pOrder );
    }



    private MessageInterface matchBook(TreeMap<Double,LinkedList<Order>> pBook, Order pNewOrder, RequestContextInterface pRqstCntx ) {
        int tTotMatched = 0;
        boolean tEndOfMatch = false;


        // Loop as long as we can match
        pRqstCntx.timestamp("Starting matching order");
        Iterator<LinkedList<Order>> tOrderListItr = pBook.values().iterator();

        while(tOrderListItr.hasNext() && (pNewOrder.getVolume() > 0) && (!tEndOfMatch)) {
            LinkedList<Order> tOrderList = tOrderListItr.next();
            Iterator<Order> tItr = tOrderList.iterator();
            while (tItr.hasNext()) {
                Order tBookOrder = tItr.next();
                pRqstCntx.timestamp("find orderbook");
                if (pNewOrder.match(tBookOrder)) {
                    pRqstCntx.timestamp("match orderbook");
                    int tMatchedSize = Math.min(tBookOrder.getVolume(), pNewOrder.getVolume());
                    mLastTradePrice = tBookOrder.getPrice();
                    mLastTradeTime = System.currentTimeMillis();

                    mEngineCallbackIf.trade(new InternalTrade(tBookOrder.getPrice(), tMatchedSize, pNewOrder, tBookOrder), pRqstCntx.getSessionContext());
                    pRqstCntx.timestamp("create and process trade");

                    tBookOrder.setVolume(tBookOrder.getVolume() - tMatchedSize);
                    pNewOrder.setVolume(pNewOrder.getVolume() - tMatchedSize);
                    tTotMatched += tMatchedSize;

                    if (tBookOrder.getVolume() == 0) {
                        mEngineCallbackIf.orderRemoved(tBookOrder,  pRqstCntx.getSessionContext(), ++mSeqNo);
                        tItr.remove();
                        if (tOrderList.isEmpty()) {
                            tOrderListItr.remove();  // remove prece level
                        }
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
        if (pNewOrder.getVolume() > 0) {
            addOrderToBook( pNewOrder );
            mEngineCallbackIf.orderAdded( pNewOrder, pRqstCntx.getSessionContext(), ++mSeqNo );
            pRqstCntx.timestamp("insert new order");
        } else {
            mEngineCallbackIf.newOrderMatched( pNewOrder, pRqstCntx.getSessionContext(), mSeqNo );
            pRqstCntx.timestamp("new order matched");
        }

        mEngineCallbackIf.orderbookChanged( mSymbol.getId() );

        AddOrderResponse tResponse = new AddOrderResponse();
        tResponse.setInserted( (pNewOrder.getVolume() > 0) ? true : false );
        tResponse.setMatched( tTotMatched );
        tResponse.setOrderId( Long.toHexString(pNewOrder.getOrderId()));
        tResponse.setRef(pNewOrder.getUserRef());
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
                tVolume += tOrder.getVolume();
            }
            PriceLevel pl = new PriceLevel().setPrice(tPriceEntry.getKey()).setVolume( tVolume );
            tPriceLevelList.add( pl );
            i++;
        }
        return tPriceLevelList;
    }


    public QueryOrderbookResponse orderbookSnapshot( QueryOrderbookRequest pRqst ) {
        QueryOrderbookResponse tResponse = new QueryOrderbookResponse();
        tResponse.setRef( pRqst.getRef().get());
        tResponse.setSymbol( mSymbol.getId());
        tResponse.setObSeqNo( this.mSeqNo );


        Iterator<LinkedList<Order>> tItrLevel = mBuySide.values().iterator();
        while ( tItrLevel.hasNext() ) {
            Iterator<Order> tItr = tItrLevel.next().iterator();
            while( tItr.hasNext() ) {
                Order o = tItr.next();;
                tResponse.addBuyOrders( o.toMsgOrder());
            }
        }
        tItrLevel = mSellSide.values().iterator();
        while ( tItrLevel.hasNext() ) {
            Iterator<Order> tItr = tItrLevel.next().iterator();
            while( tItr.hasNext() ) {
                Order o = tItr.next();;
                tResponse.addSellOrders( o.toMsgOrder());
            }
        }
        return tResponse;
    }

    private void validateOrder(Order pNewOrder  ) throws Exception {
        // Validate Order Price
        mSymbol.validate(pNewOrder.getPrice(), mLastTradePrice );
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

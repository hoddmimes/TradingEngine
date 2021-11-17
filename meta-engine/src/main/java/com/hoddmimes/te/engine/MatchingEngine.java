package com.hoddmimes.te.engine;


import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.interfaces.MarketDataInterface;
import com.hoddmimes.te.common.interfaces.RequestContextInterface;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.instrumentctl.InstrumentContainer;
import com.hoddmimes.te.instrumentctl.Symbol;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import com.hoddmimes.te.sessionctl.RequestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MatchingEngine implements MatchingEngineCallback
{
	private static SimpleDateFormat SDT_TIME = new SimpleDateFormat("HH:mm:ss.SSS");

	private Logger mLog = LogManager.getLogger( MatchingEngine.class);
	private InstrumentContainer         mInstrumentContainer;
	private JsonObject                  mConfiguration;
	private MarketDataInterface         mMarketDataDistributor;
	private HashMap<String,Orderbook>   mOrderbooks;


	private boolean mIsEnabledPrivateFlow, mIsEnabledPriceLevelFlow, mIsEnabledTradeflow, mIsEnabledOrderbookChangeFlow;

	public MatchingEngine(JsonObject pTeConfiguration, InstrumentContainer pInstrumentContainer, MarketDataInterface pMarketDataInterface) {
		mInstrumentContainer = pInstrumentContainer;
		mMarketDataDistributor = pMarketDataInterface;
		mConfiguration = AuxJson.navigateObject(pTeConfiguration, "TeConfiguration/matchingEngineConfiguration");
		initializeOrderbooks();
		configureDataDistribution(AuxJson.navigateObject(pTeConfiguration, "TeConfiguration/marketDataConfiguration"));
	}


	private void configureDataDistribution( JsonObject pMarketDataConfiguration ) {
		mIsEnabledOrderbookChangeFlow = AuxJson.navigateBoolean(pMarketDataConfiguration,"enableOrdebookChanges");
		mIsEnabledPriceLevelFlow =  AuxJson.navigateBoolean(pMarketDataConfiguration,"enablePriceLevels");
		mIsEnabledPrivateFlow =  AuxJson.navigateBoolean(pMarketDataConfiguration,"enablePrivateFlow");
		mIsEnabledTradeflow =  AuxJson.navigateBoolean(pMarketDataConfiguration,"enableTradeFlow");
	}


	private void initializeOrderbooks() {
		mOrderbooks = new HashMap<>();
		mInstrumentContainer.getSymbols().stream().forEach( s -> { mOrderbooks.put( s.getId(), new Orderbook(s, mLog, this)); } );
	}

	MessageInterface processAddOrder(AddOrderRequest pAddOrderRequest, RequestContextInterface pRequestContext ) {
		Orderbook tOrderbook = mOrderbooks.get(pAddOrderRequest.getSymbol().get());
		if (tOrderbook == null) {
			mLog.warn("orderbook \"" + pAddOrderRequest.getSymbol().get() + "\" does not exists " + pRequestContext);
			return  StatusMessageBuilder.error("orderbook \"" + pAddOrderRequest.getSymbol().get() + "\" does not exists", pAddOrderRequest.getRef().get());
		}
		synchronized (tOrderbook) {
			Order tOrder = new Order(pRequestContext.getAccountId(), pAddOrderRequest);

			MessageInterface tMsg = tOrderbook.addOrder(tOrder, pRequestContext);
			return tMsg;
		}
	}

	MessageInterface processAmendOrder( AmendOrderRequest pAmendOrderRequest, RequestContextInterface pRequestContext) {
		long tOrderId;

		Orderbook tOrderbook = mOrderbooks.get(pAmendOrderRequest.getSymbol().get());
		if (tOrderbook == null) {
			mLog.warn("orderbook \"" + pAmendOrderRequest.getSymbol().get() + "\" does not exists " + pRequestContext);
			return StatusMessageBuilder.error("orderbook \"" + pAmendOrderRequest.getSymbol().get() + "\" does not exists", pAmendOrderRequest.getRef().get());
		}

		synchronized (tOrderbook) {
			try {
				tOrderId = Long.parseLong(pAmendOrderRequest.getOrderId().get(), 16);
			} catch (NumberFormatException e) {
				mLog.warn("amend order, invalid order id " + pRequestContext);
				return StatusMessageBuilder.error("invalid order id", pAmendOrderRequest.getRef().get());
			}

			MessageInterface tRspMsg = tOrderbook.amendOrder(tOrderId, pAmendOrderRequest, pRequestContext);
			return tRspMsg;
		}
	}

	MessageInterface processDeleteOrder( DeleteOrderRequest pDeleteOrderRequest, RequestContextInterface pRequestContext) {
		long tOrderId;

		Orderbook tOrderbook = mOrderbooks.get( pDeleteOrderRequest.getSymbol().get());
		if (tOrderbook == null) {
			mLog.warn("orderbook \"" + pDeleteOrderRequest.getSymbol().get() + "\" does not exists " + pRequestContext );
			return StatusMessageBuilder.error("orderbook \"" + pDeleteOrderRequest.getSymbol().get() + "\" does not exists", pDeleteOrderRequest.getRef().get());
		}

		synchronized( tOrderbook ) {
			try {
				tOrderId = Long.parseLong(pDeleteOrderRequest.getOrderId().get(), 16);
			} catch (NumberFormatException e) {
				mLog.warn("delete order, invalid order id " + pRequestContext);
				return StatusMessageBuilder.error("invalid order id", pDeleteOrderRequest.getRef().get());
			}

			MessageInterface tMsg = tOrderbook.deleteOrder(tOrderId, pDeleteOrderRequest.getRef().get(), pRequestContext);
			return tMsg;
		}
	}

	MessageInterface processDeleteOrders(  DeleteOrdersRequest pDeleteOrdersRequest, RequestContextInterface pRequestContext) {
		Orderbook tOrderbook = mOrderbooks.get( pDeleteOrdersRequest.getSymbol().get());
		if (tOrderbook == null) {
			mLog.warn("orderbook \"" + pDeleteOrdersRequest.getSymbol().get() + "\" does not exists " + pRequestContext );
			return StatusMessageBuilder.error("orderbook \"" + pDeleteOrdersRequest.getSymbol().get() + "\" does not exists", pDeleteOrdersRequest.getRef().get());
		}

		synchronized( tOrderbook ) {
			MessageInterface tMsg = tOrderbook.deleteOrders(pDeleteOrdersRequest.getRef().get(), pRequestContext);
			return tMsg;
		}
	}


	 MessageInterface processQueryOrderbook( QueryOrderbookRequest pQueryOrderbookRequest, RequestContextInterface  pRequestContext) {
		Orderbook tOrderbook = mOrderbooks.get( pQueryOrderbookRequest.getSymbol().get());
		if (tOrderbook == null) {
			mLog.warn("orderbook \"" + pQueryOrderbookRequest.getSymbol().get() + "\" does not exists " + pRequestContext );
			return StatusMessageBuilder.error("orderbook \"" + pQueryOrderbookRequest.getSymbol().get() + "\" does not exists", pQueryOrderbookRequest.getRef().get());
		}

		synchronized ( tOrderbook ) {
			 return tOrderbook.orderbookSnapshot(pQueryOrderbookRequest);
		 }
	}

	MessageInterface processQueryOwnOrders( InternalOwnOrdersRequest pInternalOwnOrdersRequest, RequestContextInterface pRequestContext) {
		Orderbook tOrderbook = mOrderbooks.get( pInternalOwnOrdersRequest.getSymbol().get());
		if (tOrderbook == null) {
			mLog.warn("orderbook \"" + pInternalOwnOrdersRequest.getSymbol().get() + "\" does not exists " + pRequestContext );
			return StatusMessageBuilder.error("orderbook \"" + pInternalOwnOrdersRequest.getSymbol().get() + "\" does not exists", pInternalOwnOrdersRequest.getRef().get());
		}

		synchronized( tOrderbook ) {
			MessageInterface tMsg = tOrderbook.queryOwnOrders(pInternalOwnOrdersRequest.getRef().get(), pRequestContext);
			return tMsg;
		}
	}

	 MessageInterface processPriceLevel(InternalPriceLevelRequest pInternalPriceLevelRequest, RequestContextInterface pRequestContext) {
		Orderbook tOrderbook = mOrderbooks.get( pInternalPriceLevelRequest.getSymbol().get());

		if (tOrderbook == null) {
			mLog.warn("orderbook \"" + pInternalPriceLevelRequest.getSymbol().get() + "\" does not exists " + pRequestContext );
			return StatusMessageBuilder.error("orderbook \"" + pInternalPriceLevelRequest.getSymbol().get() + "\" does not exists", pInternalPriceLevelRequest.getRef().get());
		}

		synchronized (tOrderbook) {
			BdxPriceLevel tBdxPriceLevel = tOrderbook.buildPriceLevelBdx(pInternalPriceLevelRequest.getLevels().get());
			InternalPriceLevelResponse tResponse = new InternalPriceLevelResponse();
			tResponse.setRef(pInternalPriceLevelRequest.getRef().get());
			tResponse.setBdxPriceLevel(tBdxPriceLevel);
			return tResponse;
		}
	}




	private MessageInterface executeDeleteOrders( DeleteOrdersRequest pDeleteOrderRequest, RequestContext pRequestContext) {
		return null;
	}



	public List<String> getOrderbookSymbols() {
		ArrayList<String> tList = new ArrayList<>();
		Iterator<Orderbook> tItr = mOrderbooks.values().iterator();
		while( tItr.hasNext()) {
			tList.add( tItr.next().getSymbolName());
		}
		return tList;
	}



	@Override
	public void orderAdded(Order pOrder, SessionCntxInterface pSessionCntx, long pOrderbookSeqNo) {
		// will trigger a public and private orderbook change
		if (mIsEnabledPrivateFlow) {
			mMarketDataDistributor.queueBdxPrivate( pSessionCntx, pOrder.toOwnOrderBookChg(Order.ChangeAction.ADD, pOrderbookSeqNo));
		}
		if (mIsEnabledOrderbookChangeFlow) {
			mMarketDataDistributor.queueBdxPublic(pOrder.toOrderBookChg(Order.ChangeAction.ADD, pOrderbookSeqNo));
		}
	}

	@Override
	public void orderRemoved(Order pOrder, SessionCntxInterface pSessionCntx, long pOrderbookSeqNo) {
		if (mIsEnabledPrivateFlow) {
			mMarketDataDistributor.queueBdxPrivate(pSessionCntx, pOrder.toOwnOrderBookChg(Order.ChangeAction.REMOVE, pOrderbookSeqNo));
		}
		if (mIsEnabledOrderbookChangeFlow) {
			mMarketDataDistributor.queueBdxPublic(pOrder.toOrderBookChg(Order.ChangeAction.REMOVE, pOrderbookSeqNo));
		}
	}

	@Override
	public void orderModified(Order pOrder, SessionCntxInterface pSessionCntx, long pOrderbookSeqNo) {
		if (mIsEnabledPrivateFlow) {
			mMarketDataDistributor.queueBdxPrivate(pSessionCntx, pOrder.toOwnOrderBookChg(Order.ChangeAction.MODIFY, pOrderbookSeqNo));
		}
		if (mIsEnabledOrderbookChangeFlow) {
			mMarketDataDistributor.queueBdxPublic(pOrder.toOrderBookChg(Order.ChangeAction.MODIFY, pOrderbookSeqNo));
		}
	}

	@Override
	public void trade(InternalTrade pTrade, SessionCntxInterface pSessionCntx) {
		String tSymbol =  pTrade.getSymbol();

		TeAppCntx.getInstance().getTradeContainer().addTrade( pTrade );


		if (mIsEnabledTradeflow) {
			mMarketDataDistributor.queueBdxPublic(pTrade.toBdxTrade());
		}

		if (mIsEnabledPrivateFlow) {
			if (pTrade.isOnBuySide( pSessionCntx.getAccount())) {
				mMarketDataDistributor.queueBdxPrivate( pSessionCntx, pTrade.toOwnBdxTrade(Order.Side.BUY));
			}
			if (pTrade.isOnSellSide(pSessionCntx.getAccount())) {
				mMarketDataDistributor.queueBdxPrivate(pSessionCntx, pTrade.toOwnBdxTrade(Order.Side.SELL));
			}
		}
	}

	@Override
	public void newOrderMatched(Order pOrder, SessionCntxInterface pSessionCntx, long pOrderbookSeqNo) {
		// New order completly matched and not in the book
		if (mIsEnabledPrivateFlow) {
			mMarketDataDistributor.queueBdxPrivate( pSessionCntx, pOrder.toOwnOrderBookChg(Order.ChangeAction.ADD, pOrderbookSeqNo));
		}
	}

	@Override
	public void orderbookChanged(String pSymbol) {
		// Trigger price level update
		if (mIsEnabledOrderbookChangeFlow) {
			mMarketDataDistributor.orderbookChanged( pSymbol );
		}
	}
}

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
import com.hoddmimes.te.common.TeException;
import com.hoddmimes.te.common.interfaces.MarketDataInterface;
import com.hoddmimes.te.common.interfaces.RequestContextInterface;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.common.interfaces.TeMgmtServices;
import com.hoddmimes.te.instrumentctl.InstrumentContainer;
import com.hoddmimes.te.instrumentctl.SymbolX;
import com.hoddmimes.te.management.RateItem;
import com.hoddmimes.te.management.RateStatistics;
import com.hoddmimes.te.management.service.MgmtCmdCallbackInterface;
import com.hoddmimes.te.management.service.MgmtComponent;
import com.hoddmimes.te.management.service.MgmtComponentInterface;
import com.hoddmimes.te.messages.MgmtMessageRequest;
import com.hoddmimes.te.messages.MgmtMessageResponse;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import com.hoddmimes.te.positions.AccountPosition;
import com.hoddmimes.te.sessionctl.RequestContext;
import com.hoddmimes.te.trades.TradeX;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MatchingEngine implements MatchingEngineCallback, MgmtCmdCallbackInterface
{
	private static SimpleDateFormat SDT_TIME = new SimpleDateFormat("HH:mm:ss.SSS");

	private Logger mLog = LogManager.getLogger( MatchingEngine.class);
	private InstrumentContainer         mInstrumentContainer;
	private JsonObject                  mConfiguration;
	private MarketDataInterface         mMarketDataDistributor;
	private HashMap<String,Orderbook>   mOrderbooks;
	private RateItem                    mOrderRate;
	private boolean                     mPreTradeValidation;

	private AtomicInteger  mOrderCount;
	private AtomicInteger  mDeleteCount;
	private AtomicInteger  mAmendCount;





	private boolean mIsEnabledPrivateFlow, mIsEnabledPriceLevelFlow, mIsEnabledTradeflow, mIsEnabledOrderbookChangeFlow;

	public MatchingEngine(JsonObject pTeConfiguration, InstrumentContainer pInstrumentContainer, MarketDataInterface pMarketDataInterface) {
		mAmendCount = new AtomicInteger(0);
		mOrderCount = new AtomicInteger(0);
		mDeleteCount = new AtomicInteger(0);

		mInstrumentContainer = pInstrumentContainer;
		mMarketDataDistributor = pMarketDataInterface;
		mConfiguration = AuxJson.navigateObject(pTeConfiguration, "TeConfiguration/matchingEngineConfiguration");
		initializeOrderbooks();
		configureDataDistribution(AuxJson.navigateObject(pTeConfiguration, "TeConfiguration/marketDataConfiguration"));
		MgmtComponentInterface tMgmt = TeAppCntx.getInstance().getMgmtService().registerComponent( TeMgmtServices.MatchingService, 0, this );
		mOrderRate = RateStatistics.getInstance().addRateItem("Order rate");
		mPreTradeValidation = TeAppCntx.getInstance().getPositionController().isPreTradingValidationEnabled();
	}


	private void configureDataDistribution( JsonObject pMarketDataConfiguration ) {
		mIsEnabledOrderbookChangeFlow = AuxJson.navigateBoolean(pMarketDataConfiguration,"enableOrdebookChanges");
		mIsEnabledPriceLevelFlow =  AuxJson.navigateBoolean(pMarketDataConfiguration,"enablePriceLevels");
		mIsEnabledPrivateFlow =  AuxJson.navigateBoolean(pMarketDataConfiguration,"enablePrivateFlow");
		mIsEnabledTradeflow =  AuxJson.navigateBoolean(pMarketDataConfiguration,"enableTradeFlow");
	}


	private void initializeOrderbooks() {
		mOrderbooks = new HashMap<>();
		for(SymbolX tSymbol : mInstrumentContainer.getSymbols()) {
			if (!tSymbol.getSuspended().orElse(false)) {
				mOrderbooks.put( tSymbol.getSid().get(), new Orderbook(tSymbol, mLog, this));
			} else {
				mLog.warn("Orderbook " + tSymbol + " will not be loaded, symbol is disabled!");
			}
		}
	}

	MessageInterface processAddOrder(AddOrderRequest pAddOrderRequest, RequestContextInterface pRequestContext ) {
		Orderbook tOrderbook = mOrderbooks.get(pAddOrderRequest.getSid().get());
		pRequestContext.timestamp("retrieve orderbook");
		if (tOrderbook == null) {
			mLog.warn("orderbook " + pAddOrderRequest.getSid().get() + " does not exists " + pRequestContext);
			return  StatusMessageBuilder.error("orderbook " + pAddOrderRequest.getSid().get() + " does not exists", pAddOrderRequest.getRef().get());
		}
		if (!mInstrumentContainer.isOpen( pAddOrderRequest.getSid().get())) {
			return  StatusMessageBuilder.error("orderbook " + pAddOrderRequest.getSid().get() + " is not open for trading", pAddOrderRequest.getRef().get());
		}
		pRequestContext.timestamp("is orderbook open");

		synchronized (tOrderbook) {

			Order tOrder = new Order(pRequestContext.getAccountId(), pAddOrderRequest);

			if (mPreTradeValidation) {
				try {preTradeValidation( tOrderbook, tOrder  ); }
				catch( TeException te) {
					return StatusMessageBuilder.error("pre-trade validation failure", pAddOrderRequest.getRef().get(), te );
				}
			}

			mOrderRate.increment();
			mOrderCount.incrementAndGet();

			pRequestContext.timestamp("create order");

			MessageInterface tMsg = tOrderbook.addOrder(tOrder, pRequestContext);
			return tMsg;
		}
	}

	private void preTradeValidation( Orderbook pOrderbook, Order pOrder ) throws TeException
	{
		AccountPosition tAccount = TeAppCntx.getInstance().getPositionController().getAccount( pOrder.getAccountId() );
		if (tAccount == null) {
			throw new TeException(0, StatusMessageBuilder.error("No deposit position defined for account: " + pOrder.getAccountId(), pOrder.getUserRef()));
		}

		if (pOrder.getSide() == Order.Side.BUY) {
			if (!tAccount.validateBuyOrder(  pOrder, pOrderbook.getAccountExposure( pOrder.getAccountId(), Order.Side.BUY))) {
				throw new TeException(0, StatusMessageBuilder.error("Order will exceed cash exposure limit ", pOrder.getUserRef()));
			}
		} else {
			if (!tAccount.validateSellOrder(pOrder, pOrderbook.getAccountPosition(pOrder.getAccountId(), Order.Side.SELL))) {
				throw new TeException(0, StatusMessageBuilder.error("Order will exceed selling position limit ", pOrder.getUserRef()));
			}
		}
	}

	MessageInterface processAmendOrder( AmendOrderRequest pAmendOrderRequest, RequestContextInterface pRequestContext) {
		long tOrderId;

		Orderbook tOrderbook = mOrderbooks.get(pAmendOrderRequest.getSid().get());
		if (tOrderbook == null) {
			mLog.warn("orderbook " + pAmendOrderRequest.getSid().get() + " does not exists " + pRequestContext);
			return StatusMessageBuilder.error("orderbook " + pAmendOrderRequest.getSid().get() + " does not exists", pAmendOrderRequest.getRef().get());
		}
		if (!mInstrumentContainer.isOpen( pAmendOrderRequest.getSid().get())) {
			return  StatusMessageBuilder.error("orderbook " + pAmendOrderRequest.getSid().get() + " is not open for trading", pAmendOrderRequest.getRef().get());
		}

		synchronized (tOrderbook) {
			mAmendCount.incrementAndGet();
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

	int mgmtDeleteAllOrders( MgmtDeleteAllOrdersRequest pRequest ) {
		int tOrdersDeleted = 0;
		String tAccount = pRequest.getAccount().get();
		RequestContext tRqstCntx = new RequestContext( pRequest, TeAppCntx.getInstance().getIntenalSessionContext());

		Iterator<Orderbook> tItr = mOrderbooks.values().iterator();
		while(tItr.hasNext()) {
			Orderbook ob = tItr.next();
			synchronized (ob) {
				tOrdersDeleted += ob.deleteOrders( pRequest.getAccount().get(),tRqstCntx );
			}
		}
		return tOrdersDeleted;
	}

	boolean mgmtDeleteOrder( MgmtDeleteOrderRequest pRequest ) {
		OwnOrder tOrder = pRequest.getOrder().get();
		long tOrderId;

		Orderbook tOrderbook = mOrderbooks.get( tOrder.getSid().get());
		if (tOrderbook == null) {
			mLog.warn("orderbook " + tOrder.getSid().get() + " does not exists (mgmtDelete)" );
			return false;
		}

		synchronized( tOrderbook ) {
			try {
				tOrderId = Long.parseLong(tOrder.getOrderId().get(), 16);
			} catch (NumberFormatException e) {
				mLog.warn("delete order, invalid order id (mgmt delete)");
				return false;
			}

			RequestContext tRqstCntx = new RequestContext( pRequest, TeAppCntx.getInstance().getIntenalSessionContext());
			MessageInterface tMsg = tOrderbook.deleteOrder(tOrderId, pRequest.getRef().get(), tRqstCntx);
			if (tMsg instanceof DeleteOrderResponse) {
				return true;
			} else {
				return false;
			}
		}
	}

	MgmtQueryMatcherResponse mgmtGetStatisticals( MgmtQueryMatcherRequest pRequest ) {
		MgmtQueryMatcherResponse tResponse = new MgmtQueryMatcherResponse().setRef( pRequest.getRef().get());
		List<MgmtSymbolMatcherEntry> tSidLst = new ArrayList<>();

		Iterator<Orderbook> tItr = mOrderbooks.values().iterator();
		while(tItr.hasNext()) {
			Orderbook ob = tItr.next();
			synchronized (ob) {
				tSidLst.add( ob.getStatistics());
			}
		}

		List<MgmtStatEntry> tStatCounter = new ArrayList<>();
		tStatCounter.add( new MgmtStatEntry().setAttribute("Total Orders Requests").setValue( String.valueOf( mOrderCount.get())));
		tStatCounter.add( new MgmtStatEntry().setAttribute("Total Delete Requests").setValue( String.valueOf( mDeleteCount.get())));
		tStatCounter.add( new MgmtStatEntry().setAttribute("Total Amend Requests").setValue( String.valueOf( mAmendCount.get())));
		tStatCounter.add( mOrderRate.get1SecMaxStat());
		tStatCounter.add( mOrderRate.get10SecMaxStat());
		tStatCounter.add( mOrderRate.get60SecMaxStat());

		tResponse.setMatcherCounters( tStatCounter );
		tResponse.setSids( tSidLst );
		return tResponse;
	}

	InternalTrade mgmtRevertTrade( MgmtRevertTradeRequest pRequest ) {
		ContainerTrade tTrade = pRequest.getTrade().get();

		Orderbook tOrderbook = mOrderbooks.get( tTrade.getSid().get());
		if (tOrderbook == null) {
			mLog.warn("orderbook " + tTrade.getSid().get() + " does not exists (mgmtDelete)" );
			return null;
		}

		RequestContext tRqstCntx = new RequestContext( pRequest, TeAppCntx.getInstance().getIntenalSessionContext());

		synchronized( tOrderbook ) {
			return tOrderbook.revertTrade( tTrade, tRqstCntx);
		}
	}


	MessageInterface processDeleteOrder( DeleteOrderRequest pDeleteOrderRequest, RequestContextInterface pRequestContext) {
		long tOrderId;

		Orderbook tOrderbook = mOrderbooks.get( pDeleteOrderRequest.getSid().get());
		if (tOrderbook == null) {
			mLog.warn("orderbook " + pDeleteOrderRequest.getSid().get() + " does not exists " + pRequestContext );
			return StatusMessageBuilder.error("orderbook " + pDeleteOrderRequest.getSid().get() + " does not exists", pDeleteOrderRequest.getRef().get());
		}
		//if (!mInstrumentContainer.isOpen( pDeleteOrderRequest.getSid().get())) {
		//	return  StatusMessageBuilder.error("orderbook " + pDeleteOrderRequest.getSid().get() + " is not open for trading", pDeleteOrderRequest.getRef().get());
		//}

		synchronized( tOrderbook ) {
			try {
				mDeleteCount.incrementAndGet();
				tOrderId = Long.parseLong(pDeleteOrderRequest.getOrderId().get(), 16);
			} catch (NumberFormatException e) {
				mLog.warn("delete order, invalid order id " + pRequestContext);
				return StatusMessageBuilder.error("invalid order id", pDeleteOrderRequest.getRef().get());
			}

			MessageInterface tMsg = tOrderbook.deleteOrder(tOrderId, pDeleteOrderRequest.getRef().get(), pRequestContext);
			return tMsg;
		}
	}

	MessageInterface processQueryBBO( QueryBBORequest pRqstMsg, RequestContextInterface pRequestContext) {
		Iterator<Orderbook> tItr = mOrderbooks.values().iterator();
		QueryBBOResponse tRspMsg = new QueryBBOResponse();
		tRspMsg.setRef( pRqstMsg.getRef().get());
		while(tItr.hasNext()) {
			Orderbook ob = tItr.next();
			if (ob.getMarketId() == pRqstMsg.getMarketId().get()) {
				synchronized (ob) {
					tRspMsg.addPrices( ob.getBBO());
				}
			}
		}
		return tRspMsg;
	}



	MessageInterface processDeleteOrders(  DeleteOrdersRequest pDeleteOrdersRequest, RequestContextInterface pRequestContext) {
		Iterator<Orderbook> tItr = mOrderbooks.values().iterator();
		int tOrdersDeleted = 0;

		while(tItr.hasNext()) {
			Orderbook ob = tItr.next();
			if (mInstrumentContainer.isOpen( ob.getSymbolId())) {
				if (ob.getMarketId() == pDeleteOrdersRequest.getMarket().get() &&
					(pDeleteOrdersRequest.getSid().isEmpty() || (pDeleteOrdersRequest.getSid().get().contentEquals( ob.getSymbolId())))) {
					synchronized (ob) {
						tOrdersDeleted += ob.deleteOrders(
								pRequestContext.getAccountId(),
								pRequestContext);
					}
				}
			}
		}
		return StatusMessageBuilder.success("Deleted " + tOrdersDeleted + " orders for account: " + pRequestContext.getAccountId(), pDeleteOrdersRequest.getRef().get());
	}


	 MessageInterface processQueryOrderbook( QueryOrderbookRequest pQueryOrderbookRequest, RequestContextInterface  pRequestContext) {
		Orderbook tOrderbook = mOrderbooks.get( pQueryOrderbookRequest.getSid().get());
		if (tOrderbook == null) {
			mLog.warn("orderbook " + pQueryOrderbookRequest.getSid().get() + " does not exists " + pRequestContext );
			return StatusMessageBuilder.error("orderbook " + pQueryOrderbookRequest.getSid().get() + " does not exists", pQueryOrderbookRequest.getRef().get());
		}

		synchronized ( tOrderbook ) {
			 return tOrderbook.orderbookSnapshot(pQueryOrderbookRequest);
		 }
	}

	MgmtGetAccountOrdersResponse getOrdersForAccount( MgmtGetAccountOrdersRequest pRequest ) {
		MgmtGetAccountOrdersResponse tResponse = new MgmtGetAccountOrdersResponse().setRef( pRequest.getRef().get());	Iterator<Orderbook> tItr = mOrderbooks.values().iterator();

		while(tItr.hasNext()) {
			Orderbook ob = tItr.next();
			synchronized (ob) {
				tResponse.addOrders(ob.getOrdersForAccount(pRequest.getAccount().get()));
			}
		}
		return tResponse;
	}


	MessageInterface processQueryOwnOrders( InternalOwnOrdersRequest pInternalOwnOrdersRequest, RequestContextInterface pRequestContext) {
		Orderbook tOrderbook = mOrderbooks.get( pInternalOwnOrdersRequest.getSid().get());
		if (tOrderbook == null) {
			mLog.warn("orderbook " + pInternalOwnOrdersRequest.getSid().get() + " does not exists " + pRequestContext );
			return StatusMessageBuilder.error("orderbook " + pInternalOwnOrdersRequest.getSid().get() + " does not exists", pInternalOwnOrdersRequest.getRef().get());
		}

		synchronized( tOrderbook ) {
			MessageInterface tMsg = tOrderbook.queryOwnOrders(pInternalOwnOrdersRequest.getRef().get(), pRequestContext);
			return tMsg;
		}
	}

	 MessageInterface processPriceLevel(InternalPriceLevelRequest pInternalPriceLevelRequest, RequestContextInterface pRequestContext) {
		Orderbook tOrderbook = mOrderbooks.get( pInternalPriceLevelRequest.getSid().get());

		if (tOrderbook == null) {
			mLog.warn("orderbook " + pInternalPriceLevelRequest.getSid().get() + " does not exists " + pRequestContext );
			return StatusMessageBuilder.error("orderbook " + pInternalPriceLevelRequest.getSid().get() + " does not exists", pInternalPriceLevelRequest.getRef().get());
		}

		synchronized (tOrderbook) {
			BdxPriceLevel tBdxPriceLevel = tOrderbook.buildPriceLevelBdx(pInternalPriceLevelRequest.getLevels().get());
			InternalPriceLevelResponse tResponse = new InternalPriceLevelResponse();
			tResponse.setRef(pInternalPriceLevelRequest.getRef().get());
			tResponse.setBdxPriceLevel(tBdxPriceLevel);
			return tResponse;
		}
	}



	public List<String> getOrderbookSymbolIds() {
		ArrayList<String> tList = new ArrayList<>();
		Iterator<Orderbook> tItr = mOrderbooks.values().iterator();
		while( tItr.hasNext()) {
			tList.add( tItr.next().getSymbolId());
		}
		return tList;
	}



	@Override
	public void orderAdded(Order pOrder, SessionCntxInterface pSessionCntx, long pOrderbookSeqNo) {
		// will trigger a public and private orderbook change
		if (mIsEnabledPrivateFlow) {
			mMarketDataDistributor.queueBdxPrivate( pOrder.getAccountId(), pOrder.toOwnOrderBookChg(Order.ChangeAction.ADD, pOrderbookSeqNo));
		}
		if (mIsEnabledOrderbookChangeFlow) {
			mMarketDataDistributor.queueBdxPublic(pOrder.toOrderBookChg(Order.ChangeAction.ADD, pOrderbookSeqNo));
		}
	}

	@Override
	public void orderRemoved(Order pOrder, SessionCntxInterface pSessionCntx, long pOrderbookSeqNo) {
		if (mIsEnabledPrivateFlow) {
			mMarketDataDistributor.queueBdxPrivate(pOrder.getAccountId(), pOrder.toOwnOrderBookChg(Order.ChangeAction.REMOVE, pOrderbookSeqNo));
		}
		if (mIsEnabledOrderbookChangeFlow) {
			mMarketDataDistributor.queueBdxPublic(pOrder.toOrderBookChg(Order.ChangeAction.REMOVE, pOrderbookSeqNo));
		}
	}

	@Override
	public void orderModified(Order pOrder, SessionCntxInterface pSessionCntx, long pOrderbookSeqNo) {
		if (mIsEnabledPrivateFlow) {
			mMarketDataDistributor.queueBdxPrivate(pOrder.getAccountId(), pOrder.toOwnOrderBookChg(Order.ChangeAction.MODIFY, pOrderbookSeqNo));
		}
		if (mIsEnabledOrderbookChangeFlow) {
			mMarketDataDistributor.queueBdxPublic(pOrder.toOrderBookChg(Order.ChangeAction.MODIFY, pOrderbookSeqNo));
		}
	}

	@Override
	public void trade(InternalTrade pTrade, SessionCntxInterface pSessionCntx) {
		String tSymbol =  pTrade.getSid();

		BdxTrade tBdxTrade = TeAppCntx.getInstance().getTradeContainer().addTrade( pTrade );

		TeAppCntx.getInstance().getPositionController().tradeExcution ( pTrade );

		if (mIsEnabledTradeflow) {
			mMarketDataDistributor.queueBdxPublic(tBdxTrade);
		}

		if (mIsEnabledPrivateFlow) {
			if (pTrade.isOnBuySide( pSessionCntx.getAccount())) {
				mMarketDataDistributor.queueBdxPrivate( pSessionCntx.getAccount(), pTrade.toOwnBdxTrade(Order.Side.BUY));
			}
			if (pTrade.isOnSellSide(pSessionCntx.getAccount())) {
				mMarketDataDistributor.queueBdxPrivate(pSessionCntx.getAccount(), pTrade.toOwnBdxTrade(Order.Side.SELL));
			}
		}
	}

	@Override
	public void newOrderMatched(Order pOrder, SessionCntxInterface pSessionCntx, long pOrderbookSeqNo) {
		// New order completly matched and not in the book
		if (mIsEnabledPrivateFlow) {
			mMarketDataDistributor.queueBdxPrivate( pOrder.getAccountId(), pOrder.toOwnOrderBookChg(Order.ChangeAction.ADD, pOrderbookSeqNo));
		}
	}

	@Override
	public void orderbookChanged(String pSymbol) {
		// Trigger price level update
		if (mIsEnabledOrderbookChangeFlow) {
			mMarketDataDistributor.orderbookChanged( pSymbol );
		}
	}

	@Override
	public MgmtMessageResponse mgmtRequest(MgmtMessageRequest pMgmtRequest) {
		if (pMgmtRequest instanceof MgmtGetAccountOrdersRequest) {
			return getOrdersForAccount((MgmtGetAccountOrdersRequest) pMgmtRequest);
		}
		if (pMgmtRequest instanceof MgmtDeleteOrderRequest) {
			boolean tDeleted = mgmtDeleteOrder((MgmtDeleteOrderRequest) pMgmtRequest);
			return new MgmtDeleteOrderResponse().setRef( pMgmtRequest.getRef().get()).setDeleted( tDeleted );
		}
		if (pMgmtRequest instanceof MgmtDeleteAllOrdersRequest) {
			int tOrdersDeleted = mgmtDeleteAllOrders((MgmtDeleteAllOrdersRequest) pMgmtRequest);
			return new MgmtDeleteAllOrdersResponse().setRef( pMgmtRequest.getRef().get()).setDeleted( tOrdersDeleted);
		}
		if (pMgmtRequest instanceof MgmtRevertTradeRequest) {
			InternalTrade tTrade = mgmtRevertTrade((MgmtRevertTradeRequest) pMgmtRequest);
			MgmtRevertTradeResponse tResponse = new MgmtRevertTradeResponse().setRef( pMgmtRequest.getRef().get());
			tResponse.setRevertedTrades( new TradeX( tTrade));
			return tResponse;
		}
		if (pMgmtRequest instanceof MgmtQueryMatcherRequest) {
			return  mgmtGetStatisticals((MgmtQueryMatcherRequest) pMgmtRequest);
		}
		throw new RuntimeException("no management entry for : " + pMgmtRequest.getMessageName());
	}
}

package com.hoddmimes.te.engine;


import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.instrumentctl.InstrumentContainer;
import com.hoddmimes.te.instrumentctl.Symbol;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.AddOrderRequest;
import com.hoddmimes.te.messages.generated.BdxPriceLevel;
import com.hoddmimes.te.messages.generated.InternalPriceLevelRequest;
import com.hoddmimes.te.messages.generated.InternalPriceLevelResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class MatchingEngine implements EngineInterface
{
	private Logger mLog = LogManager.getLogger( MatchingEngine.class);
	private InstrumentContainer         mInstrumentContainer;
	private JsonObject                  mConfiguration;
	private HashMap<String,Orderbook>   mOrderbooks;

	private boolean mIsEnablePrivateFlow, mIsEnablePriceLevelFlow, mIsEnableOrderbookChangeFlow;

	public MatchingEngine(JsonObject pTeConfiguration, InstrumentContainer pInstrumentContainer) {
		mInstrumentContainer = pInstrumentContainer;
		mConfiguration = AuxJson.navigateObject(pTeConfiguration, "TeConfiguration/matchingEngineConfiguration");
		TeAppCntx.getInstance().setMatchingEngine( this );
		initializeOrderbooks();
		configureDataDistribution(AuxJson.navigateObject(pTeConfiguration, "TeConfiguration/marketDataConfiguration"));
	}


	private void configureDataDistribution( JsonObject pMarketDataConfiguration ) {
		mIsEnableOrderbookChangeFlow = AuxJson.navigateBoolean(pMarketDataConfiguration,"enableOrdebookChanges");
		mIsEnablePriceLevelFlow =  AuxJson.navigateBoolean(pMarketDataConfiguration,"enablePriceLevels");
		mIsEnablePrivateFlow =  AuxJson.navigateBoolean(pMarketDataConfiguration,"enablePrivateFlow");
	}


	private void initializeOrderbooks() {
		mOrderbooks = new HashMap<>();
		mInstrumentContainer.getSymbols().stream().forEach( s -> { mOrderbooks.put( s.getId(), new Orderbook(s, mLog, this)); } );
	}


	public MessageInterface execute(MeRqstCntx pRqstCntx ) {
		pRqstCntx.timestamp("ME start processing");

		Symbol tSymbol = mInstrumentContainer.getSymbol( pRqstCntx.mRequest.getSymbol().get());
		if (tSymbol == null) {
			return StatusMessageBuilder.error("unknown symbol \"" + pRqstCntx.mRequest.getSymbol().get() + "\"", null );
		}


		if (pRqstCntx.mRequest instanceof AddOrderRequest) {
			return executeAddOrder( (AddOrderRequest) pRqstCntx.mRequest, pRqstCntx );
		}

		if (pRqstCntx.mRequest instanceof InternalPriceLevelRequest) {
			return executePriceLevelRequest( (InternalPriceLevelRequest) pRqstCntx.mRequest, pRqstCntx );
		}

		return null;
	}

	private MessageInterface executePriceLevelRequest( InternalPriceLevelRequest pRequest, MeRqstCntx  pRqstCntx) {
		Orderbook tOrderbook = mOrderbooks.get( pRequest.getSymbol().get());

		if (tOrderbook == null) {
			mLog.warn("orderbook \"" + pRequest.getSymbol().get() + "\" does not exists " + pRqstCntx.getSessionInfo() );
			return StatusMessageBuilder.error("orderbook \"" + pRequest.getSymbol().get() + "\" does not exists", pRequest.getRef().get());
		}

		BdxPriceLevel tBdxPriceLevel = tOrderbook.buildPriceLevelBdx( pRequest.getLevels().get());
		InternalPriceLevelResponse tResponse = new InternalPriceLevelResponse();
		tResponse.setRef( pRequest.getRef().get());
		tResponse.setBdxPriceLevel(tBdxPriceLevel);
		pRqstCntx.addPublicBdx( tBdxPriceLevel );
		return  tResponse;
	}

	private MessageInterface executeAddOrder( AddOrderRequest pAddOrderRequest, MeRqstCntx  pRqstCntx)
	{
		Orderbook tOrderbook = mOrderbooks.get( pAddOrderRequest.getSymbol().get());
		if (tOrderbook == null) {
			mLog.warn("orderbook \"" + pAddOrderRequest.getSymbol().get() + "\" does not exists " + pRqstCntx.getSessionInfo() );
			return StatusMessageBuilder.error("orderbook \"" + pAddOrderRequest.getSymbol().get() + "\" does not exists", pAddOrderRequest.getRef().get());
		}
		Order tOrder = new Order(pRqstCntx.getUserId(), pAddOrderRequest);

		MessageInterface tMsg =  tOrderbook.addOrder( tOrder, pRqstCntx);
		return tMsg;
	}

	@Override
	public void orderAdded(Order pOrder, MeRqstCntx pRqstCntx, long pOrderbookSeqNo) {
		// will trigger a public and private orderbook change
		mLog.info("order Added");
		pRqstCntx.addPrivateBdx( pOrder.toOwnOrderBookChg(Order.ChangeAction.ADD, pOrderbookSeqNo));

		pRqstCntx.addPublicBdx( pOrder.toOrderBookChg(Order.ChangeAction.ADD, pOrderbookSeqNo));
	}

	@Override
	public void orderRemoved(Order pOrder, MeRqstCntx RqstCntx, long pOrderbookSeqNo) {
		mLog.info("order removed");
	}

	@Override
	public void orderModified(Order pOrder, MeRqstCntx RqstCntx, long pOrderbookSeqNo) {
		mLog.info("order modified");
	}

	@Override
	public void trade(Trade pTrade, MeRqstCntx RqstCntx) {
		mLog.info("new Trade");
	}

	@Override
	public void newOrderMatched(Order pOrder, MeRqstCntx RqstCntx, long pOrderbookSeqNo) {
		mLog.info("new OrderMatched");
	}

	@Override
	public void orderbookChanged(String mSymbol) {
		if (mIsEnableOrderbookChangeFlow) {
			mLog.info("price level orderbookChanged");
			TeAppCntx.getInstance().getMarketDataDistributor().orderbookChanged(mSymbol);
		}
	}
}

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

package com.hoddmimes.te.instrumentctl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.interfaces.MarketStates;
import com.hoddmimes.te.common.interfaces.TeMgmtServices;
import com.hoddmimes.te.management.service.MgmtCmdCallbackInterface;
import com.hoddmimes.te.management.service.MgmtComponentInterface;
import com.hoddmimes.te.messages.MgmtMessageRequest;
import com.hoddmimes.te.messages.MgmtMessageResponse;
import com.hoddmimes.te.messages.SID;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import com.hoddmimes.te.sessionctl.RequestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InstrumentContainer implements MgmtCmdCallbackInterface
{
	private Logger mLog = LogManager.getLogger( InstrumentContainer.class );
	private HashMap<Integer,  MarketX> mMarkets;
	private JsonObject mConfiguration;
	private Timer mPhaseTimer;
	private ConcurrentHashMap<Integer,TradingPhaseEvent> mTradingPhaseEvents;

   public InstrumentContainer( JsonObject pTeConfiguration ) {
	 mMarkets = new HashMap<>();
	 mTradingPhaseEvents = new ConcurrentHashMap<>();

	 mConfiguration = AuxJson.navigateObject( pTeConfiguration,"TeConfiguration/instrumentContainerConfiguration");
	 String tDataStore = AuxJson.navigateString(mConfiguration,"dataStore");
	 mPhaseTimer = new Timer();

	 try {
		 JsonElement jElement = AuxJson.loadAndParseFile( tDataStore ).get(0);
	     JsonObject tInstruments = jElement.getAsJsonObject();
		 JsonArray tMarketArray = tInstruments.get("MarketConfiguration").getAsJsonArray();
		 for (int i = 0; i < tMarketArray.size(); i++) {
			 JsonObject jMarket = tMarketArray.get(i).getAsJsonObject();
			 MarketX tMarket = new MarketX( jMarket );
			 mMarkets.put( tMarket.getId().get(), tMarket );
			 setMarketState( tMarket );
		 }
	 }
	 catch( Exception e) {
		 mLog.fatal("Fail to load instruments configuration from \"" + tDataStore + "\"", e);
		 System.exit(-1);
	 }
	 TeAppCntx.getInstance().setInstrumentContainer( this );
	 MgmtComponentInterface tMgmt = TeAppCntx.getInstance().getMgmtService().registerComponent( TeMgmtServices.InstrumentData, 0, this );
   }

	/**
	 * Invoked at system start, to set mark state
	 * @param pMarket
	 */
	private void setMarketState( MarketX pMarket ) {


		/**
		 * Market is not started normally. A hard state is set. Any further change to the market
		 * state has to be done via the TE Management interface
		 */
		String tCfgState = AuxJson.navigateString(mConfiguration, "startState", "normal");
		if (!tCfgState.contentEquals("normal")) {
			pMarket.setState(MarketStates.valueOf(tCfgState.toUpperCase()).name());
			return;
		}

		setMarketStateAndSchedule(pMarket);
	}

	private void setMarketStateAndSchedule( MarketX pMarket ) {
		long tTimeToNextPhaseMs = 0;

		String tPreOpen = pMarket.getMarketPreOpen().orElse(null);
	    String tOpen = pMarket.getMarketOpen().get();
	    String tClose = pMarket.getMarketClose().get();

	   SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
	   String tNow = sdf.format(System.currentTimeMillis());

	   if (tNow.compareTo( tClose ) > 0) {
		   pMarket.setState( MarketStates.CLOSED.name());
		   return;
	   }

		/**
		 * After Open, set state Open
		 * and schedule time for next trading phase i.e. Close
		 */
	   if (tNow.compareTo( tOpen ) > 0) {
		   pMarket.setState( MarketStates.OPEN.name());

		   tTimeToNextPhaseMs = getTimeDittFromNow( tClose );
		   TradingPhaseEvent tEvent = new TradingPhaseEvent( pMarket.getId().get(), MarketStates.CLOSED);
		   mTradingPhaseEvents.put( pMarket.getId().get(), tEvent );
		   mPhaseTimer.schedule( tEvent, tTimeToNextPhaseMs );
		   return;
	   }

		/**
		 * After preOpen, set state preOpen
		 * and schedule time for next trading phase i.e. Open
		 */

		if (tPreOpen != null) {
		   if (tNow.compareTo(tPreOpen) > 0) {
			   pMarket.setState( MarketStates.PREOPEN.name());

			   tTimeToNextPhaseMs = getTimeDittFromNow( tPreOpen );
			   TradingPhaseEvent tEvent = new TradingPhaseEvent( pMarket.getId().get(), MarketStates.OPEN);
			   mTradingPhaseEvents.put( pMarket.getId().get(), tEvent );
			   mPhaseTimer.schedule( tEvent, tTimeToNextPhaseMs );
			   return;
		   }
	   }


		/**
		 * Before preOpen / Open state is closed.
		 * Schedule a timer for next trading phase i.e. preOpen / Open.
		 */
		pMarket.setState( MarketStates.CLOSED.name());

	   if (tPreOpen != null) {
		   tTimeToNextPhaseMs = getTimeDittFromNow( tPreOpen );
		   TradingPhaseEvent tEvent = new TradingPhaseEvent( pMarket.getId().get(), MarketStates.PREOPEN);
		   mTradingPhaseEvents.put( pMarket.getId().get(), tEvent );
		   mPhaseTimer.schedule( tEvent, tTimeToNextPhaseMs );
	   } else {
		   tTimeToNextPhaseMs = getTimeDittFromNow( tOpen );
		   TradingPhaseEvent tEvent = new TradingPhaseEvent( pMarket.getId().get(), MarketStates.OPEN);
		   mTradingPhaseEvents.put( pMarket.getId().get(), tEvent );
		   mPhaseTimer.schedule( tEvent, tTimeToNextPhaseMs );

	   }
   }

	public MgmtMessageResponse setMarketState( MgmtSetMarketsRequest pRequest )
	{
		MarketX tMarket = mMarkets.get( pRequest.getMarketId().get());
		if (tMarket == null) {
			MgmtStatusResponse tStsRsp = new MgmtStatusResponse().setIsOk(false).setRef( pRequest.getRef().get()).setMessage("market not found");
			return tStsRsp;
		}

		if (pRequest.getHardClose().get()) {
			tMarket.setState( MarketStates.CLOSED.name());
			cancelAndRemoveTimers( pRequest.getMarketId().get());
			List<Market> tMktLst = new ArrayList<>( mMarkets.values());
			MgmtSetMarketsResponse tRsp = new MgmtSetMarketsResponse().setRef( pRequest.getRef().get()).setMarkets( tMktLst );
			return tRsp;
		}

		cancelAndRemoveTimers( pRequest.getMarketId().get());

		if (!pRequest.getPreOpen().isEmpty()) {
			tMarket.setMarketPreOpen( pRequest.getPreOpen().get());
		}
		if (!pRequest.getOpen().isEmpty()) {
			tMarket.setMarketOpen( pRequest.getOpen().get());
		}
		if (!pRequest.getClose().isEmpty()) {
			tMarket.setMarketClose( pRequest.getClose().get());
		}

		setMarketStateAndSchedule( tMarket );

		List<Market> tMktLst = new ArrayList<>( mMarkets.values());
		MgmtSetMarketsResponse tRsp = new MgmtSetMarketsResponse().setRef( pRequest.getRef().get()).setMarkets(tMktLst);
		return tRsp;
	}

	/**
	 * Invoked by timer event when there is time to change market state
	 * @param pMarketId
	 * @param pMarketState, upcomming market state
	 */
	private void scheduleMarketState( int pMarketId, MarketStates pMarketState)
	{
		MarketX tMarket = mMarkets.get( pMarketId );
		cancelAndRemoveTimers( pMarketId );

		tMarket.setState( pMarketState.name());
		setMarketStateAndSchedule( tMarket );

	}





	private void cancelAndRemoveTimers( int pMarketId ) {
		Iterator<TradingPhaseEvent> tTimerItr = mTradingPhaseEvents.values().iterator();
		while( tTimerItr.hasNext()) {
			TradingPhaseEvent tTimEvent = tTimerItr.next();
			if (tTimEvent.getMarketId() == pMarketId) {
				tTimEvent.cancel();
				tTimerItr.remove();
			}
		}
	}


	public QueryMarketsResponse queryMarkets(QueryMarketsRequest pRqstMsg, RequestContext pRequestContext) {
		QueryMarketsResponse tRspMsg = new QueryMarketsResponse();

		Iterator<MarketX> tItr = mMarkets.values().iterator();
		while( tItr.hasNext()) {
			tRspMsg.addMarkets( tItr.next());
		}
		return tRspMsg;
	}

	private MgmtSetSymbolResponse setSymbolsState( MgmtSetSymbolRequest pRqst ) {
		MarketX tMarket = mMarkets.get( pRqst.getMarketId().get());
		SymbolX tSymbol = tMarket.getSymbol( pRqst.getSid().get());
		tSymbol.setSuspended( pRqst.getSuspended().get());

		MgmtSetSymbolResponse tResponse = new MgmtSetSymbolResponse().setRef( pRqst.getRef().get());
		tResponse.setSymbol( tSymbol );
		return tResponse;
	}

	private MgmtGetSymbolsResponse  getSymbolsState( MgmtGetSymbolsRequest pRequest ) {
		MgmtGetSymbolsResponse tRsp = new MgmtGetSymbolsResponse().setRef( pRequest.getRef().get());
		MarketX tMarket  = mMarkets.get( pRequest.getMarketId().get());
		List<Symbol> tSymLst = new ArrayList<>( tMarket.getSymbols());
		tRsp.setSymbols( tSymLst );
		return tRsp;
	}

   public MessageInterface querySymbols(QuerySymbolsRequest pRqstMsg, RequestContext pRequestContext) {
	   QuerySymbolsResponse tRspMsg = new QuerySymbolsResponse();

	   MarketX tMarket = mMarkets.get( pRqstMsg.getMarketId().get());
	   if (tMarket == null) {
		   return StatusMessageBuilder.error("Market with id: " + pRqstMsg.getMarketId().get() + " is not defined ", pRqstMsg.getRef().get());
	   }

	   for( Symbol s : tMarket.getSymbols()) {
		   tRspMsg.addSymbols(s);
	   }
	   return tRspMsg;
   }

	public boolean marketDefined( int pMarket ) {
	   return mMarkets.containsKey( pMarket );
	}

	public boolean marketDefined( SID pSID ) {
		return mMarkets.containsKey( pSID );
	}

	public MarketX getMarket( int pMarket ) { return mMarkets.get( pMarket ); }


   public SymbolX getSymbol(String pSymbolId ) {
	   SymbolX tSymbol = null;

	   Iterator<MarketX> tItr = mMarkets.values().iterator();
	   while(tItr.hasNext() && (tSymbol == null)) {
		   tSymbol = tItr.next().getSymbol( pSymbolId );
	   }
	   return tSymbol;
   }

   public boolean isOpen( String pSymbolId ) {
	   SymbolX tSymbol = this.getSymbol(pSymbolId);
	   if (tSymbol == null) {
		   mLog.warn("open/close check symbol: " + pSymbolId + " is not found");
		   return false;
	   }

	   return tSymbol.isOpen();
   }

   public List<SymbolX> getSymbols() {
	   List<SymbolX> tSymbols = new ArrayList<>();
	   Iterator<MarketX> tItr = mMarkets.values().iterator();
	   while( tItr.hasNext() ) {
		   tSymbols.addAll( tItr.next().getSymbols());
	   }
	   return tSymbols;
   }

    private MgmtGetMarketsResponse serveGetMarkets( String pRef ) {
	    MgmtGetMarketsResponse tRsp = new MgmtGetMarketsResponse().setRef( pRef );
		List<Market> tMkts = new ArrayList<>( mMarkets.values());
		tRsp.setMarkets( tMkts );
        return tRsp;
	}

	@Override
	public MgmtMessageResponse mgmtRequest(MgmtMessageRequest pMgmtRequest) {
		if (pMgmtRequest instanceof MgmtGetMarketsRequest) {
			return serveGetMarkets( pMgmtRequest.getRef().get());
		}
		if (pMgmtRequest instanceof MgmtSetMarketsRequest) {
			return setMarketState((MgmtSetMarketsRequest) pMgmtRequest);
		}
		if (pMgmtRequest instanceof MgmtSetMarketsRequest) {
			return setMarketState((MgmtSetMarketsRequest) pMgmtRequest);
		}
		if (pMgmtRequest instanceof MgmtGetSymbolsRequest) {
			return getSymbolsState((MgmtGetSymbolsRequest) pMgmtRequest);
		}

		if (pMgmtRequest instanceof MgmtSetSymbolRequest) {
			return setSymbolsState((MgmtSetSymbolRequest) pMgmtRequest);
		}



		throw new RuntimeException("No service point for " + pMgmtRequest.getMessageName());
	}

	private long getTimeDittFromNow( String pNextStop ) {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
		SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		try {
			long tNxtStp = sdf.parse(pNextStop + ":00.000").getTime();
			String tNowStr = SDF.format( System.currentTimeMillis());
			long tNow = sdf.parse(tNowStr.substring(11)).getTime();
			//System.out.println( "next stop: " + SDF.format(tNxtStp ));
			//System.out.println( "now: " + SDF.format(tNow ));
			return (tNxtStp - tNow);
		}
		catch( Exception e) {
			e.printStackTrace();
		}
		return -1L;
	}

	class TradingPhaseEvent extends TimerTask
	{
		private int         mMarketId;
		private MarketStates    mNextMarketPhase;

		TradingPhaseEvent( int pMarketId, MarketStates pNextTradingPhase ) {
			mMarketId = pMarketId;
			mNextMarketPhase = pNextTradingPhase;
		}

		public int getMarketId() {
			return mMarketId;
		}

		public void run() {
			InstrumentContainer.this.scheduleMarketState( mMarketId, mNextMarketPhase );
		}
	}
}

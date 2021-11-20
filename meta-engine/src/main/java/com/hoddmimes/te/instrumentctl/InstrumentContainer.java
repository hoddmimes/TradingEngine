package com.hoddmimes.te.instrumentctl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import com.hoddmimes.te.sessionctl.RequestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;

public class InstrumentContainer
{
	private Logger mLog = LogManager.getLogger( InstrumentContainer.class );
	private HashMap<Integer,  MarketX> mMarkets;
	private JsonObject mConfiguration;

   public InstrumentContainer( JsonObject pTeConfiguration ) {
	 mMarkets = new HashMap<>();

	 mConfiguration = AuxJson.navigateObject( pTeConfiguration,"TeConfiguration/instrumentContainerConfiguration");
	 String tDataStore = AuxJson.navigateString(mConfiguration,"dataStore");

	 try {
		 JsonElement jElement = AuxJson.loadAndParseFile( tDataStore ).get(0);
	     JsonObject tInstruments = jElement.getAsJsonObject();
		 JsonArray tMarketArray = tInstruments.get("MarketConfiguration").getAsJsonArray();
		 for (int i = 0; i < tMarketArray.size(); i++) {
			 JsonObject jMarket = tMarketArray.get(i).getAsJsonObject();
			 MarketX tMarket = new MarketX( jMarket );
			 mMarkets.put( tMarket.getId().get(), tMarket );
		 }
	 }
	 catch( Exception e) {
		 mLog.fatal("Fail to load instruments configuration from \"" + tDataStore + "\"");
		 System.exit(-1);
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




   public SymbolX getSymbol(String pSymbolId ) {
	   SymbolX tSymbol = null;

	   Iterator<MarketX> tItr = mMarkets.values().iterator();
	   while(tItr.hasNext() && (tSymbol == null)) {
		   tSymbol = tItr.next().getSymbol( pSymbolId );
	   }
	   return tSymbol;
   }

   public boolean isOpen( String pSymbolId ) {
	   SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
	   String tNowStr = sdf.format(System.currentTimeMillis());

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

}

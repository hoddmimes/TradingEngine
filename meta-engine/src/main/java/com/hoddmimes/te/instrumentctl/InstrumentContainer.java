package com.hoddmimes.te.instrumentctl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hoddmimes.te.common.AuxJson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstrumentContainer
{
	private Logger mLog = LogManager.getLogger( InstrumentContainer.class );
	private Map<String,Symbol> mInstruments;
	private JsonObject mConfiguration;

   public InstrumentContainer( JsonObject pTeConfiguration ) {
	 mInstruments = new HashMap<>();

	 mConfiguration = AuxJson.navigateObject( pTeConfiguration,"TeConfiguration/instrumentContainerConfiguration");
	 String tDataStore = AuxJson.navigateString(mConfiguration,"dataStore");

	 try {
		 JsonElement jElement = AuxJson.loadAndParseFile( tDataStore ).get(0);
	     JsonObject tInstruments = jElement.getAsJsonObject();
		 JsonArray tSymbolArray = tInstruments.get("instruments").getAsJsonArray();
		 for (int i = 0; i < tSymbolArray.size(); i++) {
			 JsonObject jSym = tSymbolArray.get(i).getAsJsonObject();
			 double minP = (jSym.has("minPricePct")) ? jSym.get("minPricePct").getAsDouble() : 0.0d;
			 double maxP = (jSym.has("maxPricePct")) ? jSym.get("maxPricePct").getAsDouble() : 0.0d;
			 Symbol tSymbol = new Symbol( jSym.get("symbol").getAsString(),
					                      jSym.get("tickSize").getAsDouble(), minP, maxP );
			 mInstruments.put( tSymbol.getId(), tSymbol );
		 }
	 }
	 catch( Exception e) {
		 mLog.fatal("Fail to load instruments from \"" + tDataStore + "\"");
		 System.exit(-1);
	 }
   }

   public Symbol getSymbol( String pSymbolId ) {
	   return mInstruments.get( pSymbolId );
   }

   public List<Symbol> getSymbols() {
	   List<Symbol> tSymbols = new ArrayList<>();
	   tSymbols.addAll( mInstruments.values());
	   return tSymbols;
   }
}

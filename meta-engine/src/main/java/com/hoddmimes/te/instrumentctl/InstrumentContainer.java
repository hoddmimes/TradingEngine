package com.hoddmimes.te.instrumentctl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hoddmimes.te.common.AuxJson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstrumentContainer
{
	private Logger mLog = LogManager.getLogger( InstrumentContainer.class );
	private Map<String, SymbolX> mInstruments;
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
			 JsonObject jSymbol = tSymbolArray.get(i).getAsJsonObject();
			 SymbolX tSymbol = new SymbolX( jSymbol );
			 tSymbol.setClosed( false );
			 mInstruments.put( tSymbol.getId(), tSymbol );
		 }
	 }
	 catch( Exception e) {
		 mLog.fatal("Fail to load instruments from \"" + tDataStore + "\"");
		 System.exit(-1);
	 }

   }

   public SymbolX getSymbol(String pSymbolId ) {
	   return mInstruments.get( pSymbolId );
   }

   public boolean isOpen( String pSymbolId ) {
	   SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
	   String tNowStr = sdf.format(System.currentTimeMillis());

	   SymbolX tSymbol = mInstruments.get(pSymbolId);
	   if (tSymbol == null) {
		   mLog.warn("open/close check symbol: " + pSymbolId + " is not found");
		   return false;
	   }

	   if (tSymbol.getClosed().get()) {
		   return false;
	   }

	   if (tSymbol.getMarketOpen().get().contentEquals("00:00") && tSymbol.getMarketClose().get().contentEquals("00:00")) {
		   return true;
	   }
	   if ((tSymbol.getMarketClose().get().compareTo(tNowStr) > 0) && (tSymbol.getMarketOpen().get().compareTo(tNowStr) <= 0)) {
		   return true;
	   }

	   return false;
   }




   public List<SymbolX> getSymbols() {
	   List<SymbolX> tSymbols = new ArrayList<>();
	   tSymbols.addAll( mInstruments.values());
	   return tSymbols;
   }
}

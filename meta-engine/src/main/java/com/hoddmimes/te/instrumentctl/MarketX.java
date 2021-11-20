package com.hoddmimes.te.instrumentctl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hoddmimes.te.messages.generated.Market;
import com.hoddmimes.te.messages.generated.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MarketX extends Market
{
	private HashMap<String, SymbolX> mSymbols;

	public MarketX( JsonObject pMarketObject ) {
		super( pMarketObject.toString());
		JsonArray jSymbolArr = pMarketObject.get("symbols").getAsJsonArray();
		for (int i = 0; i < jSymbolArr.size(); i++)
		{
			JsonObject jSymObj = jSymbolArr.get(i).getAsJsonObject();
			SymbolX tSymbol = new SymbolX( jSymObj, this  );
			tSymbol.setMarketId( super.getId().get());
			mSymbols.put( tSymbol.getSymbol().get(), tSymbol);
		}
	}

	public SymbolX getSymbol( String pSymbolId ) {
		return mSymbols.get( pSymbolId );
	}


	public List<SymbolX> getSymbols() {
		List<SymbolX> tSymbols = new ArrayList<>();
		Iterator<SymbolX> tItr = mSymbols.values().iterator();
		while( tItr.hasNext() ) {
			tSymbols.add( tItr.next() );
		}
		return tSymbols;
	}

}

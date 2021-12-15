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
import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.JsonSchemaValidator;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.interfaces.MarketStates;
import com.hoddmimes.te.messages.generated.Market;
import com.hoddmimes.te.messages.generated.Symbol;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MarketX extends Market
{
	private HashMap<String, SymbolX> mSymbols;

	public MarketX( JsonObject pMarketObject ) {
		super( pMarketObject.toString());
		mSymbols = new HashMap<>();

		JsonSchemaValidator tSchemaValidator = new JsonSchemaValidator(AuxJson.navigateString( TeAppCntx.getInstance().getTeConfiguration(),
				"TeConfiguration/sessionControllerConfiguration/schemaDefinitions"));

		Logger tLogger = LogManager.getLogger(InstrumentContainer.class);

		try { tSchemaValidator.validate( Market.NAME, this.toJson().toString()); }
		catch( Exception e) {
			tLogger.fatal("failed to load market definition", e);
			System.exit(0);
		}

		JsonArray jSymbolArr = pMarketObject.get("symbols").getAsJsonArray();
		for (int i = 0; i < jSymbolArr.size(); i++)
		{
			JsonObject jSymObj = jSymbolArr.get(i).getAsJsonObject();
			SymbolX tSymbol = new SymbolX( jSymObj, this  );
			tSymbol.setMarketId( super.getId().get());
			mSymbols.put( tSymbol.getSid().get(), tSymbol);
			try { tSchemaValidator.validate( Symbol.NAME, tSymbol.toJson().toString()); }
			catch( Exception e) {
				tLogger.fatal("failed to load symbol definition\n  " + tSymbol.toJson().toString(), e);
				System.exit(0);
			}

		}
	}

	public SymbolX getSymbol( String pSid ) {
		return mSymbols.get( pSid );
	}

	public boolean isClosed() {
		return super.getState().get().contentEquals(MarketStates.CLOSED.name());
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

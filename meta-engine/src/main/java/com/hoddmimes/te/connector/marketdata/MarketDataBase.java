/*
 * Copyright (c)  Hoddmimes Solution AB 2022.
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

package com.hoddmimes.te.connector.marketdata;

import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.TeCoreService;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.interfaces.MarketDataInterface;
import com.hoddmimes.te.common.interfaces.RequestContextInterface;
import com.hoddmimes.te.common.ipc.IpcService;
import com.hoddmimes.te.messages.EngineBdxInterface;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.QueryPriceLevelsRequest;

public abstract class MarketDataBase  extends TeCoreService implements  MarketDataInterface {
	protected JsonObject mConfiguration;
	protected MarketDataConsilidator mConsilidator;


	protected MarketDataBase(JsonObject pTEConfiguration, IpcService pIpcService ) {
		super( pTEConfiguration, pIpcService);
		mConfiguration = AuxJson.navigateObject(pTEConfiguration, "TeConfiguration/marketDataConfiguration/connectorConfiguration").getAsJsonObject();
		initialize(AuxJson.navigateObject(pTEConfiguration, "TeConfiguration/marketDataConfiguration").getAsJsonObject());
	}

	protected abstract void sendPrivateBdx(String pAccountId, EngineBdxInterface pBdx);

	protected abstract void sendPublicBdx(EngineBdxInterface pBdx);


	private void initialize(JsonObject pConfiguration) {
		if (AuxJson.navigateBoolean(pConfiguration,"enablePriceLevels", true)) {
			int tLevels = AuxJson.navigateInt(pConfiguration,"priceLevels", 5);
			long tInterval = AuxJson.navigateLong(pConfiguration,"priceLevelUpdateInterval", 500L);
			mConsilidator = new MarketDataConsilidator( tLevels, tInterval);
		}
	}

	public void orderbookChanged( String pSymbol ) {
		if (mConsilidator != null) {
			mConsilidator.touch( pSymbol );
		}
	}

	public MessageInterface queryPriceLevels(QueryPriceLevelsRequest pQryRqst, RequestContextInterface pRequestContext)
	{
	  	if (mConsilidator == null) {
		    return StatusMessageBuilder.error("price level data is not configured", pQryRqst.getRef().get());
	    }
		  return mConsilidator.queryPriceLevels( pQryRqst );

	}
}

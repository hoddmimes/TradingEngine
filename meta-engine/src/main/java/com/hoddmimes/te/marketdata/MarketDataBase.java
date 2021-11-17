package com.hoddmimes.te.marketdata;

import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.interfaces.ConnectorInterface;
import com.hoddmimes.te.common.interfaces.MarketDataInterface;
import com.hoddmimes.te.common.interfaces.RequestContextInterface;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.messages.EngineBdxInterface;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.QueryPriceLevelsRequest;
import com.hoddmimes.te.messages.generated.QueryPriceLevelsResponse;

public abstract class MarketDataBase implements MarketDataInterface {
	protected JsonObject mConfiguration;
	protected MarketDataConsilidator mConsilidator;


	protected MarketDataBase(JsonObject pTEConfiguration) {
		TeAppCntx.getInstance().setMarketDataDistributor( this );
		mConfiguration = AuxJson.navigateObject(pTEConfiguration, "TeConfiguration/marketDataConfiguration/connectorConfiguration").getAsJsonObject();
		initialize(AuxJson.navigateObject(pTEConfiguration, "TeConfiguration/marketDataConfiguration").getAsJsonObject());
	}

	protected abstract void sendPrivateBdx(SessionCntxInterface pSessionCntxInterface, EngineBdxInterface pBdx);

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

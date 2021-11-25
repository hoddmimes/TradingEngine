package com.hoddmimes.te.engine;

import com.google.gson.JsonObject;
import com.hoddmimes.jaux.AuxTimestamp;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.interfaces.RequestContextInterface;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import com.hoddmimes.te.sessionctl.RequestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MatchingEngineFrontend implements MatchingEngineInterface
{
	private Logger mLog = LogManager.getLogger( MatchingEngineFrontend.class );
	private MatchingEngine              mMatchingEngine;
	private JsonObject                  mConfiguration;


	public MatchingEngineFrontend(JsonObject pTeConfiguration, MatchingEngine pMatchingEngine ) {
		mConfiguration = AuxJson.navigateObject(pTeConfiguration,"TeConfiguration/matchingEngineFrontendConfiguration");
		mMatchingEngine = pMatchingEngine;
		int tRqstCacheSize = mConfiguration.get("requestCacheSize").getAsInt();
		TeAppCntx.getInstance().setMatchingEngine(this);
	}


	@Override
	public MessageInterface executeAddOrder(AddOrderRequest pAddOrderRequest, RequestContextInterface pRequestContext) {
		MessageInterface tRspMsg = mMatchingEngine.processAddOrder( pAddOrderRequest, pRequestContext );
		return tRspMsg;
	}

	@Override
	public MessageInterface executeAmendOrder(AmendOrderRequest pAmendOrderRequest,  RequestContextInterface pRequestContext) {
		MessageInterface tRspMsg = mMatchingEngine.processAmendOrder( pAmendOrderRequest, pRequestContext );
		return tRspMsg;
	}

	@Override
	public MessageInterface executeDeleteOrder(DeleteOrderRequest pDeleteOrderRequest, RequestContextInterface pRequestContext) {
		MessageInterface tRspMsg = mMatchingEngine.processDeleteOrder( pDeleteOrderRequest, pRequestContext );
		return tRspMsg;
	}

	@Override
	public MessageInterface executeQueryOwnOrders(QueryOwnOrdersRequest pQueryOwnOrdersRequest, RequestContextInterface pRequestContext) {
		QueryOwnOrdersResponse tQueryOwnOrdersResponse = new QueryOwnOrdersResponse();
		tQueryOwnOrdersResponse.setRef( pQueryOwnOrdersRequest.getRef().get());

		List<String> tSidLst = mMatchingEngine.getOrderbookSymbolIds();
		for( String tSid : tSidLst ) {
			InternalOwnOrdersRequest tQryRqst = new InternalOwnOrdersRequest().setRef(pQueryOwnOrdersRequest.getRef().get()).setSid( tSid );
			MessageInterface tRspMsg = mMatchingEngine.processQueryOwnOrders( tQryRqst, pRequestContext );
			if (tRspMsg instanceof StatusMessage) {
				return tRspMsg;
			}

			InternalOwnOrdersResponse ior = (InternalOwnOrdersResponse) tRspMsg;
			if (!ior.getOrders().isEmpty()) {
				tQueryOwnOrdersResponse.addOrders(((InternalOwnOrdersResponse) tRspMsg).getOrders().get());
			}
		}
		return tQueryOwnOrdersResponse;
	}

	@Override
	public MessageInterface executeDeleteOrders(DeleteOrdersRequest pDeleteOrderRequest, RequestContextInterface pRequestContext) {
			MessageInterface tMsg = mMatchingEngine.processDeleteOrders( pDeleteOrderRequest, pRequestContext );
			return tMsg;
	}

	@Override
	public MessageInterface executeQueryOrderbook(QueryOrderbookRequest pQueryOrderbookRequest, RequestContextInterface pRequestContext) {
		MessageInterface tRspMsg = mMatchingEngine.processQueryOrderbook( pQueryOrderbookRequest, pRequestContext );
		return tRspMsg;
	}

	@Override
	public MessageInterface executePriceLevel(InternalPriceLevelRequest pInternalPriceLevelRequest, RequestContextInterface pRequestContext) {
		MessageInterface tRspMsg = mMatchingEngine.processPriceLevel( pInternalPriceLevelRequest, pRequestContext );
		return tRspMsg;
	}

	@Override
	public MessageInterface executeQueryBBO(QueryBBORequest pQueryBBORequest, RequestContextInterface pRequestContext) {
		MessageInterface tRspMsg = mMatchingEngine.processQueryBBO( pQueryBBORequest, pRequestContext );
		return tRspMsg;
	}
}

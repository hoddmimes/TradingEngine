package com.hoddmimes.te.engine;


import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.interfaces.RequestContextInterface;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.messages.generated.*;
import com.hoddmimes.te.sessionctl.RequestContext;

public interface MatchingEngineInterface
{
	MessageInterface executeAddOrder(AddOrderRequest pAddOrderRequest, RequestContextInterface pRequestContext );
	MessageInterface executeAmendOrder(AmendOrderRequest pAmendOrderRequest,  RequestContextInterface pRequestContext );
	MessageInterface executeDeleteOrder(DeleteOrderRequest pDeleteOrderRequest,  RequestContextInterface pRequestContext );
	MessageInterface executeDeleteOrders(DeleteOrdersRequest pDeleteOrderRequest,  RequestContextInterface pRequestContext );
	MessageInterface executeQueryOrderbook(QueryOrderbookRequest pQueryOrderbookRequest,  RequestContextInterface pRequestContext );
	MessageInterface executePriceLevel( InternalPriceLevelRequest pInternalPriceLevelRequest,  RequestContextInterface pRequestContext );
	MessageInterface executeQueryOwnOrders( QueryOwnOrdersRequest pQueryOwnOrdersRequest,  RequestContextInterface pRequestContext );
	MessageInterface executeQueryBBO( QueryBBORequest pQueryBBORequest,  RequestContextInterface pRequestContext );
}

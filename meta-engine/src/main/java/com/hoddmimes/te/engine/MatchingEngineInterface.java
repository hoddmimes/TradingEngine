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
	MessageInterface redrawCryptoRequest( CryptoReDrawRequest pCryptoReDrawRequest, RequestContextInterface pRequestContext );
}

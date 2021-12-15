
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

package com.hoddmimes.te.messages.generated;

import com.hoddmimes.jsontransform.*;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.NameNotFoundException;

	
import com.hoddmimes.te.messages.generated.*;
	
import com.hoddmimes.te.messages.generated.*;
	
import com.hoddmimes.te.messages.generated.*;
	

@SuppressWarnings({"WeakerAccess","unused","unchecked"})
public class MessageFactory implements MessageFactoryInterface
{
	public static Pattern JSON_MESSAGE_NAME_PATTERN = Pattern.compile("^\\s*\\{\\s*\"(\\w*)\"\\s*:\\s*\\{");


	public String getJsonMessageId( String pJString ) throws NameNotFoundException
	{
		Matcher tMatcher = JSON_MESSAGE_NAME_PATTERN.matcher(pJString);
		if (tMatcher.find()) {
		  return tMatcher.group(1);
		}
		throw new NameNotFoundException("Failed to extract message id from JSON message");
	}

	@Override
	public MessageInterface getMessageInstance(String pJsonMessageString) {
		String tMessageId = null;

		try { tMessageId = getJsonMessageId( pJsonMessageString ); }
		catch( NameNotFoundException e ) { return null; }
	
		switch( tMessageId ) 
		{

            case "AddOrderRequest":
            {
            	AddOrderRequest tMessage = new AddOrderRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "AddOrderResponse":
            {
            	AddOrderResponse tMessage = new AddOrderResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "AmendOrderRequest":
            {
            	AmendOrderRequest tMessage = new AmendOrderRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "AmendOrderResponse":
            {
            	AmendOrderResponse tMessage = new AmendOrderResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "DeleteOrderRequest":
            {
            	DeleteOrderRequest tMessage = new DeleteOrderRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "DeleteOrderResponse":
            {
            	DeleteOrderResponse tMessage = new DeleteOrderResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "DeleteOrdersRequest":
            {
            	DeleteOrdersRequest tMessage = new DeleteOrdersRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "QueryMarketsRequest":
            {
            	QueryMarketsRequest tMessage = new QueryMarketsRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "QueryMarketsResponse":
            {
            	QueryMarketsResponse tMessage = new QueryMarketsResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "QuerySymbolsRequest":
            {
            	QuerySymbolsRequest tMessage = new QuerySymbolsRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "QuerySymbolsResponse":
            {
            	QuerySymbolsResponse tMessage = new QuerySymbolsResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "QueryPriceLevelsRequest":
            {
            	QueryPriceLevelsRequest tMessage = new QueryPriceLevelsRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "QueryPriceLevelsResponse":
            {
            	QueryPriceLevelsResponse tMessage = new QueryPriceLevelsResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "QueryOrderbookRequest":
            {
            	QueryOrderbookRequest tMessage = new QueryOrderbookRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "QueryOrderbookResponse":
            {
            	QueryOrderbookResponse tMessage = new QueryOrderbookResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "QueryTradePricesRequest":
            {
            	QueryTradePricesRequest tMessage = new QueryTradePricesRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "QueryTradePricesResponse":
            {
            	QueryTradePricesResponse tMessage = new QueryTradePricesResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "QueryOwnTradesRequest":
            {
            	QueryOwnTradesRequest tMessage = new QueryOwnTradesRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "QueryOwnTradesResponse":
            {
            	QueryOwnTradesResponse tMessage = new QueryOwnTradesResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "QueryOwnOrdersRequest":
            {
            	QueryOwnOrdersRequest tMessage = new QueryOwnOrdersRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "QueryOwnOrdersResponse":
            {
            	QueryOwnOrdersResponse tMessage = new QueryOwnOrdersResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "QueryBBORequest":
            {
            	QueryBBORequest tMessage = new QueryBBORequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "QueryBBOResponse":
            {
            	QueryBBOResponse tMessage = new QueryBBOResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "SubscriptionRequest":
            {
            	SubscriptionRequest tMessage = new SubscriptionRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "SubscriptionResponse":
            {
            	SubscriptionResponse tMessage = new SubscriptionResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "StatusMessage":
            {
            	StatusMessage tMessage = new StatusMessage();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "BdxOrderbookChange":
            {
            	BdxOrderbookChange tMessage = new BdxOrderbookChange();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "OwnTrade":
            {
            	OwnTrade tMessage = new OwnTrade();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "BdxOwnTrade":
            {
            	BdxOwnTrade tMessage = new BdxOwnTrade();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "BdxOwnOrderbookChange":
            {
            	BdxOwnOrderbookChange tMessage = new BdxOwnOrderbookChange();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "BdxTrade":
            {
            	BdxTrade tMessage = new BdxTrade();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "BdxBBO":
            {
            	BdxBBO tMessage = new BdxBBO();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "BdxPriceLevel":
            {
            	BdxPriceLevel tMessage = new BdxPriceLevel();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "LogonRequest":
            {
            	LogonRequest tMessage = new LogonRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "InternalPriceLevelRequest":
            {
            	InternalPriceLevelRequest tMessage = new InternalPriceLevelRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "InternalOwnOrdersRequest":
            {
            	InternalOwnOrdersRequest tMessage = new InternalOwnOrdersRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "InternalOwnOrdersResponse":
            {
            	InternalOwnOrdersResponse tMessage = new InternalOwnOrdersResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "LogonResponse":
            {
            	LogonResponse tMessage = new LogonResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "InternalPriceLevelResponse":
            {
            	InternalPriceLevelResponse tMessage = new InternalPriceLevelResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "ContainerTrade":
            {
            	ContainerTrade tMessage = new ContainerTrade();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtStatusResponse":
            {
            	MgmtStatusResponse tMessage = new MgmtStatusResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtConfigurationBdx":
            {
            	MgmtConfigurationBdx tMessage = new MgmtConfigurationBdx();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtConfigurationPingBdx":
            {
            	MgmtConfigurationPingBdx tMessage = new MgmtConfigurationPingBdx();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtGetMarketsRequest":
            {
            	MgmtGetMarketsRequest tMessage = new MgmtGetMarketsRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtGetMarketsResponse":
            {
            	MgmtGetMarketsResponse tMessage = new MgmtGetMarketsResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtSetMarketsRequest":
            {
            	MgmtSetMarketsRequest tMessage = new MgmtSetMarketsRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtSetMarketsResponse":
            {
            	MgmtSetMarketsResponse tMessage = new MgmtSetMarketsResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtGetSymbolsRequest":
            {
            	MgmtGetSymbolsRequest tMessage = new MgmtGetSymbolsRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtGetSymbolsResponse":
            {
            	MgmtGetSymbolsResponse tMessage = new MgmtGetSymbolsResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtSetSymbolRequest":
            {
            	MgmtSetSymbolRequest tMessage = new MgmtSetSymbolRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtSetSymbolResponse":
            {
            	MgmtSetSymbolResponse tMessage = new MgmtSetSymbolResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtGetAccountsRequest":
            {
            	MgmtGetAccountsRequest tMessage = new MgmtGetAccountsRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtGetAccountsResponse":
            {
            	MgmtGetAccountsResponse tMessage = new MgmtGetAccountsResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtSetAccountsRequest":
            {
            	MgmtSetAccountsRequest tMessage = new MgmtSetAccountsRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtSetAccountsResponse":
            {
            	MgmtSetAccountsResponse tMessage = new MgmtSetAccountsResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtGetAccountOrdersRequest":
            {
            	MgmtGetAccountOrdersRequest tMessage = new MgmtGetAccountOrdersRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtGetAccountOrdersResponse":
            {
            	MgmtGetAccountOrdersResponse tMessage = new MgmtGetAccountOrdersResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtDeleteOrderRequest":
            {
            	MgmtDeleteOrderRequest tMessage = new MgmtDeleteOrderRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtDeleteOrderResponse":
            {
            	MgmtDeleteOrderResponse tMessage = new MgmtDeleteOrderResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtDeleteAllOrdersRequest":
            {
            	MgmtDeleteAllOrdersRequest tMessage = new MgmtDeleteAllOrdersRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtDeleteAllOrdersResponse":
            {
            	MgmtDeleteAllOrdersResponse tMessage = new MgmtDeleteAllOrdersResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtGetTradesRequest":
            {
            	MgmtGetTradesRequest tMessage = new MgmtGetTradesRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtGetTradesResponse":
            {
            	MgmtGetTradesResponse tMessage = new MgmtGetTradesResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtRevertTradeRequest":
            {
            	MgmtRevertTradeRequest tMessage = new MgmtRevertTradeRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtRevertTradeResponse":
            {
            	MgmtRevertTradeResponse tMessage = new MgmtRevertTradeResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtGetLogMessagesRequest":
            {
            	MgmtGetLogMessagesRequest tMessage = new MgmtGetLogMessagesRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtGetLogMessagesResponse":
            {
            	MgmtGetLogMessagesResponse tMessage = new MgmtGetLogMessagesResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtAddAccountRequest":
            {
            	MgmtAddAccountRequest tMessage = new MgmtAddAccountRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtAddAccountResponse":
            {
            	MgmtAddAccountResponse tMessage = new MgmtAddAccountResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtUpdateAccountRequest":
            {
            	MgmtUpdateAccountRequest tMessage = new MgmtUpdateAccountRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtUpdateAccountResponse":
            {
            	MgmtUpdateAccountResponse tMessage = new MgmtUpdateAccountResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtDeleteAccountRequest":
            {
            	MgmtDeleteAccountRequest tMessage = new MgmtDeleteAccountRequest();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            case "MgmtDeleteAccountResponse":
            {
            	MgmtDeleteAccountResponse tMessage = new MgmtDeleteAccountResponse();
            	tMessage.decode( new JsonDecoder(pJsonMessageString));
            	return tMessage;
            }
			
            default:
              return null;
		}	
	}
}


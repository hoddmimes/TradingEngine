
package com.hoddmimes.te.messages.generated;

import com.hoddmimes.jsontransform.*;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.NameNotFoundException;

	
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
			
            default:
              return null;
		}	
	}
}


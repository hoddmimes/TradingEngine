package com.hoddmimes.te.connector.rest;

import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.TeException;
import com.hoddmimes.te.common.interfaces.ConnectorInterface;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.TextMessage;

import javax.servlet.http.HttpSession;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@Configuration
@RequestMapping("/te")

public class RestMessageController
{
	private Logger mLog = LogManager.getLogger( RestMessageController.class );
	private ConnectorInterface.ConnectorCallbackInterface mCallback;
	private JsonObject mConfiguration;
	private MessageFactory mMessageFactory;
	private AtomicInteger mInternalRef = new AtomicInteger(1);


	public RestMessageController() {
		mCallback = TeAppCntx.getInstance().getSessionController();
		mConfiguration = AuxJson.navigateObject( TeAppCntx.getInstance().getTeConfiguration(), "TeConfiguration/connectorConfiguration/configuration");
		mMessageFactory = new MessageFactory();
	}

	/**
	 * ======================================================================================
	 * Rest Controller end points
	 * ======================================================================================
	 */
	@PostMapping( path = "/logon" )
	ResponseEntity<String> logon(HttpSession pSession, @RequestBody String pJsonRqstString )
	{
		String jRqstMsgString  = AuxJson.tagMessageBody(LogonRequest.NAME, pJsonRqstString);
		try {
			LogonResponse tRspMsg = mCallback.logon(pSession.getId(), jRqstMsgString);
			SessionCntxInterface tSessCntx = mCallback.getSessionCntx(pSession.getId());
			pSession.setAttribute(TeFilter.TE_SESS_CNTX, tSessCntx);
			pSession.setMaxInactiveInterval(AuxJson.navigateInt(mConfiguration, "inactivityTimer"));
			return buildResponse(tRspMsg);
		}
		catch( TeException te) {
			return new ResponseEntity<String>( AuxJson.getMessageBody(te.getStatusMessage().toJson()).toString(), HttpStatus.resolve(te.getStatusCode()) );
		}
	}

	@PostMapping( path = "/addOrder" )
	ResponseEntity<?> addOrder(HttpSession pSession, @RequestBody String pJsonRqstString ) {
		String jRqstMsgString  = AuxJson.tagMessageBody(AddOrderRequest.NAME, pJsonRqstString);
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), jRqstMsgString );
		return buildResponse( tResponseMessage );
	}

	@PostMapping( path = "/amendOrder" )
	ResponseEntity<?> amendOrder(HttpSession pSession, @RequestBody String pJsonRqstString ) {
		String jRqstMsgString  = AuxJson.tagMessageBody(AmendOrderRequest.NAME, pJsonRqstString);
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), jRqstMsgString );
		return buildResponse( tResponseMessage );
	}

	@PostMapping( path = "/deleteOrder" )
	ResponseEntity<?> deleteOrder(HttpSession pSession, @RequestBody String pJsonRqstString ) {
		String jRqstMsgString  = AuxJson.tagMessageBody(DeleteOrderRequest.NAME, pJsonRqstString);
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), jRqstMsgString );
		return buildResponse( tResponseMessage );
	}

	@PostMapping( path = "/deleteOrders" )
	ResponseEntity<?> deleteOrders(HttpSession pSession, @RequestBody String pJsonRqstString ) {
		String jRqstMsgString  = AuxJson.tagMessageBody(DeleteOrdersRequest.NAME, pJsonRqstString);
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), jRqstMsgString );
		return buildResponse( tResponseMessage );
	}

	@GetMapping( path = "/querySymbols" )
	ResponseEntity<?> querySymbols(HttpSession pSession ) {
		QuerySymbolsRequest tRequest = new QuerySymbolsRequest().setRef( String.valueOf( mInternalRef.getAndIncrement()));
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(),  tRequest.toJson().toString() );
		return buildResponse( tResponseMessage );
	}


	@PostMapping( path = "/queryTradePrices" )
	ResponseEntity<?> queryTradePrices(HttpSession pSession, @RequestBody String pJsonRqstString ) {
		String jRqstMsgString  = AuxJson.tagMessageBody(QueryTradePricesRequest.NAME, pJsonRqstString);
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), jRqstMsgString );
		return buildResponse( tResponseMessage );
	}

	@PostMapping( path = "/queryTradePrice" )
	ResponseEntity<?> queryTradePrice(HttpSession pSession, @RequestBody String pJsonRqstString ) {
		String jRqstMsgString  = AuxJson.tagMessageBody(QueryTradePriceRequest.NAME, pJsonRqstString);
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), jRqstMsgString );
		return buildResponse( tResponseMessage );
	}

	@PostMapping( path = "/queryOrderbook" )
	ResponseEntity<?> queryOrderbook(HttpSession pSession, @RequestBody String pJsonRqstString ) {
		String jRqstMsgString  = AuxJson.tagMessageBody(QueryOrderbookRequest.NAME, pJsonRqstString);
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), jRqstMsgString );
		return buildResponse( tResponseMessage );
	}

	@PostMapping( path = "/queryLevels" )
	ResponseEntity<?> queryPriceLevels(HttpSession pSession, @RequestBody String pJsonRqstString ) {
		String jRqstMsgString = AuxJson.tagMessageBody(QueryPriceLevelsRequest.NAME, pJsonRqstString);
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), jRqstMsgString);
		return buildResponse(tResponseMessage);
	}

	@PostMapping( path = "/queryOwnTrades" )
	ResponseEntity<?> QueryOwnTrades(HttpSession pSession, @RequestBody String pJsonRqstString ) {
		String jRqstMsgString = AuxJson.tagMessageBody(QueryOwnTradesRequest.NAME, pJsonRqstString);
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), jRqstMsgString);
		return buildResponse(tResponseMessage);
	}

	@PostMapping( path = "/queryOwnOrders" )
	ResponseEntity<?> QueryOwnOrders(HttpSession pSession, @RequestBody String pJsonRqstString ) {
		String jRqstMsgString = AuxJson.tagMessageBody(QueryOwnOrdersRequest.NAME, pJsonRqstString);
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), jRqstMsgString);
		return buildResponse(tResponseMessage);
	}

	private ResponseEntity<String> buildStatusMessageResponse( StatusMessage pStsMsg ) {
		if (pStsMsg.getIsOk().get()) {
			return new ResponseEntity<>( AuxJson.getMessageBody(pStsMsg.toJson()).toString(), HttpStatus.OK );
		} else {
			return new ResponseEntity<>( AuxJson.getMessageBody(pStsMsg.toJson()).toString(), HttpStatus.BAD_REQUEST );
		}
	}

	private ResponseEntity<String> buildResponse(MessageInterface pResponseMessage ) {
		if (pResponseMessage instanceof StatusMessage) {
			return buildStatusMessageResponse((StatusMessage) pResponseMessage);
		}
		return new ResponseEntity<>( AuxJson.getMessageBody(pResponseMessage.toJson()).toString(), HttpStatus.OK );
	}


}

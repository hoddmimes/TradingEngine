package com.hoddmimes.te.connector.rest;

import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
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

@RestController
@Configuration
@RequestMapping("/te")

public class RestMessageController
{
	private Logger mLog = LogManager.getLogger( RestMessageController.class );
	private ConnectorInterface.ConnectorCallbackInterface mCallback;
	private JsonObject mConfiguration;
	private MessageFactory mMessageFactory;


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
	ResponseEntity<?> logon(HttpSession pSession, @RequestBody String pJsonRqstString )
	{
		String jRqstMsgString  = AuxJson.tagMessageBody(LogonRequest.NAME, pJsonRqstString);
		LogonResponse tRspMsg =  mCallback.logon( pSession.getId(), jRqstMsgString );
		if (tRspMsg.getIsOk().get()) {
			// Login Ok
			SessionCntxInterface tSessCntx = mCallback.getSessionCntx( pSession.getId() );
			pSession.setAttribute( TeFilter.TE_SESS_CNTX, tSessCntx);
			pSession.setMaxInactiveInterval( AuxJson.navigateInt( mConfiguration, "inactivityTimer"));
		}
		return buildResponse( tRspMsg );
	}

	@PostMapping( path = "/addOrder" )
	ResponseEntity<?> addOrder(HttpSession pSession, @RequestBody String pJsonRqstString ) {
		String jRqstMsgString  = AuxJson.tagMessageBody(AddOrderRequest.NAME, pJsonRqstString);
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), jRqstMsgString );
		return buildResponse( tResponseMessage );
	}

	@PostMapping( path = "/queryLevels" )
	ResponseEntity<?> queryPriceLevels(HttpSession pSession, @RequestBody String pJsonRqstString ) {
		String jRqstMsgString  = AuxJson.tagMessageBody(QueryPriceLevelsRequest.NAME, pJsonRqstString);
		String tRef = AuxJson.getMessageRef(jRqstMsgString);

		QueryPriceLevelsRequest tQryPLRqst = null;
		try {
			tQryPLRqst = (QueryPriceLevelsRequest) mCallback.validateMessage(jRqstMsgString);
		}
		catch( Exception e) {
			mLog.error("invalid " + QueryPriceLevelsRequest.NAME + " request, reason: " + e.getMessage(), e);
			StatusMessage tStsMsg = StatusMessageBuilder.error("invalid "  + QueryPriceLevelsRequest.NAME, tRef, e );
			return buildResponse( tStsMsg );
		}

		MessageInterface tResponseMessage = null;
		TeAppCntx.getInstance().getMarketDataDistributor().queryPriceLevels( tQryPLRqst );
		return buildResponse( tResponseMessage );
	}


	private ResponseEntity<?> buildStatusMessageResponse( StatusMessage pStsMsg ) {
		if (pStsMsg.getIsOk().get()) {
			return new ResponseEntity<>( AuxJson.getMessageBody(pStsMsg.toJson()).toString(), HttpStatus.OK );
		} else {
			return new ResponseEntity<>( AuxJson.getMessageBody(pStsMsg.toJson()).toString(), HttpStatus.BAD_REQUEST );
		}
	}

	private ResponseEntity<?> buildResponse(MessageInterface pResponseMessage ) {
		if (pResponseMessage instanceof StatusMessage) {
			return buildStatusMessageResponse((StatusMessage) pResponseMessage);
		}
		if (pResponseMessage instanceof LogonResponse) {
			LogonResponse tLogonRsp = (LogonResponse) pResponseMessage;
			if (!tLogonRsp.getIsOk().get()) {
				new ResponseEntity<>( AuxJson.getMessageBody(pResponseMessage.toJson()).toString(), HttpStatus.UNAUTHORIZED );
			} else {
				new ResponseEntity<>( AuxJson.getMessageBody(pResponseMessage.toJson()).toString(), HttpStatus.OK );
			}
		}
		return new ResponseEntity<>( AuxJson.getMessageBody(pResponseMessage.toJson()).toString(), HttpStatus.OK );
	}


}

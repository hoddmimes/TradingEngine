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

package com.hoddmimes.te.connector.rest;

import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.TeException;
import com.hoddmimes.te.common.db.TEDB;
import com.hoddmimes.te.common.interfaces.ConnectorInterface;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.common.interfaces.TeIpcServices;
import com.hoddmimes.te.common.ipc.IpcProxy;
import com.hoddmimes.te.common.ipc.IpcService;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@Configuration
@RequestMapping("/te-trading")

public class TeRestMessageController
{
	private Logger mLog = LogManager.getLogger( TeRestMessageController.class );
	private ConnectorInterface.ConnectorCallbackInterface mCallback;
	private JsonObject mConfiguration;
	private MessageFactory mMessageFactory;
	private AtomicInteger mInternalRef = new AtomicInteger(1);
	private IpcService mIpcService;

	public TeRestMessageController() {
		mCallback = TeAppCntx.getInstance().getSessionController();
		mConfiguration = AuxJson.navigateObject( TeAppCntx.getInstance().getTeConfiguration(), "TeConfiguration/connectorConfiguration/configuration");
		mMessageFactory = new MessageFactory();
		mIpcService = TeAppCntx.getInstance().getIpcService();
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
		LogonRequest tLogonRequest = (LogonRequest) mMessageFactory.getMessageInstance( jRqstMsgString );
		if (tLogonRequest.getRef().isEmpty()) {
			tLogonRequest.setRef( String.valueOf(mInternalRef.getAndIncrement()));
		}

		try {
			LogonResponse tRspMsg = mCallback.logon(pSession.getId(), tLogonRequest.toJson().toString());
			tRspMsg.setRef( null );
			SessionCntxInterface tSessCntx = mCallback.getSessionContext(pSession.getId());
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
		AmendOrderRequest tAmndRqst = new AmendOrderRequest( jRqstMsgString );
		if (tAmndRqst.getRef().isEmpty()) {
			tAmndRqst.setRef( String.valueOf(mInternalRef.getAndIncrement()));
		}
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), tAmndRqst.toJson().toString() );
		return buildResponse( tResponseMessage );
	}

	@PostMapping( path = "/deleteOrder" )
	ResponseEntity<?> deleteOrder(HttpSession pSession, @RequestBody String pJsonRqstString ) {
		String jRqstMsgString  = AuxJson.tagMessageBody(DeleteOrderRequest.NAME, pJsonRqstString);
		DeleteOrderRequest tDelOrderRqst = new DeleteOrderRequest( jRqstMsgString );
		if (tDelOrderRqst.getRef().isEmpty()) {
			tDelOrderRqst.setRef( String.valueOf(mInternalRef.getAndIncrement()));
		}
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), tDelOrderRqst.toJson().toString() );
		return buildResponse( tResponseMessage );
	}

	@DeleteMapping( path = "/deleteAllOrders/{marketId}" )
	ResponseEntity<?> deleteOrders(HttpSession pSession, @PathVariable Integer marketId, @RequestParam(required = false) String sid ) {
		DeleteOrdersRequest tRqstMsg = new DeleteOrdersRequest();
		tRqstMsg.setRef( String.valueOf(this.mInternalRef.getAndIncrement()));
		tRqstMsg.setMarket(marketId);

		if (sid != null) {
			tRqstMsg.setSid( sid );
		}

		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), tRqstMsg.toString() );
		return buildResponse( tResponseMessage );
	}

	@GetMapping( path = "/queryMarkets" )
	ResponseEntity<?> querySymbols(HttpSession pSession ) {
		QueryMarketsRequest tRequest = new QueryMarketsRequest();
		tRequest.setRef( String.valueOf( mInternalRef.getAndIncrement()));
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(),  tRequest.toJson().toString() );
		return buildResponse( tResponseMessage );
	}

	@GetMapping( path = "/querySymbols/{marketId}" )
	ResponseEntity<?> querySymbols(HttpSession pSession, @PathVariable Integer marketId ) {
		QuerySymbolsRequest tRequest = new QuerySymbolsRequest();
		tRequest.setRef( String.valueOf( mInternalRef.getAndIncrement()));
		tRequest.setMarketId( marketId );
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(),  tRequest.toJson().toString() );
		return buildResponse( tResponseMessage );
	}




	@GetMapping( path = "/queryTradePrices/{marketId}" )
	ResponseEntity<?> queryTradePrices(HttpSession pSession, @PathVariable Integer marketId, @RequestParam(required = false) String sid ) {
		QueryTradePricesRequest pRqst = new QueryTradePricesRequest();
		pRqst.setRef( String.valueOf( mInternalRef.getAndIncrement()));
		pRqst.setMarketId( marketId );
		if (sid != null) {
			pRqst.setSid(sid);
		}

		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), pRqst.toString() );
		if (tResponseMessage instanceof QueryTradePricesResponse) {
			((QueryTradePricesResponse)tResponseMessage).setRef(null);
		}
		return buildResponse( tResponseMessage );
	}

	@GetMapping( path = "/queryOrderbook/{sid}" )
	ResponseEntity<?> queryOrderbook(HttpSession pSession, @PathVariable String sid ) {
		QueryOrderbookRequest tRqstMsg = new QueryOrderbookRequest();
		tRqstMsg.setRef( String.valueOf( mInternalRef.getAndIncrement()));
		tRqstMsg.setSid( sid );

		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), tRqstMsg.toJson().toString() );
		if (tResponseMessage instanceof QueryOrderbookResponse) {
			((QueryOrderbookResponse)tResponseMessage).setRef(null);
		}
		return buildResponse( tResponseMessage );
	}

	@GetMapping( path = "/queryPriceLevels/{market}" )
	ResponseEntity<?> queryPriceLevels(HttpSession pSession, @PathVariable Integer market ) {
		QueryPriceLevelsRequest tRqstMsg = new QueryPriceLevelsRequest();
		tRqstMsg.setRef( String.valueOf( mInternalRef.getAndIncrement()));
		tRqstMsg.setMarketId( market );
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), tRqstMsg.toJson().toString());

		if (tResponseMessage instanceof QueryPriceLevelsResponse) {
			((QueryPriceLevelsResponse) tResponseMessage).setRef(null);
		}
		return buildResponse(tResponseMessage);
	}

	@GetMapping( path = "/queryOwnTrades/{market}" )
	ResponseEntity<?> QueryOwnTrades(HttpSession pSession, @PathVariable Integer market,  @RequestParam(required = false) String sid) {
		QueryOwnTradesRequest tRqstMsg = new QueryOwnTradesRequest();
		tRqstMsg.setRef( String.valueOf( mInternalRef.getAndIncrement()));
		tRqstMsg.setMarketId( market );
		if (sid != null) {
			tRqstMsg.setRef( sid );
		}
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), tRqstMsg.toJson().toString());
		if (tResponseMessage instanceof QueryOwnTradesResponse) {
			((QueryOwnTradesResponse) tResponseMessage).setRef(null);
		}
		return buildResponse(tResponseMessage);
	}

	@GetMapping( path = "/queryOwnOrders/{market}" )
	ResponseEntity<?> QueryOwnOrders(HttpSession pSession, @PathVariable Integer market ) {
		QueryOwnOrdersRequest tRqstMsg = new QueryOwnOrdersRequest();
		tRqstMsg.setRef( String.valueOf( mInternalRef.getAndIncrement()));
		tRqstMsg.setMarketId( market );

		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), tRqstMsg.toJson().toString());
		if (tResponseMessage instanceof QueryOwnOrdersResponse) {
			((QueryOwnOrdersResponse) tResponseMessage).setRef(null);
		}
		return buildResponse(tResponseMessage);
	}

	@GetMapping( path = "/queryBBO/{market}" )
	ResponseEntity<?> QueryBBO(HttpSession pSession, @PathVariable Integer market ) {
		QueryBBORequest tRqstMsg = new QueryBBORequest();
		tRqstMsg.setRef( String.valueOf( mInternalRef.getAndIncrement()));
		tRqstMsg.setMarketId( market );

		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), tRqstMsg.toJson().toString());
		if (tResponseMessage instanceof QueryBBOResponse) {
			((QueryBBOResponse) tResponseMessage).setRef(null);
		}
		return buildResponse(tResponseMessage);
	}




	@GetMapping( path = "/getDepositEntry/{coin}" )
	ResponseEntity<String> getPaymentEntry(HttpSession pSession, @PathVariable String coin) {
		GetDepositEntryRequest tPayEntryRqst = new GetDepositEntryRequest();
		tPayEntryRqst.setRef(String.valueOf(mInternalRef.getAndIncrement()));

		if (!AuxJson.navigateBoolean( TeAppCntx.getInstance().getTeConfiguration(),"TeConfiguration/cryptoGateway/enable")) {
			return buildResponse(StatusMessageBuilder.error("Crypto functionality is not enabled", null));
		}

		if ((coin == null) && ((!coin.contentEquals(TEDB.CoinType.BTC.name())) && (!coin.contentEquals(TEDB.CoinType.ETH.name())))) {
			return buildResponse(StatusMessageBuilder.error("Invalid coin type", null));
		}

		tPayEntryRqst.setAccountId( mCallback.getSessionContext( pSession.getId()).getAccount());
		tPayEntryRqst.setCoin( coin );

		MessageInterface tResponse =  TeAppCntx.getInstance().getCryptoGateway().setDepositAddressEntry( tPayEntryRqst );
		return buildResponse( tResponse );
	}

	@GetMapping( path = "/setRedrawEntry/{coin}/{address}" )
	ResponseEntity<String> setRedrawEntry(HttpSession pSession, @PathVariable String coin, @PathVariable String address) {
		SetReDrawEntryRequest tRedrawEntryRqst = new SetReDrawEntryRequest();
		tRedrawEntryRqst.setRef(String.valueOf(mInternalRef.getAndIncrement()));

		if (!AuxJson.navigateBoolean( TeAppCntx.getInstance().getTeConfiguration(),"TeConfiguration/cryptoGateway/enable")) {
			return buildResponse(StatusMessageBuilder.error("Crypto functionality is not enabled", null));
		}

		if ((coin == null) && ((!coin.contentEquals(TEDB.CoinType.BTC.name())) && (!coin.contentEquals(TEDB.CoinType.ETH.name())))) {
			return buildResponse(StatusMessageBuilder.error("Invalid coin asset", null));
		}

		tRedrawEntryRqst.setAddress( address );
		tRedrawEntryRqst.setAccountId( mCallback.getSessionContext( pSession.getId()).getAccount());
		tRedrawEntryRqst.setCoin( coin );

		MessageInterface tResponse =  TeAppCntx.getInstance().getCryptoGateway().setRedrawAddressEntry( tRedrawEntryRqst );
		return buildResponse( tResponse );
	}

	@PostMapping( path = "/redrawCrypto" )
	ResponseEntity<?> redrawCrypto(HttpSession pSession, @RequestBody String pJsonRqstString ) {
		if (!AuxJson.navigateBoolean( TeAppCntx.getInstance().getTeConfiguration(),"TeConfiguration/cryptoGateway/enable")) {
			return buildResponse(StatusMessageBuilder.error("Crypto functionality is not enabled", null));
		}

		String jRqstMsgString  = AuxJson.tagMessageBody(CryptoReDrawRequest.NAME, pJsonRqstString);
		CryptoReDrawRequest tRedrawCryptoRequest = new CryptoReDrawRequest( jRqstMsgString );
		if (tRedrawCryptoRequest.getRef().isEmpty()) {
			tRedrawCryptoRequest.setRef( String.valueOf(mInternalRef.getAndIncrement()));
		}
		MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), tRedrawCryptoRequest.toJson().toString() );
		return buildResponse( tResponseMessage );
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

	private ResponseEntity<String> buildResponse(JsonObject pResponseMessage ) {
		return new ResponseEntity<>( pResponseMessage.toString(), HttpStatus.OK );
	}



}

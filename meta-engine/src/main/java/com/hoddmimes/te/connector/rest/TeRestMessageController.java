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
import com.hoddmimes.te.common.Crypto;
import com.hoddmimes.te.common.TeException;
import com.hoddmimes.te.common.interfaces.ConnectorInterface;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.common.interfaces.TeService;
import com.hoddmimes.te.common.ipc.IpcService;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import com.hoddmimes.te.sessionctl.SessionController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
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
		mCallback = (ConnectorInterface.ConnectorCallbackInterface) TeAppCntx.getInstance().getService(TeService.SessionService);
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


	/**
	 * This entry is called when a client like to retreive and register a deposit entry
	 * For Bitcoin the user will receive a unique deposit address being assoiciated with the client account
	 * For Ethereum the address retorn will be the one and only address of the TE ethereum receive address. Instead the client
	 * will be identified by the address the deposit is sent from, som the {fromAddress} is only applicable if the coin type is ETH
	 * @param pSession
	 * @param coin BTC or ETH
	 * @return GetDepositEntryResponse
	 */

	@GetMapping( path = "/addDepositEntry/{coin}/{fromAddress}" )
	ResponseEntity<String> getPaymentEntry(HttpSession pSession, @PathVariable String coin, @PathVariable String fromAddress) {
		return addPaymentEntry(pSession, coin, fromAddress);
	}

	@GetMapping( path = "/addDepositEntry/{coin}" )
	ResponseEntity<String> getPaymentEntry(HttpSession pSession, @PathVariable String coin) {
		return addPaymentEntry(pSession, coin, null);
	}


	private ResponseEntity<String> addPaymentEntry(HttpSession pSession, String coin, String fromAddress) {
		try {
			GetDepositEntryRequest tPayEntryRqst = new GetDepositEntryRequest();
			tPayEntryRqst.setRef(String.valueOf(mInternalRef.getAndIncrement()));

			if (coin == null) {
				return buildResponse(StatusMessageBuilder.error("No crypto coin identifier was specified in the GET request", null));
			}

			Crypto.CoinType tCoinType = Crypto.CoinType.valueOf(coin);
			if (tCoinType == null) {
				return buildResponse(StatusMessageBuilder.error("Unknown coin type specified", null));
			}


			if (fromAddress == null) {
				if (coin.contentEquals(Crypto.CoinType.ETH.name())) {
					return buildResponse(StatusMessageBuilder.error("For ETH a from address has to be specified", null));
				}
			} else {
				if (coin.contentEquals(Crypto.CoinType.BTC.name())) {
					return buildResponse(StatusMessageBuilder.error("For BTC a from address must not be specified", null));
				}
				tPayEntryRqst.setFromAddress(fromAddress);
			}

			if (!TeAppCntx.getInstance().getCryptoGateway().isEnabled(tCoinType)) {
				return buildResponse(StatusMessageBuilder.error("Crypto functionality is not enabled", null));
			}

			tPayEntryRqst.setAccountId(mCallback.getSessionContext(pSession.getId()).getAccount());
			tPayEntryRqst.setCoin(coin);


			MessageInterface tResponse = TeAppCntx.getCryptoGateway().addDepositEntry(tPayEntryRqst);
			return buildResponse(tResponse);
		}
		catch( Throwable e) {
			mLog.error("(addPaymentEntry) internal error", e);
			return buildResponse( StatusMessageBuilder.error("(addPaymentEntry) internal error, reason: " + e.getMessage(), null));
		}
	}

	/**
	 * In order for a client to redraw coins there must be a confirmed redraw payment entry defined
	 * This method is invoked when the client would like to add a redraw payment entry.
	 * And before the client can redrawn any holdings the entry needs to be confirmed. Confirmation requests are
	 * sent out via mail seprately
	 * @param pSession
	 * @param coin
	 * @param  (redraw address)
	 * @return SetRedrawEntryResponse
	 */

	@GetMapping( path = "/addRedrawEntry/{coin}/{address}" )
	ResponseEntity<String> addRedrawEntry(HttpSession pSession, @PathVariable String coin, @PathVariable String address) {
		SetRedrawEntryRequest tRedrawEntryRqst = new SetRedrawEntryRequest();
		tRedrawEntryRqst.setRef(String.valueOf(mInternalRef.getAndIncrement()));

		if (coin == null) {
			return buildResponse(StatusMessageBuilder.error("No crypto coin identifier was specified in the GET request", null));
		}

		Crypto.CoinType tCoinType = Crypto.CoinType.valueOf(coin);
		if ( tCoinType == null) {
			return buildResponse(StatusMessageBuilder.error("Unknown coin type specified", null));
		}
		if ( address == null) {
			return buildResponse(StatusMessageBuilder.error("No redraw address specified", null));
		}

		if (!TeAppCntx.getInstance().getCryptoGateway().isEnabled( tCoinType )) {
			return buildResponse(StatusMessageBuilder.error("Crypto functionality is not enabled", null));
		}

		tRedrawEntryRqst.setAddress( address );
		tRedrawEntryRqst.setAccountId( mCallback.getSessionContext( pSession.getId()).getAccount());
		tRedrawEntryRqst.setCoin( coin );

		MessageInterface tResponse =  TeAppCntx.getInstance().getCryptoGateway().addRedrawEntry( tRedrawEntryRqst );
		return buildResponse( tResponse );
	}


	/**
	 * Invoked by clients to redraw coins from its account. In order to do so a payment entry must previously  been
	 * added and confirmed by the client.
	 * @param pSession
	 * @param pJsonRqstString
	 * @return
	 */

	@PostMapping( path = "/redrawCrypto" )
	ResponseEntity<?> redrawCrypto(HttpSession pSession, @RequestBody String pJsonRqstString ) {
		try {
			String jRqstMsgString = AuxJson.tagMessageBody(CryptoRedrawRequest.NAME, pJsonRqstString);
			CryptoRedrawRequest tRedrawCryptoRequest = new CryptoRedrawRequest(jRqstMsgString);

			if (tRedrawCryptoRequest.getRef().isEmpty()) {
				tRedrawCryptoRequest.setRef(String.valueOf(mInternalRef.getAndIncrement()));
			}

			MessageInterface tResponseMessage = mCallback.connectorMessage(pSession.getId(), tRedrawCryptoRequest.toJson().toString());
			return buildResponse(tResponseMessage);
		} catch (Throwable t) {
			mLog.fatal("(redrawCrypto) internal error", t);
			throw t;
		}
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

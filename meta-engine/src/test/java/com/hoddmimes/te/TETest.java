/*
 * Copyright (c)  Hoddmimes Solution AB 2022.
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

package com.hoddmimes.te;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hoddmimes.te.TradingEngine;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.TeException;
import com.hoddmimes.te.common.transport.http.TeHttpClient;
import com.hoddmimes.te.common.transport.http.TeRequestException;
import com.hoddmimes.te.common.transport.http.TeWebsocketClient;
import org.junit.jupiter.api.*;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import java.io.IOException;
import java.sql.SQLOutput;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

public class TETest implements TeWebsocketClient.WssCallback {
	private static final String TE_HTTP_URI = "https://localhost:8883/te/";
	private static final String TE_WSS_URI = "wss://localhost:8883/marketdata";

	private  TeWebsocketClient   mWssClient;
	private  TeHttpClient        mHttpClient;
	private  TeThread            mTeThread;
	private  LinkedList<BdxCondition> mBdxConditions;
	private  String mOrderId;



	@BeforeAll
	public void TeSetup() {
		System.out.println("TesSetup");

		System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");
		System.setProperty("java.util.logging.SimpleFormatter.format", "");


		mBdxConditions = new LinkedList<>();


		mTeThread = new TeThread();

		mTeThread.start();

		mTeThread.waitForTeToStart();

	}

	@AfterAll
	public void cleanUp(){
		System.out.println("All Done");
		try { Thread.sleep(3000L);}
		catch( InterruptedException ie) {}
	}

	@Test
	@Order(1)
	public void test_connect() throws Exception {

		mHttpClient = new TeHttpClient( TE_HTTP_URI, false);
		JsonObject jRsp = mHttpClient.post( toJsonString("{'account':'test', 'password': 'test','ref' : 'test-1'}"),"logon");
		if (!jRsp.has("sessionAuthId")) {
			assertTrue(false, ("Login failure: " + jRsp.toString()));
			System.exit(0);
		}
		String tSessionAuthId = jRsp.get("sessionAuthId").getAsString();
		mWssClient = new TeWebsocketClient( TE_WSS_URI, tSessionAuthId, this );
	}


	@Test
	@Order(2)
	public void test_subscriptions() throws Exception
	{
		BdxCondition bc = addBdxCondition("SubscriptionResponse", "{'key':'isOk','value':true}");
		mWssClient.sendMessage( toJsonString("{'command':'ADD', 'topic' :  '/BdxBBO/...'}"));
		assertTrue( bc.matchConditionUntil(3000L),"subscription failed BdxBBO/...");


		bc = addBdxCondition("SubscriptionResponse", "{'key':'isOk','value':true}");
		mWssClient.sendMessage( toJsonString("{'command':'ADD', 'topic' :  '/BdxTrade/...'}"));
		assertTrue( bc.matchConditionUntil(3000L), "subscription failed BdxTrade/...");

		bc = addBdxCondition("SubscriptionResponse", "{'key':'isOk','value':true}");
		mWssClient.sendMessage( toJsonString("{'command':'ADD', 'topic' :  '/BdxPriceLevel/...'}"));
		assertTrue( bc.matchConditionUntil(3000L), "subscription failed BdxPriceLevel/...");


		bc = addBdxCondition("SubscriptionResponse", "{'key':'isOk','value':true}");
		mWssClient.sendMessage( toJsonString("{'command':'ADD', 'topic' :  '/BdxOwnOrderbookChange/...'}"));
		assertTrue( bc.matchConditionUntil(3000L), "subscription failed BdxOwnOrderbookChange/...");

		bc = addBdxCondition("SubscriptionResponse", "{'key':'isOk','value':true}");
		mWssClient.sendMessage( toJsonString("{'command':'ADD', 'topic' :  '/BdxOwnTrade/...'}"));
		assertTrue( bc.matchConditionUntil(3000L), "subscription failed BdxOwnTrade/...");
	}


	@Test
	@Order(3)
	public void test_query_markets() throws IOException {
		// {"markets":[{"id":1,"name":"Equity","description":"US Equity blue chip market","marketPreOpen":"07:40","marketOpen":"07:00","marketClose":"23:30","minPricePctChg":20.0,"maxPricePctChg":20.0,"enabled":true,"state":"OPEN"},{"id":2,"name":"Crypto","description":"CryptoCurrency","marketPreOpen":"07:45","marketOpen":"07:00","marketClose":"23:30","minPricePctChg":40.0,"maxPricePctChg":40.0,"enabled":true,"state":"OPEN"}]}

		JsonObject jRspMsg = mHttpClient.get( "queryMarkets");
		assertTrue(jRspMsg.has("markets"),"Not a market response");
		JsonArray jMktArr = jRspMsg.get("markets").getAsJsonArray();
		assertTrue((jMktArr.size() == 2), "Response did not contain two markets");
		//System.out.println( jRsp.toString());
	}

	@Test
	@Order(4)
	public void test_query_symbols() throws IOException {

		JsonObject jRspMsg = mHttpClient.get( "querySymbols/1");

		assertTrue(jRspMsg.has("symbols"),"Not a market-symbols response");
		JsonArray jSymArr = jRspMsg.get("symbols").getAsJsonArray();
		assertTrue((jSymArr.size() > 0), "Response did not contain any symbols");
		//System.out.println(jRspMsg);
	}

	@Test
	@Order(5)
	public void test_add_order() throws IOException {
		try {
			BdxCondition bc1 = addBdxCondition( "BdxPriceLevel" );
			JsonObject jRspMsg = mHttpClient.post("{'sid':'1:AMZN','price':100.00,'quantity': 444, 'side':'BUY','ref' : 'test-5'}", "addOrder");
			assertTrue( jRspMsg.has("orderId"));
			mOrderId =  jRspMsg.get("orderId").getAsString();
			assertTrue( bc1.matchConditionUntil( 2000L ));


			//System.out.println(jRspMsg);
		}
		catch( TeRequestException te ) {
			assertTrue( false, te.toJson().toString());
		}
	}

	@Test
	@Order(6)
	public void test_match_order() throws IOException {
		try {
			BdxCondition bc1 = addBdxCondition("BdxTrade", BdxCondition.condition("last",100.0), BdxCondition.condition("totQuantity",10));
			BdxCondition bc2 = addBdxCondition("BdxOwnTrade", BdxCondition.condition("orderRef","test-6"), BdxCondition.condition("price",100.0));
			BdxCondition bc3 = addBdxCondition("BdxOwnTrade", BdxCondition.condition("orderRef","test-6"), BdxCondition.condition("price",100.0));

			JsonObject jRspMsg = mHttpClient.post("{'sid':'1:AMZN','price':99.00,'quantity': 10, 'side':'SELL','ref' : 'test-6'}", "addOrder");
			assertTrue( jRspMsg.has("matched"));
			assertTrue( (jRspMsg.get("matched").getAsInt() == 10), "Invalid matched volume");
			assertTrue( bc1.matchConditionUntil(2000L));
			assertTrue( bc2.matchConditionUntil(2000L));
			assertTrue( bc3.matchConditionUntil(2000L));

		}
		catch( TeRequestException te ) {
			assertTrue( false, te.toJson().toString());
		}
	}

	@Test
	@Order(7)
	public void test_ammend_order() throws IOException {
		try {
			// Lower the volume, existing order will be updated
			BdxCondition bc1 = addBdxCondition("BdxBBO", BdxCondition.condition("bidQty", 424));
			JsonObject jRspMsg = mHttpClient.post("{'sid':'1:AMZN','orderId' : '" + mOrderId + "', 'deltaQuantity': -10, 'ref' : 'test-7a'}", "amendOrder");
			assertTrue(bc1.matchConditionUntil(2000L),"BBO with right quantity is missing");
			assertTrue( mOrderId.contentEquals( jRspMsg.get("orderId").getAsString()),"OrderIds are not the same");

			// Increase the volume, should result in a new order
			bc1 = addBdxCondition("BdxBBO", BdxCondition.condition("bidQty", 444));
			BdxCondition bc2 = addBdxCondition("BdxOwnOrderbookChange", BdxCondition.condition("action", "REMOVE"));
			jRspMsg = mHttpClient.post("{'sid':'1:AMZN','orderId' : '" + mOrderId + "', 'deltaQuantity': 20, 'ref' : 'test-7b'}", "amendOrder");
			assertFalse( mOrderId.contentEquals(jRspMsg.get("orderId").getAsString()));
			mOrderId = jRspMsg.get("orderId").getAsString(); // new orderId
			assertTrue(bc1.matchConditionUntil(2000L),"BBO with right quantity is missing \n" + bc1.toString());
			assertTrue(bc2.matchConditionUntil(2000L),"BdxOwnOrderbookChange action REMOVE is missing \n" + bc2.toString());

			// Lower the price, existing order will be update
			bc1 = addBdxCondition("BdxBBO", BdxCondition.condition("bidQty", 444), BdxCondition.condition("bid", 99.0));
			jRspMsg = mHttpClient.post("{'sid':'1:AMZN','orderId' : '" + mOrderId + "', 'price': 99, 'ref' : 'test-7c'}", "amendOrder");
			assertTrue(bc1.matchConditionUntil(2000L),"BBO with right quantity & price is missing \n" + bc1.toString());
			assertTrue( mOrderId.contentEquals( jRspMsg.get("orderId").getAsString()),"OrderIds are not the same");

			// Increase the price, should result in a new order
			bc1 = addBdxCondition("BdxBBO", BdxCondition.condition("bidQty", 444), BdxCondition.condition("bid", 101.0));
			bc2 = addBdxCondition("BdxOwnOrderbookChange", BdxCondition.condition("action", "REMOVE"));
			jRspMsg = mHttpClient.post("{'sid':'1:AMZN','orderId' : '" + mOrderId + "', 'price': 101, 'ref' : 'test-7d'}", "amendOrder");
			mOrderId = jRspMsg.get("orderId").getAsString(); // new orderId
			assertTrue(bc1.matchConditionUntil(2000L),"BBO with right quantity & price is missing \n" + bc1.toString());
			assertTrue(bc2.matchConditionUntil(2000L),"BdxOwnOrderbookChange action REMOVE is missing \n" + bc2.toString());

		}
		catch( TeRequestException te ) {
			assertTrue( false, te.toJson().toString());
		}
	}

	@Test
	@Order(8)
	public void test_delete_order() throws IOException {
		try {
			BdxCondition bc1 = addBdxCondition("BdxOwnOrderbookChange",
					BdxCondition.condition("quantity", 444),
					BdxCondition.condition("action", "REMOVE"));
			JsonObject jRspMsg = mHttpClient.post("{'sid':'1:AMZN','orderId' : '" + mOrderId + "', 'ref' : 'test-8'}", "deleteOrder");
			assertTrue( mOrderId.contentEquals( jRspMsg.get("orderId").getAsString()),"OrderId not equal in response");
			assertTrue(bc1.matchConditionUntil(2000L),"BBO with right quantity & price is missing \n" + bc1.toString());
			mOrderId = null;
		}
		catch( TeRequestException te ) {
			assertTrue( false, te.toJson().toString());
		}
	}

	@Test
	@Order(9)
	public void test_add_multiple_order() throws IOException {
		JsonObject jRspMsg = null;
		try {
			BdxCondition  bc1 = addBdxCondition("BdxBBO",
					BdxCondition.condition("bid", 100.0),
					BdxCondition.condition("offer", 101.0));


			BdxCondition  bc2 = addBdxCondition("BdxPriceLevel",
					BdxCondition.condition("sid", "1:AMZN"));


			 jRspMsg = mHttpClient.post("{'sid':'1:AMZN','price':100.00,'quantity': 100, 'side':'BUY','ref' : 'test-9a'}", "addOrder");
			assertTrue(jRspMsg.has("orderId"));
			 jRspMsg = mHttpClient.post("{'sid':'1:AMZN','price':99.0,'quantity': 99, 'side':'BUY','ref' : 'test-9b'}", "addOrder");
			assertTrue(jRspMsg.has("orderId"));
			 jRspMsg = mHttpClient.post("{'sid':'1:AMZN','price':98.00,'quantity': 98, 'side':'BUY','ref' : 'test-9c'}", "addOrder");
			assertTrue(jRspMsg.has("orderId"));

			jRspMsg = mHttpClient.post("{'sid':'1:AMZN','price':101.00,'quantity': 101, 'side':'SELL','ref' : 'test-9d'}", "addOrder");
			assertTrue(jRspMsg.has("orderId"));
			jRspMsg = mHttpClient.post("{'sid':'1:AMZN','price':102.0,'quantity': 102, 'side':'SELL','ref' : 'test-9f'}", "addOrder");
			assertTrue(jRspMsg.has("orderId"));
			jRspMsg = mHttpClient.post("{'sid':'1:AMZN','price':103.00,'quantity': 103, 'side':'SELL','ref' : 'test-9f'}", "addOrder");
			assertTrue(jRspMsg.has("orderId"));

			assertTrue(bc1.matchConditionUntil(2000L),"BBO bid/offer price not expected \n" + bc1.toString());
			assertTrue(bc2.matchConditionUntil(2000L),"BdxPriceLevel for sid 1:AMZN missing \n" + bc1.toString());

		} catch (TeRequestException te) {
			assertTrue(false, te.toJson().toString());
		}
	}

	@Test
	@Order(10)
	public void test_query_trade_prices() throws Exception {
		JsonObject jRspMsg = mHttpClient.get("queryTradePrices/1");
		//System.out.println( jRspMsg.toString());
		JsonObject jPrice = jRspMsg.get("tradePrices").getAsJsonArray().get(0).getAsJsonObject();
		assertTrue( jPrice.get("quantity").getAsInt() == 10, "invalid quantity");
		assertTrue( jPrice.get("last").getAsDouble() == 100.d, "invalid last price");
	}

	@Test
	@Order(11)
	public void test_query_orderbook() throws Exception {
		JsonObject jRspMsg = mHttpClient.get("queryOrderbook/1:AMZN");
		//System.out.println( jRspMsg.toString());

		JsonArray jBuyOrders = jRspMsg.get("buyOrders").getAsJsonArray();
		JsonArray jSellOrders = jRspMsg.get("sellOrders").getAsJsonArray();

		assertTrue( jBuyOrders.size() == 3);
		assertTrue( jSellOrders.size() == 3);

		JsonObject jBuy = jBuyOrders.get(0).getAsJsonObject();
		assertTrue( jBuy.get("price").getAsDouble() == 100.0d);
		assertTrue( jBuy.get("quantity").getAsInt() == 100);
		jBuy = jBuyOrders.get(2).getAsJsonObject();
		assertTrue( jBuy.get("price").getAsDouble() == 98.0d);
		assertTrue( jBuy.get("quantity").getAsInt() == 98);

		JsonObject jSell = jSellOrders.get(0).getAsJsonObject();
		assertTrue( jSell.get("price").getAsDouble() == 101.0d);
		assertTrue( jSell.get("quantity").getAsInt() == 101);
		jSell = jSellOrders.get(2).getAsJsonObject();
		assertTrue( jSell.get("price").getAsDouble() == 103.0d);
		assertTrue( jSell.get("quantity").getAsInt() == 103);

	}

	@Test
	@Order(12)
	public void test_query_price_level() throws Exception {
		JsonObject jRspMsg = mHttpClient.get("queryPriceLevels/1");
		//System.out.println( jRspMsg.toString());
		JsonObject jTradePrice = jRspMsg.get("orderbooks").getAsJsonArray().get(0).getAsJsonObject().get("buySide").getAsJsonArray().get(0).getAsJsonObject();
		assertTrue( jTradePrice.get("price").getAsDouble() == 100.0);
		assertTrue( jTradePrice.get("quantity").getAsInt() == 100);
	}


	@Test
	@Order(13)
	public void test_query_own_trades() throws Exception {
		JsonObject jRspMsg = mHttpClient.get("queryOwnTrades/1");
		//System.out.println( jRspMsg.toString());
		JsonObject jTrade = jRspMsg.get("trades").getAsJsonArray().get(0).getAsJsonObject().get("OwnTrade").getAsJsonObject();
		assertTrue( jTrade.get("price").getAsDouble() == 100.0);
		assertTrue( jTrade.get("quantity").getAsInt() == 10);
	}

	@Test
	@Order(14)
	public void test_query_own_orders() throws Exception {
		JsonObject jRspMsg = mHttpClient.get("queryOwnOrders/1");
		System.out.println( jRspMsg.toString());
		JsonArray jOrderArray = jRspMsg.get("orders").getAsJsonArray();
		assertTrue( jOrderArray.size() == 6);

		double sPrice = 0, bPrice = 0;
		int sQty = 0, bQty = 0;

		for (int i = 0; i < jOrderArray.size(); i++) {
			JsonObject jOrder = jOrderArray.get(i).getAsJsonObject();
			if (jOrder.get("side").getAsString().contentEquals("SELL")) {
				sPrice += jOrder.get("price").getAsDouble();
				sQty += jOrder.get("quantity").getAsInt();
			} else {
				bPrice += jOrder.get("price").getAsDouble();
				bQty += jOrder.get("quantity").getAsInt();
			}
		}
		assertTrue( sPrice == 306.0 );
		assertTrue( sQty == 306 );
		assertTrue( bPrice == 297.0 );
		assertTrue( bQty == 297 );

	}

	@Test
	@Order(15)
	public void test_query_BBO() throws Exception {
		boolean tSidFound = false;

		JsonObject jRspMsg = mHttpClient.get("queryBBO/1");
		JsonArray jBBOArr = jRspMsg.get("prices").getAsJsonArray();
		for (int i = 0; i < jBBOArr.size(); i++) {
			JsonObject jPrice = jBBOArr.get(i).getAsJsonObject();
			if (jPrice.get("sid").getAsString().contentEquals("1:AMZN")) {
				tSidFound = true;
				assertTrue( jPrice.get("bid").getAsDouble() == 100.0);
				assertTrue( jPrice.get("offer").getAsDouble() == 101.0);
				assertTrue( jPrice.get("bidQty").getAsInt() == 100);
				assertTrue( jPrice.get("offerQty").getAsInt() == 101);
			}
		}
		assertTrue( tSidFound );
	}

	@Test
	@Order(16)
	public void test_delete_all() throws Exception {

		JsonObject jRspMsg = mHttpClient.delete("deleteAllOrders/1");
		assertTrue( jRspMsg.get("isOk").getAsBoolean());
		assertTrue( jRspMsg.get("statusMessage").getAsString().contains("6 orders"));
	}



	private BdxCondition addBdxCondition( String pBdxName, String ... pConditions) {
		BdxCondition bc = new BdxCondition(pBdxName, pConditions);
		synchronized ( mBdxConditions ) {
			mBdxConditions.add( bc );
		}
		return bc;
	}


	public  boolean responseCheck( String pExpectedResponse, String pActualResponse ) {
		JsonObject jExpectedResponse = JsonParser.parseString( pExpectedResponse ).getAsJsonObject();
		JsonObject jActualResponse = JsonParser.parseString( pActualResponse ).getAsJsonObject();
		return jExpectedResponse.toString().contentEquals( jActualResponse.toString());
	}

	private String toJsonString( String pSnuttifiedJsonString ) {
		return pSnuttifiedJsonString.replace('\'','\"');
	}

	@Override
	public void onOpen(Session session, EndpointConfig config) {

	}

	@Override
	public void onMessage(String pBdxMsg) {
		System.out.println("[Broadcast] " + pBdxMsg);
		synchronized ( mBdxConditions ) {
			Iterator<BdxCondition> tItr = mBdxConditions.iterator();
			while(tItr.hasNext()) {
				BdxCondition bc = tItr.next();
				if (bc.matchReceivedBdx( pBdxMsg ) || bc.isTimedOut()) {
					tItr.remove();
				}
			}
		}

	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
	}

	@Override
	public void onError(Session session, Throwable throwable) {

	}


	static class TeThread extends Thread
	{
		private Object mTeStarted;

		TeThread() {
			mTeStarted = new Object();
		}

		public void run() {
			String args[] = {"file:///Users/Bertilsson/source/TradingEngine/configuration/TeConfiguration.json"};

			//Start the TE engine
			TradingEngine te = new TradingEngine();
			te.setTestMode();

			synchronized (te) {
				te.parsePargument(args);
				te.initialize();

				synchronized (mTeStarted) {
					mTeStarted.notifyAll();
				}
			}


			while( true ) {
				try {
					Thread.sleep(1000L);
				}
				catch( InterruptedException e) {};
			}
		}

		public void waitForTeToStart() {
			synchronized( mTeStarted ) {
				try {mTeStarted.wait();}
				catch( InterruptedException ie) {}
			}
		}
	}
}



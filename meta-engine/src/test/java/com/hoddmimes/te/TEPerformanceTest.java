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
import com.hoddmimes.te.common.transport.http.TeHttpClient;
import com.hoddmimes.te.common.transport.http.TeRequestException;
import com.hoddmimes.te.common.transport.http.TeWebsocketClient;
import org.junit.jupiter.api.*;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

public class TEPerformanceTest {
	 enum Side {BUY, SELL};

	private static final String TE_HTTP_URI = "https://localhost:8883/te/";
	private static final String TE_WSS_URI = "wss://localhost:8883/marketdata";

	private  TeWebsocketClient   mWssClient;
	private  TeHttpClient        mHttpClient;
	private  TeThread            mTeThread;
	private Random               mRandom;
	private long                 mUserRef;
	private  String mOrderId;



	@BeforeAll
	public void TeSetup() {
		System.out.println("TesSetup");

		System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");
		System.setProperty("java.util.logging.SimpleFormatter.format", "");

		mRandom = new Random();
		mUserRef = 1;

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
	}



	@Test
	@Order(2)
	public void test_performance() throws IOException {
		long tStartTime = System.currentTimeMillis();
		JsonObject jOrder;

		try {
			for (int i = 0; i < 1000; i++) {
				jOrder = createOrder("1:AMZN", Side.BUY);
				JsonObject jRspMsg = mHttpClient.post(jOrder.toString(), "addOrder");
				assertTrue(jRspMsg.has("orderId"));
				mOrderId = jRspMsg.get("orderId").getAsString();
			}
			//System.out.println(jRspMsg);
		}
		catch( TeRequestException te ) {
			assertTrue( false, te.toJson().toString());
		}
	}

	JsonObject createOrder( String pSid, Side pSide ) {
		JsonObject jMsg = new JsonObject();

		long tPrice =  (pSide == Side.BUY) ? (100_0000L - (1_0000 * (mRandom.nextLong(10 )))) : (100_0000L + (1_0000 * (mRandom.nextLong(10 ))));
		int tQuantity = 50 + mRandom.nextInt(50);
		String tRef = "usrref-" + String.valueOf( (mUserRef++) );

		jMsg.addProperty("sid", pSid);
		jMsg.addProperty("price", tPrice );
		jMsg.addProperty("quantity", tQuantity);
		jMsg.addProperty("side", pSide.name());
		jMsg.addProperty("ref", tRef);
		return jMsg;

	}

	public  boolean responseCheck( String pExpectedResponse, String pActualResponse ) {
		JsonObject jExpectedResponse = JsonParser.parseString( pExpectedResponse ).getAsJsonObject();
		JsonObject jActualResponse = JsonParser.parseString( pActualResponse ).getAsJsonObject();
		return jExpectedResponse.toString().contentEquals( jActualResponse.toString());
	}

	private String toJsonString( String pSnuttifiedJsonString ) {
		return pSnuttifiedJsonString.replace('\'','\"');
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



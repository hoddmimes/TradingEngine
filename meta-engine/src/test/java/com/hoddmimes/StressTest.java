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

package com.hoddmimes;

import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TradingEngine;
import com.hoddmimes.te.common.transport.http.TeHttpClient;
import com.hoddmimes.te.common.transport.http.TeWebsocketClient;
import com.hoddmimes.te.messages.generated.AddOrderResponse;
import com.hoddmimes.te.messages.generated.DeleteOrderResponse;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StressTest
{
	enum Side {BUY,SELL};
	record User( String user, String Password ) {};

	private AtomicLong mTotalResponseTimes; // usec
	private AtomicLong mTotalTx;

	private static int TEST_REQUESTS = 500000;   // Number of orders to execute
	private static int MATCHING_RATE = 6;    // Percentage
	private static int TEST_THREADS = 1;
	private static int MAX_DEPTH = 15;

	private Random mRandom;
	private AtomicInteger mUserRef;
	private TestThread mTestThreads[];

	TradingEngine te;


	public static void main(String[] args) {
		StressTest st = new StressTest();
		st.setupAndStartTradingEngine();
		st.runTest();
	}

	public StressTest() {
		mTotalResponseTimes = new AtomicLong(0);
		mTotalTx = new AtomicLong(0);

		mUserRef = new AtomicInteger(0);
		mRandom = new Random();
		mTestThreads = new TestThread[TEST_THREADS];
	}



	private void setupAndStartTradingEngine() {
		System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");
		System.setProperty("java.util.logging.SimpleFormatter.format", "");

		String args[] = {"file:///Users/Bertilsson/source/TradingEngine/configuration/TeConfiguration.json"};

		//Start the TE engine
		te = new TradingEngine();
		te.setTestMode();
		te.parsePargument(args);
		te.initialize();
	}

	private void runTest() {
		warmup();
		for (int i = 0; i < TEST_THREADS; i++) {
			mTestThreads[i] = new TestThread((i+1),"1:AMZN", Side.BUY);
			mTestThreads[i].start();
		}
		for (int i = 0; i < TEST_THREADS; i++) {
			mTestThreads[i].waitForCompleation();
		}
		long txAvg = mTotalResponseTimes.get() / mTotalTx.get();

		System.out.println("Threads :" + TEST_THREADS + " Request Count : " + mTotalTx.get() + " Average TX time: " + txAvg + " usec");
	}

	private JsonObject createDeleteOrder( String pOrderId, String pSid, String pRef ) {
		JsonObject jMsg = new JsonObject();
		JsonObject jBody = new JsonObject();
		jBody.addProperty( "sid", pSid);
		jBody.addProperty("orderId", pOrderId);
		jBody.addProperty("ref", pRef);
		jMsg.add("DeleteOrderRequest", jBody);
		return jMsg;

	}


	JsonObject createOrder( boolean pMatch, String pSid, Side pSide, String pRef ) {
		JsonObject jBody = new JsonObject();
		JsonObject jMsg = new JsonObject();

		long tPrice =  ((!pMatch) && (pSide == Side.BUY)) ? (100_0000L - (1_0000 * (mRandom.nextLong(10 )))) : (100_0000L + (1_0000 * (mRandom.nextLong(10 ))));
		int tQuantity = 50 + mRandom.nextInt(50);

		jBody.addProperty("sid", pSid);
		jBody.addProperty("price", tPrice );
		jBody.addProperty("quantity", tQuantity);
		jBody.addProperty("side", pSide.name());
		jBody.addProperty("ref", pRef);
		jMsg.add("AddOrderRequest", jBody);
		return jMsg;

	}

	private void warmup() {
		for (int i = 0; i < 1000; i++) {
			JsonObject jOrder = createOrder(false, "1:AMZN", Side.BUY, "warmup");
			MessageInterface tRspMsg = te.testMessage(jOrder);
			if (tRspMsg instanceof AddOrderResponse) {
				String tOrderId = ((AddOrderResponse) tRspMsg).getOrderId().get();
				JsonObject jDelete = createDeleteOrder(tOrderId, "1:AMZN", "warmup");
				tRspMsg = te.testMessage(jDelete);
				//System.out.println( tRspMsg);
			}
		}

	}

	class  TestThread extends Thread
	{
		private String mSid;
		private Side mSide;

		private String mUser,mPassword;
		private LinkedList<String> mOrderIds;
		private Random mRandom;
		private int mUserCount;
		private int mThrCnt;
		private boolean mComplete;


		private TeWebsocketClient mWssClient;
		private TeHttpClient mHttpClient;


		TestThread( int pThreadCount, String pSid, Side pSide ) {
			mComplete = false;
			mSid = pSid;
			mSide = pSide;
			mOrderIds = new LinkedList<>();
			mRandom = new Random();
			mUserCount = 1;
			mThrCnt = pThreadCount;
		}

		void waitForCompleation() {
			synchronized ( this ) {
				if (mComplete) {
					return;
				}
				try { this.wait();}
				catch( InterruptedException ie) {}
			}
		}

		private String enterOrder( boolean tMatch) {
			String tRef = String.valueOf( mThrCnt) + ":" + String.valueOf( mUserCount++);
			JsonObject jOrder = createOrder(tMatch, mSid, mSide, tRef );
			long tStartTime = System.nanoTime();
			MessageInterface tRspMsg = te.testMessage(jOrder);
			long tExecTime = (System.nanoTime() - tStartTime) / 1000L;
			mTotalResponseTimes.addAndGet(tExecTime);
			mTotalTx.incrementAndGet();

			if (tRspMsg instanceof AddOrderResponse) {
				return ((AddOrderResponse) tRspMsg).getOrderId().get();
			}
			throw new RuntimeException("Invalid enter order response: " + tRspMsg.toJson().toString());
		}


		public void run() {
			String tOrderId;

			while( mTotalTx.incrementAndGet() < TEST_REQUESTS) {
					if ((mOrderIds.size() > 0) && (mRandom.nextInt(100) <= MATCHING_RATE)) {
						tOrderId = enterOrder(true);
					} else {
						tOrderId = enterOrder(false);
						mOrderIds.add(tOrderId);
					}
					if (mOrderIds.size() >= MAX_DEPTH) {
						purge();
					}
			}
			synchronized( this ) {
				mComplete = true;
				this.notifyAll();
			}
		}

		private void purge() {
			int tRemoveCount = 0;
			int tOrderToRemove = 1 + mRandom.nextInt( (MAX_DEPTH / 2));
			Iterator<String> tItr = mOrderIds.iterator();
			for (int i = 0; i < mRandom.nextInt( MAX_DEPTH - 1); i++) {
				tItr.next();
			}
			while( tRemoveCount < tOrderToRemove) {
				if (!tItr.hasNext()) {
					tItr = mOrderIds.iterator();
				} else {
					try {
						String tOrderId = tItr.next();
						String tRef = String.valueOf( mThrCnt) + ":" + String.valueOf( mUserCount++);
						JsonObject jMsg = createDeleteOrder(tOrderId, mSid, tRef);
						long tStartTime = System.nanoTime();
						MessageInterface tRspMsg = te.testMessage( jMsg );
						long tExecTime = (System.nanoTime() - tStartTime) / 1000L;
						if (tRspMsg instanceof DeleteOrderResponse) {
							tItr.remove();
							tRemoveCount++;
							mTotalTx.incrementAndGet();
							mTotalResponseTimes.addAndGet( tExecTime );
						} else {
							throw new RuntimeException("Remove order failed :  " + tRspMsg.toJson().toString());
						}
					}
					catch( Exception e) {
						e.printStackTrace();
					}
				}
			}
		}


	}



	static class TradingEngineThread extends Thread
	{
		private Object mTeStarted;

		TradingEngineThread() {
			mTeStarted = new Object();
		}

		public void run() {



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

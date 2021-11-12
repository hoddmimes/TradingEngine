package com.hoddmimes.te.engine;

import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.TradingEngine;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.SingleExecutor;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.messages.EngineMsgInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MatchingEngineFrontend
{
	private Logger mLog = LogManager.getLogger( MatchingEngineFrontend.class );
	private ArrayList<SingleExecutor>   mExecutors;
	private Queue<MeRqstCntx>           mRqstCntxCache;
	private MatchingEngine              mMatchingEngine;
	private JsonObject mConfiguration;

	public MatchingEngineFrontend(JsonObject pTeConfiguration, MatchingEngine pMatchingEngine ) {
		mConfiguration = AuxJson.navigateObject(pTeConfiguration,"TeConfiguration/matchingEngineFrontendConfiguration");
		mMatchingEngine = pMatchingEngine;
		int tExecQueues = mConfiguration.get("executionQueues").getAsInt();
		int tMaxExecQueueLength = mConfiguration.get("maxExecutionQueueLength").getAsInt();
		int tRqstCacheSize = mConfiguration.get("requestCacheSize").getAsInt();
		mExecutors = new ArrayList<>( tExecQueues );
		for (int i = 0; i < tExecQueues; i++) {
			mExecutors.add( new SingleExecutor(tMaxExecQueueLength,"ME-EXEC-" + String.valueOf((i+1))));
		}
		mRqstCntxCache = new ConcurrentLinkedQueue<>();
		for (int i = 0; i < tRqstCacheSize; i++) {
			mRqstCntxCache.add( new MeRqstCntx( mMatchingEngine, mRqstCntxCache, mLog  ));
		}
		TeAppCntx.getInstance().setMatchingEngineFrontend(this);
	}

	public MeRqstCntx queue(SessionCntxInterface pSessCntx, EngineMsgInterface pRequestMsg ) {
		MeRqstCntx tRqstCntx = mRqstCntxCache.poll();
		if (tRqstCntx == null) {
			tRqstCntx = new MeRqstCntx( mMatchingEngine, null, mLog );
		}
		tRqstCntx.init(pSessCntx, pRequestMsg );

		// Queue the request to the Matching Engine in another Thread
		// Make sure that requests for a specific symbol is always executed by the same thread
		mExecutors.get( pRequestMsg.getSymbol().get().hashCode() % mExecutors.size()).queue( tRqstCntx );
		return tRqstCntx;
	}


}

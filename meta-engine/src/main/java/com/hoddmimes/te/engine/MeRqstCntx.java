package com.hoddmimes.te.engine;

import com.hoddmimes.jaux.AuxTimestamp;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.TradingEngine;
import com.hoddmimes.te.common.interfaces.MarketDataInterface;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.messages.EngineBdxInterface;
import com.hoddmimes.te.messages.EngineMsgInterface;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.MessageFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;


public class MeRqstCntx implements Runnable
{
	private Logger                   mLog;
	private SessionCntxInterface     mSessCntx;
	public  EngineMsgInterface        mRequest;
	public MessageInterface         mResponse;
	private MatchingEngine           mMatchingEngine;
	private AuxTimestamp             mTimestamp;
	private List<EngineBdxInterface> mPrivateBdx;
	private List<EngineBdxInterface> mPublicBdx;
	private List<Trade>              mTrades;
	private Queue<MeRqstCntx>        mRequestPool;


	MeRqstCntx(MatchingEngine pMatchinEngine, Queue<MeRqstCntx> pRequestPool, Logger pLogger) {
		mPrivateBdx = new ArrayList<>();
		mPublicBdx = new ArrayList<>();
		mTrades = new ArrayList<>();
		mMatchingEngine = pMatchinEngine;
		mRequestPool = pRequestPool;
		mLog = pLogger;
	}



	public void init( SessionCntxInterface pSessCntx, EngineMsgInterface pMeRqstMsg) {
		mTimestamp = new AuxTimestamp("ME " + pMeRqstMsg.getMessageName() + " Start");
		mRequest = pMeRqstMsg;
		mSessCntx = pSessCntx;
		mPublicBdx.clear();
		mPrivateBdx.clear();
		mTrades.clear();
		mResponse = null;
	}

	public void setResponse( MessageInterface pResponseMsg ) {
		this.mResponse = pResponseMsg;
	}

	public String getUserId() {
		return mSessCntx.getUserId();
	}

	public String getSessionInfo() {
		return "[user: " + mSessCntx.getUserId() + "sid: " + mSessCntx.getSessionId() + "]";
	}


	@Override
	public void run() {
		try {
			this.mResponse = mMatchingEngine.execute(this);
		}
		catch( Throwable e) {
			mLog.error("Failed to execute " + this.mRequest.getMessageName() + " " + getSessionInfo());
			this.mResponse = StatusMessageBuilder.error("Failed to execute " + this.mRequest.getMessageName(), null, e);
		}

		synchronized( this ) {
			this.notifyAll();
		}
	}



	public void addPublicBdx( EngineBdxInterface pBdx ) {
		mPublicBdx.add( pBdx );
	}

	public void addPrivateBdx( EngineBdxInterface pBdx ) {
		mPrivateBdx.add( pBdx );
	}

	public void timestamp( String pLabel ) {
		mTimestamp.add( pLabel );
	}

	private MessageInterface finalizeProcessing() {
		// publish all bdx
		MarketDataInterface tMarketData = TeAppCntx.getInstance().getMarketDataDistributor();
		// Todo: Fix publication


		MessageInterface tReturnMessage = mResponse;
		if (mRequestPool != null) {
			mRequestPool.add( this );
		}
		return tReturnMessage;
	}

	public MessageInterface waitForCompleation() {
		synchronized( this ) {
			if (mResponse != null) {
				return finalizeProcessing();
			}
			try {this.wait();}
			catch( InterruptedException e) {}
			return finalizeProcessing();
		}
	}



}

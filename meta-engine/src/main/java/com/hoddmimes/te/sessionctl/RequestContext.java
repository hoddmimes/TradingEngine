package com.hoddmimes.te.sessionctl;

import com.hoddmimes.jaux.AuxTimestamp;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.interfaces.RequestContextInterface;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import org.apache.logging.log4j.Logger;


public class RequestContext implements RequestContextInterface
{
	private MessageInterface mRequest;
	private SessionCntxInterface mSessCntx;
	private AuxTimestamp mTimestamp;

	public RequestContext( MessageInterface pRequestMessage, SessionCntxInterface pSessionCntx ) {
		mRequest = pRequestMessage;
		mTimestamp = new AuxTimestamp("Initializing request \"" + pRequestMessage.getMessageName() + "\"");
		mSessCntx = pSessionCntx;
	}

	public long getExecTimeUsec() {
		return mTimestamp.getTotalTimeUsec();
	}


	@Override
	public String toString() {
		return "[ rqst: " + mRequest.getMessageName() + " account: " + mSessCntx.getAccount() + " sid: " + mSessCntx.getSessionId() + "]";
	}
	public SessionCntxInterface getSessionContext() {
		return mSessCntx;
	}

	public void timestamp( String pLabel ) {
		mTimestamp.add( pLabel );
	}

	public String getAccountId() {
		return mSessCntx.getAccount();
	}

	public void traceExecTime( long pTraceTimeLimit, boolean pVerbose, Logger pLogger ) {
		if (pTraceTimeLimit < 0) {
			return;
		}

		if (mTimestamp.getTotalTimeUsec() < pTraceTimeLimit) {
			return;
		}

		String tLogStr = (pVerbose) ? mTimestamp.toString() : mTimestamp.toShortString();
		pLogger.info( tLogStr );
	}



}

package com.hoddmimes.te.common.interfaces;

import org.apache.logging.log4j.Logger;

public interface RequestContextInterface
{
	public SessionCntxInterface getSessionContext();
	public void timestamp( String pLabel );
	public String getAccountId();
	public void traceExecTime( long pTraceTimeLimit, boolean pVerbose, Logger pLogger );
}

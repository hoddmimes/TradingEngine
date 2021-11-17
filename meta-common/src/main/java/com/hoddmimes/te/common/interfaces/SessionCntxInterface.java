package com.hoddmimes.te.common.interfaces;

public interface SessionCntxInterface {
	public String getAccount();
	public String getSessionId();
	public String getSessionStartTime();
	public long getSessionStartTimeBin();
	public String getApiAuthId();
	public void setMarketDataSessionId( String pMarketDataSessionId );
	public String getMarketDataSessionId();
}

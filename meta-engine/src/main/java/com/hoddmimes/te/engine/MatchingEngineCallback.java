package com.hoddmimes.te.engine;

import com.hoddmimes.te.common.interfaces.SessionCntxInterface;

public interface MatchingEngineCallback {

	public void orderAdded(Order pOrder, SessionCntxInterface pSessionCntx, long pOrderbookSeqNo);
	public void orderRemoved(Order pOrder, SessionCntxInterface pSessionCntx, long pOrderbookSeqNo);
	public void orderModified(Order pOrder, SessionCntxInterface pSessionCntx, long pOrderbookSeqNo);
	public void trade(InternalTrade pTrade, SessionCntxInterface pSessionCntx);
	public void newOrderMatched(Order pOrder, SessionCntxInterface pSessionCntx, long pOrderbookSeqNo);
	public void orderbookChanged(String mSymbol);

}

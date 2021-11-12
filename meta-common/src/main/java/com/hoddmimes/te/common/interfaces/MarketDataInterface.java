package com.hoddmimes.te.common.interfaces;

import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.messages.EngineBdxInterface;
import com.hoddmimes.te.messages.generated.QueryPriceLevelsRequest;

public interface MarketDataInterface
{
	public void queueBdxPrivate( SessionCntxInterface pSessionCntx, EngineBdxInterface pBdx );
	public void queueBdxPublic( EngineBdxInterface pBdx );
	public MessageInterface queryPriceLevels(QueryPriceLevelsRequest pQryPLRqst);
	public void orderbookChanged( String pSymbol );
}

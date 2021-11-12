package com.hoddmimes.te.engine;



public interface EngineInterface
{
    public void orderAdded(Order pOrder, MeRqstCntx pRqstCntx, long pOrderBookSeqNo );
    public void orderRemoved( Order pOrder, MeRqstCntx RqstCntx, long pOrderBookSeqNo);
    public void orderModified( Order pOrder, MeRqstCntx RqstCntx, long pOrderBookSeqNo );
    public void trade( Trade pTrade, MeRqstCntx RqstCntx );
    public void newOrderMatched( Order pOrder, MeRqstCntx RqstCntx, long pOrderBookSeqNo );
    public void orderbookChanged( String mSymbol );


}

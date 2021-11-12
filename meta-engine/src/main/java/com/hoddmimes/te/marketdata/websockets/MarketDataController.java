package com.hoddmimes.te.marketdata.websockets;


import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.marketdata.MarketDataBase;
import com.hoddmimes.te.messages.EngineBdxInterface;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


@Configuration
@EnableWebSocket
public class MarketDataController extends MarketDataBase implements WebSocketConfigurer, Runnable
{
    public record BdxQueueItem( EngineBdxInterface mBdx, SessionCntxInterface mSessCntx){}


    WebSocketHandler mWebSocketHandler;
    private BlockingQueue<BdxQueueItem> mBdxQueue;


    public  MarketDataController() {
        super(TeAppCntx.getInstance().getTeConfiguration());
        mBdxQueue = new LinkedBlockingQueue<>();
    }


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        mWebSocketHandler = new WebSocketHandler();
        WebSocketHandlerRegistration tWsHandler = registry.addHandler(mWebSocketHandler, "/marketdata");
        tWsHandler.addInterceptors( new WebSocketHandshakeInterceptor());
        TeAppCntx.getInstance().setMarketDataDistributor( this );
    }


    @Override
    public void queueBdxPrivate(SessionCntxInterface pSessionCntx, EngineBdxInterface pBdx) {
        try {
            mBdxQueue.put( new BdxQueueItem( pBdx, pSessionCntx ));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void queueBdxPublic(EngineBdxInterface pBdx)
    {
        try {
            mBdxQueue.put( new BdxQueueItem(pBdx,null ));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void sendPrivateBdx(SessionCntxInterface pSessionCntxInterface, EngineBdxInterface pBdx) {
        mWebSocketHandler.sendPrivateBdx( pSessionCntxInterface, pBdx);
    }

    @Override
    protected void sendPublicBdx(EngineBdxInterface pBdx) {
        mWebSocketHandler.sendPublicBdx( pBdx);
    }

    private void sendBdx( BdxQueueItem pBdxQueueItem) {
        if (pBdxQueueItem.mSessCntx == null) {
            sendPublicBdx( pBdxQueueItem.mBdx );
        } else {
            sendPrivateBdx( pBdxQueueItem.mSessCntx, pBdxQueueItem.mBdx);
        }
    }

    @Override
    public void run() {
        List<BdxQueueItem> tBdxList = new ArrayList<>(30);
        BdxQueueItem tBdxQueItm = null;

        while( true ) {
            tBdxQueItm = null;
            try {
                tBdxQueItm = mBdxQueue.take();
            } catch (InterruptedException e) {
            }

            if (tBdxQueItm != null) {
                sendBdx(tBdxQueItm);
            }
            tBdxList.clear();
            mBdxQueue.drainTo( tBdxList, 30);
            for( BdxQueueItem tBdxItm : tBdxList) {
                sendBdx( tBdxItm );
            }
        }
    }
}

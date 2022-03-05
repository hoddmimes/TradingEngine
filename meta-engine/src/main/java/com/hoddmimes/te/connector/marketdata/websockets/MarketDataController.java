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

package com.hoddmimes.te.connector.marketdata.websockets;


import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.TeCoreService;
import com.hoddmimes.te.common.interfaces.TeService;
import com.hoddmimes.te.connector.marketdata.MarketDataBase;
import com.hoddmimes.te.messages.EngineBdxInterface;
import com.hoddmimes.te.messages.StatusMessageBuilder;
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
public class MarketDataController extends MarketDataBase implements WebSocketConfigurer, Runnable {



    public record BdxQueueItem(EngineBdxInterface mBdx, String mAccountId) {
    }


    WebSocketHandler mWebSocketHandler;
    private BlockingQueue<BdxQueueItem> mBdxQueue;
    Thread mTransmitter;

    public MarketDataController() {
        super(TeAppCntx.getInstance().getTeConfiguration(), TeAppCntx.getIpcService());
        mBdxQueue = new LinkedBlockingQueue<>();
        mTransmitter = new Thread(this);
        mTransmitter.start();


    }


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        mWebSocketHandler = new WebSocketHandler();
        WebSocketHandlerRegistration tWsHandler = registry.addHandler(mWebSocketHandler, "/te-marketdata/");
        tWsHandler.addInterceptors(new WebSocketHandshakeInterceptor());
        TeAppCntx.getInstance().registerService( this );
        this.serviceIsSynchronized();
    }


    @Override
    public void queueBdxPrivate(String pAccountId, EngineBdxInterface pBdx) {
        try {
            mBdxQueue.put(new BdxQueueItem(pBdx, pAccountId));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void queueBdxPublic(EngineBdxInterface pBdx) {
        try {
            mBdxQueue.put(new BdxQueueItem(pBdx, null));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void sendPrivateBdx(String pAccountId, EngineBdxInterface pBdx) {
        if (mWebSocketHandler != null) {
            mWebSocketHandler.sendPrivateBdx(pAccountId, pBdx);
         }
    }

    @Override
    protected void sendPublicBdx(EngineBdxInterface pBdx) {
        if (mWebSocketHandler != null) {
            mWebSocketHandler.sendPublicBdx(pBdx);
        }
    }

    private void sendBdx( BdxQueueItem pBdxQueueItem) {
        if (pBdxQueueItem.mAccountId == null) {
            sendPublicBdx( pBdxQueueItem.mBdx );
        } else {
            sendPrivateBdx( pBdxQueueItem.mAccountId, pBdxQueueItem.mBdx);
        }
    }

    @Override
    public TeService getServiceId() {
        return TeService.MarketData;
    }

    @Override
    public MessageInterface ipcRequest(MessageInterface pMgmtRequest) {
        if (mWebSocketHandler == null) {
            return StatusMessageBuilder.error("Market data Distributor  not yet started", null);
        }
        return mWebSocketHandler.marketDataIpcRequest( pMgmtRequest);
    }


    @Override
    public void run() {
        mTransmitter.setName("Market Data BDX Publisher");
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

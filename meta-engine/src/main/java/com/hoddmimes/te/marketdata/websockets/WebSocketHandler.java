package com.hoddmimes.te.marketdata.websockets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.messages.EngineBdxInterface;
import generated.TestMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public class WebSocketHandler extends TextWebSocketHandler  {
    private Logger mLog = LogManager.getLogger(WebSocketHandler.class);
    private ConcurrentHashMap<String, WebSocketSessionCntx> mSessions;


    public WebSocketHandler()
    {
        mSessions = new ConcurrentHashMap<>();
    }

    @Override
    public void handleTextMessage(WebSocketSession pSession, TextMessage pMessage)
            throws InterruptedException, IOException {
        try {
            String tMsgString = pMessage.getPayload();
            mLog.warn("message from ws client: " + tMsgString);

        } catch (JsonSyntaxException je) {
        }
    }

    private void sendBdx( WebSocketSession pSession, TextMessage pTxtMsg  ) throws IOException{
        pSession.sendMessage( pTxtMsg );
    }

    public void sendPublicBdx( EngineBdxInterface pBdx) {
        synchronized (mSessions) {
            TextMessage tTxtMsg = new TextMessage(pBdx.toJson().toString());
            Iterator<String> tKeyItr = mSessions.keys().asIterator();
            while (tKeyItr.hasNext()) {
                WebSocketSessionCntx wscntx = mSessions.get(tKeyItr.next());
                if (wscntx != null) {
                    try {
                        sendBdx(wscntx.mWsSession, tTxtMsg);
                    } catch (IOException e) {
                        mLog.warn("failed to send public bdx to " + wscntx.toString());
                        mSessions.remove(wscntx.mWsSession.getId());
                        mLog.warn("ws session " + wscntx.toString() + " removed");
                    }
                }
            }
        }
    }

    void sendPrivateBdx(SessionCntxInterface pSessionCntxInterface, EngineBdxInterface pBdx) {
        WebSocketSessionCntx tWsSessionCntx = mSessions.get(pSessionCntxInterface.getMarketDataSessionId());
        if (tWsSessionCntx != null) {
            try {
                sendBdx(tWsSessionCntx.mWsSession, new TextMessage(pBdx.toJson().toString()));
            } catch (IOException e) {
                mLog.warn("failed to send private bdx to " + tWsSessionCntx.toString());
                synchronized ( mSessions ) {
                    mSessions.remove(mSessions.remove(tWsSessionCntx.mWsSession.getId()));
                }
                mLog.warn("ws session " + tWsSessionCntx.toString() + " removed");
            }
        }
    }




    @Override
    public void afterConnectionEstablished(WebSocketSession pSession) throws Exception {
        String tAuthId = pSession.getAttributes().get("authid").toString();
        if (tAuthId == null) {
            mLog.warn("\"authid\" is not found in WS session attributes, session not added, WS session id: " + pSession.getId());
            return;
        }
        SessionCntxInterface tHttpSessionCntx = TeAppCntx.getInstance().getSessionController().getSessionCntxByAuthId( tAuthId );
        if (tHttpSessionCntx == null) {
            mLog.warn("Http session cntx is not found for \"authid\", session not added, WS session id: " + pSession.getId() + " authid: " + tAuthId);
            return;
        }
        tHttpSessionCntx.setMarketDataSessionId( pSession.getId());

        WebSocketSessionCntx tWsSessionCntx = new WebSocketSessionCntx( pSession, tHttpSessionCntx);
        mSessions.put( pSession.getId(), tWsSessionCntx );
        mLog.info("session established, remote: " + pSession.getRemoteAddress() + tWsSessionCntx.toString());
    }




    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        try { session.close(); }
        catch( Exception e) {}

        WebSocketSessionCntx tWsSessionCntx = null;
        synchronized ( mSessions ) {
            tWsSessionCntx = mSessions.remove( session.getId() );
        }
        if (tWsSessionCntx != null) {
            mLog.warn("session disconnected, " + tWsSessionCntx.toString());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception
    {
        WebSocketSessionCntx tWsSessionCntx = null;
        synchronized ( mSessions ) {
            tWsSessionCntx = mSessions.remove( session.getId() );
        }
        if (tWsSessionCntx != null) {
            mLog.warn("session closed and removed, " + tWsSessionCntx.toString());
        }
    }

    class WebSocketSessionCntx
    {
        WebSocketSession        mWsSession;
        SessionCntxInterface    mHttpSessionCntx;

        WebSocketSessionCntx( WebSocketSession pWsSession, SessionCntxInterface pHttpSessionCntx ) {
            mWsSession = pWsSession;
            mHttpSessionCntx = pHttpSessionCntx;
        }

        public String toString() {
            return " id: " + mWsSession.getId() + " user: " + mHttpSessionCntx.getUserId();
        }
    }
}


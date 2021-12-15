/*
 * Copyright (c)  Hoddmimes Solution AB 2021.
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

package com.hoddmimes.te.marketdata.websockets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.marketdata.SubscriptionFilter;
import com.hoddmimes.te.marketdata.SubscriptionUpdateCallbackIf;
import com.hoddmimes.te.messages.EngineBdxInterface;
import com.hoddmimes.te.messages.generated.SubscriptionRequest;
import com.hoddmimes.te.messages.generated.SubscriptionResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class WebSocketHandler extends TextWebSocketHandler implements SubscriptionUpdateCallbackIf {
    private Logger mLog = LogManager.getLogger(WebSocketHandler.class);
    private ConcurrentHashMap<String, WebSocketSessionCntx> mSessions;


    public WebSocketHandler()
    {
        mSessions = new ConcurrentHashMap<>();
    }


    /**
     * Client are suposed to send two types of messages
     * 1) add subscription {"command" : "ADD", "subject" : "/<bdx></market>/<sid>" }
     * 2) clear (all) subscription {"command" : "CLEAR" }
     */

    @Override
    public void handleTextMessage(WebSocketSession pSession, TextMessage pMessage)  throws InterruptedException, IOException {
        String tMsg = null;
        SubscriptionResponse tSubscrRsp = null;


        WebSocketSessionCntx tWsSessionCntx = mSessions.get(pSession.getId());
        if (tWsSessionCntx == null) {
            mLog.warn("(receive) invalid WS client : " + pSession.getId());
            return;
        }

        try {
            SubscriptionRequest tSubScr = parseSubscriptionRequest(pMessage.getPayload());
            String tCommand = tSubScr.getCommand().get().toUpperCase();

            // ADD subscription
            if (tCommand.contentEquals("ADD")) {
                String tSubject = tSubScr.getTopic().orElse(null);
                if (tSubject == null) { throw new JsonSyntaxException("topic not specified"); }
                tWsSessionCntx.mFilter.add( tSubject, this, tWsSessionCntx );
                tSubscrRsp = new SubscriptionResponse().setIsOk(true).setMessage("successfully added filter : " + tSubject );

            }

            // Clear ALL subscriptions
            else if (tCommand.contentEquals("CLEAR")) {
                tWsSessionCntx.mFilter.remove( tWsSessionCntx );
                tSubscrRsp = new SubscriptionResponse().setIsOk(true).setMessage("cleared subscription");
            } else {
                throw new JsonSyntaxException(tMsg);
            }

        } catch (Exception je) {
            mLog.warn("message from ws client: " + tMsg + " client: " + tWsSessionCntx.toString());
            tSubscrRsp = new SubscriptionResponse().setIsOk(false).setMessage( je.getMessage());
        }

        // Send response back to clients
        try {
            sendMsgToClient( tWsSessionCntx, tSubscrRsp.toJson().toString());
        }
        catch( IOException ie ) {
            mLog.warn("failed to send wss message to " + tWsSessionCntx.toString());
            synchronized ( mSessions ) {
                mSessions.remove(mSessions.remove(tWsSessionCntx.mWsSession.getId()));
            }
            mLog.warn("ws session " + tWsSessionCntx.toString() + " removed");
        }
    }



    SubscriptionRequest parseSubscriptionRequest( String pRequestMsg )  throws JsonSyntaxException
    {
        JsonObject jMsg = JsonParser.parseString( pRequestMsg ).getAsJsonObject();
        String tRqstMsgStr = AuxJson.tagMessageBody( SubscriptionRequest.NAME, pRequestMsg );
        SubscriptionRequest tSubScr = new SubscriptionRequest( tRqstMsgStr );
        return tSubScr;
    }




    private void sendMsgToClient( WebSocketSessionCntx pWssCntx, String pMessage  ) throws IOException
    {
            pWssCntx.mWsSession.sendMessage( new TextMessage(  pMessage ) );
    }


    public void sendPublicBdx( EngineBdxInterface pBdx) {
        synchronized (mSessions) {
            Iterator<String> tKeyItr = mSessions.keys().asIterator();
            while (tKeyItr.hasNext()) {
                WebSocketSessionCntx wscntx = mSessions.get(tKeyItr.next());
                wscntx.mFilter.match( pBdx.getSubjectName(), pBdx);
            }
        }
    }

    void sendPrivateBdx(String  pAccountId, EngineBdxInterface pBdx) {
        List<SessionCntxInterface> tSessCntxLst = TeAppCntx.getInstance().getSessionController().getSessionContextByAccount(pAccountId);
        if (tSessCntxLst == null) {
            return;
        }

        for( SessionCntxInterface tSessionCntx : tSessCntxLst) {
            if (tSessionCntx.getMarketDataSessionId() != null) {
                WebSocketSessionCntx tWsSessionCntx = mSessions.get(tSessionCntx.getMarketDataSessionId());
                if (tWsSessionCntx != null) {
                    tWsSessionCntx.mFilter.match(pBdx.getSubjectName(), pBdx);
                }
            }
        }
    }

    @Override
    public void distributorUpdate(String pSubjectName, EngineBdxInterface pBdxMessage, Object pCallbackParameter) {
        WebSocketSessionCntx tWssCntx = (WebSocketSessionCntx) pCallbackParameter;

        try {
            sendMsgToClient( tWssCntx, pBdxMessage.toJson().toString());
        }
        catch( IOException ie) {
            mLog.warn("failed to send wss message to " + tWssCntx.toString());
            synchronized ( mSessions ) {
                mSessions.remove(tWssCntx.mWsSession.getId());
            }
            mLog.warn("ws session " + tWssCntx.toString() + " removed");
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
        SubscriptionFilter      mFilter;

        WebSocketSessionCntx( WebSocketSession pWsSession, SessionCntxInterface pHttpSessionCntx ) {
            mWsSession = pWsSession;
            mHttpSessionCntx = pHttpSessionCntx;
            mFilter = new SubscriptionFilter();
        }

        public String toString() {
            return " id: " + mWsSession.getId() + " user: " + mHttpSessionCntx.getAccount();
        }
    }
}


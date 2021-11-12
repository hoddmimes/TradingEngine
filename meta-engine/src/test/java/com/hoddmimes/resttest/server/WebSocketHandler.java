package com.hoddmimes.resttest.server;

import com.google.gson.JsonSyntaxException;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class WebSocketHandler extends TextWebSocketHandler  {
    private  static  SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss.SSS");
    private static WebSocketHandler cInstance = null;
    private LinkedList<WebSocketSession> mSessions;



    public WebSocketHandler() {
        synchronized ( WebSocketHandler.class ) {
            if (cInstance == null) {
                cInstance = this;
                mSessions = new LinkedList<>();
            }
        }
        System.out.println("-- WebSocketHandler");
    }

    public static WebSocketHandler getInstance() {
        return cInstance;
    }

    @Override
    public void handleTextMessage(WebSocketSession pSession, TextMessage pMessage)
            throws InterruptedException, IOException {
        /**
         * Entry point for handling messages from web socket clients
         * Clients can send the following text messages (Json formatted)
         * - AddSubscriptionRqst, add subscription
         * - Remove SubscriptionRqst, removes one or more subscription
         */
        try {
            String tMsgString = pMessage.getPayload();
            System.out.println( "--handleTextMessage from: " +pSession.getRemoteAddress() + " msg: " + tMsgString);
            return;
        } catch (JsonSyntaxException je) {
            je.printStackTrace();
        }
    }




    @Override
    public void afterConnectionEstablished(WebSocketSession pSession) throws Exception {
        StringBuilder esb  = new StringBuilder();

        //the messages will be broadcasted to all users.
        List<WebSocketExtension> tExtensions = pSession.getExtensions();
        for( WebSocketExtension tExtention : tExtensions) {
            esb.append("\n          " + tExtention.getName());
            for( Map.Entry<String,String> tParam : tExtention.getParameters().entrySet()) {
                esb.append("\n               " + tParam.getKey() + "  :  " + tParam.getValue());
            }
        }
        synchronized ( mSessions ) {
            mSessions.add( pSession );
        }
        System.out.println("--afterConnectionEstablished \n" + esb.toString());
    }

    @Override
    public void handleTransportError(WebSocketSession pSession, Throwable exception) throws Exception {
        System.out.println("--handleTransportError \n" + exception.toString());
        try {
            pSession.close();
        }
        catch( Exception e) {}
    }

    public void sendBdx( String pBdx ) {
        WebSocketMessage<String> tMessage = new TextMessage( pBdx );
        synchronized ( mSessions ) {
            for( WebSocketSession tSession : mSessions ) {
                try {
                    tSession.sendMessage( tMessage );
                }
                catch( IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception
    {
        synchronized ( mSessions ) {
            mSessions.remove();
        }
        System.out.println("--afterConnectionClosed" );
    }
}


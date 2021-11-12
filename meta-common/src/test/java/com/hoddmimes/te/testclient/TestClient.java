package com.hoddmimes.te.testclient;

import com.google.gson.JsonObject;
import com.hoddmimes.te.common.transport.http.TeWebsocketClient;
import com.hoddmimes.te.common.transport.http.TeHttpClient;



import javax.websocket.*;
import java.io.IOException;


@ClientEndpoint
public class TestClient extends Endpoint {
    private static String BASE_URI = "https://localhost:8883/te/";
    private static String WSS_URI = "wss://localhost:8883/marketdata";

    private TeWebsocketClient tWssClient;
    private TeHttpClient tHttpClient;

    public static void main(String[] args) {
        System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");
        System.setProperty("java.util.logging.SimpleFormatter.format", "");

        TestClient tc = new TestClient();
        tc.init();
        tc.test();
    }

    private void init() {
        String tAuthId = null;

        tHttpClient = new TeHttpClient(BASE_URI, true);
        try {
            JsonObject jLogonRsp = tHttpClient.post("{'username':'test','password':'test','ref' :'0001'}".replace('\'', '"'), "logon");
            tAuthId = jLogonRsp.get("sessionAuthId").getAsString();
        }
        catch( Exception e) { e.printStackTrace();}

        tWssClient = new TeWebsocketClient( WSS_URI, tAuthId, this );
        //tWssClient.sendMessage("{'from':'TestClient','source':'WssClient'".replace('\'','"'));
        System.out.println("Init");
    }


    private void test() {
    JsonObject jResponse;
        try {
            jResponse = tHttpClient.post("{'symbol':'AMZN','price':100.0,'volume':20,'side':'BUY','ref' :'0002'}".replace('\'','"'), "addOrder");
            System.out.println( jResponse.toString());
            //tHttpClient.post("{'foo':'bar','kalle':'frotz'}".replace('\'','"'), "frotz");
            //tHttpClient.post("{'foo':'bar','kalle':'frotz'}".replace('\'','"'), "frotz");
            //tHttpClient.post("{'foo':'bar','kalle':'frotz'}".replace('\'','"'), "frotz");
        }
        catch( IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onOpen(Session session, EndpointConfig config) {
        System.out.println("WSS socket session established");
    }
}

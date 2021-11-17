package com.hoddmimes.te.testclient;

import com.google.gson.JsonObject;
import com.hoddmimes.te.common.transport.http.TeWebsocketClient;
import com.hoddmimes.te.common.transport.http.TeHttpClient;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewResponseBodyAdvice;


import javax.websocket.*;
import java.io.IOException;
import java.math.BigDecimal;


@ClientEndpoint
public class TestClient extends Endpoint {
    private enum Side { BUY, SELL};
    private static String BASE_URI = "https://localhost:8883/te/";
    private static String WSS_URI = "wss://localhost:8883/marketdata";
    private long tRef = (System.currentTimeMillis() / 10000L);

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
    }

    private void createOrders( String pSymbol, Side pSide, double pPrice, int pOrdersOnLevel, int pLevels ) throws Exception{
        JsonObject jResponse;
        int tVol = 10;
        BigDecimal bdStep = new BigDecimal( "0.1");
        BigDecimal bdPrice = new BigDecimal( Double.toString( pPrice));

        for (int pl = 1; pl <= pLevels; pl++) {
            for (int ol = 1; ol <= pOrdersOnLevel; ol++) {
                jResponse = tHttpClient.post(("{'symbol':'" + pSymbol+ "','price':" + bdPrice.toString() +",'volume':" + (tVol * ol) +",'side':'" +
                        pSide.name() +"','ref' :'" + Long.toHexString( (tRef++)) +"'}").replace('\'','"'), "addOrder");
            }
            bdPrice = (pSide == pSide.BUY) ? bdPrice.subtract( bdStep) : bdPrice.add( bdStep);
            tVol *= pl;
        }
    }

    private void chill( long pTime ) {
        try { Thread.sleep( pTime ); }
        catch( InterruptedException ie) {}
    }


    private void test() {
    JsonObject jResponse;
        try {

            createOrders("AMZN", Side.BUY, 99.90d, 4, 5);
            createOrders("AMZN", Side.SELL, 100.10d, 4, 5);

            jResponse = tHttpClient.post("{'ref' :'2222'}".replace('\'','"'), "queryOwnOrders");
            jResponse = tHttpClient.post("{'ref' :'2223'}".replace('\'','"'), "queryOwnOrders");
            jResponse = tHttpClient.post("{'ref' :'2224'}".replace('\'','"'), "queryOwnOrders");
            jResponse = tHttpClient.post("{'ref' :'2225'}".replace('\'','"'), "queryOwnOrders");

            jResponse = tHttpClient.post("{'symbol':'AMZN','price':100.0,'volume':22,'side':'BUY','ref' :'0003'}".replace('\'','"'), "addOrder");
            chill( 1000L);
            jResponse = tHttpClient.post("{'symbol':'AMZN','price':100.0,'volume':22,'side':'BUY','ref' :'0004'}".replace('\'','"'), "addOrder");
            chill( 1000L);
            jResponse = tHttpClient.post("{'symbol':'AMZN','price':100.0,'volume':22,'side':'BUY','ref' :'0005'}".replace('\'','"'), "addOrder");
            chill( 1000L);

            System.out.println("foo");

            //String tOrderId = jResponse.get("orderId").getAsString();
            //jResponse = tHttpClient.post(("{'symbol':'AMZN','orderId':'" + tOrderId + "','ref' :'0004b','deltaVolume': 4 }").replace('\'','"'), "amendOrder");
            //System.out.println(jResponse.toString());
            /*
            jResponse = tHttpClient.post("{'symbol':'AMZN','price':100.0,'volume':22,'side':'BUY','ref' :'0003'}".replace('\'','"'), "addOrder");
            jResponse = tHttpClient.post("{'symbol':'AMZN','price':99.75,'volume':40,'side':'BUY','ref' :'0004'}".replace('\'','"'), "addOrder");

            String tOrderId = jResponse.get("orderId").getAsString();
            jResponse = tHttpClient.post(("{'symbol':'AMZN','orderId':'" + tOrderId + "','ref' :'0004b'}").replace('\'','"'), "deleteOrder");


            jResponse = tHttpClient.post("{'symbol':'AMZN','price':100.25,'volume':20,'side':'SELL','ref' :'0005'}".replace('\'','"'), "addOrder");
            jResponse = tHttpClient.post("{'symbol':'AMZN','price':100.50,'volume':10,'side':'SELL','ref' :'0006'}".replace('\'','"'), "addOrder");
            jResponse = tHttpClient.post("{'symbol':'AMZN','price':100.50,'volume':10,'side':'SELL','ref' :'0007'}".replace('\'','"'), "addOrder");
            jResponse = tHttpClient.post("{'symbol':'AMZN','price':100.50,'volume':10,'side':'SELL','ref' :'0008'}".replace('\'','"'), "addOrder");
            jResponse = tHttpClient.post("{'symbol':'AMZN','ref' :'0009'}".replace('\'','"'), "queryOrderbook");
            */

        }
        catch( Exception e) {
            e.printStackTrace();
        }

        try { Thread.sleep(15000L);}
        catch( InterruptedException e) {}

    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        System.out.println("WSS socket session established");
    }
}

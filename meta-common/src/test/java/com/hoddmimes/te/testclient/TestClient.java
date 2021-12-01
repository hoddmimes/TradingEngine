package com.hoddmimes.te.testclient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.transport.http.TeRequestException;
import com.hoddmimes.te.common.transport.http.TeWebsocketClient;
import com.hoddmimes.te.common.transport.http.TeHttpClient;
import com.hoddmimes.te.messages.generated.AddOrderResponse;


import javax.websocket.*;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@ClientEndpoint
public class TestClient implements  TeWebsocketClient.WssCallback {
    private static SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss.SSS");
    private enum MsgType { RESP,RQST };

    private OrderIdContainer mOrderIdContainer = new OrderIdContainer();
    private JsonObject mCmdRoot;
    private JsonArray mRequests;
    private long tRef = (System.currentTimeMillis() / 10000L);
    private Gson mGson = new Gson();



    private TeWebsocketClient tWssClient;
    private TeHttpClient tHttpClient;

    public static void main(String[] args) {
        System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");
        System.setProperty("java.util.logging.SimpleFormatter.format", "");

        if (args.length == 0) {
            System.out.println("script file argument missing");
            System.exit(0);
        }


        TestClient tc = new TestClient();
        tc.loadCommandFile( args[0]);
        tc.init();
        tc.test();
    }

    private void msglog( JsonObject pMsg  )
    {
        if (pMsg == null) {
            System.out.println(SDF.format(System.currentTimeMillis()) + " [RESP]  msg: <null message>");
            return;
        }

        if (pMsg.has("endpoint")) {
            String tEndpoint = pMsg.get("endpoint").getAsString();
            String tMethod = pMsg.get("endpoint").getAsString();
            JsonObject jRqstMsg = (pMsg.has("body")) ? pMsg.get("body").getAsJsonObject() : null;
            if (jRqstMsg != null) {
                System.out.println(SDF.format(System.currentTimeMillis()) + " [RQST] (" + tMethod + ") endpoint: <" + tEndpoint + "> msg: " + mGson.toJson(jRqstMsg).toString());
            } else {
                System.out.println(SDF.format(System.currentTimeMillis()) + " [RQST] (" + tMethod + ") endpoint: <" + tEndpoint + ">");
            }
        } else {
            System.out.println(SDF.format(System.currentTimeMillis()) + " [RESP]  msg: " + mGson.toJson(pMsg).toString());
        }
    }

    private void msglog( String  pMsg  ) {
        System.out.println(SDF.format(System.currentTimeMillis()) + pMsg);
    }


    private void loadCommandFile( String pCommandfile ) {
        try {
            FileReader tCmdFileReader = new FileReader( pCommandfile );
            mCmdRoot = JsonParser.parseReader(tCmdFileReader).getAsJsonObject();
            mRequests = mCmdRoot.get("requests").getAsJsonArray();
        }
        catch( IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }



    private void init() {
        String tAuthId = null;

        tHttpClient = new TeHttpClient(AuxJson.navigateString( mCmdRoot,"baseHttp/uri"), false);
        try {
            JsonObject tLogonRqst = mRequests.get(0).getAsJsonObject();
            msglog( tLogonRqst );
            JsonObject jLogonRsp = tHttpClient.post( tLogonRqst.get("body").getAsJsonObject().toString(), tLogonRqst.get("endpoint").getAsString());
            msglog( jLogonRsp );
            tAuthId = jLogonRsp.get("sessionAuthId").getAsString();
        }
        catch( IOException e) { e.printStackTrace();}
        catch( TeRequestException tre) {
            msglog( tre.toJson() );
        }

        tWssClient = new TeWebsocketClient( AuxJson.navigateString(mCmdRoot,"baseWss/uri"), tAuthId, this );

    }








    private void chill( long pTime ) {
        try { Thread.sleep( pTime ); }
        catch( InterruptedException ie) {}
    }


    private void test() {
        for (int i = 1; i < mRequests.size(); i++) {
            JsonObject tRqst = mRequests.get(i).getAsJsonObject();
            try {
                JsonObject tRspMsg = null;

                msglog( tRqst );

                mOrderIdContainer.orderIdReplace( tRqst );

                if (tRqst.get("method").getAsString().contentEquals("POST")) {
                    if (tRqst.get("endpoint").getAsString().contentEquals("marketdata")) {
                        tRspMsg = sendSubscriptionRequest( tRqst.get("body").getAsJsonObject() );
                    } else {
                        tRspMsg = tHttpClient.post(tRqst.get("body").getAsJsonObject().toString(), tRqst.get("endpoint").getAsString());
                    }
                } else if (tRqst.get("method").getAsString().contentEquals("GET")) {
                    tRspMsg = tHttpClient.get(tRqst.get("endpoint").getAsString());
                } else if (tRqst.get("method").getAsString().contentEquals("DELETE")) {
                    tRspMsg = tHttpClient.delete(tRqst.get("endpoint").getAsString());
                } else {
                    throw new RuntimeException("Unknown request method");
                }
                mOrderIdContainer.processResponse( tRqst, tRspMsg );




                msglog( tRspMsg );
            }
            catch( IOException e) {
                e.printStackTrace();
            }
            catch( TeRequestException tre) {
                msglog( tre.toJson() );
            }
        }

        try { Thread.sleep(5000L);}
        catch( InterruptedException e) {}

    }


    private JsonObject sendSubscriptionRequest( JsonObject jRqstBody ) {
        tWssClient.sendMessage( jRqstBody.toString() );

        JsonObject jObject = new JsonObject();
        jObject.addProperty("endpoint", "subscription");
        jObject.add( "body", jRqstBody );
        return jObject;
    }


    @Override
    public void onOpen(Session session, EndpointConfig config) {
        System.out.println("WSS socket session established");
    }

    @Override
    public void onMessage(String pBdxMsg) {
        msglog(" [BDX] " +pBdxMsg);
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {

    }

    @Override
    public void onError(Session session, Throwable throwable)
    {

    }

    class OrderIdContainer {
        Pattern mRefPattern;
        HashMap<String, String> mRefOrderIdmap;

        OrderIdContainer() {
            mRefOrderIdmap = new HashMap<>();
            mRefPattern = Pattern.compile("%ref-([^%]+)%");
        }

        public void processResponse(JsonObject pRqstMsg, JsonObject pRspMsg) {
            if (pRqstMsg.get("endpoint").getAsString().contentEquals("addOrder")) {
                if (pRspMsg.has("orderId")) {
                    mRefOrderIdmap.put( pRspMsg.get("ref").getAsString(), pRspMsg.get("orderId").getAsString());
                }
            }
        }

        public void orderIdReplace( JsonObject pRqstMsg ) {

            if (pRqstMsg.has("body")) {
                JsonObject jRqstBody = pRqstMsg.get("body").getAsJsonObject();
                if (jRqstBody.has("orderId")) {
                    String tOrderId = jRqstBody.get("orderId").getAsString();
                    Matcher m = mRefPattern.matcher(tOrderId);
                    if (m.matches()) {
                        tOrderId = mRefOrderIdmap.get(String.valueOf(m.group(1)));
                        if (tOrderId != null) {
                            jRqstBody.addProperty("orderId", tOrderId);
                        }
                    }
                }
            }
        }
    }
}

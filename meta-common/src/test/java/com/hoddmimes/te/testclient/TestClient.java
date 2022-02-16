package com.hoddmimes.te.testclient;

import com.google.gson.*;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.transport.http.TeRequestException;
import com.hoddmimes.te.common.transport.http.TeWebsocketClient;
import com.hoddmimes.te.common.transport.http.TeHttpClient;


import javax.websocket.*;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
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
    private NumberFormat numfmt;


    private TeWebsocketClient tWssClient;
    private TeHttpClient tTEHttpClient;

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

    public TestClient()
    {
        numfmt = NumberFormat.getInstance();
        numfmt.setGroupingUsed(false);
        numfmt.setMinimumIntegerDigits(2);
        numfmt.setMaximumFractionDigits(2);
    }

    private void msglog( JsonObject pMsg  )
    {
        if (pMsg == null) {
            System.out.println(SDF.format(System.currentTimeMillis()) + " [RESP]  msg: <null message>");
            return;
        }

        JsonObject tMsg = pMsg.deepCopy();
        adjustFromPriceValues( tMsg );

        if (tMsg.has("endpoint")) {
            String tEndpoint = tMsg.get("endpoint").getAsString();
            String tMethod = tMsg.get("endpoint").getAsString();
            JsonObject jRqstMsg = (tMsg.has("body")) ? tMsg.get("body").getAsJsonObject() : null;
            if (jRqstMsg != null) {
                System.out.println(SDF.format(System.currentTimeMillis()) + " [RQST] (" + tMethod + ") endpoint: <" + tEndpoint + "> msg: " + mGson.toJson(jRqstMsg).toString());
            } else {
                System.out.println(SDF.format(System.currentTimeMillis()) + " [RQST] (" + tMethod + ") endpoint: <" + tEndpoint + ">");
            }
        } else {
            System.out.println(SDF.format(System.currentTimeMillis()) + " [RESP]  msg: " + mGson.toJson(tMsg).toString());
        }
    }

    private void msglog( String  tMsg  ) {
        System.out.println(SDF.format(System.currentTimeMillis()) + tMsg);
    }


    private void loadCommandFile( String pCommandfile ) {
        try {
            FileReader tCmdFileReader = new FileReader( pCommandfile );
            mCmdRoot = JsonParser.parseReader(tCmdFileReader).getAsJsonObject();
            mRequests = mCmdRoot.get("requests").getAsJsonArray();
            adjustToPriceValues( mRequests );
        }
        catch( IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    double scaleDownPrice( long pLongValue ) {
        double tDoubleValue = (double) ((double) pLongValue / 10000.0);
        return tDoubleValue;
    }

    long scaleUpPrice( double pDoubleValue ) {
        return (long) (pDoubleValue * 10000L);
    }

    private void adjustFromPriceValues(JsonObject jObject) {
        Iterator<String> tKeyItr = jObject.keySet().iterator();
        while (tKeyItr.hasNext()) {
            String tKey = tKeyItr.next();
            JsonElement jElement = jObject.get(tKey);
            if (jElement.isJsonObject()) {
                adjustFromPriceValues(jElement.getAsJsonObject());
            } else if (jElement.isJsonArray()) {
                JsonArray jArr = jElement.getAsJsonArray();
                for (int i = 0; i < jArr.size(); i++) {
                    JsonElement tArrElement = jArr.get(i);
                    if (tArrElement.isJsonObject()) {
                        adjustFromPriceValues(tArrElement.getAsJsonObject());
                    }
                }
            } else {
                if (tKey.contentEquals("price")) {
                    jObject.addProperty("price", scaleDownPrice(jObject.get("price").getAsLong()));
                }
                if (tKey.contentEquals("tickSize")) {
                    jObject.addProperty("tickSize", scaleDownPrice(jObject.get("tickSize").getAsLong()));
                }
                if (tKey.contentEquals("offer")) {
                    jObject.addProperty("offer", scaleDownPrice(jObject.get("offer").getAsLong()));
                }
                if (tKey.contentEquals("bid")) {
                    jObject.addProperty("bid", scaleDownPrice(jObject.get("bid").getAsLong()));
                }
            }
        }
    }



    private void adjustToPriceValues( JsonArray jRequests ) {
        for (int i = 0; i < jRequests.size(); i++) {
            JsonObject jRqstMsg = jRequests.get(i).getAsJsonObject();
            if (jRqstMsg.has("body")) {
                JsonObject jRqst = jRqstMsg.get("body").getAsJsonObject();
                if (jRqst.has("price")) {
                    jRqst.addProperty("price", scaleUpPrice(jRqst.get("price").getAsDouble()));
                }
            }
        }
    }

    private void init() {
        String tAuthId = null;

        tTEHttpClient = new TeHttpClient(AuxJson.navigateString( mCmdRoot,"baseHttp/uri"), false);
        try {
            JsonObject tLogonRqst = mRequests.get(0).getAsJsonObject();
            msglog( tLogonRqst );
            JsonObject jLogonRsp = tTEHttpClient.post( tLogonRqst.get("body").getAsJsonObject().toString(), tLogonRqst.get("endpoint").getAsString());
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

                mOrderIdContainer.orderIdReplace( tRqst );
                msglog( tRqst );

                if (tRqst.get("method").getAsString().contentEquals("POST")) {
                    if (tRqst.get("endpoint").getAsString().contentEquals("te-marketdata")) {
                        tRspMsg = sendSubscriptionRequest( tRqst.get("body").getAsJsonObject() );
                    } else {
                        tRspMsg = tTEHttpClient.post(tRqst.get("body").getAsJsonObject().toString(), tRqst.get("endpoint").getAsString());
                    }
                } else if (tRqst.get("method").getAsString().contentEquals("GET")) {
                    tRspMsg = tTEHttpClient.get(tRqst.get("endpoint").getAsString());
                } else if (tRqst.get("method").getAsString().contentEquals("DELETE")) {
                    tRspMsg = tTEHttpClient.delete(tRqst.get("endpoint").getAsString());
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
    public void onMessage(String jBdxMsgStr) {
        JsonObject jBdxMsg = JsonParser.parseString( jBdxMsgStr ).getAsJsonObject();
        adjustFromPriceValues( jBdxMsg );
        msglog(" [BDX] " + jBdxMsg.toString());
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

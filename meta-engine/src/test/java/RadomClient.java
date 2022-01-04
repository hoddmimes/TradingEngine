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

import com.google.gson.*;
import com.hoddmimes.te.common.transport.http.TeHttpClient;
import com.hoddmimes.te.common.transport.http.TeRequestException;
import com.hoddmimes.te.common.transport.http.TeWebsocketClient;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@ClientEndpoint
public class RadomClient  {
    static  AtomicLong REF = new AtomicLong(System.currentTimeMillis() / 10000L);
    enum RqstMethod {POST,GET,DELETE};
    enum OrderSide { BUY,SELL};
    Gson mGson = new Gson();
    Random rnd = new Random( System.nanoTime());
    long mWaitBeforeExit = 2000L;

    private static SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss.SSS");

    private OrderIdContainer mOrderIdContainer = new OrderIdContainer();
    private List<Client> mClients = new ArrayList<>();
    private List<InstrumentRef> mInstruments = new ArrayList<>();
    String mTeHttpsURI;
    String mTeWssURI;

    int mOrderCount;
    int mMatchOneTo;



    public static void main(String[] args) {
        System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");
        System.setProperty("java.util.logging.SimpleFormatter.format", "");

        if (args.length == 0) {
            System.out.println("script file argument missing");
            System.exit(0);
        }

        RadomClient rc = new RadomClient();
        if (args.length == 2) {
            rc.mWaitBeforeExit = Long.parseLong( args[1] ) * 1000L;
        }
        rc.loadScript( args[0] );
        rc.connect();
        rc.execute();
    }

    private void connect() {
        for (Client clt : mClients) {
            clt.logon();
        }
    }

    private void execute() {

        for( int i = 0; i < mOrderCount; i++ ) {
            Client tClient = mClients.get( rnd.nextInt( mClients.size()));
            InstrumentRef tInstrument = mInstruments.get( rnd.nextInt( mInstruments.size()));
            boolean tShouldMatch = (rnd.nextInt( mMatchOneTo) == 0) ? true : false;
            OrderSide tSide = (rnd.nextBoolean()) ? OrderSide.BUY : OrderSide.SELL;
            TxRqst txRqst = createOrder( tInstrument, tShouldMatch, tSide);

            try {
                JsonObject tRspMsg = tClient.post( txRqst );
                mOrderIdContainer.processResponse( txRqst.toJson(), tRspMsg );
            }
            catch( IOException e) {
                e.printStackTrace();
            }
            catch( TeRequestException tre) {
                msglog(tClient.mAccount, tre.toJson() );
            }
        }
        try { Thread.sleep(mWaitBeforeExit);}
        catch( InterruptedException ie) {}
    }



    private TxRqst genOrder( String pSymbol, String pSide, double pPrice, int pQuantity ) {
        NumberFormat nfmt = NumberFormat.getInstance(Locale.US);
        nfmt.setMinimumFractionDigits(2);
        nfmt.setMaximumFractionDigits(2);
        nfmt.setGroupingUsed(false);

        String tOrderStr = "{\"sid\":\"" + pSymbol + "\",\"price\":"  + nfmt.format(pPrice) + ",\"quantity\": " + pQuantity + ",\"side\":\"" + pSide + "\",\"ref\" : \"" + Long.toHexString( REF.incrementAndGet()) +"\"}";
        TxRqst txRqst = new TxRqst(RqstMethod.POST,"addOrder", tOrderStr );
        return txRqst;
    }



    private TxRqst createOrder( InstrumentRef pInstRef, boolean pShouldMatch, OrderSide pSide) {
        int qty = (rnd.nextInt(40) + 50);
        double price = 0.0;

        if (pSide == OrderSide.BUY) {
             double m = (pShouldMatch) ? 1 : -1;
             price = (((rnd.nextInt(9) * 0.1) + 0.1) * m) + pInstRef.getRefPrice();
        } else {
            double m = (pShouldMatch) ? -1 : 1;
            price = (((rnd.nextInt(9) * 0.1) + 0.1) * m) + pInstRef.getRefPrice();
        }
        return genOrder( pInstRef.mSid, pSide.name(), price, qty );
    }

    private void loadScript( String pScriptfile ) {
        try {
            FileReader tCmdFileReader = new FileReader( pScriptfile );
            JsonObject tScriptRoot = JsonParser.parseReader(tCmdFileReader).getAsJsonObject();

            // Parse connection URIs
            mTeHttpsURI = tScriptRoot.get("baseHttp").getAsJsonObject().get("uri").getAsString();
            mTeWssURI = tScriptRoot.get("baseWss").getAsJsonObject().get("uri").getAsString();

            JsonObject jExecution = tScriptRoot.get("execution").getAsJsonObject();
            mOrderCount = jExecution.get("orders").getAsInt();
            mMatchOneTo = jExecution.get("matchingRateOneTo").getAsInt();

            // Parse instruments
            JsonArray jInstArr = tScriptRoot.get("instruments").getAsJsonArray();
            for (int i = 0; i < jInstArr.size(); i++) {
                mInstruments.add( new InstrumentRef( jInstArr.get(i).getAsJsonObject()));
            }
            //Parse and load
            JsonArray jClientArr = tScriptRoot.get("clients").getAsJsonArray();
            for (int i = 0; i < jClientArr.size(); i++) {
                mClients.add( new Client( jClientArr.get(i).getAsJsonObject()));
            }
        }
        catch( IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }





    void msglog( String pUsername, JsonObject pMsg  )
    {

        if (pMsg == null) {
            System.out.println(SDF.format(System.currentTimeMillis()) + " [" + pUsername + "] [RESP]  msg: <null message>");
            return;
        }

        if (pMsg.has("endpoint")) {
            String tEndpoint = pMsg.get("endpoint").getAsString();
            String tMethod = pMsg.get("endpoint").getAsString();
            JsonObject jRqstMsg = (pMsg.has("body")) ? pMsg.get("body").getAsJsonObject() : null;
            if (jRqstMsg != null) {
                System.out.println(SDF.format(System.currentTimeMillis()) + " [" + pUsername + "] [RQST] (" + tMethod + ") endpoint: <" + tEndpoint + "> msg: " + mGson.toJson(jRqstMsg).toString());
            } else {
                System.out.println(SDF.format(System.currentTimeMillis()) + " [" + pUsername + "] [RQST] (" + tMethod + ") endpoint: <" + tEndpoint + ">");
            }
        } else {
            Gson tGson = new Gson();
            System.out.println(SDF.format(System.currentTimeMillis()) + " [" + pUsername + "] [RESP]  msg: " + tGson.toJson(pMsg).toString());
        }
    }

    void msglog( String pUsername, String  pMsg  ) {
        System.out.println(SDF.format(System.currentTimeMillis()) + "[" + pUsername + "] " +  pMsg);
    }


    private void test() {

    }








     class  Client implements TeWebsocketClient.WssCallback {
        private TeWebsocketClient mWss;
        private TeHttpClient mHttp;
        private String mAccount;
        private String mPassword;
        private boolean mUseWss;

        Client( JsonObject jClient ) {
            mAccount = jClient.get("account").getAsString();
            mPassword = jClient.get("password").getAsString();
            mUseWss = jClient.get("broadcast").getAsBoolean();
        }


         JsonObject post( TxRqst txRqst ) throws IOException, TeRequestException {
             msglog(mAccount, txRqst.toJson());
             JsonObject jRsp = mHttp.post(txRqst.getRqstObject(), txRqst.getEndpoint());
             msglog(mAccount, jRsp);
             return jRsp;
         }

         JsonObject get( TxRqst txRqst ) throws Exception {
             msglog(mAccount, txRqst.toJson());
             JsonObject tRspMsg = mHttp.get( txRqst.getEndpoint() );
             msglog(mAccount, tRspMsg );
             return tRspMsg;
         }

        JsonObject delete( TxRqst txRqst ) throws Exception {
            msglog(mAccount, txRqst.toJson());
            JsonObject tRspMsg = mHttp.delete( txRqst.getEndpoint() );
            msglog(mAccount, tRspMsg );
            return tRspMsg;
        }



        void logon() {
            try {
                mHttp = new TeHttpClient( mTeHttpsURI, false);

                String pLogonRqst = "{'account': '" + mAccount + "', 'password': '" + mPassword +  "', 'ref' : '" + Long.toHexString( REF.incrementAndGet()) + "'}";
                TxRqst txRqst = new TxRqst(RqstMethod.POST,"logon", pLogonRqst );

                JsonObject jLogonRsp = post( txRqst );
                String tAuthId = jLogonRsp.get("sessionAuthId").getAsString();
                if (mUseWss) {
                    mWss = new TeWebsocketClient( mTeWssURI, tAuthId, this );
                    String jSubRqstStr = "{'command':'ADD', 'topic' : '/BdxBBO/...'}".replace('\'','\"');
                    JsonObject tSubRsp = sendSubscriptionRequest(JsonParser.parseString( jSubRqstStr ).getAsJsonObject());
                    msglog(mAccount, jSubRqstStr );
                }

            }
            catch( IOException e) { e.printStackTrace();}
            catch( TeRequestException tre) {
                msglog(mAccount, tre.toJson() );
            }
        }


        private JsonObject sendSubscriptionRequest( JsonObject jRqstBody ) {
            mWss.sendMessage( jRqstBody.toString() );

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
            msglog(mAccount, " [BDX] " + pBdxMsg);
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            msglog(mAccount, "  [wss close] reason: " + closeReason);
        }

        @Override
        public void onError(Session session, Throwable throwable)
        {
            msglog(mAccount, "  [wss error] reason: " + throwable.getMessage());
        }


    }

    static class TxRqst  {
        private JsonObject jtx;

        TxRqst(RqstMethod pMethod, String pEndpoint ) {
            jtx = new JsonObject();
            jtx.addProperty("method", pMethod.name());
            jtx.addProperty("endpoint", pEndpoint );
        }

        TxRqst(RqstMethod pMethod, String pEndpoint, String pJsonRequest ) {
            jtx = new JsonObject();
            jtx.addProperty("method", pMethod.name());
            jtx.addProperty("endpoint", pEndpoint );
            try {
                jtx.add("body", JsonParser.parseString((pJsonRequest.replace('\'','\"'))));
            }
            catch(JsonSyntaxException jse) {
                System.out.println("Invalid Post Request String : " + (pJsonRequest.replace('\'','\"')));
                System.exit(0);
            }
        }

        String getEndpoint() {
            return jtx.get("endpoint").getAsString();
        }

        RqstMethod getMethod() {
            return RqstMethod.valueOf( jtx.get("method").getAsString());
        }

        JsonObject getRqstObject() {
            if (jtx.has("body")) {
                return jtx.get("body").getAsJsonObject();
            }
            return null;
        }

        JsonObject toJson() {
            return jtx;
        }

        public String toString() {
            return jtx.toString();
        }
    }

    class InstrumentRef {
        private String mSid;
        private double mRefPrice;

        InstrumentRef( JsonObject pInstRef ) {
            mSid = pInstRef.get("sid").getAsString();
            mRefPrice = Double.parseDouble( pInstRef.get("refPrice").getAsString());
        }

        String getSid() {
            return mSid;
        }

        double getRefPrice() {
            return mRefPrice;
        }
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

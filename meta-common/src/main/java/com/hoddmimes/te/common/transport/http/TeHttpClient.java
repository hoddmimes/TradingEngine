
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

package com.hoddmimes.te.common.transport.http;


import com.google.gson.*;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;



import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;

public class TeHttpClient
{
	private static SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss.SSS");
	private static boolean VERBOSE = false;
	private static boolean ERROR_VERBOSE = true;


	private String  mBaseUrl;
	private CloseableHttpClient mHttpclient;
	private Gson mGsonPrinter;


	public TeHttpClient(String pBaseUrl, boolean pVerboseMode ) {
		mBaseUrl = pBaseUrl;
		VERBOSE = pVerboseMode;
		mGsonPrinter = new GsonBuilder().setPrettyPrinting().create();

		System.setProperty("org.apache.http", "org.apache.commons.logging.impl.NoOpLog");
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

		try {
			mHttpclient = createAcceptSelfSignedCertificateClient();
			//mHttpclient =  HttpClients.createDefault();
		}
		catch(Exception e ) { e.printStackTrace(); }
	}

	private void log( String pMessage )
	{
		log( pMessage, false);
	}

	private void log( String pMessage, boolean pError ) {
		if (VERBOSE || pError) {
			System.out.println(SDF.format(System.currentTimeMillis()) + " " + pMessage );
		}
	}






	public JsonObject get( String pDestination ) throws IOException, TeRequestException
	{
		HttpGet tGetRqst = new HttpGet( mBaseUrl + pDestination );
		log("[get] destination: " + tGetRqst.toString());
		CloseableHttpResponse tResponse = mHttpclient.execute(tGetRqst);

		if (tResponse.getStatusLine().getStatusCode() != 200) {
			String rspmsg  = readResponse( tResponse );
			String stsmsg = (getJsonStsMsg(rspmsg) != null) ? getJsonStsMsg(rspmsg) : tResponse.getStatusLine().getReasonPhrase();

			log("[Receive-Error] \n   " + " status-code: "  + stsmsg + " status: " + rspmsg, true);
			throw new TeRequestException( tResponse.getStatusLine().getStatusCode(), stsmsg, rspmsg  );
		}

		// Read response data from the response message
		String tResponseData = readResponse( tResponse );
		// Build Json response message
		JsonObject tJsonRsp = JsonParser.parseString( tResponseData ).getAsJsonObject();
		log("[Receive] \n   " + mGsonPrinter.toJson( tJsonRsp ));
		return tJsonRsp;
	}

	public JsonObject delete( String pDestination ) throws IOException,TeRequestException
	{
		HttpDelete tDelRqst = new HttpDelete( mBaseUrl + pDestination );
		log("[delete] destination: " + tDelRqst.toString());
		CloseableHttpResponse tResponse = mHttpclient.execute(tDelRqst);

		if (tResponse.getStatusLine().getStatusCode() != 200) {
			String rspmsg  = readResponse( tResponse );
			String stsmsg = (getJsonStsMsg(rspmsg) != null) ? getJsonStsMsg(rspmsg) : tResponse.getStatusLine().getReasonPhrase();

			log("[Receive-Error] \n   " + " status-code: "  + stsmsg + " status: " + rspmsg, true);
			throw new TeRequestException( tResponse.getStatusLine().getStatusCode(), stsmsg, rspmsg  );
		}

		// Read response data from the response message
		String tResponseData = readResponse( tResponse );
		// Build Json response message
		JsonObject tJsonRsp = JsonParser.parseString( tResponseData ).getAsJsonObject();
		log("[Receive] \n   " + mGsonPrinter.toJson( tJsonRsp ));
		return tJsonRsp;
	}


	public JsonObject post( JsonObject pJsonRqst, String pDestination) throws  IOException, TeRequestException
	{
		return post( pJsonRqst.toString(), pDestination );
	}


	public JsonObject post(String pJsonRqstString, String pDestination ) throws IOException, TeRequestException
	{
		// Construct HTTP/HTTPS request

		HttpPost tPostRqst = new HttpPost( mBaseUrl + pDestination );
		HttpEntity tStringEntity = new StringEntity(pJsonRqstString, ContentType.APPLICATION_JSON);
		tPostRqst.setEntity(tStringEntity);

		log("[post] destination: " + tPostRqst.toString() + "\n   " + mGsonPrinter.toJson( pJsonRqstString ) );
		CloseableHttpResponse tResponse = mHttpclient.execute(tPostRqst);


		if (tResponse.getStatusLine().getStatusCode() != 200) {
			String rspmsg  = readResponse( tResponse );
			String stsmsg = (getJsonStsMsg(rspmsg) != null) ? getJsonStsMsg(rspmsg) : tResponse.getStatusLine().getReasonPhrase();

			log("[Receive-Error] \n   " + " status-code: "  + stsmsg + " status: " + rspmsg, true);
			throw new TeRequestException( tResponse.getStatusLine().getStatusCode(), stsmsg, rspmsg  );
		}

		// Read response data from the response message
		String tResponseData = readResponse( tResponse );
		// Build Json response message
		JsonObject tJsonRsp = JsonParser.parseString( tResponseData ).getAsJsonObject();
		log("[Receive] \n   " + mGsonPrinter.toJson( tJsonRsp ));

		return tJsonRsp;
	}



	private String readResponse( CloseableHttpResponse pResponse ) throws IOException {
		int tSize = 0;
		char[] tBuffer = new char[256];
		InputStreamReader tIn = new InputStreamReader(pResponse.getEntity().getContent(), Consts.UTF_8);
		StringBuilder tBody = new StringBuilder();
		while (tSize >= 0) {
			tSize = tIn.read(tBuffer);
			if (tSize != -1) {
				tBody.append(tBuffer, 0, tSize);
			}
		}
		return tBody.toString();
	}


	private  CloseableHttpClient createAcceptSelfSignedCertificateClient()
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {

		// use the TrustSelfSignedStrategy to allow Self Signed Certificates
		SSLContext sslContext = SSLContextBuilder
				.create()
				.loadTrustMaterial(new TrustSelfSignedStrategy())
				.build();

		// we can optionally disable hostname verification.
		// if you don't want to further weaken the security, you don't have to include this.
		HostnameVerifier allowAllHosts = new NoopHostnameVerifier();

		// create an SSL Socket Factory to use the SSLContext with the trust self signed certificate strategy
		// and allow all hosts verifier.
		SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, allowAllHosts);

		// finally create the HttpClient using HttpClient factory methods and assign the ssl socket factory
		return HttpClients
				.custom()
				.setSSLSocketFactory(connectionFactory)
				.build();
	}

	private String getJsonStsMsg( String pResponseMessage ) {
		if (pResponseMessage == null) {
			return null;
		}

		try {
			JsonObject jObject = JsonParser.parseString( pResponseMessage ).getAsJsonObject();
			if (jObject.has("statusMessage")) {
				return jObject.get("statusMessage").getAsString();
			}
			return null;
		}
		catch( JsonSyntaxException e) {
			return null;
		}
	}
}

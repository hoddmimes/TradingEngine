
package com.hoddmimes.resttest.client;


import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class HttpClient
{
	private  static boolean USE_SSL = true;

	private String  mBaseUrl;
	private CloseableHttpClient mHttpclient;


	public HttpClient(String pBaseUrl ) {
		mBaseUrl = pBaseUrl;
		try {
			if (USE_SSL) {
				mHttpclient = createAcceptSelfSignedCertificateClient();
			} else {
				mHttpclient =  HttpClients.createDefault();
			}
		}
		catch(Exception e ) { e.printStackTrace(); }
	}

	public String get( String pDestination ) throws IOException
	{
		HttpGet tGetRqst = new HttpGet( mBaseUrl + pDestination );
		System.out.println("\n\n[get] destination: " + tGetRqst.toString());
		CloseableHttpResponse tResponse = mHttpclient.execute(tGetRqst);

		if (tResponse.getStatusLine().getStatusCode() != 200) {
			System.out.println("\n\n[Receive-Error] \n   " + " status-code: "  + tResponse.getStatusLine().getStatusCode() +
					" status: " + readResponse( tResponse ));
			return null;
		}

		// Read response data from the response message
		String tResponseData = readResponse( tResponse );
		// Build Json response message
		System.out.println("\n\n[Receive] \n   " + tResponseData);
		return tResponseData;
	}



	public String post(String pRqstString, String pDestination ) throws IOException
	{
		// Construct HTTP/HTTPS request

		HttpPost tPostRqst = new HttpPost( mBaseUrl + pDestination );
		HttpEntity tStringEntity = new StringEntity(pRqstString, ContentType.DEFAULT_TEXT);
		tPostRqst.setEntity(tStringEntity);

		// Send request to com.hoddmimes.resttest.server
		System.out.println("\n\n[post] destination: " + tPostRqst.toString() + "\n   " + pRqstString);
		CloseableHttpResponse tResponse = mHttpclient.execute(tPostRqst);

		if (tResponse.getStatusLine().getStatusCode() != 200) {
			System.out.println("\n\n[Receive-Error] \n   " + " status-code: "  + tResponse.getStatusLine().getStatusCode() +
					" status: " + readResponse( tResponse ));
			return null;
		}

		// Read response data from the response message
		String tResponseData = readResponse( tResponse );

		System.out.println("\n\n[Receive] \n   " + tResponseData );

		return tResponseData;
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

}

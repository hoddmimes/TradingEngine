package com.hoddmimes.resttest.client;

import org.springframework.web.socket.client.WebSocketClient;

import javax.net.ssl.*;
import javax.websocket.*;
import java.net.Socket;
import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@ClientEndpoint
public class WSClient extends Endpoint {
	private WebSocketClient mClient;
	private Session mSession;


	public WSClient(String endpointURI) {
		try {
			WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();

			ClientEndpointConfig.Configurator configurator = new ClientEndpointConfig.Configurator() {};
			ClientEndpointConfig clientEndpointConfig = ClientEndpointConfig.Builder.create().configurator(configurator).build();
			clientEndpointConfig.getUserProperties().put("org.apache.tomcat.websocket.SSL_CONTEXT", createSSLContext());


			Session session = webSocketContainer.connectToServer(this, clientEndpointConfig, new URI(endpointURI));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public void onOpen(Session session, EndpointConfig config) {
		mSession = session;
		System.out.println("opening websocket");
	}

	@OnClose
	public void onClose(Session userSession, CloseReason reason) {
		System.out.println("closing websocket");
	}


	@OnMessage
	public void onMessage(String message) {
		System.out.println("onMessage: " + message );
	}

	public void sendMessage(String message) {
		mSession.getAsyncRemote().sendText( message );
	}


	private SSLContext createSSLContext() {
		SSLContext tSSLContext = null;
		try {
			tSSLContext = SSLContext.getInstance("TLS");
			TrustManager[] tTrustMgrs = new TrustManager[]{new WSClient.BlindTrustManager()};
			tSSLContext.init(null, tTrustMgrs, new SecureRandom());


			SSLSessionContext tSSLSessionContext = tSSLContext.getClientSessionContext();
			tSSLSessionContext.setSessionTimeout(0);
			tSSLSessionContext.setSessionCacheSize(0);
			SSLParameters tSSLParams = tSSLContext.getDefaultSSLParameters();
			tSSLParams.setNeedClientAuth(false);

			tSSLContext.getDefaultSSLParameters().setEndpointIdentificationAlgorithm(null);
			tSSLContext.getDefaultSSLParameters().setWantClientAuth(false);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return tSSLContext;


	}


	class BlindTrustManager extends X509ExtendedTrustManager {
		@Override
		public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
			System.out.println("checkClientTrusted");
		}
		@Override
		public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
			System.out.println("checkServerTrusted");
		}
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			System.out.println("getAcceptedIssuers");
			return new X509Certificate[0];
		}

		@Override
		public void checkClientTrusted(X509Certificate[] pX509Certificates, String pS, Socket pSocket) throws CertificateException {
			System.out.println("checkClientTrusted");
		}

		@Override
		public void checkServerTrusted(X509Certificate[] pX509Certificates, String pS, Socket pSocket) throws CertificateException {
			System.out.println("checkServerTrusted");
		}

		@Override
		public void checkClientTrusted(X509Certificate[] pX509Certificates, String pS, SSLEngine pSSLEngine) throws CertificateException {
			System.out.println("checkServerTrusted");
		}

		@Override
		public void checkServerTrusted(X509Certificate[] pX509Certificates, String pS, SSLEngine pSSLEngine) throws CertificateException {

		}
	}

}

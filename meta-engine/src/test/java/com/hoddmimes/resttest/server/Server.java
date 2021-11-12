package com.hoddmimes.resttest.server;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;

import java.util.Properties;

public class Server {
	public static void main(String[] args) {
		Server s = new Server();
		s.test( args );
	}


	private void test(String args[]) {
		try {

			//System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");


			SpringApplication tApplication = new SpringApplication(Connector.class);
			Properties tAppProperties = new Properties();

			tAppProperties.setProperty("server.port", String.valueOf("8883"));

			tAppProperties.setProperty("server.ssl.key-store-type","PKCS12");
			tAppProperties.setProperty("server.ssl.key-store", "configuration/teengine.jks");
			tAppProperties.setProperty("server.ssl.key-alias","teengine");
			tAppProperties.setProperty("server.ssl.key-store-password","lostiblasten");
			tAppProperties.setProperty("server.ssl.enabled","true");


			tApplication.setDefaultProperties(tAppProperties);
			tApplication.setBannerMode(Banner.Mode.OFF);
			tApplication.run(args);
			//SpringApplication.run(DistributorWsSocketGateway.class, args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

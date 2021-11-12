package com.hoddmimes.resttest.client;

import java.net.URI;

public class Client
{
		public static void main(String[] args) {
			Client cy = new Client();
			cy.execute();
		}

		private void execute() {
			int tCount = 1;

			try {
				// open websocket
				final HttpClient tRestClient = new HttpClient("https://localhost:8883/test");
				final WSClient tWsEndPoint = new WSClient("wss://localhost:8883/distributor");


				tWsEndPoint.sendMessage("Client start message :-)");

				while( true ) {
					String tRspTxt = tRestClient.post("Post request count: " + (tCount++), "/testtx" );
					System.out.println("Rest response: " + tRspTxt );
					try {Thread.sleep(1000L);}
					catch( InterruptedException e) {}
				}

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}



}

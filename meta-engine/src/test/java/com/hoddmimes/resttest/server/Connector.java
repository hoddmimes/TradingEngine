package com.hoddmimes.resttest.server;


import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;

@SpringBootApplication
@RestController

@Configuration
@EnableWebSocket
@RequestMapping("/test")
public class Connector extends Thread implements WebSocketConfigurer
{

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		String tPath = "/distributor";
		WebSocketHandlerRegistration tWsHandler = registry.addHandler(new WebSocketHandler(), tPath );
		System.out.println("--registerWebSocketHandlers: " + tPath);
		tWsHandler.addInterceptors( new WebSocketHandshakeInterceptor());
		this.start();
	}

	@PostMapping( path = "/testtx" )
	ResponseEntity<?> testtx(HttpSession pSession, @RequestBody String pRqstMsg )
	{
		System.out.println("[testtx] " + pRqstMsg );
		return new ResponseEntity<>( "TestTx response", HttpStatus.OK );
	}



	public void run()
	{
		int tCounter = 1;
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
		WebSocketHandler tSocketHandle = WebSocketHandler.getInstance();
		try { Thread.sleep(2000L );}
		catch( InterruptedException e) {}

		while( true ) {
			tSocketHandle.sendBdx("BDX counter: " + (tCounter++) + " time: " + sdf.format( System.currentTimeMillis()));
			try { Thread.sleep(5000L );}
			catch( InterruptedException e) {}
		}
	}

}

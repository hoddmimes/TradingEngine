package com.hoddmimes.te.common.transport.tcpip;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;


public class TcpServer extends Thread 
{
	ServerSocketChannel	        mServerChannel;
	Map<TcpThread, TcpThread>   mClients;
	TcpServerCallbackIf         mCallback;
	boolean                     mUseSocketChannel;
	
	
	public TcpServer( TcpServerCallbackIf pCallback ) {
		mClients = new HashMap<TcpThread, TcpThread>();
		mCallback = pCallback;
	}

	public void declareServer( int pAcceptPort ) throws IOException {
		declareServer("0.0.0.0", pAcceptPort);
	}

	public void declareServer(String pInterface, int pAcceptPort ) throws IOException {
		String tInterfaceAddressString = (pInterface == null) ? "0.0.0.0" : pInterface;

		mServerChannel = ServerSocketChannel.open();
		mServerChannel.socket().bind(new InetSocketAddress( tInterfaceAddressString, pAcceptPort));
		this.start();
	}

	public void run() {
		SocketChannel tSocketChannel = null;
		
		while( true ) {
			try {

			  tSocketChannel = mServerChannel.accept();
				tSocketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
				tSocketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

			  try {
			      TcpThread tThread = new TcpThread(tSocketChannel, null);
			      mCallback.tcpInboundConnection(tThread);
			  } catch( Exception e ) {
				  e.printStackTrace();
			  }
			} catch( IOException e ) {
				e.printStackTrace();
			}
		}
	}
}
	
	

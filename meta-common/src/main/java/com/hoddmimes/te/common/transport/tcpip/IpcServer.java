package com.hoddmimes.te.common.transport.tcpip;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;


public class IpcServer extends Thread {
	private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss.SSS");
	private Logger cLog = LogManager.getLogger( IpcServer.class);

	ServerSocketChannel         mServerSocketChannel;
	TcpServerCallbackIf         mCallback;

	public IpcServer( TcpServerCallbackIf pCallback ) {
		mCallback = pCallback;
	}

	public void run() {
		while (true) {
			try {
				SocketChannel tSocketChannel = mServerSocketChannel.accept();
				tSocketChannel.configureBlocking(true);
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


	public  void declareServer(String  pAddressName) throws IOException {
		//mServerSocketChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
		//mServerSocketChannel.configureBlocking(true);
		//UnixDomainSocketAddress tSocketAddress = UnixDomainSocketAddress.of("./" + pAddressName);
		//mServerSocketChannel.bind(tSocketAddress);
		cLog.info("Declared IPC com.hoddmimes.resttest.server, name-space: " +pAddressName);
		this.start();
	}
}

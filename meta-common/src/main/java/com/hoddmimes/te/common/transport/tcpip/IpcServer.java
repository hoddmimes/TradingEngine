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

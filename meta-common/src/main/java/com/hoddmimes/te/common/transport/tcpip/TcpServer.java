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
	int                         mServerPort;
	
	
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
		mServerPort = mServerChannel.socket().getLocalPort();
		this.start();
	}

	public int getLocalPort() {
		return mServerPort;
	}


	public void run() {
		SocketChannel tSocketChannel = null;
		
		while( true ) {
			try {

			  tSocketChannel = mServerChannel.accept();
				tSocketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
				tSocketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

			  try {
			      TcpThread tThread = new TcpThread(tSocketChannel, TcpThread.ThreadType.ThreadServer);
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
	
	

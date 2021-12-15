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
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;


public class TcpClient
{
	/**
     * Method issuing a connection. The com.hoddmimes.resttest.server and port used will be the ones
     * specified in the constructor
     * @throws IOException throws a I/O exception in case of failure
     */
    public static TcpThread connect(String pHost, int pPort, TcpThreadCallbackIf pCallback) throws IOException
	{
        synchronized (TcpClient.class)
		{
			SocketChannel tClientChannel = SocketChannel.open();
			SocketAddress socketAddr = new InetSocketAddress(pHost, pPort);
			tClientChannel.connect(socketAddr);
			tClientChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
			tClientChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            TcpThread tTcpThread = new TcpThread( tClientChannel, pCallback);
			tTcpThread.start();
            return tTcpThread;
        }
    }

	public static TcpThread connect(String pHost, int pPort) throws IOException {
		return connect(pHost, pPort, null);
	}
}
    

 
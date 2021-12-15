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
//import java.net.UnixDomainSocketAddress;


public class IpcClient
{
    TcpThreadCallbackIf mCallbackIf;		// Callback interface
    TcpThread mTcpThread;


        
    /**
     * Method issuing a connection. The com.hoddmimes.resttest.server and port used will be the ones
     * specified in the constructor
     * @throws IOException throws a I/O exception in case of failure
     */
    public static TcpThread connect(String pAddressName, TcpThreadCallbackIf pCallback) throws IOException
	{
        synchronized (IpcClient.class)
		{
			//SocketChannel tSocketChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
			//tSocketChannel.configureBlocking(true);
			//UnixDomainSocketAddress tSocketAddress = UnixDomainSocketAddress.of("./" + pAddressName );
			//tSocketChannel.connect(tSocketAddress);
            //TcpThread tIpcThread = new TcpThread( tSocketChannel, pCallback);
			//tIpcThread.start();
            //return tIpcThread;
			return null;
        }
    }
}
    

 
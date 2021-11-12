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
    

 
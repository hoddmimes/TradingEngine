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
}
    

 
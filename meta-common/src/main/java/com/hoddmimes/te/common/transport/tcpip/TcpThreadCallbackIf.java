package com.hoddmimes.te.common.transport.tcpip;
import java.io.IOException;


public interface TcpThreadCallbackIf 
{
	public void tcpMessageRead(TcpThread pThread, byte[] pBuffer );
	public void tcpErrorEvent(TcpThread pThread, IOException pException );
}

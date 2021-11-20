package com.hoddmimes.te.common.interfaces;

import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.TeException;
import com.hoddmimes.te.messages.generated.LogonResponse;

import java.io.IOException;

public interface ConnectorInterface
{
	public interface ConnectorCallbackInterface
	{
		public SessionCntxInterface connectorDisconnectSession( String pSessionId );
		public SessionCntxInterface terminateSession( String pSessionId );
		public SessionCntxInterface getSessionCntx( String pSessionId );
		public MessageInterface connectorMessage( String pSessionId, String pJsonRqstMessage );
		public LogonResponse logon(String pSessionId, String pJsonRqstMessage ) throws TeException;
		public MessageInterface validateMessage( String pJsonMessage ) throws Exception;
	}

	public void declareAndStart() throws IOException;

}

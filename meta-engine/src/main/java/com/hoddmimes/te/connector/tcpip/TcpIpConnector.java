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

package com.hoddmimes.te.connector.tcpip;

import com.google.gson.JsonObject;

import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.transport.tcpip.TcpServer;
import com.hoddmimes.te.common.transport.tcpip.TcpServerCallbackIf;
import com.hoddmimes.te.common.transport.tcpip.TcpThread;
import com.hoddmimes.te.common.transport.tcpip.TcpThreadCallbackIf;
import com.hoddmimes.te.connector.ConnectorBase;
import com.hoddmimes.te.messages.generated.MessageFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class TcpIpConnector extends ConnectorBase implements TcpServerCallbackIf, TcpThreadCallbackIf
{
	private Logger mLog = LogManager.getLogger( TcpIpConnector.class );

	private int             mAcceptPort;
	private String          mInterfaceAddress;
	private MessageFactory  mMsgFactory;


	private TcpServer mTcpServer;

	public TcpIpConnector(JsonObject pDeclareData, ConnectorCallbackInterface pCallback) {
		super(pDeclareData, pCallback);
		parseDeclareData( pDeclareData );
		mMsgFactory = new MessageFactory();
	}

	private void parseDeclareData( JsonObject pConfiguration ) {
		mAcceptPort = pConfiguration.get("port").getAsInt();
		mInterfaceAddress = (pConfiguration.has("interface")) ? pConfiguration.get("interface").getAsString() : "0.0.0.0";
	}
	@Override
	public void declareAndStart() throws IOException {
		mTcpServer = new TcpServer( this );
		try {
			mTcpServer.declareServer(mInterfaceAddress, mAcceptPort);
			mLog.info("successfully declared com.hoddmimes.resttest.server on interface \"" + mInterfaceAddress + "\" port " + mAcceptPort);
		}
		catch(IOException e) {
			mLog.fatal("failed to declare connector on interface \"" + mInterfaceAddress + "\" port " + mAcceptPort, e);
			System.exit(-1);
		}
	}

	@Override
	public void tcpInboundConnection(TcpThread pThread) {
		mLog.info("inbound connection " + pThread.toString());
		pThread.setCallback( this );
		pThread.start();
	}

	@Override
	public void tcpMessageRead(TcpThread pThread, byte[] pBuffer) {
		String tMsgString = null;

		try {
			tMsgString = new String( pBuffer, "UTF-8");
			MessageInterface tMessage = mMsgFactory.getMessageInstance( tMsgString );
			if (tMessage == null) {
				mLog.error("failed to parse inbound message \"" + tMsgString + "\" for com.hoddmimes.resttest.client " + pThread.toString());
				return;
			}
			mCallback.connectorMessage(pThread.getSessionId(), tMessage.toJson().toString());

		}
		catch( UnsupportedEncodingException e) {
			mLog.error("Failed to encode message", e);
		}
	}

	@Override
	public void tcpErrorEvent(TcpThread pThread, IOException pException) {
		mLog.warn("error event, reason:  " + pException.getMessage() + "\n   " + pThread.toString());
		mCallback.connectorDisconnectSession( pThread.getSessionId() );
	}


	public void sendResponse(Object pSessionObject, MessageInterface pMessage) {
		TcpThread pThread = (TcpThread) pSessionObject;
		try {pThread.send( pMessage.toJson().toString().getBytes(StandardCharsets.UTF_8) );}
		catch( IOException e) {
			mLog.warn("session disconnected " + pThread.toString());
			mCallback.connectorDisconnectSession( pThread.getSessionId() );
		}
	}
}

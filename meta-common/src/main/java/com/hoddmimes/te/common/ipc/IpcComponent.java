/*
 * Copyright (c)  Hoddmimes Solution AB 2022.
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

package com.hoddmimes.te.common.ipc;

import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.interfaces.TeService;
import com.hoddmimes.te.common.transport.tcpip.TcpServer;
import com.hoddmimes.te.common.transport.tcpip.TcpServerCallbackIf;
import com.hoddmimes.te.common.transport.tcpip.TcpThread;
import com.hoddmimes.te.common.transport.tcpip.TcpThreadCallbackIf;
import com.hoddmimes.te.messages.generated.IpcComponentConfiguration;
import com.hoddmimes.te.messages.generated.MessageFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

public class IpcComponent implements IpcComponentInterface, TcpServerCallbackIf, TcpThreadCallbackIf {
	private TeService    mService;
	private long        mCreateTime;
	private String      mHost;
	private int         mPort;
	private TcpServer   mServer;
	private MessageFactory mMsgFactory;
	private Logger      mLog;
	private IpcRequestCallbackInterface mCallbacktHandler;


	public IpcComponent(TeService pService, String pHost, int pPort, IpcRequestCallbackInterface pDefaultHandler ) {
		mService = pService;
		mHost = pHost;
		mPort = pPort;
		mCreateTime = System.currentTimeMillis();
		mLog = LogManager.getLogger( IpcComponent.class );
		mMsgFactory = new MessageFactory();
		mCallbacktHandler = pDefaultHandler;
		declareServer();
	}

	private void declareServer() {
		mServer = new TcpServer( this );
		try {
			mServer.declareServer( mPort );
			mPort = mServer.getLocalPort();
			mLog.info("declare mgmt server for component \"" + mService + "\" on port " + mPort );
		}
		catch ( IOException e) {
			mLog.fatal("failed to declare mgmt server for component \"" + mService + "\" on port " + mPort, e );
			new RuntimeException( e );
		}
	}

	@Override
	public void tcpInboundConnection(TcpThread pThread) {
		mLog.info("inbound connection from: " + pThread.getRemoteAddress());
		pThread.setCallback( this );
		pThread.start();
	}



	@Override
	public void tcpErrorEvent(TcpThread pThread, IOException pException) {
		mLog.warn("client disconnect client: " + pThread.getRemoteAddress());
		pThread.close();
	}



	private void dispatchRequest( TcpThread pThread, MessageInterface pRequest ) {
			MessageInterface tResponse = mCallbacktHandler.ipcRequest( pRequest );
			send( pThread, tResponse );
	}

	@Override
	public void tcpMessageRead(TcpThread pThread, byte[] pBuffer)
	{
		String jMsgRqst = new String( pBuffer);
		MessageInterface tRqstMsg = mMsgFactory.getMessageInstance( jMsgRqst );
		if (tRqstMsg == null) {
			mLog.error("invalid request message : " + jMsgRqst );
			pThread.close();
		}
		dispatchRequest( pThread,  tRqstMsg );
	}

	private void send( TcpThread pThread, MessageInterface pMsg ) {
		try {
			pThread.send( pMsg.toJson().toString().getBytes(StandardCharsets.UTF_8));
		}
		catch( IOException e) {
			mLog.error("failed to send message, thread: " + pThread.getRemoteAddress(), e);
			pThread.close();
		}
	}


	public IpcComponentConfiguration toIpcComponentConfigMsg() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		IpcComponentConfiguration tComp = new IpcComponentConfiguration();
		tComp.setCretime( mCreateTime );
		tComp.setLastTimeSeen( System.currentTimeMillis());
		tComp.setName( mService.name() );
		tComp.setHost( mHost );
		tComp.setPort( mPort );
		return tComp;
	}



}

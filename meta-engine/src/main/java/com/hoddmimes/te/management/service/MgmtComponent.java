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

package com.hoddmimes.te.management.service;

import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.transport.tcpip.TcpServer;
import com.hoddmimes.te.common.transport.tcpip.TcpServerCallbackIf;
import com.hoddmimes.te.common.transport.tcpip.TcpThread;
import com.hoddmimes.te.common.transport.tcpip.TcpThreadCallbackIf;
import com.hoddmimes.te.messages.MgmtMessageRequest;
import com.hoddmimes.te.messages.MgmtMessageResponse;
import com.hoddmimes.te.messages.generated.MessageFactory;
import com.hoddmimes.te.messages.generated.MgmtStatusResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.HashMap;

public class MgmtComponent implements MgmtComponentInterface, TcpServerCallbackIf, TcpThreadCallbackIf {
	private String      mName;
	private long        mCreateTime;
	private int         mPort;
	private TcpServer   mServer;
	private MessageFactory mMsgFactory;
	private Logger      mLog;
	private MgmtCmdCallbackInterface                        mDefaultHandler;
	private HashMap<String, MgmtCmdCallbackInterface>       mHandlers;

	public MgmtComponent( String pName, int pPort, MgmtCmdCallbackInterface pDefaultHandler ) {
		mName = pName;
		mPort = pPort;
		mCreateTime = System.currentTimeMillis();
		mLog = LogManager.getLogger( MgmtComponent.class );
		mMsgFactory = new MessageFactory();
		mDefaultHandler = pDefaultHandler;
		mHandlers = new HashMap<>();
		declareServer();
	}

	private void declareServer() {
		mServer = new TcpServer( this );
		try {
			mServer.declareServer( mPort );
			mPort = mServer.getLocalPort();
			mLog.info("declare mgmt server for component \"" + mName + "\" on port " + mPort );
		}
		catch ( IOException e) {
			mLog.fatal("failed to declare mgmt server for component \"" + mName + "\" on port " + mPort, e );
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

	@Override
	public void registerHandler(String pCommad, MgmtCmdCallbackInterface pCallbackHandler) {
		mHandlers.put( pCommad, pCallbackHandler);
	}

	private void dispatchRequest( TcpThread pThread, MgmtMessageRequest pRequest ) {
		MgmtCmdCallbackInterface tCallback = mHandlers.get( pRequest.getMessageName() );
		if (tCallback != null) {
			MgmtMessageResponse tResponse = tCallback.mgmtRequest( pRequest );
			send( pThread, tResponse );
			return;
		}
		if (mDefaultHandler != null) {
			MgmtMessageResponse tResponse = mDefaultHandler.mgmtRequest( pRequest );
			send( pThread, tResponse );
			return;
		}
		MgmtStatusResponse tRsp = new MgmtStatusResponse().setRef(pRequest.getRef().get()).setMessage("No command destination found");
		send( pThread, tRsp );
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
		if (!(tRqstMsg instanceof MgmtMessageRequest)) {
			mLog.error("Not a MgmtMessageRequest message : " + jMsgRqst );
			String tRef = AuxJson.getMessageRef(jMsgRqst, null);
			MgmtStatusResponse tRsp = new MgmtStatusResponse().setRef(tRef).setMessage("Not a MgmtMessageRequest message");
			send( pThread, tRsp );
		}

		dispatchRequest( pThread,  (MgmtMessageRequest) tRqstMsg );
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

	public com.hoddmimes.te.messages.generated.MgmtComponent toMgmtComponentMsg() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		com.hoddmimes.te.messages.generated.MgmtComponent tComp = new com.hoddmimes.te.messages.generated.MgmtComponent();
		tComp.setCretime( sdf.format(mCreateTime));
		tComp.setName( mName );
		tComp.setPort( mPort );
		return tComp;
	}



}

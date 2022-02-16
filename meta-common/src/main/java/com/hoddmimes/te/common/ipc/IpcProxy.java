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
import com.hoddmimes.te.common.transport.IpmgPublisher;
import com.hoddmimes.te.common.transport.IpmgSubscriber;
import com.hoddmimes.te.common.transport.IpmgSubscriberListenerInterface;
import com.hoddmimes.te.common.transport.tcpip.TcpClient;
import com.hoddmimes.te.common.transport.tcpip.TcpThread;
import com.hoddmimes.te.messages.MgmtMessageRequest;
import com.hoddmimes.te.messages.MgmtMessageResponse;
import com.hoddmimes.te.messages.generated.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import javax.swing.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public class IpcProxy {
	private static MessageFactory cMessageFactory = new MessageFactory();
	private static Logger cLog = LogManager.getLogger( IpcProxy.class );
	private TcpThread mTcpThread;

	private String mServicename;

	public IpcProxy( TcpThread pTcpThread, String pServicename ) {
		mTcpThread = pTcpThread;
		mServicename = pServicename;
	}

	public MessageInterface transceive( MessageInterface pRequest) {

		MessageInterface tResponse = null;
		try {
			byte[] tRcvBuf = mTcpThread.transceive(pRequest.toJson().toString().getBytes(StandardCharsets.UTF_8));
			tResponse = (tRcvBuf != null) ? cMessageFactory.getMessageInstance(new String(tRcvBuf)) : null;
			if (tResponse == null) {
				cLog.warn("invalid response message from service: " + mServicename + " msg: " + ((tRcvBuf == null) ? "<null>" : new String(tRcvBuf)));
				return (MessageInterface) tResponse;
			}
			return tResponse;
		}
		catch( IOException e) {
			cLog.warn("disconnected service \"" + mServicename + "\" reason: " + e.getMessage());
		}
		return null;
	}
}

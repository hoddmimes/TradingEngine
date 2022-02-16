
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

import com.hoddmimes.te.common.transport.tcpip.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;


public class TcpTest implements TcpServerCallbackIf, TcpThreadCallbackIf {
	private enum ClientType {ClientThread,ServerThread};
	private static final long MSGS_TO_SEND = 1000;

	private static final int PORT = 7373;
	private SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss.SSS");
	private boolean mSyncMode = false;

	public static void main(String[] args) {
		TcpTest t = new TcpTest();
		try { t.test(); }
		catch( Exception e) {
			e.printStackTrace();
		}
	}

	private void log( String pMsg ) {
		System.out.println(SDF.format(System.currentTimeMillis()) + " " + pMsg);
		System.out.flush();
	}




	private void test() throws Exception {
		TcpServer tServer = new TcpServer(this);
		tServer.declareServer(PORT);
		if (mSyncMode) {
			syncExecute();
		} else {
			asyncExecute();
		}
	}

	private void syncExecute() throws IOException {
		TcpThread tThread = TcpClient.connect("localhost", PORT);
		log("[connected to host]");
		tThread.setAppCntx(new AppCntx(ClientType.ClientThread));
		for (int i = 0; i < MSGS_TO_SEND; i++) {
			byte[] tBuffer = tThread.transceive("TestMessage 123456789".getBytes(StandardCharsets.UTF_8));
			if ((i % 50) == 0) {
				log("[sync counter] " + i);
			}
		}
		log("[ALL DONE] ");
	}

	private void asyncExecute() throws IOException
	{
		TcpThread tThread = TcpClient.connect("localhost", PORT, this );
		tThread.setAppCntx( new AppCntx( ClientType.ClientThread));
		tThread.send("TestMessage 123456789".getBytes(StandardCharsets.UTF_8));
	}




	@Override
	public void tcpInboundConnection(TcpThread pThread) {
		pThread.setCallback( this );
		log("[inbound connection] thread: " + pThread.toString());
		pThread.setAppCntx( new AppCntx( ClientType.ServerThread ));
		pThread.start();
	}

	@Override
	public void tcpMessageRead(TcpThread pThread, byte[] pBuffer) {
		AppCntx tAppCntx = (AppCntx) pThread.getAppCntx();
		if (tAppCntx.mType == ClientType.ClientThread) {
			tAppCntx.mCounter++;
			if (tAppCntx.mCounter >= MSGS_TO_SEND) {
				log("All done!!!");
				pThread.close();
				return;
			} else {
				if ((tAppCntx.mCounter % 50) == 0) {
					log("[read] message: " + tAppCntx.mCounter);
				}
			}
		}

		try {pThread.send( pBuffer );}
		catch( IOException e) {
		   log("[send failure] thread: " + pThread.toString() + " reason: " + e.toString());
		   pThread.close();
		}
	}

	@Override
	public void tcpErrorEvent(TcpThread pThread, IOException pException) {
		log("[disconnected] thread:  " + pThread.toString());
		pThread.close();
	}

	class AppCntx {
		ClientType mType;
		long   mCounter;

		AppCntx( ClientType pType) {
			mType = pType;
			mCounter = 0l;
		}
	}
}









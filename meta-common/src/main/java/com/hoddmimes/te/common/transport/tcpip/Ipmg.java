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




import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

public class Ipmg
{
	Logger cLog = LogManager.getLogger( Ipmg.class );
	private static boolean TRACE_XTA = true;
	private static long XTA_MINDISPLAY_TIME = Long.MAX_VALUE;


	InetSocketAddress mDestinationSocketAddress;
	MulticastSocket mSocket;
	InetAddress mInetAddress;
	int mPort;
	long mTotalIOs, mDelayedIOs;
	NetworkInterface mNetworkInterface;

	public Ipmg(String  pMcaAddress, String pNetworkInterface, int pPort, int pBufferSize, int pTTL) throws Exception {

		mPort = pPort;
		mTotalIOs = mDelayedIOs = 0;
		mInetAddress = InetAddress.getByName( pMcaAddress );

		if (pNetworkInterface != null) {
			try {
				mNetworkInterface = NetworkInterface.getByName(pNetworkInterface);
				if (mNetworkInterface == null) {
					throw new Exception( "Could not locate network interface \""+ pNetworkInterface + "\"");
				}
			} catch (SocketException e1) {
				throw new Exception("Could not locate network interface \""+ pNetworkInterface + "\" SocketException: " + e1.getMessage());
			}
		} else {
			mNetworkInterface = null;
		}

		try {
			mSocket = new MulticastSocket(null);
			mSocket.setReuseAddress(true);
			mSocket.bind(new InetSocketAddress(mPort));
			if (!mSocket.getReuseAddress()) {
				throw new Exception("Hmmm, reuse of address not supported, socket: " + this.toString());
			}
		} catch (IOException e2) {
			throw new Exception("Could not create MulticastSocket IOException: " + e2.getMessage());
		}
		try {
			mSocket.setReceiveBufferSize(pBufferSize);
			mSocket.setSendBufferSize(pBufferSize);
		} catch (SocketException e3) {
			throw new Exception("Could not set buffer size  SocketException: " + e3.getMessage());
		}

		try {
			if (mNetworkInterface != null) {
				mSocket.setNetworkInterface(mNetworkInterface);
			}
		} catch (SocketException e4) {
			throw new Exception( "Could not set the network interface \"" + pNetworkInterface + "\"  SocketException: " + e4.getMessage());
		}

		try {
			mSocket.setTimeToLive(pTTL);
		} catch (IOException e5) {
			throw new Exception( "Could not set the TTL  IOException: " + e5.getMessage());
		}
		try {
			mSocket.setLoopbackMode( false );
			//mSocket.setOption( StandardSocketOptions.IP_MULTICAST_LOOP, true);
		} catch (Exception e6) {
			throw new Exception("Could not set loopback mode,  SocketException: " + e6.getMessage());
		}

		mDestinationSocketAddress = new InetSocketAddress(mInetAddress, mPort);
		try {
			if (mNetworkInterface != null) {
				mSocket.joinGroup(mDestinationSocketAddress, mNetworkInterface);
			} else {
				mSocket.joinGroup(mInetAddress);
			}
		} catch (IOException e7) {
			throw new Exception( "Could not join multicast group \"" + pMcaAddress + "\" IOException: " + e7.getMessage());
		}

	}

	public void close() {
		try {
			mSocket.leaveGroup(mInetAddress);
			mSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void send(ByteBuffer pBuffer) throws Exception {
		long tSendStartTime = (TRACE_XTA) ? System.nanoTime() : 0;

		DatagramPacket tPacket = new DatagramPacket(pBuffer.array(), pBuffer.position(), mDestinationSocketAddress);
		 mSocket.send(tPacket);
		if (TRACE_XTA) {
			long tXtaTime = (System.nanoTime() - tSendStartTime) / 1000L;
			mTotalIOs++;
			if (tXtaTime >= XTA_MINDISPLAY_TIME) {
				mDelayedIOs++;
				System.out.println("IPMG XTA time: " +(tXtaTime) + " usec  Delayed IOs: " + mDelayedIOs + " total IOs: " + mTotalIOs);
			}
		}
		if (cLog.isTraceEnabled()) {
			cLog.trace("IPMG XTA Size: " + tPacket.getLength()  + " address: " + mInetAddress.toString());
		}

		if (pBuffer.position() != tPacket.getLength()) {
			throw new IOException("Failed to send, send data("
					+ tPacket.getLength() + " != buffer length("
					+ pBuffer.position() + ") MCA: "
					+ mDestinationSocketAddress.toString());
		}

		//System.out.println("IPMG send: " + tPacket.getLength() + " xta time: " + tSendTime);
		tPacket = null;
	}

	public SocketAddress receive(ByteBuffer pBuffer) throws IOException {
		pBuffer.clear();
		DatagramPacket tPacket = new DatagramPacket(pBuffer.array(), pBuffer.capacity());
		mSocket.receive(tPacket);

		pBuffer.position(tPacket.getLength());

		if (cLog.isTraceEnabled()) {
			cLog.trace("IPMG RCV Size: " + tPacket.getLength() + " address: " + mInetAddress.toString() + " port: " + mSocket.getPort());
		}


		return tPacket.getSocketAddress();
	}

	public long getMcaConnectionId() {
		byte[] tBuffer = mInetAddress.getAddress();
		long tHigh = ((tBuffer[0] & 0xff) << 24) + ((tBuffer[1] & 0xff) << 16)
				+ ((tBuffer[2] & 0xff) << 8) + (tBuffer[3] & 0xff);
		long tLow = mPort;

		return ((tHigh << 24) + tLow);
	}

	@Override
	public String toString() {
		return mInetAddress.getHostAddress() + ":" + mPort;
	}

}

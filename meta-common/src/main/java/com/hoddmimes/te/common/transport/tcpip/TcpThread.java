package com.hoddmimes.te.common.transport.tcpip;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;


public class TcpThread extends Thread
{
	private static int				MAGIC_SIGN = 0x504f4242;
    private static int              mClientIndex = 0;
	private static int              BUFFER_SIZE = 4096;
    private static final SimpleDateFormat cSDF = new SimpleDateFormat("HH:mm:ss.SSS");
    
	private SocketChannel           mChannel;
	private TcpThreadCallbackIf		mCallbackIf;
	private volatile boolean		mClosed = true;
	private int                     mIndex;
	private String                  mConnectTime;
	private Object 					mAppCntx;



	
	public TcpThread(SocketChannel pChannel, TcpThreadCallbackIf pCallbackIf) {
		mChannel = pChannel;
		mClosed = false;
		mIndex = ++mClientIndex;
		mConnectTime = cSDF.format(System.currentTimeMillis());
		mAppCntx = null;


		mCallbackIf = pCallbackIf;

		try {
			mChannel.configureBlocking(true);

			//start();
		} catch( IOException e) {
			e.printStackTrace();
		}
	}


	public void setAppCntx( Object pCntx ) {
		mAppCntx = pCntx;
	}

	public Object getAppCntx() {
		return mAppCntx;
	}

	public String getRemoteAddress() {
		try {
			return mChannel.getRemoteAddress().toString();
		}
		catch( Exception e) {
			return "<unknown>";
		}

	}

	public String getSessionId() {
		return Integer.toHexString(this.hashCode());
	}
	
	public String toString() {
	    return "[TcpThread: " + mIndex + " addr: " + getRemoteAddress() + " connTime: " + mConnectTime +"]";
	}
	
	public void setCallback( TcpThreadCallbackIf pCallbackIf ) {
	    mCallbackIf = pCallbackIf;
	}
	
	public void close() {
	  synchronized( this ) {
		  mClosed = true;
		  this.notify();
		  try {mChannel.close();}
		  catch( IOException e ) {}
	  }
	}
	
	public void send( byte[] pBuffer ) throws IOException
	{
		ByteBuffer bb = ByteBuffer.allocate( pBuffer.length + 8);
		bb.putInt( MAGIC_SIGN );
		bb.putInt(pBuffer.length);
		bb.put( pBuffer );
		bb.flip();
		while (bb.hasRemaining()) {
			mChannel.write(bb);
		}
	}
	
	public void run() {
		ByteBuffer tHdr = ByteBuffer.allocate(8);
		ByteBuffer tData = ByteBuffer.allocate( BUFFER_SIZE );
		ByteBuffer tReadBuffer = null;
		byte[] tBuffer;
		int		tSize;

		
		while ( !mClosed ) 
		{
			try {
				/*
				 * Read Header
				 */
				tHdr.clear();
				while( tHdr.position() < 8 ) {
					mChannel.read( tHdr );
				}

				tHdr.flip();

				if (tHdr.getInt() != MAGIC_SIGN) {
					throw new IOException("tcp/ip read, invalid magic sign");
				}
				tSize = tHdr.getInt();
				tReadBuffer = (tSize > BUFFER_SIZE) ? ByteBuffer.allocate(tSize) : tData;

				/*
				 * Read user data
				 */
				tReadBuffer.clear();
				tReadBuffer.limit( tSize );
				while( tReadBuffer.position() < tSize ) {
					mChannel.read(tReadBuffer);
				}
				tBuffer = new byte[ tSize ];
				tReadBuffer.flip();
				tReadBuffer.get( tBuffer );
			} catch( IOException e) {
				if (!mClosed) {
					mCallbackIf.tcpErrorEvent(this, e);
					return;
				} else {
					return;
				}
			}

			/**
			 * Deliver the read data
			 */
			try {
				mCallbackIf.tcpMessageRead(this, tBuffer); }
			catch( Throwable e) 
			{
				e.printStackTrace();
			}
		}
	}
}

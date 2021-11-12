package com.hoddmimes.te.common;

import java.util.concurrent.LinkedBlockingQueue;

public class ThreadPoolBlockingQueue<T> extends LinkedBlockingQueue<T>
{
	public ThreadPoolBlockingQueue() {
		super();
	}

	public ThreadPoolBlockingQueue( int pCapacity ) {
		super(pCapacity);
	}

	@Override
	public boolean offer( T pObject ) {
		try {
			super.put( pObject );
			return true;
		}
		catch( InterruptedException e) {}
		return false;
	}
}

package com.hoddmimes.te.common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class SingleExecutor extends Thread
{
	private LinkedBlockingQueue<Runnable> mQueue;

	public SingleExecutor( int pQueueCapacity, String pThreadName  ) {
		mQueue = new LinkedBlockingQueue<>( pQueueCapacity );
		this.setName( pThreadName );
		this.start();
	}

	public void queue( Runnable pTask) {
		synchronized ( mQueue ) {
			try {mQueue.put( pTask );}
			catch( InterruptedException e) {}
			mQueue.notifyAll();
		}
	}


	@Override
	public void run() {
		List<Runnable> tTaskList = new ArrayList<>( 30 );
		Runnable tTask = null;

		while( true ) {
			try {
				tTask = null;
				tTask = mQueue.take();
			}
			catch( InterruptedException e) {}

			if (tTask != null) {
				try { tTask.run(); }
				catch( Throwable e) {}

				tTaskList.clear();
				mQueue.drainTo( tTaskList, 30);
				for( Runnable t : tTaskList ) {
					try { t.run(); }
					catch( Throwable e) {}
				}
			}
		}
	}
}

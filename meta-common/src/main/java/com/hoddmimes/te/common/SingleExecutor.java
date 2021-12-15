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

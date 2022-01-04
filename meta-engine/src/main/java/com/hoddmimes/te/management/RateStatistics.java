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

package com.hoddmimes.te.management;


import java.util.ArrayList;
import java.util.List;

public class RateStatistics extends Thread
{
	private static RateStatistics cInstance = null;
	private List<RateItem> mRateItems;

	public static RateStatistics getInstance() {
		if (cInstance == null) {
			cInstance = new RateStatistics();
		}
		return cInstance;
	}

	private RateStatistics() {
		mRateItems = new ArrayList();
		this.start();
	}

	public RateItem addRateItem( String pAttribute ) {
		RateItem tItem = new RateItem( pAttribute );
		synchronized( mRateItems ) {
			mRateItems.add( tItem );
		}
		return tItem;
	}

	public void run() {
		long ms = 1000;
		int ns = 0;

		while( true ) {
			try { Thread.sleep(ms, ns); }
			catch( InterruptedException ie) {}
			long tStartTime = System.nanoTime();
			for( RateItem ri : mRateItems) {
				ri.valuate();
			}
			long tExecTimeNs = System.nanoTime() - tStartTime;
			ms = tExecTimeNs / 1000000L;
			ns = (int) (tExecTimeNs - (ms * 1000000L));
		}
	}
}


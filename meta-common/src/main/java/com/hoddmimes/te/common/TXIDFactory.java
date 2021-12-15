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

public class TXIDFactory
{
	private static final long cResetCount = 0xffffff;
	private static long mSeqNo = 0L;
	private static long mSeconds = (System.currentTimeMillis() / 1000L) << 24L;


	public static synchronized long getId()
	{
		mSeqNo++;
		if (mSeqNo >= cResetCount) {
			mSeconds = (System.currentTimeMillis() / 1000L) << 24L;
			mSeqNo = 1;
		}
		return (mSeconds + mSeqNo);
	}

	public static synchronized long getOrderId( boolean pBuyOrder ) {
		mSeqNo++;
		if (mSeqNo >= cResetCount) {
			mSeconds = (System.currentTimeMillis() / 1000L) << 24L;
			mSeqNo = 1;
		}
		long x = (mSeconds + mSeqNo);
		if (pBuyOrder) {
			x |= 0x4000000000000000L;
		} else {
			x &= 0x3fffffffffffffffL;
		}
		return x;
	}
}

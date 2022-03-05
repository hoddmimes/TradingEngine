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

package com.hoddmimes.te.positions;

import java.text.SimpleDateFormat;

public class HoldingEntry
{
	private final String  mSid;
	private long    mHolding;
	private long    mTxNo;
	private long    mTime;

	 HoldingEntry( String pSid, long pHolding, long pTxNo ) {
		mSid = pSid;
		mHolding = pHolding;
		mTxNo = pTxNo;
		mTime = System.currentTimeMillis();
	}

	 String getSid() {
		return mSid;
	}

	void updateHolding( long pDeltaHolding, long pTxNo ) {
		mHolding += pDeltaHolding;
		if (pTxNo > 0 ) {
			mTxNo = pTxNo;
		}
		mTime = System.currentTimeMillis();
	}

	void setHolding( long pHolding, long pTxNo) {
		mHolding = pHolding;
		mTxNo = pTxNo;
		mTime = System.currentTimeMillis();
	}


	long getHolding() {
		return mHolding;
	}



	long getTxNo() {
		return mTxNo;
	}

	public String toString() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-Mm-dd HH:mm:ss.SSS");
		return "sid: " + mSid + " holding: " + mHolding + " txno: " + mTxNo + " lstupd: " + sdf.format( mTime );
	}
}

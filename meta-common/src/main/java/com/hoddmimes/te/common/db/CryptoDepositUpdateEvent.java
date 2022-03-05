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

package com.hoddmimes.te.common.db;

public class CryptoDepositUpdateEvent {

	public static enum UpdateType {NetworkUpdate, TeUpdate};

	private String mTxid;
	private Long   mTeTradeSeqno;
	private long   mDeltaQuantityNormalized;
	private String mAccountId;
	private String mSid;
	private UpdateType mUpdateType;


	public CryptoDepositUpdateEvent(String pAccountId, String pSid, long pDeltaQuantityNormalized, long pTeTradeSeqno ) {
		mAccountId = pAccountId;
		mSid = pSid;
		mDeltaQuantityNormalized = pDeltaQuantityNormalized;
		mTeTradeSeqno = pTeTradeSeqno;
		mUpdateType = UpdateType.TeUpdate;
	}

	public CryptoDepositUpdateEvent(String pAccountId, String pSid, long pDeltaQuantityNormalized, String pTxid  ) {
		mAccountId = pAccountId;
		mSid = pSid;
		mDeltaQuantityNormalized = pDeltaQuantityNormalized;
		mTxid = pTxid;
		mUpdateType = UpdateType.TeUpdate;
	}

	public String getTxid() {
		return mTxid;
	}

	public Long getTeTradeSeqno() {
		return mTeTradeSeqno;
	}

	public long getDeltaQuantityNormalized() {
		return mDeltaQuantityNormalized;
	}

	public String getAccountId() {
		return mAccountId;
	}

	public String getSid() {
		return mSid;
	}

	public UpdateType getUpdateType() {
		return mUpdateType;
	}

}
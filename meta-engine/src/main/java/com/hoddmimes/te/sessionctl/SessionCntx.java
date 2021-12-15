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

package com.hoddmimes.te.sessionctl;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;

public class SessionCntx implements SessionCntxInterface
{

	private static SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private String mSessionId; // Interactive session id
	private long   mSignInTime;            // Sign on time
	private String mAccount;                // User id
	private String mApiAuthId; // Unique API session id (should not be the same as the mSessionId)
	private String mMarketDataSessionId;


	public SessionCntx( String pAccount, String pSessionId ) {
		mSessionId = pSessionId;
		mAccount = pAccount;
		mSignInTime = System.currentTimeMillis();
		mApiAuthId = generateApiAuthId();
		mMarketDataSessionId = null;
	}

	private String generateApiAuthId() {
		String tAuthId = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			String tInStr = mAccount + mSessionId + String.valueOf(Runtime.getRuntime().freeMemory());
			md.update( tInStr.getBytes(StandardCharsets.UTF_8));
			byte[] bytes = md.digest();
			StringBuilder sb = new StringBuilder();
			for(int i=0; i< bytes.length ;i++)
			{
				sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			tAuthId = sb.toString();
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		return tAuthId;
	}

	public String getApiAuthId() {
		return mApiAuthId;
	}

	@Override
	public void setMarketDataSessionId( String pMarketDataSessionId ) {
	  mMarketDataSessionId = pMarketDataSessionId;
	}

	@Override
	public String getMarketDataSessionId() {
		return mMarketDataSessionId;
	}

	@Override
	public String getAccount() {
		return mAccount;
	}

	@Override
	public String getSessionId() {
		return mSessionId;
	}

	@Override
	public String getSessionStartTime() {
		return SDF.format( mSignInTime );
	}

	@Override
	public long getSessionStartTimeBin() {
		return mSignInTime;
	}

	@Override
	public String toString() {
		return " [sessid: " + mSessionId + " account: " + mAccount + "]";
	}
}

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

import com.hoddmimes.te.messages.generated.Account;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AccountX extends Account
{
	public AccountX()
	{
		super();
	}


	public AccountX(String pAccountId, String pHashedPassword  )
	{
		this( pAccountId, pHashedPassword,true);
	}

	public AccountX(String pAccountId, String pHashedPassword, boolean pSuspended  ) {
		super();
		super.setAccount( pAccountId );
		super.setSuspended( pSuspended );
		super.setPassword( pHashedPassword );
	}


	public boolean validatePassword( String pPassword ) {
	  if ((pPassword == null) || (pPassword.isEmpty())) {
		  return false;
	  }
	  String tHashPwd = hashPassword( pPassword );
	  if (tHashPwd.contentEquals( super.getPassword().get())) {
		  return true;
	  }
	  return false;
	}

	public static String hashPassword( String pPasswordToHash ) {
		String tGeneratedPassword = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(pPasswordToHash.getBytes());
			byte[] bytes = md.digest();
			StringBuilder sb = new StringBuilder();
			for(int i=0; i< bytes.length ;i++)
			{
				sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			tGeneratedPassword = sb.toString();
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		return tGeneratedPassword;
	}


	public String getAccountId() {
		return super.getAccount().get();
	}

	public boolean isSuspended() {
		return super.getSuspended().get();
	}

	public void setSuspended(boolean pEnabled) {
		super.setSuspended( pEnabled );
	}

	public String getHashedPassword() {
		return super.getPassword().get();
	}

	public void setHashedPassword(String pHashedPassword) {
		super.setPassword( pHashedPassword );
	}
}

package com.hoddmimes.te.sessionctl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class User
{
	private String mUserId;
	private boolean mEnabled;
	private String mHashedPassword;



	public User( String pUserId, String pHashedPassword  ) {
		this( pUserId, pHashedPassword,true);
	}

	public User( String pUserId, String pHashedPassword, boolean pEnabled  ) {
		mEnabled = pEnabled;
		mUserId = pUserId;
		mHashedPassword = pHashedPassword;
	}


	public boolean validatePassword( String pPassword ) {
	  if ((pPassword == null) || (pPassword.isEmpty())) {
		  return false;
	  }
	  if (hashPassword( pPassword ).contentEquals( this.mHashedPassword)) {
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


	public String getUserId() {
		return mUserId;
	}

	public boolean isEnabled() {
		return mEnabled;
	}

	public void setEnabled(boolean pEnabled) {
		mEnabled = pEnabled;
	}

	public String getHashedPassword() {
		return mHashedPassword;
	}

	public void setHashedPassword(String pHashedPassword) {
		mHashedPassword = pHashedPassword;
	}
}

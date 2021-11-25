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

	public AccountX(String pAccountId, String pHashedPassword, boolean pEnabled  ) {
		super();
		super.setAccount( pAccountId );
		super.setEnabled( pEnabled );
		super.setPassword( pHashedPassword );
	}


	public boolean validatePassword( String pPassword ) {
	  if ((pPassword == null) || (pPassword.isEmpty())) {
		  return false;
	  }
	  if (hashPassword( pPassword ).contentEquals( super.getPassword().get())) {
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

	public boolean isEnabled() {
		return super.getEnabled().get();
	}

	public void setEnabled(boolean pEnabled) {
		super.setEnabled( pEnabled );
	}

	public String getHashedPassword() {
		return super.getPassword().get();
	}

	public void setHashedPassword(String pHashedPassword) {
		super.setPassword( pHashedPassword );
	}
}

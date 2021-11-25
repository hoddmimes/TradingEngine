package com.hoddmimes.te.sessionctl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hoddmimes.te.common.interfaces.AuthenticateInterface;
import com.hoddmimes.te.common.AuxJson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class Authenticator implements AuthenticateInterface
{
	private Logger mLog = LogManager.getLogger( Authenticator.class );

	private HashMap<String, AccountX> mAccounts;

	public Authenticator( String pDataStore ) {
		mAccounts = new HashMap<>();
		loadUsers( pDataStore );
	}

	@Override
	public boolean logon(String pAccount, String pPassword) {
		if ((pAccount == null) || (pPassword == null)) {
			mLog.info("Invalid login data, user or password must no be null");
			return false;
		}

		AccountX a = mAccounts.get(pAccount.toUpperCase());
		if (a == null) {
			mLog.info("Account \"" + pAccount + "\" not found");
			return false;
		}
		if (!a.validatePassword( pPassword )) {
			mLog.info("Account \"" + pAccount + "\" invalid password");
			return false;
		}
		mLog.info("Account \"" + pAccount + "\" successfully authenticated");
		return true;
	}

	private void loadUsers( String pDataStore ) {
		JsonArray tAccounts = null;

		try {
			List<JsonElement> tElementList = AuxJson.loadAndParseFile( pDataStore );
			if ((tElementList == null) || (tElementList.size() == 0)) {
				mLog.error("no user defined in user data store \"" + pDataStore + "\"");
				return;
			}
			tAccounts = tElementList.get(0).getAsJsonObject().get("accounts").getAsJsonArray();
			for (int i = 0; i < tAccounts.size(); i++)
			{
				JsonObject a = tAccounts.get(i).getAsJsonObject();
				AccountX tAccount = new AccountX( a.get("account").getAsString().toUpperCase(), a.get("password").getAsString(), a.get("enabled").getAsBoolean());
				mAccounts.put( tAccount.getAccountId(), tAccount );
			}
		}
		catch( IOException e) {
			mLog.fatal("Fail to load accounts from \"" + pDataStore + "\"", e);
			System.exit(-1);
		}
	}
}

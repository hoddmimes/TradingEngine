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

	private HashMap<String, User> mUsers;

	public Authenticator( String pDataStore ) {
		mUsers = new HashMap<>();
		loadUsers( pDataStore );
	}

	@Override
	public boolean logon(String pUser, String pPassword) {
		if ((pUser == null) || (pPassword == null)) {
			mLog.info("Invalid login data, user or password must no be null");
			return false;
		}

		User u = mUsers.get(pUser.toUpperCase());
		if (u == null) {
			mLog.info("User \"" + pUser + "\" not found");
			return false;
		}
		if (!u.validatePassword( pPassword )) {
			mLog.info("User \"" + pUser + "\" invalid password");
			return false;
		}
		mLog.info("User \"" + pUser + "\" successfully authenticated");
		return true;
	}

	private void loadUsers( String pDataStore ) {
		JsonArray tUsers = null;

		try {
			List<JsonElement> tElementList = AuxJson.loadAndParseFile( pDataStore );
			if ((tElementList == null) || (tElementList.size() == 0)) {
				mLog.error("no user defined in user data store \"" + pDataStore + "\"");
				return;
			}
			tUsers = tElementList.get(0).getAsJsonObject().get("users").getAsJsonArray();
			for (int i = 0; i < tUsers.size(); i++)
			{
				JsonObject u = tUsers.get(i).getAsJsonObject();
				User tUser = new User( u.get("username").getAsString().toUpperCase(), u.get("password").getAsString(), u.get("enabled").getAsBoolean());
				mUsers.put( tUser.getUserId(), tUser );
			}
		}
		catch( IOException e) {
			mLog.fatal("Fail to load users from \"" + pDataStore + "\"", e);
			System.exit(-1);
		}
	}
}

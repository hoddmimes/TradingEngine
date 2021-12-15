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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.interfaces.AuthenticateInterface;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.interfaces.TeMgmtServices;
import com.hoddmimes.te.management.service.MgmtCmdCallbackInterface;
import com.hoddmimes.te.management.service.MgmtComponentInterface;
import com.hoddmimes.te.messages.MgmtMessageRequest;
import com.hoddmimes.te.messages.MgmtMessageResponse;
import com.hoddmimes.te.messages.generated.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Authenticator implements AuthenticateInterface, MgmtCmdCallbackInterface
{
	private Logger mLog = LogManager.getLogger( Authenticator.class );
	private String mDataStore;
	private HashMap<String, AccountX> mAccounts;

	public Authenticator( String pDataStore ) {
		mAccounts = new HashMap<>();
		mDataStore = pDataStore;
		loadAccounts( pDataStore );
		MgmtComponentInterface tMgmt = TeAppCntx.getInstance().getMgmtService().registerComponent( TeMgmtServices.Autheticator, 0, this );
	}

	@Override
	public Account logon(String pAccount, String pPassword) {
		if ((pAccount == null) || (pPassword == null)) {
			mLog.info("Invalid login data, user or password must no be null");
			return null;
		}

		AccountX a = mAccounts.get(pAccount.toUpperCase());
		if (a == null) {
			mLog.info("Account \"" + pAccount + "\" not found");
			return null;
		}
		if (!a.validatePassword( pAccount.toUpperCase() + pPassword )) {
			mLog.info("Account \"" + pAccount + "\" invalid password");
			return null;
		}
		mLog.info("Account \"" + pAccount + "\" successfully authenticated");
		return a;
	}

	private void loadAccounts(String pDataStore ) {
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
				AccountX tAccount = new AccountX( a.get("account").getAsString().toUpperCase(), a.get("password").getAsString(), a.get("suspended").getAsBoolean());
				mAccounts.put( tAccount.getAccountId(), tAccount );
			}
		}
		catch( IOException e) {
			mLog.fatal("Fail to load accounts from \"" + pDataStore + "\"", e);
			System.exit(-1);
		}
	}

	private void saveAccounts( String pReason) {
		JsonObject jAccounts = new JsonObject();
		JsonArray jAccountArray = new JsonArray();
		for( Account tAcc : mAccounts.values()) {
			jAccountArray.add( tAcc.toJson());
		}
		jAccounts.add("accounts", jAccountArray );

		try {
			FileOutputStream tOut = new FileOutputStream(  mDataStore );
			tOut.write( jAccounts.toString().getBytes(StandardCharsets.UTF_8));
			tOut.flush();
			tOut.close();
		}
		catch( IOException e) {
			e.printStackTrace();
		}
		mLog.info("AccountDefinitions \"" + mDataStore + "\" is updated, reason: " + pReason );
	}

	private MgmtGetAccountsResponse mgmtCmdGetAccounts(MgmtGetAccountsRequest pRqst ) {
		MgmtGetAccountsResponse tRsp = new MgmtGetAccountsResponse().setRef( pRqst.getRef().get());
		List<Account> tAccLst = new ArrayList<>( mAccounts.values());
		tRsp.addAccounts( tAccLst );
		return tRsp;
	}

	private MgmtSetAccountsResponse mgmtCmdSetAccounts(MgmtSetAccountsRequest pRqst ) {
		MgmtSetAccountsResponse tRsp = new MgmtSetAccountsResponse().setRef( pRqst.getRef().get());
		AccountX tAccount = mAccounts.get( pRqst.getAccountId().get());
		tAccount.setSuspended( pRqst.getSuspended().get());
		tRsp.setAccount( tAccount );
		return tRsp;
	}

	MgmtUpdateAccountResponse mgmtCmdUpdateAccounts( MgmtUpdateAccountRequest pRqst )
	{
			AccountX tAccount = mAccounts.get( pRqst.getAccountId().get());
			if (tAccount == null) {
				return new MgmtUpdateAccountResponse().setRef( pRqst.getRef().get()).setIsUpdated(false).setAccount(null);
			}
			tAccount.setSuspended( pRqst.getSuspended().get());
			if (!pRqst.getHashedPassword().isEmpty()) {
				tAccount.setHashedPassword( pRqst.getHashedPassword().get());
			}
			saveAccounts(" account: " + pRqst.getAccountId().get() + " updated");
			return new MgmtUpdateAccountResponse().setRef( pRqst.getRef().get()).setIsUpdated(true).setAccount(tAccount);
	}

	MgmtAddAccountResponse mgmtCmdAddAccounts( MgmtAddAccountRequest pRqst ) {
		AccountX tAccount = mAccounts.get(pRqst.getAccountId().get());
		if (tAccount != null) {
			return new MgmtAddAccountResponse().setRef(pRqst.getRef().get()).setIsAddded(false).setStatusMessage("account already exists");
		}
		tAccount = new AccountX(pRqst.getAccountId().get(), pRqst.getHashedPassword().get(), pRqst.getSuspended().get());
		mAccounts.put(pRqst.getAccountId().get(), tAccount);
		saveAccounts(" account: " + pRqst.getAccountId().get() + " added");
		return new MgmtAddAccountResponse().setRef(pRqst.getRef().get()).setIsAddded(true).setAccount(tAccount).setStatusMessage("successfully added");
	}


	@Override
	public MgmtMessageResponse mgmtRequest(MgmtMessageRequest pMgmtRequest) {
		if (pMgmtRequest instanceof MgmtGetAccountsRequest) {
			return mgmtCmdGetAccounts( (MgmtGetAccountsRequest) pMgmtRequest );
		}
		if (pMgmtRequest instanceof MgmtSetAccountsRequest) {
			return mgmtCmdSetAccounts( (MgmtSetAccountsRequest) pMgmtRequest );
		}
		if (pMgmtRequest instanceof MgmtUpdateAccountRequest) {
			return mgmtCmdUpdateAccounts( (MgmtUpdateAccountRequest) pMgmtRequest );
		}
		if (pMgmtRequest instanceof  MgmtAddAccountRequest) {
			return mgmtCmdAddAccounts( (MgmtAddAccountRequest) pMgmtRequest );
		}
		throw new RuntimeException("No commnad entry found for : " + pMgmtRequest.getMessageName());
	}
}

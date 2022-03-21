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

package com.hoddmimes.te.management;

import com.google.gson.*;
import com.hoddmimes.te.common.db.TEDB;
import com.hoddmimes.te.messages.generated.Account;
import com.hoddmimes.te.sessionctl.AccountX;
import com.mongodb.client.result.UpdateResult;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateAccounts
{
	private final String REGEX_EMAIL_VALIDATION = "^[\\w-\\+]+(\\.[\\w]+)*@[\\w-]+(\\.[\\w]+)*(\\.[a-zA-Z]{2,})$";
	private static final String DB_HOST  = "localhost";
	private static final int DB_PORT = 27017;
	private static final String DB_NAME = "TradingEngine";



	TEDB mDb ;
	public static void main(String[] args) {
		CreateAccounts pca = new CreateAccounts();
		pca.create( args );
	}

	private void create(String[] args ) {
		if (args.length == 0) {
			System.out.println("usage: $CreateAccount <account-file>.json");
			System.exit(0);
		}
		try {
			readAndLoadAccounts( args[0] );
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}

	public int readAndLoadAccounts( String pAccountFilename) throws IOException, JsonSyntaxException
	{
		return readAndLoadAccounts( new File( pAccountFilename ));
	}

	public int readAndLoadAccounts(File tAccountFile ) throws IOException, JsonSyntaxException {
		FileReader tReader = new FileReader(tAccountFile);
		JsonObject jAccountConfig = JsonParser.parseReader(tReader).getAsJsonObject();
		JsonArray jAccounts = jAccountConfig.get("accounts").getAsJsonArray();
		for (int i = 0; i < jAccounts.size(); i++) {
			createUser(jAccounts.get(i).getAsJsonObject());
		}
		return jAccounts.size();
	}

	private void createUser( JsonObject jAccount) {
		if (!jAccount.has("account")) {
			System.out.println(" required parameter \"account\" is missing, in " + jAccount.toString());
			System.exit(0);
		}
		if (!jAccount.has("mail")) {
			System.out.println(" required parameter \"mail\" is missing, in " + jAccount.toString());
			System.exit(0);
		}
		Pattern tMailPattern = Pattern.compile(REGEX_EMAIL_VALIDATION);
		Matcher m = tMailPattern.matcher(jAccount.get("mail").getAsString());
		if (!m.matches()) {
			System.out.println("invalid mail address syntax (" + jAccount.get("mail").getAsString() + ")");
			System.exit(0);
		}
		if ((!jAccount.has("password")) || (jAccount.get("password").getAsString().isEmpty()) || (jAccount.get("password").getAsString().isBlank())) {
			System.out.println(" required parameter \"password\" is missing or is empty, in " + jAccount.toString());
			System.exit(0);
		}

		boolean tConfirmed = (jAccount.has("confirmed")) ? jAccount.get("confirmed").getAsBoolean() : true;
		boolean tSuspended = (jAccount.has("suspended")) ? jAccount.get("suspended").getAsBoolean() : false;

		mDb = new TEDB( DB_NAME, DB_HOST, DB_PORT);
		mDb.connectToDatabase();
		addAccount( jAccount.get("account").getAsString(),
					jAccount.get("mail").getAsString(),
					jAccount.get("password").getAsString(),
					jAccount.get("confirmed").getAsBoolean(),
					jAccount.get("suspended").getAsBoolean());
	}


	private void addAccount( String pAccountId, String pMailAddr, String pPassword, boolean pConfirmed, boolean pSuspended ) {
		Account tAccount = new Account().setAccountId( pAccountId).setMailAddr( pMailAddr).setConfirmed( pConfirmed).setSuspended( pSuspended);
		tAccount.setPassword( AccountX.hashPassword(pAccountId.toUpperCase() + pPassword));
		UpdateResult tUpdResult = mDb.updateAccount( tAccount, true );
		if (tUpdResult.getUpsertedId() == null) {
			System.out.println("Warning account " + pAccountId + " was updated " + tUpdResult.toString());
		}
	}
}

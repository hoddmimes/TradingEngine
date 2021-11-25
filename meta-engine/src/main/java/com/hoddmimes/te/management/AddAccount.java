package com.hoddmimes.te.management;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hoddmimes.jaux.AuxParseArguments;
import com.hoddmimes.te.messages.generated.Account;
import com.hoddmimes.te.sessionctl.AccountX;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class AddAccount
{
	String mAccount;
	String mPassword;
	boolean mEnabled = true;
	String mAccountConfiguration;
	JsonObject jAccount;


	public static void main(String[] args) {
		AddAccount au = new AddAccount();
		au.parseArguments( args );
		au.loadUsers();
		au.getAccount();
		au.addAccount();
		au.saveUsers();
	}

	private void parseArguments( String[] args ) {
		mAccount = AuxParseArguments.parse( args, "user", "");
		mPassword = AuxParseArguments.parse( args, "password", "");
		mEnabled = AuxParseArguments.parseBoolean( args, "enabled", true);
		mAccountConfiguration = AuxParseArguments.parse( args, "accountdb", "AccountDefinitions.json");
	}

	private void saveUsers() {
		try {
			FileOutputStream tOut = new FileOutputStream(  mAccountConfiguration );
			tOut.write( mAccount.toString().getBytes(StandardCharsets.UTF_8));
			tOut.flush();
			tOut.close();
			System.out.println(" user \"" + mAccount + "\" save to user definition file \"" + mAccountConfiguration + "\"");
		}
		catch( IOException e) {
			e.printStackTrace();
		}
	}
	private void loadUsers() {
		try {
			InputStreamReader tReader = new InputStreamReader( new FileInputStream( mAccountConfiguration ));
			jAccount = JsonParser.parseReader( tReader ).getAsJsonObject();
			tReader.close();
		}
		catch( IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void addAccount() {
		AccountX a = new AccountX();
		a.setAccount(mAccount);
		a.setPassword(AccountX.hashPassword( mPassword ));
		a.setEnabled( mEnabled );
		jAccount.get("account").getAsJsonArray().add( a.toJson());
	}

	private void getAccount() {
		Scanner tScanner = new Scanner(System.in);
		while( (mAccount.isEmpty()) || (mAccount.isEmpty())) {
			System.out.print(" account: ");
			mAccount = tScanner.nextLine();
		}

		while( (mPassword.isEmpty()) || (mPassword.isEmpty())) {
			System.out.print(" password: ");
			mPassword = tScanner.nextLine();
		}
	}
}

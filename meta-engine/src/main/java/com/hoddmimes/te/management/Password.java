package com.hoddmimes.te.management;

import com.google.gson.JsonArray;
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

public class Password
{
	String mUsername;
	String mPassword;
	boolean mEnabled = true;
	String mUserConfiguration;
	JsonObject mUsers;


	public static void main(String[] args) {
		Password au = new Password();
		au.parseArguments( args );
		au.loadUsers();
		au.getUser();
		au.changeUser();
		au.saveUsers();
	}

	private void parseArguments( String[] args ) {
		mUsername = AuxParseArguments.parse( args, "user", "");
		mPassword = AuxParseArguments.parse( args, "password", "");
		mUserConfiguration = AuxParseArguments.parse( args, "userdb", "UserDefinitions.json");
	}

	private void saveUsers() {
		try {
			FileOutputStream tOut = new FileOutputStream(  mUserConfiguration );
			tOut.write( mUsers.toString().getBytes(StandardCharsets.UTF_8));
			tOut.flush();
			tOut.close();
			System.out.println(" Password for user \"" + mUsername + "\" updated" );
		}
		catch( IOException e) {
			e.printStackTrace();
		}
	}
	private void loadUsers() {
		try {
			InputStreamReader tReader = new InputStreamReader( new FileInputStream( mUserConfiguration ));
			mUsers = JsonParser.parseReader( tReader ).getAsJsonObject();
			tReader.close();
		}
		catch( IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void changeUser() {
		JsonArray jUsers = mUsers.get("users").getAsJsonArray();
		JsonObject jUser = null;
		for (int i = 0; i < jUsers.size(); i++) {
			jUser = jUsers.get(i).getAsJsonObject();
			if (mUsername.contentEquals( jUser.get("username").getAsString())) {
				break;
			}
		}

		if (jUser == null) {
			System.out.println("User \"" + mUsername + "\" not found");
			System.exit(-1);
		}

		Account a = new Account( jUser.toString() );
		a.setPassword( AccountX.hashPassword( mPassword ));
	}

	private void getUser() {
		Scanner tScanner = new Scanner(System.in);
		while( (mUsername.isEmpty()) || (mUsername.isEmpty())) {
			System.out.print(" username: ");
			mUsername = tScanner.nextLine();
		}

		while( (mPassword.isEmpty()) || (mPassword.isEmpty())) {
			System.out.print(" password: ");
			mPassword = tScanner.nextLine();
		}
	}
}

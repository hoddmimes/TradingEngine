package com.hoddmimes.te.management;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hoddmimes.jaux.AuxParseArguments;
import com.hoddmimes.te.messages.generated.User;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class AddUser
{
	String mUsername;
	String mPassword;
	boolean mEnabled = true;
	String mUserConfiguration;
	JsonObject mUsers;


	public static void main(String[] args) {
		AddUser au = new AddUser();
		au.parseArguments( args );
		au.loadUsers();
		au.getUser();
		au.addUser();
		au.saveUsers();
	}

	private void parseArguments( String[] args ) {
		mUsername = AuxParseArguments.parse( args, "user", "");
		mPassword = AuxParseArguments.parse( args, "password", "");
		mEnabled = AuxParseArguments.parseBoolean( args, "enabled", true);
		mUserConfiguration = AuxParseArguments.parse( args, "userdb", "UserDefinitions.json");
	}

	private void saveUsers() {
		try {
			FileOutputStream tOut = new FileOutputStream(  mUserConfiguration );
			tOut.write( mUsers.toString().getBytes(StandardCharsets.UTF_8));
			tOut.flush();
			tOut.close();
			System.out.println(" user \"" + mUsername + "\" save to user definition file \"" + mUserConfiguration + "\"");
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

	private void addUser() {
		User u = new User();
		u.setUsername( mUsername );
		u.setPassword( com.hoddmimes.te.sessionctl.User.hashPassword( mPassword ));
		u.setEnabled( mEnabled );
		mUsers.get("users").getAsJsonArray().add( u.toJson());
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

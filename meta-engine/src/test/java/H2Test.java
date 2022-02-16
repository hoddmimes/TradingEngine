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

import java.io.File;
import java.sql.*;

public class H2Test
{
	private static final String DB_DRIVER = "org.h2.Driver";
	private static final String DB_CONNECTION = "jdbc:h2:file:./h2/TE";
	private static final String DB_USER = "sa";
	private static final String DB_PASSWORD = "sa";

	Connection mDbConnection;


	public static void main(String[] args) {
		H2Test h2 = new H2Test();
		h2.connect();
		h2.createSchema();

	}

	private void createSchema() {
		File tDbFile = new File("./h2/TE.mv.db");
		if (tDbFile.exists()) {
			System.out.println("Database exists will not be created");
			return;
		}

		Statement stmt = null;
		try {
			mDbConnection.setAutoCommit(false);
			stmt = mDbConnection.createStatement();
			stmt.execute("create table Account(name varchar(255)  primary key not null, password varchar(255) not null, suspended varchar (255) not null, tradeCrypto varchar (255) not null)");
			stmt.execute("create table CryptoDeposit (name varchar(255) primary key not null, bitcoins int, ether int)");
			stmt.execute("create table CryptoEvent (time varchar (255) primary key, wallet varchar(255), cevent varchar(255))");

			ResultSet rs = stmt.executeQuery("select * from Account");
			System.out.println("H2 Database inserted through Statement");
			while (rs.next()) {
				System.out.println(" Name "+rs.getString("name"));
			}
			stmt.close();
			mDbConnection.commit();
		} catch (SQLException e) {
			System.out.println("Exception Message " + e.getLocalizedMessage());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				mDbConnection.close();
			} catch (SQLException pE) {
				pE.printStackTrace();
			}
		}
	}

	private void connect() {
		mDbConnection = null;
		try {
			Class.forName(DB_DRIVER);
		} catch (ClassNotFoundException e) {
			System.out.println(e.getMessage());
		}
		try {
			mDbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

}

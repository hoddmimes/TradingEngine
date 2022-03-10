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

package com.hoddmimes.te.client;

import javax.swing.*;

public class TeGuiTestApp
{
	Connector mConnector;
	MainFrame mMainFrame;
	String mAccount = null;
	String mPassword = null;


	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		TeGuiTestApp tClient = new TeGuiTestApp();
		tClient.parseArguments( args );
		tClient.login();
		tClient.go();
		while( true ) {
			try { Thread.sleep( 1000L);}
			catch( InterruptedException e) {}
		}
	}

	private void go() {
		mMainFrame = new MainFrame( mConnector );
	}

	private void login() {
		mConnector = new Connector( null, null, mAccount, mPassword );
	}

	private void parseArguments( String args[] ) {
		int i = 0;
		while( i < args.length) {
			if (args[i].contentEquals("-account")) {
				mAccount = args[++i];
			}
			if (args[i].contentEquals("-password")) {
				mPassword = args[++i];
			}
			i++;
		}
	}
}

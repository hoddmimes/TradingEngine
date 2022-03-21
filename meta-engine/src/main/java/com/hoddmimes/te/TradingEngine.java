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

package com.hoddmimes.te;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.db.TEDB;
import com.hoddmimes.te.common.interfaces.ConnectorInterface;
import com.hoddmimes.te.common.interfaces.TeService;
import com.hoddmimes.te.common.ipc.IpcService;
import com.hoddmimes.te.cryptogwy.CryptoGateway;
import com.hoddmimes.te.engine.MatchingEngine;
import com.hoddmimes.te.instrumentctl.InstrumentContainer;
import com.hoddmimes.te.management.CreateAccounts;
import com.hoddmimes.te.messages.generated.Account;
import com.hoddmimes.te.positions.PositionController;
import com.hoddmimes.te.sessionctl.SessionController;
import com.hoddmimes.te.trades.TradeContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.hoddmimes.te.sessionctl.SessionController.INTERNAL_SESSION_ID;

@SpringBootApplication
public class TradingEngine
{
	private Logger mLog = LogManager.getLogger( TradingEngine.class );
	private JsonObject                  mConfiguration;
	private IpcService                  mIpcService;
	private TEDB                        mDb;



	public static void main(String[] args) {
		TradingEngine te = new TradingEngine();
		te.mLog.info("Starting TE Engine " + Version.build);

		te.parsePargument( args );

		te.initialize();
		while( true ) {
			try {
				Thread.sleep(1000L);
			}
			catch( InterruptedException e) {};
		}
	}

	public void setTestMode() {
		TeAppCntx.getInstance().setTestMode();
	}

	 public void initialize() {
		mLog.info("current working dir: " + currentWorkingDirURI());

		// Setup Management Service
		 mIpcService = new IpcService( AuxJson.navigateObject( mConfiguration, "TeConfiguration/ipc"));
		 mLog.info("IPC Service successfully started");


		// Connect the database
		 String tDbHost = AuxJson.navigateString( mConfiguration, "TeConfiguration/dbConfiguration/dbHost");
		 int tDbPort = AuxJson.navigateInt( mConfiguration, "TeConfiguration/dbConfiguration/dbPort");
		 String tDbName = AuxJson.navigateString( mConfiguration, "TeConfiguration/dbConfiguration/dbName");
		 mDb = new TEDB( tDbName, tDbHost, tDbPort );
		 mDb.connectToDatabase();
		 TeAppCntx.getInstance().setDatabase( mDb );

		 mIpcService = new IpcService( AuxJson.navigateObject( mConfiguration, "TeConfiguration/ipc"));
		TeAppCntx.getInstance().setIpcService( mIpcService );

		// For convience pre-create user if does not exists. This just for making testing easier.
		 // this code should be removed eventually
		 initialLoadOfAccounts();

		// Instansiate Instrument Container
		InstrumentContainer tInstrumentContainer = new InstrumentContainer( mConfiguration, mIpcService );

		// Instansiate Position Controller
		 PositionController tPositionController = new PositionController( mConfiguration, mIpcService, mDb );

		 // Instansiate Crypto Gateway Controller
		 CryptoGateway CryptoGateway = new CryptoGateway( mConfiguration, mIpcService );

		// Instansiate Trade container
		TradeContainer tTradeContainer = new TradeContainer( mConfiguration, mIpcService );

		// Instansiate Session Control, will start the Rest Controller and the WebSocket distributor
		initConnector();

		//Instansiate Matching Engine
		TeAppCntx.getInstance().waitForServiceToStart( TeService.MarketData );
		MatchingEngine tMatchingEngine = new MatchingEngine( mConfiguration, mIpcService );
	}
	private String currentWorkingDirURI() {
		return FileSystems.getDefault().getPath("").toAbsolutePath().toUri().toString();
	}

	private void initConnector() {
		SessionController tSessionController = null;
		ConnectorInterface tConnector = null;

		try {
			tSessionController = new SessionController( mConfiguration, mIpcService );
		} catch (IOException e) {
			mLog.fatal("fatal to instansiate session controller", e);
			System.exit(-1);
		}


		String tConnImplClsString = AuxJson.navigateString( mConfiguration, "TeConfiguration/connectorConfiguration/implementaion");
		try {
			Class[] cArg = new Class[2];
			cArg[0] = JsonObject.class;
			cArg[1] = ConnectorInterface.ConnectorCallbackInterface.class;
			Class c = Class.forName(tConnImplClsString);
			tConnector = (ConnectorInterface) c.getDeclaredConstructor( cArg ).newInstance(mConfiguration, tSessionController);
		} catch (Exception e) {
			mLog.error("Failed to instansiate Connector class \"" + tConnImplClsString +"\" reason: " + e.getMessage());
		}

		try {
			tConnector.declareAndStart();
			TeAppCntx.getInstance().registerService( tSessionController);
		}
		catch( IOException e) {
			mLog.fatal("fatail to declare and run connector ( " + tConnector.getClass().getSimpleName() + "), reason:  " + e.getMessage(), e );
			System.exit(-1);
		}
		synchronized (this) {
			this.notifyAll();
		}
	}

	private void  initialLoadOfAccounts() {
		String  tAccountFileName = AuxJson.navigateString( mConfiguration,"TeConfiguration/loadAccounts", null);
		if (tAccountFileName == null) {
			return;
		}

		File tAccountFile = new File( tAccountFileName );
		if (!tAccountFile.exists()) {
			return;
		}

		List<Account> tAccounts = mDb.findAllAccount();
		if (tAccounts.size() > 0) {
			return;
		}

		try {
			CreateAccounts tAccountFabric = new CreateAccounts();
			int tCount = tAccountFabric.readAndLoadAccounts( tAccountFile );
			mLog.info( tCount + " accounts intitially loaded from file: " + tAccountFileName);
		}
		catch( Exception e) {
			mLog.error("failed to load initial accounts, reason: " + e.getMessage(), e);
		}

	}
	private boolean isComment( String pString )
	{
		if ((pString.isEmpty()) || (pString.isBlank())) {
			return true;
		}
		Pattern tPattern = Pattern.compile("^\\s*#");
		Matcher m = tPattern.matcher( pString );
		if (m.find()) {
			return true;
		}
		return false;
	}

	 public void parsePargument( String pArgs[]) {
		 URI tConfigURI = null;

		 try {
			 if (System.getenv("teconfiguration") != null) {
				 tConfigURI = new URI(System.getenv("teconfiguration"));
			 } else {
				 if (pArgs.length == 0) {
					 mLog.error("configuration URI source program argument is missing");
					 System.exit(-1);
				 }
				 tConfigURI = new URI(pArgs[0]);
			 }
		 } catch (URISyntaxException ue) {
			 mLog.error("invalid configuration URI syntax \"" + pArgs[0] + "\", reason: " + ue.getMessage());
			 System.exit(-1);
		 }

		InputStream tInStream = null;
		StringBuilder tStringBuilder = new StringBuilder();
		try {
			tInStream = tConfigURI.toURL().openConnection().getInputStream();
			BufferedReader tReader = new BufferedReader(new InputStreamReader(tInStream));
			mConfiguration = JsonParser.parseReader(tReader).getAsJsonObject();
			TeAppCntx.getInstance().setTeConfiguration( mConfiguration );
			mLog.info("successfully load TE configuration from \"" + pArgs[0] + "\"");

			if (System.getenv("usecrypto") != null) {
				JsonObject jObject = AuxJson.navigateObject(mConfiguration,"TeConfiguration/cryptoGateway").getAsJsonObject();
				boolean tEnableFlag = Boolean.parseBoolean( System.getenv("usecrypto"));
				jObject.addProperty("enable", tEnableFlag );

				JsonObject jBtcObject = AuxJson.navigateObject(mConfiguration,"TeConfiguration/cryptoGateway/bitcoin").getAsJsonObject();
				jObject.addProperty("enable", tEnableFlag );

				JsonObject jEthereumObject = AuxJson.navigateObject(mConfiguration,"TeConfiguration/cryptoGateway/ethereum").getAsJsonObject();
				jObject.addProperty("enable", tEnableFlag );

				mLog.info("JUNIT TEST TeConfiguration/cryptoGateway/enable == " + tEnableFlag );

			}


		} catch (MalformedURLException ue) {
			mLog.error("invalid malformed URI syntax \"" + pArgs[0] + "\", reason: " + ue.getMessage());
			System.exit(-1);
		} catch (IOException e) {
			mLog.error("failed to read configuration file \"" + pArgs[0] + "\", reason: " + e.getMessage());
			System.exit(-1);
		}
	}

	public MessageInterface testMessage(JsonObject pRequestMessage ) {
		return ((SessionController) TeAppCntx.getInstance().getService(TeService.SessionService)).connectorMessage( INTERNAL_SESSION_ID, pRequestMessage.toString() );
	}

}

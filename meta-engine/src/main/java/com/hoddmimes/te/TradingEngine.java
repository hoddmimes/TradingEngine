package com.hoddmimes.te;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.interfaces.ConnectorInterface;
import com.hoddmimes.te.engine.MatchingEngine;
import com.hoddmimes.te.engine.MatchingEngineFrontend;
import com.hoddmimes.te.instrumentctl.InstrumentContainer;
import com.hoddmimes.te.sessionctl.SessionController;
import com.hoddmimes.te.trades.TradeContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootApplication
public class TradingEngine
{
	private Logger mLog = LogManager.getLogger( TradingEngine.class );
	private SessionController           mSessionController;
	private ConnectorInterface          mConnector;
	private MatchingEngine              mMatchingEngine;
	private MatchingEngineFrontend      mMatchingEngineFrontend;
	private InstrumentContainer         mInstrumentContainer;
	private TradeContainer              mTradeContainer;
	private JsonObject                  mConfiguration;



	public static void main(String[] args) {
		TradingEngine te = new TradingEngine();
		te.parsePargument( args );

		te.initialize();
		while( true ) {
			try {
				Thread.sleep(1000L);
			}
			catch( InterruptedException e) {};
		}
	}


	private void initialize() {
		// Instansiate Instrument Container
		mInstrumentContainer = new InstrumentContainer( mConfiguration );
		mLog.info("successfully loaded InstrumentContainer");

		//Instansiate Matching Engine Frontend
		mMatchingEngineFrontend = new MatchingEngineFrontend( mConfiguration, mMatchingEngine);
		mLog.info("successfully loaded MatchingEngineFrontend");

		// Instansiate Trade container
		mTradeContainer = new TradeContainer( mConfiguration );
		mLog.info("successfully loaded TradeContainer");

		// Instansiate Session Control
		try {
			mSessionController = new SessionController( mConfiguration );
			mLog.info("successfully loaded SessionController");
		} catch (IOException e) {
			mLog.fatal("fatal to instansiate session controller", e);
			System.exit(-1);
		}

		// Instansiate Controller
		initConnector();

		//Instansiate Matching Engine
		mMatchingEngine = new MatchingEngine( mConfiguration, mInstrumentContainer, TeAppCntx.getInstance().getMarketDataDistributor() );
		mLog.info("successfully loaded MatchingEngine");

		//Instansiate Matching Engine Frontend
		mMatchingEngineFrontend = new MatchingEngineFrontend( mConfiguration, mMatchingEngine);
		mLog.info("successfully loaded MatchingEngineFrontend");
	}

	private void initConnector() {
		String tConnImplClsString = AuxJson.navigateString( mConfiguration, "TeConfiguration/connectorConfiguration/implementaion");
		try {
			Class[] cArg = new Class[2];
			cArg[0] = JsonObject.class;
			cArg[1] = ConnectorInterface.ConnectorCallbackInterface.class;
			Class c = Class.forName(tConnImplClsString);
			mConnector = (ConnectorInterface) c.getDeclaredConstructor( cArg ).newInstance(mConfiguration, mSessionController);
			TeAppCntx.getInstance().setConnector( mConnector );
			mLog.info("successfully loaded Connector ( " + mConnector.getClass().getSimpleName() + " )");
		} catch (Exception e) {
			mLog.error("Failed to instansiate Connector class \"" + tConnImplClsString +"\" reason: " + e.getMessage());
		}

		try {
			mConnector.declareAndStart();
		}
		catch( IOException e) {
			mLog.fatal("fatail to declare and run connector ( " + mConnector.getClass().getSimpleName() + "), reason:  " + e.getMessage(), e );
			System.exit(-1);
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

	private void parsePargument( String pArgs[]) {
		if (pArgs.length == 0) {
			mLog.error("configuration URI source program argument is missing");
			System.exit(-1);
		}

		InputStream tInStream = null;
		StringBuilder tStringBuilder = new StringBuilder();
		try {
			URI tConfigURI = new URI(pArgs[0]);
			tInStream = tConfigURI.toURL().openConnection().getInputStream();
			BufferedReader tReader = new BufferedReader(new InputStreamReader(tInStream));
			mConfiguration = JsonParser.parseReader(tReader).getAsJsonObject();
			TeAppCntx.getInstance().setTeConfiguration( mConfiguration );
			mLog.info("successfully load TE configuration from \"" + pArgs[0] + "\"");
		} catch (URISyntaxException ue) {
			mLog.error("invalid configuration URI syntax \"" + pArgs[0] + "\", reason: " + ue.getMessage());
			System.exit(-1);
		} catch (IOException e) {
			mLog.error("failed to read configuration file \"" + pArgs[0] + "\", reason: " + e.getMessage());
			System.exit(-1);
		}
	}


}

package com.hoddmimes.te;

import com.google.gson.JsonObject;
import com.hoddmimes.te.common.interfaces.ConnectorInterface;
import com.hoddmimes.te.common.interfaces.MarketDataInterface;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.engine.MatchingEngine;
import com.hoddmimes.te.engine.MatchingEngineFrontend;
import com.hoddmimes.te.instrumentctl.InstrumentContainer;
import com.hoddmimes.te.sessionctl.SessionController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;


public class TeAppCntx {
	private static final Logger mLog = LogManager.getLogger( TeAppCntx.class );
	private static TeAppCntx mInstance;
	private ConnectorInterface  mConnector;
	private SessionController mSessionController;
	private InstrumentContainer mInstrumentContainer;
	private MatchingEngine mMatchingEngine;
	private MarketDataInterface mMarketDataDistributor;
	private MatchingEngineFrontend mMatchingEngineFrontend;
	private JsonObject mTeConfiguration;

	private TeAppCntx() {
		setConnector(null);
		setSessionController(null);
		setInstrumentContainer(null);
		setMatchingEngine(null);
		setMatchingEngineFrontend(null);
		setTeConfiguration(null);
		setMarketDataDistributor(null);
	}

	public static TeAppCntx getInstance() {
		synchronized ( TeAppCntx.class ) {
			if (mInstance == null) {
				mInstance = new TeAppCntx();
			}
			return mInstance;
		}
	}

	public SessionCntxInterface getIntenalSessionContext() {
		return this.getSessionController().getInternalSessionContext();
	}

	public ConnectorInterface getConnector() {
		if (mConnector == null) {
			mLog.fatal("try to retrieve Connector implementation before setting it ", new Exception("Connector is null"));
			System.exit(-1);
		}
		return mConnector;
	}

	public void setConnector(ConnectorInterface pConnector) {
		mConnector = pConnector;
	}

	public SessionController getSessionController() {
		if (mSessionController == null) {
			mLog.fatal("try to retrieve SessionController implementation before setting it ", new Exception("SessionController is null"));
			System.exit(-1);
		}
		return mSessionController;
	}

	public void setSessionController(SessionController pSessionController) {
		mSessionController = pSessionController;
	}

	public InstrumentContainer getInstrumentContainer() {
		if (mInstrumentContainer == null) {
			mLog.fatal("try to retrieve InstrumentContainer implementation before setting it ", new Exception("InstrumentContainer is null"));
			System.exit(-1);
		}
		return mInstrumentContainer;
	}

	public void setInstrumentContainer(InstrumentContainer pInstrumentContainer) {
		mInstrumentContainer = pInstrumentContainer;
	}

	public MatchingEngine getMatchingEngine() {
		if (mMatchingEngine == null) {
			mLog.fatal("try to retrieve MatchingEngine implementation before setting it ", new Exception("MatchingEngine is null"));
			System.exit(-1);
		}
		return mMatchingEngine;
	}

	public void setMatchingEngine(MatchingEngine pMatchingEngine) {
		mMatchingEngine = pMatchingEngine;
	}

	public MatchingEngineFrontend getMatchingEngineFrontend() {
		if (mMatchingEngineFrontend == null) {
			mLog.fatal("try to retrieve MatchingEngineFrontend implementation before setting it ", new Exception("MatchingEngineFrontend is null"));
			System.exit(-1);
		}
		return mMatchingEngineFrontend;
	}

	public void setMatchingEngineFrontend(MatchingEngineFrontend pMatchingEngineFrontend) {
		mMatchingEngineFrontend = pMatchingEngineFrontend;
	}


	public void setTeConfiguration( JsonObject pTeConfiguration ) {
		mTeConfiguration = pTeConfiguration;
	}

	public JsonObject getTeConfiguration() {
		if (mTeConfiguration == null) {
			mLog.fatal("try to retrieve TEConfiguration implementation before setting it ", new Exception("TEConfiguration is null"));
			System.exit(-1);
		}
		return mTeConfiguration;
	}

	public MarketDataInterface getMarketDataDistributor() {
		if (mMarketDataDistributor == null) {
			mLog.fatal("try to retrieve MarketData implementation before setting it ", new Exception("MarketData is null"));
			System.exit(-1);
		}
		return mMarketDataDistributor;
	}

	public void setMarketDataDistributor(MarketDataInterface pMarketDataDistributor) {
		mMarketDataDistributor = pMarketDataDistributor;
	}
}

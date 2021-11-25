package com.hoddmimes.te;

import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.JsonSchemaValidator;
import com.hoddmimes.te.common.interfaces.ConnectorInterface;
import com.hoddmimes.te.common.interfaces.MarketDataInterface;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.engine.MatchingEngineInterface;
import com.hoddmimes.te.instrumentctl.InstrumentContainer;
import com.hoddmimes.te.sessionctl.SessionController;
import com.hoddmimes.te.trades.TradeContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class TeAppCntx {
	private static final Logger mLog = LogManager.getLogger( TeAppCntx.class );
	private static TeAppCntx mInstance;
	private ConnectorInterface  mConnector;
	private SessionController mSessionController;
	private InstrumentContainer mInstrumentContainer;
	private MarketDataInterface mMarketDataDistributor;
	private MatchingEngineInterface mMatchingEngine;
	private TradeContainer mTradeContainer;
	private JsonObject mTeConfiguration;
	private Object mMarketDataDistributorMutex;

	private TeAppCntx() {
		mMarketDataDistributorMutex = new Object();
		setConnector(null);
		setSessionController(null);
		setInstrumentContainer(null);
		setMatchingEngine(null);
		setTeConfiguration(null);
		setMarketDataDistributor(null);
		setTradeContainer(null);
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

	public MatchingEngineInterface getMatchingEngine() {
		if (mMatchingEngine == null) {
			mLog.fatal("try to retrieve MatchingEngine implementation before setting it ", new Exception("MatchingEngine is null"));
			System.exit(-1);
		}
		return mMatchingEngine;
	}

	public void setMatchingEngine(MatchingEngineInterface pMatchingEngine) {
		mMatchingEngine = pMatchingEngine;
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
		synchronized (mMarketDataDistributorMutex) {
			if (mMarketDataDistributor != null) {
				return mMarketDataDistributor;
			}
			try {
				mMarketDataDistributorMutex.wait(5000L);
			} catch (InterruptedException e) {
			}
		}
		if (mMarketDataDistributor == null) {
			mLog.fatal("try to retrieve MarketData implementation before setting it ", new Exception("MarketData is null"));
			System.exit(-1);
		}
		return mMarketDataDistributor;
	}

	public void setMarketDataDistributor(MarketDataInterface pMarketDataDistributor) {
		synchronized (mMarketDataDistributorMutex) {
			mMarketDataDistributor = pMarketDataDistributor;
			mMarketDataDistributorMutex.notifyAll();
		}
	}

	public TradeContainer getTradeContainer() {
		if (mTradeContainer == null) {
			mLog.fatal("try to retrieve Trade Container implementation before setting it ", new Exception("TradeContainer is null"));
			System.exit(-1);
		}
		return mTradeContainer;
	}

	public void setTradeContainer(TradeContainer pTradeContainer) {
		mTradeContainer = pTradeContainer;
	}
}

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
import com.hoddmimes.te.common.db.TEDB;
import com.hoddmimes.te.common.interfaces.ConnectorInterface;
import com.hoddmimes.te.common.interfaces.MarketDataInterface;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.common.ipc.IpcService;
import com.hoddmimes.te.cryptogwy.CryptoGateway;
import com.hoddmimes.te.engine.MatchingEngineInterface;
import com.hoddmimes.te.instrumentctl.InstrumentContainer;
import com.hoddmimes.te.positions.PositionController;
import com.hoddmimes.te.sessionctl.SessionController;
import com.hoddmimes.te.trades.TradeContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class TeAppCntx {
	public static final long PRICE_MULTIPLER = 10000L;

	private static final Logger mLog = LogManager.getLogger( TeAppCntx.class );
	private static TeAppCntx mInstance;
	private ConnectorInterface  mConnector;
	private SessionController mSessionController;
	private InstrumentContainer mInstrumentContainer;
	private MarketDataInterface mMarketDataDistributor;
	private MatchingEngineInterface mMatchingEngine;
	private TradeContainer mTradeContainer;
	private PositionController mPositionController;
	private JsonObject mTeConfiguration;
	private Object mMarketDataDistributorMutex;
	private IpcService mIpcService;
	private CryptoGateway mCryptoGateway;
	private TEDB mDb;
	private boolean mTestMode;

	private TeAppCntx() {
		mMarketDataDistributorMutex = new Object();
		setConnector(null);
		setPositionController( null );
		setSessionController(null);
		setInstrumentContainer(null);
		setMatchingEngine(null);
		setTeConfiguration(null);
		setMarketDataDistributor(null);
		setTradeContainer(null);
		setCryptoGateway(null);
		setDb(null);
		mTestMode = false;
	}

	public static TeAppCntx getInstance() {
		synchronized ( TeAppCntx.class ) {
			if (mInstance == null) {
				mInstance = new TeAppCntx();
			}
			return mInstance;
		}
	}

	public void setTestMode() {
		mTestMode = true;
	}

	public boolean getTestMode() {
		return mTestMode;
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

	public void setIpcService(IpcService pIpcService)  {
		mIpcService = pIpcService;
	}

	public IpcService getIpcService() {
		if (mIpcService == null) {
			mLog.fatal("try to retrieve IPC Service implementation before setting it ", new Exception("mIpcService is null"));
			System.exit(-1);
		}
		return mIpcService;
	}

	public void setCryptoGateway(CryptoGateway pGateway)  {
		mCryptoGateway = pGateway;
	}

	public CryptoGateway getCryptoGateway() {
		if (mCryptoGateway == null) {
			mLog.fatal("try to retrieve Crypto Gateway  implementation before setting it ", new Exception("mCryptoGateway is null"));
			System.exit(-1);
		}
		return mCryptoGateway;
	}


	public void setDb( TEDB pDB ) {
		mDb = pDB;
	}

	public TEDB getDb() {
		if (mDb == null) {
			mLog.fatal("try to retrieve TE Database  implementation before setting it ", new Exception("mDb is null"));
			System.exit(-1);
		}
		return mDb;
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


	public PositionController getPositionController() {
		if (mPositionController == null) {
			mLog.fatal("try to retrieve PositionController implementation before setting it ", new Exception("PositionController is null"));
			System.exit(-1);
		}
		return mPositionController;
	}

	public void setPositionController(PositionController pPositionController) {
		mPositionController = pPositionController;
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

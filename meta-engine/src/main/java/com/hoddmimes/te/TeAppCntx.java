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
import com.hoddmimes.te.common.interfaces.TeService;
import com.hoddmimes.te.common.ipc.IpcRequestCallbackInterface;
import com.hoddmimes.te.common.ipc.IpcService;
import com.hoddmimes.te.cryptogwy.CryptoGateway;
import com.hoddmimes.te.engine.MatchingEngine;
import com.hoddmimes.te.engine.MatchingEngineInterface;
import com.hoddmimes.te.instrumentctl.InstrumentContainer;
import com.hoddmimes.te.positions.PositionController;
import com.hoddmimes.te.sessionctl.SessionController;
import com.hoddmimes.te.trades.TradeContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;


public class TeAppCntx {
	public static final long PRICE_MULTIPLER = 10000L;
	private static final Logger mLog = LogManager.getLogger( TeAppCntx.class );
	private static TeAppCntx mInstance = null;
	private ConcurrentHashMap<TeService, TeCoreService> mServices;
	private boolean mTestMode;
	private JsonObject mTeConfiguration;
	private TEDB mDatabase;
	private IpcService mIpcService;




	private TeAppCntx() {
		mServices = new ConcurrentHashMap<>();
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

	public void setTeConfiguration( JsonObject pTeConfiguration ) {
		mTeConfiguration = pTeConfiguration;
	}
	public static JsonObject getTeConfiguration() {
		TeAppCntx tInstance = TeAppCntx.getInstance();
		if (tInstance.mTeConfiguration == null) {
			mLog.fatal("try to retrieve TEConfiguration implementation before setting it ", new Exception("TEConfiguration is null"));
			System.exit(-1);
		}
		return tInstance.mTeConfiguration;
	}


	public void setDatabase( TEDB pDatabase ) { mDatabase = pDatabase; }
	public static TEDB getDatabase() {
		TeAppCntx tInstance = TeAppCntx.getInstance();
		if (tInstance.mDatabase == null) {
			mLog.fatal("try to retrieve TE Database implementation before setting it ", new Exception("mDatabase is null"));
			System.exit(-1);
		}
		return tInstance.mDatabase;
	}
	public void setIpcService( IpcService pIpcService ) { mIpcService = pIpcService; }
	public static IpcService getIpcService() {
		TeAppCntx tInstance = TeAppCntx.getInstance();
		if (tInstance.mIpcService == null) {
			mLog.fatal("try to retrieve IPC service implementation before setting it ", new Exception("mIpcService is null"));
			System.exit(-1);
		}
		return tInstance.mIpcService;
	}



	public void waitForServiceToStart( TeService pService ) {
		TeCoreService tService = null;
		while( tService == null) {
			tService = mServices.get( pService );
			try { Thread.sleep( 100L);}
			catch( InterruptedException e) {}
		}
		mServices.get( pService ).waitForService();
	}

	public void setTestMode() {
		mTestMode = true;
	}
	public boolean getTestMode() {
		return mTestMode;
	}

	public TeCoreService getService(TeService pService) {
		TeCoreService tCoreService = mServices.get( pService );
		if (tCoreService == null) {
			throw new RuntimeException("tried to access service: " + pService.name() + " before declared ");
		}
		return tCoreService;
	}

	public void registerService( TeCoreService pCoreService ) {
		mServices.put( pCoreService.getServiceId(), pCoreService );
		mIpcService.registerComponent(pCoreService.getServiceId(), 0, pCoreService);
		mLog.info("TE service [ " +pCoreService.getServiceId().name() + "] is successfully started");
	}



	public SessionCntxInterface getIntenalSessionContext() {
		SessionController tSessionController = (SessionController) mServices.get( TeService.SessionService );
		return tSessionController.getInternalSessionContext();
	}

	public static SessionController getSessionController() {
		return (SessionController) TeAppCntx.getInstance().getService( TeService.SessionService );
	}
	public static InstrumentContainer getInstrumentContainer() {
		return (InstrumentContainer) TeAppCntx.getInstance().getService( TeService.InstrumentData );
	}
	public static TradeContainer getTradeContainer() {
		return (TradeContainer) TeAppCntx.getInstance().getService( TeService.TradeData );
	}
	public static CryptoGateway getCryptoGateway() {
		return (CryptoGateway) TeAppCntx.getInstance().getService( TeService.CryptoGwy );
	}
	public static MarketDataInterface getMarketDistributor() {
		return (MarketDataInterface) TeAppCntx.getInstance().getService( TeService.MarketData );
	}
	public static MatchingEngine getMatchingEngine() {
		return (MatchingEngine) TeAppCntx.getInstance().getService( TeService.MatchingService );
	}
	public static PositionController getPositionController() {
		return (PositionController) TeAppCntx.getInstance().getService( TeService.PositionData );
	}
}

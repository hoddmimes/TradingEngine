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

package com.hoddmimes.te.trades;

import java.util.List;
import com.google.gson.JsonObject;
import com.hoddmimes.jaux.txlogger.*;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.TeCoreService;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.interfaces.TeService;
import com.hoddmimes.te.common.ipc.IpcService;
import com.hoddmimes.te.engine.InternalTrade;
import com.hoddmimes.te.engine.Order;
import com.hoddmimes.te.instrumentctl.InstrumentContainer;
import com.hoddmimes.te.instrumentctl.MarketX;
import com.hoddmimes.te.common.ipc.IpcComponentInterface;
import com.hoddmimes.te.messages.MgmtMessageRequest;
import com.hoddmimes.te.messages.SID;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import com.hoddmimes.te.positions.PositionController;
import com.hoddmimes.te.sessionctl.RequestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TradeContainer extends TeCoreService
{
	private SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private Logger mLog = LogManager.getLogger( TradeContainer.class);

	public record MarketSymbol (int market, String symbol)
	{
		public String toString() {
			return "mkt: " + market + " sym: " +symbol;
		}
	}

	private InstrumentContainer mInstrumentContainer;
	private PositionController mPositionController;
	private JsonObject mTradeContainerConfig;
	private ConcurrentHashMap<Integer, List<InternalTrade>>                        mTradesByMkt;
	private ConcurrentHashMap<String, List<InternalTrade>>                         mTradesBySid;

	private ConcurrentHashMap<Integer,ConcurrentHashMap<String, TradePriceX>>   mTradePricesMap;

	private ConcurrentHashMap<Integer,ConcurrentHashMap<String, BdxTrade>>      mBdxTrade;
	private TxLoggerWriterInterface mTradeLogger;

	public TradeContainer(JsonObject pTeConfiguration, IpcService pIpcService) {
		super( pTeConfiguration, pIpcService );
		mTradesByMkt = new ConcurrentHashMap<>();
		mTradesBySid = new ConcurrentHashMap<>();
		mTradePricesMap = new ConcurrentHashMap<>();
		mBdxTrade = new ConcurrentHashMap<>();

		mTradeContainerConfig = AuxJson.navigateObject( pTeConfiguration,"TeConfiguration/tradeContainer/configuration");
		mPositionController = (PositionController) TeAppCntx.getInstance().getService( TeService.PositionData);
		mInstrumentContainer = (InstrumentContainer) TeAppCntx.getInstance().getService( TeService.InstrumentData);

		if (!TeAppCntx.getInstance().getTestMode()) {
			loadTrades();
		}
		openTradelog();
		TeAppCntx.getInstance().registerService( this );
	}

	@Override
	public TeService getServiceId() {
		return TeService.TradeData;
	}


	private void logTrade( InternalTrade pTrade ) {
		mTradeLogger.write(pTrade.toJson().toString().getBytes(StandardCharsets.UTF_8));
	}

	private void openTradelog() {

		TxLoggerConfigInterface tTxConfig = TxLoggerFactory.getConfiguration();
		tTxConfig.setWriteStatistics( AuxJson.navigateBoolean(mTradeContainerConfig, "txStatistics"));
		tTxConfig.setSyncDisabled(AuxJson.navigateBoolean(mTradeContainerConfig, "txSyncDisabled"));
		mTradeLogger = TxLoggerFactory.getWriter(
				AuxJson.navigateString(mTradeContainerConfig, "txlogDir"),
				AuxJson.navigateString(mTradeContainerConfig, "txlogName"));


	}

	private void toTrades( InternalTrade pTrade, boolean pUpdatePosition ) {

		// Update the position holdings
		mPositionController.tradeExcution( pTrade );

		// Add trade by Market
		List<InternalTrade> tTrdLst = mTradesByMkt.get( pTrade.getMarketId());
		if (tTrdLst == null) {
			tTrdLst = new LinkedList<>();
			mTradesByMkt.put( pTrade.getMarketId(), tTrdLst);
		}
		synchronized( tTrdLst ) {
			tTrdLst.add( pTrade);
		}

		// Add trade by SID
		tTrdLst = mTradesBySid.get( pTrade.getSid());
		if (tTrdLst == null) {
			tTrdLst = new LinkedList<>();
			mTradesBySid.put( pTrade.getSid(), tTrdLst);
		}
		synchronized( tTrdLst ) {
			tTrdLst.add( pTrade);
		}
	}



	private void loadTrades() {
		int tCount = 0;
		TxLoggerReplayInterface txReplay = TxLoggerFactory.getReplayer(
				AuxJson.navigateString(mTradeContainerConfig, "txlogDir"),
				AuxJson.navigateString(mTradeContainerConfig, "txlogName"));
		TxLoggerReplayIterator tItr = txReplay.replaySync(TxLoggerReplayInterface.DIRECTION.Backward, new Date());

		try {
			while (tItr.hasMore()) {
				TxLoggerReplayEntry txEntry = tItr.next();
				String jObjectString = new String(txEntry.getData());
				InternalTrade trd = new InternalTrade(jObjectString);
				/**
				 * Note! Positions for crypto trades are maintained by the CryptoDepository which is also responsible
				 * for setting the position in the position controller. Crpto trades should not be replayed and applied
				 * on initial day positions as for 'equities'
				 */
				boolean tUpdatePosition = (!mInstrumentContainer.isCryptoMarket(trd.getSid())) ? true : false;
				toTrades(trd, tUpdatePosition);
				updateTradePrice(trd);
				tCount++;
			}
			mLog.info("loaded (" + tCount + ") trades.");
		}
		catch( Exception e) {
			mLog.fatal("failed to load trades ", e );
			System.exit(-1);
		}
	}


	public BdxTrade addTrade(InternalTrade pInternalTrade) {
		mTradeLogger.write(pInternalTrade.toJson().toString().getBytes(StandardCharsets.UTF_8));
		toTrades( pInternalTrade, true );
		updateTradePrice(  pInternalTrade );
		return updateBdxTrade( pInternalTrade );
	}

	public synchronized MessageInterface queryTradePrices(QueryTradePricesRequest pRqstMsg, RequestContext pRequestContext)
	{
		QueryTradePricesResponse tRspMsg = new QueryTradePricesResponse();
		tRspMsg.setRef( pRqstMsg.getRef().get());


		if (!mInstrumentContainer.marketDefined( pRqstMsg.getMarketId().get() )) {
			return StatusMessageBuilder.error("No such market (" + pRqstMsg.getMarketId().get() + ")", pRqstMsg.getRef().get());
		}


		if (pRqstMsg.getSid().isPresent()) {
			List<TradePrice> tTrdPrcLst = new ArrayList<>();

			Iterator<ConcurrentHashMap<String, TradePriceX>> tMktItr = mTradePricesMap.values().iterator();
			while (tMktItr.hasNext()) {
				TradePriceX trdprc = tMktItr.next().get(pRqstMsg.getSid());
				if (trdprc != null) {
					tTrdPrcLst.add(trdprc);
					break;
				}
			}
			tRspMsg.setTradePrices(tTrdPrcLst);
			return tRspMsg;
		}



		ConcurrentHashMap<String, TradePriceX> tTrdPrcMap = mTradePricesMap.get(pRqstMsg.getMarketId().get());
		Iterator<TradePriceX> tSidItr = tTrdPrcMap.values().iterator();
		while( tSidItr.hasNext() ) {
			tRspMsg.addTradePrices( tSidItr.next() );
		}
		return tRspMsg;
	}

	public synchronized QueryOwnTradesResponse queryOwnTrades( QueryOwnTradesRequest pRequest, RequestContext pRequestContext ) {
		QueryOwnTradesResponse tRspMsg = new QueryOwnTradesResponse();
		tRspMsg.setRef( pRequest.getRef().get());

		List<InternalTrade> tTrdLst = mTradesByMkt.get( pRequest.getMarketId().get());
		if (tTrdLst == null) {
			tRspMsg.setTrades( new ArrayList<>());
			return tRspMsg;
		}
		synchronized( tTrdLst ) {
			for (InternalTrade trd : tTrdLst) {
				if (trd.getBuyOrder().getAccountId().contentEquals(pRequestContext.getAccountId())) {
					OwnTrade ot = new OwnTrade();
					ot.setOrderId(Long.toHexString(trd.getBuyOrder().getOrderId()));
					ot.setPrice(trd.getPrice());
					ot.setTime(SDF.format(trd.getTradeTime()));
					ot.setSid(trd.getSid());
					ot.setTradeId(Long.toHexString(trd.getTradeNo()));
					ot.setQuantity(trd.getQuantity());
					ot.setSide(Order.Side.BUY.name());
					ot.setOrderRef(trd.getBuyOrder().getUserRef());
					tRspMsg.addTrades(ot);
				}
				if (trd.getSellOrder().getAccountId().contentEquals(pRequestContext.getAccountId())) {
					OwnTrade ot = new OwnTrade();
					ot.setOrderId(Long.toHexString(trd.getSellOrder().getOrderId()));
					ot.setPrice(trd.getPrice());
					ot.setTime(SDF.format(trd.getTradeTime()));
					ot.setSid(trd.getSid());
					ot.setTradeId(Long.toHexString(trd.getSellOrder().getOrderId()));
					ot.setQuantity(trd.getQuantity());
					ot.setSide(Order.Side.SELL.name());
					ot.setOrderRef(trd.getSellOrder().getUserRef());
					tRspMsg.addTrades(ot);
				}
			}
		}
		return tRspMsg;
	}

	private synchronized BdxTrade updateBdxTrade( InternalTrade pTrade ) {
		ConcurrentHashMap<String,BdxTrade> tTrdBdxMap = mBdxTrade.get( pTrade.getMarketId());
		if (tTrdBdxMap == null) {
			tTrdBdxMap = new ConcurrentHashMap<>();
			mBdxTrade.put( pTrade.getMarketId(), tTrdBdxMap);
		}
		BdxTrade trd = tTrdBdxMap.get( pTrade.getSid());
		if (trd == null) {
			trd = new BdxTrade();
			trd.setQuantity( pTrade.getQuantity());
			trd.setSid( pTrade.getSid());
			trd.setHigh( pTrade.getPrice());
			trd.setLow( pTrade.getPrice());
			trd.setOpen( pTrade.getPrice());
			trd.setLast( pTrade.getPrice());
			trd.setTotQuantity( pTrade.getQuantity());
		} else {
			long tPrice = pTrade.getPrice();
			trd.setTotQuantity((trd.getQuantity().get() + pTrade.getQuantity()));
			trd.setLast( tPrice );
			if (tPrice > trd.getHigh().get()) {
				trd.setHigh( tPrice );
			}
			if (tPrice < trd.getLow().get()) {
				trd.setLow( tPrice );
			}
		}
		return trd;
	}

	private synchronized void updateTradePrice(  InternalTrade pTrade ) {

		ConcurrentHashMap<String,TradePriceX> tTrdPrcMap = mTradePricesMap.get( pTrade.getMarketId());
		if (tTrdPrcMap == null) {
			tTrdPrcMap = new ConcurrentHashMap<>();
			mTradePricesMap.put( pTrade.getMarketId(), tTrdPrcMap);
		}
		TradePriceX tp = tTrdPrcMap.get( pTrade.getSid());
		if (tp == null) {
			tp = new TradePriceX( pTrade );
			tTrdPrcMap.put( pTrade.getSid(), tp);
		} else {
			tp.update( pTrade );
		}
	}


	@Override
	public MessageInterface ipcRequest(MessageInterface pMgmtRequest) {
		if (pMgmtRequest instanceof MgmtGetTradesRequest) {
			return getTradesForMgmt((MgmtMessageRequest) pMgmtRequest);
		}
		if (pMgmtRequest instanceof MgmtQueryTradeRequest) {
			return getTradeStatisticsForMgmt((MgmtQueryTradeRequest) pMgmtRequest);
		}
		throw new RuntimeException("Unknown Mgmt request : " + pMgmtRequest.getMessageName());
	}

	MgmtGetTradesResponse getTradesForMgmt(MgmtMessageRequest pMgmtRequest) {
		MgmtGetTradesResponse tRsp = new MgmtGetTradesResponse().setRef( pMgmtRequest.getRef().get());
		Iterator<List<InternalTrade>> tMktItr = mTradesByMkt.values().iterator();
		while( tMktItr.hasNext()) {
			List<InternalTrade> tTrdLst = tMktItr.next();
			synchronized ( tTrdLst) {
				tTrdLst.stream().forEach( it -> tRsp.addTrades( it.toTradeExecution()));
			}
		}
		return tRsp;
	}

	MgmtQueryTradeResponse getTradeStatisticsForMgmt(MgmtQueryTradeRequest pMgmtRequest) {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setGroupingUsed(true);
		nf.setMaximumFractionDigits(2);
		nf.setMaximumFractionDigits(2);

		MgmtQueryTradeResponse tRsp = new MgmtQueryTradeResponse().setRef( pMgmtRequest.getRef().get());

		HashMap<Integer, MgmtMarketTradeEntry> tMarketStatMap = new HashMap<>();

		Iterator<List<InternalTrade>> tSidItr = mTradesBySid.values().iterator();
		while( tSidItr.hasNext()) {
			List<InternalTrade> tTrdLst = tSidItr.next();
			int tVolume = 0, tExecutions = 0;
			long tMinPrice = Long.MAX_VALUE, tMaxPrice = 0, tAvgPrice = 0, tTurnover = 0;
			String tSid = tTrdLst.get(0).getSid();
			synchronized ( tTrdLst ) {
				for (InternalTrade trd : tTrdLst) {
					tAvgPrice = (tAvgPrice == 0) ? trd.getPrice() :
							((tVolume * tAvgPrice) + (trd.getQuantity() * trd.getPrice())) / (tVolume + trd.getQuantity());

					tVolume += trd.getQuantity();
					tTurnover += trd.getQuantity() * trd.getPrice();
					tExecutions++;
					if (trd.getPrice() < tMinPrice) {
						tMinPrice = trd.getPrice();
					}
					if (trd.getPrice() > tMaxPrice) {
						tMaxPrice = trd.getPrice();
					}
				}

				MgmtSymbolTradeEntry mst = new MgmtSymbolTradeEntry().setTrades(tExecutions).setSid(tSid).setAveragePrice(tAvgPrice)
						.setMaxPrice(tMaxPrice).setMinPrice(tMinPrice).setTurnover(tTurnover).setVolume(tVolume);
				tRsp.addSids(mst);

				SID s = new SID(tSid);
				MgmtMarketTradeEntry tMrktStat = tMarketStatMap.get(s.getMarket());
				if (tMrktStat == null) {
					MarketX tMarketX = mInstrumentContainer.getMarket( s.getMarket());
					tMrktStat = new MgmtMarketTradeEntry().setMarket( tMarketX.getName().get()).setMarketId( s.getMarket());
					tMarketStatMap.put( s.getMarket(), tMrktStat);
				}
				tMrktStat.update( tExecutions, tVolume, tTurnover );
			}
		}
		tRsp.setMarkets( tMarketStatMap.values().stream().toList() );
		return tRsp;
	}



}

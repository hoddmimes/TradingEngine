package com.hoddmimes.te.trades;

import com.google.gson.JsonObject;
import com.hoddmimes.jaux.txlogger.*;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.engine.InternalTrade;
import com.hoddmimes.te.engine.Order;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import com.hoddmimes.te.sessionctl.RequestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class TradeContainer
{
	private Logger mLog = LogManager.getLogger( TradeContainer.class);


	private JsonObject mConfiguration;
	private ConcurrentHashMap<String,TradeX> mTrades;
	private ConcurrentHashMap<String, TradePriceX> mTradePrices;
	private TxLoggerWriterInterface mTradeLogger;

	public TradeContainer( JsonObject pTeConfiguration ) {
		mConfiguration = AuxJson.navigateObject( pTeConfiguration,"TeConfiguration/tradeContainer/configuration");
		loadTrades();
		openTradelog();
		mTrades = new ConcurrentHashMap<>();
		mTradePrices = new ConcurrentHashMap<>();
		TeAppCntx.getInstance().setTradeContainer( this );
	}



	private String getLogDir() {
		return AuxJson.navigateString( mConfiguration,"txlogDir");
	}

	private void logTrade( TradeX pTrade ) {
		mTradeLogger.write( pTrade.toJson().toString().getBytes(StandardCharsets.UTF_8));
	}

	private void openTradelog() {
		mTradeLogger = TxLoggerFactory.getWriter(
				AuxJson.navigateString(mConfiguration, "txlogDir"),
				AuxJson.navigateString(mConfiguration, "txlogName"));

	}

	private void loadTrades() {
		int tCount = 0;
		TxLoggerReplayInterface txReplay = TxLoggerFactory.getReplayer(
				AuxJson.navigateString(mConfiguration, "txlogDir"),
				AuxJson.navigateString(mConfiguration, "txlogName"));
		TxLoggerReplayIterator tItr = txReplay.replaySync(TxLoggerReplayInterface.DIRECTION.Backward, new Date());

		try {
			while (tItr.hasMore()) {
				TxLoggerReplayEntry txEntry = tItr.next();
				String jObjectString = new String(txEntry.getData());
				TradeX trd = new TradeX( jObjectString );
				mTrades.put( trd.getSymbol().get(), trd );
				updateTradePrice(  trd );
				tCount++;
			}
			mLog.info("loaded (" + tCount + ") trades.");
		}
		catch( Exception e) {
			mLog.fatal("failed to load trades ", e );
			System.exit(-1);
		}
	}


	public void addTrade(InternalTrade pInternalTrade) {
		TradeX tTrade = new TradeX( pInternalTrade);
		mTradeLogger.write( tTrade.toJson().toString().getBytes(StandardCharsets.UTF_8));
		mTrades.put( pInternalTrade.getSymbol(), tTrade);
		updateTradePrice(  tTrade );
	}

	public synchronized QueryTradePricesCompactResponse queryTradePrices(QueryTradePricesRequest pRqstMsg, RequestContext pRequestContext)
	{
		QueryTradePricesCompactResponse tRsp = new QueryTradePricesCompactResponse();
		tRsp.setRef( pRqstMsg.getRef().get() );
		Iterator<TradePriceX> tItr = mTradePrices.values().iterator();
		while( tItr.hasNext() ) {
			tRsp.addPrices(tItr.next().toCompact());
		}
		return tRsp;
	}

	public synchronized QueryOwnTradesResponse queryOwnTrades( QueryOwnTradesRequest pRequest, RequestContext pRequestContext ) {
		QueryOwnTradesResponse tRspMsg = new QueryOwnTradesResponse();
		tRspMsg.setRef( pRequest.getRef().get());

		Iterator<TradeX> tItr = mTrades.values().iterator();
		while( tItr.hasNext() ) {
			TradeX trd = tItr.next();
			if (trd.getBuyer().get().contentEquals(pRequestContext.getAccountId())) {
				OwnTrade ot = new OwnTrade();
				ot.setOrderId(Long.toHexString(trd.getBuyerOrderId().get()));
				ot.setPrice( trd.getPrice().get());
				ot.setRef( pRequest.getRef().get());
				ot.setSymbol( trd.getSymbol().get());
				ot.setTradeId( Long.toHexString( trd.getTradeId().get()));
				ot.setVolume( trd.getQuantity().get());
				ot.setSide(Order.Side.BUY.name());
				tRspMsg.addTrades( ot );
			}
			if (trd.getSeller().get().contentEquals(pRequestContext.getAccountId())) {
				OwnTrade ot = new OwnTrade();
				ot.setOrderId(Long.toHexString(trd.getSellerOrderId().get()));
				ot.setPrice( trd.getPrice().get());
				ot.setRef( pRequest.getRef().get());
				ot.setSymbol( trd.getSymbol().get());
				ot.setTradeId( Long.toHexString( trd.getTradeId().get()));
				ot.setVolume( trd.getQuantity().get());
				ot.setSide(Order.Side.SELL.name());
				tRspMsg.addTrades( ot );
			}
		}
		return tRspMsg;
	}

	public synchronized QueryTradePriceResponse queryTradePrice(QueryTradePriceRequest pRqstMsg, RequestContext pRequestContext)
	{
		QueryTradePriceResponse tRsp = new QueryTradePriceResponse();
		tRsp.setRef( pRqstMsg.getRef().get() );
		TradePriceX tpx = mTradePrices.get( pRqstMsg.getSymbol().get());
		if (tpx != null) {
			tRsp.setPrices( tpx );
		}
		return tRsp;
	}

	private synchronized void updateTradePrice(  TradeX pTrade ) {
		TradePriceX tp = mTradePrices.get( pTrade.getSymbol());
		if (tp == null) {
			tp = new TradePriceX( pTrade );
			mTradePrices.put( pTrade.getSymbol().get(), tp);
		} else {
			tp.update( pTrade );
		}
	}






}

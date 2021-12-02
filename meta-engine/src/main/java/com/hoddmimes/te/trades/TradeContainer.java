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

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class TradeContainer
{
	private SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private Logger mLog = LogManager.getLogger( TradeContainer.class);

	public record MarketSymbol (int market, String symbol)
	{
		public String toString() {
			return "mkt: " + market + " sym: " +symbol;
		}
	}

	private JsonObject mConfiguration;
	private ConcurrentHashMap<Integer,ConcurrentHashMap<String,TradeX>>         mTrades;
	private ConcurrentHashMap<Integer,ConcurrentHashMap<String, TradePriceX>>   mTradePrices;
	private ConcurrentHashMap<Integer,ConcurrentHashMap<String, BdxTrade>>      mBdxTrade;
	private TxLoggerWriterInterface mTradeLogger;

	public TradeContainer( JsonObject pTeConfiguration ) {
		mTrades = new ConcurrentHashMap<>();
		mTradePrices = new ConcurrentHashMap<>();
		mBdxTrade = new ConcurrentHashMap<>();

		mConfiguration = AuxJson.navigateObject( pTeConfiguration,"TeConfiguration/tradeContainer/configuration");
		loadTrades();
		openTradelog();

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

	private void toTrades( TradeX pTrade ) {
		ConcurrentHashMap<String, TradeX> tTrdMap = mTrades.get( pTrade.getMarketId());
		if (tTrdMap == null) {
			tTrdMap = new ConcurrentHashMap<>();
			mTrades.put( pTrade.getMarketId().get(), tTrdMap);
		}
		tTrdMap.put( pTrade.getSid().get(), pTrade);
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
				toTrades( trd );
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


	public BdxTrade addTrade(InternalTrade pInternalTrade) {
		TradeX tTrade = new TradeX( pInternalTrade);
		mTradeLogger.write( tTrade.toJson().toString().getBytes(StandardCharsets.UTF_8));
		toTrades( tTrade );
		updateTradePrice(  tTrade );
		return updateBdxTrade( tTrade );
	}

	public synchronized MessageInterface queryTradePrices(QueryTradePricesRequest pRqstMsg, RequestContext pRequestContext)
	{
		QueryTradePricesResponse tRspMsg = new QueryTradePricesResponse();
		tRspMsg.setRef( pRqstMsg.getRef().get());


		if (!TeAppCntx.getInstance().getInstrumentContainer().marketDefined( pRqstMsg.getMarketId().get() )) {
			return StatusMessageBuilder.error("No such market (" + pRqstMsg.getMarketId().get() + ")", pRqstMsg.getRef().get());
		}

		ConcurrentHashMap<String,TradePriceX> tTrdPrcMap = mTradePrices.get( pRqstMsg.getMarketId().get());
		if (tTrdPrcMap == null) {
			tRspMsg.setTradePrices( new ArrayList<TradePrice>());
			return tRspMsg;
		}

		Iterator<TradePriceX> tItr = tTrdPrcMap.values().iterator();
		while( tItr.hasNext() ) {
			TradePriceX tp = tItr.next();
			if (pRqstMsg.getSid().isEmpty() || (pRqstMsg.getSid().get().contentEquals( tp.getSid().get()))) {
				tRspMsg.addTradePrices( tp );
			}
		}
		return tRspMsg;
	}

	public synchronized QueryOwnTradesResponse queryOwnTrades( QueryOwnTradesRequest pRequest, RequestContext pRequestContext ) {
		QueryOwnTradesResponse tRspMsg = new QueryOwnTradesResponse();
		tRspMsg.setRef( pRequest.getRef().get());

		ConcurrentHashMap<String,TradeX> tTrdMktMap = mTrades.get( pRequest.getMarketId().get());
		if (tTrdMktMap == null) {
			return tRspMsg;
		}
		Iterator<TradeX> tItr = tTrdMktMap.values().iterator();
		while( tItr.hasNext() ) {
			TradeX trd = tItr.next();
			if (trd.getBuyer().get().contentEquals(pRequestContext.getAccountId())) {
				OwnTrade ot = new OwnTrade();
				ot.setOrderId(Long.toHexString(trd.getBuyerOrderId().get()));
				ot.setPrice( trd.getPrice().get());
				ot.setTime( SDF.format( trd.getTradeTime().get()));
				ot.setSid( trd.getSid().get());
				ot.setTradeId( Long.toHexString( trd.getTradeId().get()));
				ot.setQuantity( trd.getQuantity().get());
				ot.setSide(Order.Side.BUY.name());
				ot.setOrderRef( trd.getBuyerOrderRef().get());
				tRspMsg.addTrades( ot );
			}
			if (trd.getSeller().get().contentEquals(pRequestContext.getAccountId())) {
				OwnTrade ot = new OwnTrade();
				ot.setOrderId(Long.toHexString(trd.getSellerOrderId().get()));
				ot.setPrice( trd.getPrice().get());
				ot.setTime( SDF.format( trd.getTradeTime().get()));
				ot.setSid( trd.getSid().get());
				ot.setTradeId( Long.toHexString( trd.getTradeId().get()));
				ot.setQuantity( trd.getQuantity().get());
				ot.setSide(Order.Side.SELL.name());
				ot.setOrderRef( trd.getSellerOrderRef().get());
				tRspMsg.addTrades( ot );
			}
		}
		return tRspMsg;
	}

	private synchronized BdxTrade updateBdxTrade( TradeX pTrade ) {
		ConcurrentHashMap<String,BdxTrade> tTrdBdxMap = mBdxTrade.get( pTrade.getMarketId());
		if (tTrdBdxMap == null) {
			tTrdBdxMap = new ConcurrentHashMap<>();
			mBdxTrade.put( pTrade.getMarketId().get(), tTrdBdxMap);
		}
		BdxTrade trd = tTrdBdxMap.get( pTrade.getSid());
		if (trd == null) {
			trd = new BdxTrade();
			trd.setQuantity( pTrade.getQuantity().get());
			trd.setSid( pTrade.getSid().get());
			trd.setHigh( pTrade.getPrice().get());
			trd.setLow( pTrade.getPrice().get());
			trd.setOpen( pTrade.getPrice().get());
			trd.setLast( pTrade.getPrice().get());
			trd.setTotQuantity( pTrade.getQuantity().get());
		} else {
			double tPrice = pTrade.getPrice().get();
			trd.setTotQuantity((trd.getQuantity().get() + pTrade.getQuantity().get()));
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

	private synchronized void updateTradePrice(  TradeX pTrade ) {

		ConcurrentHashMap<String,TradePriceX> tTrdPrcMap = mTradePrices.get( pTrade.getMarketId());
		if (tTrdPrcMap == null) {
			tTrdPrcMap = new ConcurrentHashMap<>();
			mTradePrices.put( pTrade.getMarketId().get(), tTrdPrcMap);
		}
		TradePriceX tp = tTrdPrcMap.get( pTrade.getSid());
		if (tp == null) {
			tp = new TradePriceX( pTrade );
			tTrdPrcMap.put( pTrade.getSid().get(), tp);
		} else {
			tp.update( pTrade );
		}
	}




}

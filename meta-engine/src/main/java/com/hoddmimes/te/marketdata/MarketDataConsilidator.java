package com.hoddmimes.te.marketdata;

import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.messages.generated.*;
import com.hoddmimes.te.sessionctl.SessionController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class MarketDataConsilidator extends Thread {
	private final Logger mLog = LogManager.getLogger(MarketDataConsilidator.class);
	private final long mInterval;
	private final int mLevels;
	private AtomicReference<ConcurrentHashMap<String, String>> mSymolTouchedRef ;
	private final ConcurrentHashMap<String, BdxPriceLevel> mSymolPriceLevels;


	MarketDataConsilidator(int pLevels, long pInterval) {
		mInterval = pInterval;
		mLevels = pLevels;
		mSymolTouchedRef = new AtomicReference<>(new ConcurrentHashMap<>());
		mSymolPriceLevels = new ConcurrentHashMap<>();
		this.start();
	}

	void touch(String pSymbol) {
		mSymolTouchedRef.get().put(pSymbol, pSymbol);
	}

	private void update(InternalPriceLevelResponse pRspMsg, String pSymbol) {
		BdxPriceLevel tCurrValue = mSymolPriceLevels.get(pSymbol);
		if ((tCurrValue != null) && (tCurrValue.same(pRspMsg.getBdxPriceLevel().get()))) {
			return;
		}
		mSymolPriceLevels.put(pSymbol, pRspMsg.getBdxPriceLevel().get());
		TeAppCntx.getInstance().getMarketDataDistributor().queueBdxPublic(pRspMsg.getBdxPriceLevel().get());
	}

	public QueryPriceLevelsResponse queryPriceLevels( QueryPriceLevelsRequest pQryRqst ) {
		QueryPriceLevelsResponse tRsp = new QueryPriceLevelsResponse();
		tRsp.setRef( pQryRqst.getRef().get());
		Iterator<BdxPriceLevel> tItr = mSymolPriceLevels.values().iterator();
		while( tItr.hasNext() ) {
			BdxPriceLevel tBdx = tItr.next();
			PriceLevelSymbol pls = new PriceLevelSymbol().setSymbol( tBdx.getSymbol().get());
			pls.setBuySide( tBdx.getBuySide().get());
			pls.setSellSide( tBdx.getSellSide().get());
			tRsp.addOrderbooks( pls );
		}
		return tRsp;
	}

	@Override
	public void run() {
		setName("MarketData Consilidator " + mLevels + ":" + mInterval);
		while (true) {
			try {
				Thread.sleep(mInterval);
			} catch (InterruptedException e) {
			}

			if (mSymolTouchedRef.get().size() > 0) {
				SessionController tSessionController = TeAppCntx.getInstance().getSessionController();

				ConcurrentHashMap<String, String> tNewSymbolMap = new ConcurrentHashMap<>();
				ConcurrentHashMap<String, String> tOldSymbolMap = mSymolTouchedRef.getAndSet(tNewSymbolMap);

				for (String tSymbol : tOldSymbolMap.values()) {
					InternalPriceLevelRequest tRqst = new InternalPriceLevelRequest();
					tRqst.setRef("X");
					tRqst.setSymbol(tSymbol);
					tRqst.setLevels(mLevels);

					MessageInterface tRspMsg = tSessionController.connectorMessage( SessionController.INTERNAL_SESSION_ID, tRqst.toString() );

					if (tRspMsg instanceof StatusMessage) {
						StatusMessage tSts = (StatusMessage) tRspMsg;
						mLog.error("InternalPriceLevelRequest failed \"" + tSymbol + "\" reason: " + tSts.getStatusMessage().orElse("unknown"));
					} else if (tRspMsg instanceof InternalPriceLevelResponse) {
						update((InternalPriceLevelResponse) tRspMsg, tSymbol);
					}
				}
			}
		}
	}
}



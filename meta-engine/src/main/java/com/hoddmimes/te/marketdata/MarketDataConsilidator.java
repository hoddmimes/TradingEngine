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

public class MarketDataConsilidator extends Thread
{
	private final Logger mLog = LogManager.getLogger(MarketDataConsilidator.class);
	private final long mInterval;
	private final int mLevels;
	private AtomicReference<ConcurrentHashMap<String, String>>        mTouchedRef ;
	private final ConcurrentHashMap<String, BdxPriceLevel>            mPriceLevels;



	MarketDataConsilidator(int pLevels, long pInterval) {
		mInterval = pInterval;
		mLevels = pLevels;
		mTouchedRef = new AtomicReference<>(new ConcurrentHashMap<>());
		mPriceLevels = new ConcurrentHashMap<>();
		this.start();
	}



	void touch(String pSid) {
		mTouchedRef.get().put(pSid, pSid);
	}

	private void update(InternalPriceLevelResponse pRspMsg, String pSid ) {
		BdxPriceLevel tCurrValue = mPriceLevels.get( pSid );
		if ((tCurrValue != null) && (tCurrValue.same(pRspMsg.getBdxPriceLevel().get()))) {
			return;
		}

		mPriceLevels.put( pSid, pRspMsg.getBdxPriceLevel().get());
		TeAppCntx.getInstance().getMarketDataDistributor().queueBdxPublic(pRspMsg.getBdxPriceLevel().get());
	}

	public QueryPriceLevelsResponse queryPriceLevels( QueryPriceLevelsRequest pQryRqst ) {
		QueryPriceLevelsResponse tRsp = new QueryPriceLevelsResponse();
		tRsp.setRef( pQryRqst.getRef().get());

		Iterator<BdxPriceLevel> tItr = mPriceLevels.values().iterator();
		while( tItr.hasNext() ) {
			BdxPriceLevel tBdx = tItr.next();
			PriceLevelSymbol pls = new PriceLevelSymbol().setSid( tBdx.getSid().get());
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

			if (mTouchedRef.get().size() > 0) {
				SessionController tSessionController = TeAppCntx.getInstance().getSessionController();

				ConcurrentHashMap<String, String> tNewTouchMap = new ConcurrentHashMap<>();
				ConcurrentHashMap<String, String> tOldSymbolMap = mTouchedRef.getAndSet(tNewTouchMap);

				for (String tSid : tOldSymbolMap.values()) {
					InternalPriceLevelRequest tRqst = new InternalPriceLevelRequest();
					tRqst.setRef("X");
					tRqst.setSid(tSid);
					tRqst.setLevels(mLevels);

					MessageInterface tRspMsg = tSessionController.connectorMessage( SessionController.INTERNAL_SESSION_ID, tRqst.toString() );

					if (tRspMsg instanceof StatusMessage) {
						StatusMessage tSts = (StatusMessage) tRspMsg;
						mLog.error("InternalPriceLevelRequest failed \"" + tSid + "\" reason: " + tSts.getStatusMessage().orElse("unknown"));
					} else if (tRspMsg instanceof InternalPriceLevelResponse) {
						update((InternalPriceLevelResponse) tRspMsg, tSid);
					}
				}
			}
		}
	}
}



/*
 * Copyright (c)  Hoddmimes Solution AB 2022.
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

package com.hoddmimes.te.cryptogwy;

import com.google.gson.JsonObject;
import com.hoddmimes.te.common.db.CryptoDepositUpdateEvent;
import com.hoddmimes.te.common.db.TEDB;
import com.hoddmimes.te.messages.DbCryptoDeposit;
import com.hoddmimes.te.messages.generated.DbCryptoPayment;
import com.hoddmimes.te.messages.generated.DbCryptoPaymentEntry;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class CryptoDepository extends Thread implements CryptoDepositReadOnly
{

	private TEDB mDb;
	private JsonObject mTeConfiguration;
	private BlockingQueue<CryptoDepositUpdateEvent> mEventQueue;

	public CryptoDepository(JsonObject pTeConfiguration, TEDB pDb) {
		mTeConfiguration = pTeConfiguration;
		mDb = pDb;
		mEventQueue = new LinkedBlockingQueue<>();
		start();
	}

	public synchronized void directPositionAdjustment( String pAccountId, String pSid, long pDeltaAmountNormalized ) {
		DbCryptoDeposit tCryptoDeposit = mDb.findDbCryptoDepositByAccountId( pAccountId );
		tCryptoDeposit.directUpdate( pSid, pDeltaAmountNormalized);
		mDb.updateDbCryptoDeposit( tCryptoDeposit, false);
	}


	public synchronized void set( String pAccountId, String pSid, long pQuantityNormalized ) {
		DbCryptoDeposit tDbCryptoDeposit =  mDb.findDbCryptoDepositByAccountId( pAccountId );
		if (tDbCryptoDeposit == null) {
			tDbCryptoDeposit = new DbCryptoDeposit().setAccountId( pAccountId );
		}
		tDbCryptoDeposit.set( pSid, pQuantityNormalized);
		mDb.updateDbCryptoDeposit( tDbCryptoDeposit, true);
	}

	 public synchronized boolean teUpdate( String pAccountId, String pSid, long pQuantityNormalized, long pTeTradeSeqno ) {
		DbCryptoDeposit tDbCryptoDeposit =  mDb.findDbCryptoDepositByAccountId( pAccountId );
		if (tDbCryptoDeposit == null) {
			tDbCryptoDeposit = new DbCryptoDeposit().setAccountId( pAccountId );
		}
		if (tDbCryptoDeposit.teUpdate( pSid, pQuantityNormalized, pTeTradeSeqno)) {
			mDb.updateDbCryptoDeposit( tDbCryptoDeposit, true);
			return true;
		}
		return false;
	}

	public synchronized boolean networkUpdate( String pAccountId, String pSid, long pQuantityNormalized, String pTxid ) {
		DbCryptoDeposit tDbCryptoDeposit =  mDb.findDbCryptoDepositByAccountId( pAccountId );
		if (tDbCryptoDeposit == null) {
			tDbCryptoDeposit = new DbCryptoDeposit().setAccountId( pAccountId );
		}

		if (tDbCryptoDeposit.networkUpdate( pSid, pQuantityNormalized, pTxid)) {
			mDb.updateDbCryptoDeposit(tDbCryptoDeposit, true);
			return true;
		}
		return false;
	}

	@Override
	public List<DbCryptoPaymentEntry> getCryptoCryptoPaymentEntries() {
		return mDb.findAllDbCryptoPaymentEntry();
	}

	public List<DbCryptoPaymentEntry> getPaymentEntries(String pAccountId ) {
		return mDb.findPaymentEntryByAccountId(pAccountId);
	}

	@Override
	public synchronized List<DbCryptoDeposit> getCryptoPositions() {
		return mDb.findAllDbCryptoDeposit();
	}

	@Override
	public synchronized List<DbCryptoPayment> getCryptoPayments() {
		return mDb.findAllDbCryptoPayment();
	}

	@Override
	public synchronized long getCryptoHolding( String pAccountId, String pSid ) {
		DbCryptoDeposit tDeposit = mDb.findDbCryptoDepositByAccountId( pAccountId );
		return (tDeposit == null) ? 0L : tDeposit.getHolding( pSid );
	}



	@Override
	 public boolean checkHoldingsForRedraw( String pAccountId, String pSid, long pQuantityNormalized) {
		if (getCryptoHolding( pAccountId, pSid) < pQuantityNormalized) {
			return false;
		}
		return true;
	}


	public void update( CryptoDepositUpdateEvent tUpdateEvent ) {
		if (tUpdateEvent == null) {
			return;
		}

		if (tUpdateEvent.getUpdateType() == CryptoDepositUpdateEvent.UpdateType.TeUpdate) {
			teUpdate( tUpdateEvent.getAccountId(), tUpdateEvent.getSid(), tUpdateEvent.getDeltaQuantityNormalized(), tUpdateEvent.getTeTradeSeqno());
		} else {
			networkUpdate(tUpdateEvent.getAccountId(), tUpdateEvent.getSid(), tUpdateEvent.getDeltaQuantityNormalized(), tUpdateEvent.getTxid());
		}
	}

	public void queueCryptoUpdate( CryptoDepositUpdateEvent pCryptoDepositUpdateEvent ) {
		try { mEventQueue.put( pCryptoDepositUpdateEvent ); }
		catch( InterruptedException e) { e.printStackTrace();}
	}



	public void run() {
		CryptoDepositUpdateEvent tUpdateEvent;

		setName("CryptoDepositUpdateThread");
		while( true ) {
			tUpdateEvent = null;
			try { tUpdateEvent = mEventQueue.take(); }
			catch( InterruptedException e) {}

			if (tUpdateEvent != null) {
				update( tUpdateEvent );
			}
		}
	}

}

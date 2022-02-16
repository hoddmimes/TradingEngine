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

package com.hoddmimes.te.common.db;

import com.hoddmimes.te.messages.generated.*;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.beans.PropertyAccessException;

import java.util.ArrayList;
import java.util.List;

public class TEDB extends MongoAux
{
	private static final long CONFIRM_EXPIRATION_INTERVAL = (86400L * 2L * 1000L); // 48 hrs
	public static enum CoinType {BTC, ETH};
	public static enum ActionType {DEPOSIT, REDRAW};
	public static enum StateType {CONFIRM, PENDING};
	public static enum ConfirmationType {ACCOUNT, PAYMENT};

	private static final Logger cLog = LogManager.getLogger(TEDB.class);

	public TEDB(String pDbName, String pDbHost, int pDbPort) {
		super(pDbName, pDbHost, pDbPort);
	}


	public List<DbCryptoPaymentEntry> findPaymentEntryByConfirmationId(String pConfirmationId) {
		List<DbCryptoPaymentEntry> tResult = new ArrayList<>();

		Bson tKeyFilter = Filters.eq("confirmationId", pConfirmationId);

		return super.findDbCryptoPaymentEntry( tKeyFilter );
	}

	public List<DbCryptoPayment> findDbCryptoPaymentByAccountId( String pAccountId ) {
		Bson tFilter = Filters.eq("accountId", pAccountId);
		return super.findDbCryptoPayment( tFilter );
	}

	public List<DbCryptoPaymentEntry> findPaymentEntryByAccountId(String pAccountId) {
		List<DbCryptoPaymentEntry> tResult = new ArrayList<>();

		Bson tKeyFilter = Filters.eq("accountId", pAccountId);
		return super.findDbCryptoPaymentEntry( tKeyFilter );
	}


	public List<DbCryptoPaymentEntry> findPaymentEntryByAddressAndCoinType( String pAddress, String pCoinType ) {
		Bson tFilter = Filters.and(Filters.eq("address", pAddress),
				       Filters.eq("coinType", pCoinType ));

		return super.findDbCryptoPaymentEntry( tFilter );
	}



	public void cleanConfirmations() {
		long tNow = System.currentTimeMillis();
		List<DbConfirmation> tConfirmations = super.findAllDbConfirmation();
		for (DbConfirmation tConfirmation : tConfirmations) {
			if ((tConfirmation.getBinTime().get() + CONFIRM_EXPIRATION_INTERVAL) < tNow) {
				if (super.deleteDbConfirmationByConfirmationId(tConfirmation.getConfirmationId().get()) == 0) {
					cLog.warn("failed to clea (delete) confirmation : " + tConfirmation.toJson().toString());
				} else {
					cLog.info("clean (deleted) expired confirmation : " + tConfirmation.toJson().toString());
				}
				if (tConfirmation.getConfirmationType().get().contentEquals(ConfirmationType.ACCOUNT.name())) {
					if (super.deleteAccountByAccountId(tConfirmation.getAccount().get()) == 0) {
						cLog.warn("failed to clean (delete) unconfirmed account : " + tConfirmation.getAccount().get());
					} else {
						cLog.info("clean (deleted) expired account : " + tConfirmation.getAccount().get());
					}
				}
				if (tConfirmation.getConfirmationType().get().contentEquals(ConfirmationType.PAYMENT.name())) {
					Bson tKeyFilter = Filters.and(Filters.eq("confirmationId", tConfirmation.getConfirmationId().get()),
							Filters.eq("mConfirmed", false));

					if (super.deleteDbCryptoPaymentEntry(tKeyFilter) == 0) {
						cLog.warn("failed to clean (delete) unconfirmed payment entry : " + tConfirmation.toJson().toString());
					} else {
						cLog.info("clean (deleted) expired payment entry : " + tConfirmation.toJson().toString());
					}
				}
			}
		}
	}

	public boolean  confirmPaymentEntry(String pAccountId, String pConfirmationId) {
		Bson tFilter = Filters.and(Filters.eq("confirmationId", pConfirmationId),
				                   Filters.eq("mAccountId", pAccountId ));
		List<DbCryptoPaymentEntry> tPaymentEtries = super.findDbCryptoPaymentEntry( tFilter  );
		if (tPaymentEtries.size() != 1) {
			cLog.warn("failed to locate unconfirmed payment entries accountId : " + pAccountId + " confirmationId: " + pConfirmationId);
			return false;
		}
		DbCryptoPaymentEntry tCryptoPaymentEntry = tPaymentEtries.get(0);
		tCryptoPaymentEntry.setConfirmed( true );
		UpdateResult tUpdResult = super.updateDbCryptoPaymentEntry( tCryptoPaymentEntry, false);
		return (tUpdResult.getModifiedCount() > 0) ? true : false;
	}

	public boolean  confirmAccount( String pAccountId ) {
		List<Account> tAccounts = super.findAccount( pAccountId );
		if (tAccounts.size() != 1) {
			cLog.warn("failed to locate unconfirmed account : " + pAccountId);
			return false;
		}
		Account tAccount = tAccounts.get(0);
		tAccount.setConfirmed( true );
		UpdateResult tUpdResult = super.updateAccount( tAccount, false);
		return (tUpdResult.getModifiedCount() > 0) ? true : false;
	}


	public void updateAccountCryptoDeposit( String pAccount, String pCoinType, long pAmount ) {
		DbCryptoDeposit tDeposit = (DbCryptoDeposit) dbEntryFound(super.findDbCryptoDepositByAccountId(pAccount));
		if (tDeposit == null) {
			tDeposit = new DbCryptoDeposit().setAccountId(pAccount);
		}
		DbDepositHolding tHolding = tDeposit.findHolding(pCoinType);
		if (tHolding == null) {
			tHolding = new DbDepositHolding().setSid(pCoinType);
		}
		tHolding.setHolding(pAmount);

		super.updateDbCryptoDeposit( tDeposit, true);
	}



	public static Object dbEntryFound( List<?> pObjects ) {
		if ((pObjects == null) || (pObjects.size() != 1)) {
			return null;
		}
		return pObjects.get(0);
	}
}

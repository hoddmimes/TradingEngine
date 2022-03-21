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

import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.Crypto;
import com.hoddmimes.te.common.TXIDFactory;
import com.hoddmimes.te.common.db.TEDB;
import com.hoddmimes.te.instrumentctl.SymbolX;
import com.hoddmimes.te.messages.generated.DbCryptoPayment;

import java.text.SimpleDateFormat;
import java.util.UUID;

public abstract class CoinGateway implements CoinGatewayInterface
{
	protected    SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	protected   TEDB        mDb;
	protected   SymbolX     mCoinInst;

	CoinGateway(TEDB pDatabase, Crypto.CoinType pCoinType ) {
		mDb = pDatabase;
		mCoinInst = TeAppCntx.getInstance().getInstrumentContainer().getCryptoInstrument( pCoinType.name());
	}


	protected static String getConfirmationId() {
		return  String.valueOf(TXIDFactory.getId()) + "-" + UUID.randomUUID().toString();
	}

	protected DbCryptoPayment dbCreatePaymentEntry(Crypto.CoinType pCoinType, String pAccountId, String pTxid, String pAddress, String pFriendlyAmount, Crypto.StateType pStateType, Crypto.PaymentType pPaymentType) {
		DbCryptoPayment tPayment = new DbCryptoPayment();
		if (pAccountId != null) {
			tPayment.setAccountId(pAccountId);
		}
		tPayment.setCoinType(pCoinType.name());
		tPayment.setPaymentType( pPaymentType.name() );
		tPayment.setTxid( pTxid );
		tPayment.setAddress( pAddress );
		tPayment.setTime( SDF.format( System.currentTimeMillis()));
		tPayment.setState( pStateType.name() );
		tPayment.setAmount( pFriendlyAmount );
		mDb.updateDbCryptoPayment( tPayment, true);
		return tPayment;
	}
}

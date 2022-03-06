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

import com.hoddmimes.te.messages.DbCryptoDeposit;
import com.hoddmimes.te.messages.generated.DbCryptoPayment;
import com.hoddmimes.te.messages.generated.DbCryptoPaymentEntry;

import java.util.List;

public interface CryptoDepositReadOnly
{
	public List<DbCryptoDeposit> getCryptoPositions();
	public List<DbCryptoPayment> getCryptoPayments();

	public List<DbCryptoPaymentEntry> getCryptoCryptoPaymentEntries();
	public List<DbCryptoPaymentEntry> getPaymentEntries(String pAccountId );



	public long getCryptoHolding( String pAccountId, String pSid );
	public boolean checkHoldingsForRedraw( String pAccountId, String pSid, long pQuantityNormalized);

}

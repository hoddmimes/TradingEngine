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

import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.TeException;
import com.hoddmimes.te.messages.generated.CryptoRedrawRequest;
import com.hoddmimes.te.messages.generated.GetDepositEntryRequest;
import com.hoddmimes.te.messages.generated.SetRedrawEntryRequest;

import java.io.IOException;

public interface CoinGatewayInterface
{
	public MessageInterface addDepositEntry(GetDepositEntryRequest pPaymentEntryRequest );
	public MessageInterface addRedrawEntry(SetRedrawEntryRequest pPaymentEntryRequest, boolean pAutoConfirm );
	public String sendCoins( CryptoRedrawRequest pRedrawRqst ) throws TeException;
	public long getEstimatedTxFeeNA();
}

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

import com.hoddmimes.te.messages.generated.TradePrice;


import java.text.SimpleDateFormat;

public class TradePriceX extends TradePrice
{
	private static  SimpleDateFormat SDF_TIME = new SimpleDateFormat("HH:mm:ss.SSS");

	public TradePriceX( TradeX pTrade ) {
		super();
		super.setQuantity( pTrade.getQuantity().get());
		super.setSid( pTrade.getSid().get());
		super.setTime( SDF_TIME.format( pTrade.getTradeTime().get()));
		super.setLow( pTrade.getPrice().get());
		super.setHigh( pTrade.getPrice().get());
		super.setOpen( pTrade.getPrice().get());
		super.setLast( pTrade.getPrice().get());
	};

	public void update( TradeX pTrade ) {
		super.setQuantity( (super.getQuantity().get() + pTrade.getQuantity().get()));
		super.setLast( pTrade.getPrice().get());
		super.setTime( SDF_TIME.format( pTrade.getTradeTime()));
		if (pTrade.getPrice().get() < super.getLow().get()) {
			super.setLow(pTrade.getPrice().get());
		}
		if (pTrade.getPrice().get() > super.getHigh().get()) {
			super.setHigh(pTrade.getPrice().get());
		}
	}
}

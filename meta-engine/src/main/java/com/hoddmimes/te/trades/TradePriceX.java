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

import com.hoddmimes.te.engine.InternalTrade;
import com.hoddmimes.te.messages.generated.TradePrice;


import java.text.SimpleDateFormat;

public class TradePriceX extends TradePrice
{
	private static  SimpleDateFormat SDF_TIME = new SimpleDateFormat("HH:mm:ss.SSS");

	public TradePriceX( InternalTrade pTrade ) {
		super();
		super.setQuantity( pTrade.getQuantity());
		super.setSid( pTrade.getSid());
		super.setTime( SDF_TIME.format( pTrade.getTradeTime()));
		super.setLow( pTrade.getPrice());
		super.setHigh( pTrade.getPrice());
		super.setOpen( pTrade.getPrice());
		super.setLast( pTrade.getPrice());
	};

	public void update( InternalTrade pTrade ) {
		super.setQuantity( (super.getQuantity().get() + pTrade.getQuantity()));
		super.setLast( pTrade.getPrice());
		super.setTime( SDF_TIME.format( pTrade.getTradeTime()));
		if (pTrade.getPrice() < super.getLow().get()) {
			super.setLow(pTrade.getPrice());
		}
		if (pTrade.getPrice() > super.getHigh().get()) {
			super.setHigh(pTrade.getPrice());
		}
	}
}

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
import com.hoddmimes.te.messages.generated.ContainerTrade;

import java.nio.charset.StandardCharsets;

public class TradeX extends ContainerTrade
{
	public TradeX(InternalTrade pInternalTrade) {
		this.setSid(pInternalTrade.getSid());
		this.setMarketId(pInternalTrade.getMarketId());
		this.setBuyer(pInternalTrade.getBuyOrder().getAccountId());
		this.setSeller( pInternalTrade.getSellOrder().getAccountId());
		this.setBuyerOrderId(pInternalTrade.getBuyOrder().getOrderId());
		this.setSellerOrderId( pInternalTrade.getSellOrder().getOrderId());
		this.setPrice(pInternalTrade.getPrice());
		this.setQuantity(pInternalTrade.getQuantity());
		this.setTradeId(pInternalTrade.getTradeNo());
		this.setTradeTime(pInternalTrade.getTradeTime());
		this.setSellerOrderRef( pInternalTrade.getSellOrder().getUserRef());
		this.setBuyerOrderRef( pInternalTrade.getBuyOrder().getUserRef());
	}

	public TradeX( String pJsonString ) {
		super( pJsonString );
	}
}

package com.hoddmimes.te.trades;

import com.hoddmimes.te.engine.InternalTrade;
import com.hoddmimes.te.messages.generated.ContainerTrade;

public class TradeX extends ContainerTrade
{
	public TradeX(InternalTrade pInternalTrade) {
		this.setSymbol(pInternalTrade.getSymbol());
		this.setBuyer(pInternalTrade.getBuyOrder().getAccountId());
		this.setSeller( pInternalTrade.getSellOrder().getAccountId());
		this.setBuyerOrderId(pInternalTrade.getBuyOrder().getOrderId());
		this.setSellerOrderId( pInternalTrade.getSellOrder().getOrderId());
		this.setPrice(pInternalTrade.getPrice());
		this.setQuantity(pInternalTrade.getQuantity());
		this.setTradeId(pInternalTrade.getTradeNo());
		this.setTradeTime(pInternalTrade.getTradeTime());
	}

	public TradeX( String pJsonString ) {
		super( pJsonString );
	}
}

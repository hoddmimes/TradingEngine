package com.hoddmimes.te.trades;

import com.hoddmimes.te.messages.generated.TradePrice;
import com.hoddmimes.te.messages.generated.TradePriceCompact;

import java.text.SimpleDateFormat;

public class TradePriceX extends TradePrice
{
	private static  SimpleDateFormat SDF_TIME = new SimpleDateFormat("HH:mm:ss.SSS");

	public TradePriceX( TradeX pTrade ) {
		super();
		super.setQuantity( pTrade.getQuantity().get());
		super.setSymbol( pTrade.getSymbol().get());
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

	public TradePriceCompact toCompact() {
		TradePriceCompact tpc = new TradePriceCompact();
		tpc.setHi( super.getHigh().get());
		tpc.setLo( super.getLow().get());
		tpc.setL( super.getLast().get());
		tpc.setQ( super.getQuantity().get());
		tpc.setS( super.getSymbol().get());
		tpc.setT( super.getTime().get());
		return tpc;
	}
}

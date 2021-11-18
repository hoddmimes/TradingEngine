package com.hoddmimes.te.instrumentctl;

import com.google.gson.JsonObject;
import com.hoddmimes.te.messages.generated.Symbol;

import java.math.BigDecimal;

public class SymbolX extends Symbol
{


	SymbolX(JsonObject pJsonObject) {
		super( pJsonObject.toString() );
	}

	public String getId() {
		return super.getSymbol().get();
	}


	private boolean isTickSizeAligned( double pPrice ) {
		if (super.getTickSize().get() == 0) {
			return true;
		}
		BigDecimal tPrice = new BigDecimal( Double.toString( pPrice));
		double  r = tPrice.subtract(tPrice.divideToIntegralValue(new BigDecimal("1.0"))).doubleValue();

		int x = (int) (r * 1000.0d);
		int y = (int) (super.getTickSize().get() * 1000.0d);

		return ((x % y) == 0);
	}

	public boolean isEnabled() {
		return super.getEnabled().get();
	}

	public void validate( double pPrice, double pLastKnownTradingPrice ) throws Exception {
		// Validate that price is a multiple of the tick size
		if (!isTickSizeAligned( pPrice )) {
			throw new Exception("order price is not tick size aligned");
		}

		if ((super.getMinPricePctChg().get() != 0.0d) && (pLastKnownTradingPrice != 0)) {
			double p = (super.getMinPricePctChg().get() / 100.0d);
			if (pPrice < (pLastKnownTradingPrice - (pLastKnownTradingPrice * p))) {
				throw new Exception("order price is outside (min) price limit " + super.getMinPricePctChg().get() + " %");
			}
		}

		if ((super.getMaxPricePctChg().get() != 0.0d) && (pLastKnownTradingPrice != 0)) {
			double p = (super.getMaxPricePctChg().get() / 100.0d);
			if (pPrice > (pLastKnownTradingPrice + (pLastKnownTradingPrice * p))) {
				throw new Exception("order price is outside (max) price limit " + super.getMaxPricePctChg().get() + " %");
			}
		}
	}

}

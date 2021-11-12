package com.hoddmimes.te.instrumentctl;

public class Symbol
{
	private String mId;
	private double mTickSize;
	private double mMinPricePct;
	private double mMaxPricePct;

	Symbol( String pSymbolId, double pTickSize ) {
		mId = pSymbolId;
		mTickSize = pTickSize;
		mMinPricePct = 0;
		mMaxPricePct = 0;
	}

	Symbol( String pSymbolId, double pTickSize, double pMinPricePct, double pMaxPricePct ) {
		mId = pSymbolId;
		mTickSize = pTickSize;
		mMinPricePct = pMinPricePct;
		mMaxPricePct = pMaxPricePct;
	}

	public String getId() {
		return mId;
	}

	public double getTickSize() {
		return mTickSize;
	}

	private boolean isTickSizeAligned( double pPrice ) {
		int y = 0;
		int x = ((int) pPrice / 1);
		double  r = pPrice - (double) x;

		x = (int) (r * 1000.0d);
		y = (int) (mTickSize * 1000.0d);

		return ((x % y) == 0);

	}

	public void validate( double pPrice, double pLastKnownTradingPrice ) throws Exception {
		// Validate that price is a multiple of the tick size
		if (!isTickSizeAligned( pPrice )) {
			throw new Exception("order price is not tick size aligned");
		}

		if ((mMinPricePct != 0.0d) && (pLastKnownTradingPrice != 0)) {
			double p = (mMinPricePct / 100.0d);
			if (pPrice < (pLastKnownTradingPrice - (pLastKnownTradingPrice * p))) {
				throw new Exception("order price is outside (min) price limit " + mMinPricePct + " %");
			}
		}

		if ((mMaxPricePct != 0.0d) && (pLastKnownTradingPrice != 0)) {
			double p = (mMaxPricePct / 100.0d);
			if (pPrice > (pLastKnownTradingPrice + (pLastKnownTradingPrice * p))) {
				throw new Exception("order price is outside (max) price limit " + mMaxPricePct + " %");
			}
		}
	}

}

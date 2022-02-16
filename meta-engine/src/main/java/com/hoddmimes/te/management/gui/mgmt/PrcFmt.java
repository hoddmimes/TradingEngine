package com.hoddmimes.te.management.gui.mgmt;

import com.hoddmimes.te.TeAppCntx;

import java.text.NumberFormat;

public class PrcFmt
{

	public static String format( long pPrice ) {
		NumberFormat numfmt = NumberFormat.getInstance();
		numfmt.setMinimumFractionDigits(2);
		numfmt.setMaximumFractionDigits(2);
		numfmt.setGroupingUsed(false);
		return numfmt.format( (pPrice / TeAppCntx.PRICE_MULTIPLER));
	}

	public static long convert( double pPrice ) {
		return (long) (pPrice * TeAppCntx.PRICE_MULTIPLER);
	}

	public static double convert( long pPrice ) {
		double d = pPrice / TeAppCntx.PRICE_MULTIPLER;
		return d;
	}


}

package com.hoddmimes.te.management.gui.mgmt;

import java.text.NumberFormat;

public class PrcFmt
{
	private static double MULTIPLIER = 10000.0d;

	public static String format( long pPrice ) {
		NumberFormat numfmt = NumberFormat.getInstance();
		numfmt.setMinimumFractionDigits(2);
		numfmt.setMaximumFractionDigits(2);
		numfmt.setGroupingUsed(false);
		return numfmt.format( (pPrice / MULTIPLIER));
	}

	public static long convert( double pPrice ) {
		return (long) (pPrice * MULTIPLIER);
	}

	public static double convert( long pPrice ) {
		double d = pPrice / MULTIPLIER;
		return d;
	}


}

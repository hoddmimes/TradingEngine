package com.hoddmimes.te.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SID
{
	private String  mSID;
	private int     mMarket;
	private String  mSymbol;

	private static final Pattern cTIDPattern = Pattern.compile("^(\\d+):([^\\s:]+)$");

	public SID(int pMarketId, String pSymbol ) {
		mSID = String.valueOf( pMarketId ) + ":" + pSymbol;
		mMarket = pMarketId;
		mSymbol = pSymbol;
	}

	public SID(String pTIDString )  {
		if (pTIDString == null) {
			throw new RuntimeException("invalid TID, must not be null");
		}
		Matcher m = cTIDPattern.matcher( pTIDString );
		if (!m.matches()) {
			throw new RuntimeException("invalid TID format : " + pTIDString);
		}
		mSID = pTIDString;
		mMarket = Integer.parseInt( m.group(1));
		mSymbol = m.group(2);
	}


	public boolean isValid( String pTIDStr) {
		if (pTIDStr == null) {
			return false;
		}
		Matcher m = cTIDPattern.matcher( pTIDStr );
		return m.matches();
	}

	public int getMarket() {
		return mMarket;
	}

	public String getSymbol() {
		return mSymbol;
	}

	@Override
	public String toString() {
		return mSID;
	}
}

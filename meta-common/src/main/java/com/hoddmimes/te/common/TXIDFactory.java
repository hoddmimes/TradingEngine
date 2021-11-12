package com.hoddmimes.te.common;

public class TXIDFactory
{
	private static final long cResetCount = 0xffffff;
	private static long mSeqNo = 0L;
	private static long mSeconds = (System.currentTimeMillis() / 1000L) << 24L;


	public static synchronized long getId()
	{
		mSeqNo++;
		if (mSeqNo >= cResetCount) {
			mSeconds = (System.currentTimeMillis() / 1000L) << 24L;
			mSeqNo = 1;
		}
		return (mSeconds + mSeqNo);
	}

	public static synchronized long getOrderId( boolean pBuyOrder ) {
		mSeqNo++;
		if (mSeqNo >= cResetCount) {
			mSeconds = (System.currentTimeMillis() / 1000L) << 24L;
			mSeqNo = 1;
		}
		long x = (mSeconds + mSeqNo);
		if (pBuyOrder) {
			x |= 0x4000000000000000L;
		} else {
			x &= 0x3fffffffffffffffL;
		}
		return x;
	}
}

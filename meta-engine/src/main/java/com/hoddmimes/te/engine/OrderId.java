package com.hoddmimes.te.engine;

public class OrderId
{
	private static long mTime = ((System.currentTimeMillis() / 1000L) << 32);
	private static long mSeqNo = 1;

	public static synchronized long get( Order.Side pSide ) {
		if (mSeqNo >= 0x7fffff00) {
			mTime = ((System.currentTimeMillis() / 1000L) << 32);
			mSeqNo = 1;
		}
		if (pSide == Order.Side.BUY) {
			if ((mSeqNo & 1) == 0) {
			   mSeqNo++;
			}
		} else {
			if ((mSeqNo & 1) == 1) {
				mSeqNo++;
			}
		}

		return (mTime + mSeqNo++);
	}


	public static boolean isBuyOrder( long pOrderId ) {
		return ((pOrderId & 1) == 1);
	}

}

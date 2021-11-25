package com.hoddmimes.te.engine;

import com.hoddmimes.te.common.TXIDFactory;
import com.hoddmimes.te.messages.generated.AddOrderRequest;
import com.hoddmimes.te.messages.generated.BdxOrderbookChange;
import com.hoddmimes.te.messages.generated.BdxOwnOrderbookChange;
import com.hoddmimes.te.messages.generated.OwnOrder;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;

public class Order implements Comparable<Order>
{
	public String getSid() {
		return mSid;
	}

	public double getPrice() {
		return mPrice;
	}

	public Side getSide() {
		return mSide;
	}

	public int getQuantity() {
		return mQuantity;
	}

	public long getCreationTime() {
		return mCreationTime;
	}

	public String getUserRef() {
		return mUserRef;
	}

	public long getOrderId() {
		return mOrderId;
	}

	public String getAccountId() {
		return mAccountId;
	}

	public void setQuantity(int pQuantity) {
		mQuantity = pQuantity;
	}

	public void setPrice(double pPrice ) {
		mPrice = pPrice;
	}


	public enum Side {BUY,SELL};
	public enum ChangeAction {ADD,REMOVE,MODIFY };
	private static SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	private String     mSid;
	private double     mPrice;
	private Side       mSide;
	private int        mQuantity;
	private long       mCreationTime;
	private String     mUserRef;
	private long       mOrderId;
	private String     mAccountId;

	@Override
	public int compareTo(Order pOrder) {
		if (this.mSide == Side.BUY) {
			if (this.mPrice < pOrder.mPrice) {
				return 1;
			} else if (this.mPrice > pOrder.mPrice) {
				return -1;
			}
		}  else {
			if (this.mPrice > pOrder.mPrice) {
				return 1;
			} else if (this.mPrice < pOrder.mPrice) {
				return -1;
			}
		}

		if (this.mCreationTime > pOrder.mCreationTime) {
			return 1;
		} else if (this.mCreationTime < pOrder.mCreationTime) {
			return -1;
		}
		return 0;
	}

	public boolean match( Order pOrder ) {
		if ((this.mSide == Side.BUY) && (this.mPrice >= pOrder.mPrice)) {
			return true;
		}
		if ((this.mSide == Side.SELL) && (this.mPrice <= pOrder.mPrice)) {
			return true;
		}
		return false;
	}



	public com.hoddmimes.te.messages.generated.Order toMsgOrder() {
		com.hoddmimes.te.messages.generated.Order o = new com.hoddmimes.te.messages.generated.Order();
		o.setSide( this.mSide.toString());
		o.setOrderId( Long.toHexString(this.mOrderId));
		o.setPrice( this.mPrice );
		o.setQuantity( this.mQuantity );
		return o;
	}

	public BdxOrderbookChange toOrderBookChg(ChangeAction pAction, long pObSeqNo ) {
		BdxOrderbookChange tBdx = new  BdxOrderbookChange();
		tBdx.setAction( pAction.name() );
		tBdx.setSid( this.mSid);
		tBdx.setSide( this.mSide.toString());
		tBdx.setOrderId(Long.toHexString(this.mOrderId));
		tBdx.setPrice(this.mPrice);
		tBdx.setQuantity(this.mQuantity);
		return tBdx;
	}

	public OwnOrder toOwnOrder() {
		OwnOrder oo = new OwnOrder();
		oo.setOrderId(Long.toHexString(this.mOrderId));
		oo.setPrice( this.mPrice );
		oo.setSide( this.mSide.name());
		oo.setRef( this.mUserRef );
		oo.setQuantity( this.mQuantity);
		oo.setSid( this.mSid );
		return oo;
	}

	public BdxOwnOrderbookChange toOwnOrderBookChg(ChangeAction pAction, long pObSeqNo ) {
		BdxOwnOrderbookChange tBdx = new  BdxOwnOrderbookChange();
		tBdx.setAction( pAction.name() );
		tBdx.setSid( this.mSid );
		tBdx.setSide( this.mSide.toString());
		tBdx.setOrderId(Long.toHexString(this.mOrderId));
		tBdx.setPrice(this.mPrice);
		tBdx.setQuantity(this.mQuantity);
		tBdx.setRef( this.mUserRef );
		return tBdx;
	}


	public Order(String pUserId, AddOrderRequest pAddOrderRequest) {
		mSid = pAddOrderRequest.getSid().get();
		mPrice = pAddOrderRequest.getPrice().get();
		mQuantity = pAddOrderRequest.getQuantity().get();
		mSide = Side.valueOf( pAddOrderRequest.getSide().get().toUpperCase());
		mUserRef = pAddOrderRequest.getRef().orElse(null);
		mCreationTime = System.currentTimeMillis();
		mOrderId = TXIDFactory.getId();
		mAccountId = pUserId;
	}

	private Order( String pUserId,  String pSymbol, double pPrice, int pVolume, Side pSide, String pRef, String pCreationTime  ) {
		this.mSid = pSymbol;
		this.mPrice = pPrice;
		this.mQuantity = pVolume;
		this.mSide = pSide;
		this.mUserRef = pRef;
		mOrderId = TXIDFactory.getId();
		this.mAccountId = pUserId;
		try {
			this.mCreationTime = SDF.parse( pCreationTime ).getTime();
		}
		catch( ParseException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {

		return "Symbol: " + mSid + " " + mQuantity + "@" + fmtprice(mPrice)  + " " + mSide.toString() +
				" ref: " + mUserRef + " time: " + SDF.format( mCreationTime ) + " user: " + mAccountId +
				" ordid: " + Long.toHexString( mOrderId );
	}

	private String fmtprice( double pPrice ) {
		double d = (double) Math.round( pPrice * 100.0d ) / 100.0d;
		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		nf.setMaximumFractionDigits(2); nf.setMinimumFractionDigits(2);
		return nf.format(d);
	}


	public static void main( String args[] )
	{
		LinkedList<Order> tList = new LinkedList<>();


		tList.add( new Order(null, "FOO", 10.07, 1, Side.BUY, "1", "2020-11-1 10:01:00.000"));
		tList.add( new Order(null, "FOO", 10.4, 1, Side.BUY, "2", "2020-11-1 10:03:00.000"));
		tList.add( new Order(null, "FOO", 10.4, 1, Side.BUY, "3", "2020-11-1 10:02:00.000"));
		tList.add( new Order(null, "FOO", 10.23, 1, Side.BUY, "4", "2020-11-1 10:04:00.000"));
		tList.add( new Order(null, "FOO", 10.14, 1, Side.BUY, "5", "2020-11-1 10:05:00.000"));
		tList.add( new Order(null, "FOO", 10.61, 1, Side.BUY, "6", "2020-11-1 10:06:00.000"));
		tList.add( new Order(null, "FOO", 10.52, 1, Side.BUY, "7", "2020-11-1 10:07:00.000"));

		Collections.sort( tList );
		tList.stream().forEach( t -> System.out.println(t));

		tList.clear();
		System.out.println("=============================================================");

		tList.add( new Order(null, "FOO", 10.0, 1, Side.SELL, "1", "2020-11-1 10:01:00.000"));
		tList.add( new Order(null, "FOO", 10.4, 1, Side.SELL, "2", "2020-11-1 10:03:00.000"));
		tList.add( new Order(null, "FOO", 10.4, 1, Side.SELL, "3", "2020-11-1 10:02:00.000"));
		tList.add( new Order(null, "FOO", 10.2, 1, Side.SELL, "4", "2020-11-1 10:04:00.000"));
		tList.add( new Order(null, "FOO", 10.1, 1, Side.SELL, "5", "2020-11-1 10:05:00.000"));
		tList.add( new Order(null, "FOO", 10.6, 1, Side.SELL, "6", "2020-11-1 10:06:00.000"));
		tList.add( new Order(null, "FOO", 10.5, 1, Side.SELL, "7", "2020-11-1 10:07:00.000"));

		Collections.sort( tList );
		tList.stream().forEach( t -> System.out.println(t));
	}



}

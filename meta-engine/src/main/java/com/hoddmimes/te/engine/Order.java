/*
 * Copyright (c)  Hoddmimes Solution AB 2022.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hoddmimes.te.engine;

import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.JsonDecoder;
import com.hoddmimes.jsontransform.JsonEncoder;
import com.hoddmimes.jsontransform.MessageInterface;
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

public class Order implements Comparable<Order>, MessageInterface
{
	public enum Side {BUY,SELL};
	public enum ChangeAction {ADD,REMOVE,MODIFY };


	private String     mSid;
	private long       mPrice;
	private Side       mSide;
	private long       mQuantity;
	private long       mCreationTime;
	private String     mUserRef;
	private long       mOrderId;
	private String     mAccountId;


	public Order(String pUserId, AddOrderRequest pAddOrderRequest) {
		this(  pUserId,
				pAddOrderRequest.getSid().get(),
				pAddOrderRequest.getPrice().get(),
				pAddOrderRequest.getQuantity().get(),
				Side.valueOf( pAddOrderRequest.getSide().get().toUpperCase()),
				pAddOrderRequest.getRef().orElse(null),
				System.currentTimeMillis());
	}

	 Order( String pUserId,  String pSymbol, long pPrice, long pVolume, Side pSide, String pRef, long pCreationTime ) {
		this.mSid = pSymbol;
		this.mPrice = pPrice;
		this.mQuantity = pVolume;
		this.mSide = pSide;
		this.mUserRef = pRef;
		this.mOrderId = OrderId.get( mSide );
		this.mAccountId = pUserId;
		this.mCreationTime = pCreationTime;
	}

	public Order( JsonObject pJsonString) {
		JsonDecoder tDecoder = new JsonDecoder( pJsonString );
		this.decode( tDecoder );
	}


	@Override
	public String getMessageName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public void encode(JsonEncoder pJsonEncoder) {
		pJsonEncoder.add("account", mAccountId);
		pJsonEncoder.add("price", mPrice);
		pJsonEncoder.add("quantity", mQuantity);
		pJsonEncoder.add("side", mSide.name());
		pJsonEncoder.add("ref", mUserRef);
		pJsonEncoder.add("orderId", mOrderId);
		pJsonEncoder.add("createTime", mCreationTime);
	}

	@Override
	public void decode(JsonDecoder pJsonDecoder) {
		mAccountId = pJsonDecoder.readString("account");
		mPrice = pJsonDecoder.readLong("price");
		mQuantity = pJsonDecoder.readLong("quantity");
		mSide = Side.valueOf( pJsonDecoder.readString("side"));
		mUserRef = pJsonDecoder.readString("ref");
		mOrderId = pJsonDecoder.readLong("orderId");
		mCreationTime = pJsonDecoder.readLong("createTime");
	}

	public JsonObject toJson() {
		JsonEncoder tEncoder = new JsonEncoder();
		this.encode( tEncoder );
		return tEncoder.toJson();
	}

	public String getSid() {
		return mSid;
	}

	public long getPrice() {
		return mPrice;
	}

	public Side getSide() {
		return mSide;
	}

	public long getQuantity() {
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

	public void setQuantity(long pQuantity) {
		mQuantity = pQuantity;
	}

	public void setPrice(long pPrice ) {
		mPrice = pPrice;
	}



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
		if (pOrder.mQuantity == 0) {
			return false;
		} else if (this.mQuantity == 0) {
			return false;
		}

		if ((this.mSide == Side.BUY) && (this.mPrice >= pOrder.mPrice)) {
			return true;
		}
		if ((this.mSide == Side.SELL) && (this.mPrice <= pOrder.mPrice)) {
			return true;
		}
		return false;
	}

	// For revert trade operations
	public void setOrderId( long pOrderId ) {
		mOrderId = pOrderId;
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





	@Override
	public String toString() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		return "Symbol: " + mSid + " " + mQuantity + "@" + fmtprice(mPrice)  + " " + mSide.toString() +
				" ref: " + mUserRef + " time: " + sdf.format( mCreationTime ) + " user: " + mAccountId +
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


		tList.add( new Order(null, "FOO", 100700, 1, Side.BUY, "1", cvtTimStr("10:01:00.000")));
		tList.add( new Order(null, "FOO", 104000, 1, Side.BUY, "2", cvtTimStr("10:03:00.000")));
		tList.add( new Order(null, "FOO", 104000, 1, Side.BUY, "3", cvtTimStr("10:02:00.000")));
		tList.add( new Order(null, "FOO", 102300, 1, Side.BUY, "4", cvtTimStr("10:04:00.000")));
		tList.add( new Order(null, "FOO", 101400, 1, Side.BUY, "5", cvtTimStr("10:05:00.000")));
		tList.add( new Order(null, "FOO", 106100, 1, Side.BUY, "6", cvtTimStr("10:06:00.000")));
		tList.add( new Order(null, "FOO", 105200, 1, Side.BUY, "7", cvtTimStr("10:07:00.000")));

		Collections.sort( tList );
		tList.stream().forEach( t -> System.out.println(t));

		tList.clear();
		System.out.println("=============================================================");

		tList.add( new Order(null, "FOO", 100000, 1, Side.SELL, "1", cvtTimStr("10:01:00.000")));
		tList.add( new Order(null, "FOO", 104000, 1, Side.SELL, "2", cvtTimStr("10:03:00.000")));
		tList.add( new Order(null, "FOO", 104000, 1, Side.SELL, "3", cvtTimStr("10:02:00.000")));
		tList.add( new Order(null, "FOO", 102000, 1, Side.SELL, "4", cvtTimStr("10:04:00.000")));
		tList.add( new Order(null, "FOO", 101000, 1, Side.SELL, "5", cvtTimStr("10:05:00.000")));
		tList.add( new Order(null, "FOO", 106000, 1, Side.SELL, "6", cvtTimStr("10:06:00.000")));
		tList.add( new Order(null, "FOO", 105000, 1, Side.SELL, "7", cvtTimStr("10:07:00.000")));

		Collections.sort( tList );
		tList.stream().forEach( t -> System.out.println(t));
	}

	private static long cvtTimStr(String timstr) {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
		try {sdf.parse(timstr).getTime();}
		catch (ParseException e) {e.printStackTrace();}
		return 0;
	}

}

package com.hoddmimes.te.engine;

import java.util.*;
import java.util.stream.Collectors;


public class OrderMap implements OrderMapInterface
{
	private Order.Side mSide;
	private LinkedList<Order> mOrders;

	public OrderMap(Order.Side pSide ) {
		mSide = pSide;
		mOrders = new LinkedList<>();
	}

	@Override
	public int size() {
		return mOrders.size();
	}

	public void add(Order pOrder ) {
		if (mOrders.isEmpty()) {
			mOrders.add( pOrder );
			return;
		}
		ListIterator<Order> tItr = mOrders.listIterator();
		while( tItr.hasNext() ) {
			Order o = tItr.next();
			if (o.compareTo(pOrder) > 0) {
				tItr.previous();
				tItr.add( pOrder );
			    return;
			}
		}
		mOrders.addLast( pOrder );

	}

	public Order remove( long pOrderId ) {
		ListIterator<Order> tItr = mOrders.listIterator();
		while( tItr.hasNext() ) {
			Order o = tItr.next();
			if (o.getOrderId() == pOrderId) {
				tItr.remove();
				return o;
			}
		}
		return null;
	}

	public Iterator<Order> iterator() {
		return mOrders.iterator();
	}

	@Override
	public List<Order> getOrders() {
		return mOrders;
	}

	@Override
	public Order peek() {
		return (mOrders.isEmpty()) ? null : mOrders.getFirst();
	}



}

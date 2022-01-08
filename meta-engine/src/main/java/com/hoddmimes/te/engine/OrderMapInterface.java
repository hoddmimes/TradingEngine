package com.hoddmimes.te.engine;

import java.util.Iterator;
import java.util.List;

public interface OrderMapInterface
{
	public int size();
	public void add( Order pOrder );
	public Order remove( long pOrderId );
	public Iterator<Order> iterator();
	public List<Order> getOrders();
	public Order peek();

}

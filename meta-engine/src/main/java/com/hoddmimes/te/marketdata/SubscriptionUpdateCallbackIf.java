package com.hoddmimes.te.marketdata;

import com.hoddmimes.te.messages.EngineBdxInterface;


public interface SubscriptionUpdateCallbackIf {


	public void distributorUpdate(String pSubjectName,
								  EngineBdxInterface pBdxMessage,
								  Object pCallbackParameter );
}

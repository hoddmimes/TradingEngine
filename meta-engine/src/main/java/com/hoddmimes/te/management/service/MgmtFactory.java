/*
 * Copyright (c)  Hoddmimes Solution AB 2021.
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

package com.hoddmimes.te.management.service;

import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.LocalIP4Address;
import com.hoddmimes.te.common.transport.IpmgPublisher;
import com.hoddmimes.te.common.transport.IpmgSubscriber;
import com.hoddmimes.te.common.transport.IpmgSubscriberListenerInterface;
import com.hoddmimes.te.messages.generated.MgmtConfigurationBdx;
import com.hoddmimes.te.messages.generated.MgmtConfigurationPingBdx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class MgmtFactory extends Thread implements IpmgSubscriberListenerInterface {
	private Logger mLog = LogManager.getLogger( MgmtFactory.class );
	private IpmgPublisher mPublisher;
	private IpmgSubscriber mSubscriber;
	private ConcurrentHashMap<String, MgmtComponentInterface> mComponents;
	private String mLocalIP4Address;
	private JsonObject mConfiguration;


	public MgmtFactory()
	{
		mComponents = new ConcurrentHashMap<>();
		mConfiguration = AuxJson.navigateObject(TeAppCntx.getInstance().getTeConfiguration(),"TeConfiguration/management");
		getLocalAddress();
		declareIpmg();
		this.start();
	}

	public MgmtComponentInterface registerComponent( String pComponentName, int pPort, MgmtCmdCallbackInterface pDefaultCommandHandler)
	{
		 MgmtComponent mci = new MgmtComponent( pComponentName, pPort, pDefaultCommandHandler);
		 mComponents.put( pComponentName, mci );
		 return null;
	}

	private void getLocalAddress() {
		mLocalIP4Address = AuxJson.navigateString(mConfiguration, "localAddress", null);
		if (mLocalIP4Address == null) {
			mLocalIP4Address = LocalIP4Address.getLocalIP4Address();
		}
	}


	private void declareIpmg() {

		mPublisher = new IpmgPublisher();
		mSubscriber = new IpmgSubscriber();
		try {
			mPublisher.initialize( AuxJson.navigateString( mConfiguration,"groupAddress"), AuxJson.navigateInt( mConfiguration,"groupPort"));
			mSubscriber.initialize(AuxJson.navigateString( mConfiguration,"groupAddress"), AuxJson.navigateInt( mConfiguration,"groupPort"));
			mSubscriber.addSubscriber(this );
		}
		catch( Exception e) {
			mLog.fatal("failed to initialize MgmtFactory", e);
			new RuntimeException(e);
		}
}

	@Override
	public void multicastReceived(MessageInterface pMsg)
	{
		if (pMsg instanceof MgmtConfigurationPingBdx) {
			collectAndPublish();
		}
	}


	private synchronized void collectAndPublish() {
		MgmtConfigurationBdx tBdx = new MgmtConfigurationBdx();
		tBdx.setHost( mLocalIP4Address );
		Iterator<MgmtComponentInterface> tItr = mComponents.values().iterator();
		while( tItr.hasNext() ) {
			tBdx.addComponents( tItr.next().toMgmtComponentMsg());
		}

		try { mPublisher.publish( tBdx ); }
		catch( Exception ie ) {
			mLog.error("failed to publish Mgmt configuration broadcast", ie);
		}
	}

	public void run() {
		while (true) {
			try {
				Thread.sleep(10000L);
			} catch (InterruptedException ie) {
			}

			collectAndPublish();
		}
	}
}


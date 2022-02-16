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

package com.hoddmimes.te.common.ipc;

import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.LocalIP4Address;
import com.hoddmimes.te.common.transport.IpmgPublisher;
import com.hoddmimes.te.common.transport.IpmgSubscriber;
import com.hoddmimes.te.common.transport.IpmgSubscriberListenerInterface;
import com.hoddmimes.te.common.transport.tcpip.TcpClient;
import com.hoddmimes.te.common.transport.tcpip.TcpThread;
import com.hoddmimes.te.messages.generated.IpcComponentConfiguration;
import com.hoddmimes.te.messages.generated.IpcConfigurationBdx;
import com.hoddmimes.te.messages.generated.IpcConfigurationPingBdx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class implements a IPC (inter process communication)
 * service allowing components to register them self as service endpoints
 * or retrieve an IPC proxy interface.
 */
public class IpcService implements IpmgSubscriberListenerInterface {
	private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss.SSS");
	private static Logger cLog = LogManager.getLogger( IpcService.class );
	private static long   BdxInterval = 5000L; // Every 5 sec
	public static enum ServiceEvent {Add , Remove };

	public interface IpcServiceListner {
		public void ipcServiceAdded( IpcComponentConfiguration pIpcComponentConfiguration);
		public void ipcServiceRemoved( IpcComponentConfiguration pIpcComponentConfiguration);
	}

	private HashMap<String, IpcComponent> mRegisteredComponents;
	private HashMap<String, TcpThread> mProxyConnections;
	private ConcurrentHashMap<String, IpcComponentConfiguration> mProxyServices;
	private List<IpcServiceListner> mServiceListeners;

	private IpmgSubscriber mIpmgSubscriber;
	private IpmgPublisher  mIpmgPublisher;

	private String     mLocalIP4Address;
	private JsonObject mIpcConfiguration;
	private IpcServicePublisher mServicePublisher;


	public IpcService( String pGroupAddress, int pGroupPort, String pLocalIP4Address ) {
	   JsonObject tConfiguration = new JsonObject();
	   tConfiguration.addProperty("groupAddress", pGroupAddress);
	   tConfiguration.addProperty("groupPort", pGroupPort);
	   tConfiguration.addProperty("localAddress", pLocalIP4Address );

		mRegisteredComponents = new HashMap<>();
		mProxyConnections = new HashMap<>();
		mProxyServices = new ConcurrentHashMap<>();
		mServiceListeners = new LinkedList<>();

		mIpcConfiguration = tConfiguration;
		setupIpmg();
		mServicePublisher = new IpcServicePublisher();
		mServicePublisher.start();
	}

	public IpcService( JsonObject pIpcConfiguration ) {
		  this( pIpcConfiguration.get("groupAddress").getAsString(),
				pIpcConfiguration.get("groupPort").getAsInt(),
				pIpcConfiguration.get("localAddress").getAsString());
	}

	public void addServiceListener( IpcServiceListner pListener ) {
		synchronized ( mServiceListeners ) {
			mServiceListeners.add( pListener );
			Iterator<IpcComponentConfiguration> tItr = mProxyServices.values().iterator();
			while (tItr.hasNext()) {
				pListener.ipcServiceAdded( tItr.next());
			}
		}
	}

	public String getGroupAddress() {
		return mIpcConfiguration.get("groupAddress").getAsString();
	}
	public int getGroupPort() {
		return mIpcConfiguration.get("groupPort").getAsInt();
	}

	public IpcComponentInterface registerComponent(String pComponentName, int pPort, IpcRequestCallbackInterface pDefaultCommandHandler)
	{
		IpcComponent mci = new IpcComponent( pComponentName, mLocalIP4Address, pPort, pDefaultCommandHandler);
		mRegisteredComponents.put( pComponentName, mci );
		return null;
	}

	public void waitForService(String pServiceName) {
		boolean tFound = false;
		while (!tFound) {
			Iterator<IpcComponentConfiguration> tItr = mProxyServices.values().iterator();
			while (tItr.hasNext()) {
				if (tItr.next().getName().get().contentEquals(pServiceName)) {
					tFound = true;
					break;
				}
			}
			if (!tFound) {
				synchronized ( mProxyServices ) {
					try { mProxyServices.wait();}
					catch (InterruptedException e) {}
				}
			}
		}
	}

	public IpcProxy getServiceProxy( String pServiceName ) throws IOException {
		TcpThread tTcpThread = mProxyConnections.get( pServiceName );
		if (tTcpThread != null) {
		  return new IpcProxy( tTcpThread, pServiceName );
		}

		IpcComponentConfiguration tCompCfg = mProxyServices.get( pServiceName );
		if (tCompCfg == null) {
			throw new IOException("Service \"" + pServiceName + "\" is not available");
		}


		tTcpThread = TcpClient.connect( tCompCfg.getHost().get(), tCompCfg.getPort().get());
		mProxyConnections.put( pServiceName, tTcpThread );
		return new IpcProxy(tTcpThread, pServiceName);
	}

	private void setupIpmg() {

		getLocalAddress();

		try {
			String tGroupAddress = AuxJson.navigateString( mIpcConfiguration, "groupAddress");
			int tGroupPort = AuxJson.navigateInt( mIpcConfiguration, "groupPort");

			mIpmgPublisher = new IpmgPublisher();
			mIpmgPublisher.initialize( tGroupAddress, tGroupPort );
			mIpmgSubscriber = new IpmgSubscriber();
			mIpmgSubscriber.initialize(tGroupAddress, tGroupPort);
			mIpmgSubscriber.addSubscriber( this );

			IpcConfigurationPingBdx tBdx = new IpcConfigurationPingBdx();
			mIpmgPublisher.publish( tBdx );

		}
		catch( Exception e) {
			cLog.fatal("failed to declare IPC service", e);
			System.exit(0);
		}
	}

	private void notifyListeners( IpcComponentConfiguration pService, ServiceEvent pEvent ) {
		synchronized ( mServiceListeners ) {
			for( IpcServiceListner tListener : mServiceListeners) {
				if (pEvent == ServiceEvent.Add) {
					tListener.ipcServiceAdded( pService );
				} else {
					tListener.ipcServiceRemoved( pService );
				}
			}
		}
	}

	private void getLocalAddress() {
		mLocalIP4Address = (mIpcConfiguration.has("localAddress") && (!mIpcConfiguration.get("localAddress").isJsonNull()))
				? AuxJson.navigateString(mIpcConfiguration, "localAddress") : null;

		if (mLocalIP4Address == null) {
			mLocalIP4Address = LocalIP4Address.getLocalIP4Address();
		}
	}


	private void updateConfiguration(IpcConfigurationBdx pCfgBdx )
	{
		long tNow = System.currentTimeMillis();


		for (IpcComponentConfiguration tBdxCompCfg : pCfgBdx.getComponents().get()) {
			IpcComponentConfiguration tCompCfg = mProxyServices.get( tBdxCompCfg.getName().get());
			if (tCompCfg == null) {
				mProxyServices.put( tBdxCompCfg.getName().get(), tBdxCompCfg);
				cLog.info("IPC proxy service \"" + tBdxCompCfg.toJson().toString() + "\" discovered");
				notifyListeners( tBdxCompCfg, ServiceEvent.Add );
			} else {
				long t1 = tCompCfg.getLastTimeSeen().get();
				tCompCfg.setLastTimeSeen( tNow );
				//System.out.println("updcfg: " + tCompCfg.getName().get() + " tim-before: " + SDF.format( t1 ) + " tim-after: " + SDF.format( tCompCfg.getLastTimeSeen().get()) );
			}
		}
		synchronized ( mProxyServices ) {
			mProxyServices.notifyAll();
		}
	}

	@Override
	public void multicastReceived(MessageInterface pMsg) {
		if (pMsg instanceof IpcConfigurationBdx) {
			updateConfiguration((IpcConfigurationBdx) pMsg);
		}
	}

	class IpcServicePublisher extends Thread
	{
		IpcServicePublisher() {}

		private void publishConfiguration() {
			IpcConfigurationBdx tBdx = new IpcConfigurationBdx();
			long tNow = System.currentTimeMillis();
			synchronized ( mRegisteredComponents ) {
				Iterator<IpcComponent> tItr = mRegisteredComponents.values().iterator();
				while( tItr.hasNext() ) {
					tBdx.addComponents( tItr.next().toIpcComponentConfigMsg());
				}
			}
			if (tBdx.getComponents().isPresent() && (tBdx.getComponents().get().size() > 0)) {
				try {
					mIpmgPublisher.publish( tBdx );
				} catch (Exception e) {
					cLog.error("failed to publisg IPC configuration bdx", e);
				}
			}
		}

		private void checkDeadServices() {

			List<IpcComponentConfiguration> mServicesToRemove = new ArrayList<>();
			long tNow = System.currentTimeMillis();
				Iterator<IpcComponentConfiguration> tItr = mProxyServices.values().iterator();
				while (tItr.hasNext()) {
					IpcComponentConfiguration tCompCfg = tItr.next();
					long tTimeOut = (tCompCfg.getLastTimeSeen().get() + (BdxInterval * 4));

					if (tTimeOut <= tNow) {
						//System.out.println("dead-service: " + tCompCfg.getName().get() + " timout: " + SDF.format( tTimeOut) + " tim-now: " + SDF.format( tTimeOut ) );
						mServicesToRemove.add( tCompCfg );
					}
				}
				for (IpcComponentConfiguration tCompConfig : mServicesToRemove) {
					mProxyServices.remove(tCompConfig);
					notifyListeners( tCompConfig, ServiceEvent.Remove );
					cLog.info("IPC proxy service \"" + tCompConfig + "\" timed out");
				}
		}

		public void run() {
			setName("IpcServicePublisher");
			try { Thread.sleep( 1000L); }
			catch( InterruptedException ie) {}
			publishConfiguration();
			try { Thread.sleep( (BdxInterval * 4L)); }
			catch( InterruptedException ie) {}

			while( true ) {
				publishConfiguration();
				checkDeadServices();

				try { Thread.sleep(BdxInterval); }
				catch( InterruptedException ie) {}
			}
		}
	}
}

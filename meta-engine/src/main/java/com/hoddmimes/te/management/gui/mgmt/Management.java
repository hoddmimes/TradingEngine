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

package com.hoddmimes.te.management.gui.mgmt;

import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.transport.IpmgPublisher;
import com.hoddmimes.te.common.transport.IpmgSubscriber;
import com.hoddmimes.te.common.transport.IpmgSubscriberListenerInterface;
import com.hoddmimes.te.common.transport.tcpip.TcpClient;
import com.hoddmimes.te.common.transport.tcpip.TcpThread;
import com.hoddmimes.te.messages.MgmtMessageRequest;
import com.hoddmimes.te.messages.MgmtMessageResponse;
import com.hoddmimes.te.messages.generated.*;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class Management extends JFrame implements IpmgSubscriberListenerInterface, ServiceInterface
{
	static final Font DEFAULT_FONT = new Font("Arial", Font.PLAIN, 14 );
	static final Font DEFAULT_FONT_BOLD = new Font("Arial", Font.BOLD, 14 );
	static final Color BUTTON_BACKGROUND = new Color(0xcad9cd);
	static final Color PANEL_BACKGROUND = new Color(0xe6e2d8);
	static final Color TXTFLD_BACKGROUND = new Color(0xdfdfdd);
	static final Color SUSPENDED_BACKGROUND = new Color(0xffa366);


	ConcurrentHashMap<String, TcpThread> mServices;
	MessageFactory mMessageFactory;



	JTabbedPane mTabbedPane;
	ConfigPanel mConfigPanel;
	MarketPanel mMarketPanel;
	SymbolPanel mSymbolPanel;
	AccountsPanel mAccountPanel;
	OrderPanel mOrderPanel;
	TradePanel mTradePanel;
	MsglogPanel mMsglogPanel;
	StatisticsPanel mStatisticsPanel;


	IpmgSubscriber mIpmgSubscriber;
	IpmgPublisher mIpmgPublisher;

	private String mGroupAddress;
	private int mGroupPort;

	private WaitForServerFrame mWaitForServerFrame;



	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		Management m = new Management();
		m.parseArguments( args );
		m.init();
		m.pack();
		m.setVisible( true );
	}

	private void parseArguments( String[] args ) {
		int i = 0;
		while( i < args.length) {
			if (args[i].contentEquals("-grpAddr")) {
				mGroupAddress = args[++i];
			}
			if (args[i].contentEquals("-grpPort")) {
				mGroupPort = Integer.parseInt(args[++i]);
			}
			i++;
		}
	}

	private void init() {
		mServices = new ConcurrentHashMap<>();
		mTabbedPane = new JTabbedPane();
		mMessageFactory = new MessageFactory();




		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.add( createTopPanel(), BorderLayout.NORTH );
		//tRootPanel.add( createCenterPanel(), BorderLayout.CENTER );

		this.setContentPane( tRootPanel );
		setupIpmg();

		this.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent componentEvent) {
				//System.out.println( componentEvent.toString());
				mSymbolPanel.resizeEvent( componentEvent.getComponent().getSize());
				mMsglogPanel.resizeEvent( componentEvent.getComponent().getSize());
			}
		});

		mWaitForServerFrame = new WaitForServerFrame( mGroupAddress, mGroupPort );
		mWaitForServerFrame.setVisible( true );

		synchronized ( mWaitForServerFrame ) {
			try {
				mWaitForServerFrame.wait();
			} catch (InterruptedException ie) {}
		}
		mWaitForServerFrame = null;
	}


	private TcpThread getService( String pService ) {
		TcpThread tTcpThread = mServices.get( pService );
		if (tTcpThread != null) {
			return tTcpThread;
		}
		List<ConfigPanel.ComponentEntity> tServices = mConfigPanel.getServiceComponents();
		for( ConfigPanel.ComponentEntity ce : tServices) {
			if (ce.getName().contentEquals( pService)) {
				try {
					tTcpThread = TcpClient.connect( ce.getHost(), Integer.parseInt(ce.getPort()));
					mServices.put( pService, tTcpThread );
					return tTcpThread;
				}
				catch( IOException e) {
					System.out.println("failed to connect to service \"" + pService + "\" reason: " + e.getMessage() );
					return null;
				}

			}
		}
		return null;
	}

	@Override
	public MgmtMessageResponse transceive(String pService, MgmtMessageRequest pRequest) {
		TcpThread tTcpThread = getService( pService );
		if (tTcpThread == null) {
			JOptionPane.showMessageDialog(this,
					"Service \"" + pService + "\" is currently not available",
					"Service not avaiable",
					JOptionPane.WARNING_MESSAGE);
			return null;
		}

		MessageInterface tResponse = null;
		try {
			byte[] tRcvBuf = tTcpThread.transceive(pRequest.toJson().toString().getBytes(StandardCharsets.UTF_8));
			tResponse = mMessageFactory.getMessageInstance(new String(tRcvBuf));
			if (tResponse == null) {
				JOptionPane.showMessageDialog(this,
						"Invalid response from service \"" + pService + "\"\n rspmsg: " + new String(tRcvBuf),
						"Invalid Response",
						JOptionPane.WARNING_MESSAGE);
				System.out.println("invalied response message from service: " + pService + " msg: " + new String(tRcvBuf));
				return null;
			}
			if (((MgmtMessageResponse) tResponse) instanceof MgmtStatusResponse) {
				JOptionPane.showMessageDialog(this,
						"Error from service \"" + pService + "\"\n" +
								((MgmtStatusResponse) tResponse).getMessage(),
						"Invalid Response",
						JOptionPane.WARNING_MESSAGE);
				return null;
			}
		}
		catch( IOException e) {
			JOptionPane.showMessageDialog(this,
					"disconnected service \"" + pService + "\"\n error: " + e.getMessage(),
					"Disconnected Service",
					JOptionPane.WARNING_MESSAGE);
		}

		return (MgmtMessageResponse) tResponse;
	}

	private void
	setupIpmg() {
		try {
			mIpmgPublisher = new IpmgPublisher();
			mIpmgPublisher.initialize(mGroupAddress, mGroupPort);
			mIpmgSubscriber = new IpmgSubscriber();
			mIpmgSubscriber.initialize(mGroupAddress, mGroupPort);
			mIpmgSubscriber.addSubscriber( this );

			MgmtConfigurationPingBdx tBdx = new MgmtConfigurationPingBdx();
			mIpmgPublisher.publish( tBdx );

		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}

	private JPanel createTopPanel() {
		JPanel tRootPanel = new JPanel( new BorderLayout());

		// Create Header Panel
		JPanel tHdrLblPanel   = new JPanel(new BorderLayout());
		JPanel tHdrLblContent = new JPanel(new BorderLayout());
		tHdrLblContent.setBorder( new EmptyBorder(12,40,10,0));
		JLabel tHdrLbl = new JLabel("TE Managment Controller");
		tHdrLbl.setFont( new Font("arial", Font.BOLD, 18 ));
		tHdrLbl.setForeground( Color.blue);
		tHdrLblContent.add( tHdrLbl, BorderLayout.CENTER);
		tHdrLblPanel.add( tHdrLblContent, BorderLayout.WEST);

		tRootPanel.add( tHdrLblPanel, BorderLayout.NORTH);

		// Create tabbed panel
		mConfigPanel = new ConfigPanel();
		mMarketPanel = new MarketPanel(this);
		mSymbolPanel = new SymbolPanel( this );
		mAccountPanel = new AccountsPanel( this );
		mOrderPanel = new OrderPanel( this );
		mTradePanel = new TradePanel( this );
		mMsglogPanel = new MsglogPanel( this );
		mStatisticsPanel = new StatisticsPanel(this );

		mTabbedPane.addTab("Configuration", mConfigPanel );
		mTabbedPane.addTab("Markets", mMarketPanel);
		mTabbedPane.addTab("Symbols", mSymbolPanel);
		mTabbedPane.addTab("Accounts", mAccountPanel);
		mTabbedPane.addTab("Orders", mOrderPanel);
		mTabbedPane.addTab("Trades", mTradePanel);
		mTabbedPane.addTab("MsgLog", mMsglogPanel);
		mTabbedPane.addTab("Statistics", mStatisticsPanel);

		mTabbedPane.setFont( Management.DEFAULT_FONT );
		mTabbedPane.setPreferredSize( new Dimension(800,650));


		tRootPanel.add( mTabbedPane, BorderLayout.CENTER);
		mTabbedPane.setPreferredSize( new Dimension(860,580));

		mTabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JTabbedPane tabPane = (JTabbedPane) e.getSource();
				Component c = tabPane.getSelectedComponent();
				if (c instanceof MarketPanel) {
					((MarketPanel) c).loadData();
				}
				if (c instanceof SymbolPanel) {
					((SymbolPanel) c).loadMarketData();
				}
				if (c instanceof AccountsPanel) {
					((AccountsPanel) c).loadAccountData();
				}
				if (c instanceof OrderPanel) {
					((OrderPanel) c).loadAccountData();
				}
				if (c instanceof TradePanel) {
					((TradePanel) c).loadAccountAndSidData();
				}
				if (c instanceof MsglogPanel) {
					((MsglogPanel) c).loadAccountData();
				}
				if (c instanceof StatisticsPanel) {
					((StatisticsPanel) c).loadStatisticsData();
				}
				tabPane.setSelectedComponent(c);
			}
		});

		return tRootPanel;
	}

	JComponent makeTextJanel( String pText ) {
		JTextArea tTxtArea = new JTextArea(pText);
		tTxtArea.setRows(10);
		tTxtArea.setColumns(80);
		tTxtArea.setPreferredSize(new Dimension(800,400));
		return tTxtArea;
	}


	void mgmtConfigurationUpdate( MgmtConfigurationBdx pConfigUpdate ) {
		mConfigPanel.configurationUpdate(pConfigUpdate);

		if (mWaitForServerFrame != null) {
			synchronized (mWaitForServerFrame) {
				mWaitForServerFrame.serverIsAvailable();
			}
		}
	}

	@Override
	public void multicastReceived(MessageInterface pMsg) {
		if (pMsg instanceof MgmtConfigurationBdx) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
				   mgmtConfigurationUpdate((MgmtConfigurationBdx) pMsg);
				}
			});
		}
	}
}



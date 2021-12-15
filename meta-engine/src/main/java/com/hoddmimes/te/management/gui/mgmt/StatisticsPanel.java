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

import com.hoddmimes.te.common.interfaces.TeMgmtServices;
import com.hoddmimes.te.messages.generated.Account;
import com.hoddmimes.te.messages.generated.MgmtGetAccountsRequest;
import com.hoddmimes.te.messages.generated.MgmtGetAccountsResponse;
import org.springframework.security.core.parameters.P;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StatisticsPanel extends BasePanel
{
	private ServiceInterface mServiceInterface;


	private JTabbedPane mTabbedPane;
	private ServiceStatisticsPanel mSessionsPanel;
	private ServiceStatisticsPanel mMatchingPanel;
	private ServiceStatisticsPanel mTradePanel;
	private ServiceStatisticsPanel mMarketDataPanel;




	StatisticsPanel(ServiceInterface pServiceInterface ) {
		super( pServiceInterface );
		this.setLayout(new BorderLayout());
		mServiceInterface = pServiceInterface;

		mTabbedPane = new JTabbedPane();

		mSessionsPanel = new ServiceStatisticsPanel( "Session Controller Statistics" );
		mMatchingPanel = new ServiceStatisticsPanel( "Matching Statistics" );
		mTradePanel = new ServiceStatisticsPanel( "Trades Statistics" );
		mMarketDataPanel = new ServiceStatisticsPanel( "Market Data Statistics" );


		mTabbedPane.addTab("Session", mSessionsPanel );
		mTabbedPane.addTab("Matcher", mMatchingPanel );
		mTabbedPane.addTab("Trades", mTradePanel );
		mTabbedPane.addTab("Market Data", mMarketDataPanel );

		this.add( mTabbedPane, BorderLayout.CENTER);
		this.add( createButtonPanel(), BorderLayout.SOUTH);
	}

	private JPanel mockPanel(String pText ) {
		JPanel tRootPanel = new JPanel(new BorderLayout());
		tRootPanel.add(new TextArea(pText), BorderLayout.CENTER);
		return tRootPanel;
	}

	private JPanel createButtonPanel()
	{
		JPanel tPanel = new JPanel( new GridBagLayout());
		tPanel.setBorder( new EtchedBorder(3));
		tPanel.setBackground( PANEL_BACKGROUND );

		JPanel tContentPanel = new JPanel( new FlowLayout( FlowLayout.CENTER));
		tContentPanel.setBackground( PANEL_BACKGROUND );
		tContentPanel.setBorder( new EmptyBorder(0,10,0,10));

		JButton tRefreshButton = new JButton("Refresh");
		tRefreshButton.setBackground( BUTTON_BACKGROUND );
		tRefreshButton.setFont( DEFAULT_FONT );
		tContentPanel.add( tRefreshButton );

		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = gc.gridy = 0;
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.insets = new Insets(3,10,3,10);
		gc.fill = GridBagConstraints.HORIZONTAL;
		tPanel.add( tContentPanel, gc );
		return tPanel;
	}





}

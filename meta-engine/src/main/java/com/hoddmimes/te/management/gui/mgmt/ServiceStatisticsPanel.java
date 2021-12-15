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

import com.hoddmimes.te.messages.generated.MgmtStatEntry;

import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import javax.swing.border.EtchedBorder;
import java.awt.*;


public class ServiceStatisticsPanel extends JPanel
{
	JScrollPane mScrollPane;
	JPanel      mStatPane;
	JPanel      mTitlePanel;
	JPanel      mStatPanel;


	ServiceStatisticsPanel( String pTitle ) {
		this.setLayout( new BorderLayout());
		mTitlePanel = createTitlePane( pTitle );

		this.add( mTitlePanel , BorderLayout.NORTH);
		this.add( createStatPane(), BorderLayout.CENTER);

		List<MgmtStatEntry> tList = new ArrayList<>();
		tList.add( new MgmtStatEntry().setAttribute("Attribute foo bar ").setValue("66"));
		tList.add( new MgmtStatEntry().setAttribute("Attribute foo bar 01 ").setValue("123452"));
		tList.add( new MgmtStatEntry().setAttribute("Attribute foo bar 02").setValue("kalle"));
		tList.add( new MgmtStatEntry().setAttribute("Attribute foo bar 03").setValue("51432"));
		tList.add( new MgmtStatEntry().setAttribute("Attribute foo bar 04").setValue("77635"));
		loadStatistics( tList );
	}

	private JPanel createStatPane() {
		JPanel tRootPane = new JPanel( new BorderLayout());
		mStatPane = new JPanel (new GridBagLayout());

		mScrollPane = new JScrollPane( mStatPane );
		mScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		mScrollPane.setPreferredSize(new Dimension(this.getWidth(), (this.getHeight() - mTitlePanel.getHeight())));

		tRootPane.add( mScrollPane, BorderLayout.CENTER );
		return tRootPane;
	}

	private void loadStatistics( List<MgmtStatEntry> pStatistics ) {
		int tEntries = mStatPane.getComponentCount();

		// Remove all old entries
		for( int i = tEntries; i > 0; i-- ) {
			mStatPane.remove(i);
		}

		GridBagConstraints gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.gridx = gc.gridy = 0;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.insets = new Insets(1,10,1, 10 );

		for( MgmtStatEntry se : pStatistics ) {
			mStatPane.add( new StatEntry( se ), gc );
			gc.gridy++;
		}
	}



	private JPanel createTitlePane(String pTitle) {
		JPanel tRootPanel = new JPanel(new GridBagLayout());
		tRootPanel.setBackground(BasePanel.PANEL_BACKGROUND);

		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = gc.gridy = 0;
		gc.insets = new Insets(3, 10, 3, 10);
		gc.fill = GridBagConstraints.HORIZONTAL;

		JPanel tContentPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		tContentPanel.setBackground(BasePanel.PANEL_BACKGROUND);
		tContentPanel.setBorder(new EtchedBorder(3));

		JLabel tLabel = new JLabel(pTitle);
		tLabel.setFont(new Font("Arial", Font.BOLD, 14));
		tLabel.setBackground(BasePanel.PANEL_BACKGROUND);
		tContentPanel.add(tLabel);
		tRootPanel.add(tContentPanel);
		return tRootPanel;
	}








	class StatEntry extends JPanel {
		private MgmtStatEntry mStatistics;

		public StatEntry(MgmtStatEntry pStat ) {
			mStatistics = pStat;
			this.setLayout( new GridBagLayout());
			this.setBackground( BasePanel.PANEL_BACKGROUND );
			this.setBorder(new EtchedBorder(3));

			JTextField tAttributeTxtFld = new JTextField( mStatistics.getAttribute().get());
			tAttributeTxtFld.setFont(new Font("Arial", Font.PLAIN, 12));
			tAttributeTxtFld.setBackground( BasePanel.TXTFLD_BACKGROUND);
			tAttributeTxtFld.setEditable(false);
			tAttributeTxtFld.setPreferredSize( new Dimension(260,22));

			GridBagConstraints gc = new GridBagConstraints();
			gc.insets = new Insets(1,10,1,10);
			gc.gridx = gc.gridy = 0;
			gc.fill = GridBagConstraints.HORIZONTAL;
			gc.anchor = GridBagConstraints.NORTHWEST;

			this.add( tAttributeTxtFld, gc );

			JTextField tValueTxtFld = new JTextField( mStatistics.getValue().get());
			tValueTxtFld.setFont(new Font("Arial", Font.PLAIN, 12));
			tValueTxtFld.setBackground( BasePanel.TXTFLD_BACKGROUND);
			tValueTxtFld.setEditable(false);
			tValueTxtFld.setHorizontalAlignment(SwingConstants.RIGHT);
			tValueTxtFld.setPreferredSize( new Dimension(60,22));

			gc.gridx++;
			this.add( tValueTxtFld, gc );
		}
	}
}

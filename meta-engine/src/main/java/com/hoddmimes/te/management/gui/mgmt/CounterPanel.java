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

import java.util.List;
import javax.swing.*;

import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;


public class CounterPanel extends JPanel
{
	JScrollPane mScrollPane;
	JPanel      mTitlePanel;
	JPanel      mCounterPanel;


	CounterPanel( List<MgmtStatEntry> pStatElements, boolean pUseScrollPanel ) {
		this.setLayout( new BorderLayout());

		this.setBorder( new EtchedBorder(3));
		this.add( createStatPane( pUseScrollPanel ), BorderLayout.CENTER);
		if (pStatElements != null) {
			loadStatistics(pStatElements);
		}
	}

	CounterPanel(boolean pUseScrollPanel ) {
		this( null, pUseScrollPanel );
	}

	private JPanel createStatPane( boolean pUseScrollPanel) {
		JPanel tRootPane = new JPanel( new BorderLayout());
		tRootPane.setBorder(new EmptyBorder(6,0,6,0));
		mCounterPanel = new JPanel (new GridBagLayout());

		if (pUseScrollPanel) {
			mScrollPane = new JScrollPane(mCounterPanel);
			mScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			mScrollPane.setPreferredSize(new Dimension(this.getWidth(), (this.getHeight() - mTitlePanel.getHeight())));
			tRootPane.add( mScrollPane, BorderLayout.CENTER );
		} else {
			tRootPane.add(mCounterPanel, BorderLayout.CENTER );
		}
		return tRootPane;
	}

	public void loadStatistics( List<MgmtStatEntry> pStatistics ) {
		int tEntries = mCounterPanel.getComponentCount();

		// Remove all old entries
		for( int i = tEntries - 1; i >= 0; i-- ) {
			mCounterPanel.remove(i);
		}

		GridBagConstraints gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.gridx = gc.gridy = 0;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.insets = new Insets(1,10,1, 10 );

		for( MgmtStatEntry se : pStatistics ) {
			mCounterPanel.add( new StatEntry( se ), gc );
			gc.gridy++;
		}
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

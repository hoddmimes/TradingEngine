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

import com.hoddmimes.te.common.interfaces.TeService;
import com.hoddmimes.te.management.gui.table.Table;
import com.hoddmimes.te.management.gui.table.TableAttribute;
import com.hoddmimes.te.management.gui.table.TableModel;
import com.hoddmimes.te.messages.SID;
import com.hoddmimes.te.messages.generated.*;

import java.util.Collections;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Comparator;

public class StatMatcherPanel extends BasePanel {

	private CounterPanel mCounterPanel;
	private TableModel<StatMatcherPanel.SymbolEntry> mMatchTableModel;
	private Table mMatchTable;

	StatMatcherPanel(ServiceInterface pServiceInterface) {
		super(pServiceInterface);
		init();
	}

	private void init()
	{
		this.setLayout( new BorderLayout() );
		this.setBackground( PANEL_BACKGROUND );

		// Create and add counter panel
		JPanel tTopPanel = new JPanel( new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = gc.gridy = 0;
		gc.anchor = GridBagConstraints.CENTER;
		gc.insets = new Insets(16,0,8,0);

		mCounterPanel = new CounterPanel( false );
		tTopPanel.add( mCounterPanel, gc );
		this.add( tTopPanel, BorderLayout.NORTH );


		mMatchTableModel = new TableModel(StatMatcherPanel.SymbolEntry.class);

		mMatchTable = new Table(mMatchTableModel, new Dimension(mMatchTableModel.getPreferedWith() + 18, 220), null);
		mMatchTable.setBackground(Color.white);
		mMatchTable.setFont(new Font("Arial", Font.PLAIN, 14));

		JPanel tTablePanel = new JPanel();
		tTablePanel.setBorder( new EmptyBorder(10,10,10,10));
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 5, 10, 5));
		tTablePanel.add(mMatchTable);

		this.add(tTablePanel, BorderLayout.CENTER);

	}

	public void refreshStatistics() {
		MgmtQueryMatcherRequest tReq = new MgmtQueryMatcherRequest().setRef("qms");
		MgmtQueryMatcherResponse tResp = (MgmtQueryMatcherResponse) mServiceInterface.transceive(TeService.MatchingService.name(), tReq );

		mCounterPanel.loadStatistics( tResp.getMatcherCounters().get());
		mMatchTableModel.clear();
		List<MgmtSymbolMatcherEntry> tSidList = tResp.getSids().get();
		Collections.sort( tSidList, new SidSorter());

		for( MgmtSymbolMatcherEntry tSidEntry : tResp.getSids().get()) {
			mMatchTableModel.addEntry( new SymbolEntry( tSidEntry ));
		}
		this.revalidate();
		this.repaint();
	}



	public class SymbolEntry {
		MgmtSymbolMatcherEntry mSidEntry;
		NumberFormat numfmt;

		public SymbolEntry(MgmtSymbolMatcherEntry pSidEntry ) {
			mSidEntry = pSidEntry;
			numfmt = NumberFormat.getInstance();
			numfmt.setGroupingUsed(false);
			numfmt.setMaximumFractionDigits(2);
			numfmt.setMinimumFractionDigits(2);
		}


		@TableAttribute(header = "SID", column = 1, width = 100, alignment = JLabel.LEFT)
		public String getSid() {
			return mSidEntry.getSid().get();
		}

		@TableAttribute(header = "Buy Orders", column = 2, width = 80, alignment = JLabel.LEFT)
		public String getBuyOrders() {
			return String.valueOf(mSidEntry.getBuyOrders().get());
		}

		@TableAttribute(header = "Best Bid", column = 3, width = 80, alignment = JLabel.LEFT)
		public String getBestBid() {
			String tPrice = (mSidEntry.getBuyPrice().isEmpty()) ? "----" : numfmt.format( mSidEntry.getBuyPrice().get());
			return tPrice;
		}

		@TableAttribute(header = "Sell Orders", column = 4, width = 80, alignment = JLabel.LEFT)
		public String getSellOrders() {
			return String.valueOf(mSidEntry.getSellOrders().get());
		}
		@TableAttribute(header = "Best Offer", column = 5, width = 80, alignment = JLabel.LEFT)
		public String getBestOffer() {
			String tPrice = (mSidEntry.getSellPrice().isEmpty()) ? "----" : numfmt.format( mSidEntry.getSellPrice().get());
			return tPrice;
		}

		@TableAttribute(header = "Order Rqst", column = 6, width = 80, alignment = JLabel.LEFT)
		public String getOrderRequests() {
			return String.valueOf(mSidEntry.getOrders().get());
		}

		@TableAttribute(header = "Executions", column = 7, width = 80, alignment = JLabel.LEFT)
		public String getExecutions() {
			return String.valueOf(mSidEntry.getExecutions().get());
		}
	}

	class SidSorter implements Comparator<MgmtSymbolMatcherEntry>
	{
		@Override
		public int compare(MgmtSymbolMatcherEntry ME1, MgmtSymbolMatcherEntry ME2) {
			SID s1 = new SID(ME1.getSid().get());
			SID s2 = new SID(ME2.getSid().get());
			return s1.compare(s1,s2);
		}
	}

}

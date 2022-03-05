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

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StatTradePanel extends BasePanel {

	private TableModel<StatTradePanel.MarketEntry> mMktTableModel;
	private Table mMktTable;

	private TableModel<StatTradePanel.SymbolEntry> mSidTableModel;
	private Table mSidTable;

	StatTradePanel(ServiceInterface pServiceInterface) {
		super(pServiceInterface);
		init();
	}

	private void init()
	{
		this.setLayout( new BorderLayout() );
		this.setBackground( PANEL_BACKGROUND );

		// Create and add market stat tabel
		JPanel tMarketPanel = new JPanel( new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = gc.gridy = 0;
		gc.anchor = GridBagConstraints.CENTER;
		gc.insets = new Insets(20,0,0,0);

		JLabel tMktLabel = new JLabel("Market Trade Statistics");
		tMktLabel.setFont( new Font("Arial", Font.BOLD, 15));
		tMarketPanel.add( tMktLabel, gc );


		mMktTableModel = new TableModel(StatTradePanel.MarketEntry.class);
		mMktTable = new Table(mMktTableModel, new Dimension(mMktTableModel.getPreferedWith() + 18, 100), null);
		mMktTable.setBackground(Color.white);
		mMktTable.setFont(new Font("Arial", Font.PLAIN, 14));

		JPanel tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.add(mMktTable);

		gc.insets = new Insets(8,0,0,0);
		gc.gridy++;
		tMarketPanel.add( tTablePanel, gc );
		this.add( tMarketPanel, BorderLayout.NORTH );


		// Create and add symbol stat tabel
		JPanel tSidPanel = new JPanel( new GridBagLayout());

		JLabel tSidLabel = new JLabel("Symbol Trade Statistics");
		tSidLabel.setFont( new Font("Arial", Font.BOLD, 15));
		gc.insets = new Insets(20,0,0,0);
		gc.gridx = gc.gridy = 0;
		tSidPanel.add( tSidLabel, gc );


		mSidTableModel = new TableModel(StatTradePanel.SymbolEntry.class);
		mSidTable = new Table(mSidTableModel, new Dimension(mSidTableModel.getPreferedWith() + 18, 240), null);
		mSidTable.setBackground(Color.white);
		mSidTable.setFont(new Font("Arial", Font.PLAIN, 14));

		gc.gridy++;
		gc.insets = new Insets(8,0,0,0);

		tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.add(mSidTable);
		tSidPanel.add( tTablePanel, gc );
		this.add(tSidPanel, BorderLayout.CENTER);
	}

	public void refreshStatistics() {
		MgmtQueryTradeRequest tReq = new MgmtQueryTradeRequest().setRef("qts");
		MgmtQueryTradeResponse tResp = (MgmtQueryTradeResponse) mServiceInterface.transceive(TeService.TradeData.name(), tReq );

		mMktTableModel.clear();
		List<MgmtMarketTradeEntry> tMktList = tResp.getMarkets().get();
		Collections.sort( tMktList, new MktSorter());

		for( MgmtMarketTradeEntry tMktEntry : tResp.getMarkets().get()) {
			mMktTableModel.addEntry( new MarketEntry( tMktEntry ));
		}

		mSidTableModel.clear();
		if (tResp.getSids().isPresent()) {
			List<MgmtSymbolTradeEntry> tSidList = tResp.getSids().get();
			Collections.sort(tSidList, new SidSorter());

			for (MgmtSymbolTradeEntry tSidEntry : tResp.getSids().get()) {
				mSidTableModel.addEntry(new SymbolEntry(tSidEntry));
			}
		}




		this.revalidate();
		this.repaint();
	}



	public class MarketEntry {
		MgmtMarketTradeEntry mMarketEntry;

		public MarketEntry(MgmtMarketTradeEntry pMarketEntry ) {
			mMarketEntry = pMarketEntry;
		}


		@TableAttribute(header = "Market", column = 1, width = 100, alignment = JLabel.LEFT)
		public String getMarket() {
			return mMarketEntry.getMarket().get();
		}

		@TableAttribute(header = "Trades", column = 2, width = 80, alignment = JLabel.RIGHT)
		public String getTradesCount() {
			return String.valueOf(mMarketEntry.getTrades().get());
		}

		@TableAttribute(header = "Volume", column = 3, width = 80, alignment = JLabel.RIGHT)
		public String getVolumeCount() {
			return String.valueOf( mMarketEntry.getVolume().get());
		}

		@TableAttribute(header = "Turnover", column = 4, width = 80, alignment = JLabel.RIGHT)
		public String getTurnover() {
			NumberFormat nf = NumberFormat.getInstance();
			nf.setGroupingUsed(true); nf.setMinimumFractionDigits(0); nf.setMaximumFractionDigits(0);
			return nf.format( mMarketEntry.getTurnover().get());
		}
	}

	public class SymbolEntry {
		MgmtSymbolTradeEntry mSidEntry;

		public SymbolEntry(MgmtSymbolTradeEntry pSidEntry ) {
			mSidEntry = pSidEntry;
		}


		@TableAttribute(header = "SID", column = 1, width = 100, alignment = JLabel.LEFT)
		public String getSid() {
			return mSidEntry.getSid().get();
		}

		@TableAttribute(header = "Trades", column = 2, width = 80, alignment = JLabel.RIGHT)
		public String getTradesCount() {
			return String.valueOf(mSidEntry.getTrades().get());
		}

		@TableAttribute(header = "Volume", column = 3, width = 80, alignment = JLabel.RIGHT)
		public String getVolumeCount() {
			return String.valueOf( mSidEntry.getVolume().get());
		}

		@TableAttribute(header = "Turnover", column = 4, width = 80, alignment = JLabel.RIGHT)
		public String getTurnover() {
			NumberFormat nf = NumberFormat.getInstance();
			nf.setGroupingUsed(true); nf.setMinimumFractionDigits(0); nf.setMaximumFractionDigits(0);
			return nf.format( mSidEntry.getTurnover().get());
		}

		@TableAttribute(header = "Avg Price", column = 5, width = 80, alignment = JLabel.RIGHT)
		public String getAvgPrice() {
			NumberFormat nf = NumberFormat.getInstance();
			nf.setGroupingUsed(false); nf.setMinimumFractionDigits(2); nf.setMaximumFractionDigits(2);
			String tPrice = (mSidEntry.getAveragePrice().isEmpty()) ? "---" : nf.format( mSidEntry.getAveragePrice().get());
			return tPrice;
		}

		@TableAttribute(header = "Min Price", column = 6, width = 80, alignment = JLabel.RIGHT)
		public String getMinPrice() {
			NumberFormat nf = NumberFormat.getInstance();
			nf.setGroupingUsed(false); nf.setMinimumFractionDigits(2); nf.setMaximumFractionDigits(2);
			String tPrice = (mSidEntry.getMinPrice().isEmpty()) ? "---" : nf.format( mSidEntry.getMinPrice().get());
			return tPrice;
		}
		@TableAttribute(header = "Max Price", column = 7, width = 80, alignment = JLabel.RIGHT)
		public String getMaxPrice() {
			NumberFormat nf = NumberFormat.getInstance();
			nf.setGroupingUsed(false); nf.setMinimumFractionDigits(2); nf.setMaximumFractionDigits(2);
			String tPrice = (mSidEntry.getMaxPrice().isEmpty()) ? "---" : nf.format( mSidEntry.getMaxPrice().get());
			return tPrice;
		}

	}

	class SidSorter implements Comparator<MgmtSymbolTradeEntry>
	{
		@Override
		public int compare(MgmtSymbolTradeEntry TE1, MgmtSymbolTradeEntry TE2) {
			SID s1 = new SID(TE1.getSid().get());
			SID s2 = new SID(TE2.getSid().get());
			return s1.compare(s1,s2);
		}
	}

	class MktSorter implements Comparator<MgmtMarketTradeEntry>
	{
		@Override
		public int compare(MgmtMarketTradeEntry mkt1, MgmtMarketTradeEntry mkt2) {
			return (mkt1.getMarketId().get() - mkt2.getMarketId().get());
		}
	}

}

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
import com.hoddmimes.te.management.gui.table.TableCallbackInterface;
import com.hoddmimes.te.management.gui.table.TableModel;
import com.hoddmimes.te.messages.generated.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TradePanel extends JPanel implements TableCallbackInterface {
	ServiceInterface mServiceInterface;
	JPanel mTopPanel;
	JPanel mTablePanel;
	JPanel mButtonPanel;

	TableModel<TradeEntry> mTradeTableModel;
	Table mTradeTable;

	JComboBox<AccountEntry> mAccountComboBox;
	JComboBox<SidEntry> mSidComboBox;

	JButton mRevertTradeBtn;
	List<TradeExecution> mTrades;
	List<Symbol> mSymbols;
	List<Market> mMarkets = null;

	TradeEntry mLatestClickedTrade = null;


	public TradePanel(ServiceInterface pServiceInterface ) {
		this.setLayout(new BorderLayout());

		mTopPanel =  createTopPanel();
		mTablePanel = createTablePanel();
		mButtonPanel = createButtonPanel();
		mServiceInterface = pServiceInterface;

		this.add(mTopPanel, BorderLayout.NORTH);
		this.add( mTablePanel, BorderLayout.CENTER );
		this.add( mButtonPanel, BorderLayout.SOUTH );
	}




	private JPanel createTopPanel()
	{
		JPanel tRoot = new JPanel( new BorderLayout());
		tRoot.setBorder( new EmptyBorder(10,10,10,10));

		JPanel tPanel = new JPanel( new FlowLayout( FlowLayout.CENTER));
		tPanel.setBackground( Management.PANEL_BACKGROUND);
		tPanel.setBorder( new EtchedBorder(3));
		tRoot.add( tPanel, BorderLayout.CENTER);

		mAccountComboBox = new JComboBox<>();
		mAccountComboBox.setFont(Management.DEFAULT_FONT_BOLD);
		tPanel.add( mAccountComboBox );

		mSidComboBox = new JComboBox<>();
		mSidComboBox.setFont(Management.DEFAULT_FONT_BOLD);
		tPanel.add( mSidComboBox );

		mAccountComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				filterTrades();
			}
		});

		mSidComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				filterTrades();
			}
		});

		return tRoot;
	}

	private JPanel createTablePanel() {
		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBorder( new EmptyBorder(10,5,10,5));
		JPanel tContentPanel = new JPanel( new BorderLayout());
		tContentPanel.setBorder( new EtchedBorder(2));
		tRootPanel.add( tContentPanel);

		mTradeTableModel = new TableModel(TradeEntry.class);

		mTradeTable = new Table(mTradeTableModel, new Dimension(mTradeTableModel.getPreferedWith() + 18, 280), this);
		mTradeTable.setBackground(Color.white);

		JPanel tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 5, 10, 5));
		tTablePanel.add(mTradeTable);

		tContentPanel.add(tTablePanel, BorderLayout.CENTER);
		return tRootPanel;
	}

	private void revertTrade(TradeEntry pTrade )
	{
		if (pTrade == null) {
			JOptionPane.showMessageDialog(this,
					"No Trade selected !",
					"No Trade Selected",
					JOptionPane.PLAIN_MESSAGE);
			return;
		}

		MgmtRevertTradeResponse tResponse = (MgmtRevertTradeResponse) mServiceInterface.transceive( TeService.MatchingService.name(), new MgmtRevertTradeRequest().setRef("rt").setTrade( pTrade.getTrade()));
		if (tResponse != null) {
			mTrades.add(tResponse.getRevertedTrades().get());
			filterTrades();
		}
	}


	private JPanel createButtonPanel() {
		JPanel tRootPanel = new JPanel( new FlowLayout( FlowLayout.CENTER));
		tRootPanel.setBorder( new EtchedBorder(2));
		JPanel tContentPanel = new JPanel( new FlowLayout( FlowLayout.CENTER));
		tRootPanel.add( tContentPanel );
		tContentPanel.setBorder( new EmptyBorder(5,5,5,5));
		mRevertTradeBtn = new JButton("Revert Trade");
		mRevertTradeBtn.setFont( Management.DEFAULT_FONT_BOLD);
		mRevertTradeBtn.setBackground( Management.BUTTON_BACKGROUND);
		tContentPanel.add( mRevertTradeBtn );


		mRevertTradeBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				revertTrade( mLatestClickedTrade );
			}
		});


		tContentPanel.setBackground( Management.PANEL_BACKGROUND);
		tRootPanel.setBackground( Management.PANEL_BACKGROUND);
		return tRootPanel;
	}

	public void resizeEvent( Dimension pSize) {
		System.out.println("size: " + this.getSize());
	}


	private void sidChanged() {
		mLatestClickedTrade = null;
	}


	public void loadAccountAndSidData() {

		// Load Account Data
		if (mAccountComboBox.getItemCount() == 0) {
			mAccountComboBox.addItem(new AccountEntry(null));

			MgmtGetAccountsResponse tAccountsResponse = (MgmtGetAccountsResponse) mServiceInterface.transceive(TeService.Autheticator.name(), new MgmtGetAccountsRequest().setRef("ga"));
			if (tAccountsResponse == null) {
				return;
			}

			List<Account> tAccLst = tAccountsResponse.getAccounts().get();
			Collections.sort( tAccLst, new BasePanel.AccountSort());
			for (int i = 0; i < tAccLst.size(); i++) {
				mAccountComboBox.addItem(new AccountEntry(tAccLst.get(i)));
			}
		}

		if (mMarkets == null) {
			MgmtGetMarketsResponse tMktRsp = (MgmtGetMarketsResponse) mServiceInterface.transceive(TeService.InstrumentData.name(), new MgmtGetMarketsRequest().setRef("gm"));
			mMarkets = tMktRsp.getMarkets().get();

			mSidComboBox.addItem(new SidEntry(null));
			for( Market mkt : mMarkets) {
				// Load SID Data
				MgmtGetSymbolsResponse tSymRsp = (MgmtGetSymbolsResponse) mServiceInterface.transceive(TeService.InstrumentData.name(), new MgmtGetSymbolsRequest().setRef("gs").setMarketId( mkt.getId().get()));

				if (tSymRsp == null) {
					return;
				}
				List<Symbol> tSymbols = tSymRsp.getSymbols().get();
				for (Symbol s : tSymbols) {
					mSidComboBox.addItem(new SidEntry(s));
				}
			}
		}
		loadTradeData();
	}


	public void loadTradeData() {

			MgmtGetTradesResponse tTradeResponse = (MgmtGetTradesResponse) mServiceInterface.transceive(TeService.TradeData.name(), new MgmtGetTradesRequest().setRef("gt"));
			if (tTradeResponse == null) {
				return;
			}

			mTrades = tTradeResponse.getTrades().orElse( new ArrayList<>());
			filterTrades();
	}



	private void filterTrades() {
		mLatestClickedTrade = null;
		mTradeTableModel.clear();
		if (mTrades == null) {
			return;
		}

		AccountEntry tAccount = (AccountEntry) mAccountComboBox.getSelectedItem();
		SidEntry tSid = (SidEntry) mSidComboBox.getSelectedItem();
		for( TradeExecution tTrade : mTrades ) {
			if ((tSid.getSid() == null) || (tSid.getSid().contentEquals(tTrade.getSid().get()))) {
				if ((tAccount.getAccountId() == null) || (tAccount.getAccountId().contentEquals(tTrade.getBuyer().get())) ||
						(tAccount.getAccountId().contentEquals(tTrade.getSeller().get()))) {
					mTradeTableModel.addEntry( new TradeEntry( tTrade ));
				}
			}
		}
		mTradeTableModel.fireTableDataChanged();
		this.validate();
		this.repaint();
	}


	@Override
	public void tableMouseButton2(Object pObject, int pRow, int pCol) {

	}

	@Override
	public void tableMouseClick(Object pObject, int pRow, int pCol) {
		//System.out.println("mouse click " + pRow + " " + pCol );
		mLatestClickedTrade = (TradeEntry) pObject;
	}

	@Override
	public void tableMouseDoubleClick(Object pObject, int pRow, int pCol) {

	}


	public static class TradeEntry {
		static SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss.SSS");
		NumberFormat          nbf;
		public TradeExecution mTrade;


		public TradeEntry(TradeExecution pTrade ) {
			mTrade = pTrade;
			nbf = NumberFormat.getInstance(Locale.US);
			nbf.setMaximumFractionDigits(2);
			nbf.setMinimumFractionDigits(2);
			nbf.setGroupingUsed(false);
		}

		public String toString() {
			return "sid: " + mTrade.getSid().get() + " " + mTrade.getQuantity().get() + "@" + PrcFmt.format( mTrade.getPrice().get()) +
					" buyer: " + mTrade.getBuyer().get() + " seller: "+ mTrade.getSeller().get() + " time: " + sdfTime.format(mTrade.getTradeTime());
		}

		@TableAttribute(header = "SID", column = 1, width = 100, alignment = JLabel.LEFT)
		public String getSid() {
			return mTrade.getSid().get();
		}

		@TableAttribute(header = "Time", column = 2, width = 70, alignment = JLabel.LEFT)
		public String getTime() {
			return sdfTime.format(mTrade.getTradeTime().get());
		}

		@TableAttribute(header = "Price", column = 3, width = 50)
		public String getPrice() {
			return PrcFmt.format( mTrade.getPrice().get());
		}

		@TableAttribute(header = "Quantity", column = 4, width = 70)
		public String getQuantity() {
			return String.valueOf(mTrade.getQuantity().get());
		}

		@TableAttribute(header = "Buyer", column = 5, width = 120)
		public String getBuyer() {
			return String.valueOf(mTrade.getBuyer().get());
		}

		@TableAttribute(header = "Seller", column = 6, width = 120)
		public String getSeller() {
			return String.valueOf(mTrade.getSeller().get());
		}

		@TableAttribute(header = "TradeNo", column = 7, width = 132)
		public String getTradeNo() {
			return Long.toHexString(mTrade.getTradeId().get());
		}

		public TradeExecution getTrade() {
			return mTrade;
		}
	}


	class SidEntry
	{
		String mSid;

		SidEntry( Symbol pSymbol ) {
			mSid = (pSymbol == null) ? null : pSymbol.getSid().get();
		}

		String getSid() {
			return mSid;
		}
		public String toString() {
			return (mSid == null) ? "" : mSid ;
		}
	}


	class AccountEntry
	{
		Account mAccount;

		AccountEntry( Account pAccount ) {
			mAccount = pAccount;
		}

		String getAccountId() {
			return (mAccount == null) ? null : mAccount.getAccountId().get();
		}

		public String toString() {
			return (mAccount == null) ? "" : mAccount.getAccountId().get() ;
		}
	}
}
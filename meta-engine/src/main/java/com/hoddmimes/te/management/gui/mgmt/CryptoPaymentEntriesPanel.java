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

import com.hoddmimes.te.common.interfaces.TeIpcServices;
import com.hoddmimes.te.management.gui.table.Table;
import com.hoddmimes.te.management.gui.table.TableAttribute;
import com.hoddmimes.te.management.gui.table.TableModel;
import com.hoddmimes.te.messages.generated.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CryptoPaymentEntriesPanel extends BasePanel {
	private enum CoinType {All,BTC,ETH};
	CryptoPanel mCryptoPanel;

	JPanel mTopPanel;
	JPanel mTablePanel;

	JComboBox mAccountComboBox;
	JComboBox mCoinComboBox;

	TableModel<CryptoPaymentAddressEntry> mPaymentTableModel;
	Table mPaymentTable;



	public CryptoPaymentEntriesPanel(CryptoPanel pCryptoPanel ) {
		super(pCryptoPanel.getServiceInterface());
		mCryptoPanel = pCryptoPanel;
		this.setLayout(new BorderLayout());

		mTopPanel = createTopPanel();
		mTablePanel = createTablePanel();

		this.add( mTopPanel, BorderLayout.NORTH );
		this.add( mTablePanel, BorderLayout.CENTER );
	}

	private JPanel createTopPanel()
	{
		JPanel tRoot = new JPanel( new BorderLayout());
		tRoot.setBorder( new EmptyBorder(10,10,10,10));

		JPanel tPanel = new JPanel( new GridBagLayout());
		tPanel.setBackground( Management.PANEL_BACKGROUND);
		tPanel.setBorder( new EtchedBorder(3));
		tRoot.add( tPanel, BorderLayout.CENTER);

		GridBagConstraints gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.CENTER;
		gc.insets = new Insets(10,0,10,0);
		gc.gridx = gc.gridy = 0;

		tPanel.add(makeLabel("Account"), gc );
		gc.insets.left = 10;
		gc.gridx++;

		String tCoinModel[] = { CoinType.All.name(), CoinType.BTC.name(), CoinType.ETH.name() };
		mAccountComboBox = new JComboBox<>(tCoinModel);
		mAccountComboBox.setFont(Management.DEFAULT_FONT_BOLD);
		tPanel.add( mAccountComboBox, gc  );

		gc.insets.left = 20;
		gc.gridx++;
		tPanel.add(makeLabel("Coin"), gc );

		gc.insets.left = 10;
		gc.gridx++;
		String tCoinsModel[] = { CoinType.All.name(), CoinType.BTC.name(), CoinType.ETH.name() };
		mCoinComboBox = new JComboBox<>(tCoinsModel);
		mCoinComboBox.setFont(Management.DEFAULT_FONT_BOLD);
		tPanel.add( mCoinComboBox, gc  );

		mAccountComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				accountChanged();
			}
		});

		mCoinComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				coinChanged();
			}
		});

		return tRoot;
	}


	private void accountChanged() {
		loadPaymentEntries();
	}

	private void coinChanged() {
		loadPaymentEntries();
	}


	private JPanel createTablePanel() {
		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBorder( new EmptyBorder(10,5,10,5));
		JPanel tContentPanel = new JPanel( new BorderLayout());
		tContentPanel.setBorder( new EtchedBorder(2));
		tRootPanel.add( tContentPanel);

		mPaymentTableModel = new TableModel(CryptoPaymentAddressEntry.class);

		mPaymentTable = new Table(mPaymentTableModel, new Dimension(mPaymentTableModel.getPreferedWith() + 18, 380), null);
		mPaymentTable.setBackground(Color.white);

		JPanel tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 5, 10, 5));
		tTablePanel.add(mPaymentTable);

		tContentPanel.add(tTablePanel, BorderLayout.CENTER);
		return tRootPanel;
	}

	public  void loadData( ) {
		mAccountComboBox.setModel( mCryptoPanel.getAccountModel());
		loadPaymentEntries();
	}

	private void loadPaymentEntries() {
		mAccountComboBox.setModel( mCryptoPanel.getAccountModel());

		if (mCryptoPanel.getAccountModel().getSize() == 0) {
			JOptionPane.showMessageDialog(this,
					"No accounts with crypto deposits found",
					"No Crypto Accounts",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		CoinType tCointType = CoinType.valueOf( (String) mCoinComboBox.getSelectedItem());
		MgmtGetCryptoAccountsAddressesRequest tRequest = new MgmtGetCryptoAccountsAddressesRequest().setRef("caa");
		tRequest.setAccountId( (String) mAccountComboBox.getSelectedItem());
		MgmtGetCryptoAccountsAddressesResponse tResponse = (MgmtGetCryptoAccountsAddressesResponse) mCryptoPanel.getServiceInterface().transceive( TeIpcServices.CryptoGwy, tRequest );
		mPaymentTableModel.clear();
		if (tResponse.getPaymentEntries().isPresent()) {
			List<DbCryptoPaymentEntry> tPaymentEntries = tResponse.getPaymentEntries().get();

			for (DbCryptoPaymentEntry tPaymentEntry : tPaymentEntries) {
				switch (tCointType) {
					case All:
						mPaymentTableModel.addEntry(new CryptoPaymentAddressEntry(tPaymentEntry));
						break;
					case BTC:
						if (tPaymentEntry.getCoinType().get().contentEquals(CoinType.BTC.name())) {
							mPaymentTableModel.addEntry(new CryptoPaymentAddressEntry(tPaymentEntry));
						}
						break;
					case ETH:
						if (tPaymentEntry.getCoinType().get().contentEquals(CoinType.ETH.name())) {
							mPaymentTableModel.addEntry( new CryptoPaymentAddressEntry(tPaymentEntry));
						}
						break;
				}
			}
		}
		mPaymentTableModel.fireTableDataChanged();
	}


	public void resizeEvent( Dimension pSize) {
		System.out.println("size: " + this.getSize());
	}

	public static class CryptoPaymentAddressEntry {
		public DbCryptoPaymentEntry mPaymentEntry;
		NumberFormat nbf;


		public CryptoPaymentAddressEntry(DbCryptoPaymentEntry pPaymentEntry ) {
			mPaymentEntry = pPaymentEntry;
			nbf = NumberFormat.getInstance(Locale.US);
			nbf.setMaximumFractionDigits(2);
			nbf.setMinimumFractionDigits(2);
			nbf.setGroupingUsed(false);
		}


		@TableAttribute(header = "Coin", column = 1, width = 80, alignment = JLabel.RIGHT)
		public String getCoinType()
		{
			return mPaymentEntry.getCoinType().get();
		}

		@TableAttribute(header = "Type", column = 2, width = 80, alignment = JLabel.RIGHT)
		public String getAmount() {
			return mPaymentEntry.getActionType().get();
		}

		@TableAttribute(header = "Address", column = 3, width = 240, alignment = JLabel.RIGHT)
		public String getType() {
			return mPaymentEntry.getAddress().get();
		}

		@TableAttribute(header = "Confirm State", column = 4, width = 80, alignment = JLabel.RIGHT)
		public String getState() {
			return String.valueOf(mPaymentEntry.getConfirmed().get());
		}

	}


}
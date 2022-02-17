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

import com.hoddmimes.te.common.db.TEDB;
import com.hoddmimes.te.common.interfaces.TeIpcServices;
import com.hoddmimes.te.management.gui.table.Table;
import com.hoddmimes.te.management.gui.table.TableAttribute;
import com.hoddmimes.te.management.gui.table.TableCallbackInterface;
import com.hoddmimes.te.management.gui.table.TableModel;
import com.hoddmimes.te.messages.generated.*;
import org.bitcoinj.core.Coin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CryptoDepositPanel extends JPanel {
	ServiceInterface mServiceInterface;
	JPanel mTablePanel;

	TableModel<CryptoDepositEntry> mDepositTableModel;
	Table mDepositTable;



	public CryptoDepositPanel(ServiceInterface pServiceInterface ) {
		this.setLayout(new BorderLayout());

		mTablePanel = createTablePanel();
		mServiceInterface = pServiceInterface;

		this.add( mTablePanel, BorderLayout.CENTER );
	}



	private JPanel createTablePanel() {
		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBorder( new EmptyBorder(10,5,10,5));
		JPanel tContentPanel = new JPanel( new BorderLayout());
		tContentPanel.setBorder( new EtchedBorder(2));
		tRootPanel.add( tContentPanel);

		mDepositTableModel = new TableModel(CryptoDepositEntry.class);

		mDepositTable = new Table(mDepositTableModel, new Dimension(mDepositTableModel.getPreferedWith() + 18, 380), null);
		mDepositTable.setBackground(Color.white);

		JPanel tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 5, 10, 5));
		tTablePanel.add(mDepositTable);

		tContentPanel.add(tTablePanel, BorderLayout.CENTER);
		return tRootPanel;
	}

	public  void loadData( ) {
		MgmtGetCryptoDepositAccountsResponse tResponse = (MgmtGetCryptoDepositAccountsResponse) mServiceInterface.transceive( TeIpcServices.CryptoGwy,
															new MgmtGetCryptoDepositAccountsRequest().setRef("cad"));

		mDepositTableModel.clear();
		if (tResponse.getAccounts().isPresent()) {
			List<DbCryptoDeposit> tDepositList = tResponse.getAccounts().get();
			Collections.sort(tDepositList, new CryptoPanel.AccountDepositSort());
			for (DbCryptoDeposit tDeposit : tResponse.getAccounts().get()) {
				mDepositTableModel.addEntry(new CryptoDepositEntry(tDeposit));
			}
		}
	}



	public void resizeEvent( Dimension pSize) {
		System.out.println("size: " + this.getSize());
	}

	public static class CryptoDepositEntry {
		public DbCryptoDeposit mDeposit;
		NumberFormat nbf;


		public CryptoDepositEntry(DbCryptoDeposit pDeposit ) {
			mDeposit = pDeposit;
			nbf = NumberFormat.getInstance(Locale.US);
			nbf.setMaximumFractionDigits(2);
			nbf.setMinimumFractionDigits(2);
			nbf.setGroupingUsed(false);
		}


		@TableAttribute(header = "Account", column = 1, width = 120, alignment = JLabel.LEFT)
		public String getAccountId() {
			return mDeposit.getAccountId().get();
		}

		@TableAttribute(header = "Bitcoins", column = 2, width = 90, alignment = JLabel.RIGHT)
		public String getBitCoins()
		{
			Coin tCoin = Coin.valueOf(mDeposit.findHolding(TEDB.CoinType.BTC.name()).getHolding().get());
			return tCoin.toFriendlyString();
		}

		@TableAttribute(header = "Ether", column = 3, width = 90, alignment = JLabel.RIGHT)
		public String getEther() {
			return "0.00"; //TODO: fix conversion
		}

	}

}
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

import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.interfaces.TeService;
import com.hoddmimes.te.common.table.Table;
import com.hoddmimes.te.common.table.TableAttribute;
import com.hoddmimes.te.common.table.TableModel;
import com.hoddmimes.te.messages.generated.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;

public class PositionPanel extends JPanel  {
	ServiceInterface mServiceInterface;
	JPanel mTopPanel;
	JPanel mTablePanel;
	JPanel mButtonPanel;

	TableModel<PositionEntry> mPositionTableModel;
	Table mOrderTable;

	JComboBox<AccountEntry> mAccountComboBox;
	JTextField mCashTxt;



	public PositionPanel(ServiceInterface pServiceInterface ) {
		this.setLayout(new BorderLayout());

		mTopPanel =  createTopPanel();
		mTablePanel = createTablePanel();
		mServiceInterface = pServiceInterface;

		this.add(mTopPanel, BorderLayout.NORTH);
		this.add( createCashPanel(), BorderLayout.CENTER );
		this.add( mTablePanel, BorderLayout.SOUTH );
	}


	private JPanel createCashPanel() {
		JPanel tRootPanel = new JPanel( new FlowLayout( FlowLayout.CENTER));
		tRootPanel.setBorder( new EtchedBorder(2));
		tRootPanel.setBackground( Management.PANEL_BACKGROUND);

		JPanel tPanel = new JPanel( new GridBagLayout());
		tPanel.setBackground( Management.PANEL_BACKGROUND);
		GridBagConstraints gc = new GridBagConstraints();
		gc.insets = new Insets(10,0,10, 0 );
		gc.gridy = gc.gridx = 0;
		gc.anchor = GridBagConstraints.NORTHWEST;

		JLabel tLbl = new JLabel("Cash Position");
		tLbl.setFont( Management.DEFAULT_FONT );
		tLbl.setBackground( Management.PANEL_BACKGROUND );
		tPanel.add( tLbl, gc );

		gc.insets.left = 15;
		gc.gridx++;

		mCashTxt = new JTextField("");
		mCashTxt.setBackground( Management.TXTFLD_BACKGROUND);
		mCashTxt.setFont( Management.DEFAULT_FONT);
		mCashTxt.setPreferredSize(new Dimension(80,22));

		tPanel.add( mCashTxt, gc );
		tRootPanel.add( tPanel );
		return tRootPanel;
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

		mAccountComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				accountChanged();
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

		mPositionTableModel = new TableModel(PositionEntry.class);

		mOrderTable = new Table(mPositionTableModel, new Dimension(mPositionTableModel.getPreferedWith(), 380), null);
		mOrderTable.setBackground(Color.white);

		JPanel tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 5, 10, 5));
		tTablePanel.add(mOrderTable);

		tContentPanel.add(tTablePanel, BorderLayout.CENTER);
		return tRootPanel;
	}




	public void resizeEvent( Dimension pSize) {
		System.out.println("size: " + this.getSize());
	}

	private void accountChanged() {
		AccountEntry ae = (AccountEntry) mAccountComboBox.getSelectedItem();
		loadPositions( ae.getAccountId(), true );
	}

	private void reloadPositions() {
		AccountEntry ae = (AccountEntry) mAccountComboBox.getSelectedItem();
		loadPositions( ae.getAccountId(), false );
	}



	public void loadAccountData() {
		// Load market data if not already loaded
		if (mAccountComboBox.getItemCount() == 0) {
			MgmtGetAccountsResponse tAccountsResponse = (MgmtGetAccountsResponse) mServiceInterface.transceive(TeService.Autheticator.name(), new MgmtGetAccountsRequest().setRef("ga"));
			if (tAccountsResponse == null) {
				return;
			}
			List<Account> tAccLst = tAccountsResponse.getAccounts().get();
			Collections.sort( tAccLst, new BasePanel.AccountSort());
			for (int i = 0; i < tAccLst.size(); i++) {
					mAccountComboBox.addItem( new AccountEntry( tAccLst.get(i) ));
			}

		}
		AccountEntry tAccountEntry = (AccountEntry) mAccountComboBox.getSelectedItem();
		loadPositions(tAccountEntry.getAccountId(), false );
	}


	void loadPositions( String pAccount, boolean pNoOrderInfo ) {
		MgmtGetAccountPositionsResponse tPositionResponse = (MgmtGetAccountPositionsResponse) mServiceInterface.transceive(TeService.PositionData.name(), new MgmtGetAccountPositionsRequest().setRef("X").setAccount(pAccount));

		mCashTxt.setText("");
		mPositionTableModel.clear();


		if (tPositionResponse == null) {
			return;
		}
		if (!tPositionResponse.getIsDefined().get()) {
			JOptionPane.showMessageDialog(this,
					"No position/deposit defined account \"" + pAccount + "\"",
					"No Position Found",
					JOptionPane.PLAIN_MESSAGE);
		}

		updatePostionModel( tPositionResponse );
	}

	private void updatePostionModel( MgmtGetAccountPositionsResponse pPositionsResponse)
	{
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		nf.setMinimumIntegerDigits(2);
		nf.setGroupingUsed( false );

		mPositionTableModel.clear();
		double tCashHolding = (double) pPositionsResponse.getCash().get() / (double) TeAppCntx.PRICE_MULTIPLER;
		mCashTxt.setText( nf.format(tCashHolding ));
		for( MgmtPositionEntry tPosEntry : pPositionsResponse.getPositions().get()) {
			mPositionTableModel.addEntry( new PositionEntry( tPosEntry.getSid().get(), tPosEntry.getPosition().get()));
		}
		mPositionTableModel.fireTableDataChanged();
		this.revalidate();
		this.repaint();
	}


	public static class PositionEntry {
		public String mSid;
		public long    mPosition;



		public PositionEntry(String pSid, long pPosition ) {
			mSid = pSid;
			mPosition = pPosition;

		}

		public String toString() {
			return "sid: " + mSid + " position: " + mPosition;
		}

		@TableAttribute(header = "Sid", column = 1, width = 200, alignment = JLabel.LEFT)
		public String getSid() {
			return mSid;
		}

		@TableAttribute(header = "Position", column = 2, width = 80, alignment = JLabel.RIGHT)
		public String getPostion() {
			return String.valueOf(mPosition);
		}
	}


	class AccountEntry
	{
		Account mAccount;

		AccountEntry( Account pAccount ) {
			mAccount = pAccount;
		}

		String getAccountId() {
			return mAccount.getAccountId().get();
		}

		public String toString() {
			return mAccount.getAccountId().get() ;
		}
	}
}
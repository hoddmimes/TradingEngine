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
import com.hoddmimes.te.messages.generated.*;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class AccountPanelOld extends JPanel
{
	ServiceInterface mServiceInterface;
	JPanel mContentPanel;
	JScrollPane mScrollPane;


	public AccountPanelOld(ServiceInterface pServiceInterface ) {
		this.setLayout(new BorderLayout());

		mContentPanel = new JPanel (new GridBagLayout());
		mServiceInterface = pServiceInterface;
		mScrollPane = new JScrollPane( mContentPanel );
		mScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		mScrollPane.setPreferredSize(this.getSize());
		this.add( mScrollPane, BorderLayout.CENTER );
	}



	public void resizeEvent( Dimension pSize) {
		//System.out.println("size: " + this.getSize());

		this.setPreferredSize( new Dimension( pSize ));
		this.revalidate();
		mScrollPane.setPreferredSize(new Dimension(this.getWidth(), this.getHeight()));

	}


	public void loadAccountData() {
		MgmtGetAccountsResponse tAccountResponse = (MgmtGetAccountsResponse) mServiceInterface.transceive(TeMgmtServices.Autheticator, new MgmtGetAccountsRequest().setRef("X"));
		if (tAccountResponse != null) {
			displayAccountData( tAccountResponse.getAccounts().get());
		}
	}

	void updateAccount( AccountEntryPanel pAccountEntryPanel ) {
		MgmtSetAccountsRequest tRqst = new MgmtSetAccountsRequest().setRef("sa").
				setSuspended( pAccountEntryPanel.isSuspended()).
				setAccountId( pAccountEntryPanel.getAccountId());

		MgmtSetAccountsResponse tResponse = (MgmtSetAccountsResponse) mServiceInterface.transceive( TeMgmtServices.Autheticator, tRqst );
		if (tResponse != null) {
			pAccountEntryPanel.updateSuspendStatus( tResponse.getAccount().get().getSuspended().get());
		}
	}

	private void displayAccountData( List<Account> pSymbols )
	{
		int tComponentCount = mContentPanel.getComponentCount();
		for (int i = tComponentCount - 1; i >= 0; i--) {
			mContentPanel.remove(i);
		}

		GridBagConstraints gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.insets = new Insets(0,10,0,10);
		gc.gridx = gc.gridy = 0;

		for( Account tAccount : pSymbols) {
			mContentPanel.add( new AccountEntryPanel( tAccount ), gc );
			gc.gridy++;
		}
		this.revalidate();
		this.repaint();
	}

	class AccountEntryPanel extends JPanel
	{
		Account     mAccount;
		JButton     mUpdateBtn;
		JCheckBox   mSuspendedChkBox;

		AccountEntryPanel( Account pAccount ) {
			mAccount = pAccount;
			this.setLayout( new GridBagLayout());
			this.setBorder( new EtchedBorder(2));
			init();
		}

		String getAccountId() {
			return mAccount.getAccount().get();
		}

		private void init() {
			GridBagConstraints gc = new GridBagConstraints();
			gc.anchor = GridBagConstraints.NORTHWEST;
			gc.insets = new Insets(5,10,3,0);
			gc.gridy = gc.gridx = 0;

			this.add( makeLabel("account"), gc);
			gc.gridx++;
			this.add( makeTextFields(mAccount.getAccount().get(), new Dimension(110,22), false), gc );
			gc.gridx++;
			boolean tSuspended = mAccount.getSuspended().get();
			gc.gridx++;
			mSuspendedChkBox = new JCheckBox("Suspended");
			mSuspendedChkBox.setFont( Management.DEFAULT_FONT);
			mSuspendedChkBox.setSelected( tSuspended );
			this.add( mSuspendedChkBox, gc );

			gc.gridx++;
			gc.insets.right = 10;
			mUpdateBtn = new JButton("Update");
			mUpdateBtn.setBackground( Management.BUTTON_BACKGROUND);
			mUpdateBtn.setFont( Management.DEFAULT_FONT_BOLD);
			this.add( mUpdateBtn, gc );
			if (tSuspended) {
				this.setBackground(Management.SUSPENDED_BACKGROUND);
			} else {
				this.setBackground(Management.PANEL_BACKGROUND);
			}
			mUpdateBtn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					AccountPanelOld.this.updateAccount(AccountEntryPanel.this);
				}
			});
		}

		void updateSuspendStatus( boolean pSuspended ) {
			mSuspendedChkBox.setSelected( pSuspended );
			if (pSuspended) {
				this.setBackground(Management.SUSPENDED_BACKGROUND);
			} else {
				this.setBackground(Management.PANEL_BACKGROUND);
			}
		}

		boolean isSuspended() {
			return mSuspendedChkBox.isSelected();
		}


		private JLabel makeLabel( String pText ) {
			JLabel tLbl = new JLabel(pText );
			tLbl.setFont( Management.DEFAULT_FONT_BOLD);
			return tLbl;
		}

		private JTextField makeTextFields( String pText, Dimension pSize, boolean pEditable ) {
			JTextField tf = new JTextField( pText );
			tf.setEditable( pEditable );
			if (!pEditable) {
				tf.setBackground( Management.TXTFLD_BACKGROUND );
			} else {
				tf.setBackground(Color.WHITE);
			}
			tf.setMargin( new Insets(0,8,0,0));
			tf.setFont( Management.DEFAULT_FONT );
			tf.setPreferredSize( pSize );
			return tf;
		}

	}

	class MarketEntry
	{
		Market mMarket;

		MarketEntry( Market pMarket ) {
			mMarket = pMarket;
		}

		int getMarketId() {
			return mMarket.getId().get();
		}

		public String toString() {
			return mMarket.getName().get() + " [ " + mMarket.getId().get() + " ] ";
		}
	}
}
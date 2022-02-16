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
import com.hoddmimes.te.messages.generated.Account;
import com.hoddmimes.te.messages.generated.DbCryptoDeposit;
import com.hoddmimes.te.messages.generated.MgmtGetCryptoDepositAccountsRequest;
import com.hoddmimes.te.messages.generated.MgmtGetCryptoDepositAccountsResponse;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CryptoPanel extends BasePanel
{
	private ServiceInterface mServiceInterface;


	private JTabbedPane mTabbedPane;
	private CryptoWalletPanel           mWalletPanel;
	private CryptoPaymentEntriesPanel   mAddressesPanel;
	private CryptoDepositPanel      mDepositPanel;
	private CryptoPaymentsPanel     mTransfersPanel;
	private CryptoAccountComboModel mCryptoAccountComboModel;



	CryptoPanel(ServiceInterface pServiceInterface ) {
		super( pServiceInterface );
		this.setLayout(new BorderLayout());
		mServiceInterface = pServiceInterface;

		mTabbedPane = new JTabbedPane();

		mWalletPanel = new CryptoWalletPanel( pServiceInterface );
		mAddressesPanel = new CryptoPaymentEntriesPanel( this );
		mDepositPanel = new CryptoDepositPanel(pServiceInterface);
		mTransfersPanel = new CryptoPaymentsPanel(this);
		mCryptoAccountComboModel = new CryptoAccountComboModel();



		mTabbedPane.addTab("Wallets", mWalletPanel );
		mTabbedPane.addTab("Account Deposit", mDepositPanel );
		mTabbedPane.addTab("Account Addresses", mAddressesPanel );
		mTabbedPane.addTab("Account Deposit", mDepositPanel );
		mTabbedPane.addTab("Account Transfers", mTransfersPanel );
		this.add( mTabbedPane, BorderLayout.CENTER);

		mTabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JTabbedPane tabPane = (JTabbedPane) e.getSource();
				Component c = tabPane.getSelectedComponent();
				if (c instanceof CryptoWalletPanel) {
					((CryptoWalletPanel) c).loadData();
				}
				if (c instanceof CryptoDepositPanel) {
					((CryptoDepositPanel) c).loadData();
				}
				if (c instanceof CryptoPaymentsPanel) {
					((CryptoPaymentsPanel) c).loadData();
				}
				if (c instanceof CryptoPaymentEntriesPanel) {
					((CryptoPaymentEntriesPanel) c).loadData();
				}
				tabPane.setSelectedComponent(c);
			}
		});

	}


	ServiceInterface getServiceInterface() {
		return mServiceInterface;
	}
	CryptoAccountComboModel getAccountModel() {
		return mCryptoAccountComboModel;
	}


	void loadData() {
		mWalletPanel.loadData();

		MgmtGetCryptoDepositAccountsResponse tResponse = (MgmtGetCryptoDepositAccountsResponse) mServiceInterface.transceive(TeIpcServices.CryptoGwy,
				new MgmtGetCryptoDepositAccountsRequest().setRef("cad"));
		if (tResponse == null) {
			JOptionPane.showMessageDialog( this, "Crypto gateway is not available" , "No CryptoGwy", JOptionPane.WARNING_MESSAGE );
			return;
		}
		if ((tResponse.getAccounts().isPresent()) && (tResponse.getAccounts().get().size() > 0)) {
			mCryptoAccountComboModel = new CryptoAccountComboModel( tResponse.getAccounts().get() );
		}
	}




	static class AccountDepositSort implements Comparator<DbCryptoDeposit>
	{
		@Override
		public int compare(DbCryptoDeposit A1, DbCryptoDeposit A2) {
			return A1.getAccountId().get().compareTo( A2.getAccountId().get());
		}
	}

	static class CryptoAccountComboModel extends DefaultComboBoxModel<String>
	{
		CryptoAccountComboModel() {
			super();
		}

		CryptoAccountComboModel( List<DbCryptoDeposit> pAccounts ) {
			super();
			this.removeAllElements();
			Collections.sort(pAccounts,  new AccountDepositSort());
			for( DbCryptoDeposit tDepositAccount : pAccounts ) {
				this.addElement( tDepositAccount.getAccountId().get());
			}
		}
	}
}

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
import com.hoddmimes.te.management.gui.table.*;
import com.hoddmimes.te.messages.generated.Account;
import com.hoddmimes.te.messages.generated.MgmtDeleteAccountRequest;
import com.hoddmimes.te.messages.generated.MgmtDeleteAccountResponse;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public class RemoveUpdateAccountPanel extends BasePanel implements TableCallbackInterface, ModelRenderCallback
{


	enum Action {Remove, Update};
	JButton mActionBtn;
	TableModel<AccountEntry>    mAccountTableModel;
	Table                       mAccountTable;
	Action                      mAction;
	AccountEntry                mLatestSelectedAccount;
	AccountsPanel               mAccountsPanel;


	RemoveUpdateAccountPanel(AccountsPanel pAccountsPanel, Action pAction) {
		super(pAccountsPanel.getServiceInterface());
		mAction = pAction;
		mAccountsPanel = pAccountsPanel;

		this.setLayout( new BorderLayout());

		this.add( createTablePanel(), BorderLayout.CENTER);
		this.add( createButtonPanel(), BorderLayout.SOUTH);
	}

	void initData( List<Account> pAccountList ) {
		mAccountTableModel.clear();
		for( Account acc : pAccountList ) {
			mAccountTableModel.addEntry( new AccountEntry( acc ));
		}
		mLatestSelectedAccount = null;
		mAccountTableModel.setRenderCallback( this );
		mAccountTableModel.fireTableDataChanged();
	}

	private JPanel createTablePanel() {
		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBorder( new EmptyBorder(10,5,10,5));
		JPanel tContentPanel = new JPanel( new BorderLayout());
		tContentPanel.setBorder( new EtchedBorder(2));
		tRootPanel.add( tContentPanel);

		mAccountTableModel = new TableModel(AccountEntry.class);

		mAccountTable = new Table(mAccountTableModel, new Dimension(mAccountTableModel.getPreferedWith() + 18, 400), this);
		mAccountTable.setBackground(Color.white);

		JPanel tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 5, 10, 5));
		tTablePanel.add(mAccountTable);

		tContentPanel.add(tTablePanel, BorderLayout.CENTER);
		return tRootPanel;
	}

	void doAction( AccountEntry pAccountEntry ) {
		if (pAccountEntry == null) {
			JOptionPane.showMessageDialog( this, "No account selected", "No Selection", JOptionPane.WARNING_MESSAGE );
			return;
		}

		if (mAction == Action.Remove) {
			removeAccount( pAccountEntry );
		}
		if (mAction == Action.Update) {
			updateAccount( pAccountEntry );
		}
	}

	private void updateAccount( AccountEntry pAccountEntry) {
		AddUpdateAccountPanel tUpdPanel = new AddUpdateAccountPanel( mAccountsPanel, pAccountEntry.mAccount );
		AccountUpdateFrame tUpdFrame = new AccountUpdateFrame( tUpdPanel );
		tUpdFrame.setVisible( true );
	}

	private void removeAccount( AccountEntry pAccountEntry) {
		int tOption = JOptionPane.showConfirmDialog( this, "Do you really whant to delete account " + pAccountEntry.getAccount(), "Delete Account", JOptionPane.OK_CANCEL_OPTION );
		if (tOption == JOptionPane.CANCEL_OPTION) {
			return;
		}
		MgmtDeleteAccountRequest pRequest = new MgmtDeleteAccountRequest().setRef("ra").setAccount( pAccountEntry.mAccount );
		MgmtDeleteAccountResponse tResponse = (MgmtDeleteAccountResponse) mServiceInterface.transceive(TeMgmtServices.Autheticator, pRequest );
		if (tResponse.getIsDeleted().get()) {
			JOptionPane.showMessageDialog( this, "Account successfully deleted " , "Account Deleted", JOptionPane.PLAIN_MESSAGE );
			mAccountsPanel.loadAccountData();
		} else {
			JOptionPane.showMessageDialog( this, "Account could no be deleted " , "Account Not Deleted", JOptionPane.WARNING_MESSAGE );
		}
	}


	private JPanel createButtonPanel() {
		JPanel tRootPanel = new JPanel( new FlowLayout( FlowLayout.CENTER));
		tRootPanel.setBorder( new EtchedBorder(2));
		JPanel tContentPanel = new JPanel( new FlowLayout( FlowLayout.CENTER));
		tRootPanel.add( tContentPanel );
		tContentPanel.setBorder( new EmptyBorder(5,5,5,5));



		mActionBtn = (mAction == Action.Remove) ?  new JButton("Remove Account") : new JButton("Update Account");
		mActionBtn.setFont( Management.DEFAULT_FONT_BOLD);
		mActionBtn.setBackground( Management.BUTTON_BACKGROUND);
		tContentPanel.add(mActionBtn);


		mActionBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doAction( mLatestSelectedAccount );
			}
		});

		tContentPanel.setBackground( Management.PANEL_BACKGROUND);
		tRootPanel.setBackground( Management.PANEL_BACKGROUND);
		return tRootPanel;
	}

	@Override
	public void tableCellRendererComponent(JLabel pCellRenderObject, JTable pTable, Object pValue, int pRow, int pCol) {
		AccountEntry tAccountEntry = mAccountTableModel.getObjectsAtRow( pRow );
		if (tAccountEntry.mAccount.getSuspended().get()) {
			pCellRenderObject.setBackground( Color.orange);
		}
	}


	@Override
	public void tableMouseButton2(Object pObject, int pRow, int pCol) {

	}

	@Override
	public void tableMouseClick(Object pObject, int pRow, int pCol) {
		mLatestSelectedAccount = (AccountEntry) pObject;
	}

	@Override
	public void tableMouseDoubleClick(Object pObject, int pRow, int pCol) {

	}

	public static class AccountEntry {
		Account mAccount;


		public AccountEntry(Account pAccount ) {
			mAccount = pAccount;

		}


		@TableAttribute(header = "Account", column = 1, width = 120, alignment = JLabel.LEFT)
		public String getAccount() {
			return mAccount.getAccount().get();
		}

		@TableAttribute(header = "Password", column = 2, width = 220, alignment = JLabel.LEFT)
		public String getPassword() {
			return mAccount.getPassword().get();
		}

		@TableAttribute(header = "Suspended", column = 3, width = 80, alignment = JLabel.LEFT)
		public String getRef() {
			return String.valueOf(mAccount.getSuspended().get());
		}
	}

}

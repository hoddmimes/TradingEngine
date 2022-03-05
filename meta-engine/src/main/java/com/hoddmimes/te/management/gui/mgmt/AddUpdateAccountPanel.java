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
import com.hoddmimes.te.messages.generated.*;
import com.hoddmimes.te.sessionctl.AccountX;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddUpdateAccountPanel extends BasePanel
{
	private final static String REGEX_EMAIL_VALIDATION = "^[\\w-\\+]+(\\.[\\w]+)*@[\\w-]+(\\.[\\w]+)*(\\.[a-zA-Z]{2,})$";
	private static final Pattern cMailPattern = Pattern.compile(REGEX_EMAIL_VALIDATION);

	enum Action { Add, Update };
	JTextField      mAccountTxt;
	JPasswordField  mPasswordField,mPasswordConfirmField;
	JTextField      mMailAddressTxt;
	JCheckBox       mSuspendedChkBox;
	JCheckBox       mConfirmChkBox;
	JButton         mActionBtn;
	JButton         mCancelBtn;
	Action          mAction;
	AccountsPanel   mAccountsPanel;
	Account         mAccountToUpdate;

	AddUpdateAccountPanel(AccountsPanel pAccountsPanel ) {
		this( pAccountsPanel, null);
	}

	AddUpdateAccountPanel(AccountsPanel pAccountsPanel, Account pAccount) {
		super(pAccountsPanel.getServiceInterface());
		mAccountToUpdate = pAccount;
		mAccountsPanel = pAccountsPanel;

		mAction = (pAccount != null) ? Action.Update :  Action.Add;;
		this.setLayout( new BorderLayout());
		this.add( createInputPanel(), BorderLayout.NORTH);

		if (pAccount != null) {
			mAccountTxt.setText( pAccount.getAccountId().get());
			mAccountTxt.setEditable(false);
			mConfirmChkBox.setSelected( pAccount.getConfirmed().get());
			mSuspendedChkBox.setSelected( pAccount.getSuspended().get());
			mPasswordConfirmField.setText("");
			mPasswordField.setText("");
			mMailAddressTxt.setText(pAccount.getMailAddr().get());
		}
	}

	private boolean doesAccountExist( String pAccountId ) {
		Account tAccount = new Account().setAccountId( pAccountId );
		return mAccountsPanel.doesAccountExist( tAccount );
	}


	void addAccout() {
		if (mAccountTxt.getText().isBlank() || mAccountTxt.getText().isEmpty()) {
			JOptionPane.showMessageDialog( this, "Account must not be emtpty or blank " , "Add Account", JOptionPane.WARNING_MESSAGE );
			return;
		}
		if (mPasswordField.getText().isBlank() || mPasswordField.getText().isEmpty()) {
			JOptionPane.showMessageDialog( this, "Password must not be emtpty or blank " , "Add Account", JOptionPane.WARNING_MESSAGE );
			return;
		}
		if (!mPasswordField.getText().contentEquals(mPasswordConfirmField.getText())) {
			JOptionPane.showMessageDialog( this, "Confirmation password not same " , "Add Account", JOptionPane.WARNING_MESSAGE );
			return;
		}
		if (mMailAddressTxt.getText().isBlank() || mMailAddressTxt.getText().isEmpty()) {
			JOptionPane.showMessageDialog( this, "Mail address must not be emtpty or blank " , "Add Mail", JOptionPane.WARNING_MESSAGE );
			return;
		}

		Matcher m = cMailPattern.matcher( mMailAddressTxt.getText());
		if (!m.matches()) {
			JOptionPane.showMessageDialog( this, "Invalid mail address " , "Add Mail", JOptionPane.WARNING_MESSAGE );
			return;
		}


		Account tSearchAccount = new Account().setAccountId( mAccountTxt.getText());
		if (mAccountsPanel.doesAccountExist(tSearchAccount)) {
			JOptionPane.showMessageDialog( this, "Account already exists" , "Add Account", JOptionPane.WARNING_MESSAGE );
			return;
		}

		Account tAccount = new Account()
				.setSuspended( mSuspendedChkBox.isSelected())
				.setConfirmed( mConfirmChkBox.isSelected())
				.setAccountId( mAccountTxt.getText())
				.setMailAddr( mMailAddressTxt.getText())
				.setPassword(  AccountX.hashPassword(mAccountTxt.getText().toUpperCase() + mPasswordField.getText()));



		MgmtAddAccountRequest tRequest = new MgmtAddAccountRequest().setRef("aa");
		tRequest.setAccount( tAccount );

		MgmtAddAccountResponse tResponse = (MgmtAddAccountResponse) mServiceInterface.transceive(TeService.Autheticator.name(), tRequest);
		if (tResponse.getIsAddded().get()) {
			JOptionPane.showMessageDialog( this, "Account successfully added " , "Account Added", JOptionPane.PLAIN_MESSAGE );
			mAccountsPanel.loadAccountData();
		} else {
			JOptionPane.showMessageDialog( this, "Failed to add acoout \n" + 	tResponse.getStatusMessage().orElse("") , "Add Account", JOptionPane.WARNING_MESSAGE );
		}
	}

	private void updateAccout() {

		if ((!mMailAddressTxt.getText().isEmpty()) && (!mMailAddressTxt.getText().isBlank())) {
			Matcher m = cMailPattern.matcher(mMailAddressTxt.getText());
			if (!m.matches()) {
				JOptionPane.showMessageDialog(this, "Invalid mail address ", "Add Mail", JOptionPane.WARNING_MESSAGE);
				return;
			}
		}


		if ((!mPasswordField.getText().isBlank()) && (!mPasswordField.getText().isEmpty())) {
			if (!mPasswordField.getText().contentEquals(mPasswordConfirmField.getText())) {
				JOptionPane.showMessageDialog(this, "Confirmation password not same ", "Update Account", JOptionPane.WARNING_MESSAGE);
				return;
			}
		}

		// Verify that something is changed
		if (!accountChanged()) {
			JOptionPane.showMessageDialog(this, "Nothing changed for the account ", "Update Account", JOptionPane.WARNING_MESSAGE);
			return;
		}

		MgmtUpdateAccountRequest tRequest = new MgmtUpdateAccountRequest().setRef("ua");

		if (mSuspendedChkBox.isSelected() != mAccountToUpdate.getSuspended().get()) {
			tRequest.setSuspended(mSuspendedChkBox.isSelected());
		}
		if (mConfirmChkBox.isSelected() != mAccountToUpdate.getConfirmed().get()) {
			tRequest.setConfirmed( mConfirmChkBox.isSelected());
		}
		if (!mMailAddressTxt.getText().contentEquals(mAccountToUpdate.getMailAddr().get())) {
			tRequest.setMailAddress( mMailAddressTxt.getText());
		}

		if ((!mPasswordField.getText().isBlank()) && (!mPasswordField.getText().isEmpty())) {
			String tHashPwd = AccountX.hashPassword(mAccountTxt.getText().toUpperCase() + mPasswordField.getText());
			if (!tHashPwd.contentEquals( mAccountToUpdate.getPassword().get())) {
				tRequest.setHashedPassword( tHashPwd );
			}
		}
		tRequest.setAccountId( mAccountTxt.getText());


		MgmtUpdateAccountResponse tResponse = (MgmtUpdateAccountResponse) mServiceInterface.transceive(TeService.Autheticator.name(), tRequest);
		if (tResponse.getIsUpdated().get()) {
			JOptionPane.showMessageDialog( this, "Account successfully updated " , "Account Updates", JOptionPane.PLAIN_MESSAGE );
			mAccountsPanel.loadAccountData();
		} else {
			JOptionPane.showMessageDialog( this, "Failed to update acoout", "Update Account", JOptionPane.WARNING_MESSAGE );
		}
		cancelFrame();
	}

	boolean accountChanged() {
		if (mSuspendedChkBox.isSelected() != mAccountToUpdate.getSuspended().get()) {
			return true;
		}
		if (mConfirmChkBox.isSelected() != mAccountToUpdate.getConfirmed().get()) {
			return true;
		}
		if (!mMailAddressTxt.getText().contentEquals(mAccountToUpdate.getMailAddr().get())) {
			return true;
		}

		if ((!mPasswordField.getText().isBlank()) && (!mPasswordField.getText().isEmpty())) {
			String tHashPwd = AccountX.hashPassword(mAccountTxt.getText().toUpperCase() + mPasswordField.getText());
			if (!tHashPwd.contentEquals( mAccountToUpdate.getPassword().get())) {
				return true;
			}
		}
		return false;
	}


	void cancelFrame() {
		JFrame tFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
		tFrame.dispose();
	}


	private JPanel createInputPanel() {
		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBorder( new EmptyBorder(70,10,10,10));

		JPanel tInPanel = new JPanel( new GridBagLayout());
		tInPanel.setBackground( PANEL_BACKGROUND );
		tInPanel.setBorder( new EtchedBorder(1));
		GridBagConstraints cb = new GridBagConstraints();
		cb.gridx = 0; cb.gridy = 0;
		cb.insets = new Insets(10,10,0,10);
		cb.anchor = GridBagConstraints.NORTHWEST;

		// Username
		JLabel tUsrLbl = new JLabel("Username");
		tUsrLbl.setFont( DEFAULT_FONT );
		tUsrLbl.setPreferredSize( new Dimension(120,22));
		tInPanel.add( tUsrLbl, cb );
		cb.gridx++;

		mAccountTxt = new JTextField();
		mAccountTxt.setFont( DEFAULT_FONT );
		mAccountTxt.setPreferredSize(new Dimension(150,22));
		mAccountTxt.setMargin( new Insets(0,5,0,0));
		tInPanel.add(mAccountTxt, cb );

		// Add Mail Address
		cb.gridx = 0; cb.gridy++;
		JLabel tMailLbl = new JLabel("Mail Address");
		tMailLbl.setFont( DEFAULT_FONT );
		tMailLbl.setPreferredSize( new Dimension(120,22));
		tInPanel.add( tMailLbl, cb );
		cb.gridx++;

		mMailAddressTxt = new JTextField();
		mMailAddressTxt.setFont( DEFAULT_FONT );
		mMailAddressTxt.setPreferredSize(new Dimension(150,22));
		mMailAddressTxt.setMargin( new Insets(0,5,0,0));
		tInPanel.add(mMailAddressTxt, cb );

		// Add Password

		cb.gridx = 0; cb.gridy++;
		JLabel tPwdLbl = new JLabel("Password");
		tPwdLbl.setFont( DEFAULT_FONT );
		tPwdLbl.setPreferredSize( new Dimension(120,22));
		tInPanel.add( tPwdLbl, cb );
		cb.gridx++;

		mPasswordField = new JPasswordField();
		mPasswordField.setFont( DEFAULT_FONT );
		mPasswordField.setPreferredSize(new Dimension(150,22));
		mPasswordField.setMargin( new Insets(0,5,0,0));
		tInPanel.add( mPasswordField, cb );

		cb.gridx= 0; cb.gridy++;
		JLabel tPwdConfirmLbl = new JLabel("Password Confirm");
		tPwdConfirmLbl.setFont( DEFAULT_FONT );
		tPwdConfirmLbl.setPreferredSize( new Dimension(120,22));
		tInPanel.add( tPwdConfirmLbl, cb );
		cb.gridx++;

		mPasswordConfirmField = new JPasswordField();
		mPasswordConfirmField.setFont( DEFAULT_FONT );
		mPasswordConfirmField.setPreferredSize(new Dimension(150,22));
		mPasswordConfirmField.setMargin( new Insets(0,5,0,0));
		tInPanel.add( mPasswordConfirmField, cb );

		// Add Confirmation chkbox
		cb.gridx = 1; cb.gridy++;
		cb.insets = new Insets(20,5,10,10);
		mConfirmChkBox = new JCheckBox("Confirmed");
		mConfirmChkBox.setSelected( false );
		mConfirmChkBox.setFont( DEFAULT_FONT );
		mConfirmChkBox.setBackground( PANEL_BACKGROUND );
		mConfirmChkBox.setPreferredSize(new Dimension(150,22));
		tInPanel.add(mConfirmChkBox, cb );

		// Add Suspend chkbox
		cb.gridx = 1; cb.gridy++;
		cb.insets = new Insets(10,5,10,10);
		mSuspendedChkBox = new JCheckBox("Suspended");
		mSuspendedChkBox.setSelected( false );
		mSuspendedChkBox.setFont( DEFAULT_FONT );
		mSuspendedChkBox.setBackground( PANEL_BACKGROUND );
		mSuspendedChkBox.setPreferredSize(new Dimension(150,22));
		tInPanel.add(mSuspendedChkBox, cb );


		JPanel tBtnPanel = new JPanel( new FlowLayout( FlowLayout.CENTER));
		tBtnPanel.setBackground( PANEL_BACKGROUND );

		if (mAction == Action.Update) {
			mCancelBtn = new JButton("Cancel");
			mCancelBtn.setFont( DEFAULT_FONT);
			mCancelBtn.setBackground( BUTTON_BACKGROUND );
			mCancelBtn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					cancelFrame();
				}
			});
			tBtnPanel.add( mCancelBtn );
		}

		mActionBtn = new JButton( (mAction == Action.Add) ? "Add Account" : "Update Account");
		mActionBtn.setFont( DEFAULT_FONT);
		mActionBtn.setBackground( BUTTON_BACKGROUND );
		tBtnPanel.add(mActionBtn);

		mActionBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (mAction == Action.Add) {
					addAccout();
				}
				if (mAction == Action.Update) {
					updateAccout();
				}
			}
		});

		cb.gridx = 0; cb.gridy++;
		cb.gridwidth = 2;
		cb.fill = GridBagConstraints.HORIZONTAL;
		cb.insets = new Insets(30,10,40,10);

		tInPanel.add( tBtnPanel, cb);

		tRootPanel.add( tInPanel, BorderLayout.CENTER);

		return tRootPanel;
	}


}

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

package com.hoddmimes.te.management.gui;

import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.db.TEDB;
import com.hoddmimes.te.management.gui.table.Table;
import com.hoddmimes.te.management.gui.table.TableAttribute;
import com.hoddmimes.te.management.gui.table.TableCallbackInterface;
import com.hoddmimes.te.management.gui.table.TableModel;
import com.hoddmimes.te.sessionctl.AccountX;
import com.mongodb.client.result.UpdateResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Account extends JFrame implements TableCallbackInterface<Account.AccountEntry> {
	private final String REGEX_EMAIL_VALIDATION = "^[\\w-\\+]+(\\.[\\w]+)*@[\\w-]+(\\.[\\w]+)*(\\.[a-zA-Z]{2,})$";

	private static final Font DEFAULT_FONT = new Font("Arial", Font.PLAIN, 14 );
	TEDB mDb;
	List<com.hoddmimes.te.messages.generated.Account> mAccounts;
	JTextField mAccountTxt;
	JTextField mMailAddrTxt;
	JPasswordField mPasswordField;
	JPasswordField mPasswordConfirmField;
	JCheckBox mSuspendedChkBox;
	JCheckBox mConfirmedChkBox;
	int mSelectedRow = -1;

	JButton mCreatBtn;
	JButton mUpdateBtn;
	JButton mDeleteBtn;



	TableModel<AccountEntry> mAccountTableModel;
	Table mTable;

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}



		Account au = new Account();
		au.parseArguments( args );
		au.init();
	}

	private void parseArguments( String[] args ) {
	}

	private void init() {
		mDb = TeAppCntx.getDatabase();
		loadAccounts();

		JPanel tRootPanel = new JPanel( new BorderLayout());

		tRootPanel.add( createUserInputPanel(), BorderLayout.NORTH);
		tRootPanel.add( createUserTablePanel(), BorderLayout.CENTER);
		tRootPanel.add( createButtonPanel(), BorderLayout.SOUTH);

		mUpdateBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateAccount();
			}
		});

		mDeleteBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				removeAccount();
			}
		});

		mCreatBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				createUser();
			}
		});

		this.setTitle("Trading Engine Add / Update Account");
		this.setContentPane( tRootPanel );
		this.pack();
		this.setVisible(true);
	}

	private void removeAccount() {
		if (mSelectedRow >= 0) {
			AccountEntry tAccountEntry = (AccountEntry) mAccountTableModel.remove(mSelectedRow);
			mAccountTableModel.fireTableDataChanged();
			if (mDb.deleteAccountByAccountId(tAccountEntry.getAccountId()) < 0) {
				JOptionPane.showMessageDialog(this,
						"Failed to remove account", "Delete Account Failed", JOptionPane.WARNING_MESSAGE);
			}
		}
	}
	private void updateAccount() {
		boolean tUpdatePassword = false;

		if (!validateInput()) {
			return;
		}

		String tUsername = mAccountTxt.getText();
		String tMailAddr = mMailAddrTxt.getText();
		String tPassword = mPasswordField.getText();
		String tPasswordConfirm = mPasswordConfirmField.getText();

		if ((!tPasswordConfirm.isEmpty())  && (!tPasswordConfirm.isBlank())) {
			tUpdatePassword = true;
		}

		com.hoddmimes.te.messages.generated.Account tAccount = null;
		for(AccountEntry ae : mAccountTableModel.getObjects()) {
			if (ae.getAccountId().contentEquals( tUsername )) {
				tAccount = ae.getAccount();
				break;
			}
		}
		if (tUpdatePassword) {
			tAccount.setPassword(AccountX.hashPassword(tUsername.toUpperCase() + tPassword));
		}
		tAccount.setMailAddr( tMailAddr );
		tAccount.setConfirmed( mConfirmedChkBox.isSelected() );
		tAccount.setSuspended( mSuspendedChkBox.isSelected() );
		mAccountTableModel.fireTableDataChanged();

		saveUsers( tAccount ); // Save user in data base
	}

	private void createUser() {
		if (!validateInput()) {
			return;
		}

		String tUsername = mAccountTxt.getText();
		String tPassword = mPasswordField.getText();
		String tPasswordConfirm = mPasswordConfirmField.getText();


		if (isUserDefined( tUsername )) {
			JOptionPane.showMessageDialog(this,
					"Username is already taken",
					"Invalid username",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		com.hoddmimes.te.messages.generated.Account tAccount = new com.hoddmimes.te.messages.generated.Account();
		tAccount.setAccountId( tUsername );
		tAccount.setPassword( AccountX.hashPassword( tUsername.toUpperCase() + tPassword ));
		tAccount.setSuspended( mSuspendedChkBox.isSelected());
		tAccount.setConfirmed( mConfirmedChkBox.isSelected());
		mAccounts.add( tAccount );

		mAccountTableModel.addEntry(new AccountEntry( tAccount ));
		saveUsers( tAccount );
	}

	private boolean validateInput() {

		String tUsername = mAccountTxt.getText();
		if (tUsername.isEmpty() || tUsername.isBlank()) {
			JOptionPane.showMessageDialog(this,
					"Account must not be empty or blank",
					"Invalid account",
					JOptionPane.WARNING_MESSAGE);
			return false;
		}
		String tMailAddr = mMailAddrTxt.getText();
		Pattern tMailPattern = Pattern.compile(REGEX_EMAIL_VALIDATION);
		if (tMailAddr.isEmpty() || tMailAddr.isBlank()) {
			JOptionPane.showMessageDialog(this,
					"Mail address must not be empty or blank",
					"Invalid mail address",
					JOptionPane.WARNING_MESSAGE);
			return false;
		}
		Matcher m = tMailPattern.matcher(tMailAddr);
		if (!m.matches()) {
			JOptionPane.showMessageDialog(this,
					"Invalid mail address",
					"Invalid mail address",
					JOptionPane.WARNING_MESSAGE);
			return false;
		}

		String tPassword = mPasswordField.getText();
		if (tPassword.isEmpty() || tPassword.isBlank()) {
			JOptionPane.showMessageDialog(this,
					"Password must not be empty or blank",
					"Invalid account",
					JOptionPane.WARNING_MESSAGE);
			return false;
		}
		String tPasswordConfirm = mPasswordConfirmField.getText();
		if ((!tPasswordConfirm.isEmpty()) && (!tPasswordConfirm.isBlank())) {
			if (!tPasswordConfirm.contentEquals(tPassword)) {
				JOptionPane.showMessageDialog(this,
						"Confirm password must not be same as password",
						"Invalid Password",
						JOptionPane.WARNING_MESSAGE);
				return false;
			}
		}

		return true;
	}




	private com.hoddmimes.te.messages.generated.Account getUser(String pUsername ) {
		for(com.hoddmimes.te.messages.generated.Account tAccount : mAccounts ) {
			if (tAccount.getAccountId().get().contentEquals( pUsername )) {
				return tAccount;
			}
		}
		return null;
	}

	private boolean isUserDefined( String pUsername ) {
		for(com.hoddmimes.te.messages.generated.Account tAccount : mAccounts ) {
			if (tAccount.getAccountId().get().contentEquals( pUsername)) {
				return true;
			}
		}
		return false;
	}

	private void saveUsers(com.hoddmimes.te.messages.generated.Account pAccount) {
		UpdateResult tUpdResult = mDb.updateAccount(pAccount, true);
		if (tUpdResult.getModifiedCount() <= 0) {
			JOptionPane.showMessageDialog(this,
					"Failed to save user to DB", "Invalid username", JOptionPane.WARNING_MESSAGE);
			return;
		}
	}

	private JPanel createButtonPanel() {
		JPanel tRootPanel = new JPanel( new FlowLayout());
		tRootPanel.setBorder( new EmptyBorder(10,10,15,10));

		JPanel tBtnPanel = new JPanel( new GridBagLayout());
		tBtnPanel.setBorder( new EtchedBorder(2));

		mCreatBtn = new JButton("Add User");
		mCreatBtn.setFont( DEFAULT_FONT);

		mUpdateBtn = new JButton("Update User");
		mUpdateBtn.setFont( DEFAULT_FONT );
		mUpdateBtn.setEnabled( false );

		mDeleteBtn = new JButton("Remove User");
		mDeleteBtn.setFont( DEFAULT_FONT );
		mDeleteBtn.setEnabled( false );

		GridBagConstraints gc = new GridBagConstraints();
		gc.gridy = 0; gc.gridx = 0;
		gc.insets = new Insets(10,20,10, 20);

		tBtnPanel.add( mDeleteBtn, gc );
		gc.gridx++;
		tBtnPanel.add( mUpdateBtn, gc );
		gc.gridx++;
		tBtnPanel.add( mCreatBtn, gc  );

		tRootPanel.add( tBtnPanel );
		return tRootPanel;
	}



	private JPanel createUserTablePanel() {
		JPanel tRootPanel = new JPanel( new FlowLayout( FlowLayout.CENTER));
		tRootPanel.setBorder(new EmptyBorder(10, 10, 0, 10));

		// Create Table
		mAccountTableModel = new TableModel(AccountEntry.class);
		for(com.hoddmimes.te.messages.generated.Account tAccount : mAccounts) {
			mAccountTableModel.addEntry( new AccountEntry( tAccount ) );
		}

		mTable = new Table(mAccountTableModel, new Dimension(mAccountTableModel.getPreferedWith() + 20, 220), this);
		mTable.setBackground(Color.white);

		JPanel tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 0, 10, 0));
		tTablePanel.add(mTable);

		tRootPanel.add(tTablePanel, BorderLayout.CENTER);
		return tRootPanel;
	}

	private JPanel createUserInputPanel() {
		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBorder( new EmptyBorder(10,10,10,10));


		JPanel tInPanel = new JPanel( new GridBagLayout());
		tInPanel.setBorder( new EtchedBorder(1));
		GridBagConstraints cb = new GridBagConstraints();
		cb.gridx = 0; cb.gridy = 0;
		cb.insets = new Insets(10,10,0,10);
		cb.anchor = GridBagConstraints.LINE_START;

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

		// Mail Address
		cb.gridx = 0; cb.gridy++;
		JLabel tMailLbl = new JLabel("Mail Address");
		tMailLbl.setFont( DEFAULT_FONT );
		tMailLbl.setPreferredSize( new Dimension(150,22));
		tInPanel.add( tMailLbl, cb );
		cb.gridx++;

		mMailAddrTxt = new JTextField();
		mMailAddrTxt.setFont( DEFAULT_FONT );
		mMailAddrTxt.setPreferredSize(new Dimension(150,22));
		mMailAddrTxt.setMargin( new Insets(0,5,0,0));
		tInPanel.add(mMailAddrTxt, cb );


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

		// Add Suspend checkbox

		cb.gridx = 1; cb.gridy++;
		cb.insets = new Insets(20,10,10,10);
		mSuspendedChkBox = new JCheckBox("Suspended");
		mSuspendedChkBox.setSelected( false );
		mSuspendedChkBox.setFont( DEFAULT_FONT );
		mSuspendedChkBox.setPreferredSize(new Dimension(150,22));
		tInPanel.add(mSuspendedChkBox, cb );

		// Add Confirmation check box
		cb.gridx = 1; cb.gridy++;
		cb.insets = new Insets(10,10,10,10);
		mConfirmedChkBox = new JCheckBox("Confirmed");
		mConfirmedChkBox.setSelected( false );
		mConfirmedChkBox.setFont( DEFAULT_FONT );
		mConfirmedChkBox.setPreferredSize(new Dimension(150,22));
		tInPanel.add(mConfirmedChkBox, cb );


		tRootPanel.add( tInPanel, BorderLayout.SOUTH);
		return tRootPanel;
	}

	private void loadAccounts()  {
		mAccounts = mDb.findAllAccount();
	}




	private File selectConfigurationFile() {

			JFileChooser tFileChooser = new JFileChooser();
			tFileChooser.setMultiSelectionEnabled(false);

			tFileChooser.setCurrentDirectory(new File("./"));
			tFileChooser.setFont(new Font("Arial", Font.PLAIN, 14));
			tFileChooser.setFileFilter(new FileTypeFilter("json", ""));
			int tResult = tFileChooser.showOpenDialog(this);

			if (tResult == JFileChooser.APPROVE_OPTION) {
				return tFileChooser.getSelectedFile();
			}
			if (tResult == JFileChooser.CANCEL_OPTION) {
				System.exit(-1);
			}
			return null;
		}



	@Override
	public void tableMouseButton2(AccountEntry pObject, int pRow, int pCol) {

	}

	@Override
	public void tableMouseClick(AccountEntry pAccountEntry, int pRow, int pCol) {
		if (mSelectedRow < 0) {
			mTable.setSelectedRow( pRow );
			mAccountTxt.setText( pAccountEntry.getAccountId());
			mPasswordField.setText( pAccountEntry.getPassword());
			mMailAddrTxt.setText( pAccountEntry.getMail());
			mSuspendedChkBox.setSelected( Boolean.parseBoolean( pAccountEntry.getSuspended()));
			mConfirmedChkBox.setSelected( Boolean.parseBoolean( pAccountEntry.getConfirmed()));
			mUpdateBtn.setEnabled( true );
			mDeleteBtn.setEnabled( true );
			mCreatBtn.setEnabled( false );
			mAccountTxt.setEditable( false );
			mSelectedRow = pRow;
			mAccountTableModel.fireTableRowsUpdated(pRow,pRow);
		} else {
			//Selected
			mTable.deSelect();
			mAccountTableModel.fireTableRowsUpdated(mSelectedRow,mSelectedRow);
			mUpdateBtn.setEnabled( false );
			mDeleteBtn.setEnabled( false );
			mCreatBtn.setEnabled( true );
			mAccountTxt.setText("");
			mPasswordField.setText("");
			mSuspendedChkBox.setSelected(true);
			mAccountTxt.setEditable( true );
			//mAccountTableModel.doubleClickedClear();
			mSelectedRow = -1;
		}
	}

	@Override
	public void tableMouseDoubleClick(AccountEntry pUserEntry, int pRow, int pCol) {
	}

	class FileTypeFilter extends FileFilter {

			private String mExtension;
			private String mDescription;

			public FileTypeFilter(String pExtension, String pDescription) {
				mExtension = pExtension;
				mDescription = pDescription;
			}

			public boolean accept(File file) {
				if (file.isDirectory()) {
					return true;
				}
				return file.getName().endsWith(mExtension);
			}

			public String getDescription() {
				return mDescription + String.format(" (*.%s)", mExtension);
			}
		}

	public static class AccountEntry {
		com.hoddmimes.te.messages.generated.Account mAccount;


		public AccountEntry(com.hoddmimes.te.messages.generated.Account pAccount) {
			mAccount = pAccount;
		}

		public com.hoddmimes.te.messages.generated.Account getAccount() {
			return mAccount;
		}

		@TableAttribute(header = "Account", column = 1, width = 100, alignment = JLabel.LEFT)
		public String getAccountId() {
			return mAccount.getAccountId().get();
		}

		@TableAttribute(header = "Mail", column = 2, width = 180, alignment = JLabel.LEFT)
		public String getMail() {
			return mAccount.getMailAddr().get();
		}

		@TableAttribute(header = "Password", column = 3, width = 240, alignment = JLabel.LEFT)
		public String getPassword() {
			return mAccount.getPassword().get();
		}

		@TableAttribute(header = "Confirmed", column = 4, width = 65)
		public String getConfirmed() {
			return mAccount.getConfirmed().get().toString();
		}

		@TableAttribute(header = "Suspended", column = 5, width = 65)
		public String getSuspended() {
			return mAccount.getSuspended().get().toString();
		}
	}
}

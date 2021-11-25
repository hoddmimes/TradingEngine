package com.hoddmimes.te.management.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hoddmimes.te.management.gui.table.Table;
import com.hoddmimes.te.management.gui.table.TableAttribute;
import com.hoddmimes.te.management.gui.table.TableCallbackInterface;
import com.hoddmimes.te.management.gui.table.TableModel;
import com.hoddmimes.te.messages.generated.Account;
import com.hoddmimes.te.sessionctl.AccountX;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.StandardCharsets;


public class AddAccount extends JFrame implements TableCallbackInterface<AddAccount.AccountEntry> {
	private static final Font DEFAULT_FONT = new Font("Arial", Font.PLAIN, 14 );
	String mConfigurationFile;
	JsonObject jAccounts;
	JTextField mAccountTxt;
	JPasswordField mPasswordField;
	JCheckBox mEnabledChkBox;

	JButton mCreatBtn;
	JButton mUpdateBtn;


	TableModel<AccountEntry> mUserTableModel;

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}

		AddAccount au = new AddAccount();
		au.parseArguments( args );
		au.init();
	}

	private void parseArguments( String[] args ) {
		int i = 0;
		while( i < args.length) {
			if (args[i].contentEquals("-accountdb")) {
				mConfigurationFile = args[++i];
			}
			i++;
		}
	}

	private void init() {
		loadConfigurationFile();

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

	private void updateAccount() {
		String tUsername = mAccountTxt.getText();
		if (tUsername.isEmpty() || tUsername.isBlank()) {
			JOptionPane.showMessageDialog(this,
					"Account must not be empty or blank",
					"Invalid account",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		String tPassword = mPasswordField.getText();
		if (tUsername.isEmpty() || tUsername.isBlank()) {
			JOptionPane.showMessageDialog(this,
					"Account must not be empty or blank",
					"Invalid account",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		JsonObject jUser = getJsonUser( tUsername );
		jUser.addProperty("password", AccountX.hashPassword(tPassword));
		jUser.addProperty( "enabled", mEnabledChkBox.isSelected());

	    // Update table model
		java.util.List<AccountEntry> tTableUsers =  mUserTableModel.getObjects();
		for( AccountEntry ue : tTableUsers) {
			if (ue.mAccount.contentEquals( tUsername )) {
				ue.mPassword = AccountX.hashPassword(tPassword);
				ue.mEnabled = String.valueOf( mEnabledChkBox.isSelected());
				mUserTableModel.fireTableDataChanged();
				break;
			}
		}

		saveUsers();
	}

	private void createUser() {
		String tUsername = mAccountTxt.getText();
		if (tUsername.isEmpty() || tUsername.isBlank()) {
			JOptionPane.showMessageDialog(this,
					"Username must not be empty or blank",
					"Invalid username",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		String tPassword = mPasswordField.getText();

		if (tUsername.isEmpty() || tUsername.isBlank()) {
			JOptionPane.showMessageDialog(this,
					"Username must not be empty or blank",
					"Invalid username",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (isUserDefined( tUsername )) {
			JOptionPane.showMessageDialog(this,
					"Username is already taken",
					"Invalid username",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		Account tAccount = new Account();
		tAccount.setAccount( tUsername );
		tAccount.setPassword( AccountX.hashPassword( tPassword ));
		tAccount.setEnabled( mEnabledChkBox.isSelected());

		JsonArray jUsrArr = jAccounts.get("accounts").getAsJsonArray();
		jUsrArr.add( tAccount.toJson() );

		mUserTableModel.addEntry(new AccountEntry( tUsername, tAccount.getPassword().get(), tAccount.getEnabled().get()));

		saveUsers();
	}

	private JsonObject getJsonUser( String pUsername ) {
		JsonArray jUsrArr = jAccounts.get("accounts").getAsJsonArray();
		for (int i = 0; i < jUsrArr.size(); i++) {
			JsonObject u = jUsrArr.get(i).getAsJsonObject();
			if (pUsername.contentEquals(u.get("account").getAsString())) {
				return u;
			}
		}
		return null;
	}

	private boolean isUserDefined( String pUsername ) {
		JsonArray jUsrArr = jAccounts.get("accounts").getAsJsonArray();
		for (int i = 0; i < jUsrArr.size(); i++) {
			JsonObject u = jUsrArr.get(i).getAsJsonObject();
			if (pUsername.contentEquals(u.get("account").getAsString())) {
				return true;
			}
		}
		return false;
	}

	private void saveUsers() {
		try {
			FileOutputStream tOut = new FileOutputStream(  mConfigurationFile );
			tOut.write( jAccounts.toString().getBytes(StandardCharsets.UTF_8));
			tOut.flush();
			tOut.close();
		}
		catch( IOException e) {
			e.printStackTrace();
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

		GridBagConstraints gc = new GridBagConstraints();
		gc.gridy = 0; gc.gridx = 0;
		gc.insets = new Insets(10,20,10, 20);

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
		mUserTableModel = new TableModel(AccountEntry.class);

		JsonArray tUsrArr = jAccounts.get("accounts").getAsJsonArray();
		for (int i = 0; i < tUsrArr.size(); i++) {
			JsonObject u = tUsrArr.get(i).getAsJsonObject();
			AccountEntry ue = new AccountEntry(u.get("account").getAsString(), u.get("password").getAsString(), u.get("enabled").getAsBoolean());
			mUserTableModel.addEntry( ue );
		}



		Table tTable = new Table(mUserTableModel, new Dimension(mUserTableModel.getPreferedWith(), 140), this);
		tTable.setBackground(Color.white);

		JPanel tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 0, 10, 0));
		tTablePanel.add(tTable);

		tRootPanel.add(tTablePanel, BorderLayout.CENTER);
		return tRootPanel;
	}

	private JPanel createUserInputPanel() {
		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBorder( new EmptyBorder(10,10,10,10));

		JPanel tConfigFilePanel = new JPanel( new FlowLayout( FlowLayout.CENTER));
		tConfigFilePanel.setBorder( new EmptyBorder(10,10,10,10));
		JPanel tConfigFilePanelBorder = new JPanel( new FlowLayout( FlowLayout.CENTER));
		tConfigFilePanelBorder.setBorder(new EtchedBorder(1));
		JLabel tCfgFileLbl = new JLabel("User definition file \"" + mConfigurationFile + "\"");
		tCfgFileLbl.setFont( new Font("Arial", Font.BOLD, 14 ));
		tConfigFilePanelBorder.add( tCfgFileLbl );
		tConfigFilePanel.add( tConfigFilePanelBorder );
		tRootPanel.add( tConfigFilePanel, BorderLayout.NORTH );

		JPanel tInPanel = new JPanel( new GridBagLayout());
		tInPanel.setBorder( new EtchedBorder(1));
		GridBagConstraints cb = new GridBagConstraints();
		cb.gridx = 0; cb.gridy = 0;
		cb.insets = new Insets(10,10,0,10);
		cb.anchor = GridBagConstraints.LINE_START;

		// Username
		JLabel tUsrLbl = new JLabel("Username");
		tUsrLbl.setFont( DEFAULT_FONT );
		tUsrLbl.setPreferredSize( new Dimension(66,22));
		tInPanel.add( tUsrLbl, cb );
		cb.gridx++;

		mAccountTxt = new JTextField();
		mAccountTxt.setFont( DEFAULT_FONT );
		mAccountTxt.setPreferredSize(new Dimension(150,22));
		mAccountTxt.setMargin( new Insets(0,5,0,0));
		tInPanel.add(mAccountTxt, cb );

		cb.gridx = 0; cb.gridy++;
		JLabel tPwdLbl = new JLabel("Password");
		tPwdLbl.setFont( DEFAULT_FONT );
		tPwdLbl.setPreferredSize( new Dimension(66,22));
		tInPanel.add( tPwdLbl, cb );
		cb.gridx++;

		mPasswordField = new JPasswordField();
		mPasswordField.setFont( DEFAULT_FONT );
		mPasswordField.setPreferredSize(new Dimension(150,22));
		mPasswordField.setMargin( new Insets(0,5,0,0));
		tInPanel.add( mPasswordField, cb );

		cb.gridx = 0; cb.gridy++;
		cb.insets = new Insets(10,10,10,10);
		mEnabledChkBox = new JCheckBox("Enabled");
		mEnabledChkBox.setSelected( true );
		mEnabledChkBox.setFont( DEFAULT_FONT );
		mEnabledChkBox.setPreferredSize(new Dimension(150,22));
		tInPanel.add( mEnabledChkBox, cb );

		tRootPanel.add( tInPanel, BorderLayout.SOUTH);
		return tRootPanel;
	}

	private void loadConfigurationFile() {
		File tFile = null;

		if (mConfigurationFile != null) {
			tFile = new File(mConfigurationFile);
			if (((!tFile.exists()) || (!tFile.canRead()))) {
				tFile = null;
			}
		}

		while( tFile == null ) {
			tFile = selectConfigurationFile();
			if (((tFile == null) || (!tFile.exists()) || (!tFile.canRead()))) {
				tFile = null;


				JOptionPane.showMessageDialog(this,
						"Can not find or open users definition file",
						"Invalid user definition file",
						JOptionPane.WARNING_MESSAGE);
			}
		}

		try {
			InputStreamReader tReader = new InputStreamReader(new FileInputStream(tFile));
			jAccounts = JsonParser.parseReader(tReader).getAsJsonObject();
			tReader.close();
			mConfigurationFile = tFile.getAbsolutePath();
		}
		catch( IOException e) {
			JOptionPane.showMessageDialog(this,
					"Can not open users definition file, reason: " + e.getMessage(),
					"Invalid user definition file",
					JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
		}
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
	public void tableMouseClick(AccountEntry pObject, int pRow, int pCol) {
		if (!mCreatBtn.isEnabled()) {
			mUpdateBtn.setEnabled( false );
			mCreatBtn.setEnabled( true );
			mAccountTxt.setText("");
			mPasswordField.setText("");
			mEnabledChkBox.setSelected(true);
			mAccountTxt.setEditable( true );
			mUserTableModel.doubleClickedClear();
		}
	}

	@Override
	public void tableMouseDoubleClick(AccountEntry pUserEntry, int pRow, int pCol) {
		mAccountTxt.setText( pUserEntry.mAccount);
		mPasswordField.setText( pUserEntry.mPassword);
		mEnabledChkBox.setSelected( Boolean.parseBoolean( pUserEntry.mEnabled));
		mUpdateBtn.setEnabled( true );
		mCreatBtn.setEnabled( false );
		mAccountTxt.setEditable( false );
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

		public String mAccount;
		public String mPassword;
		public String mEnabled;


		public AccountEntry(String pAccount, String pPassword, boolean pEnabled ) {
			mAccount = pAccount;
			mPassword = pPassword;
			mEnabled = String.valueOf( pEnabled );
		}


		@TableAttribute(header = "Account", column = 1, width = 100, alignment = JLabel.LEFT)
		public String getAccount() {
			return mAccount;
		}

		@TableAttribute(header = "Password", column = 2, width = 240, alignment = JLabel.LEFT)
		public String getPassword() {
			return mPassword;
		}

		@TableAttribute(header = "Enabled", column = 3, width = 65)
		public String getEnabled() {
			return mEnabled;
		}

	}
}

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
import com.hoddmimes.te.common.table.Table;
import com.hoddmimes.te.common.table.TableAttribute;
import com.hoddmimes.te.common.table.TableCallbackInterface;
import com.hoddmimes.te.common.table.TableModel;
import com.hoddmimes.te.messages.generated.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MsglogPanel extends JPanel implements TableCallbackInterface {

	String mMaxLinesValues[] = {"10","50","100","200","500","1000","MAX"};

	SimpleDateFormat SDFTime = new SimpleDateFormat("HH:mm:ss.SSS");

	ServiceInterface mServiceInterface;
	JPanel mTopPanel;
	JPanel mTablePanel;


	TableModel<LogEntry> mLogTableModel;
	Table mLogTable;

	JComboBox<AccountEntry> mAccountComboBox;

	JTextField mTimeTxtFld;
	JTextField mSearchField;
	JTextField mDateTxtFld;
	JButton mRefreshBtn;
	JComboBox<String> mMaxLinesComboBox;


	public MsglogPanel(ServiceInterface pServiceInterface ) {
		this.setLayout(new BorderLayout());

		mTopPanel =  createTopPanel();
		mTablePanel = createTablePanel();

		mServiceInterface = pServiceInterface;

		this.add(mTopPanel, BorderLayout.NORTH);
		this.add( mTablePanel, BorderLayout.CENTER );
	}




	private JPanel createTopPanel() {
		JPanel tRoot = new JPanel(new BorderLayout());
		tRoot.setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel tPanel = new JPanel(new GridBagLayout());
		tPanel.setBackground(Management.PANEL_BACKGROUND);
		tPanel.setBorder(new EtchedBorder(3));
		tRoot.add(tPanel, BorderLayout.CENTER);

		GridBagConstraints gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.gridx = gc.gridy = 0;
		gc.insets = new Insets(10, 10, 10, 0);
		tPanel.add(createFilterPanel(), gc);

		gc.gridy++;
		gc.gridx = 0;
		tPanel.add(createSearchPanel(), gc);

		return tRoot;
	}

	private JPanel createFilterPanel() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		JPanel tPanel = new JPanel( new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.gridx = gc.gridy = 0;
		gc.insets = new Insets(0, 10, 0, 0);

		tPanel.add(makeLabel("Account"), gc);
		gc.gridx++;
		gc.insets.left = 32;

		mAccountComboBox = new JComboBox<>();
		mAccountComboBox.setFont(Management.DEFAULT_FONT_BOLD);
		tPanel.add(mAccountComboBox, gc);


		gc.gridx++;
		gc.insets.left = 32;
		tPanel.add( makeLabel("Date"), gc );
		gc.gridx++;
		gc.insets.left = 10;
		mDateTxtFld = makeTextFields(sdf.format(System.currentTimeMillis()), new Dimension(96,22), true);
		tPanel.add( mDateTxtFld, gc );

		gc.gridx++;
		gc.insets.left = 32;
		tPanel.add(makeLabel("StartWith Time"), gc);
		gc.gridx++;
		gc.insets.left = 10;
		mTimeTxtFld = makeTextFields("", new Dimension(90, 22), true);
		mTimeTxtFld.setToolTipText("HH:mm:ss");
		tPanel.add(mTimeTxtFld, gc);
		return tPanel;

	}

	private JPanel createSearchPanel() {


		JPanel tPanel = new JPanel( new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.gridx = gc.gridy = 0;
		gc.insets = new Insets(0, 10, 0, 0);

		tPanel.add( makeLabel("In msg search"), gc );
		gc.gridx++;
		mSearchField = makeTextFields("", new Dimension(140,22), true);
		mSearchField.setToolTipText("regular expression");
		tPanel.add( mSearchField, gc );

		gc.gridx++;
		gc.insets.left = 62;
		tPanel.add( makeLabel("Lines"), gc );
		gc.gridx++;
		gc.insets.left = 10;
		mMaxLinesComboBox = new JComboBox<>( mMaxLinesValues);
		mMaxLinesComboBox.setSelectedIndex(1);
		mMaxLinesComboBox.setFont( Management.DEFAULT_FONT);
		tPanel.add( mMaxLinesComboBox, gc );

		mRefreshBtn = new JButton("Refresh");
		mRefreshBtn.setBackground( Management.BUTTON_BACKGROUND);
		mRefreshBtn.setFont( Management.DEFAULT_FONT);
		mRefreshBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadLogMessages();
			}
		});

		gc.gridx++;
		gc.insets.left = 62;
		tPanel.add( mRefreshBtn, gc );

		return tPanel;
	}

	private JPanel createTablePanel() {
		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBorder( new EmptyBorder(10,5,10,5));
		JPanel tContentPanel = new JPanel( new BorderLayout());
		tContentPanel.setBorder( new EtchedBorder(2));
		tRootPanel.add( tContentPanel);

		mLogTableModel = new TableModel(LogEntry.class);

		mLogTable = new Table(mLogTableModel, new Dimension(mLogTableModel.getPreferedWith() + 18, 280), this);
		mLogTable.setBackground(Color.white);

		JPanel tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 5, 10, 5));
		tTablePanel.add(mLogTable);

		tContentPanel.add(tTablePanel, BorderLayout.CENTER);
		return tRootPanel;
	}


	public void resizeEvent( Dimension pSize) {
		this.setPreferredSize( new Dimension(pSize.width, (pSize.height - mTopPanel.getHeight() - 50)) );
		this.revalidate();
		mTablePanel.setPreferredSize(new Dimension(this.getWidth(), (this.getHeight() - mTopPanel.getHeight())));
		mLogTable.setPreferredSize( new Dimension(this.getWidth(), (this.getHeight() - mTopPanel.getHeight())));
		mLogTableModel.fireTableDataChanged();
	}

	private void accountChanged() {
		AccountEntry ae = (AccountEntry) mAccountComboBox.getSelectedItem();
		loadLogMessages();
	}

	private void loadLogMessages() {
		AccountEntry ae = (AccountEntry) mAccountComboBox.getSelectedItem();
		MgmtGetLogMessagesRequest tRqst = new MgmtGetLogMessagesRequest().setRef("glm");
		if (mAccountComboBox.getSelectedItem() != null) {
			tRqst.setAccountFilter(  ((AccountEntry) mAccountComboBox.getSelectedItem()).getAccountId());
		}
		if ((!mSearchField.getText().isEmpty()) && (!mSearchField.getText().isBlank())) {
			tRqst.setMsgFilter( mSearchField.getText());
		}

		try {
			tRqst.setDateFilter(getDateFilter());
			tRqst.setTimeFilter( getTimeFilter());
		}
		catch( Exception e ) {
			return;
		}

		tRqst.setMaxLines( getMaxLines());

		mLogTableModel.clear();
		MgmtGetLogMessagesResponse tResponse = (MgmtGetLogMessagesResponse) mServiceInterface.transceive( TeService.SessionService.name(), tRqst );
		if (tResponse != null) {
			for( MsgLogEntry le : tResponse.getLogMessages().get()) {
				mLogTableModel.addEntry( new LogEntry( le ));
			}
		}
		mLogTableModel.fireTableDataChanged();
		this.validate();
		this.repaint();
	}


	private String getDateFilter() throws Exception{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String tDateStr;

		if ((!mDateTxtFld.getText().isBlank()) && (!mDateTxtFld.getText().isBlank())) {
			tDateStr = mDateTxtFld.getText().trim();
		} else {
			tDateStr = sdf.format( System.currentTimeMillis());
		}

		try {sdf.parse(tDateStr);}
		catch( ParseException pe ) {
			JOptionPane.showMessageDialog(this,
					"Invalid date filter, \"yyyy-MM-dd\"",
					"Invalid Date Format",
					JOptionPane.WARNING_MESSAGE);
			throw new Exception("Invalid date timeformat");
		}
		return tDateStr;
	}

	private String getTimeFilter() throws Exception {
		String tTimStr = "";
		if ((!mTimeTxtFld.getText().isBlank()) && (!mTimeTxtFld.getText().isEmpty())) {
			tTimStr = mTimeTxtFld.getText();
		} else {
			return null;
		}

		if (tTimStr.length() < "00:00:00".length()) {
			int tLen = tTimStr.length();
			tTimStr += "00:00:00".substring(tLen, "00:00:00".length());
		}

		Pattern timPattern = Pattern.compile("^[0-2][0-9]:[0-5][0-9]:[0-5][0-9]");
		Matcher m = timPattern.matcher(tTimStr);
		if ((tTimStr.length() != "00:00:00".length()) || (!m.matches())) {
			JOptionPane.showMessageDialog(this,
					"Invalid time filter, \"HH:mm:ss\"",
					"Invalid Time Format",
					JOptionPane.WARNING_MESSAGE);
			throw new Exception("Invalid date timeformat");
		}
		return mTimeTxtFld.getText();
	}


	private int getMaxLines() {
		String tMaxLinesStr = (String) mMaxLinesComboBox.getSelectedItem();
		if (tMaxLinesStr.contentEquals("MAX")) {
			return Integer.MAX_VALUE;
		}

		return Integer.parseInt( tMaxLinesStr );
	}


	public void loadAccountData() {
		// Load market data if not already loaded
		if (mAccountComboBox.getItemCount() == 0) {
			MgmtGetAccountsResponse tAccountsResponse = (MgmtGetAccountsResponse) mServiceInterface.transceive(TeService.Autheticator.name(), new MgmtGetAccountsRequest().setRef("ga"));
			if (tAccountsResponse == null) {
				return;
			}

			mAccountComboBox.addItem(new AccountEntry(null));

			List<Account> tAccLst = tAccountsResponse.getAccounts().get();
			Collections.sort( tAccLst, new BasePanel.AccountSort());
			for (int i = 0; i < tAccLst.size(); i++) {
					mAccountComboBox.addItem( new AccountEntry( tAccLst.get(i) ));
			}

		}
		AccountEntry tAccountEntry = (AccountEntry) mAccountComboBox.getSelectedItem();
		loadLogMessages();
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

	@Override
	public void tableMouseButton2(Object pObject, int pRow, int pCol) {

	}

	@Override
	public void tableMouseClick(Object pObject, int pRow, int pCol) {
	}

	@Override
	public void tableMouseDoubleClick(Object pObject, int pRow, int pCol) {
		LogEntry tLogEntry = (LogEntry) pObject;
		MsgDetailFrame tMsgFrame = new MsgDetailFrame(tLogEntry.mLogEntry);
		tMsgFrame.setVisible(true);
	}


	public  class LogEntry {
		MsgLogEntry mLogEntry;


		public LogEntry(MsgLogEntry pLogEntry ) {
			mLogEntry = pLogEntry;
		}


		@TableAttribute(header = "time", column = 1, width = 90, alignment = JLabel.LEFT)
		public String getTime() {
			return SDFTime.format( mLogEntry.getTimeStamp().get());
		}

		@TableAttribute(header = "message", column = 2, width = 650, alignment = JLabel.LEFT)
		public String getMessage() {
			return mLogEntry.getLogMsg().get();
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
			return (mAccount == null) ? "    " : mAccount.getAccountId().get() ;
		}
	}
}
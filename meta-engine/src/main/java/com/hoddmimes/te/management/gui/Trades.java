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

import com.google.gson.JsonObject;
import com.hoddmimes.jaux.txlogger.TxLoggerFactory;
import com.hoddmimes.jaux.txlogger.TxLoggerReplayEntry;
import com.hoddmimes.jaux.txlogger.TxLoggerReplayInterface;
import com.hoddmimes.jaux.txlogger.TxLoggerReplayIterator;
import com.hoddmimes.te.engine.InternalTrade;
import com.hoddmimes.te.management.gui.table.Table;
import com.hoddmimes.te.management.gui.table.TableAttribute;
import com.hoddmimes.te.management.gui.table.TableCallbackInterface;
import com.hoddmimes.te.management.gui.table.TableModel;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Trades  extends JFrame implements TableCallbackInterface<Trades.TradeEntry>
{
	private enum Side { BUY, SELL};
	enum FilterAttribute { SID, Account, Time, OrderId, TradeId };
	enum ExportFormat {EXCEL,JSON,HTML};
	private NumberFormat nfmt;
	private String mTradeLogDirectory = null;
	private int mFractionDigits = 2;
	private List<File> mTxlFiles = null;
	private List<String> mTxlDates = null;

	private JButton mBrowseBtn;
	private JLabel mTradeLogDirLbl;
	private JComboBox<String> mDatesComboBox;

	private JButton      mExportBtn;
	private JTextField  mDestinationFileTxt;
	private JButton  mDestinationBrowseBtn;
	private JComboBox<ExportFormat> mFormatComboBox;

	private JButton      mFilterBtn;
	private JButton      mFilterClearBtn;
	private JTextField   mFilterTxt;
	private JComboBox<FilterAttribute> mFilterFieldsComboBox;


	TableModel<Trades.TradeEntry> mTradeTableModel;
	Table mTable;

	Trades() {

	}


	public static void main(String[] args)
	{
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}

		Trades t = new Trades();
		t.parseParameters( args );
		t.init();
	}

	private void parseParameters( String[] args ) {
		int i = 0;
		while( i < args.length) {
			if (args[i].contentEquals("-tradeDir")) {
				mTradeLogDirectory = args[++i];
			}
			if (args[i].contentEquals("-decimals")) {
				mFractionDigits = Integer.parseInt(args[++i]);
			}
			i++;
		}

		nfmt = NumberFormat.getNumberInstance();
		nfmt.setMaximumFractionDigits( mFractionDigits );
		nfmt.setMinimumFractionDigits( mFractionDigits );
		nfmt.setGroupingUsed(false);
	}

	private void init() {
		examineTradeDir();

		this.setTitle("TE Trade Browser");
		JPanel tRoot = new JPanel( new BorderLayout());

		tRoot.add( initTopPanel(), BorderLayout.NORTH);
		tRoot.add( initTablePanel(), BorderLayout.CENTER);
		tRoot.add( initExportPanel(), BorderLayout.SOUTH);

		while (!loadData()) {
			mTradeLogDirectory = null;
			mTxlFiles = null;
			examineTradeDir();
		}

		this.setContentPane( tRoot );
		this.pack();
		this.setVisible( true );
	}

	private boolean loadData() {

		if ((mTxlFiles == null) || (mTxlFiles.size() == 0)) {
			return false;
		}

		Date tDate = null;
		mTradeTableModel.clear();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try {
			tDate = sdf.parse( mDatesComboBox.getSelectedItem().toString());
		}
		catch( ParseException e) {
			JOptionPane.showMessageDialog(this,
					"Invalid trade date format: " + mDatesComboBox.getSelectedItem().toString(),
					"Invalid trade date format",
					JOptionPane.WARNING_MESSAGE);
			return false;
		}



		TradeFilter tFilter = new TradeFilter( (FilterAttribute) mFilterFieldsComboBox.getSelectedItem(), mFilterTxt.getText());

		TxLoggerReplayInterface txReplay = TxLoggerFactory.getReplayer( mTradeLogDirectory, "trades");
		TxLoggerReplayIterator tItr = txReplay.replaySync(TxLoggerReplayInterface.DIRECTION.Backward, tDate );

		try {
			while (tItr.hasMore()) {
				TxLoggerReplayEntry txEntry = tItr.next();
				String jObjectString = new String(txEntry.getData());
				InternalTrade tTrade = new InternalTrade( jObjectString );
				TradeEntry tTradeEntry = new TradeEntry( tTrade, Side.BUY);
				if (tFilter.filter( tTradeEntry)) {
					mTradeTableModel.addEntry(tTradeEntry);
				}

				tTradeEntry = new TradeEntry( tTrade, Side.SELL);
				if (tFilter.filter( tTradeEntry)) {
					mTradeTableModel.addEntry(tTradeEntry);
				}
			}
		}
		catch( Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	private JPanel initFilterPanel() {
		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBorder( new EmptyBorder( 10,10,10,10));

		JPanel tComponentPanel = new JPanel( new GridBagLayout());
		tComponentPanel.setBorder( new EtchedBorder(2));

		mFilterBtn = new JButton("Filter");
		mFilterBtn.setFont( new Font( "Arail", Font.BOLD, 10));
		mFilterBtn.setForeground( Color.BLACK);
		mFilterBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadData();
			}
		});

		mFilterClearBtn = new JButton("Clear");
		mFilterClearBtn.setFont( new Font( "Arail", Font.BOLD, 10));
		mFilterClearBtn.setForeground( Color.BLACK);
		mFilterClearBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mFilterTxt.setText("");
			}
		});

		mFilterTxt = new JTextField("");
		mFilterTxt.setFont(new Font("Arial", Font.PLAIN, 14));
		mFilterTxt.setMargin( new Insets( 0,4,0,0));
		mFilterTxt.setPreferredSize( new Dimension(90,20));

		FilterAttribute tFilterAttributes[] = { FilterAttribute.Account, FilterAttribute.SID, FilterAttribute.Time,
				                                 FilterAttribute.OrderId, FilterAttribute.TradeId};
		mFilterFieldsComboBox = new JComboBox<>( tFilterAttributes );

		JLabel tFilterLbl = new JLabel("Filtering");
		tFilterLbl.setFont( new Font("Arial", Font.ITALIC , 14));
		tFilterLbl.setForeground( Color.gray);

		GridBagConstraints cb = new GridBagConstraints();
		cb.anchor = GridBagConstraints.LINE_START;
		cb.gridx = cb.gridy = 0;
		cb.insets = new Insets(8,40,8,0);

		tComponentPanel.add( tFilterLbl, cb);
		cb.insets.left = 10;
		cb.gridx++;
		tComponentPanel.add( mFilterFieldsComboBox, cb);

		cb.gridx++;
		tComponentPanel.add( mFilterTxt, cb);
		cb.gridx++;
		tComponentPanel.add( mFilterClearBtn, cb);
		cb.gridx++;
		tComponentPanel.add( mFilterBtn, cb);

		tRootPanel.add( tComponentPanel, BorderLayout.CENTER);
		return tRootPanel;
	}

	private JPanel initExportPanel()
	{

		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBorder( new EmptyBorder( new Insets(10,10,10,10)));

		JPanel tComponentPanel = new JPanel( new GridBagLayout());
		tComponentPanel.setBorder( new EtchedBorder(1));
		JLabel tDestFileLbl = new JLabel("Destination");
		tDestFileLbl.setFont( new Font("Arial", Font.PLAIN, 14));

		ExportFormat tFormats[] = { ExportFormat.EXCEL, ExportFormat.JSON, ExportFormat.HTML};
		mFormatComboBox = new JComboBox<>( tFormats );
		mFormatComboBox.setFont(new Font("Arial", Font.PLAIN, 12));

		mDestinationFileTxt = new JTextField("");
		mDestinationFileTxt.setFont(new Font("Arial", Font.PLAIN, 14));
		mDestinationFileTxt.setMargin( new Insets( 0,4,0,0));
		mDestinationFileTxt.setPreferredSize( new Dimension(348,20));


		mDestinationBrowseBtn = new JButton("...");
		mDestinationBrowseBtn.setFont( new Font( "Arail", Font.BOLD, 18));
		mDestinationBrowseBtn.setForeground( Color.BLUE);
		mDestinationBrowseBtn.setPreferredSize( new Dimension(40, 20));
		mDestinationBrowseBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pickDestinationFile();
			}
		});

		mExportBtn = new JButton("Export");
		mExportBtn.setFont( new Font( "Arail", Font.BOLD, 14));
		mExportBtn.setForeground( Color.BLACK);
		mExportBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				export();
			}
		});

		GridBagConstraints cb = new GridBagConstraints();

		cb.anchor = GridBagConstraints.LINE_START;
		cb.insets = new Insets(10,20,0,0);
		cb.gridy = cb.gridx = 0;
		tComponentPanel.add( tDestFileLbl, cb );

		cb.insets.left = 10;
		cb.gridx++;
		tComponentPanel.add( mDestinationFileTxt, cb );

		cb.insets.left = 20;
		cb.gridx++;
		tComponentPanel.add( mDestinationBrowseBtn, cb );


		cb.gridy = 1;
		cb.gridx = 1;
		cb.insets.top = 20;
		cb.insets.bottom = 12;
		cb.insets.right = 20;
		cb.anchor = GridBagConstraints.EAST;
		tComponentPanel.add( mFormatComboBox, cb );


		cb.anchor = GridBagConstraints.EAST;
		cb.gridx++;
		cb.insets.right = 30;
		tComponentPanel.add( mExportBtn, cb );




		tRootPanel.add( tComponentPanel, BorderLayout.CENTER);
		return tRootPanel;
	}

	private JPanel initTablePanel() {
		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBorder(new EmptyBorder(10, 10, 0, 10));

		// Create Table
		mTradeTableModel = new TableModel(Trades.TradeEntry.class);
		mTable = new Table(mTradeTableModel, new Dimension(mTradeTableModel.getPreferedWith() + 20, 240), this);
		mTable.setBackground(Color.white);

		JPanel tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 0, 10, 0));
		tTablePanel.add(mTable);

		tRootPanel.add(tTablePanel, BorderLayout.CENTER);
		tRootPanel.add( initFilterPanel(), BorderLayout.SOUTH);
		return tRootPanel;
	}

	private JPanel initTopPanel() {
		JPanel tRoot = new JPanel( new BorderLayout());
		tRoot.setBorder( new EmptyBorder(8,8,8,8));

		JPanel tComponetPanel = new JPanel( new GridBagLayout());
		tComponetPanel.setBorder( new EtchedBorder(1));

		// Trade Log Directory
		mTradeLogDirLbl = new JLabel( mTradeLogDirectory );
		mTradeLogDirLbl.setFont( new Font( "Arail", Font.PLAIN, 16));
		mTradeLogDirLbl.setForeground( Color.BLUE);

		mBrowseBtn = new JButton("...");
		mBrowseBtn.setFont( new Font( "Arail", Font.BOLD, 18));
		mBrowseBtn.setForeground( Color.BLUE);
		mBrowseBtn.setPreferredSize( new Dimension(40, 20));

		mBrowseBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mTxlFiles = null;
				examineTradeDir();
				loadData();
			}
		});

		GridBagConstraints cb = new GridBagConstraints();
		cb.anchor = GridBagConstraints.LINE_START;
		cb.gridx = cb.gridy = 0;
		cb.insets = new Insets(10,30,0, 8);
		tComponetPanel.add( mTradeLogDirLbl, cb );
		cb.gridx++;
		cb.insets.left = 10;
		tComponetPanel.add( mBrowseBtn, cb );

		mDatesComboBox = new JComboBox( mTxlDates.toArray( new String[0]));
		mDatesComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadData();

			}
		});
		cb.gridx = 0;
		cb.gridy++;
		cb.insets = new Insets(10,30,5, 8);
		tComponetPanel.add( mDatesComboBox, cb );

		tRoot.add( tComponetPanel, BorderLayout.CENTER);
		return tRoot;
	}

	private void examineTradeDir() {
		while( mTxlFiles == null) {
			getTradeLogDir();
			getTxlFiles();
		}
	}


	private void getTxlFiles() {
		File folder = new File(mTradeLogDirectory);
		File[] listOfFiles = folder.listFiles( new TxlNameFilter("txl"));

		if ((listOfFiles != null) || (listOfFiles.length > 0)) {
			mTxlFiles = new ArrayList<>();
			for (int i = 0; i < listOfFiles.length; i++) {
				mTxlFiles.add( listOfFiles[i]);
			}
			addDates();
		}
	}

	private void addDates() {
		Pattern tPattern = Pattern.compile(".*-(\\d{2})(\\d{2})(\\d{2})-\\d+.txl");

		mTxlDates = new ArrayList<>();
		for( File tFile : mTxlFiles) {
			// "trades-211118-164456563.txl"
			Matcher m = tPattern.matcher( tFile.getName());
			if (m.matches()) {
				String tDateStr = String.format("20%s-%s-%s", m.group(1), m.group(2), m.group(3));
				if (!mTxlDates.stream().anyMatch( d -> {return d.contentEquals( tDateStr);})) {
					mTxlDates.add( tDateStr );
				}
			}
		}
	}

	private void getTradeLogDir() {
		File tFile = null;

		if (mTradeLogDirectory != null) {
			tFile = new File(mTradeLogDirectory);
			if (((!tFile.exists()) || (!tFile.canRead()))) {
				tFile = null;
			}
		}

		while( tFile == null ) {
			tFile = selectTradeLogDir();
			if (((tFile == null) || (!tFile.exists()) || (!tFile.canRead()))) {
				tFile = null;


				JOptionPane.showMessageDialog(this,
						"Can not find or open users definition file",
						"Invalid user definition file",
						JOptionPane.WARNING_MESSAGE);
			}
		}
		mTradeLogDirectory = tFile.getAbsolutePath();
	}

	public void export()
	{
		if ((mDestinationFileTxt.getText().isEmpty()) || (mDestinationFileTxt.getText().isEmpty())) {
			JOptionPane.showMessageDialog(this,
					"No destination file specified",
					"No Destination",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		List<Trades.TradeEntry> tTradeEntries = mTradeTableModel.getObjects();

		if (tTradeEntries.size() == 0) {
			JOptionPane.showMessageDialog(this,
					"No trades to export",
					"No Trades",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		 switch( (ExportFormat) mFormatComboBox.getSelectedItem()) {
			 case HTML:
				exportHTML();
				return;
			 case JSON:
				 exportJSON();
				 return;
			 case EXCEL:
				 exportExcel();
				 return;
		 }
	}

	private void exportExcel()
	{
		List<Trades.TradeEntry> tTradeEntries = mTradeTableModel.getObjects();
		StringBuilder sb = new StringBuilder();
		sb.append("SID;Account;Price;Quantity;Side;Time;TradeId;OrderId\n");
		for( TradeEntry te : tTradeEntries) {
			sb.append( te.toCsv());
		}

		try {
			PrintWriter tOut = new PrintWriter(new FileWriter(mDestinationFileTxt.getText()));
			tOut.println( sb.toString());
			tOut.flush();
			tOut.close();

			JOptionPane.showMessageDialog(this,
					"Successfully export " + tTradeEntries.size() + " trades to \n" +
							" file: " + mDestinationFileTxt.getText(),
					"Export",
					JOptionPane.INFORMATION_MESSAGE);
		}
		catch( IOException e) {
			JOptionPane.showMessageDialog(this,
					"EXCEL/CSV export failed, reason: " + e.getMessage(),
					"Export failed",
					JOptionPane.WARNING_MESSAGE);
			e.printStackTrace();
		}

	}

	private void exportHTML() {
		List<Trades.TradeEntry> tTradeEntries = mTradeTableModel.getObjects();

		HtmlBuilder tHtml = new HtmlBuilder();
		tHtml.add("<html>\n");
		tHtml.add("<head>\n");
		tHtml.add("<style>\n");
		tHtml.add("table, th, td {\n");
		tHtml.add(" border: 1px solid gray; padding-right:5px; padding-left:5px; border-collapse: collapse; font-family: clibri,arial; font-size: 12px; margin-left: auto; margin-right: auto; }\n");
		tHtml.add("h1 {  font-family: clibri,arial; text-align: center; }\n");
		tHtml.add("</style>\n");
		tHtml.add("</head>\n");
		tHtml.add("<body>\n");
		tHtml.add("<h1>Trades " + mDatesComboBox.getSelectedItem().toString() +"</h1>\n");
		tHtml.add("<table>\n");
		tHtml.add("<tr>\n");
        tHtml.add("<th>Account</th><th>Price</th><th>Quantity</th><th>Side</th><th>Time</th><th>TradeId</th><th>OrderId</th>\n");
		tHtml.add("</tr>\n");
		for( TradeEntry te : tTradeEntries) {
			tHtml.addTradeEntry( te );
		}
		tHtml.add("</table>\n");
		tHtml.add("</body>\n");
		tHtml.add("</html>\n");
		try {
			PrintWriter tOut = new PrintWriter(new FileWriter(mDestinationFileTxt.getText()));
			tOut.println( tHtml.toString());
			tOut.flush();
			tOut.close();

			JOptionPane.showMessageDialog(this,
					"Successfully export " + tTradeEntries.size() + " trades to \n" +
							" file: " + mDestinationFileTxt.getText(),
					"Export",
					JOptionPane.INFORMATION_MESSAGE);
		}
		catch( IOException e) {
			JOptionPane.showMessageDialog(this,
					"HTML export failed, reason: " + e.getMessage(),
					"Export failed",
					JOptionPane.WARNING_MESSAGE);
			e.printStackTrace();
		}
	}


	private void exportJSON() {
		List<Trades.TradeEntry> tTradeEntries = mTradeTableModel.getObjects();

		try {
			PrintWriter tOut = new PrintWriter(new FileWriter(mDestinationFileTxt.getText()));
			tOut.println("{ \"date\": \"" + mDatesComboBox.getSelectedItem() + "\",");
			tOut.println("  \"trades\": [");
			for (int i = 0; i < (tTradeEntries.size() - 1); i++) {
				tOut.println(tTradeEntries.get(i).toJson().toString() + ",");
			}
			tOut.println(tTradeEntries.get((tTradeEntries.size() - 1)).toJson().toString());
			tOut.println("]}");
			tOut.flush();
			tOut.close();

			JOptionPane.showMessageDialog(this,
					"Successfully export " + tTradeEntries.size() + " trades to \n" +
							" file: " + mDestinationFileTxt.getText(),
					"Export",
					JOptionPane.INFORMATION_MESSAGE);
		}
		catch( IOException e) {
			JOptionPane.showMessageDialog(this,
					"JSON export failed, reason: " + e.getMessage(),
					"Export failed",
					JOptionPane.WARNING_MESSAGE);
			e.printStackTrace();
		}
	}

	private void pickDestinationFile() {
		JFileChooser tFileChooser = new JFileChooser();
		tFileChooser.setDialogTitle("Trade Export File");
		tFileChooser.setFileSelectionMode(JFileChooser.SAVE_DIALOG );
		tFileChooser.setCurrentDirectory(new File("./"));
		tFileChooser.setFont(new Font("Arial", Font.PLAIN, 14));
		int tResult = tFileChooser.showSaveDialog(this);
		if (tResult == JFileChooser.APPROVE_OPTION) {
			File tFile = tFileChooser.getSelectedFile();
			mDestinationFileTxt.setText( tFile.getAbsolutePath());
		}
	}
	private File selectTradeLogDir() {

		JFileChooser tFileChooser = new JFileChooser();
		tFileChooser.setDialogTitle("Trade Log Directory");
		tFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES );
		tFileChooser.setFileFilter(new Trades.FileTypeFilter("txl", ""));
		tFileChooser.setCurrentDirectory(new File("./"));
		tFileChooser.setFont(new Font("Arial", Font.PLAIN, 14));
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
	public void tableMouseButton2(TradeEntry pObject, int pRow, int pCol) {

	}

	@Override
	public void tableMouseClick(TradeEntry pObject, int pRow, int pCol) {

	}

	@Override
	public void tableMouseDoubleClick(TradeEntry pObject, int pRow, int pCol) {

	}

	class HtmlBuilder
	{
		StringBuilder sb = new StringBuilder();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		void add( String pTag ) {
			sb.append( pTag );
		}

		void addTradeEntry( TradeEntry te ) {
			sb.append( te.toHtml());
		}



		public String toString() {
			return sb.toString();
		}
	}





	class TxlNameFilter implements FilenameFilter {
		private String mExtension;

		TxlNameFilter( String pExtension ) {
			mExtension = pExtension;
		}

		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(mExtension);
		}
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

	public class TradeFilter{
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
		FilterAttribute mFilterAttribute;
		String mFilterText;

		TradeFilter(FilterAttribute pFilterAttributes, String pFilterText ) {
			mFilterAttribute = pFilterAttributes;
			mFilterText = pFilterText;
		}

		boolean filter( TradeEntry pTradeEntry ) {
			if ((mFilterText == null) || (mFilterText.isBlank()) || (mFilterText.length() == 0)) {
				return true;
			}
			switch( mFilterAttribute) {
				case TradeId:
					return Long.toHexString(pTradeEntry.mTradeId).contains( mFilterText );
				case OrderId:
					return Long.toHexString(pTradeEntry.mOrderId).contains( mFilterText );
				case SID:
					return pTradeEntry.mSid.contains( mFilterText );
				case Account:
					return pTradeEntry.mAccount.contains( mFilterText );
				case Time:
					return sdf.format(pTradeEntry.mTime).startsWith( mFilterText );
			}
			return false;
		}

	}
	public class TradeEntry
	{
		public String mSid;
		public Side mSide;
		public String mAccount;
		public long mOrderId;
		public double mPrice;
		public long mQuantity;
		public long mTradeId;
		public long mTime;

		public TradeEntry(InternalTrade pTrade, Side pSide )
		{
			mSid = pTrade.getSid();
			mSide = pSide;
			mAccount = (pSide == Side.BUY) ? pTrade.getBuyOrder().getAccountId() : pTrade.getSellOrder().getAccountId();
			mOrderId = (pSide == Side.BUY) ? pTrade.getBuyOrder().getOrderId() : pTrade.getSellOrder().getOrderId();
			mPrice = pTrade.getPrice();
			mQuantity = pTrade.getQuantity();
			mTradeId = pTrade.getTradeNo();
			mTime = pTrade.getTradeTime();
		}


		@TableAttribute(header = "SID", column = 1, width = 60, alignment = JLabel.LEFT)
		public String getSid() {
			return mSid;
		}

		@TableAttribute(header = "B/S", column = 2, width = 45, alignment = JLabel.LEFT)
		public String getSide() {
			return mSide.name();
		}

		@TableAttribute(header = "Account", column = 3, width = 65)
		public String getAccount() {
			return mAccount;
		}

		@TableAttribute(header = "OrderId", column = 4, width = 85)
		public String getOrderId() {
			return Long.toHexString(mOrderId);
		}

		@TableAttribute(header = "Price", column = 5, width = 45)
		public String getPrice() {
			return nfmt.format( mPrice );
		}

		@TableAttribute(header = "Quantity", column = 6, width = 65)
		public String getQuantity() {
			return String.valueOf( mQuantity );
		}

		@TableAttribute(header = "TradeId", column = 7, width = 85)
		public String getTradeId() {
			return Long.toHexString(mTradeId);
		}

		@TableAttribute(header = "Time", column = 8, width = 85)
		public String getTime() {
			SimpleDateFormat sdf = new SimpleDateFormat("HH:ss:mm.SSS");
			return sdf.format( mTime );
		}

		public String toCsv() {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:hh:ss.SSS");
			return this.mSid + ";" +
					this.mAccount + ";" +
					nfmt.format(mPrice) + ";" +
					String.valueOf(this.mQuantity) + ";" +
					this.mSide.name() + ";" +
					sdf.format(this.mTime) + ";" +
					Long.toHexString(mTradeId) + ";" +
					Long.toHexString(mOrderId) + ";\n";

		}

		public String toHtml() {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:hh:ss.SSS");
			StringBuilder sb = new StringBuilder();
				sb.append("<tr>");
				sb.append("<td>" + this.mSid + "</td>");
				sb.append("<td>" + this.mAccount + "</td>");
				sb.append("<td>" + nfmt.format(this.mPrice) + "</td>");
				sb.append("<td>" + this.mQuantity + "</td>");
				sb.append("<td>" + this.mSide.name() + "</td>");
				sb.append("<td>" + sdf.format( this.mTime )+ "</td>");
				sb.append("<td>" + Long.toHexString( this.mTradeId) + "</td>");
				sb.append("<td>" + Long.toHexString( this.mOrderId) + "</td>");
				sb.append("</tr>\n");
			return sb.toString();
		}
		public JsonObject toJson() {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:hh:ss.SSS");
			JsonObject jObject = new JsonObject();
			jObject.addProperty("SID", this.mSid);
			jObject.addProperty( "account", this.mAccount);
			jObject.addProperty( "side", this.mSide.name());
			jObject.addProperty( "price", nfmt.format(mPrice));
			jObject.addProperty( "quantity", this.mQuantity);
			jObject.addProperty("orderId", Long.toHexString( mOrderId ));
			jObject.addProperty("tradeId", Long.toHexString( mTradeId));
			jObject.addProperty("time", sdf.format( this.mTime ));
			return jObject;
		}

	}
}

/*
 * Copyright (c)  Hoddmimes Solution AB 2022.
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

package com.hoddmimes.te.client;

import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.GridBagPanel;
import com.hoddmimes.te.common.table.Table;
import com.hoddmimes.te.common.table.TableAttribute;
import com.hoddmimes.te.common.table.TableCallbackInterface;
import com.hoddmimes.te.common.table.TableModel;
import com.hoddmimes.te.common.transport.http.TeRequestException;
import com.hoddmimes.te.messages.generated.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class EnterOrderDialog extends JFrame implements TableCallbackInterface<EnterOrderDialog.BookOrderEntry>, TeBroadcastListener {
	private Connector mConnector;
	private Table mTable;
	private TableModel<BookOrderEntry> mTableModel;
	private JComboBox<String> mOrderbookComboBox;
	private JTextField mSidTxtFld;
	private JTextField mPriceTxtFld;
	private JTextField mQuantityTxtFld;
	private JTextField mUserTxtFld;
	private JComboBox<String> mSideComboBox;
	private JButton mCancelBtn,mAddOrderBtn;
	private MessageFactory mMessageFactory;




	EnterOrderDialog( Connector pConnector) {
		super();
		mConnector = pConnector;
		mConnector.addSubription( this );

		mMessageFactory = new MessageFactory();

		init();
		queryOrderbook( (String) mOrderbookComboBox.getSelectedItem() );
		this.setTitle("Enter Order");
		this.pack();
		AuxClt.centeredFrame(this );
		this.setVisible( true );
	}

	private void init() {
		GridBagPanel tRootPanel = new GridBagPanel( GridBagConstraints.CENTER );
		tRootPanel.setBackground( AuxClt.PANEL_BACKGROUND );
		tRootPanel.insets( new Insets(10,0,0,0));

		tRootPanel.add( AuxClt.makeheaderpanel("Enter Order"));

		tRootPanel.incy().add( createSelectionPanel() );
		tRootPanel.incy().add( createTablePanel() );
		tRootPanel.incy().top(18).bottom(25).add( createEnterOrderPanel() );

		this.setContentPane( tRootPanel );
	}

	JPanel createSelectionPanel() {
		GridBagPanel tSelectionPanel = new GridBagPanel( GridBagConstraints.CENTER );
		tSelectionPanel.setPreferredSize( new Dimension(444, 36));
		tSelectionPanel.insets( new Insets(3,0, 3, 0 ));
		tSelectionPanel.setBackground( AuxClt.PANEL_BACKGROUND);
		tSelectionPanel.setBorder( new EtchedBorder( EtchedBorder.RAISED));

		tSelectionPanel.add( AuxClt.makelabel("Orderbook"));

		mOrderbookComboBox = new JComboBox(	mConnector.getSymbolIdentities().stream().toArray() );
		mOrderbookComboBox.setBackground(  Color.white );
		mOrderbookComboBox.setFont( AuxClt.DEFAULT_FONT );

		mOrderbookComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				queryOrderbook( (String) mOrderbookComboBox.getSelectedItem());
			}
		});
		tSelectionPanel.incx().left(12).add( mOrderbookComboBox );
		return tSelectionPanel;
	}

	JPanel createEnterOrderPanel() {
		GridBagPanel tOrderPanel = new GridBagPanel( GridBagConstraints.CENTER );
		tOrderPanel.setBackground( AuxClt.PANEL_BACKGROUND);
		tOrderPanel.setBorder( new EtchedBorder( EtchedBorder.RAISED));

		// Row one
		GridBagPanel tRowOnePanel = new GridBagPanel( GridBagConstraints.CENTER );
		tRowOnePanel.insets(new Insets( 10,16, 0, 0));
		tRowOnePanel.setBackground( AuxClt.PANEL_BACKGROUND);

		tRowOnePanel.add(AuxClt.makelabel("SID"));
		mSidTxtFld = AuxClt.maketxtfld( "", 85 );
		mSidTxtFld.setEditable( false );
		mSidTxtFld.setBackground( AuxClt.LIGHT_LIGHT_GRAY );
		tRowOnePanel.left(10).incx().add( mSidTxtFld );

		tOrderPanel.add( tRowOnePanel);

		GridBagPanel tRowTwoPanel = new GridBagPanel( GridBagConstraints.CENTER );
		tRowTwoPanel.setBackground( AuxClt.PANEL_BACKGROUND);
		tRowTwoPanel.insets( new Insets(10,10, 0, 0 ));
		tRowTwoPanel.add(AuxClt.makelabel("Price"));


		mPriceTxtFld =  AuxClt.maketxtfld(65 );
		tRowTwoPanel.incx().add( mPriceTxtFld );

		tRowTwoPanel.incx().add(AuxClt.makelabel("Quantity"));

		mQuantityTxtFld =  AuxClt.maketxtfld(65 );
		tRowTwoPanel.incx().add( mQuantityTxtFld );

		String tSides[] = {"BUY","SELL"};
		mSideComboBox = new JComboBox( tSides );
		mSideComboBox.setFont( AuxClt.DEFAULT_FONT_BOLD);
		mSideComboBox.setPreferredSize( new Dimension(72, 22));
		mSideComboBox.setBackground( AuxClt.TXTFLD_BACKGROUND );
		tRowTwoPanel.incx().left(30).add( mSideComboBox );


		tRowTwoPanel.incx().left(15).add(AuxClt.makelabel("User ref"));

		mUserTxtFld =  AuxClt.maketxtfld(80 );
		tRowTwoPanel.incx().left(10).right(20).add(mUserTxtFld );
		tOrderPanel.incy().add( tRowTwoPanel );

		// Row Three (  CANCEL ADD-ORDER )
		GridBagPanel tRowThreePanel = new GridBagPanel( GridBagConstraints.CENTER );
		tRowThreePanel.setBackground( AuxClt.PANEL_BACKGROUND);
		tRowThreePanel.insets( new Insets(18,0, 10, 0 ));


		mCancelBtn = AuxClt.makebutton("Cancel", 142);
		tRowThreePanel.add(mCancelBtn );

		mAddOrderBtn = AuxClt.makebutton("Add Order", 142);
		tRowThreePanel.left(20).incx().add(mAddOrderBtn);

		mCancelBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				EnterOrderDialog.this.dispose();
			}
		});

		mAddOrderBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addOrder( (String) mOrderbookComboBox.getSelectedItem());
			}
		});



		tOrderPanel.incy().add( tRowThreePanel );

		return tOrderPanel;
	}

	private void queryOrderbook( String pSid ) {

			mTableModel.clear();
			mTableModel.fireTableDataChanged();
			mSidTxtFld.setText( pSid );
			this.setTitle("Enter Order " + pSid );

			try {
				JsonObject jResp = mConnector.get("queryOrderbook/" + pSid);
				MessageInterface tResp = mMessageFactory.getMessageInstance(AuxJson.tagMessageBody(QueryOrderbookResponse.NAME, jResp));
				if (tResp instanceof QueryOrderbookResponse) {
					loadData((QueryOrderbookResponse) tResp);
				} else {
					JOptionPane.showMessageDialog(null,
							"Orderbook (" + pSid + ") load failure, reason: " + tResp.toJson().toString(),
							"Load Orderbook Failure",
							JOptionPane.WARNING_MESSAGE);
					return;
				}
			} catch (TeRequestException | IOException e) {
				JOptionPane.showMessageDialog(null,
						"Orderbook (" + pSid + ") load failure, reason: " + e.getMessage(),
						"Load Orderbook Failure",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
		}

	private void loadData(  QueryOrderbookResponse pOrderboookResponse ) {
		mTableModel.clear();
		if ((!pOrderboookResponse.getBuyOrders().isPresent()) && (!pOrderboookResponse.getSellOrders().isPresent())) {
			JOptionPane.showMessageDialog(null,
					"Orderbook (" + pOrderboookResponse.getSid().get() + ") is empty",
					"Orderbook Is Empty",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		int tMaxLevels = pOrderboookResponse.getMaxLevels();
		if (tMaxLevels == 0) {
			JOptionPane.showMessageDialog(null,
					"Orderbook (" + pOrderboookResponse.getSid().get() + ") is empty",
					"Orderbook Is Empty",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		for (int i = 0; i <  tMaxLevels; i++) {
			mTableModel.addEntry(new BookOrderEntry(pOrderboookResponse.getBuyOrder(i), pOrderboookResponse.getSellOrder(i)));
		}
	}


	JPanel createTablePanel() {
		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBorder( new EmptyBorder(10,5,10,5));
		JPanel tContentPanel = new JPanel( new BorderLayout());
		tContentPanel.setBorder( new EtchedBorder(2));
		tRootPanel.add( tContentPanel);

		mTableModel = new TableModel(BookOrderEntry.class);

		mTable = new Table(mTableModel, new Dimension(mTableModel.getPreferedWith() + 18, 280), this);
		mTable.setBackground(Color.white);

		JPanel tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 5, 10, 5));
		tTablePanel.add(mTable);

		tContentPanel.add(tTablePanel, BorderLayout.CENTER);
		return tRootPanel;
	}

	@Override
	public void tableMouseButton2(BookOrderEntry pObject, int pRow, int pCol) {

	}

	@Override
	public void tableMouseClick(BookOrderEntry pObject, int pRow, int pCol) {

	}

	@Override
	public void tableMouseDoubleClick(BookOrderEntry pObject, int pRow, int pCol) {
		if (pObject == null) {
			return;
		}

		BookOrderEntry boe = (BookOrderEntry) pObject;
		if (pCol == 0) {
			mPriceTxtFld.setText( AuxClt.fmtInternalPrice( boe.mBidOrder.getPrice().get()));
			mQuantityTxtFld.setText( String.valueOf( boe.mBidOrder.getQuantity().get()));
			mSideComboBox.setSelectedIndex(1);
		} else {
			mPriceTxtFld.setText( AuxClt.fmtInternalPrice( boe.mOfferOrder.getPrice().get()));
			mQuantityTxtFld.setText( String.valueOf( boe.mOfferOrder.getQuantity().get()));
			mSideComboBox.setSelectedIndex(0);
		}
	}

	private void addOrder( String pSid ) {
		// Validate Price
		long tPrice;
		long tQuantity;
		String tSide;
		String tRef;
		try {
			double d = Double.parseDouble( mPriceTxtFld.getText());
			tPrice = (long) ((double)AuxClt.PRICE_MULTIPLER * d);

		}
		catch( NumberFormatException e ) {
			JOptionPane.showMessageDialog(null,
					"Invalid price",
					"Invalid price",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		try {
			tQuantity = Long.parseLong( mQuantityTxtFld.getText());
		}
		catch( NumberFormatException e ) {
			JOptionPane.showMessageDialog(null,
					"Invalid quality",
					"Invalid quantity",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		tSide = (String) mSideComboBox.getSelectedItem();
		if ((mUserTxtFld.getText().isEmpty()) || (mUserTxtFld.getText().isBlank())) {
			tRef = AuxClt.getUserRef();
		} else {
			tRef = mUserTxtFld.getText();
		}
		AddOrderRequest tRqst = new AddOrderRequest();
		tRqst.setSid( pSid ).setPrice( tPrice ).setQuantity( tQuantity ).setSide( tSide ).setRef( tRef );
		try {
			JsonObject tRsp = mConnector.post(AuxJson.getMessageBody( tRqst.toJson()), "addOrder" );
			if (tRsp.has("orderId")) {
				JOptionPane.showMessageDialog(null,
						"Order successfully submitted",
						"Add Order Success",
						JOptionPane.PLAIN_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(null,
						"Add Order Failure, reason: " + tRef.toString(),
						"Add Order Failure",
						JOptionPane.WARNING_MESSAGE);
			}
		}
		catch( TeRequestException | IOException e) {
			JOptionPane.showMessageDialog(null,
					"Add Order Failure, reason: " + e.getMessage(),
					"Add Order Failure",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
	}

	@Override
	public void onTeBdx(MessageInterface pTeBroadcast) {
		if (pTeBroadcast instanceof BdxTrade) {
			BdxTrade tBdx = (BdxTrade) pTeBroadcast;
			if (tBdx.getSid().get().contentEquals( (String) mOrderbookComboBox.getSelectedItem())) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						queryOrderbook( tBdx.getSid().get() );
					}
				});
			}
		}
		if (pTeBroadcast instanceof BdxOrderbookChange) {
			BdxOrderbookChange tBdx = (BdxOrderbookChange) pTeBroadcast;

			if (tBdx.getSid().get().contentEquals( (String) mOrderbookComboBox.getSelectedItem())) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						queryOrderbook( tBdx.getSid().get() );
					}
				});
			}
		}
	}


	public class BookOrderEntry {
		public Order mBidOrder;
		public Order mOfferOrder;

		public BookOrderEntry(Order pBidOrder, Order pOfferOrder ) {
			mBidOrder = pBidOrder;
			mOfferOrder = pOfferOrder;
		}

		@TableAttribute(header = "bid", column = 1, width = 200, alignment = JLabel.CENTER)
		public String getbid() {
			if (mBidOrder == null) {
				return "----------";
			}
			return mBidOrder.getQuantity().get() + "@" + AuxClt.fmtInternalPrice( mBidOrder.getPrice().get());
		}
		@TableAttribute(header = "offer", column = 2, width = 200, alignment = JLabel.CENTER)
		public String getOffer() {
			if (mOfferOrder == null) {
				return "----------";
			}
			return mOfferOrder.getQuantity().get() + "@" + AuxClt.fmtInternalPrice( mOfferOrder.getPrice().get());
		}

	}
}

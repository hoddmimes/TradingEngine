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
import com.hoddmimes.te.common.table.Table;
import com.hoddmimes.te.common.table.TableAttribute;
import com.hoddmimes.te.common.table.TableCallbackInterface;
import com.hoddmimes.te.common.table.TableModel;
import com.hoddmimes.te.common.transport.http.TeRequestException;
import com.hoddmimes.te.messages.generated.BdxTrade;
import com.hoddmimes.te.messages.generated.MessageFactory;
import com.hoddmimes.te.messages.generated.QueryTradesResponse;
import com.hoddmimes.te.messages.generated.Trade;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;


public class TradesFrame extends JFrame implements TeBroadcastListener, TableCallbackInterface, DoubleClickAdapter.DoubleClickCallback {
	private Connector mConnector;
	private MessageFactory mMessageFactory;


	TableModel<TradeEntry> mTableModel;
	Table               mTable;
	JComboBox<String>   mSidComboBox;

	TradesFrame(Connector pConnector ) {
		mConnector = pConnector;
		mMessageFactory = new MessageFactory();
		loadSids();

		init();

		pConnector.addSubription( this );

		this.pack();
		AuxClt.centeredFrame(this);
		this.setVisible(true);
		loadData( (String) mSidComboBox.getSelectedItem());
	}

	private void loadSids() {
		List<String> tInstruments = mConnector.getSymbolIdentities();
		String sidArr[] = tInstruments.toArray(new String[0]);
		mSidComboBox = new JComboBox<>( sidArr );
		mSidComboBox.setFont( AuxClt.DEFAULT_FONT );
		mSidComboBox.setPreferredSize( new Dimension( 80,22));
		mSidComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sidChanged();
			}
		});
	}

	private void sidChanged() {
		loadData( (String) mSidComboBox.getSelectedItem());
	}

	private void init() {
		JPanel tRootPanel = new JPanel( new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.insets = new Insets(10,60, 5, 60 );
		gc.anchor = GridBagConstraints.CENTER;
		gc.gridy = gc.gridx = 0;
		gc.fill = GridBagConstraints.HORIZONTAL;

		JPanel tHeaderPanel =  AuxClt.makeheaderwithsymbolspanel("Trades", mSidComboBox);
		tHeaderPanel.addMouseListener(new DoubleClickAdapter( this ));
		tRootPanel.add( tHeaderPanel, gc);

		gc.insets = new Insets(0,5, 10, 5 );
		gc.gridy++;
		tRootPanel.add( createTablePanel(), gc);



		this.setTitle("Trades");
		this.setContentPane( tRootPanel );
	}

	JPanel createTablePanel() {
		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBorder( new EmptyBorder(10,5,10,5));
		JPanel tContentPanel = new JPanel( new BorderLayout());
		tContentPanel.setBorder( new EtchedBorder(2));
		tRootPanel.add( tContentPanel);

		mTableModel = new TableModel(TradeEntry.class);

		mTable = new Table(mTableModel, new Dimension(mTableModel.getPreferedWith() + 18, 380), this);
		mTable.setBackground(Color.white);

		JPanel tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 5, 10, 5));
		tTablePanel.add(mTable);

		tContentPanel.add(tTablePanel, BorderLayout.CENTER);
		return tRootPanel;
	}





	private void loadData( QueryTradesResponse pTradesResponse  ) {
		mTableModel.clear();
		if (pTradesResponse.getTrades().isPresent()) {
			for (Trade trd : pTradesResponse.getTrades().get()) {
				mTableModel.addEntry(new TradeEntry(trd));
			}
		}
		mTableModel.fireTableDataChanged();
	}




	private void loadData( String pSid ) {
		mTableModel.clear();
		mTableModel.fireTableDataChanged();
		try {
			JsonObject jResp = mConnector.get("queryTrades/" + pSid);
			MessageInterface tResp = mMessageFactory.getMessageInstance(AuxJson.tagMessageBody(QueryTradesResponse.NAME, jResp));
			if (tResp instanceof QueryTradesResponse) {
					loadData((QueryTradesResponse) tResp );
			} else {
				JOptionPane.showMessageDialog(null,
						"Load trades (" + pSid + ") failure, reason: " + tResp.toJson().toString(),
						"Load Trades Failure",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
		} catch (TeRequestException | IOException e) {
			JOptionPane.showMessageDialog(null,
					"Load Trades (" + pSid + ") failure, reason: " + e.getMessage(),
					"Load Trades Failure",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
	}



	@Override
	public void onTeBdx(MessageInterface pTeBroadcast) {
		if (pTeBroadcast instanceof BdxTrade) {
			BdxTrade tBdxTrade = (BdxTrade) pTeBroadcast;
			if (tBdxTrade.getSid().get().contentEquals((String) mSidComboBox.getSelectedItem())) {
				loadData(tBdxTrade.getSid().get());
			}
		}
	}

	@Override
	public void tableMouseButton2(Object pObject, int pRow, int pCol) {

	}

	@Override
	public void tableMouseClick(Object pObject, int pRow, int pCol) {

	}

	@Override
	public void tableMouseDoubleClick(Object pObject, int pRow, int pCol) {
		String tSid = (String) mSidComboBox.getSelectedItem();
		OrderDialog od = new OrderDialog( tSid, mConnector);
		od.pack();
		AuxClt.centeredFrame( od );
		od.setVisible(true);
	}

	@Override
	public void doubleClick() {
		String tSid = (String) mSidComboBox.getSelectedItem();
		OrderDialog od = new OrderDialog( tSid, mConnector);
		od.pack();
		AuxClt.centeredFrame( od );
		od.setVisible(true);
	}


	public static class TradeEntry {
		public Trade mTrade;


		public TradeEntry( Trade pTrade ) {
			mTrade = pTrade;
		}

		@TableAttribute(header = "Time", column = 1, width = 100, alignment = JLabel.CENTER)
		public String getTime() {
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
			return sdf.format( mTrade.getTime().get());
		}

		@TableAttribute(header = "Vol/Price", column = 2, width = 100, alignment = JLabel.CENTER)
		public String getVolumePrice() {
			return mTrade.getQuantity().get() + "@" + AuxClt.fmtInternalPrice( mTrade.getPrice().get());
		}

		@TableAttribute(header = "Buyer", column = 3, width = 100, alignment = JLabel.CENTER)
		public String getBuyer() {
			return mTrade.getBuyer().get();
		}

		@TableAttribute(header = "Seller", column = 4, width = 100, alignment = JLabel.CENTER)
		public String getSeller() {
			return mTrade.getSeller().get();
		}

		@TableAttribute(header = "Trade#", column = 5, width = 200, alignment = JLabel.CENTER)
		public String getTradeNo() {
			return String.valueOf( mTrade.getTradeNo().get());
		}


	}

}

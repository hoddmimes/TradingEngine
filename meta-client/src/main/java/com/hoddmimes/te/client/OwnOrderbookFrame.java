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
import com.hoddmimes.te.messages.generated.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.io.IOException;
import java.text.SimpleDateFormat;


public class OwnOrderbookFrame extends JFrame implements TeBroadcastListener, TableCallbackInterface {
	private Connector mConnector;
	private MessageFactory mMessageFactory;


	TableModel<OrderEntry>  mTableModel;
	Table                   mTable;
	JComboBox<String>       mSidComboBox;

	OwnOrderbookFrame(Connector pConnector ) {
		mConnector = pConnector;
		mMessageFactory = new MessageFactory();

		init();

		pConnector.addSubription( this );
		loadData();

		this.pack();
		AuxClt.centeredFrame(this);
		this.setVisible(true);
	}


	private void init() {
		JPanel tRootPanel = new JPanel( new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.insets = new Insets(10,60, 5, 60 );
		gc.anchor = GridBagConstraints.CENTER;
		gc.gridy = gc.gridx = 0;
		gc.fill = GridBagConstraints.HORIZONTAL;

		JPanel tHeaderPanel =  AuxClt.makeheaderpanel("Own Orders");
		tRootPanel.add( tHeaderPanel, gc);

		gc.insets = new Insets(0,5, 10, 5 );
		gc.gridy++;
		tRootPanel.add( createTablePanel(), gc);



		this.setTitle("Own Orders");
		this.setContentPane( tRootPanel );
	}

	JPanel createTablePanel() {
		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBorder( new EmptyBorder(10,5,10,5));
		JPanel tContentPanel = new JPanel( new BorderLayout());
		tContentPanel.setBorder( new EtchedBorder(2));
		tRootPanel.add( tContentPanel);

		mTableModel = new TableModel(OrderEntry.class);

		mTable = new Table(mTableModel, new Dimension(mTableModel.getPreferedWith() + 18, 380), this);
		mTable.setBackground(Color.white);

		JPanel tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 5, 10, 5));
		tTablePanel.add(mTable);

		tContentPanel.add(tTablePanel, BorderLayout.CENTER);
		return tRootPanel;
	}





	private void loadData(  QueryOwnOrdersResponse pOwnOrdersResponse ) {
		mTableModel.clear();
		if (pOwnOrdersResponse.getOrders().isPresent()) {
			for( OwnOrder tOrder : pOwnOrdersResponse.getOrders().get() ) {
				mTableModel.addEntry(new OrderEntry( tOrder ));
			}
		}
		mTableModel.fireTableDataChanged();
	}





	private void loadData( ) {
		mTableModel.clear();
		mTableModel.fireTableDataChanged();

		for( Market tMarket : mConnector.getMarkets()) {
			try {
				JsonObject jResp = mConnector.get("queryOwnOrders/" + tMarket.getId().get());
				MessageInterface tResp = mMessageFactory.getMessageInstance(AuxJson.tagMessageBody(QueryOwnOrdersResponse.NAME, jResp));
				if (tResp instanceof QueryOwnOrdersResponse) {
					loadData((QueryOwnOrdersResponse) tResp);
				} else {
					JOptionPane.showMessageDialog(null,
							"Load Own Orders for market (" + tMarket.getId().get() + ") failure, reason: " + tResp.toJson().toString(),
							"Load Own Orderbook Failure",
							JOptionPane.WARNING_MESSAGE);
					return;
				}
			} catch (TeRequestException | IOException e) {
				JOptionPane.showMessageDialog(null,
						"Load Own Orders for market  (" + tMarket.getId().get() + ") failure, reason: " + e.getMessage(),
						"Load Own Orderbook Failure",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
		}
	}



	@Override
	public void onTeBdx(MessageInterface pTeBroadcast) {
		if (pTeBroadcast instanceof BdxOwnOrderbookChange) {
			loadData();
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
		OrderEntry tOrderEntry = (OrderEntry) pObject;

		int tOption = JOptionPane.showConfirmDialog(null, pObject,
				"Do you like to delete order \n" +  tOrderEntry.toString(),
				 JOptionPane.YES_NO_OPTION );

		if (tOption == JOptionPane.NO_OPTION) {
			return;
		}


		DeleteOrderRequest tRqst = new DeleteOrderRequest().setSid(tOrderEntry.getSid()).setOrderId( tOrderEntry.getOrderId());
		try {
			JsonObject jResponse = mConnector.post( AuxJson.getMessageBody(tRqst.toJson()),"deleteOrder");
			if (jResponse.has("orderId")) {
				JOptionPane.showMessageDialog(null,
						"Order successfully delete",
						"Order Deleted",
						JOptionPane.INFORMATION_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(null,
						"Failed to delete order, reason: " + jResponse.toString(),
						"Order Delete Failure",
						JOptionPane.WARNING_MESSAGE);
			}
		}
		catch( TeRequestException | IOException e) {
			JOptionPane.showMessageDialog(null,
					"Failed to delete order, reason: " + e.getMessage(),
					"Order Delete Failure",
					JOptionPane.WARNING_MESSAGE);
		}
	}



	public static class OrderEntry {
		public OwnOrder mOrder;


		public OrderEntry( OwnOrder pOwnOrder ) {
			mOrder = pOwnOrder;
		}

		@TableAttribute(header = "SID", column = 1, width = 100, alignment = JLabel.CENTER)
		public String getSid() {
			return mOrder.getSid().get();
		}

		@TableAttribute(header = "Volume/Price", column = 2, width = 120, alignment = JLabel.CENTER)
		public String getVolPrice() {
			return mOrder.getQuantity().get() + "@" + AuxClt.fmtInternalPrice( mOrder.getPrice().get());
		}

		@TableAttribute(header = "Side", column = 3, width = 80, alignment = JLabel.CENTER)
		public String getSide() {
			return mOrder.getSide().get();
		}

		@TableAttribute(header = "Time", column = 4, width = 100, alignment = JLabel.CENTER)
		public String getTime() {
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
			return sdf.format( mOrder.getCreateTime().get());
		}

		@TableAttribute(header = "UserRef", column = 5, width = 120, alignment = JLabel.CENTER)
		public String getRef() {
			return mOrder.getRef().orElse("");
		}

		@TableAttribute(header = "OrderId", column = 6, width = 120, alignment = JLabel.CENTER)
		public String getOrderId() {
			return String.valueOf( mOrder.getOrderId().get());
		}

		@Override
		public String toString() {
			return this.getSid() + " " + this.getVolPrice() + " orderid: " + this.getOrderId();
		}

	}

}

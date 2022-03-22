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
import com.hoddmimes.te.common.table.TableModel;
import com.hoddmimes.te.common.transport.http.TeRequestException;
import com.hoddmimes.te.messages.generated.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.io.IOException;


public class CryptoShowDepositFrame extends JFrame  {
	private Connector mConnector;
	private MessageFactory mMessageFactory;


	TableModel<TableAddressEntry>  mTableModel;
	Table                      mTable;
	JLabel                     mCashLabel;

	CryptoShowDepositFrame(Connector pConnector ) {
		mConnector = pConnector;
		mMessageFactory = new MessageFactory();

		init();
		loadData();

		this.pack();
		AuxClt.centeredFrame(this);
		this.setVisible(true);
	}


	private void init() {
		GridBagPanel tRootPanel = new GridBagPanel( GridBagConstraints.CENTER);

		JPanel tHeaderPanel =  AuxClt.makeheaderpanel("Deposit / Redrawn Entries");
		tRootPanel.insets( new Insets(20,10,0,10));
		tRootPanel.add( tHeaderPanel);

		tRootPanel.incy();
		tRootPanel.add( createTablePanel());

		this.setTitle("Deposit / Redrawn Entries");
		this.setContentPane( tRootPanel );
	}

	JPanel createTablePanel() {
		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBorder( new EmptyBorder(10,5,10,5));
		JPanel tContentPanel = new JPanel( new BorderLayout());
		tContentPanel.setBorder( new EtchedBorder(2));
		tRootPanel.add( tContentPanel);

		mTableModel = new TableModel(TableAddressEntry.class);

		mTable = new Table(mTableModel, new Dimension(mTableModel.getPreferedWith() + 18, 280), null);
		mTable.setBackground(Color.white);

		JPanel tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 5, 10, 5));
		tTablePanel.add(mTable);

		tContentPanel.add(tTablePanel, BorderLayout.CENTER);
		return tRootPanel;
	}


	private void loadData() {
		mTableModel.clear();
		mTableModel.fireTableDataChanged();

		try {
			JsonObject jResp = mConnector.get("queryCryptoAddressEntries");
			MessageInterface tResp = mMessageFactory.getMessageInstance(AuxJson.tagMessageBody(QueryAddressEntriesResponse.NAME, jResp));
			if (tResp instanceof QueryAddressEntriesResponse) {
				if (((QueryAddressEntriesResponse) tResp).getAddressEntries().isPresent()) {
					for( AddressEntry tAddressEntry : ((QueryAddressEntriesResponse) tResp).getAddressEntries().get())	{
						mTableModel.addEntry( new TableAddressEntry( tAddressEntry ));
					}
					mTableModel.fireTableDataChanged();
				}
			} else {
				JOptionPane.showMessageDialog(null,
						"Load payment Entries failure, reason: " + tResp.toJson().toString(),
						"Load Payment Failure",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
		} catch (TeRequestException | IOException e) {
			JOptionPane.showMessageDialog(null,
					"Load payment Entries failure, reason: " + e.getMessage(),
					"Load Payment Failure",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
	}




	public static class TableAddressEntry {
		public AddressEntry mAddressEntry;


		public TableAddressEntry(AddressEntry pAddressEntry ) {
			mAddressEntry = pAddressEntry;
		}

		@TableAttribute(header = "Coin", column = 1, width = 100, alignment = JLabel.CENTER)
		public String getCoin() {
			return mAddressEntry.getCoin().get();
		}

		@TableAttribute(header = "Payment Type", column = 2, width = 120, alignment = JLabel.CENTER)
		public String getPaymentType() {
			return mAddressEntry.getPaymentType().get();
		}

		@TableAttribute(header = "Address", column = 3, width = 300, alignment = JLabel.LEFT)
		public String getAddress() {
			return mAddressEntry.getAddress().get();
		}

		@TableAttribute(header = "Confirmed", column = 4, width = 70, alignment = JLabel.CENTER)
		public String getConfirmed() {
			return String.valueOf( mAddressEntry.getConfirmed().get());
		}

	}

}

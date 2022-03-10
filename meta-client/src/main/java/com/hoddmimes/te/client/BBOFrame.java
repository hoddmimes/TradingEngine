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


import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;


public class BBOFrame extends JFrame implements TeBroadcastListener, TableCallbackInterface {
	private Connector mConnector;
	private MessageFactory mMessageFactory;

	TableModel<BBOEntry> mBBOTableModel;
	Table mBBOTable;

	BBOFrame( Connector pConnector ) {
		mConnector = pConnector;
		mMessageFactory = new MessageFactory();
		init();

		loadData();

		pConnector.addSubription( this );

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

		tRootPanel.add( AuxClt.makeheaderpanel("BBO"), gc);

		gc.insets = new Insets(0,5, 10, 5 );
		gc.gridy++;
		tRootPanel.add( createTablePanel(), gc);

		this.setTitle("BBO");
		this.setContentPane( tRootPanel );
	}

	JPanel createTablePanel() {
		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBorder( new EmptyBorder(10,5,10,5));
		JPanel tContentPanel = new JPanel( new BorderLayout());
		tContentPanel.setBorder( new EtchedBorder(2));
		tRootPanel.add( tContentPanel);

		mBBOTableModel = new TableModel(BBOEntry.class);

		mBBOTable = new Table(mBBOTableModel, new Dimension(mBBOTableModel.getPreferedWith() + 18, 500), this);
		mBBOTable.setBackground(Color.white);

		JPanel tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 5, 10, 5));
		tTablePanel.add(mBBOTable);

		tContentPanel.add(tTablePanel, BorderLayout.CENTER);
		return tRootPanel;
	}


	private void loadData() {
		List<BBO> tList = new ArrayList<>();
		try {
			for (Market m : mConnector.getMarkets()) {
				JsonObject jBBORsp = mConnector.get("queryBBO/" + String.valueOf(m.getId().get()));
				QueryBBOResponse tBBORsp = (QueryBBOResponse) mMessageFactory.getMessageInstance(AuxJson.tagMessageBody(QueryBBOResponse.NAME, jBBORsp));
				tList.addAll(tBBORsp.getPrices().get());
			}
			mBBOTableModel.clear();
			Collections.sort( tList, new AuxClt.SidSort());
			for (BBO bbo : tList ) {
				mBBOTableModel.addEntry(new BBOEntry(bbo));
			}
			mBBOTableModel.fireTableDataChanged();
		}
		catch( TeRequestException | IOException e) {
			JOptionPane.showMessageDialog(null,
					"Load BBO failure, reason: " + e.getMessage(),
					"Load Data Failure",
					JOptionPane.WARNING_MESSAGE);
			System.exit(0);
		}
	}

	@Override
	public void onTeBdx(MessageInterface pTeBroadcast) {
		if (pTeBroadcast instanceof BdxBBO) {
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
		BBOEntry bboEntry = (BBOEntry) pObject;
		OrderDialog od = new OrderDialog( bboEntry.getSid(), mConnector);
		od.pack();
		AuxClt.centeredFrame( od );
		od.setVisible(true);
	}


	public static class BBOEntry {
		public BBO mBBO;

		public BBOEntry( BBO pBBO ) {
			mBBO = pBBO;
		}

		@TableAttribute(header = "SymbolId", column = 1, width = 55, alignment = JLabel.LEFT)
		public String getSid() {
			return mBBO.getSid().get();
		}

		@TableAttribute(header = "Bid", column = 2, width = 90, alignment = JLabel.RIGHT)
		public String getBid()
		{
			if (!mBBO.getBid().isPresent()) {
				return "--------";
			}
			return mBBO.getBidQty().get() + "@" + AuxClt.fmtInternalPrice( mBBO.getBid().get());
		}

		@TableAttribute(header = "Offer", column = 3, width = 90, alignment = JLabel.RIGHT)
		public String getOffer()
		{
			if (!mBBO.getOffer().isPresent()) {
				return "--------";
			}
			return mBBO.getOfferQty().get() + "@" + AuxClt.fmtInternalPrice( mBBO.getOffer().get());
		}

	}

}

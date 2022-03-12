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
import com.hoddmimes.te.messages.generated.BdxOwnTrade;
import com.hoddmimes.te.messages.generated.MessageFactory;
import com.hoddmimes.te.messages.generated.Position;
import com.hoddmimes.te.messages.generated.QueryPositionResponse;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.io.IOException;


public class PositionFrame extends JFrame implements TeBroadcastListener, TableCallbackInterface {
	private Connector mConnector;
	private MessageFactory mMessageFactory;


	TableModel<PositionEntry>  mTableModel;
	Table                      mTable;
	JLabel                     mCashLabel;

	PositionFrame(Connector pConnector ) {
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

		JPanel tHeaderPanel =  AuxClt.makeheaderpanel("Positions");
		tRootPanel.add( tHeaderPanel, gc);

		mCashLabel = AuxClt.makelabel("");
		gc.insets = new Insets(5,60, 5, 60 );
		gc.gridy++;
		tRootPanel.add( mCashLabel, gc);


		gc.insets = new Insets(0,5, 10, 5 );
		gc.gridy++;
		tRootPanel.add( createTablePanel(), gc);



		this.setTitle("Position");
		this.setContentPane( tRootPanel );
	}

	JPanel createTablePanel() {
		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBorder( new EmptyBorder(10,5,10,5));
		JPanel tContentPanel = new JPanel( new BorderLayout());
		tContentPanel.setBorder( new EtchedBorder(2));
		tRootPanel.add( tContentPanel);

		mTableModel = new TableModel(PositionEntry.class);

		mTable = new Table(mTableModel, new Dimension(mTableModel.getPreferedWith() + 18, 380), this);
		mTable.setBackground(Color.white);

		JPanel tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 5, 10, 5));
		tTablePanel.add(mTable);

		tContentPanel.add(tTablePanel, BorderLayout.CENTER);
		return tRootPanel;
	}





	private void loadData(  QueryPositionResponse pQueryPositionResponse ) {
		mTableModel.clear();
		double tCash = (double) ((double) pQueryPositionResponse.getCash().get() / (double) AuxClt.PRICE_MULTIPLER);
		mCashLabel.setText("Cash holding " + tCash );
		if (pQueryPositionResponse.getPositions().isPresent()) {
			for( Position tPosition : pQueryPositionResponse.getPositions().get() ) {
				mTableModel.addEntry(new PositionEntry( tPosition ));
			}
		}
		mTableModel.fireTableDataChanged();
	}


	private void loadData() {
		mTableModel.clear();
		mTableModel.fireTableDataChanged();

		try {
			JsonObject jResp = mConnector.get("queryPosition");
			MessageInterface tResp = mMessageFactory.getMessageInstance(AuxJson.tagMessageBody(QueryPositionResponse.NAME, jResp));
			if (tResp instanceof QueryPositionResponse) {
				loadData((QueryPositionResponse) tResp);
			} else {
				JOptionPane.showMessageDialog(null,
						"Load Positions failure, reason: " + tResp.toJson().toString(),
						"Load Positions Failure",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
		} catch (TeRequestException | IOException e) {
			JOptionPane.showMessageDialog(null,
					"Load Position failure, reason: " + e.getMessage(),
					"Load Position Failure",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
	}




	@Override
	public void onTeBdx(MessageInterface pTeBroadcast) {
		if (pTeBroadcast instanceof BdxOwnTrade) {
			loadData();
		}
	}

	@Override
	public void tableMouseButton2(Object pObject, int pRow, int pCol) {
		PositionEntry tPositionEntry = (PositionEntry)  pObject;
		OrderDialog od = new OrderDialog( tPositionEntry.getSid(), mConnector);
		od.pack();
		AuxClt.centeredFrame( od );
		od.setVisible(true);
	}

	@Override
	public void tableMouseClick(Object pObject, int pRow, int pCol) {

	}

	@Override
	public void tableMouseDoubleClick(Object pObject, int pRow, int pCol) {

	}


	public static class PositionEntry {
		public Position mPosition;


		public PositionEntry(Position pPosition ) {
			mPosition = pPosition;
		}

		@TableAttribute(header = "SID", column = 1, width = 100, alignment = JLabel.CENTER)
		public String getSid() {
			return mPosition.getSid().get();
		}

		@TableAttribute(header = "Position", column = 2, width = 120, alignment = JLabel.RIGHT)
		public String getPosition() {
			return String.valueOf(mPosition.getPosition().get());
		}

	}

}

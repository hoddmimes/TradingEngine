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
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.transport.http.TeRequestException;
import com.hoddmimes.te.messages.generated.AddOrderRequest;
import com.hoddmimes.te.messages.generated.Order;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class OrderDialog extends JFrame
{
	private String mSid;
	private JTextField mPriceTxtFld;
	private JTextField mQuantityTxtFld;
	private JComboBox<String> mSideComboBox;
	private JTextField mUserTxtFld;
	private JButton mAddOrderBtn, mCancelBtn;
	private Connector mConnector;

	OrderDialog( String pSid, Connector pConnector ) {
		mSid = pSid;
		mConnector = pConnector;
		this.setTitle("Enter Order " + pSid );
		this.setResizable( false );
		this.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
		init();
	}


	private void addOrder() {
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
		tRqst.setSid( mSid ).setPrice( tPrice ).setQuantity( tQuantity ).setSide( tSide ).setRef( tRef );
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
			this.dispose();
		}
		catch( TeRequestException | IOException e) {
			JOptionPane.showMessageDialog(null,
					"Add Order Failure, reason: " + e.getMessage(),
					"Add Order Failure",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
	}


	private void init() {
		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBackground( AuxClt.PANEL_BACKGROUND);
		tRootPanel.setBorder( new EtchedBorder( EtchedBorder.RAISED));
		// Row one
		GridBagConstraints gc = new GridBagConstraints();

		JPanel tRowOnePanel = new JPanel( new GridBagLayout());
		tRowOnePanel.setBackground( AuxClt.PANEL_BACKGROUND);
		gc.anchor = GridBagConstraints.CENTER; gc.gridy = gc.gridx = 0;
		gc.insets = new Insets( 20,0, 0, 0);

		tRowOnePanel.add(AuxClt.makelabel("SID"), gc );
		gc.gridx++; gc.insets.left = 10;
		JTextField tSidTxtFld = AuxClt.maketxtfld( mSid, 85 );
		tSidTxtFld.setEditable( false );
		tSidTxtFld.setBackground( AuxClt.LIGHT_LIGHT_GRAY );
		tRowOnePanel.add( tSidTxtFld, gc );

		tRootPanel.add( tRowOnePanel, BorderLayout.NORTH );

		// Row Two (  Price, Quantity, Side )
		JPanel tTwoOnePanel = new JPanel( new GridBagLayout());
		tTwoOnePanel.setBackground( AuxClt.PANEL_BACKGROUND);
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.insets = new Insets(10,10, 0, 0 );
		gc.gridx = 0; gc.gridy = 0;
		tTwoOnePanel.add(AuxClt.makelabel("Price"), gc );

		mPriceTxtFld =  AuxClt.maketxtfld(65 );
		gc.gridx++;
		tTwoOnePanel.add( mPriceTxtFld, gc );

		gc.gridx++;
		tTwoOnePanel.add(AuxClt.makelabel("Quantity"), gc );
		gc.gridx++;
		mQuantityTxtFld =  AuxClt.maketxtfld(65 );
		tTwoOnePanel.add( mQuantityTxtFld, gc );

		gc.gridx++; gc.insets.left = 30;
		String tSides[] = {"BUY","SELL"};
		mSideComboBox = new JComboBox<>( tSides );
		mSideComboBox.setFont( AuxClt.DEFAULT_FONT_BOLD);
		mSideComboBox.setPreferredSize( new Dimension(72, 22));
		mSideComboBox.setBackground( AuxClt.TXTFLD_BACKGROUND );
		tTwoOnePanel.add( mSideComboBox, gc );

		gc.gridx++; gc.insets.left = 15;
		tTwoOnePanel.add(AuxClt.makelabel("User ref"), gc );
		gc.gridx++; gc.insets.left = 10; gc.insets.right = 20;
		mUserTxtFld =  AuxClt.maketxtfld(80 );
		tTwoOnePanel.add(mUserTxtFld, gc );
		tRootPanel.add( tTwoOnePanel, BorderLayout.CENTER);

		// Row Three (  Price, Quantity, Side )
		JPanel tThreeOnePanel = new JPanel( new GridBagLayout());
		tThreeOnePanel.setBackground( AuxClt.PANEL_BACKGROUND);
		gc.insets = new Insets(20,0, 20, 0 );
		gc.anchor = GridBagConstraints.CENTER;
		gc.gridx = 0; gc.gridy = 0;

		mCancelBtn = AuxClt.makebutton("Cancel", 142);
		tThreeOnePanel.add(mCancelBtn, gc );

		gc.gridx++; gc.insets.left = 20;
		mAddOrderBtn = AuxClt.makebutton("Add Order", 142);
		tThreeOnePanel.add(mAddOrderBtn, gc );

		tRootPanel.add( tThreeOnePanel, BorderLayout.SOUTH);

		this.setContentPane( tRootPanel );

		mAddOrderBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addOrder();
			}
		});

		mCancelBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				OrderDialog.this.dispose();
			}
		});

	}

	public static void main(String[] args) {
		OrderDialog od = new OrderDialog("1:FOOBAR", null);
		od.pack();
		AuxClt.centeredFrame( od );
		od.setVisible(true);
	}

}

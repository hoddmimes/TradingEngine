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
import com.hoddmimes.te.common.Crypto;
import com.hoddmimes.te.common.GridBagPanel;
import com.hoddmimes.te.common.transport.http.TeRequestException;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class CryptoAddRedrawEntryDialog extends JFrame
{
	private static final String BITCOIN = "Bitcoin";
	private static final String ETHEREUM = "Ethereum";

	private JTextField mToAddressTxtFld;
	private JComboBox<String> mCoinComboBox;
	private JButton mSetAddressBtn, mCancelBtn;
	private Connector mConnector;




	CryptoAddRedrawEntryDialog(Connector pConnector ) {
		mConnector = pConnector;
		this.setTitle("Crypto Add Redraw Entry");
		this.setResizable( false );
		this.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
		init();
		this.pack();
		AuxClt.centeredFrame( this );
		this.setVisible( true );
	}


	private void registerRedrawAddress() {
		String tCoin = (String) mCoinComboBox.getSelectedItem();
		String tEndPoint = null;

		if (tCoin.contentEquals(BITCOIN)) {
			tEndPoint = "addRedrawEntry/" + Crypto.CoinType.BTC.name();

		} else {
			tEndPoint = "addRedrawEntry/" + Crypto.CoinType.ETH.name();
		}

		String tToAddress = mToAddressTxtFld.getText();
		if (tToAddress.isEmpty() || (tToAddress.isBlank())) {
				JOptionPane.showMessageDialog(null,
						"Redraw address must not be empty",
						"Invalid Redraw Address",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

		tEndPoint +=  "/" + tToAddress;


		try {
			JsonObject tResponse = mConnector.get(tEndPoint);
			if (tResponse.has("address")) {
				mCancelBtn.setText("Exit");
				mSetAddressBtn.setEnabled(false);
				JOptionPane.showMessageDialog(null,
						"Redraw address is registered ",
						"Redraw Registered",
						JOptionPane.INFORMATION_MESSAGE);
				CryptoAddRedrawEntryDialog.this.dispose();
			} else {
				JOptionPane.showMessageDialog(null,
						"Failed to register redraw address, reason: " + tResponse.toString(),
						"Register Redraw Address Failure",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

		} catch (TeRequestException | IOException e) {
			JOptionPane.showMessageDialog(null,
					"\"Failed to register redraw address, reason: " + e.getMessage(),
					"Register Redraw Address Failure",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
	}



	private JPanel makeButtonPanel() {
		GridBagPanel tButtonPanel = new GridBagPanel( GridBagConstraints.CENTER);
		tButtonPanel.setBackground( AuxClt.PANEL_BACKGROUND);
		tButtonPanel.setBorder( new EtchedBorder( EtchedBorder.RAISED));
		tButtonPanel.insets( new Insets(5,10,5,10));

		mCancelBtn = AuxClt.makebutton("Cancel", 160);
		mCancelBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CryptoAddRedrawEntryDialog.this.dispose();
			}
		});

		mSetAddressBtn = AuxClt.makebutton("Add Redraw Address", 220);

		tButtonPanel.add( mCancelBtn );
		tButtonPanel.incx().add( mSetAddressBtn );

		mSetAddressBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				registerRedrawAddress();
			}
		});


		return tButtonPanel;
	}

	private void init() {
		mToAddressTxtFld = AuxClt.maketxtfld( 333 );

		GridBagPanel tRootPanel = new GridBagPanel( GridBagConstraints.CENTER );
		tRootPanel.setBackground( AuxClt.PANEL_BACKGROUND);
		tRootPanel.insets( new Insets(20,28,0,28));

		// Add Header Text Panel
		GridBagPanel tHdrTxtPanel = new GridBagPanel( GridBagConstraints.CENTER );
		tHdrTxtPanel.setPreferredSize(new Dimension(666,48));
		tHdrTxtPanel.setBackground( AuxClt.PANEL_BACKGROUND);
		tHdrTxtPanel.setBorder( new EtchedBorder( EtchedBorder.RAISED));

		JLabel tLabel = AuxClt.makelabel("Add an address to which you can redraw coins to");
		tLabel.setFont( AuxClt.DEFAULT_FONT_BOLD);

		tHdrTxtPanel.add( tLabel );
		tRootPanel.add( tHdrTxtPanel );


		// Add Coin Selector
		String tCoinsTypes[] = { BITCOIN, ETHEREUM};
		mCoinComboBox = new JComboBox<>( tCoinsTypes );
		mCoinComboBox.setFont( AuxClt.DEFAULT_FONT);
		mCoinComboBox.setBackground( Color.WHITE);
		mCoinComboBox.setPreferredSize( new Dimension(90, 22));

		tRootPanel.incy();
		tRootPanel.add( mCoinComboBox );

		// Add to Address data
		GridBagPanel tToAddrPanel = new GridBagPanel( GridBagConstraints.CENTER );
		tToAddrPanel.setPreferredSize(new Dimension(522,56));
		tToAddrPanel.setBackground( Color.LIGHT_GRAY);
		tToAddrPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

		JLabel tRedrawLbl = AuxClt.makelabel("Redraw to Address");
		tToAddrPanel.add( tRedrawLbl );

		mToAddressTxtFld = AuxClt.maketxtfld(300);
		tToAddrPanel.incx().left(12).add( mToAddressTxtFld);

		tRootPanel.incy().add( tToAddrPanel );

		// Add Button Panel
		tRootPanel.incy().bottom(20).add( makeButtonPanel());

		this.setContentPane( tRootPanel );

	}

	public static void main(String[] args) {
		CryptoAddRedrawEntryDialog rd = new CryptoAddRedrawEntryDialog( null);
		rd.pack();
		AuxClt.centeredFrame( rd );
		rd.setVisible(true);
	}

}

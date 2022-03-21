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
import com.hoddmimes.te.messages.generated.GetDepositEntryRequest;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;

public class CryptoAddDepositEntryDialog extends JFrame
{
	private static final String BITCOIN = "Bitcoin";
	private static final String ETHEREUM = "Ethereum";

	private JTextField mFromAddressTxtFld,mToAddressTxtFld;
	private JComboBox<String> mCoinComboBox;
	private JButton mGetAddressBtn, mCancelBtn;
	private Connector mConnector;

	private JPanel mRootPanel;
	private JPanel mInPanel;




	CryptoAddDepositEntryDialog( Connector pConnector ) {
		mConnector = pConnector;
		this.setTitle("Crypto Add Deposit Entry");
		this.setResizable( true );
		this.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
		init();
		this.pack();
		AuxClt.centeredFrame( this );
		this.setVisible( true );
	}


	private void registerAndGetAddress() {
		String tCoin = (String) mCoinComboBox.getSelectedItem();
		String tEndPoint = null;

		if (tCoin.contentEquals(BITCOIN)) {
			tEndPoint = "addBTCDepositEntry";

		} else {
			String tFromAddress = mFromAddressTxtFld.getText();
			if (tFromAddress.isEmpty() || (tFromAddress.isBlank())) {
				JOptionPane.showMessageDialog(null,
						"ETH from address must not be empty",
						"Invalid From Address",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			tEndPoint = "addETHDepositEntry/" + tFromAddress;
		}

		try {
			JsonObject tResponse = mConnector.get(tEndPoint);
			if (tResponse.has("address")) {
				String tAddress = tResponse.get("address").getAsString();
				mToAddressTxtFld.setText( tAddress );

				GridBagConstraints gc = new GridBagConstraints();
				gc.gridx = 0; gc.gridy = 3; gc.anchor = GridBagConstraints.CENTER;
				gc.insets = new Insets(30,28,10,28);


				mRootPanel.add( makeResultPanel(), gc);
				mRootPanel.validate();
				mGetAddressBtn.setEnabled( false );
				mCancelBtn.setText("Exit");
				this.pack();
				this.validate();
				this.repaint();
			} else {
				JOptionPane.showMessageDialog(null,
						"Failed to get deposit address, reason: " + tResponse.toString(),
						"Get Deposit Address Failure",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

		} catch (TeRequestException | IOException e) {
			JOptionPane.showMessageDialog(null,
					"Failed to get deposit address, reason: " + e.getMessage(),
					"Get Deposit Address Failure",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
	}



	private JPanel makeEthFromPanel() {
		JPanel tPanel = new JPanel( new GridBagLayout());
		tPanel.setBackground( AuxClt.PANEL_BACKGROUND);

		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = gc.gridy = 0; gc.anchor = GridBagConstraints.CENTER;
		gc.insets = new Insets( 0, 10, 0 , 10);

		JLabel tLabel = AuxClt.makelabel("ETH Deposit Source Address");
		tLabel.setToolTipText("Provide the ETH source address from where the ETH will be sent");


		mFromAddressTxtFld = new JTextField();
		mFromAddressTxtFld.setText("Provide the ETH source address from where the ETH will be sent");
		mFromAddressTxtFld.setFont( new Font("Arial", Font.PLAIN+Font.ITALIC, 12));
		mFromAddressTxtFld.setForeground( Color.lightGray);
		mFromAddressTxtFld.setMargin( new Insets(0,5,0,0));
		mFromAddressTxtFld.setPreferredSize( new Dimension(300,22));
		mFromAddressTxtFld.setToolTipText("Provide the ETH source address from where the ETH will be sent");

		mFromAddressTxtFld.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				JTextField source = (JTextField)e.getComponent();
				source.setFont( AuxClt.DEFAULT_FONT);
				source.setForeground(Color.black);
				source.setText("");
				source.removeFocusListener(this);
			}
		});


		tPanel.add( tLabel, gc );
		gc.gridx++;
		tPanel.add( mFromAddressTxtFld , gc);
		return tPanel;
	}

	private JPanel makeInputPanel() {

		mInPanel = new JPanel( new GridBagLayout());
		mInPanel.setBackground( AuxClt.PANEL_BACKGROUND);
		mInPanel.setBorder( new EtchedBorder(EtchedBorder.RAISED));
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = gc.gridy = 0; gc.anchor = GridBagConstraints.CENTER;
		gc.insets = new Insets(5,10,5,10);


		String tCoinsTypes[] = { BITCOIN, ETHEREUM};
		mCoinComboBox = new JComboBox<>( tCoinsTypes );
		mCoinComboBox.setFont( AuxClt.DEFAULT_FONT);
		mCoinComboBox.setBackground( Color.WHITE);
		mCoinComboBox.setPreferredSize( new Dimension(90, 22));
		mInPanel.add( mCoinComboBox,gc  );

		mCoinComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				troggleInData();
			}
		});

		return mInPanel;

	}


	private void  troggleInData() {
		String tCoin = (String) mCoinComboBox.getSelectedItem();
		if (tCoin.contentEquals(BITCOIN )) {
		 mInPanel.remove(1);
		 mRootPanel.validate();
		} else {
			GridBagConstraints gc = new GridBagConstraints();
			gc.gridx = 0; gc.gridy = 1; gc.anchor = GridBagConstraints.CENTER;
			gc.insets = new Insets(5,10,5,10);
			mInPanel.add( makeEthFromPanel(), gc );
			mRootPanel.validate();
			this.pack();
			this.validate();
			this.repaint();
		}
	}


	private JPanel makeResultPanel() {
		JPanel tPanel = new JPanel( new GridBagLayout());
		tPanel.setPreferredSize(new Dimension(600,60));
		tPanel.setBackground( new Color( 0xd8dce3) );
		tPanel.setBorder( new EtchedBorder( EtchedBorder.RAISED));
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.CENTER;
		gc.insets = new Insets(10,10,10,10);

		JLabel tLabel = AuxClt.makelabel("Your Deposit Address");
		tLabel.setFont( new  Font("Arial", Font.BOLD + Font.ITALIC, 14 ));
		tPanel.add( tLabel, gc );

		gc.gridx++;
		tPanel.add( mToAddressTxtFld, gc );
		return tPanel;
	}

	private JPanel makeButtonPanel() {
		JPanel tButtonPanel = new JPanel( new GridBagLayout());
		tButtonPanel.setBackground( AuxClt.PANEL_BACKGROUND);
		tButtonPanel.setBorder( new EtchedBorder( EtchedBorder.RAISED));
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = gc.gridy = 0; gc.anchor = GridBagConstraints.CENTER;
		gc.insets = new Insets(5,10,10,10);

		mCancelBtn = AuxClt.makebutton("Cancel", 160);
		mCancelBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CryptoAddDepositEntryDialog.this.dispose();
			}
		});

		tButtonPanel.add( mCancelBtn, gc );

		mGetAddressBtn = AuxClt.makebutton("Get Deposit Address", 220);
		gc.gridx++;
		tButtonPanel.add( mGetAddressBtn, gc );

		mGetAddressBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				registerAndGetAddress();
			}
		});


		return tButtonPanel;
	}

	private void init() {
		mToAddressTxtFld = AuxClt.maketxtfld( 333 );


		mRootPanel = new JPanel( new GridBagLayout());
		mRootPanel.setBackground( AuxClt.PANEL_BACKGROUND);
		GridBagConstraints rgc = new GridBagConstraints();
		rgc.gridx = rgc.gridy = 0; rgc.anchor = GridBagConstraints.CENTER;
		rgc.insets = new Insets(16,28,10,28);

		// Add Header Text Panel
		JPanel tHdrTxtPanel = new JPanel( new GridBagLayout());
		tHdrTxtPanel.setPreferredSize(new Dimension(666,48));
		tHdrTxtPanel.setBackground( AuxClt.PANEL_BACKGROUND);
		tHdrTxtPanel.setBorder( new EtchedBorder( EtchedBorder.RAISED));
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = gc.gridy = 0; gc.anchor = GridBagConstraints.CENTER;
		gc.insets = new Insets(10,28,10,28);
		JLabel tLabel = AuxClt.makelabel("Add and retreive an address to which you can deposit coins for trading");
		tLabel.setFont( AuxClt.DEFAULT_FONT_BOLD);
		tHdrTxtPanel.add( tLabel, gc );
		mRootPanel.add( tHdrTxtPanel, rgc);

		// Add input / selection panel
		gc.gridy++;
		mRootPanel.add( makeInputPanel(), gc);

		// Add result panel
	//	gc.gridy++;
	//	mRootPanel.add( makeResultPanel(), gc);

		// Add Button Panel
		gc.gridy++;
		gc.insets.bottom = 20;
		mRootPanel.add( makeButtonPanel(), gc);

		this.setContentPane( mRootPanel );

	}

	public static void main(String[] args) {
		CryptoAddDepositEntryDialog od = new CryptoAddDepositEntryDialog( null);
		od.pack();
		AuxClt.centeredFrame( od );
		od.setVisible(true);
	}

}

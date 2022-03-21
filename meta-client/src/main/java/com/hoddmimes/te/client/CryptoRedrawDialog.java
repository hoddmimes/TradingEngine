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
import com.hoddmimes.te.common.Crypto;
import com.hoddmimes.te.common.transport.http.TeRequestException;
import com.hoddmimes.te.messages.SID;
import com.hoddmimes.te.messages.generated.*;
import org.bitcoinj.core.Coin;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class CryptoRedrawDialog extends JFrame
{
	private static final String BITCOIN = "Bitcoin";
	private static final String ETHEREUM = "Ethereum";

	private JTextField mToAddressTxtFld;
	private JTextField mAmountTxtFld;


	private JComboBox<String> mCoinComboBox;
	private JLabel mBalanceLabel;
	private JTextField mBalanceTxtFld;
	private JButton mRedrawBtn, mCancelBtn;
	private Connector mConnector;
	private CryptoScaling mScaling;
	private MessageFactory mMessageFactory;





	CryptoRedrawDialog(Connector pConnector ) {
		mConnector = pConnector;
		this.setTitle("Redraw Crypto");
		this.setResizable( false );
		this.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
		mMessageFactory = new MessageFactory();
		mScaling = new CryptoScaling( mConnector );
		init();
		loadHoldings();
		this.pack();
		AuxClt.centeredFrame( this );
		this.setVisible( true );
	}


	private void redraw() {
		Crypto.CoinType tCoinType = (((String) mCoinComboBox.getSelectedItem()).contentEquals(BITCOIN)) ? Crypto.CoinType.BTC : Crypto.CoinType.ETH;

		CryptoRedrawRequest tRequest = new CryptoRedrawRequest();
		tRequest.setCoin( tCoinType.name());

		// Validate Amount Field
		String tAmountTxt = mAmountTxtFld.getText();
		if (tAmountTxt.isEmpty() || (tAmountTxt.isBlank())) {
			JOptionPane.showMessageDialog(null,
					"Amount must not be empty",
					"Invalid Amount",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		double tAmountDouble = 0;
		try {
			tAmountDouble = Double.parseDouble( tAmountTxt );
		}
		catch( NumberFormatException e) {
			JOptionPane.showMessageDialog(null,
					"Invalid amount format",
					"Invalid Amount",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		long tCoinNominator = mScaling.coinToNominator(tCoinType, tAmountDouble);
		long tTeNominator = mScaling.scaleFromOutsideNotation( tCoinType, tCoinNominator );
		tRequest.setAmount( tTeNominator );

		// Validate TO address
		String tToAddress = mToAddressTxtFld.getText();
		if (tToAddress.isEmpty() || (tToAddress.isBlank())) {
			JOptionPane.showMessageDialog(null,
						"To-address must not be empty",
						"Invalid To Address",
						JOptionPane.WARNING_MESSAGE);
				return;
		}
		tRequest.setAddress( mToAddressTxtFld.getText());

		// Execute redraw
		try {
			JsonObject jResponse = mConnector.post( AuxJson.getMessageBody( tRequest.toJson()),"redrawCrypto");
			if (jResponse.has("txid")) {
				mRedrawBtn.setEnabled( false );;
				mCancelBtn.setText("Exit");

				CryptoRedrawResponse tResponse = (CryptoRedrawResponse) mMessageFactory.getMessageInstance(AuxJson.tagMessageBody(CryptoRedrawResponse.NAME,jResponse));
				String tRemaingAmountStr = mScaling.coinToFriendlyString( tCoinType, mScaling.scaleToOutsideNotation( tCoinType, tResponse.getRemaingCoins().get()));
				JOptionPane.showMessageDialog(null,
						"Successfull redraw \n remaing amount: " + tRemaingAmountStr + " txid: " + tResponse.getTxid().get(),
						"Crypto Redraw Success",
						JOptionPane.INFORMATION_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(null,
						"Failed to redraw crypto, reason: " + jResponse,
						"Crypto Redraw Failure",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
		}
		catch( IOException | TeRequestException e) {
			JOptionPane.showMessageDialog(null,
					"Failed to redraw crypto, reason: " + e.getMessage(),
					"Crypto Redraw Failure",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
	}

	private void loadHoldings()
	{
		Crypto.CoinType tCoinType = (((String) mCoinComboBox.getSelectedItem()).contentEquals(BITCOIN)) ?  Crypto.CoinType.BTC :  Crypto.CoinType.ETH;
		try {
			JsonObject jResponse = mConnector.get("queryPosition");
			QueryPositionResponse tResponse = (QueryPositionResponse) mMessageFactory.getMessageInstance( AuxJson.tagMessageBody( QueryPositionResponse.NAME, jResponse));
			if (tResponse instanceof QueryPositionResponse) {
				QueryPositionResponse tPosRsp = (QueryPositionResponse) tResponse;
				if (tPosRsp.getPositions().isPresent()) {
					for( Position tPos : tPosRsp.getPositions().get()) {
						SID tSid = new SID(tPos.getSid().get());
						if (tSid.getSymbol().contentEquals( tCoinType.name() )) {
							String tFriendlyString = mScaling.coinToFriendlyString(tCoinType,mScaling.scaleToOutsideNotation( tCoinType, tPos.getPosition().get()));
							mBalanceTxtFld.setText( tFriendlyString);
							return;
						}
					}
				}
			}
			mBalanceTxtFld.setText( " 0.00 " + tCoinType.name() );
		}
		catch( IOException | TeRequestException e) {
			JOptionPane.showMessageDialog(null,
					"Failed to get holdings for " + tCoinType.name() + ", reason: " + e.getMessage(),
					"Retreive holdings failure",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
	}


	private JPanel makeInPanel() {
		GridBagPanel tInPanel = new GridBagPanel( GridBagConstraints.CENTER);
		tInPanel.setBackground( AuxClt.PANEL_BACKGROUND);
		tInPanel.setBorder( new EtchedBorder( EtchedBorder.LOWERED));
		tInPanel.insets( new Insets(5,10,5,10));

		// Add Coin Selector
		String tCoinsTypes[] = { BITCOIN, ETHEREUM};
		mCoinComboBox = new JComboBox<>( tCoinsTypes );
		mCoinComboBox.setFont( AuxClt.DEFAULT_FONT);
		mCoinComboBox.setBackground( Color.WHITE);
		mCoinComboBox.setPreferredSize( new Dimension(90, 22));

		mCoinComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadHoldings();
			}
		});

		// Add Balance Data
		GridBagPanel tBalancePanel = new GridBagPanel( GridBagConstraints.CENTER );

		tBalancePanel.setBackground( AuxClt.PANEL_BACKGROUND);
		mBalanceLabel = AuxClt.makelabel("Balance");
		tBalancePanel.add( mBalanceLabel);
		
		mBalanceTxtFld = AuxClt.maketxtfld( 200);
		mBalanceTxtFld.setEditable( false );
		tBalancePanel.incx().left(12).add( mBalanceTxtFld);



		tInPanel.add( mCoinComboBox );
		tInPanel.incy().top(8).add( tBalancePanel);

		return tInPanel;
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
				CryptoRedrawDialog.this.dispose();
			}
		});

		mRedrawBtn = AuxClt.makebutton("Redraw", 220);

		tButtonPanel.add( mCancelBtn );
		tButtonPanel.incx().add( mRedrawBtn );

		mRedrawBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				redraw();
			}
		});


		return tButtonPanel;
	}

	private JPanel makeRedrawPanel() {
		// Add to Address data
		GridBagPanel tRedrawPanel = new GridBagPanel();
		//tRedrawPanel.setPreferredSize(new Dimension(522,66));
		tRedrawPanel.setBackground( Color.LIGHT_GRAY);

		JLabel tAmountLbl = AuxClt.makelabel("Amount");
		mAmountTxtFld = AuxClt.maketxtfld( 180);
		tRedrawPanel.left(20).top(5).add( tAmountLbl).left(12).incx().right(20).add( mAmountTxtFld );


		JLabel tAddressLbl = AuxClt.makelabel("Redraw to Address");
		mToAddressTxtFld = AuxClt.maketxtfld(300);
		tRedrawPanel.incy().left(20).top(10).bottom(5).add( tAddressLbl).left(12).right(20).incx().add( mToAddressTxtFld );

		return tRedrawPanel;
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

		JLabel tLabel = AuxClt.makelabel("Redrawn coins from the TE system");
		tLabel.setFont( AuxClt.DEFAULT_FONT_BOLD);

		tHdrTxtPanel.add( tLabel );
		tRootPanel.add( tHdrTxtPanel );


		// Add input / select panel

		tRootPanel.incy().add( makeInPanel() );

		// Add redraw panel
		tRootPanel.incy().add( makeRedrawPanel() );

		// Add Button Panel
		tRootPanel.incy().bottom(20).add( makeButtonPanel());

		this.setContentPane( tRootPanel );

	}



	public static void main(String[] args) {
		CryptoRedrawDialog rd = new CryptoRedrawDialog( null);
		rd.pack();
		AuxClt.centeredFrame( rd );
		rd.setVisible(true);
	}
}

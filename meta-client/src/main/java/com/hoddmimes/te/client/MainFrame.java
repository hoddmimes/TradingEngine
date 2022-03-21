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

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainFrame extends JFrame
{
	private JButton mBBOButton, mOrderBookButton, mTradesButton, mOwnOrdersButton, mPriceLevelButton, mPositionButton;
	private JButton mCryptoAddDepositButton, mCryptoAddRedrawnButton, mCryptoRedrawButton, mCryptoShowEntityAddresses;
	private Connector mConnector;

	MainFrame( Connector pConnector) {
		mConnector = pConnector;
		init();
	}

	private void init() {
		JPanel tRootPanel = new JPanel( new GridBagLayout());
		tRootPanel.setBackground( AuxClt.PANEL_BACKGROUND);
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = gc.gridy = 0;
		gc.anchor = GridBagConstraints.CENTER;
		gc.insets = new Insets( 10, 60, 10, 60);
		gc.fill = GridBagConstraints.HORIZONTAL;

		JPanel tHeaderPanel = AuxClt.makeheaderpanel("TE Test Client");
		tHeaderPanel.setBackground( AuxClt.PANEL_BACKGROUND);
		tHeaderPanel.setBorder( new EtchedBorder( EtchedBorder.LOWERED));
		tRootPanel.add( tHeaderPanel, gc );

		gc.gridy++; gc.fill = GridBagConstraints.NONE;
		gc.insets = new Insets( 10, 10, 20, 10);
		tRootPanel.add( createMenue(), gc );

		this.setTitle("TE Test Client");
		this.setResizable( false );
		this.setContentPane( tRootPanel );
		this.pack();
		AuxClt.centeredFrame( this );
		this.setVisible( true );
	}

	private JPanel createMenue() {
		JPanel tPanel = new JPanel( new BorderLayout());
		tPanel.setBackground( new Color( 0xcbd0d6));


		JPanel p1 = new JPanel( new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = gc.gridy = 0;
		gc.anchor = GridBagConstraints.CENTER;
		gc.insets = new Insets( 5, 20, 5, 3);
		gc.fill = GridBagConstraints.HORIZONTAL;
		tPanel.setBorder( new EtchedBorder( EtchedBorder.RAISED));

		mBBOButton = AuxClt.makebutton("BBO", 85);
		p1.add(mBBOButton, gc);

		gc.gridx++; gc.insets.left = 3; gc.insets.right = 3; gc.fill = GridBagConstraints.NONE;

		mPriceLevelButton = AuxClt.makebutton("PriceLevels", 130);
		p1.add(mPriceLevelButton, gc);

		gc.gridx++;
		mOrderBookButton = AuxClt.makebutton("OrderBook", 130);
		p1.add(mOrderBookButton, gc);

		gc.gridx++;
		mTradesButton = AuxClt.makebutton("Trades", 130);
		p1.add(mTradesButton, gc);

		gc.gridx++;
		mOwnOrdersButton = AuxClt.makebutton("Own Orders", 130);
		p1.add(mOwnOrdersButton, gc);

		gc.gridx++;
		mPositionButton = AuxClt.makebutton("Positions", 130);
		p1.add(mPositionButton, gc);
		tPanel.add( p1, BorderLayout.NORTH);

		JPanel p2 = new JPanel( new GridBagLayout());
		gc = new GridBagConstraints();
		gc.gridx = gc.gridy = 0;
		gc.anchor = GridBagConstraints.CENTER;
		gc.insets = new Insets( 5, 20, 5, 3);
		gc.fill = GridBagConstraints.HORIZONTAL;

		mCryptoAddDepositButton = AuxClt.makebutton("Get Crypto Deposit Entry", 222);
		p2.add(mCryptoAddDepositButton, gc);

		gc.gridx++; gc.insets.left = 3; gc.insets.right = 3; gc.fill = GridBagConstraints.NONE;

		mCryptoAddRedrawnButton = AuxClt.makebutton("Add Crypto Redraw Entry", 222);
		p2.add(mCryptoAddRedrawnButton, gc);

		gc.gridx++;
		mCryptoRedrawButton = AuxClt.makebutton("Crypto Redraw", 222);
		p2.add(mCryptoRedrawButton, gc);

		gc.gridx++;
		mCryptoShowEntityAddresses = AuxClt.makebutton("Show Crypto Deposit/Redraw Entry", 300);
		p2.add(mCryptoShowEntityAddresses, gc);

		tPanel.add(p2, BorderLayout.SOUTH);


		mBBOButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				BBOFrame tFrame = new BBOFrame( mConnector );
			}
		});

		mPriceLevelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				PriceLevelFrame tFrame = new PriceLevelFrame( mConnector);
			}
		});

		mOrderBookButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				OrderbookFrame tFrame = new OrderbookFrame( mConnector);
			}
		});

		mTradesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TradesFrame tFrame = new TradesFrame( mConnector);
			}
		});

		mOwnOrdersButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				OwnOrderbookFrame tFrame = new OwnOrderbookFrame( mConnector);
			}
		});

		mPositionButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				PositionFrame tFrame = new PositionFrame( mConnector);
			}
		});

		mCryptoAddDepositButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CryptoAddDepositEntryDialog tFrame = new CryptoAddDepositEntryDialog( mConnector );
			}
		});

		mCryptoAddRedrawnButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CryptoAddRedrawEntryDialog tFrame = new CryptoAddRedrawEntryDialog( mConnector );
			}
		});

		mCryptoRedrawButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CryptoRedrawDialog tFrame = new CryptoRedrawDialog( mConnector );
			}
		});

		mCryptoShowEntityAddresses.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CryptoShowDepositFrame tFrame = new CryptoShowDepositFrame( mConnector );
			}
		});

		return tPanel;
	}

}

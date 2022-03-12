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
	private Connector mConnector;

	MainFrame( Connector pConnector) {
		mConnector = pConnector;
		init();
	}

	private void init() {
		JPanel tRootPanel = new JPanel( new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = gc.gridy = 0;
		gc.anchor = GridBagConstraints.CENTER;
		gc.insets = new Insets( 10, 60, 10, 60);
		gc.fill = GridBagConstraints.HORIZONTAL;

		JPanel tHeaderPanel = AuxClt.makeheaderpanel("TE Test Client");
		tHeaderPanel.setBorder( new LineBorder( Color.black, 2));
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
		JPanel tPanel = new JPanel( new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = gc.gridy = 0;
		gc.anchor = GridBagConstraints.CENTER;
		gc.insets = new Insets( 5, 20, 5, 3);
		gc.fill = GridBagConstraints.HORIZONTAL;
		tPanel.setBorder( new EtchedBorder( EtchedBorder.RAISED));

		mBBOButton = AuxClt.makebutton("BBO", 85);
		tPanel.add(mBBOButton, gc);

		gc.gridx++; gc.insets.left = 3; gc.insets.right = 3; gc.fill = GridBagConstraints.NONE;

		mPriceLevelButton = AuxClt.makebutton("PriceLevels", 130);
		tPanel.add(mPriceLevelButton, gc);

		gc.gridx++;
		mOrderBookButton = AuxClt.makebutton("OrderBook", 130);
		tPanel.add(mOrderBookButton, gc);

		gc.gridx++;
		mTradesButton = AuxClt.makebutton("Trades", 130);
		tPanel.add(mTradesButton, gc);

		gc.gridx++;
		mOwnOrdersButton = AuxClt.makebutton("Own Orders", 130);
		tPanel.add(mOwnOrdersButton, gc);

		gc.gridx++;
		mPositionButton = AuxClt.makebutton("Positions", 130);
		tPanel.add(mPositionButton, gc);


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

		return tPanel;
	}

}

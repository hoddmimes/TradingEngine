/*
 * Copyright (c)  Hoddmimes Solution AB 2021.
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

package com.hoddmimes.te.management.gui.mgmt;

import com.hoddmimes.te.common.interfaces.MarketStates;
import com.hoddmimes.te.common.interfaces.TeIpcServices;

import com.hoddmimes.te.messages.generated.*;

import java.util.List;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MarketPanel extends JPanel
{
	Pattern mHHmmPattern = Pattern.compile("^[0-2][0-9]:[0-5][0-9]");
	JPanel                  mContentPanel;
	JScrollPane             mScrollPane;
	ServiceInterface        mServiceInterface;

	public MarketPanel( ServiceInterface pServiceInterface ) {
		this.setLayout(new FlowLayout(FlowLayout.CENTER));
		mContentPanel = new JPanel (new GridBagLayout());
		mScrollPane = new JScrollPane( mContentPanel );
		mServiceInterface = pServiceInterface;

		this.add( mScrollPane );

		init();
	}

	void updateMarketState( MgmtSetMarketsRequest pRqst )
	{
		MgmtSetMarketsResponse tResponse  = (MgmtSetMarketsResponse) mServiceInterface.transceive(TeIpcServices.InstrumentData, pRqst );
		if (tResponse instanceof MgmtSetMarketsResponse) {
			displayMarketData(((MgmtSetMarketsResponse)tResponse).getMarkets().get());
		}
	}

	public void loadData() {


		MgmtGetMarketsResponse tResponse = (MgmtGetMarketsResponse) mServiceInterface.transceive(TeIpcServices.InstrumentData, new MgmtGetMarketsRequest().setRef("X"));
		if (tResponse == null) {
			return;
		}
		displayMarketData( tResponse.getMarkets().get() );
	}


	private void displayMarketData( List<Market> pMarketData ) {
		int tComponentCount = mContentPanel.getComponentCount();
		for (int i = tComponentCount - 1; i >= 0; i--) {
			mContentPanel.remove(i);
		}

		GridBagConstraints gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.insets = new Insets(5,10,0,10);
		gc.gridx = gc.gridy = 0;

		for( Market mkt : pMarketData) {
			MarketEntryPanel mktentry = new MarketEntryPanel( mkt );
			mContentPanel.add( mktentry, gc );
			gc.gridy++;
		}
	}


	private void init() {
		//loadData();
	}

	class MarketEntryPanel extends JPanel
	{
		Market mMarket;
		JTextField mNameTxtFld;
		JTextField mDescriptionTxtFld;
		JTextField mMarketId;
		JTextField mOpenTxtFld, mPreOpenTxtFld, mCloseTxtFld;
		boolean mMarketEnabled;
		JComboBox<MarketStates> mMarketStatesComboBox;
		JButton mUpdateBtn;
		JButton mCloseBtn;



		MarketEntryPanel( Market pMarket) {
			mMarket = pMarket;

			mNameTxtFld = makeTextFields( pMarket.getName().get(), new Dimension(100, 22), false);
			mDescriptionTxtFld = makeTextFields( pMarket.getDescription().get(), new Dimension(260, 22), false);
			mMarketId = makeTextFields( String.valueOf( pMarket.getId().get() ),  new Dimension(30, 22), false);
			mMarketStatesComboBox = new JComboBox<>( MarketStates.values());
			mMarketStatesComboBox.setSelectedItem( MarketStates.valueOf(pMarket.getState().get()) );
			mMarketStatesComboBox.setFont( Management.DEFAULT_FONT_BOLD);
			mMarketStatesComboBox.setEnabled( false );
			mUpdateBtn = new JButton("Update");
			mUpdateBtn.setFont( Management.DEFAULT_FONT_BOLD);
			mUpdateBtn.setBackground(Management.BUTTON_BACKGROUND );
			mUpdateBtn.setToolTipText("Change schedule and \"Update\"");

			mCloseBtn = new JButton("Close");
			mCloseBtn.setFont( Management.DEFAULT_FONT_BOLD);
			mCloseBtn.setBackground(Management.BUTTON_BACKGROUND );
			mCloseBtn.setToolTipText("Hard close, must be enable manually by reschedule and \"Update\"");

			mMarketEnabled = pMarket.getEnabled().get();

			this.setBackground(Management.PANEL_BACKGROUND);
			this.setLayout( new GridBagLayout());
			this.setBorder( new EtchedBorder(2));
			init();

			mCloseBtn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					closeMarket();
				}
			});
			mUpdateBtn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					updateMarket();
				}
			});

		}

		private void invalidTimeFormat(String pTimStr ) {
			JOptionPane.showMessageDialog(this,
					"Invalid time format \"" + pTimStr + "\"",
					"Invalid Time Format",
					JOptionPane.WARNING_MESSAGE);
		}


		private void updateMarket() {
			Matcher m;

			MgmtSetMarketsRequest tRqst = new MgmtSetMarketsRequest().setRef("X");
			tRqst.setMarketId( mMarket.getId().get());
			tRqst.setHardClose( false );

			if ((!mPreOpenTxtFld.getText().isEmpty()) && (!mPreOpenTxtFld.getText().isBlank())) {
				m = mHHmmPattern.matcher( mPreOpenTxtFld.getText() );
				if (!m.matches()) {
					invalidTimeFormat( mPreOpenTxtFld.getText() );
					return;
				}
				tRqst.setPreOpen( mPreOpenTxtFld.getText() );
			}

			if ((!mOpenTxtFld.getText().isEmpty()) && (!mOpenTxtFld.getText().isBlank())) {
				 m = mHHmmPattern.matcher( mOpenTxtFld.getText() );
				 if (!m.matches()) {
					 invalidTimeFormat( mOpenTxtFld.getText() );
					 return;
				 }
				 tRqst.setOpen( mOpenTxtFld.getText() );
			}

			if ((!mCloseTxtFld.getText().isEmpty()) && (!mCloseTxtFld.getText().isBlank())) {
				m = mHHmmPattern.matcher( mCloseTxtFld.getText() );
				if (!m.matches()) {
					invalidTimeFormat( mCloseTxtFld.getText() );
					return;
				}
				tRqst.setClose( mCloseTxtFld.getText() );
			}
			updateMarketState( tRqst );
		}

		private void closeMarket() {
			MgmtSetMarketsRequest tRqst = new MgmtSetMarketsRequest().setRef("X");
			tRqst.setMarketId( mMarket.getId().get());
			tRqst.setHardClose( true );
			updateMarketState( tRqst );
		}


		private void init() {
			GridBagConstraints gc = new GridBagConstraints();
			gc.anchor = GridBagConstraints.NORTHWEST;
			gc.insets = new Insets(10,10,0,0);
			gc.gridy = gc.gridx = 0;


			// Row One
			this.add( makeLabel("Market"), gc );
			gc.gridx++;
			this.add( mNameTxtFld, gc );
			gc.gridx++;
			gc.insets.left = 20;
			this.add( makeLabel("Description"), gc );
			gc.gridx++;
			gc.insets.left = 10;
			this.add( mDescriptionTxtFld, gc );
			gc.gridx++;
			gc.insets.left = 20;
			this.add( makeLabel("Market Id"), gc );
			gc.gridx++;
			gc.insets.left = 10;
			gc.insets.right = 20;
			this.add( mMarketId, gc );

			// Row Two Schedule
			gc.gridy++;
			gc.gridx = 0;
			gc.gridwidth = 4;
			this.add( makeSchedulePanel("08:45", "09:00", "16:30", mMarketEnabled), gc );

			// Row Three States and Buttons
			gc.gridy++;
			gc.gridx = 2;
			gc.gridwidth = 4;
			this.add( makeStateButtonPanel(), gc );

			if (mMarketStatesComboBox.getSelectedItem().toString().contentEquals( MarketStates.CLOSED.name())) {
				mCloseBtn.setEnabled( false );
			}
		}

		private JPanel makeStateButtonPanel() {
			JPanel tPanel = new JPanel( new GridBagLayout());
			tPanel.setBackground( Management.PANEL_BACKGROUND);
			GridBagConstraints gc = new GridBagConstraints();
			gc.anchor = GridBagConstraints.NORTHWEST;
			gc.gridx = gc.gridy = 0;
			gc.insets = new Insets(0, 40, 10,0);

			tPanel.add( makeLabel("Market State"), gc );
			gc.gridx++;
			tPanel.add( mMarketStatesComboBox, gc );
			gc.gridx++;
			gc.insets.left = 40;
			tPanel.add( mUpdateBtn, gc );
			gc.gridx++;
			gc.insets.left = 10;
			tPanel.add( mCloseBtn, gc );
			return tPanel;
		}


		private JPanel makeSchedulePanel( String pOpen, String pPreOpen, String pClose, boolean pEnabled  ) {
			JPanel tPanel = new JPanel( new GridBagLayout());
			tPanel.setBackground( Management.PANEL_BACKGROUND);
			GridBagConstraints gc = new GridBagConstraints();
			gc.anchor = GridBagConstraints.NORTHWEST;
			gc.gridx = gc.gridy = 0;
			gc.insets = new Insets(0, 10, 0,0);

			tPanel.add( makeLabel("pre-open"), gc );
			gc.gridx++;
			mPreOpenTxtFld = makeTextFields( pPreOpen, new Dimension(55,22), true);
			tPanel.add( mPreOpenTxtFld, gc );
			gc.gridx++;
			gc.insets.left = 20;
			tPanel.add( makeLabel("open"), gc );
			gc.gridx++;
			gc.insets.left = 10;
			mOpenTxtFld = makeTextFields( pOpen, new Dimension(55,22), true);
			tPanel.add( mOpenTxtFld, gc );
			gc.gridx++;
			gc.insets.left = 20;
			tPanel.add( makeLabel("close"), gc );
			gc.gridx++;
			gc.insets.left = 10;
			mCloseTxtFld = makeTextFields( pClose, new Dimension(55,22), true);
			tPanel.add( mCloseTxtFld, gc );

			gc.gridx++;
			gc.insets.left = 30;
			JCheckBox tEnableChkBox = new JCheckBox("Market Enabled");
			tEnableChkBox.setFont( new Font("arial", Font.BOLD, 12));
			tEnableChkBox.setBackground( Management.PANEL_BACKGROUND);
			tEnableChkBox.setSelected( pEnabled );
			tEnableChkBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
						tEnableChkBox.setSelected(!tEnableChkBox.isSelected());
				}
			});
			tPanel.add( tEnableChkBox, gc );


			return tPanel;
		}

		private JLabel makeLabel( String pText ) {
			JLabel tLbl = new JLabel(pText );
			tLbl.setFont( Management.DEFAULT_FONT_BOLD);
			return tLbl;
		}

		private JTextField makeTextFields( String pText, Dimension pSize, boolean pEditable ) {
			JTextField tf = new JTextField( pText );
			tf.setEditable( pEditable );
			if (!pEditable) {
				tf.setBackground( Management.TXTFLD_BACKGROUND );
			} else {
				tf.setBackground(Color.WHITE);
			}
			tf.setMargin( new Insets(0,8,0,0));
			tf.setFont( Management.DEFAULT_FONT );
			tf.setPreferredSize( pSize );
			return tf;
		}
	}
}

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
import com.hoddmimes.te.common.interfaces.TeMgmtServices;
import com.hoddmimes.te.messages.generated.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public class SymbolPanel extends JPanel
{
	ServiceInterface mServiceInterface;
	JPanel mTopPanel;
	JPanel mContentPanel;
	JScrollPane mScrollPane;
	JComboBox<MarketEntry> mMarketComboBox;


	public SymbolPanel( ServiceInterface pServiceInterface ) {
		this.setLayout(new BorderLayout());
		mTopPanel =  createTopPanel();
		mContentPanel = new JPanel (new GridBagLayout());
		mServiceInterface = pServiceInterface;
		mScrollPane = new JScrollPane( mContentPanel );
		mScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		mScrollPane.setPreferredSize(new Dimension(this.getWidth(), (this.getHeight() - mTopPanel.getHeight())));
		this.add(mTopPanel, BorderLayout.NORTH);
		this.add( mScrollPane, BorderLayout.CENTER );
	}

	private JPanel createTopPanel()
	{
		JPanel tRoot = new JPanel( new BorderLayout());
		tRoot.setBorder( new EmptyBorder(10,10,10,10));

		JPanel tPanel = new JPanel( new FlowLayout( FlowLayout.CENTER));
		tPanel.setBackground( Management.PANEL_BACKGROUND);
		tPanel.setBorder( new EtchedBorder(3));
		tRoot.add( tPanel, BorderLayout.CENTER);

		mMarketComboBox = new JComboBox<>();
		mMarketComboBox.setFont(Management.DEFAULT_FONT_BOLD);
		tPanel.add( mMarketComboBox );

		mMarketComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				marketChanged();
			}
		});

		return tRoot;
	}

	public void resizeEvent( Dimension pSize) {
		//System.out.println("size: " + this.getSize());

		this.setPreferredSize( new Dimension(pSize.width, (pSize.height - mTopPanel.getHeight() - 50)) );
		this.revalidate();
		mScrollPane.setPreferredSize(new Dimension(this.getWidth(), (this.getHeight() - mTopPanel.getHeight())));

	}

	private void marketChanged() {
		MarketEntry me = (MarketEntry) mMarketComboBox.getSelectedItem();
		loadSymbols( me.getMarketId());
	}

	public void loadMarketData() {
		// Load market data if not already loaded
		if (mMarketComboBox.getItemCount() == 0) {
			MgmtGetMarketsResponse tMktResponse = (MgmtGetMarketsResponse) mServiceInterface.transceive(TeMgmtServices.InstrumentData, new MgmtGetMarketsRequest().setRef("X"));
			if (tMktResponse == null) {
				return;
			}
			if (tMktResponse instanceof MgmtGetMarketsResponse) {
				List<Market> tMktLst = ((MgmtGetMarketsResponse) tMktResponse).getMarkets().get();
				for (int i = 0; i < tMktLst.size(); i++) {
					mMarketComboBox.addItem( new MarketEntry( tMktLst.get(i) ));
				}
			}
			MarketEntry tMarketEntry = (MarketEntry) mMarketComboBox.getSelectedItem();
			MgmtGetSymbolsResponse tSymResponse = (MgmtGetSymbolsResponse) mServiceInterface.transceive(TeMgmtServices.InstrumentData, new MgmtGetSymbolsRequest().setRef("X").setMarketId( tMarketEntry.getMarketId()));
			displaySymbolData( tSymResponse.getSymbols().get());

		}
	}

	void updateSymbol( SymbolEntryPanel pSymbolEntryPanel ) {
		MgmtSetSymbolRequest tRqst = new MgmtSetSymbolRequest().setRef("ss").
				setMarketId( pSymbolEntryPanel.getMarketId()).
				setSuspended( pSymbolEntryPanel.isSuspended()).
				setSid(pSymbolEntryPanel.getSID());

		MgmtSetSymbolResponse tResponse = (MgmtSetSymbolResponse) mServiceInterface.transceive( TeMgmtServices.InstrumentData, tRqst );
		if (tResponse != null) {
			pSymbolEntryPanel.updateSuspendStatus( tResponse.getSymbol().get().getSuspended().orElse(false));
		}
	}

	void loadSymbols( int pMarketId ) {
		MgmtGetSymbolsResponse tResponse = (MgmtGetSymbolsResponse) mServiceInterface.transceive(TeMgmtServices.InstrumentData,
				new MgmtGetSymbolsRequest().setRef("X").setMarketId(pMarketId));

		if (tResponse == null) {
			return;
		}

		displaySymbolData(tResponse.getSymbols().get());
	}

	private void displaySymbolData( List<Symbol> pSymbols )
	{
		int tComponentCount = mContentPanel.getComponentCount();
		for (int i = tComponentCount - 1; i >= 0; i--) {
			mContentPanel.remove(i);
		}

		GridBagConstraints gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.insets = new Insets(0,10,0,10);
		gc.gridx = gc.gridy = 0;

		for( Symbol tSym : pSymbols) {
			mContentPanel.add( new SymbolEntryPanel( tSym ), gc );
			gc.gridy++;
		}
		this.revalidate();
		this.repaint();
	}

	class SymbolEntryPanel extends JPanel
	{
		Symbol  mSymbol;
		JButton     mUpdateBtn;
		JCheckBox   mSuspendedChkBox;

		SymbolEntryPanel( Symbol pSymbol ) {
			mSymbol = pSymbol;
			this.setLayout( new GridBagLayout());
			this.setBorder( new EtchedBorder(2));
			init();
		}

		private void init() {
			GridBagConstraints gc = new GridBagConstraints();
			gc.anchor = GridBagConstraints.NORTHWEST;
			gc.insets = new Insets(5,10,3,0);
			gc.gridy = gc.gridx = 0;

			this.add( makeLabel("symbol"), gc);
			gc.gridx++;
			this.add( makeTextFields(mSymbol.getName().get(), new Dimension(85,22), false), gc );
			gc.gridx++;
			//this.add( makeLabel("market"), gc);
			//gc.gridx++;
			//this.add( makeTextFields(String.valueOf(mSymbol.getMarketId().get()), new Dimension(30,22), false), gc );
			gc.gridx++;
			this.add( makeLabel("SID"), gc);
			gc.gridx++;
			this.add( makeTextFields(mSymbol.getSid().get(), new Dimension(85,22), false), gc );

			boolean tSuspended = mSymbol.getSuspended().orElse( false );
			gc.gridx++;
			mSuspendedChkBox = new JCheckBox("Suspended");
			mSuspendedChkBox.setFont( Management.DEFAULT_FONT);
			mSuspendedChkBox.setSelected( tSuspended );
			this.add( mSuspendedChkBox, gc );

			gc.gridx++;
			gc.insets.right = 10;
			mUpdateBtn = new JButton("Update");
			mUpdateBtn.setBackground( Management.BUTTON_BACKGROUND);
			mUpdateBtn.setFont( Management.DEFAULT_FONT_BOLD);
			this.add( mUpdateBtn, gc );
			if (tSuspended) {
				this.setBackground(Management.SUSPENDED_BACKGROUND);
			} else {
				this.setBackground(Management.PANEL_BACKGROUND);
			}
			mUpdateBtn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					SymbolPanel.this.updateSymbol(SymbolEntryPanel.this);
				}
			});
		}

		void updateSuspendStatus( boolean pSuspended ) {
			mSuspendedChkBox.setSelected( pSuspended );
			if (pSuspended) {
				this.setBackground(Management.SUSPENDED_BACKGROUND);
			} else {
				this.setBackground(Management.PANEL_BACKGROUND);
			}
		}

		boolean isSuspended() {
			return mSuspendedChkBox.isSelected();
		}
		int getMarketId() {
			return mSymbol.getMarketId().get();
		}

		String getSID() {
			return mSymbol.getSid().get();
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

	class MarketEntry
	{
		Market mMarket;

		MarketEntry( Market pMarket ) {
			mMarket = pMarket;
		}

		int getMarketId() {
			return mMarket.getId().get();
		}

		public String toString() {
			return mMarket.getName().get() + " [ " + mMarket.getId().get() + " ] ";
		}
	}
}
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hoddmimes.te.common.db.TEDB;
import com.hoddmimes.te.common.interfaces.TeIpcServices;
import com.hoddmimes.te.messages.generated.MgmtGetWalletRequest;
import com.hoddmimes.te.messages.generated.MgmtGetWalletResponse;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public class CryptoWalletPanel extends JPanel
{
	String[] mWalletTypes = {TEDB.CoinType.BTC.name(), TEDB.CoinType.ETH.name()};
	JComboBox<String> mWalletComboBox;
	MgmtGetWalletResponse mWalletMsg;
	JTextArea             mWalletText;
	ServiceInterface      mServiceInterface;

	CryptoWalletPanel(ServiceInterface pServiceInterface  ) {
		super( new BorderLayout());

		mWalletMsg = null;
		this.add( createTopPanel(), BorderLayout.NORTH );
		this.add( createWalletPanel(), BorderLayout.CENTER);
		mServiceInterface = pServiceInterface;
	}


	public void loadData() {
		getAndUpdateWallet();
	}

	private void getAndUpdateWallet() {
		MgmtGetWalletRequest tRqst = new MgmtGetWalletRequest().setRef("gw");
		String tData = null;
		mWalletMsg = null;
		tRqst.setCoin( (String) mWalletComboBox.getSelectedItem());
		mWalletMsg = (MgmtGetWalletResponse) mServiceInterface.transceive( TeIpcServices.CryptoGwy, tRqst );

		if ((mWalletMsg == null) || (!mWalletMsg.getWalletData().isPresent()) || (mWalletMsg.getWalletData().isEmpty())) {
			tData = "no wallet data is available";
		} else {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonObject jObject = JsonParser.parseString(mWalletMsg.getWalletData().get()).getAsJsonObject();
			tData = gson.toJson(jObject);
		}
		mWalletText.setText( tData );
		mWalletText.revalidate();
		mWalletText.repaint();
	}









	private JPanel createTopPanel() {
		JPanel tRootPanel = new JPanel( new GridBagLayout());
		tRootPanel.setBorder( new EtchedBorder(2));

		GridBagConstraints gc = new GridBagConstraints();
		gc.insets = new Insets(16,0,16,0);
		gc.anchor = GridBagConstraints.CENTER;
		gc.gridx = gc.gridy = 0;

		tRootPanel.add( makeLabel("Wallet"), gc );
		gc.gridx++;
		gc.insets.left = 10;
		mWalletComboBox = new JComboBox<>( mWalletTypes);

		tRootPanel.add( mWalletComboBox, gc );


		return tRootPanel;
	}

	private JPanel createWalletPanel() {
		String tData = null;
		JPanel tRootPanel = new JPanel(new GridBagLayout());
		tRootPanel.setBackground( Management.PANEL_BACKGROUND );
		tRootPanel.setBorder(new EtchedBorder(2));

		mWalletText = new JTextArea("");
		mWalletText.setFont(new Font("Arial", 0, 12));
		mWalletText.setMargin( new Insets(10,10,0,0));
		mWalletText.setBackground(Color.white);

		JScrollPane tScrollPane = new JScrollPane( mWalletText );
		tScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		tScrollPane.setPreferredSize(new Dimension(600,442));

		GridBagConstraints gc = new GridBagConstraints();
		gc.insets = new Insets(10, 20, 10, 20);
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.gridx = gc.gridy = 0;

		tRootPanel.add(tScrollPane, gc);
		return tRootPanel;
	}



	private JLabel makeLabel( String pText ) {
		JLabel tLbl = new JLabel(pText );
		tLbl.setFont( Management.DEFAULT_FONT_BOLD);
		return tLbl;
	}


}

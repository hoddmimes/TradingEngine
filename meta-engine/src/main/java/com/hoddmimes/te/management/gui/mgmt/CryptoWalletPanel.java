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
import com.hoddmimes.te.common.Crypto;
import com.hoddmimes.te.common.GridBagPanel;
import com.hoddmimes.te.common.interfaces.TeService;
import com.hoddmimes.te.messages.generated.MgmtGetWalletRequest;
import com.hoddmimes.te.messages.generated.MgmtGetWalletResponse;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CryptoWalletPanel extends JPanel
{
	String[] mWalletTypes = {Crypto.CoinType.BTC.name(), Crypto.CoinType.ETH.name()};
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
		mWalletMsg = (MgmtGetWalletResponse) mServiceInterface.transceive( TeService.CryptoGwy.name(), tRqst );

		if ((mWalletMsg == null) || (!mWalletMsg.getWalletData().isPresent()) || (mWalletMsg.getWalletData().isEmpty())) {
			tData = "no wallet data is available";
		}
		else {
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

		mWalletComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getAndUpdateWallet();
			}
		});

		tRootPanel.add( mWalletComboBox, gc );


		return tRootPanel;
	}

	private JPanel createWalletPanel() {
		String tData = null;
		GridBagPanel tRootPanel = new GridBagPanel( GridBagConstraints.CENTER);
		tRootPanel.setBackground( Management.PANEL_BACKGROUND );
		tRootPanel.setBorder(new EtchedBorder(2));
		tRootPanel.insets( new Insets(5,0,5,0));
		mWalletText = new JTextArea("kalle");
		mWalletText.setFont(new Font("Arial", Font.PLAIN, 12));
		mWalletText.setMargin( new Insets(20,20,20,20));
		mWalletText.setBackground(Color.white);

		/*
		JScrollPane tScrollPane = new JScrollPane( mWalletText );
		tScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		tScrollPane.setPreferredSize(new Dimension(600,442));
		*/

		tRootPanel.add(mWalletText);
		return tRootPanel;
	}



	private JLabel makeLabel( String pText ) {
		JLabel tLbl = new JLabel(pText );
		tLbl.setFont( Management.DEFAULT_FONT_BOLD);
		return tLbl;
	}


}

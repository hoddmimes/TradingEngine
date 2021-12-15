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
import com.hoddmimes.te.messages.generated.MsgLogEntry;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;

public class MsgDetailFrame extends JFrame
{
	MsgLogEntry mMsgEntry;

	MsgDetailFrame(MsgLogEntry pMsgEntry  ) {
		mMsgEntry = pMsgEntry;
		init();
	}

	private void init() {
		JPanel tRootPanel = new JPanel( new BorderLayout() );
		tRootPanel.add( createTopPanel(), BorderLayout.NORTH );
		tRootPanel.add( createMsgPanel(), BorderLayout.CENTER);
		tRootPanel.add( createButtonPanel(), BorderLayout.SOUTH);
		this.setContentPane( tRootPanel );
		this.pack();

	}

	private JPanel createTopPanel() {
		JPanel tRootPanel = new JPanel( new GridBagLayout());
		tRootPanel.setBorder( new EtchedBorder(2));

		GridBagConstraints gc = new GridBagConstraints();
		gc.insets = new Insets(10,20,10,0);
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.gridx = gc.gridy = 0;

		tRootPanel.add( makeLabel("Account"), gc );
		gc.gridx++;
		gc.insets.left = 10;

		JTextField tAccTxtFld = makeTextFields( mMsgEntry.getAccount().get(), new Dimension(110,22), false);
		tRootPanel.add( tAccTxtFld, gc  );

		gc.gridx++;
		gc.insets.left = 32;
		tRootPanel.add( makeLabel("Bromma"), gc );

		gc.gridx++;
		gc.insets.left = 10;
		gc.insets.right = 20;

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		JTextField tTimeTxtFld = makeTextFields( sdf.format(mMsgEntry.getTimeStamp().get()), new Dimension(220,22), false);
		tRootPanel.add( tTimeTxtFld, gc  );
		return tRootPanel;
	}

	private JPanel createMsgPanel() {
		JPanel tRootPanel = new JPanel(new GridBagLayout());
		tRootPanel.setBackground( Color.white );
		tRootPanel.setBorder(new EtchedBorder(2));

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonObject jObject = JsonParser.parseString(mMsgEntry.getLogMsg().get()).getAsJsonObject();

		System.out.println( gson.toJson(jObject) );

		JTextArea tTextArea = new JTextArea(gson.toJson(jObject));
		tTextArea.setFont(new Font("Arial", 0, 12));
		tTextArea.setBackground(Color.white);

		JScrollPane tScrollPane = new JScrollPane( tTextArea );
		tScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		tScrollPane.setPreferredSize(new Dimension(600,442));

		GridBagConstraints gc = new GridBagConstraints();
		gc.insets = new Insets(10, 20, 10, 20);
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.gridx = gc.gridy = 0;

		tRootPanel.add(tScrollPane, gc);
		return tRootPanel;
	}


	private JPanel createButtonPanel() {
		JPanel tRootPanel = new JPanel(new GridBagLayout());
		tRootPanel.setBorder(new EtchedBorder(2));

		GridBagConstraints gc = new GridBagConstraints();
		gc.insets = new Insets(10, 20, 10, 20);
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.gridx = gc.gridy = 0;

		JButton tCloseBtn = new JButton("Close");
		tCloseBtn.setFont( Management.DEFAULT_FONT);
		tCloseBtn.setBackground( Management.BUTTON_BACKGROUND );
		tRootPanel.add(tCloseBtn, gc);

		tCloseBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MsgDetailFrame.this.dispose();;
			}
		});

		return tRootPanel;
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

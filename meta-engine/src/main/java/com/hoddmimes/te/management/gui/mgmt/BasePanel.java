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

import com.hoddmimes.te.messages.generated.Account;

import java.util.Comparator;
import java.awt.*;
import javax.swing.*;


public class BasePanel extends JPanel
{
		static final Font DEFAULT_FONT = new Font("Arial", Font.PLAIN, 14 );
		static final Font DEFAULT_FONT_BOLD = new Font("Arial", Font.BOLD, 14 );
		static final Color BUTTON_BACKGROUND = new Color(0xcad9cd);
		static final Color PANEL_BACKGROUND = new Color(0xe6e2d8);
		static final Color TXTFLD_BACKGROUND = new Color(0xdfdfdd);
		static final Color SUSPENDED_BACKGROUND = new Color(0xffa366);

		ServiceInterface mServiceInterface;


	BasePanel( ServiceInterface pServiceInterface) {
		mServiceInterface = pServiceInterface;
	}



	protected JLabel makeLabel( String pText ) {
		JLabel tLbl = new JLabel(pText );
		tLbl.setFont( Management.DEFAULT_FONT_BOLD);
		return tLbl;
	}

	protected JTextField makeTextFields( String pText, Dimension pSize, boolean pEditable ) {
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

	static class AccountSort implements Comparator<Account>
	{
		@Override
		public int compare(Account A1, Account A2) {
			return A1.getAccountId().get().compareTo( A2.getAccountId().get());
		}
	}
}

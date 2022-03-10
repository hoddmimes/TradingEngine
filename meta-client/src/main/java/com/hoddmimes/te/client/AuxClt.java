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


import java.util.List;
import javax.swing.*;;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

public class AuxClt {
	public static final long PRICE_MULTIPLER = 10000L;

	static final Font DEFAULT_FONT = new Font("Arial", Font.PLAIN, 14);
	static final Font DEFAULT_FONT_BOLD = new Font("Arial", Font.BOLD, 14);
	static final Color BUTTON_BACKGROUND = new Color(0xcad9cd);
	static final Color LIGHT_LIGHT_GRAY = new Color( 0xe6e6e6 );
	static final Color PANEL_BACKGROUND = new Color(0xe6e2d8);
	static final Color TXTFLD_BACKGROUND = new Color(0xdfdfdd);
	static final Color SUSPENDED_BACKGROUND = new Color(0xffa366);

	private static String cRefTim;
	private static long cRefCounter = 1;

	static String fmtInternalPrice(long tInternalPrice) {
		double dPrice = (double) tInternalPrice / (double) PRICE_MULTIPLER;
		NumberFormat nbf;
		nbf = NumberFormat.getInstance(Locale.US);
		nbf.setMaximumFractionDigits(2);
		nbf.setMinimumFractionDigits(2);
		nbf.setGroupingUsed(false);
		return nbf.format(dPrice);
	}



	static JLabel makelabel(String pText) {
		return makelabel(pText, null);
	}

	static JLabel makelabel(String pText, Integer pPreferedWidth) {
		JLabel tLabel = new JLabel(pText);
		tLabel.setFont(DEFAULT_FONT);
		if (pPreferedWidth != null) {
			tLabel.setPreferredSize(new Dimension(pPreferedWidth, 22));
		}
		return tLabel;
	}

	static JTextField maketxtfld(Integer pPreferedWidth) {
		return maketxtfld(null, pPreferedWidth);
	}

	static JTextField maketxtfld(String pText, Integer pPreferedWidth) {
		JTextField tTxtFld = new JTextField(((pText != null) ? pText : ""));
		tTxtFld.setFont(DEFAULT_FONT);
		tTxtFld.setMargin(new Insets(0, 4, 0, 0));
		tTxtFld.setPreferredSize(new Dimension(pPreferedWidth, 22));
		return tTxtFld;
	}

	static JTextField makepwdfld(Integer pPreferedWidth) {
		return maketxtfld(null, pPreferedWidth);
	}

	static JPasswordField makepwdfld(String mPassword, Integer pPreferedWidth) {
		JPasswordField tPwdFld = new JPasswordField(((mPassword != null) ? mPassword : ""));
		tPwdFld.setFont(DEFAULT_FONT);
		tPwdFld.setMargin(new Insets(0, 4, 0, 0));
		tPwdFld.setPreferredSize(new Dimension(pPreferedWidth, 22));
		return tPwdFld;
	}

	static JButton makebutton(String pText, Integer pPreferedWidth) {
		JButton tButton = new JButton(pText);
		tButton.setFont(DEFAULT_FONT_BOLD);
		tButton.setPreferredSize(new Dimension(pPreferedWidth, 22));
		tButton.setBackground(BUTTON_BACKGROUND);
		return tButton;
	}

	static JPanel makeheaderpanel( String pHeaderText) {
		return makeheaderpanel( pHeaderText, null);
	}

	static JPanel makeheaderpanel( String pHeaderText, Color pColor) {
		JPanel tPanel = new JPanel( new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = gc.gridy = 0;
		gc.anchor = GridBagConstraints.CENTER;
		gc.insets = new Insets( 10,10,10,10 );
		tPanel.setBorder( new LineBorder( ((pColor != null) ? pColor : Color.black), 1));
		JLabel tLabel = makelabel( pHeaderText );
		tLabel.setFont( DEFAULT_FONT_BOLD);
		if (pColor != null) {
			tLabel.setForeground( pColor );
		}
		tPanel.add( tLabel, gc );
		return tPanel;
	}

	static JPanel makeheaderwithsymbolspanel( String pHeaderText, JComboBox<String> pInstruments) {
		return makeheaderwithsymbolspanel( pHeaderText, pInstruments,null);
	}

	static JPanel makeheaderwithsymbolspanel( String pHeaderText, JComboBox<String> pInstruments, Color pColor) {
		JPanel tPanel = new JPanel( new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = gc.gridy = 0;
		gc.anchor = GridBagConstraints.CENTER;
		gc.insets = new Insets( 10,10,5,10 );
		tPanel.setBorder( new LineBorder( ((pColor != null) ? pColor : Color.black), 1));
		JLabel tLabel = makelabel( pHeaderText );
		tLabel.setFont( DEFAULT_FONT_BOLD);
		if (pColor != null) {
			tLabel.setForeground( pColor );
		}
		tPanel.add( tLabel, gc );
		gc.gridy++;
		gc.insets.bottom = 15;
		tPanel.add( pInstruments, gc );
		return tPanel;
	}





	static void centeredFrame(javax.swing.JFrame objFrame){
		Dimension objDimension = Toolkit.getDefaultToolkit().getScreenSize();
		int iCoordX = (objDimension.width - objFrame.getWidth()) / 2;
		int iCoordY = (objDimension.height - objFrame.getHeight()) / 2;
		objFrame.setLocation(iCoordX, iCoordY);
	}

	static void centeredFrame(javax.swing.JDialog objDialog){
		Dimension objDimension = Toolkit.getDefaultToolkit().getScreenSize();
		int iCoordX = (objDimension.width - objDialog.getWidth()) / 2;
		int iCoordY = (objDimension.height - objDialog.getHeight()) / 2;
		objDialog.setLocation(iCoordX, iCoordY);
	}

	private static String getSidFromObject( Object pObject ) {
		try {
			Method m = pObject.getClass().getDeclaredMethod("getSid");
			Optional<String> tOptionalSid = (Optional<String>) m.invoke( pObject );
			return tOptionalSid.orElse( null);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}


	static String getUserRef() {
		if (cRefTim == null) {
			SimpleDateFormat sdf = new SimpleDateFormat("HHmm:");
			cRefTim = sdf.format(System.currentTimeMillis());
		}
		return cRefTim + cRefCounter++;
	}

	static class SidSort implements Comparator
	{
		@Override
		public int compare(Object obj1, Object  obj2) {
			String tSid1 = getSidFromObject( obj1 );
			String tSid2 = getSidFromObject( obj2 );
			return tSid1.compareTo( tSid2 );
		}
	}



}

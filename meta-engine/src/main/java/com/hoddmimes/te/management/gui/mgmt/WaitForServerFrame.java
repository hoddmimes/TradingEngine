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

import com.hoddmimes.te.common.interfaces.TeService;
import com.hoddmimes.te.common.ipc.IpcService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public class WaitForServerFrame extends JFrame
{
	 IpcService mIpcService;

	 WaitForServerFrame(IpcService pService)
	 {
		this.mIpcService = pService;
		this.setContentPane( createPane(pService.getGroupAddress(), pService.getGroupPort()) );
		this.pack();
		this.setLocationRelativeTo(null);
		this.setTitle("Waiting for TE");
	 }

	 JPanel createPane(String pGrpAddress, int pGrpPort) {
		 JPanel tPanel = new JPanel( new BorderLayout());
		 tPanel.setBackground( BasePanel.PANEL_BACKGROUND );
		 tPanel.setBorder( new EtchedBorder(3));

		 JPanel tHdrPanel = new JPanel(new FlowLayout( FlowLayout.CENTER ));
		 tHdrPanel.setBorder( new EmptyBorder(10,10,10,10));
		 JLabel tLbl = new JLabel("Waiting for TE system to come online ...");
		 tLbl.setBackground( BasePanel.PANEL_BACKGROUND );
		 tLbl.setFont( new Font( "Arial", Font.PLAIN, 16));
		 tHdrPanel.add( tLbl );
		 tPanel.add( tHdrPanel, BorderLayout.CENTER);

		 JPanel tInfoPanel = new JPanel(new FlowLayout( FlowLayout.CENTER ));
		 tInfoPanel.setBorder( new EmptyBorder(10,10,10,10));
		 tLbl = new JLabel("( group-address: " + pGrpAddress + " group-port: " + pGrpPort + " )");
		 tLbl.setBackground( BasePanel.PANEL_BACKGROUND );
		 tLbl.setFont( new Font( "Arial", Font.ITALIC, 12));
		 tInfoPanel.add( tLbl );
		 tPanel.add( tInfoPanel, BorderLayout.SOUTH);


		return tPanel;
	 }

	 public void waitForNamedService( String pServiceName ) {
		 mIpcService.waitForService( pServiceName );
		 this.dispose();
	 }



	public static void main(String[] args) {
		IpcService tIpcService = new IpcService( "224.20.20.20", 3939, null);
		WaitForServerFrame f = new WaitForServerFrame(tIpcService);
		f.setVisible(true);
		f.waitForNamedService(TeService.MatchingService.name() );
	}
}

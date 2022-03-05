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
import com.hoddmimes.te.management.gui.table.Table;
import com.hoddmimes.te.management.gui.table.TableAttribute;
import com.hoddmimes.te.management.gui.table.TableModel;
import com.hoddmimes.te.messages.generated.MgmtActiveSession;
import com.hoddmimes.te.messages.generated.MgmtQueryActiveSessionsRequest;
import com.hoddmimes.te.messages.generated.MgmtQueryActiveSessionsResponse;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;

public class StatSessionPanel extends BasePanel {

	private CounterPanel mCounterPanel;
	private TableModel<StatSessionPanel.SessionEntry> mSessTableModel;
	private Table mSessTable;
	private JLabel mStartTimeLabel;

	StatSessionPanel(ServiceInterface pServiceInterface) {
		super(pServiceInterface);
		init();
	}

	private void init()
	{
		this.setLayout( new BorderLayout() );
		this.setBackground( PANEL_BACKGROUND );

		// Create and add counter panel
		JPanel tTopPanel = new JPanel( new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = gc.gridy = 0;
		gc.anchor = GridBagConstraints.CENTER;
		gc.insets = new Insets(16,0,8,0);

		mStartTimeLabel = new JLabel();
		mStartTimeLabel.setFont( new Font("Arial", Font.PLAIN, 14));
		tTopPanel.add( mStartTimeLabel, gc );
		gc.gridy++;
		gc.insets = new Insets(6,0,8,0);
		mCounterPanel = new CounterPanel( false );
		tTopPanel.add( mCounterPanel, gc );
		this.add( tTopPanel, BorderLayout.NORTH );


		mSessTableModel = new TableModel(StatSessionPanel.SessionEntry.class);

		mSessTable = new Table(mSessTableModel, new Dimension(mSessTableModel.getPreferedWith() + 18, 280), null);
		mSessTable.setBackground(Color.white);
		mSessTable.setFont(new Font("Arial", Font.PLAIN, 14));

		JPanel tTablePanel = new JPanel();
		tTablePanel.setBorder( new EmptyBorder(10,10,10,10));
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 5, 10, 5));
		tTablePanel.add(mSessTable);

		this.add(tTablePanel, BorderLayout.CENTER);

	}

	public void refreshStatistics() {
		MgmtQueryActiveSessionsRequest tReq = new MgmtQueryActiveSessionsRequest().setRef("qss");
		MgmtQueryActiveSessionsResponse tResp = (MgmtQueryActiveSessionsResponse) mServiceInterface.transceive(TeService.SessionService.name(), tReq );

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		mStartTimeLabel.setText( "System Starting Time " + sdf.format( tResp.getSysStarTime().get()));
		mCounterPanel.loadStatistics( tResp.getSessionCounters().get());
		mSessTableModel.clear();
		for( MgmtActiveSession tActSess : tResp.getSessions().get()) {
			mSessTableModel.addEntry( new SessionEntry(tActSess ));
		}
		this.revalidate();
		this.repaint();
	}



	public  class SessionEntry {
		MgmtActiveSession mSessionEntry;


		public SessionEntry(MgmtActiveSession pSessionEntry ) {
			mSessionEntry = pSessionEntry;
		}


		@TableAttribute(header = "Account", column = 1, width = 120, alignment = JLabel.LEFT)
		public String getAccount() {
			return mSessionEntry.getAccount().get();
		}

		@TableAttribute(header = "SessionId", column = 2, width = 200, alignment = JLabel.LEFT)
		public String getSessionId() {
			return mSessionEntry.getSessionId().get();
		}

		@TableAttribute(header = "Creation Time", column = 3, width = 200, alignment = JLabel.CENTER)
		public String getCreationTime() {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			return sdf.format( mSessionEntry.getCreationTime().get());
		}

	}

}

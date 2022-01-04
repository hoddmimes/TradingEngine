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

import com.hoddmimes.te.common.interfaces.TeMgmtServices;
import com.hoddmimes.te.management.gui.table.Table;
import com.hoddmimes.te.management.gui.table.TableAttribute;
import com.hoddmimes.te.management.gui.table.TableModel;
import com.hoddmimes.te.messages.SID;
import com.hoddmimes.te.messages.generated.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StatMarketDataPanel extends BasePanel {

	private CounterPanel mCounterPanel;
	private TableModel<TopicEntry> mTopicTableModel;
	private Table mTopicTable;

	StatMarketDataPanel(ServiceInterface pServiceInterface) {
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

		mCounterPanel = new CounterPanel( false );
		tTopPanel.add( mCounterPanel, gc );
		this.add( tTopPanel, BorderLayout.NORTH );


		mTopicTableModel = new TableModel(TopicEntry.class);

		mTopicTable = new Table(mTopicTableModel, new Dimension(mTopicTableModel.getPreferedWith() + 18, 220), null);
		mTopicTable.setBackground(Color.white);
		mTopicTable.setFont(new Font("Arial", Font.PLAIN, 14));

		JPanel tTablePanel = new JPanel();
		tTablePanel.setBorder( new EmptyBorder(10,10,10,10));
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 5, 10, 5));
		tTablePanel.add(mTopicTable);

		this.add(tTablePanel, BorderLayout.CENTER);

	}

	public void refreshStatistics() {
		MgmtQueryMarketDataRequest tReq = new MgmtQueryMarketDataRequest().setRef("qmms");
		MgmtQueryMarketDataResponse tResp = (MgmtQueryMarketDataResponse) mServiceInterface.transceive(TeMgmtServices.MarketData, tReq );

		mCounterPanel.loadStatistics( tResp.getCounters().get());
		mTopicTableModel.clear();
		List<MgmtTropicEntry> tTopicList = tResp.getSubscriptions().get();
		Collections.sort( tTopicList, new TopicSorter());

		for( MgmtTropicEntry tTopicEntry : tTopicList ) {
			mTopicTableModel.addEntry( new TopicEntry( tTopicEntry ));
		}
		this.revalidate();
		this.repaint();
	}



	public class TopicEntry {
		MgmtTropicEntry mTopicEntry;
		NumberFormat numfmt;

		public TopicEntry(MgmtTropicEntry pTopicEntry ) {
			mTopicEntry = pTopicEntry;
		}


		@TableAttribute(header = "Account", column = 1, width = 100, alignment = JLabel.LEFT)
		public String getAccount() {
			return mTopicEntry.getAccount().get();
		}

		@TableAttribute(header = "Session Id", column = 2, width = 140, alignment = JLabel.CENTER)
		public String getSessionId() {
			return mTopicEntry.getSessionId().get();
		}

		@TableAttribute(header = "Topic", column = 3, width = 260, alignment = JLabel.LEFT)
		public String getTopic() {
			return mTopicEntry.getTopic().get();
		}

	}

	class TopicSorter implements Comparator<MgmtTropicEntry>
	{
		@Override
		public int compare(MgmtTropicEntry T1, MgmtTropicEntry T2) {
			if (T1.getAccount().get().compareTo( T2.getAccount().get()) == 0) {
				return T1.getSessionId().get().compareTo( T2.getSessionId().get());
			}
			return T1.getAccount().get().compareTo( T2.getAccount().get());
		}
	}

}

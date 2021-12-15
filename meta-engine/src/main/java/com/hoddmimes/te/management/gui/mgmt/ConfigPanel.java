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

import com.hoddmimes.te.management.gui.table.Table;
import com.hoddmimes.te.management.gui.table.TableAttribute;
import com.hoddmimes.te.management.gui.table.TableCallbackInterface;
import com.hoddmimes.te.management.gui.table.TableModel;
import com.hoddmimes.te.messages.generated.MgmtComponent;
import com.hoddmimes.te.messages.generated.MgmtConfigurationBdx;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ConfigPanel extends JPanel implements TableCallbackInterface {
	private static final Font DEFAULT_FONT = new Font("Arial", Font.PLAIN, 14 );

	TableModel<ComponentEntity> mComponentTableModel;
	Table mTable;
	Timer mTimer;


	public ConfigPanel() {
		this.setLayout(new FlowLayout( FlowLayout.CENTER));
		init();
		mTimer = new Timer( 10000,  new InactivityListner());
		mTimer.start();
	}

	List<ComponentEntity> getServiceComponents() {
		List<ComponentEntity> tComponents = new ArrayList<>( mComponentTableModel.getObjects());
		return tComponents;
	}



	public void configurationUpdate(MgmtConfigurationBdx pUpdBdx ) {
		String tHost = pUpdBdx.getHost().get();
		for (MgmtComponent mc : pUpdBdx.getComponents().get()) {
			ComponentEntity tUpdCe = new ComponentEntity(tHost, mc);

			ComponentEntity ce = componentExists( tUpdCe );

			if (ce == null) {
				mComponentTableModel.addEntry(tUpdCe);
			} else {
				ce.updateLastSeen();
				mComponentTableModel.fireTableDataChanged();
			}
		}
	}

	public ComponentEntity getMgmtComponent( String pServiceName ) {
		List<ComponentEntity> tComponents = (List<ComponentEntity>) mComponentTableModel.getObjects();
		for(ComponentEntity ce : tComponents) {
			if (ce.mName.contentEquals( pServiceName)) {
				return ce;
			}
		}
		return null;
	}



	void checkInactiveComponents() {
		long tNow = System.currentTimeMillis();
		List<ComponentEntity> tComponents = (List<ComponentEntity>) mComponentTableModel.getObjects();
		for(ComponentEntity ce : tComponents) {
			long tTimDiff = tNow - ce.mLastSeen;
			if (tTimDiff >= 30000L) {
				mComponentTableModel.remove( ce );
				mComponentTableModel.fireTableDataChanged();
			}
		}
	}

	private ComponentEntity componentExists(ComponentEntity pComponentEntry ) {
		List<ComponentEntity> tComponents = (List<ComponentEntity>) mComponentTableModel.getObjects();
		for(ComponentEntity ce : tComponents) {
			if (ce.isSame( pComponentEntry )) {
				return ce;
			}
		}
		return null;
	}

	private void init() {
		this.setBorder(new EmptyBorder(10, 10, 10, 10));

		// Create Table
		mComponentTableModel = new TableModel(ComponentEntity.class);

		mTable = new Table(mComponentTableModel, new Dimension(mComponentTableModel.getPreferedWith(), 400), this);
		mTable.setBackground(Color.white);

		JPanel tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 0, 10, 0));
		tTablePanel.add(mTable);

		this.add(tTablePanel);
	}

	@Override
	public void tableMouseButton2(Object pObject, int pRow, int pCol) {

	}

	@Override
	public void tableMouseClick(Object pObject, int pRow, int pCol) {

	}

	@Override
	public void tableMouseDoubleClick(Object pObject, int pRow, int pCol) {

	}

	class InactivityListner implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e) {
			checkInactiveComponents();
		}
	}

	public class ComponentEntity
	{
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

		public String mName;
		public String mHost;
		public int mPort;
		public String mCreateTime;
		public long mLastSeen;


		public ComponentEntity(String pHost, MgmtComponent pMgmtComponent  ) {
			mName  = pMgmtComponent.getName().get();
			mPort = pMgmtComponent.getPort().get();
			mCreateTime = pMgmtComponent.getCretime().get();
			mHost = pHost;
			mLastSeen = System.currentTimeMillis();

		}

		public void updateLastSeen() {
			this.mLastSeen = System.currentTimeMillis();
		}

		public boolean isSame( ComponentEntity ce ) {
			if ((ce.mCreateTime.contentEquals( mCreateTime)) &&
					(ce.mHost.contentEquals( mHost)) &&
					(ce.mPort == mPort) &&
					(ce.mName.contentEquals(mName))) {
				return true;
			}
			return false;
		}


		@TableAttribute(header = "Name", column = 1, width = 120, alignment = JLabel.LEFT)
		public String getName() {
			return mName;
		}

		@TableAttribute(header = "Host", column = 2, width = 120, alignment = JLabel.LEFT)
		public String getHost() {
			return mHost;
		}

		@TableAttribute(header = "Port", column = 3, width = 65)
		public String getPort() {
			return String.valueOf(mPort);
		}
		@TableAttribute(header = "Created", column = 4, width = 160)
		public String getCreateTime() {
			return mCreateTime;
		}

		@TableAttribute(header = "Last Seen", column = 5, width = 100)
		public String getLastSeen() {
			return sdf.format(mLastSeen);
		}

	}
}

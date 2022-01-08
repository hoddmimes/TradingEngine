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
import com.hoddmimes.te.management.gui.table.TableCallbackInterface;
import com.hoddmimes.te.management.gui.table.TableModel;
import com.hoddmimes.te.messages.generated.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class OrderPanel extends JPanel implements TableCallbackInterface {
	ServiceInterface mServiceInterface;
	JPanel mTopPanel;
	JPanel mTablePanel;
	JPanel mButtonPanel;

	TableModel<OrderEntry> mOrderTableModel;
	Table mOrderTable;

	JComboBox<AccountEntry> mAccountComboBox;
	JButton mDeleteOrderBtn;
	JButton mDeleteAllBtn;
	OrderEntry mLatestClickedOrder = null;


	public OrderPanel(ServiceInterface pServiceInterface ) {
		this.setLayout(new BorderLayout());

		mTopPanel =  createTopPanel();
		mTablePanel = createTablePanel();
		mButtonPanel = createButtonPanel();
		mServiceInterface = pServiceInterface;

		this.add(mTopPanel, BorderLayout.NORTH);
		this.add( mTablePanel, BorderLayout.CENTER );
		this.add( mButtonPanel, BorderLayout.SOUTH );
	}




	private JPanel createTopPanel()
	{
		JPanel tRoot = new JPanel( new BorderLayout());
		tRoot.setBorder( new EmptyBorder(10,10,10,10));

		JPanel tPanel = new JPanel( new FlowLayout( FlowLayout.CENTER));
		tPanel.setBackground( Management.PANEL_BACKGROUND);
		tPanel.setBorder( new EtchedBorder(3));
		tRoot.add( tPanel, BorderLayout.CENTER);

		mAccountComboBox = new JComboBox<>();
		mAccountComboBox.setFont(Management.DEFAULT_FONT_BOLD);
		tPanel.add( mAccountComboBox );

		mAccountComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				accountChanged();
			}
		});

		return tRoot;
	}

	private JPanel createTablePanel() {
		JPanel tRootPanel = new JPanel( new BorderLayout());
		tRootPanel.setBorder( new EmptyBorder(10,5,10,5));
		JPanel tContentPanel = new JPanel( new BorderLayout());
		tContentPanel.setBorder( new EtchedBorder(2));
		tRootPanel.add( tContentPanel);

		mOrderTableModel = new TableModel(OrderEntry.class);

		mOrderTable = new Table(mOrderTableModel, new Dimension(mOrderTableModel.getPreferedWith() + 18, 280), this);
		mOrderTable.setBackground(Color.white);

		JPanel tTablePanel = new JPanel();
		tTablePanel.setLayout(new FlowLayout());
		tTablePanel.setBorder(new EmptyBorder(10, 5, 10, 5));
		tTablePanel.add(mOrderTable);

		tContentPanel.add(tTablePanel, BorderLayout.CENTER);
		return tRootPanel;
	}

	private void deleteOrder( OrderEntry pOrder ) {
		if (pOrder == null) {
			return;
		}

		int tOption = JOptionPane.showConfirmDialog( this, "Do you want to delete order\n" +
				pOrder.toString(), "Delete Order", JOptionPane.YES_NO_OPTION );

		if (tOption == JOptionPane.NO_OPTION) {
			return;
		}

		MgmtDeleteOrderResponse tResponse = (MgmtDeleteOrderResponse) mServiceInterface.transceive( TeMgmtServices.MatchingService,
															new MgmtDeleteOrderRequest().setRef("do").setOrder(pOrder.getOrder()));

		String tMsg = (tResponse.getDeleted().get()) ? "Order delete" : "Order not found";

		JOptionPane.showMessageDialog( this, tMsg, "Order Delete", JOptionPane.PLAIN_MESSAGE );
		if (tResponse.getDeleted().get()) {
			reloadOrders(); // reload orders :-)
		}
	}

	private void deleteAllOrders( String pAccount) {
		if (mOrderTableModel.getObjects().size() == 0) {
			return;
		}

		int tOption = JOptionPane.showConfirmDialog( this, "Do you want to delete all order for account " +
				pAccount, "Delete All Order", JOptionPane.YES_NO_OPTION );

		if (tOption == JOptionPane.NO_OPTION) {
			return;
		}

		MgmtDeleteAllOrdersResponse tResponse = (MgmtDeleteAllOrdersResponse) mServiceInterface.transceive( TeMgmtServices.MatchingService,
				new MgmtDeleteAllOrdersRequest().setRef("do").setAccount( pAccount ));

		String tMsg = String.valueOf( tResponse.getDeleted().get()) + " orders deleted";

		JOptionPane.showMessageDialog( this, tMsg, "Order Delete", JOptionPane.PLAIN_MESSAGE );
		if (tResponse.getDeleted().get() > 0) {
			reloadOrders(); // reload orders :-)
		}
	}


	private JPanel createButtonPanel() {
		JPanel tRootPanel = new JPanel( new FlowLayout( FlowLayout.CENTER));
		tRootPanel.setBorder( new EtchedBorder(2));
		JPanel tContentPanel = new JPanel( new FlowLayout( FlowLayout.CENTER));
		tRootPanel.add( tContentPanel );
		tContentPanel.setBorder( new EmptyBorder(5,5,5,5));
		mDeleteAllBtn = new JButton("Delete All");
		mDeleteAllBtn.setFont( Management.DEFAULT_FONT_BOLD);
		mDeleteAllBtn.setBackground( Management.BUTTON_BACKGROUND);
		tContentPanel.add( mDeleteAllBtn );
		mDeleteOrderBtn = new JButton("Delete Order");
		mDeleteOrderBtn.setFont( Management.DEFAULT_FONT_BOLD);
		mDeleteOrderBtn.setBackground( Management.BUTTON_BACKGROUND);
		tContentPanel.add( mDeleteOrderBtn );

		mDeleteAllBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				deleteAllOrders( ((AccountEntry)mAccountComboBox.getSelectedItem()).getAccountId());
			}
		});

		mDeleteOrderBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				deleteOrder( mLatestClickedOrder );
			}
		});

		tContentPanel.setBackground( Management.PANEL_BACKGROUND);
		tRootPanel.setBackground( Management.PANEL_BACKGROUND);
		return tRootPanel;
	}

	public void resizeEvent( Dimension pSize) {
		System.out.println("size: " + this.getSize());
	}

	private void accountChanged() {
		AccountEntry ae = (AccountEntry) mAccountComboBox.getSelectedItem();
		mLatestClickedOrder = null;
		loadOrders( ae.getAccountId(), true );
	}

	private void reloadOrders() {
		AccountEntry ae = (AccountEntry) mAccountComboBox.getSelectedItem();
		mLatestClickedOrder = null;
		loadOrders( ae.getAccountId(), false );
	}



	public void loadAccountData() {
		// Load market data if not already loaded
		if (mAccountComboBox.getItemCount() == 0) {
			MgmtGetAccountsResponse tAccountsResponse = (MgmtGetAccountsResponse) mServiceInterface.transceive(TeMgmtServices.Autheticator, new MgmtGetAccountsRequest().setRef("ga"));
			if (tAccountsResponse == null) {
				return;
			}
			List<Account> tAccLst = tAccountsResponse.getAccounts().get();
			Collections.sort( tAccLst, new BasePanel.AccountSort());
			for (int i = 0; i < tAccLst.size(); i++) {
					mAccountComboBox.addItem( new AccountEntry( tAccLst.get(i) ));
			}

		}
		AccountEntry tAccountEntry = (AccountEntry) mAccountComboBox.getSelectedItem();
		loadOrders(tAccountEntry.getAccountId(), false );
	}


	void loadOrders( String pAccount, boolean pNoOrderInfo ) {
		MgmtGetAccountOrdersResponse tOrdersResponse = (MgmtGetAccountOrdersResponse) mServiceInterface.transceive(TeMgmtServices.MatchingService, new MgmtGetAccountOrdersRequest().setRef("X").setAccounts(pAccount));

		if (tOrdersResponse == null) {
			return;
		}
		if ((pNoOrderInfo) && (tOrdersResponse.getOrders().get().size() == 0)) {
			JOptionPane.showMessageDialog(this,
					"No active orders found for account \"" + pAccount + "\"",
					"No Orders Found",
					JOptionPane.PLAIN_MESSAGE);
		}

		updateOrderModel( tOrdersResponse.getOrders().get());
	}

	private void updateOrderModel( List<OwnOrder> pOrders )
	{
		mOrderTableModel.clear();
		for( OwnOrder tOrder : pOrders) {
			mOrderTableModel.addEntry( new OrderEntry(tOrder));
		}
		mOrderTableModel.fireTableDataChanged();
		this.revalidate();
		this.repaint();
	}

	@Override
	public void tableMouseButton2(Object pObject, int pRow, int pCol) {

	}

	@Override
	public void tableMouseClick(Object pObject, int pRow, int pCol) {
		//System.out.println("mouse click " + pRow + " " + pCol );
		mLatestClickedOrder = (OrderEntry) pObject;
	}

	@Override
	public void tableMouseDoubleClick(Object pObject, int pRow, int pCol) {

	}


	public static class OrderEntry {
		NumberFormat nbf;
		public OwnOrder mOrder;


		public OrderEntry(OwnOrder pOrder ) {
			mOrder = pOrder;
			nbf = NumberFormat.getInstance(Locale.US);
			nbf.setMaximumFractionDigits(2);
			nbf.setMinimumFractionDigits(2);
			nbf.setGroupingUsed(false);
		}

		public String toString() {
			return "sid: " + mOrder.getSid().get() + " " + mOrder.getQuantity().get() + "@" + PrcFmt.format( mOrder.getPrice().get()) +
					" ref: " + mOrder.getRef().get() + " orderId: " + mOrder.getOrderId().get();
		}

		@TableAttribute(header = "SID", column = 1, width = 160, alignment = JLabel.LEFT)
		public String getSid() {
			return mOrder.getSid().get();
		}

		@TableAttribute(header = "Side", column = 2, width = 66, alignment = JLabel.LEFT)
		public String getSide() {
			return mOrder.getSide().get();
		}

		@TableAttribute(header = "Ref", column = 3, width = 80, alignment = JLabel.LEFT)
		public String getRef() {
			return mOrder.getRef().get();
		}

		@TableAttribute(header = "Price", column = 4, width = 50)
		public String getPrice() {
			return PrcFmt.format( mOrder.getPrice().get());
		}

		@TableAttribute(header = "Volume", column = 5, width = 70)
		public String getQuantity() {
			return String.valueOf(mOrder.getQuantity().get());
		}
		@TableAttribute(header = "OrderId", column = 6, width = 140)
		public String getOrderId() {
			return String.valueOf(mOrder.getOrderId().get());
		}

		public OwnOrder getOrder() {
			return mOrder;
		}
	}


	class AccountEntry
	{
		Account mAccount;

		AccountEntry( Account pAccount ) {
			mAccount = pAccount;
		}

		String getAccountId() {
			return mAccount.getAccount().get();
		}

		public String toString() {
			return mAccount.getAccount().get() ;
		}
	}
}
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
import com.hoddmimes.te.messages.generated.Account;
import com.hoddmimes.te.messages.generated.MgmtGetAccountsRequest;
import com.hoddmimes.te.messages.generated.MgmtGetAccountsResponse;

import java.util.Collections;
import java.util.List;
import javax.swing.*;
import java.awt.*;

public class AccountsPanel extends BasePanel
{
	private ServiceInterface mServiceInterface;


	private JTabbedPane mTabbedPane;
	private RemoveUpdateAccountPanel mRemovePanel;
	private RemoveUpdateAccountPanel mUpdatePanel;
	private List<Account> mAccountList;



	AccountsPanel( ServiceInterface pServiceInterface ) {
		super( pServiceInterface );
		this.setLayout(new BorderLayout());
		mServiceInterface = pServiceInterface;

		mTabbedPane = new JTabbedPane();

		mRemovePanel = new RemoveUpdateAccountPanel( this, RemoveUpdateAccountPanel.Action.Remove );
		mUpdatePanel = new RemoveUpdateAccountPanel( this, RemoveUpdateAccountPanel.Action.Update );

		mTabbedPane.addTab("Add Account", new AddUpdateAccountPanel( this ));
		mTabbedPane.addTab("Remove Account", mRemovePanel);
		mTabbedPane.addTab("Update Account", mUpdatePanel);
		this.add( mTabbedPane, BorderLayout.CENTER);
	}

	private JPanel mockPanel(String pText ) {
		JPanel tRootPanel = new JPanel(new BorderLayout());
		tRootPanel.add(new TextArea(pText), BorderLayout.CENTER);
		return tRootPanel;
	}

	ServiceInterface getServiceInterface() {
		return mServiceInterface;
	}



	void loadAccountData() {
		MgmtGetAccountsResponse tAccountResponse = (MgmtGetAccountsResponse) mServiceInterface.transceive(TeService.Autheticator.name(), new MgmtGetAccountsRequest().setRef("ga"));
		if (tAccountResponse != null) {
			mAccountList = tAccountResponse.getAccounts().get();
			Collections.sort( mAccountList, new AccountSort());
			mUpdatePanel.initData( mAccountList );
			mRemovePanel.initData( mAccountList );

		}
	}

	boolean doesAccountExist( Account pAccount ) {
		int t = Collections.binarySearch( mAccountList, pAccount, new AccountSort() );
		return (t < 0) ? false : true;
	}

	void setTabbedPane( Component pComponent ) {
		mTabbedPane.setSelectedComponent( pComponent );
		this.revalidate();
		this.repaint();
	}




}

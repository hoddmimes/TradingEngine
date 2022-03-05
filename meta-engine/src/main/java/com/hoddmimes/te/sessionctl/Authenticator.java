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

package com.hoddmimes.te.sessionctl;

import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.db.TEDB;
import com.hoddmimes.te.common.interfaces.AuthenticateInterface;
import com.hoddmimes.te.common.interfaces.TeService;
import com.hoddmimes.te.common.ipc.IpcComponentInterface;
import com.hoddmimes.te.common.ipc.IpcRequestCallbackInterface;
import com.hoddmimes.te.messages.generated.*;
import com.mongodb.client.result.UpdateResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class Authenticator implements AuthenticateInterface, IpcRequestCallbackInterface
{
	private Logger mLog = LogManager.getLogger( Authenticator.class );
	private TEDB mDb;

	public Authenticator() {
		mDb = TeAppCntx.getDatabase();
		IpcComponentInterface tMgmt = TeAppCntx.getInstance().getIpcService().registerComponent( TeService.Autheticator, 0, this );
	}

	@Override
	public Account logon(String pAccount, String pPassword) {
		if ((pAccount == null) || (pPassword == null)) {
			mLog.info("Invalid login data, user or password must no be null");
			return null;
		}

		AccountX a = getAccount(pAccount);
		if (a == null) {
			mLog.info("Account \"" + pAccount + "\" not found");
			return null;
		}
		if (!a.validatePassword( pAccount.toUpperCase() + pPassword )) {
			mLog.info("Account \"" + pAccount + "\" invalid password");
			return null;
		}
		mLog.info("Account \"" + pAccount + "\" successfully authenticated");
		return a;
	}

	private AccountX getAccount( String pAccountId ) {
		List<Account> tAccounts = mDb.findAccount(pAccountId);
		if ((tAccounts == null) || (tAccounts.size() != 1)) {
			return null;
		}
		return new AccountX( tAccounts.get(0));
	}


	private void saveAccount( Account tAccount) {
		mDb.updateAccount( tAccount, true);
	}

	private MgmtGetAccountsResponse mgmtCmdGetAccounts(MgmtGetAccountsRequest pRqst) {
		MgmtGetAccountsResponse tRsp = new MgmtGetAccountsResponse().setRef(pRqst.getRef().get());
		List<Account> tAccLst = mDb.findAllAccount();
		tRsp.addAccounts(tAccLst);
		return tRsp;
	}

	private MgmtUpdateAccountResponse mgmtCmdUpdateAccount(MgmtUpdateAccountRequest pRqst ) {
		MgmtUpdateAccountResponse tRsp = new MgmtUpdateAccountResponse().setRef(pRqst.getRef().get());

		try {
			Account tAccount = mDb.findAccount(pRqst.getAccountId().get()).get(0);
			if (pRqst.getSuspended().isPresent() || (!pRqst.getSuspended().isEmpty())) {
				tAccount.setSuspended(pRqst.getSuspended().get());
			}
			if (pRqst.getConfirmed().isPresent() || (!pRqst.getConfirmed().isEmpty())) {
				tAccount.setConfirmed(pRqst.getConfirmed().get());
			}
			if (pRqst.getMailAddress().isPresent() || (!pRqst.getMailAddress().isEmpty())) {
				tAccount.setMailAddr(pRqst.getMailAddress().get());
			}
			if (pRqst.getHashedPassword().isPresent() || (!pRqst.getHashedPassword().isEmpty())) {
				tAccount.setPassword(pRqst.getHashedPassword().get());
			}
			mDb.updateAccount( tAccount, false );
			tRsp.setIsUpdated(true);
			tRsp.setStatusText("success");
			tRsp.setAccount( tAccount );
		} catch (Throwable e) {
			tRsp.setIsUpdated(false);
			tRsp.setStatusText("failed to update DB, reason: " + e.getMessage());
			mLog.error("failed to update account in DB", e);
		}
		return tRsp;
	}


	MgmtDeleteAccountResponse mgmtCmdDeleteAccounts(MgmtDeleteAccountRequest pRqst) {
		MgmtDeleteAccountResponse tDelRsp = new MgmtDeleteAccountResponse().setRef(pRqst.getRef().get());
		if (mDb.deleteAccountByAccountId(pRqst.getAccount().get().getAccountId().get()) > 0) {
			tDelRsp.setIsDeleted(true);
		} else {
			tDelRsp.setIsDeleted(false);
		}
		return tDelRsp;
	}

	MgmtAddAccountResponse mgmtCmdAddAccounts(MgmtAddAccountRequest pRqst) {

		AccountX tAccount = getAccount(pRqst.getAccount().get().getAccountId().get());

		if (tAccount != null) {
			return new MgmtAddAccountResponse().setRef(pRqst.getRef().get()).setIsAddded(false).setStatusMessage("account already exists");
		}
		UpdateResult tUpdResult = mDb.updateAccount(pRqst.getAccount().get(), true);
		if (tUpdResult.getUpsertedId() != null) {
			return new MgmtAddAccountResponse().setRef(pRqst.getRef().get()).setIsAddded(true).setAccount(tAccount).setStatusMessage("successfully added");
		}

		mLog.error("failed to add new account to DB");
		return new MgmtAddAccountResponse().setRef(pRqst.getRef().get()).setIsAddded(false).setStatusMessage("Hmmmmmmmm");

	}


	@Override
	public MessageInterface ipcRequest(MessageInterface pMgmtRequest) {
		if (pMgmtRequest instanceof MgmtGetAccountsRequest) {
			return mgmtCmdGetAccounts( (MgmtGetAccountsRequest) pMgmtRequest );
		}
		if (pMgmtRequest instanceof MgmtUpdateAccountRequest) {
			return mgmtCmdUpdateAccount( (MgmtUpdateAccountRequest) pMgmtRequest );
		}
		if (pMgmtRequest instanceof  MgmtAddAccountRequest) {
			return mgmtCmdAddAccounts( (MgmtAddAccountRequest) pMgmtRequest );
		}
		if (pMgmtRequest instanceof  MgmtDeleteAccountRequest) {
			return mgmtCmdDeleteAccounts( (MgmtDeleteAccountRequest) pMgmtRequest );
		}


		throw new RuntimeException("No commnad entry found for : " + pMgmtRequest.getMessageName());
	}
}

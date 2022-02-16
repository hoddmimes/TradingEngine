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

import com.hoddmimes.te.common.db.TEDB;
import com.hoddmimes.te.messages.generated.Account;
import com.hoddmimes.te.sessionctl.AccountX;
import com.mongodb.client.result.UpdateResult;

public class PreCreateAccounts
{
	private static final String DB_HOST  = "localhost";
	private static final int DB_PORT = 27017;
	private static final String DB_NAME = "TradingEngine";



	TEDB mDb ;
	public static void main(String[] args) {
		PreCreateAccounts pca = new PreCreateAccounts();
		pca.create();
	}

	private void create() {

		mDb = new TEDB( DB_NAME, DB_HOST, DB_PORT);
		mDb.connectToDatabase();
		addAccount("JOSHUA","test@foobar.com","test", true, false );

		addAccount("FROTZ","test@foobar.com","test", true, false );
		addAccount("ELVIS","test@foobar.com","test", true, false );
		addAccount("DONALD","test@foobar.com","test", true, false );

		addAccount("HANNES","test@foobar.com","test", true, false );
		addAccount("JOBS","test@foobar.com","test", true, false );
		addAccount("TEST","test@foobar.com","test", true, false );
		addAccount("FRANKLIN","test@foobar.com","test", true, false );
		addAccount("GRACE","test@foobar.com","test", true, false );
		addAccount("SNOPPY","test@foobar.com","test", true, false );
		addAccount("ARTHUR","test@foobar.com","test", true, false );
		addAccount("WINSTON","test@foobar.com","test", true, false );
		addAccount("VINICI","test@foobar.com","test", true, false );
		addAccount("USER01","test@foobar.com","test", true, false );
		addAccount("USER02","test@foobar.com","test", true, false );
		addAccount("USER03","test@foobar.com","test", true, false );
		addAccount("USER04","test@foobar.com","test", true, false );
		addAccount("USER05","test@foobar.com","test", true, false );
		addAccount("USER06","test@foobar.com","test", true, false );
		addAccount("USER07","test@foobar.com","test", true, false );
		addAccount("USER08","test@foobar.com","test", true, false );
		addAccount("USER09","test@foobar.com","test", true, false );
		addAccount("USER10","test@foobar.com","test", true, false );


	}

	private void addAccount( String pAccountId, String pMailAddr, String pPassword, boolean pConfirmed, boolean pSuspended ) {
		Account tAccount = new Account().setAccountId( pAccountId).setMailAddr( pMailAddr).setConfirmed( pConfirmed).setSuspended( pSuspended);
		tAccount.setPassword( AccountX.hashPassword(pAccountId.toUpperCase() + pPassword));
		UpdateResult tUpdResult = mDb.updateAccount( tAccount, true );
		if (tUpdResult.getUpsertedId() == null) {
			System.out.println("Warning account " + pAccountId + " was updated " + tUpdResult.toString());
		}
	}
}

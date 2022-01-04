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

import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.messages.generated.MgmtActiveSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SessionCntxMapper
{
	private ConcurrentHashMap<String, SessionCntxInterface>        mIdSessions;
	private ConcurrentHashMap<String, List<SessionCntxInterface>>  mAccountSessions;
	private AtomicInteger mMaxSessions;

	public SessionCntxMapper() {
		mMaxSessions = new AtomicInteger(0);
		mIdSessions = new ConcurrentHashMap<>();
		mAccountSessions = new ConcurrentHashMap<>();
	}


	public synchronized void add(SessionCntxInterface pSessCntx ) {
		mIdSessions.put(pSessCntx.getSessionId(), pSessCntx);
		if (mIdSessions.size() > mMaxSessions.get()) {
			mMaxSessions.set(mIdSessions.size());
		}
		List<SessionCntxInterface> tAccSessionList = mAccountSessions.get( pSessCntx.getAccount() );
		if (tAccSessionList == null) {
			tAccSessionList = new LinkedList<>();
			mAccountSessions.put(pSessCntx.getAccount(), tAccSessionList );
		}
		tAccSessionList.add( pSessCntx );
	}

	public synchronized SessionCntxInterface getById( String pSesssionId ) {
		return mIdSessions.get( pSesssionId );
	}

	public synchronized List<SessionCntxInterface> getByAccount( String pAccountId ) {
		return mAccountSessions.get( pAccountId );
	}

	public synchronized SessionCntxInterface removeById( String pSessionId ) {
		SessionCntxInterface tSessionCntx = mIdSessions.remove(pSessionId);
		if (tSessionCntx == null) {
			return null;
		}
		List<SessionCntxInterface> tAccSessionList = mAccountSessions.get(tSessionCntx.getAccount());
		ListIterator<SessionCntxInterface> tItr = tAccSessionList.listIterator();
		while (tItr.hasNext()) {
			if (tItr.next().getSessionId().contentEquals(pSessionId)) {
				tItr.remove();
				break;
			}
		}
		if (tAccSessionList.size() == 0) {
			mAccountSessions.remove(tSessionCntx.getAccount());
		}
		return tSessionCntx;
	}

	public synchronized List<MgmtActiveSession> getActiveSessions() {
		List<MgmtActiveSession> tSessions = new ArrayList<>();
		Iterator<SessionCntxInterface> tItr = mIdSessions.values().iterator();
		while(tItr.hasNext()) {
			SessionCntxInterface tSess = tItr.next();
			MgmtActiveSession tActiveSess = new MgmtActiveSession();
			tActiveSess.setSessionId( tSess.getSessionId());
			tActiveSess.setAccount( tSess.getAccount());
			tActiveSess.setCreationTime( tSess.getSessionStartTimeBin());
			tSessions.add( tActiveSess );
			return tSessions;
		}
		return tSessions;
	}

	public int getMaxSessions() {
		return mMaxSessions.get();
	}

	public synchronized boolean removeByAccount( String pAccountId ) {
		List<SessionCntxInterface> tAccSessionList = mAccountSessions.remove(pAccountId);
		if (tAccSessionList == null) {
			return false;
		}
		for( SessionCntxInterface sc : tAccSessionList) {
			mIdSessions.remove( sc.getSessionId());
		}
		return true;
	}

	public synchronized  boolean validateApiAuthId( String pAuthId ) {
		Iterator<SessionCntxInterface> tItr = mIdSessions.values().iterator();
		while( tItr.hasNext()) {
			SessionCntxInterface sc = tItr.next();
			if ((sc.getApiAuthId() != null) && ( sc.getApiAuthId().contentEquals( pAuthId))) {
				return true;
			}
		}
		return false;
	}

	public synchronized SessionCntxInterface getSessionCntxByAuthId( String pApiAuthId ) {
		Iterator<SessionCntxInterface> tItr = mIdSessions.values().iterator();
		while( tItr.hasNext()) {
			SessionCntxInterface sc = tItr.next();
			if (pApiAuthId.contentEquals(sc.getApiAuthId())) {
				return sc;
			}
		}
		return null;
	}
}

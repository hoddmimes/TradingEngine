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

import com.fasterxml.jackson.databind.deser.DataFormatReaders;
import com.hoddmimes.te.messages.generated.MgmtGetLogMessagesRequest;
import com.hoddmimes.te.messages.generated.MsgLogEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MgmtMsgLogFilter
{
	Logger mLog = LogManager.getLogger( MgmtMsgLogFilter.class );
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	private static Pattern cLogMsgPattern = Pattern.compile("^((\\d{4}-\\d{2}-\\d{2})\\s(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})).+account:\\s([^\\s]+)\\s*(.+)");
	String mTimeStartString = null;
	String mAccountStr = null;
	Pattern mSearchRegexp = null;

	MgmtMsgLogFilter(MgmtGetLogMessagesRequest pRqst ) {
		if (!pRqst.getAccountFilter().isEmpty()) {
			mAccountStr = pRqst.getAccountFilter().get();
		}
		if (!pRqst.getMsgFilter().isEmpty()) {
			mSearchRegexp = Pattern.compile(pRqst.getMsgFilter().get());
		}
		if (!pRqst.getTimeFilter().isEmpty()) {
			if (!pRqst.getTimeFilter().get().contentEquals("00:00:00")) {
				mTimeStartString = timeStringStart(pRqst.getTimeFilter().get());
			}
		}
	}
	//2021-12-13 07:27:45.594 [RQST       ] sid: 508CD509C441E8A0BE45214FA8EBDCC6 account: FRANKLIN     {"AddOrderRequest":{"sid":"2:ETH","price":4011.4,"quantity":54,"ref":"9c57dbf","side":"SELL"}}
	//2021-12-13 07:27:45.595 [RESP (0323)] sid: 508CD509C441E8A0BE45214FA8EBDCC6 account: FRANKLIN     {"AddOrderResponse":{"ref":"9c57dbf","orderId":"61b6e7e1000026","inserted":true,"matched":0}}

	MsgLogEntry match(String pLogLine ) {
		long tTimeStamp = 0;
		Matcher m = cLogMsgPattern.matcher( pLogLine );
	    if (m.find()) {
			if ((mAccountStr != null) && (!mAccountStr.contentEquals( m.group(4)))) {
				return null;
			}
		    if ((mTimeStartString != null) && (!m.group(3).startsWith( mTimeStartString))) {
			    return null;
		    }
			if ((mSearchRegexp != null) && (!mSearchRegexp.matcher( m.group(5)).find())) {
				return null;
			}
			MsgLogEntry tLogEntry = new MsgLogEntry();
	        tLogEntry.setLogMsg( m.group(5));
			tLogEntry.setAccount( m.group(4));
			try {tTimeStamp = sdf.parse(m.group(1)).getTime();}
			catch(ParseException pe) {
				mLog.fatal("failed to parse msg log time (" + m.group(1) + ") ", pe);
			}
			tLogEntry.setTimeStamp( tTimeStamp);
			return tLogEntry;
		}
		return null;
	}

	private String timeStringStart( String pTimeString) {
		int tPos = pTimeString.length() - 1;
		byte tBytes[] = pTimeString.getBytes(StandardCharsets.UTF_8);
		for (int i = (tBytes.length - 1); i >= 0  ; i--) {
			if ((tBytes[i] != '0') && (tBytes[i] != ':')) {
				tPos = i;
				break;
			}
		}
		return pTimeString.substring(0,tPos+1);
	}
}

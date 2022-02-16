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

package com.hoddmimes.te.common;

import com.hoddmimes.te.messages.generated.StatusMessage;

public class TeException extends Exception
{
	StatusMessage mStatusMessage;
	int mStatusCode;
	Exception mException;

	public TeException(int pStatusCode, StatusMessage pStsMsg, Exception pOrginException)
	{
		super(pStsMsg.getStatusMessage().get());
		mStatusMessage = pStsMsg;
		mStatusCode = pStatusCode;
		mException = pOrginException;
	}

	public TeException(int pStatusCode, StatusMessage pStsMsg) {
		this( pStatusCode, pStsMsg, null );
	}

	public TeException(StatusMessage pStsMsg) {
		this( 0, pStsMsg, null );
	}

	public Exception getOrginException() {
		return mException;
	}
	public int getStatusCode() {
		return mStatusCode;
	}

	public StatusMessage getStatusMessage() {
		return mStatusMessage;
	}
}

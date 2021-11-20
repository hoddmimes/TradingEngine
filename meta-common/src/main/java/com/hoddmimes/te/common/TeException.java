package com.hoddmimes.te.common;

import com.hoddmimes.te.messages.generated.StatusMessage;

public class TeException extends Exception
{
	StatusMessage mStatusMessage;
	int mStatusCode;

	public TeException(int pStatusCode, StatusMessage pStsMsg)
	{
		super(pStsMsg.getStatusMessage().get());
		mStatusMessage = pStsMsg;
		mStatusCode = pStatusCode;
	}

	public int getStatusCode() {
		return mStatusCode;
	}

	public StatusMessage getStatusMessage() {
		return mStatusMessage;
	}
}

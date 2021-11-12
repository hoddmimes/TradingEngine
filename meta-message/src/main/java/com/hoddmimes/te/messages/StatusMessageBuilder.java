package com.hoddmimes.te.messages;

import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.messages.generated.StatusMessage;

public class StatusMessageBuilder
{
	public static StatusMessage success(String pMessage, String pUserRef) {
		StatusMessage tMsg = new StatusMessage();
		tMsg.setIsOk( true );
		tMsg.setRef( pUserRef );
		if (pMessage == null) {
			tMsg.setStatusMessage("Successfully completed");
		} else {
			tMsg.setStatusMessage( pMessage );
		}
		return tMsg;
	}

	public static StatusMessage success( String pUserRef) {
		return StatusMessageBuilder.success(null, pUserRef );
	}

	public static StatusMessage error( String pErrorMessage, String pUserRef, Throwable pThrowable ) {
		StatusMessage tMsg = new StatusMessage();
		tMsg.setIsOk( false );
		tMsg.setRef( pUserRef );
		tMsg.setStatusMessage( pErrorMessage );
		if (pThrowable != null) {
			tMsg.setExceptionMessage( pThrowable.getMessage());
		}
		return tMsg;
	}

	public static StatusMessage error( String pErrorMessage, String pUserRef ) {
		return StatusMessageBuilder.error(pErrorMessage, pUserRef,null);
	}
}

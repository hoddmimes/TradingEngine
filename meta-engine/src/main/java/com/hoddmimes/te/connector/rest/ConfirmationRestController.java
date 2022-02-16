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

package com.hoddmimes.te.connector.rest;


import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.db.TEDB;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.DbConfirmation;
import com.hoddmimes.te.messages.generated.StatusMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.List;


@RestController
@Configuration
@RequestMapping("/te-confirmation")
public class ConfirmationRestController {
	public static Logger cLog = LogManager.getLogger( ConfirmationRestController.class );

	@GetMapping(path = "confirm/{confirmationId}")
	ResponseEntity<String> addPaymentEntry(HttpSession pSession, @PathVariable String confirmationId)
	{

			TEDB mDb = TeAppCntx.getInstance().getDb();
			List<DbConfirmation> tConfirmations = mDb.findDbConfirmationByConfirmationId( confirmationId );
			if (tConfirmations.size() != 1) {
				cLog.warn("confirmation object not found (id: " + confirmationId + ")");
				buildResponse(StatusMessageBuilder.error("could not found anything to confirm", null));
			}

			DbConfirmation tConfirmation = tConfirmations.get(0);

			if (tConfirmation.getConfirmationType().get().contentEquals( TEDB.ConfirmationType.ACCOUNT.name())) {
				mDb.confirmAccount(tConfirmation.getAccount().get());
				cLog.info("account: " + tConfirmation.getAccount().get() + " confirmed id: " + confirmationId);
			}
			if (tConfirmation.getConfirmationType().get().contentEquals(TEDB.ConfirmationType.PAYMENT.name())) {
				boolean tDone = mDb.confirmPaymentEntry(tConfirmation.getAccount().get(), confirmationId);
				cLog.info("payment : " + tConfirmation.getAccount().get() + " confirmed id: " + confirmationId + " done: " + String.valueOf( tDone ));
			}

			mDb.deleteDbConfirmationByConfirmationId( confirmationId );

			return buildResponse(StatusMessageBuilder.error(tConfirmation.getConfirmationType().get() + " confirmed  (id: " + confirmationId + ")", null));

	}



	private ResponseEntity<String> buildStatusMessageResponse( StatusMessage pStsMsg ) {
		if (pStsMsg.getIsOk().get()) {
			return new ResponseEntity<>( AuxJson.getMessageBody(pStsMsg.toJson()).toString(), HttpStatus.OK );
		} else {
			return new ResponseEntity<>( AuxJson.getMessageBody(pStsMsg.toJson()).toString(), HttpStatus.BAD_REQUEST );
		}
	}

	private ResponseEntity<String> buildResponse(MessageInterface pResponseMessage ) {
		if (pResponseMessage instanceof StatusMessage) {
			return buildStatusMessageResponse((StatusMessage) pResponseMessage);
		}
		return new ResponseEntity<>( AuxJson.getMessageBody(pResponseMessage.toJson()).toString(), HttpStatus.OK );
	}

	private ResponseEntity<String> buildResponse(JsonObject pResponseMessage ) {
		return new ResponseEntity<>( pResponseMessage.toString(), HttpStatus.OK );
	}
}

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

package com.hoddmimes.te.connector.rest;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.sessionctl.SessionController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.IOUtils;
import org.springframework.http.server.ServletServerHttpRequest;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;



public class TeFilter implements Filter
{
	private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
	static final String TE_SESS_CNTX = "TE_SESS_CNTX";
	private static final String LOGIN_URL = "/te-trading/logon";
	private static final String CONFIRM_URL = "/te-confimation/";
	private static final String MARKETDATA_URL = "/te-marketdata";

	private Logger mLog;

	public TeFilter() {
		mLog = LogManager.getLogger(TeFilter.class);
	}

	private boolean validateSession( HttpSession pSession ) {
		SessionController tSessionctl = TeAppCntx.getInstance().getSessionController();
		SessionCntxInterface tSessCntx = tSessionctl.getSessionContext(pSession.getId());

		if (tSessCntx == null) {
			mLog.warn("No session context found for session: " + pSession.getId());
			return false;
		}

		SessionCntxInterface tHdrSessCntx = (SessionCntxInterface) pSession.getAttribute(TE_SESS_CNTX);
		if (tHdrSessCntx == null) {
			mLog.warn("No request session context found for session: " + pSession.getId());
			return false;
		}


		if (!tHdrSessCntx.getSessionId().contentEquals(tSessCntx.getSessionId())) {
			mLog.warn("request ression context id <> Sessctl session id for request session: " + pSession.getId());
			return false;
		}

		if (tHdrSessCntx.getSessionStartTimeBin() != tSessCntx.getSessionStartTimeBin()) {
			mLog.warn("request session start time  <> Sessctl session start time for request session: " + pSession.getId());
			return false;
		}

		return true;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest tHttpRqst = (HttpServletRequest) request;
		HttpServletResponse tHttpResp = (HttpServletResponse) response;
		HttpSession tSession = tHttpRqst.getSession();

		if (mLog.isTraceEnabled()) {
			trace( tHttpRqst );
		}

		/**
		 * A bit clumsi and rough but I never bonded with the SessionManagementFilter framework
		 * A likely this should be done in another way  but we have a dead simple service
		 * that do not need a lot of filtering. There will be three URI prefix
		 * "/te-trading/..." must be validated with the exception of "/te-trading/logon"
		 * "/te-marketdata" destination is the WSS just let it through, validation will take place WebSocketHandshakeInterceptor
		 * "/te-confirmation/..." should not be validate
		 */
		 String tRqstURI = tHttpRqst.getRequestURI();
		 boolean tValidateRequest = true;

		 if (tRqstURI.contentEquals( LOGIN_URL )) {
			 tValidateRequest = false;
		 }
		if (tRqstURI.startsWith( CONFIRM_URL )) {
			tValidateRequest = false;
		}

		if (tRqstURI.startsWith( MARKETDATA_URL )) {
			tValidateRequest = false;
		}


		if ((tValidateRequest) && (!validateSession( tSession ))) {
			generateAndSendValidationError( tHttpRqst, tHttpResp);
			return;
		}

		chain.doFilter(request, response);
	}


	private void generateAndSendValidationError(HttpServletRequest pRequest, HttpServletResponse pResponse ) throws IOException{

		// Carv out user referense from the request
		String tRqstBody = IOUtils.toString( pRequest.getReader());
		String tUserRef = AuxJson.getMessageRef( tRqstBody );

		MessageInterface tStsMsg = StatusMessageBuilder.error("Session is not authorized", tUserRef);
		String tErrorMessage = AuxJson.getMessageBody( tStsMsg.toJson() ).toString();

		pResponse.setStatus(401); // Unauthorized
		pResponse.getOutputStream().write( tErrorMessage.getBytes(StandardCharsets.UTF_8));
		pResponse.getOutputStream().flush();
	}


	private void trace( HttpServletRequest pRqst) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		HttpSession tSession = pRqst.getSession();
		SessionCntxInterface tSessCntx = (SessionCntxInterface) tSession.getAttribute( TE_SESS_CNTX );
		String tUsername = (tSessCntx != null) ? tSessCntx.getAccount() : "null";

		/*
		String tRqstData = null;

		try {
			tRqstData = IOUtils.toString( new InputStreamReader(pRqst.getInputStream()));
		} catch (IOException pE) {
			pE.printStackTrace();
		}

		 */


		mLog.trace("url: " + pRqst.getRequestURI() + " (" + pRqst.getMethod() + ") authorized user: " + tUsername +
				" session cretim: " + sdf.format( tSession.getCreationTime()) +
				" session id: " + tSession.getId());
				//"\n request-data: " + tRqstData );
	}
}

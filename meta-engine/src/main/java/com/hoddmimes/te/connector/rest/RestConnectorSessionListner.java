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

import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpSessionEvent;


@Component
public class RestConnectorSessionListner implements javax.servlet.http.HttpSessionListener, ApplicationContextAware
{
	private Logger mLog = LogManager.getLogger( RestConnectorSessionListner.class );

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			if (applicationContext instanceof WebApplicationContext) {
				((WebApplicationContext) applicationContext).getServletContext().addListener(this);
			} else {
				//Either throw an exception or fail gracefully, up to you
				throw new RuntimeException("Must be inside a web application context");
			}
		}


	public void sessionCreated(HttpSessionEvent se)
	{
	}

	public  void sessionDestroyed(HttpSessionEvent se)
	{
		SessionCntxInterface  tSessCntx = TeAppCntx.getInstance().getSessionController().connectorDisconnectSession(se.getSession().getId());
		if (tSessCntx != null) {
			mLog.info("Session terminated for user: " + tSessCntx.getAccount() + " session id: " + tSessCntx.getSessionId() + " create time: " + tSessCntx.getSessionStartTime());
		}
	}
}


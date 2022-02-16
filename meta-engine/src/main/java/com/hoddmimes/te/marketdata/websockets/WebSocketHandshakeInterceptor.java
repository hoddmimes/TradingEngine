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

package com.hoddmimes.te.marketdata.websockets;




import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class WebSocketHandshakeInterceptor extends HttpSessionHandshakeInterceptor
{
    private Pattern cAuthIdPattern = Pattern.compile("authid=([^&$]+)");
    Logger mLog = LogManager.getLogger(WebSocketHandshakeInterceptor.class);

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                        WebSocketHandler wsHandler, @Nullable Exception exception) {


    }

    @Override
    public boolean beforeHandshake( ServerHttpRequest pRequest,
                                    ServerHttpResponse pResponse,
                                    WebSocketHandler    pWsHandler,
                                    Map<String, Object> pAttributes) throws Exception
    {
        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) pRequest;
        String tQueryString = servletRequest.getServletRequest().getQueryString();
        String tAuthId = getAuthId( tQueryString );
        if (tAuthId == null) {
            pResponse.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        pAttributes.put("authid", tAuthId);
        boolean tRetStatus = super.beforeHandshake( pRequest, pResponse, pWsHandler, pAttributes );
        return tRetStatus;
    }


    private String getAuthId( String pQueryString ) {
        if (pQueryString == null) {
            mLog.warn("web socket connect query string is null, auth is parameter is missing");
            return null;
        }
       Matcher m = cAuthIdPattern.matcher( pQueryString );
        if (!m.find()) {
            mLog.warn("web socket connect query string auth parameter is missing");
            return null;
        }
        String tAuthId = m.group(1);
        if (!TeAppCntx.getInstance().getSessionController().validateAuthId( tAuthId )) {
            mLog.warn("No http session context found for auth id");
          return null;
        }
        return tAuthId;
    }
}

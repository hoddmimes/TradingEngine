package com.hoddmimes.resttest.server;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;


public class WebSocketHandshakeInterceptor extends HttpSessionHandshakeInterceptor
{
    @Override
    public boolean beforeHandshake( ServerHttpRequest   pRequest,
                                    ServerHttpResponse  pResponse,
                                    WebSocketHandler    pWsHandler,
                                    Map<String, Object> pAttributes) throws Exception
    {
        boolean tRetStatus = super.beforeHandshake( pRequest, pResponse, pWsHandler, pAttributes );
        System.out.println("WebSocketHandshakeInterceptor");
        return tRetStatus;
    }

}

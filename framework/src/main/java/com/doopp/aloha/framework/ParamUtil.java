package com.doopp.aloha.framework;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.HashMap;
import java.util.Map;

class ParamUtil {

    static Map<String, Object> cookieMap(String cookieHeader) {
        // init request cookies
        Map<String, Object> httpCookieMap = new HashMap<>();
        if (cookieHeader!=null) {
            String[] allCookie = cookieHeader.split(";");
            for(String cookie : allCookie) {
                int iof = cookie.indexOf("=");
                if (iof<1) {
                    continue;
                }
                httpCookieMap.put(cookie.substring(0, iof).trim(), cookie.substring(iof+1).trim());
            }
        }
        return httpCookieMap;
    }

    static Map<String, Object> queryParamMap(String uri) {
        // init
        Map<String, Object> queryParamMap = new HashMap<>();
        return queryParamMap;
    }

    static Map<String, Object> pathParamMap(String uri) {
        // init
        Map<String, Object> pathParamMap = new HashMap<>();
        return pathParamMap;
    }

    static Map<String, Object> formParamMap(ByteBuf requestContent) {
        // init
        Map<String, Object> formParamMap = new HashMap<>();
        return formParamMap;
    }
}

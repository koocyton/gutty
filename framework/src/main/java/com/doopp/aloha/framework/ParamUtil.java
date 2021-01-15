package com.doopp.aloha.framework;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.util.ArrayList;
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
        if (requestContent != null && getRequestContentType(request).equals("")) {
            // Request headers
            // HttpHeaders requestHttpHeaders = request.requestHeaders();
            // POST Params
            FullHttpRequest dhr = new DefaultFullHttpRequest(request.version(), request.method(), request.uri(), content);
            dhr.headers().set(request.requestHeaders());
            // set Request Decoder
            HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), dhr, CharsetUtil.UTF_8);
            // loop data
            for (InterfaceHttpData data : postDecoder.getBodyHttpDatas()) {
                String name = data.getName();
                if (name!=null && data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                    formParams.computeIfAbsent(name, k -> new ArrayList<>())
                            .add(((MemoryAttribute) data).getValue());
                }
                // 上传文件的内容
                else if (name!=null && data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                    fileParams.computeIfAbsent(name, k -> new ArrayList<>())
                            .add(((MemoryFileUpload) data).retain());
                }
            }
            postDecoder.destroy();
            dhr.release();
            // content.release();
        }
        return formParamMap;
    }

    static Map<String, File> fileParamMap(ByteBuf requestContent) {
        // init
        Map<String, File> formParamMap = new HashMap<>();
        return formParamMap;
    }
}

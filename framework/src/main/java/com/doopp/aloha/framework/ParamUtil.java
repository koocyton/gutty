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

    static Map<String, Object> formParamMap(ByteBuf requestContent, FullHttpRequest httpRequest) {
        // init
        Map<String, Object> formParamMap = new HashMap<>();
        if (requestContent != null) {
            // set Request Decoder
            HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), httpRequest, CharsetUtil.UTF_8);
            // loop data
            for (InterfaceHttpData data : postDecoder.getBodyHttpDatas()) {
                String name = data.getName();
                // 表单
                if (name!=null && data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                    formParamMap.put(name, ((MemoryAttribute) data).getValue());
                }
                // 上传文件的内容
                else if (name!=null && data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                    formParamMap.put(name, ((MemoryFileUpload) data).retain());
                }
            }
        }
        return formParamMap;
    }

    static Map<String, File> fileParamMap(ByteBuf requestContent) {
        // init
        Map<String, File> formParamMap = new HashMap<>();
        return formParamMap;
    }
}

package com.doopp.aloha.framework;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ParamUtil {

    private static final Logger logger = LoggerFactory.getLogger(ParamUtil.class);

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
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
        // init
        Map<String, Object> queryParamMap = new HashMap<>();
        for (Map.Entry<String, List<String>> p : queryStringDecoder.parameters().entrySet()) {
            String key = p.getKey().trim();
            List<String> vals = p.getValue();
            if (vals.size() > 0) {
                String value = vals.get(0);
                queryParamMap.put(key, value);
            }
        }
        return queryParamMap;
    }

    static Map<String, Object> pathParamMap(String uri) {
        // init
        Map<String, Object> pathParamMap = new HashMap<>();
        return pathParamMap;
    }

    static Map<String, byte[]> formParamMap(ByteBuf requestContent, FullHttpRequest httpRequest) {
        // init
        Map<String, byte[]> formParamMap = new HashMap<>();
        if (requestContent != null) {
            // set Request Decoder
            HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), httpRequest, CharsetUtil.UTF_8);
            // loop data
            for (InterfaceHttpData data : postDecoder.getBodyHttpDatas()) {
                String name = data.getName();
                // 表单
                if (name!=null && data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                    formParamMap.put(name, ((MemoryAttribute) data).get());
                }
            }
        }
        return formParamMap;
    }

    static Map<String, File> fileParamMap(ByteBuf requestContent, FullHttpRequest httpRequest) {
        // init
        Map<String, File> fileParamMap = new HashMap<>();
        if (requestContent != null) {
            // set Request Decoder
            HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), httpRequest, CharsetUtil.UTF_8);
            // loop data
            for (InterfaceHttpData data : postDecoder.getBodyHttpDatas()) {
                String name = data.getName();
                // 上传文件的内容
                if (name!=null && data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                    try {
                        fileParamMap.put(name, ((MemoryFileUpload) data).retain().getFile());
                    }
                    catch (IOException ignore) {
                    }
                }
            }
        }
        return fileParamMap;
    }
}

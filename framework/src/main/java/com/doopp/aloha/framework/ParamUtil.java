package com.doopp.aloha.framework;

import com.doopp.aloha.framework.annotation.FileParam;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ParamUtil {

    private static final Logger logger = LoggerFactory.getLogger(ParamUtil.class);

    private ParamUtil() {
    }

    private static ParamUtil paramUtil = null;

    public static ParamUtil singleBuilder() {
        if (paramUtil==null) {
            paramUtil = new ParamUtil();
        }
        return paramUtil;
    }

    public static Object[] buildParams(Parameter[] parameters, FullHttpRequest httpRequest, FullHttpResponse httpResponse) {
        return ParamUtil.singleBuilder().getParams(parameters, httpRequest, httpResponse);
    }

    // http headers
    private final Map<String, String> headerParams = new HashMap<>();
    // cookies
    private final Map<String, String[]> cookieParams = new HashMap<>();
    // path params
    private final Map<String, String[]> pathParams = new HashMap<>();
    // query params
    private final Map<String, String[]> queryParams = new HashMap<>();
    // form params
    private final Map<String, byte[]> formParams = new HashMap<>();
    // file params
    private final Map<String, File[]> fileParams = new HashMap<>();

    private FullHttpRequest httpRequest;
    private FullHttpResponse httpResponse;

    private Object[] getParams(Parameter[] parameters, FullHttpRequest httpRequest, FullHttpResponse httpResponse) {
        // build data
        if (httpRequest==null) {
            return null;
        }

        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
        this.buildHeaderParams();
        this.buildCookieParams();
        this.buildPathParams();
        this.buildQueryParams();
        this.buildFormParams();
        this.buildFileParams();

        Object[] params = new Object[parameters.length];
        // loop params
        for (int ii=0; ii<params.length; ii++) {
            Parameter parameter = parameters[ii];
            Class<?> parameterClazz = parameter.getType();
            // request
            if (parameterClazz == HttpRequest.class || parameterClazz == FullHttpRequest.class) {
                params[ii] = httpRequest;
            }
            // response
            else if (parameterClazz == HttpResponse.class || parameterClazz == FullHttpResponse.class) {
                params[ii] = httpResponse;
            }
            // response
            else if (parameterClazz == HttpHeaders.class) {
                params[ii] = httpRequest.headers();
            }
            // CookieParam : Set<Cookie>
            else if (parameter.getAnnotation(CookieParam.class) != null) {
                String annotationKey = parameter.getAnnotation(CookieParam.class).value();
                params[ii] = classCastValue(cookieParams.get(annotationKey), parameterClazz);
            }
            // HeaderParam : String
            else if (parameter.getAnnotation(HeaderParam.class) != null) {
                String annotationKey = parameter.getAnnotation(HeaderParam.class).value();
                params[ii] = parameterClazz.cast(headerParams.get(annotationKey));
            }
            // PathParam
            else if (parameter.getAnnotation(PathParam.class) != null) {
                String annotationKey = parameter.getAnnotation(PathParam.class).value();
                params[ii] = classCastValue(pathParams.get(annotationKey), parameterClazz);
            }
            // QueryParam
            else if (parameter.getAnnotation(QueryParam.class) != null) {
                String annotationKey = parameter.getAnnotation(QueryParam.class).value();
                params[ii] = classCastValue(queryParams.get(annotationKey), parameterClazz);
            }
            // FormParam
            else if (parameter.getAnnotation(FormParam.class) != null) {
                String annotationKey = parameter.getAnnotation(FormParam.class).value();
                params[ii] = parameterClazz.cast(formParams.get(annotationKey));
            }
            // upload file
            else if (parameter.getAnnotation(FileParam.class) != null) {
                String annotationKey = parameter.getAnnotation(FileParam.class).value();
                params[ii] = fileParams.get(annotationKey);
            }
            // null
            else {
                params[ii] = null;
            }
        }
        return params;
    }

    private <T> T classCastValue(String[] values, Class<T> clazz) {
        if (values==null || values.length<1) {
            return null;
        }

        if (clazz.isArray()) {
            List<T> newList = new ArrayList<>();
            for (String value : values) {
                newList.add(clazz.cast(value));
            }
            return clazz.cast(newList.toArray());
        }
        // else if (List.class.isAssignableFrom(clazz) || clazz.newInstance() instanceof List) {

        // }
        else {
            return clazz.cast(values[0]);
        }
    }

    private void buildHeaderParams() {
        if (httpRequest.headers()!=null) {
            for (Map.Entry<String, String> header : httpRequest.headers()) {
                headerParams.put(header.getKey(), header.getValue());
            }
        }
    }

    private void buildCookieParams() {
        if (httpRequest.headers().get("cookie")!=null) {
            String[] allCookie = httpRequest.headers().get("cookie").split(";");
            for(String cookie : allCookie) {
                int iof = cookie.indexOf("=");
                if (iof<1) {
                    continue;
                }
                cookieParams.put(cookie.substring(0, iof).trim(), cookie.substring(iof+1).trim());
            }
        }
    }

    private void buildQueryParams() {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(this.httpRequest.uri());
        for (Map.Entry<String, List<String>> p : queryStringDecoder.parameters().entrySet()) {
            String key = p.getKey().trim();
            List<String> valueList = p.getValue();
            queryParams.put(key, valueList.toArray(new String[0]));
        }
    }

    private void buildPathParams() {
    }

    private void buildFormParams() {
        if (httpRequest.content() != null) {
            // set Request Decoder
            HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), httpRequest, CharsetUtil.UTF_8);
            // loop data
            for (InterfaceHttpData data : postDecoder.getBodyHttpDatas()) {
                String name = data.getName();
                // 表单
                if (name!=null && data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                    formParams.put(name, ((MemoryAttribute) data).get());
                }
            }
        }
    }

    private void buildFileParams() {
        if (httpRequest.content() != null) {
            // set Request Decoder
            HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), httpRequest, CharsetUtil.UTF_8);
            // loop data
            for (InterfaceHttpData data : postDecoder.getBodyHttpDatas()) {
                String name = data.getName();
                // 上传文件的内容
                if (name!=null && data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                    try {
                        fileParams.put(name, ((MemoryFileUpload) data).retain().getFile());
                    }
                    catch (IOException ignore) {
                    }
                }
            }
        }
    }
}

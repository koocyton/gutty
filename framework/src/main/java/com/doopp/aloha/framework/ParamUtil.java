package com.doopp.aloha.framework;

import com.doopp.aloha.framework.annotation.FileParam;
import io.netty.buffer.ByteBuf;
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
    private HttpHeaders httpHeaders;
    // cookies
    private Map<String, String> cookieParams = new HashMap<>();
    // path params
    private Map<String, String> pathParams = new HashMap<>();
    // query params
    private Map<String, String> queryParams = new HashMap<>();
    // form params
    private Map<String, byte[]> formParams = new HashMap<>();
    // file params
    private Map<String, File> fileParams = new HashMap<>();

    private FullHttpRequest httpRequest;
    private FullHttpResponse httpResponse;

    private Object[] getParams(Parameter[] parameters, FullHttpRequest httpRequest, FullHttpResponse httpResponse) {
        // build data
        this.httpRequest   = httpRequest;
        this.httpResponse  = httpResponse;
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
                params[ii] = classCastValue(cookieParams.get(annotationKey), parameterClazz);parameterClazz.cast(cookieParams.get(annotationKey));
            }
            // HeaderParam : String
            else if (parameter.getAnnotation(HeaderParam.class) != null) {
                String annotationKey = parameter.getAnnotation(HeaderParam.class).value();
                params[ii] = httpRequest.headers().get(annotationKey);
            }
            // PathParam
            else if (parameter.getAnnotation(PathParam.class) != null) {
                String annotationKey = parameter.getAnnotation(PathParam.class).value();
                params[ii] = pathParamMap.get(annotationKey);
            }
            // QueryParam
            else if (parameter.getAnnotation(QueryParam.class) != null) {
                String annotationKey = parameter.getAnnotation(QueryParam.class).value();
                params[ii] = parameterClazz.cast(queryParamMap.get(annotationKey));
            }
            // FormParam
            else if (parameter.getAnnotation(FormParam.class) != null) {
                String annotationKey = parameter.getAnnotation(FormParam.class).value();
                params[ii] = parameterClazz.cast(formParamMap.get(annotationKey));
            }
            // upload file
            else if (parameter.getAnnotation(FileParam.class) != null) {
                String annotationKey = parameter.getAnnotation(FileParam.class).value();
                params[ii] = fileParamMap.get(annotationKey);
            }
            // null
            else {
                params[ii] = null;
            }
        }
        return params;
    }

    private <T> T classCastValue(String value, Class<T> clazz) {

    }

    private <T> T classCastValue(List<String> value, Class<T> clazz) {
        // if value is null
        if (value == null) {
            return clazz.cast(null);
        }
        // Long
        else if (clazz == Long.class) {
            return clazz.cast(Long.valueOf(value.get(0)));
        }
        // Integer
        else if (clazz == Integer.class) {
            return clazz.cast(Integer.valueOf(value.get(0)));
        }
        // Boolean
        else if (clazz == Boolean.class) {
            return clazz.cast(Boolean.valueOf(value.get(0)));
        }
        // String
        else if (clazz == String.class) {
            return clazz.cast(value.get(0));
        }
        // Float
        else if (clazz == Float.class) {
            return clazz.cast(Float.valueOf(value.get(0)));
        }
        // Double
        else if (clazz == Double.class) {
            return clazz.cast(Double.valueOf(value.get(0)));
        }
        // Short
        else if (clazz == Short.class) {
            return clazz.cast(Short.valueOf(value.get(0)));
        }
        // Long[]
        else if (clazz == Long[].class) {
            ArrayList<Long> longValues = new ArrayList<>();
            for (String s : value) {
                longValues.add(Long.valueOf(s));
            }
            return clazz.cast(longValues.toArray(new Long[0]));
        }
        // Integer[]
        else if (clazz == Integer[].class) {
            ArrayList<Integer> intValues = new ArrayList<>();
            for (String s : value) {
                intValues.add(Integer.valueOf(s));
            }
            return clazz.cast(intValues.toArray(new Integer[0]));
        }
        // String[]
        else if (clazz == String[].class) {
            return clazz.cast(value.toArray(new String[0]));
        }
        // default return null;
        else {
            return clazz.cast(value);
        }
    }

    private void buildHeaderParams() {
        this.httpHeaders = httpRequest.headers();
    }

    private void buildCookieParams() {
        if (httpRequest.headers()!=null && httpRequest.headers().get("cookie")!=null) {
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
            List<String> vals = p.getValue();
            if (vals.size() > 0) {
                String value = vals.get(0);
                queryParams.put(key, value);
            }
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

package com.doopp.gutty.framework;

import com.doopp.gutty.framework.annotation.FileParam;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Parameter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class HttpParam {

    private static final Logger logger = LoggerFactory.getLogger(HttpParam.class);

    // private static HttpParam httpParam = null;
    private FullHttpRequest httpRequest;
    private FullHttpResponse httpResponse;
    private WebSocketFrame webSocketFrame;
    private ChannelHandlerContext ctx;

    private HttpParam() {}

    public static HttpParam builder(ChannelHandlerContext ctx, FullHttpRequest httpRequest, FullHttpResponse httpResponse) {
        HttpParam httpParam = new HttpParam();
        httpParam.httpRequest = httpRequest;
        httpParam.httpResponse = httpResponse;
        httpParam.ctx = ctx;
        return httpParam;
        /*
        if (httpParam==null) {
            httpParam = new HttpParam();
            httpParam.httpRequest = httpRequest;
            httpParam.httpResponse = httpResponse;
            httpParam.ctx = ctx;
        }
        return httpParam;
        */
    }

    public static HttpParam builder(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        HttpParam httpParam = new HttpParam();
        httpParam.httpRequest = httpRequest;
        // httpParam.httpResponse = httpResponse;
        httpParam.ctx = ctx;
        return httpParam;
        /*
        if (httpParam==null) {
            httpParam = new HttpParam();
            httpParam.httpRequest = httpRequest;
            // httpParam.httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            httpParam.ctx = ctx;
        }
        return httpParam;
        */
    }

    public HttpParam setWebSocketFrame(WebSocketFrame webSocketFrame) {
        this.webSocketFrame = webSocketFrame;
        return this;
    }

    // http headers
    private Map<String, String> headerParams;
    // cookies
    private Map<String, String> cookieParams;
    // path params
    private Map<String, String> pathParams;
    // query params
    private Map<String, List<String>> queryParams;
    // form params
    private Map<String, List<String>> formParams;
    // file params
    private Map<String, List<FileUpload>> fileParams;

    private void resetParams() {
        // http headers
        headerParams = new HashMap<>();
        // cookies
        cookieParams = new HashMap<>();
        // path params
        pathParams = new HashMap<>();
        // query params
        queryParams = new HashMap<>();
        // form params
        formParams = new HashMap<>();
        // file params
        fileParams = new HashMap<>();
    }

    public Object[] getParams(Parameter[] parameters, Map<String, String> pathParams) {

        this.resetParams();

        this.buildHeaderParams();
        this.buildCookieParams();
        // this.buildPathParams(pathParams);
        this.buildQueryParams();
        this.buildFormParams();

        Object[] params = new Object[parameters.length];

        // loop params
        for (int ii=0; ii<params.length; ii++) {
            Parameter parameter = parameters[ii];
            Class<?> parameterClazz = parameter.getType();
            // ChannelHandlerContext
            if (parameterClazz == ChannelHandlerContext.class) {
                params[ii] = ctx;
            }
            // websocket frame
            else if (parameterClazz == WebSocketFrame.class) {
                params[ii] = webSocketFrame;
            }
            // text websocket frame
            else if (parameterClazz == TextWebSocketFrame.class && webSocketFrame instanceof TextWebSocketFrame) {
                params[ii] = webSocketFrame;
            }
            // binary websocket frame
            else if (parameterClazz == BinaryWebSocketFrame.class && webSocketFrame instanceof BinaryWebSocketFrame) {
                params[ii] = webSocketFrame;
            }
            // ping websocket frame
            else if (parameterClazz == PingWebSocketFrame.class && webSocketFrame instanceof PingWebSocketFrame) {
                params[ii] = webSocketFrame;
            }
            // pong websocket frame
            else if (parameterClazz == PongWebSocketFrame.class && webSocketFrame instanceof PongWebSocketFrame) {
                params[ii] = webSocketFrame;
            }
            // close websocket frame
            else if (parameterClazz == CloseWebSocketFrame.class && webSocketFrame instanceof CloseWebSocketFrame) {
                params[ii] = webSocketFrame;
            }
            // Channel
            else if (parameterClazz == Channel.class) {
                params[ii] = ctx.channel();
            }
            // request
            else if (parameterClazz == HttpRequest.class || parameterClazz == FullHttpRequest.class) {
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
                params[ii] = baseParamCase(cookieParams.get(annotationKey), parameterClazz);
            }
            // HeaderParam : String
            else if (parameter.getAnnotation(HeaderParam.class) != null) {
                String annotationKey = parameter.getAnnotation(HeaderParam.class).value();
                params[ii] = baseParamCase(headerParams.get(annotationKey), parameterClazz);
            }
            // PathParam
            else if (parameter.getAnnotation(PathParam.class) != null) {
                String annotationKey = parameter.getAnnotation(PathParam.class).value();
                params[ii] = baseParamCase(pathParams.get(annotationKey), parameterClazz);
            }
            // QueryParam
            else if (parameter.getAnnotation(QueryParam.class) != null) {
                String annotationKey = parameter.getAnnotation(QueryParam.class).value();
                params[ii] = listParamCase(queryParams.get(annotationKey), parameterClazz);
            }
            // FormParam
            else if (parameter.getAnnotation(FormParam.class) != null) {
                String annotationKey = parameter.getAnnotation(FormParam.class).value();
                params[ii] = listParamCase(formParams.get(annotationKey), parameterClazz);
            }
            // upload file
            else if (parameter.getAnnotation(FileParam.class) != null) {
                String annotationKey = parameter.getAnnotation(FileParam.class).value();
                String annotationPath = parameter.getAnnotation(FileParam.class).path();
                try {
                    params[ii] = fileParamCast(fileParams.get(annotationKey), annotationPath, parameterClazz);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            // null
            else {
                params[ii] = null;
            }
        }
        return params;
    }

    private <T> T baseParamCase(String value, Class<T> clazz) {
        if (value==null) {
            return null;
        }
        // Long
        else if (clazz == Long.class || clazz==long.class) {
            return clazz.cast(Long.valueOf(value));
        }
        // Integer
        else if (clazz == Integer.class || clazz==int.class) {
            return clazz.cast(Integer.valueOf(value));
        }
        // Boolean
        else if (clazz == Boolean.class || clazz==boolean.class) {
            return clazz.cast(Boolean.valueOf(value));
        }
        // String
        else if (clazz == String.class) {
            return clazz.cast(value);
        }
        // Float
        else if (clazz == Float.class || clazz==float.class) {
            return clazz.cast(Float.valueOf(value));
        }
        // Double
        else if (clazz == Double.class || clazz==double.class) {
            return clazz.cast(Double.valueOf(value));
        }
        // Short
        else if (clazz == Short.class || clazz==short.class) {
            return clazz.cast(Short.valueOf(value));
        }
        // Short
        else if (clazz == Date.class) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                return clazz.cast(format.parse(value));
            }
            catch (ParseException e) {
                return null;
            }
        }
        else {
            return clazz.cast(value);
        }
    }

    private <T> T listParamCase(List<String> values, Class<T> clazz) {
        if (values == null || values.size() < 1) {
            return null;
        }
        // List.class.isAssignableFrom(clazz)
        if (clazz.isArray()) {
            // Long
            if (clazz == Long[].class || clazz==long[].class) {
                List<Long> resultList = new ArrayList<>();
                for (int ii=0; ii<values.size(); ii++) {
                    resultList.add(ii, baseParamCase(values.get(ii), Long.class));
                }
                return clazz.cast(resultList.toArray(new Long[0]));
            }
            // Integer
            else if (clazz == Integer[].class || clazz==int[].class) {
                List<Integer> resultList = new ArrayList<>();
                for (int ii=0; ii<values.size(); ii++) {
                    resultList.add(ii, baseParamCase(values.get(ii), Integer.class));
                }
                return clazz.cast(resultList.toArray(new Integer[0]));
            }
            // Boolean
            else if (clazz == Boolean[].class || clazz==boolean[].class) {
                List<Boolean> resultList = new ArrayList<>();
                for (int ii=0; ii<values.size(); ii++) {
                    resultList.add(ii, baseParamCase(values.get(ii), Boolean.class));
                }
                return clazz.cast(resultList.toArray(new Boolean[0]));
            }
            // String
            else if (clazz == String[].class) {
                return clazz.cast(values);
            }
            // Float
            else if (clazz == Float[].class || clazz==float[].class) {
                List<Float> resultList = new ArrayList<>();
                for (int ii=0; ii<values.size(); ii++) {
                    resultList.add(ii, baseParamCase(values.get(ii), Float.class));
                }
                return clazz.cast(resultList.toArray(new Float[0]));
            }
            // Double
            else if (clazz == Double[].class || clazz==double[].class) {
                List<Double> resultList = new ArrayList<>();
                for (int ii=0; ii<values.size(); ii++) {
                    resultList.add(ii, baseParamCase(values.get(ii), Double.class));
                }
                return clazz.cast(resultList.toArray(new Double[0]));
            }
            // Short
            else if (clazz == Short[].class || clazz==short[].class) {
                List<Short> resultList = new ArrayList<>();
                for (int ii=0; ii<values.size(); ii++) {
                    resultList.add(ii, baseParamCase(values.get(ii), Short.class));
                }
                return clazz.cast(resultList.toArray(new Short[0]));
            }
        }
        return baseParamCase(values.get(0), clazz);
    }

    private <T> T fileParamCast(List<FileUpload> fileParams, String path, Class<T> clazz) throws IOException {
        // if fileParams is null
        if (fileParams == null || fileParams.size()<1) {
            return null;
        }
        // one
        else if (clazz == File.class) {
            String[] fileNameSplit = fileParams.get(0).getFilename().split("\\.");
            String fileName = fileNameSplit.length<=1
                    ? UUID.randomUUID().toString()
                    : UUID.randomUUID().toString() + "." + fileNameSplit[fileNameSplit.length-1];

            File file = new File(path + "/" + fileName);
            saveFile(file, fileParams.get(0));
            return clazz.cast(file);
        }
        // more
        else if (clazz == File[].class) {
            ArrayList<File> files = new ArrayList<>();
            for (int ii=0; ii<fileParams.size(); ii++) {
                String[] fileNameSplit = fileParams.get(ii).getFilename().split("\\.");
                String fileName = fileNameSplit.length<=1
                        ? UUID.randomUUID().toString()
                        : UUID.randomUUID().toString() + "." + fileNameSplit[fileNameSplit.length-1];
                files.add(ii, new File(path + "/" + fileName));
                saveFile(files.get(ii), fileParams.get(ii));
            }
            return clazz.cast(files.toArray(new File[0]));
        }
        // one
        else if (clazz == FileUpload.class) {
            return clazz.cast(fileParams.get(0));
        }
        // more
        else if (clazz == FileUpload[].class) {
            return clazz.cast(fileParams.toArray(new FileUpload[0]));
        }
        // // one
        // else if (clazz == MemoryFileUpload.class) {
        //     return clazz.cast(fileParams.get(0));
        // }
        // // more
        // else if (clazz == MemoryFileUpload[].class) {
        //     return clazz.cast(fileParams.toArray(new MemoryFileUpload[0]));
        // }
        // one byte[]
        else if (clazz == byte[].class) {
            return clazz.cast(fileParams.get(0).get());
        }
        // more byte[]
        else if (clazz == byte[][].class) {
            ArrayList<byte[]> byteValues = new ArrayList<>();
            for (FileUpload s : fileParams) {
                byteValues.add(s.get());
            }
            return clazz.cast(byteValues.toArray());
        } else {
            return clazz.cast(null);
        }
    }

    private void saveFile(File file, FileUpload fileUpload) {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            randomAccessFile.seek(0);
            randomAccessFile.write(fileUpload.get());
        }
        catch(Exception ignored) {}
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
            queryParams.put(p.getKey().trim(), p.getValue());
        }
    }

    private void buildPathParams(Map<String, String> pathParams) {
        this.pathParams = pathParams;
    }

    private void buildFormParams() {
        if (httpRequest.content() != null) {
            httpRequest.retain();
            // set Request Decoder
            HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), httpRequest, CharsetUtil.UTF_8);
            // loop data
            for (InterfaceHttpData data : postDecoder.getBodyHttpDatas()) {
                String name = data.getName();
                // 表单
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
        }
    }
}

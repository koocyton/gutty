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
import java.io.RandomAccessFile;
import java.lang.reflect.Parameter;
import java.util.*;

class HttpParam {

    private static final Logger logger = LoggerFactory.getLogger(HttpParam.class);

    private static HttpParam paramUtil = null;
    private FullHttpRequest httpRequest;
    private FullHttpResponse httpResponse;

    private HttpParam() {}

    public static HttpParam singleBuilder(FullHttpRequest httpRequest, FullHttpResponse httpResponse) {
        if (paramUtil==null) {
            paramUtil = new HttpParam();
            paramUtil.httpRequest = httpRequest;
            paramUtil.httpResponse = httpResponse;
        }
        return paramUtil;
    }

    // http headers
    private final Map<String, String> headerParams = new HashMap<>();
    // cookies
    private final Map<String, String> cookieParams = new HashMap<>();
    // path params
    private final Map<String, String> pathParams = new HashMap<>();
    // query params
    private final Map<String, String[]> queryParams = new HashMap<>();
    // form params
    private final Map<String, List<String>> formParams = new HashMap<>();
    // file params
    private final Map<String, List<FileUpload>> fileParams = new HashMap<>();

    public Object[] getParams(Parameter[] parameters) {

        this.buildHeaderParams();
        this.buildCookieParams();
        this.buildPathParams();
        this.buildQueryParams();
        this.buildFormParams();

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
                params[ii] = stringCastValue(cookieParams.get(annotationKey), parameterClazz);
            }
            // HeaderParam : String
            else if (parameter.getAnnotation(HeaderParam.class) != null) {
                String annotationKey = parameter.getAnnotation(HeaderParam.class).value();
                params[ii] = stringCastValue(headerParams.get(annotationKey), parameterClazz);
            }
            // PathParam
            else if (parameter.getAnnotation(PathParam.class) != null) {
                String annotationKey = parameter.getAnnotation(PathParam.class).value();
                params[ii] = stringCastValue(pathParams.get(annotationKey), parameterClazz);
            }
            // QueryParam
            else if (parameter.getAnnotation(QueryParam.class) != null) {
                String annotationKey = parameter.getAnnotation(QueryParam.class).value();
                params[ii] = arrayCastValue(queryParams.get(annotationKey), parameterClazz);
            }
            // FormParam
            else if (parameter.getAnnotation(FormParam.class) != null) {
                String annotationKey = parameter.getAnnotation(FormParam.class).value();
                params[ii] = listCastValue(formParams.get(annotationKey), parameterClazz);
            }
            // upload file
            else if (parameter.getAnnotation(FileParam.class) != null) {
                String annotationKey = parameter.getAnnotation(FileParam.class).value();
                String annotationPath = parameter.getAnnotation(FileParam.class).path();
                try {
                    params[ii] = fileCastValue(fileParams.get(annotationKey), annotationPath, parameterClazz);
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

    private <T> T listCastValue(List<String> values, Class<T> clazz) {
        if (values==null || values.size()<1) {
            return null;
        }
        return arrayCastValue(values.toArray(new String[0]), clazz);
    }

    private <T> T stringCastValue(String value, Class<T> clazz) {
        if (value==null) {
            return null;
        }
        return arrayCastValue(new String[]{value}, clazz);
    }

    private <T> T arrayCastValue(String[] values, Class<T> clazz) {
        if (values==null || values.length<1) {
            return null;
        }
        try {
            if (clazz.isArray() || List.class.isAssignableFrom(clazz) || clazz.newInstance() instanceof List) {
                List<T> newList = new ArrayList<>();
                for (String value : values) {
                    newList.add(clazz.cast(value));
                }
                return clazz.isArray() ? clazz.cast(newList.toArray()) : clazz.cast(newList);
            }
            else {
                return clazz.cast(values[0]);
            }
        }
        catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T fileCastValue(List<FileUpload> fileParams, String path, Class<T> clazz) throws IOException {
        // if fileParams is null
        if (fileParams == null) {
            return clazz.cast(null);
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
            queryParams.put(p.getKey().trim(), p.getValue().toArray(new String[0]));
        }
    }

    private void buildPathParams() {
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
            httpRequest.release();
        }
    }
}

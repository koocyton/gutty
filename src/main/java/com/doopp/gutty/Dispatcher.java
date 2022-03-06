package com.doopp.gutty;

import com.doopp.gutty.annotation.websocket.*;
import com.doopp.gutty.json.MessageConverter;
import com.doopp.gutty.redis.JdkSerializableHelper;
import com.doopp.gutty.redis.SerializableHelper;
import com.doopp.gutty.view.ModelMap;
import com.doopp.gutty.view.ViewResolver;
import com.google.inject.Injector;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessageV3;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpMethod;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dispatcher {

    private final Map<String, HttpRoute> httpRouteMap = new HashMap<>();
    private final List<HttpRoute> patternHttpRouteList = new ArrayList<>();

    private final Map<String, SocketRoute> socketRouteMap = new HashMap<>();
    private final List<SocketRoute> patternSocketRouteList = new ArrayList<>();

    private static final Object RD_LOCK = new Object();
    private static volatile Dispatcher dispatcher;

    private Dispatcher() {
    }

    public static Dispatcher getInstance() {
        // Double-check idiom for lazy initialization of fields.
        // Local variable is used to limit the number of more expensive accesses to a volatile field.
        Dispatcher result = dispatcher;
        if (result == null) { // First check (no locking)
            synchronized (RD_LOCK) {
                result = dispatcher;
                if (result == null) { // Second check (with locking)
                    dispatcher = result = new Dispatcher();
                }
            }
        }
        return result;
    }

    public void addHttpRoute(Class<? extends Annotation> httpMethodAnnotation, String requestUri, Class<?> clazz, Method method, Parameter[] parameters) {
        String httpMethod = httpMethodAnnotation.getSimpleName().toLowerCase();
        String routeKey = httpMethod+":"+requestUri;
        if (routeKey.contains("{")) {
            patternHttpRouteList.add(new HttpRoute(routeKey, clazz, method, parameters));
        }
        else {
            httpRouteMap.put(routeKey, new HttpRoute(routeKey, clazz, method, parameters));
        }
    }

    public byte[] executeHttpRoute(Injector injector, ChannelHandlerContext ctx, FullHttpRequest httpRequest, FullHttpResponse httpResponse) {
        // get route
        HttpRoute httpRoute = this.getHttpRoute(httpRequest.method(), httpRequest.uri());
        if (httpRoute ==null) {
            throw new NotFoundException("Oho ... Not found route target");
        }
        // get controller
        Object controller = Gutty.getInstance(injector, httpRoute.getClazz());
        if (controller==null) {
            throw new NotFoundException("Oho ... Not found controller : " + httpRoute.getClazz());
        }
        // ModelMap
        ModelMap modelMap = new ModelMap();
        // method invoke
        Object result;
        try {
            result = (httpRoute.getParameters().length == 0)
                    ? httpRoute.getMethod().invoke(controller)
                    : httpRoute.getMethod().invoke(controller, HttpParam.builder(injector, ctx, httpRequest, httpResponse).setModelMap(modelMap).getParams(httpRoute.getParameters(), httpRoute.getPathParamMap()));
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        // content type
        String contentType = methodProductsValue(httpRoute.getMethod());
        // 如果要求返回 JSON
        if (contentType.contains(MediaType.APPLICATION_JSON)) {
            MessageConverter messageConverter = Gutty.getInstance(injector, MessageConverter.class);
            if (messageConverter !=null) {
                result = messageConverter.toJson(result);
            }
        }
        // 如果要求返回字符串，并且有适配的模板
        else if (result instanceof String && contentType.contains(MediaType.TEXT_HTML)) {
            ViewResolver viewResolver = Gutty.getInstance(injector, ViewResolver.class);
            if (viewResolver != null) {
                result = viewResolver.template(modelMap, (String) result);
            }
        }
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        // return
        if (result instanceof String) {
            return ((String) result).getBytes();
        }
        else if (result instanceof byte[]) {
            return (byte[]) result;
        }
        else if (result instanceof GeneratedMessageV3) {
            return ((GeneratedMessageV3) result).toByteArray();
        }
        else if (result instanceof GeneratedMessage) {
            return ((GeneratedMessage) result).toByteArray();
        }
        return (new JdkSerializableHelper()).serialize(result);
    }

    public void addSocketRoute(String requestUri, Class<?> clazz) {
        if (requestUri.contains("{")) {
            patternSocketRouteList.add(new SocketRoute(requestUri, clazz));
        }
        else {
            socketRouteMap.put(requestUri, new SocketRoute(requestUri, clazz));
        }
    }

    public SocketRoute getSocketRoute(String connectUri) {
        int indexOf = connectUri.indexOf("?");
        if (indexOf!=-1) {
            connectUri = connectUri.substring(0, indexOf);
        }
        // try index uri
        SocketRoute r = socketRouteMap.get(connectUri);
        if (r!=null)
            return r;
        // try match uri
        for (SocketRoute socketRoute : patternSocketRouteList) {
            Matcher matcher = socketRoute.getUriPattern().matcher(connectUri);
            if (matcher.find()) {
                socketRoute.setPathValues(new String[matcher.groupCount()]);
                for (int ii = 0; ii < matcher.groupCount(); ii++) {
                    socketRoute.getPathValues()[ii] = matcher.group(ii + 1);
                }
                return socketRoute;
            }
        }
        return null;
    }

    private HttpRoute getHttpRoute(HttpMethod httpMethod, String requestUri) {
        int indexOf = requestUri.indexOf("?");
        if (indexOf!=-1) {
            requestUri = requestUri.substring(0, indexOf);
        }
        String requestKey = httpMethod.name().toLowerCase() + ":" + requestUri;
        // try index uri
        HttpRoute r = httpRouteMap.get(requestKey);
        if (r!=null)
            return r;
        // try match uri
        for (HttpRoute httpRoute : patternHttpRouteList) {
            Matcher matcher = httpRoute.getUriPattern().matcher(requestKey);
            if (matcher.find()) {
                httpRoute.setPathValues(new String[matcher.groupCount()]);
                for (int ii = 0; ii < matcher.groupCount(); ii++) {
                    httpRoute.getPathValues()[ii] = matcher.group(ii + 1);
                }
                return httpRoute;
            }
        }
        return null;
    }

    public String methodProductsValue(Method method) {
        String contentType = MediaType.TEXT_HTML;
        if (method != null && method.isAnnotationPresent(Produces.class)) {
            StringBuilder _contentType = new StringBuilder();
            for (String mediaType : method.getAnnotation(Produces.class).value()) {
                _contentType.append((_contentType.toString().equals("")) ? mediaType : "; " + mediaType);
            }
            contentType = _contentType.toString().contains("charset") ? _contentType.toString() : _contentType + "; charset=UTF-8";
        }
        return contentType;
    }
}

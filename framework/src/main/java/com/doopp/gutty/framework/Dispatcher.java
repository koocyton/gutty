package com.doopp.gutty.framework;

import com.google.inject.Injector;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;

public class Dispatcher {

    private static final Logger logger = LoggerFactory.getLogger(Dispatcher.class);

    private final Map<String, Route> routeMap = new HashMap<>();

    private Dispatcher() {
    }

    private static Dispatcher dispatcher = null;

    public static Dispatcher singleBuilder() {
        if (dispatcher==null) {
            dispatcher = new Dispatcher();
        }
        return dispatcher;
    }

    public void addRoute(Class<? extends Annotation> httpMethodAnnotation, String requestUri, Class<?> clazz, Method method, Parameter[] parameters) {
        String routeKey = httpMethodAnnotation.getSimpleName().toLowerCase()+":"+requestUri;
        Route route = new Route(routeKey, clazz, method, parameters);
        routeMap.put(route.getKey(), route);
    }

    public HttpResponse respondRequest (Injector injector, FullHttpRequest httpRequest) {
        // init httpResponse
        FullHttpResponse httpResponse = new DefaultFullHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.OK);
        // execute route
        byte[] result = executeRoute(injector, httpRequest, httpResponse);
        httpResponse.content().writeBytes(Unpooled.copiedBuffer(result));
        // set length
        httpResponse.headers().set(CONTENT_LENGTH, httpResponse.content().readableBytes());
        return httpResponse;
    }

    private byte[] executeRoute(Injector injector, FullHttpRequest httpRequest, FullHttpResponse httpResponse) {
        // get route
        Route route = this.getRoute(httpRequest.method(), httpRequest.uri());
        if (route==null) {
            throw new RuntimeException("Oho ... Not found route target");
        }
        // get controller
        Object controller = injector.getInstance(route.getClazz());
        if (controller==null) {
            throw new RuntimeException("Oho ... Not found controller : " + route.getClazz());
        }
        // method invoke
        Object result;
        try {
            result = (route.getParameters().length==0)
                    ? route.getMethod().invoke(controller)
                    : route.getMethod().invoke(controller, HttpParam.singleBuilder(httpRequest, httpResponse).getParams(route.getParameters()));
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
        // return
        if (result instanceof String) {
            return ((String) result).getBytes();
        }
        return result.toString().getBytes();
    }


    private Route getRoute(HttpMethod httpMethod, String requestUri) {
        int indexOf = requestUri.indexOf("?");
        if (indexOf!=-1) {
            requestUri = requestUri.substring(0, indexOf);
        }
        return routeMap.get(httpMethod.name().toLowerCase() + ":" + requestUri);
    }

    public static class Route {
        private String key;
        private Class<?> clazz;
        private Method method;
        private Parameter[] parameters;
        private Route() {
        }
        Route(String key, Class<?> clazz, Method method, Parameter[] parameters) {
            this.key = key;
            this.clazz = clazz;
            this.method = method;
            this.parameters = parameters;
        }
        public void setKey(String key) {
            this.key = key;
        }
        public String getKey() {
            return key;
        }
        public void setClazz(Class<?> clazz) {
            this.clazz = clazz;
        }
        public Class<?> getClazz() {
            return clazz;
        }
        public void setMethod(Method method) {
            this.method = method;
        }
        public Method getMethod() {
            return method;
        }
        public void setParameters(Parameter[] parameters) {
            this.parameters = parameters;
        }
        public Parameter[] getParameters() {
            return parameters;
        }
    }
}

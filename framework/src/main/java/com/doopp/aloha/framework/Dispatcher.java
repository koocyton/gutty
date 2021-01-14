package com.doopp.aloha.framework;

import com.google.inject.Inject;
import com.google.inject.Injector;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;

public class Dispatcher {

    @Inject
    private Injector injector;

    Map<String, Route> routeMap = new HashMap<>();

    public void addRoute(Class<? extends Annotation> httpMethodAnnotation, String requestUri, Class<?> clazz, Method method, Parameter[] parameters) {
        Route route = new Route(clazz, method, parameters);
        String routeKey = httpMethodAnnotation.getSimpleName().toLowerCase()+":"+requestUri;
        routeMap.put(routeKey, route);
    }

    public HttpResponse respondRequest (HttpRequest httpRequest) {
        // init httpResponse
        FullHttpResponse httpResponse = new DefaultFullHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.OK);
        // dispatch
        Object result = this.executeRoute(httpRequest);
        httpResponse.content().writeBytes(Unpooled.copiedBuffer(result.toString().getBytes()));
        // set length
        httpResponse.headers().set(CONTENT_LENGTH, httpResponse.content().readableBytes());
        return httpResponse;
    }

    private Object executeRoute(HttpRequest httpRequest) {
        // dispatch
        Route route = this.getRoute(httpRequest.method(), httpRequest.uri());
        // check route
        if (route==null) {
            return null;
        }
        //
        Object controller = injector.getInstance(route.getClazz());
        Object result = null;
        if (controller != null) {
            result = route.getMethod().invoke(controller);
        }
        return result;
    }


    private Route getRoute(HttpMethod httpMethod, String requestUri) {
        return routeMap.get(httpMethod.name().toLowerCase() + ":" + requestUri);
    }

    public static class Route {
        private Class<?> clazz;
        private Method method;
        private Parameter[] parameters;
        Route(Class<?> clazz, Method method, Parameter[] parameters) {
            this.clazz = clazz;
            this.method = method;
            this.parameters = parameters;
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

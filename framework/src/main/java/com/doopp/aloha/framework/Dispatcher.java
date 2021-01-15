package com.doopp.aloha.framework;

import com.doopp.aloha.framework.annotation.FileParam;
import com.google.inject.Injector;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;

public class Dispatcher {

    private static final Logger logger = LoggerFactory.getLogger(Dispatcher.class);

    private final Map<String, Route> routeMap = new HashMap<>();

    public void addRoute(Class<? extends Annotation> httpMethodAnnotation, String requestUri, Class<?> clazz, Method method, Parameter[] parameters) {
        String routeKey = httpMethodAnnotation.getSimpleName().toLowerCase()+":"+requestUri;
        Route route = new Route(routeKey, clazz, method, parameters);
        routeMap.put(route.getKey(), route);
    }

    public HttpResponse respondRequest (Injector injector, HttpRequest httpRequest) {
        // init httpResponse
        FullHttpResponse httpResponse = new DefaultFullHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.OK);
        // execute route
        byte[] result = executeRoute(injector, httpRequest, httpResponse);
        httpResponse.content().writeBytes(Unpooled.copiedBuffer(result));
        // set length
        httpResponse.headers().set(CONTENT_LENGTH, httpResponse.content().readableBytes());
        return httpResponse;
    }

    private byte[] executeRoute(Injector injector, HttpRequest httpRequest, HttpResponse httpResponse) {
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
        try {
            Object result = (route.getParameters().length==0)
                    ? route.getMethod().invoke(controller)
                    : route.getMethod().invoke(controller, getParams(route.getParameters(), httpRequest, httpResponse));
            if (result instanceof String) {
                return ((String) result).getBytes();
            }
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private Object[] getParams(Parameter[] parameters, HttpRequest httpRequest, HttpResponse httpResponse) {
        // init params
        Object[] params = new Object[parameters.length];
        // get request headers
        HttpHeaders httpHeaders = httpRequest.headers();
        // init request cookies
        Map<String, Cookie> httpCookieMap = new HashMap<>();
        if (httpHeaders.get("cookie")!=null) {
            Cookie cookie = new DefaultCookie("", "");
            httpCookieMap.put("", cookie);
        }
        // init request query params
        Map<String, Object> queryParams = new HashMap<>();
        // loop params
        for (int ii=0; ii<params.length; ii++) {
            Parameter parameter = parameters[ii];
            Class<?> parameterClazz = parameter.getType();
            // request
            if (parameterClazz == HttpRequest.class) {
                params[ii] = httpRequest;
            }
            // response
            else if (parameterClazz == HttpResponse.class) {
                params[ii] = httpResponse;
            }
            // response
            else if (parameterClazz == HttpHeaders.class) {
                params[ii] = httpRequest.headers();
            }
            // CookieParam : Set<Cookie>
            else if (parameter.getAnnotation(CookieParam.class) != null) {
                String annotationKey = parameter.getAnnotation(CookieParam.class).value();
                params[ii] = httpCookieMap.get(annotationKey);
            }
            // HeaderParam : String
            else if (parameter.getAnnotation(HeaderParam.class) != null) {
                String annotationKey = parameter.getAnnotation(HeaderParam.class).value();
                params[ii] = httpRequest.headers().get(annotationKey);
            }
            // PathParam
            else if (parameter.getAnnotation(PathParam.class) != null) {
                String annotationKey = parameter.getAnnotation(PathParam.class).value();
                params[ii] = null;
            }
            // QueryParam
            else if (parameter.getAnnotation(QueryParam.class) != null) {
                String annotationKey = parameter.getAnnotation(QueryParam.class).value();
                params[ii] = queryParams.get(annotationKey);
            }
            // FormParam
            else if (parameter.getAnnotation(FormParam.class) != null) {
                String annotationKey = parameter.getAnnotation(FormParam.class).value();
                params[ii] = null;
            }
            // upload file
            else if (parameter.getAnnotation(FileParam.class) != null) {
                String annotationKey = parameter.getAnnotation(FileParam.class).value();
                params[ii] = null;
            }
            // null
            else {
                params[ii] = null;
            }
        }
        return params;
    }


    private Route getRoute(HttpMethod httpMethod, String requestUri) {
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

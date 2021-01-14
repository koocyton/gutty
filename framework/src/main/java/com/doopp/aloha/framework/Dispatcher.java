package com.doopp.aloha.framework;

import io.netty.handler.codec.http.HttpMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

public class Dispatcher {

    Map<String, Route> routeMap = new HashMap<>();

    public void addRoute(Class<? extends Annotation> httpMethodAnnotation, String requestUri, Class<?> clazz, Method method, Parameter[] parameters) {
        Route route = new Route(clazz, method, parameters);
        String routeKey = httpMethodAnnotation.getSimpleName().toLowerCase()+":"+requestUri;
        routeMap.put(routeKey, route);
    }

    public Route getRoute(HttpMethod httpMethod, String requestUri) {
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

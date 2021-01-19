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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;

public class Dispatcher {

    private static final Logger logger = LoggerFactory.getLogger(Dispatcher.class);

    private final Map<String, Route> uriRouteMap = new HashMap<>();
    private final List<Route> patternRouteList = new ArrayList<>();

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
        String httpMethod = httpMethodAnnotation.getSimpleName().toLowerCase();
        String routeKey = httpMethod+":"+requestUri;
        if (routeKey.contains("{")) {
            patternRouteList.add(new Route(routeKey, clazz, method, parameters));
        }
        else {
            uriRouteMap.put(routeKey, new Route(routeKey, clazz, method, parameters));
        }
    }

    private static String quote(StringBuilder builder) {
        return builder.length() > 0 ? Pattern.quote(builder.toString()) : "";
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
                    : route.getMethod().invoke(controller, HttpParam.singleBuilder(httpRequest, httpResponse).getParams(route.getParameters(), route.getPathParamMap()));
        }
        catch(Exception e) {
            e.printStackTrace();
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
        String requestKey = httpMethod.name().toLowerCase() + ":" + requestUri;
        // try index uri
        Route r = uriRouteMap.get(requestKey);
        if (r!=null)
            return r;
        // try match uri
        for (Route route : patternRouteList) {
            Matcher matcher = route.getUriPattern().matcher(requestKey);
            if (matcher.find()) {
                route.setPathValues(new String[matcher.groupCount()]);
                for (int ii = 0; ii < matcher.groupCount(); ii++) {
                    route.getPathValues()[ii] = matcher.group(ii + 1);
                }
                return route;
            }
        }
        return null;
    }

    public static class Route {
        private Pattern uriPattern;
        private String key;
        private Class<?> clazz;
        private Method method;
        private Parameter[] parameters;
        private String[] pathFields;
        private String[] pathValues;
        private Route() {
        }
        Route(String key, Class<?> clazz, Method method, Parameter[] parameters) {
            this.key = key;
            this.clazz = clazz;
            this.method = method;
            this.parameters = parameters;
            if (key.contains("{")) {
                this.uriPattern = parseUri(key);
                Matcher matcher = this.uriPattern.matcher(this.key);
                if (matcher.find()) {
                    pathFields = new String[matcher.groupCount()];
                    for (int ii = 0; ii < matcher.groupCount(); ii++) {
                        pathFields[ii] = matcher.group(ii + 1).substring(1, matcher.group(ii + 1).length()-1);
                    }
                }
            }
        }
        public Map<String, String> getPathParamMap() {
            Map<String, String> pathParamMap = new HashMap<>();
            if (pathFields.length!=pathValues.length) {
                return pathParamMap;
            }
            for (int ii = 0; ii < pathFields.length; ii++) {
                pathParamMap.put(pathFields[ii], pathValues[ii]);
            }
            return pathParamMap;
        }
        public void setPathValues(String[] pathValues) {
            this.pathValues = pathValues;
        }
        public String[] getPathValues() {
            return pathValues;
        }
        public void setPathFields(String[] pathFields) {
            this.pathFields = pathFields;
        }
        public String[] getPathFields() {
            return pathFields;
        }
        public void setUriPattern(Pattern uriPattern) {
            this.uriPattern = uriPattern;
        }
        public Pattern getUriPattern() {
            return uriPattern;
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

    private static Pattern parseUri(String uriTemplate) {
        int level = 0;
        // List<String> variableNames = new ArrayList();
        StringBuilder pattern = new StringBuilder();
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < uriTemplate.length(); ++i) {
            char c = uriTemplate.charAt(i);
            if (c == '{') {
                ++level;
                if (level == 1) {
                    pattern.append(quote(builder));
                    builder = new StringBuilder();
                    continue;
                }
            } else if (c == '}') {
                --level;
                if (level == 0) {
                    String variable = builder.toString();
                    int idx = variable.indexOf(58);
                    if (idx == -1) {
                        pattern.append("([^/]*)");
                        // variableNames.add(variable);
                    } else {
                        if (idx + 1 == variable.length()) {
                            throw new IllegalArgumentException("No custom regular expression specified after ':' in \"" + variable + "\"");
                        }

                        String regex = variable.substring(idx + 1, variable.length());
                        pattern.append('(');
                        pattern.append(regex);
                        pattern.append(')');
                        // variableNames.add(variable.substring(0, idx));
                    }
                    builder = new StringBuilder();
                    continue;
                }
            }
            builder.append(c);
        }

        if (builder.length() > 0) {
            pattern.append(quote(builder));
        }

        return Pattern.compile(pattern.toString());
    }
}

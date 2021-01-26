package com.doopp.gutty.framework;

import com.doopp.gutty.framework.annotation.websocket.*;
import com.google.inject.Injector;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.AttributeKey;
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

    private static String quote(StringBuilder builder) {
        return builder.length() > 0 ? Pattern.quote(builder.toString()) : "";
    }

    public byte[] executeHttpRoute(Injector injector, ChannelHandlerContext ctx, FullHttpRequest httpRequest, FullHttpResponse httpResponse) {
        // get route
        HttpRoute httpRoute = this.getHttpRoute(httpRequest.method(), httpRequest.uri());
        if (httpRoute ==null) {
            throw new RuntimeException("Oho ... Not found route target");
        }
        // get controller
        Object controller = injector.getInstance(httpRoute.getClazz());
        if (controller==null) {
            throw new RuntimeException("Oho ... Not found controller : " + httpRoute.getClazz());
        }
        // method invoke
        Object result;
        try {
            result = (httpRoute.getParameters().length==0)
                    ? httpRoute.getMethod().invoke(controller)
                    : httpRoute.getMethod().invoke(controller, HttpParam.builder(ctx, httpRequest, httpResponse).getParams(httpRoute.getParameters(), httpRoute.getPathParamMap()));
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

    public static class SocketRoute {
        private Pattern uriPattern;
        private String key;
        private Class<?> clazz;
        private String[] pathFields;
        private String[] pathValues;
        private List<Method> openMethodList;
        private List<Method> messageMethodList;
        private List<Method> textMethodList;
        private List<Method> binaryMethodList;
        private List<Method> closeMethodList;
        private List<Method> pingMethodList;
        private List<Method> pongMethodList;
        private SocketRoute() {
        }
        public SocketRoute(String key, Class<?> clazz) {
            this.key = key;
            this.clazz = clazz;
            for (Method method : clazz.getMethods()) {
                if (method.getAnnotation(Open.class)!=null) {
                    if (openMethodList==null) {
                        openMethodList = new ArrayList<>();
                    }
                    openMethodList.add(method);
                }
                else if (method.getAnnotation(Message.class)!=null) {
                    if (messageMethodList==null) {
                        messageMethodList = new ArrayList<>();
                    }
                    messageMethodList.add(method);
                }
                else if (method.getAnnotation(TextMessage.class)!=null) {
                    if (textMethodList==null) {
                        textMethodList = new ArrayList<>();
                    }
                    textMethodList.add(method);
                }
                else if (method.getAnnotation(BinaryMessage.class)!=null) {
                    if (binaryMethodList==null) {
                        binaryMethodList = new ArrayList<>();
                    }
                    binaryMethodList.add(method);
                }
                else if (method.getAnnotation(Close.class)!=null) {
                    if (closeMethodList==null) {
                        closeMethodList = new ArrayList<>();
                    }
                    closeMethodList.add(method);
                }
                else if (method.getAnnotation(Ping.class)!=null) {
                    if (pingMethodList==null) {
                        pingMethodList = new ArrayList<>();
                    }
                    pingMethodList.add(method);
                }
                else if (method.getAnnotation(Pong.class)!=null) {
                    if (pongMethodList==null) {
                        pongMethodList = new ArrayList<>();
                    }
                    pongMethodList.add(method);
                }
            }
        }
        public Map<String, String> getPathParamMap() {
            Map<String, String> pathParamMap = new HashMap<>();
            if (pathFields==null || pathValues==null || pathFields.length!=pathValues.length) {
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
        public String getKey() {
            return key;
        }
        public Class<?> getClazz() {
            return clazz;
        }
        public List<Method> getOpenMethodList() {
            return openMethodList;
        }
        public List<Method> getCloseMethodList() {
            return closeMethodList;
        }
        public List<Method> getMessageMethodList() {
            return messageMethodList;
        }
        public List<Method> getTextMethodList() {
            return textMethodList;
        }
        public List<Method> getBinaryMethodList() {
            return binaryMethodList;
        }
        public List<Method> getPingMethodList() {
            return pingMethodList;
        }
        public List<Method> getPongMethodList() {
            return pongMethodList;
        }
    }

    public static class HttpRoute {
        private Pattern uriPattern;
        private String key;
        private Class<?> clazz;
        private Method method;
        private Parameter[] parameters;
        private String[] pathFields;
        private String[] pathValues;
        private HttpRoute() {
        }
        HttpRoute(String key, Class<?> clazz, Method method, Parameter[] parameters) {
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
            if (pathFields==null || pathValues==null || pathFields.length!=pathValues.length) {
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
}

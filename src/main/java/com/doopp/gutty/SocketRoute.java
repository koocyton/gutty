package com.doopp.gutty;

import com.doopp.gutty.annotation.websocket.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SocketRoute {
    private Pattern uriPattern;
    private String key;
    private Class<?> clazz;
    private String[] pathFields;
    private String[] pathValues;
    private String subprotocol;
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

        // socket Annotation
        Socket socketAnnotation = clazz.getAnnotation(Socket.class);
        subprotocol = socketAnnotation.subprotocol().equals("") ? null : socketAnnotation.subprotocol();

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
            else if (method.getAnnotation(JsonFrame.class)!=null) {
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
            else if (method.getAnnotation(ProtobufFrame.class)!=null) {
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
    public void setSubprotocol(String subprotocol) {
        this.subprotocol = subprotocol;
    }
    public String getSubprotocol() {
        return subprotocol;
    }
}

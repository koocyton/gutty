package com.doopp.gutty;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpRoute {
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

    private static String quote(StringBuilder builder) {
        return builder.length() > 0 ? Pattern.quote(builder.toString()) : "";
    }
}

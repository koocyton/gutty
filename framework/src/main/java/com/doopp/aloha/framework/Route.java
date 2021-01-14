package com.doopp.aloha.framework;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

class Route {

    Class<?> clazz;

    Method method;

    Parameter[] parameters;

    public Route(Class<?> clazz, Method method) {
        this.clazz = clazz;
        this.method = method;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Parameter[] getParameters() {
        return parameters;
    }

    public void setParameters(Parameter[] parameters) {
        this.parameters = parameters;
    }
}

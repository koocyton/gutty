package com.doopp.gutty.framework.json;

public interface HttpMessageConverter {

    String toJson(Object object);

    <T> T fromJson(String json, Class<T> clazz);
}


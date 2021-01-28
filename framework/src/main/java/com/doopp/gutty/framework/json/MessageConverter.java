package com.doopp.gutty.framework.json;

public interface MessageConverter {

    String toJson(Object object);

    <T> T fromJson(String json, Class<T> clazz);
}


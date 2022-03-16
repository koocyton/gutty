package com.doopp.gutty.redis;

public interface SerializableHelper {

    public byte[] serialize(Object obj);

    public <T> T deserialize(byte[] bytes, Class<T> clazz);
}

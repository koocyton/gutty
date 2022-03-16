package com.doopp.gutty.redis;


import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class LettuceClient {

    private final RedisClient redisClient;

    private final JdkSerializableHelper serializableHelper = new JdkSerializableHelper();

    public LettuceClient(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    public String get(String key) {
        return this.stringCmd(c-> c.get(key));
    }

    public void set(String key, String value) {
        this.stringCmd(c-> c.set(key, value));
    }

    public void set(byte[] key, Object object) {
        this.byteArrayCmd(c->{
            byte[] objectBytes = serializableHelper.serialize(object);
            if (objectBytes!=null) {
                c.set(key, objectBytes);
            }
            return key;
        });
    }

    public void setEx(String key, long seconds, String value) {
        this.stringCmd(c->c.setex(key, seconds, value));
    }

    public void setEx(byte[] key, long seconds, Object obj) {
        this.byteArrayCmd(c->{
            byte[] objectBytes = serializableHelper.serialize(obj);
            if (objectBytes!=null) {
                c.setex(key, seconds, objectBytes);
            }
            return key;
        });
    }

    public <T> T get(byte[] key, Class<T> clazz) {
        byte[] res = this.byteArrayCmd(c->c.get(key));
        if (res==null) {
            return null;
        }
        return serializableHelper.deserialize(res, clazz);
    }

    public void del(String... keys) {
        Set<byte[]> buffer=new HashSet<>();
        for (String key : keys) {
            buffer.add(key.getBytes());
        }
        del(buffer.toArray(new byte[][]{}));
    }

    public void del(byte[]... keys) {
        this.byteArrayCmd(c->{
            for (byte[] key : keys) {
                c.del(key);
            }
            return null;
        });
    }

    public void hSet(String key, String field, String value) {
        this.stringCmd(c-> c.hset(key, field, value));
    }

    public String hGet(String key, String field) {
        return this.stringCmd(c-> c.hget(key, field));
    }

    public boolean hSet(byte[] key, byte[] field, Object object) {
        if (key==null || field==null || object==null) {
            return false;
        }
        return Boolean.TRUE.equals(
                this.byteArrayCmd(c -> c.hset(key, field, serializableHelper.serialize(object)))
        );
    }

    public <T> T hGet(byte[] key, byte[] field, Class<T> clazz) {
        if (key==null || field==null) {
            return null;
        }
        byte[] b = this.byteArrayCmd(c-> c.hget(key, field));
        if (b==null) {
            return null;
        }
        return serializableHelper.deserialize(b, clazz);
    }

    private <T> T stringCmd(Function<RedisCommands<String, String>, T> function) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisCommands<String, String> sync = connection.sync();
            return function.apply(sync);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private <T> T byteArrayCmd(Function<RedisCommands<byte[], byte[]>, T> function) {
        try (StatefulRedisConnection<byte[], byte[]> connection = redisClient.connect(new ByteArrayCodec())) {
            RedisCommands<byte[], byte[]> sync = connection.sync();
            return function.apply(sync);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

package com.doopp.gutty.redis;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import redis.clients.jedis.JedisPoolConfig;

import javax.inject.Provider;

public abstract class RedisModule extends AbstractModule {

    private boolean bindSerializableHelper = false;

    public RedisModule() {
    }

    protected final void configure() {
        this.initialize();
        bindJdkSerializableHelper();
    }

    protected final void bindJedisPoolConfigProvider(Class<? extends Provider<JedisPoolConfig>> jedisPoolConfigProvider) {
        Preconditions.checkNotNull(jedisPoolConfigProvider, "Parameter 'jedisPoolConfigProvider' must be not null");
        this.bind(JedisPoolConfig.class).toProvider(jedisPoolConfigProvider).in(Scopes.SINGLETON);
    }

    protected final void bindSerializableHelper(Class<? extends SerializableHelper> serializableHelperClass) {
        Preconditions.checkNotNull(serializableHelperClass, "Parameter 'serializableHelperClass' must be not null");
        this.bind(SerializableHelper.class).to(serializableHelperClass).in(Scopes.SINGLETON);
        bindSerializableHelper=true;
    }

    private void bindJdkSerializableHelper() {
        if (!bindSerializableHelper) {
            this.bind(SerializableHelper.class).to(JdkSerializableHelper.class).in(Scopes.SINGLETON);
            bindSerializableHelper = true;
        }
    }

    protected abstract void initialize();
}

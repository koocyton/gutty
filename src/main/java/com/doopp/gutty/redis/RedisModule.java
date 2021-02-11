package com.doopp.gutty.redis;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import redis.clients.jedis.JedisPoolConfig;

import javax.inject.Provider;

public abstract class RedisModule extends AbstractModule {

    public RedisModule() {
    }

    protected final void configure() {
        this.initialize();
    }

    protected final void bindJedisPoolConfigProvider(Class<? extends Provider<JedisPoolConfig>> jedisPoolConfigProvider) {
        this.bind(JedisPoolConfig.class).toProvider(jedisPoolConfigProvider).in(Scopes.SINGLETON);
    }

    protected abstract void initialize();
}

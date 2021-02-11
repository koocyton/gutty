package com.doopp.gutty.redis;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import redis.clients.jedis.JedisPoolConfig;

import javax.inject.Provider;

public abstract class RedisModule extends AbstractModule {

    private

    public RedisModule() {
    }

    protected final void configure() {
        this.initialize();
    }

    protected final void bindJedisPoolConfigProvider(Class<? extends Provider<JedisPoolConfig>> jedisPoolConfigProvider) {
        this.bind(JedisPoolConfig.class).toProvider(jedisPoolConfigProvider).in(Scopes.SINGLETON);
    }

    protected final void addShardedJedisHelper(String name) {
        this.bind(JedisPoolConfig.class).annotatedWith(Names.named(name)).toProvider(jedisPoolConfigProvider).in(Scopes.SINGLETON);
        this.bindJedisPoolConfigProvider(Providers.guicify(jedisPoolConfigProvider));
    }

    protected final void bindJedisPoolConfigProvider(com.google.inject.Provider<JedisPoolConfig> jedisPoolConfigProvider) {
        this.bind(JedisPoolConfig.class).toProvider(jedisPoolConfigProvider).in(Scopes.SINGLETON);
    }

    protected abstract void initialize();
}

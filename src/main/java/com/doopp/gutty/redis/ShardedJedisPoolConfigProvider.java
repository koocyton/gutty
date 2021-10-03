package com.doopp.gutty.redis;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class ShardedJedisPoolConfigProvider implements Provider<ShardedJedisPoolConfig> {

    private final ShardedJedisPoolConfig shardedJedisPoolConfig = new ShardedJedisPoolConfig();

    @Inject
    public void setMaxTotal(@Named("redis.pool.maxTotal") final int maxTotal) {
        shardedJedisPoolConfig.setMaxTotal(maxTotal);
    }

    @Inject
    public void setMaxIdle(@Named("redis.pool.maxIdle") final int maxIdle) {
        shardedJedisPoolConfig.setMaxIdle(maxIdle);
    }

    @Inject
    public void setMinIdle(@Named("redis.pool.minIdle") final int minIdle) {
        shardedJedisPoolConfig.setMinIdle(minIdle);
    }

    @Inject
    public void setMaxWaitMillis(@Named("redis.pool.maxWaitMillis") final int maxWaitMillis) {
        shardedJedisPoolConfig.setMaxWaitMillis(maxWaitMillis);
    }

    @Inject
    public void setLifo(@Named("redis.pool.lifo") final boolean lifo) {
        shardedJedisPoolConfig.setLifo(lifo);
    }

    @Inject
    public void setTestOnBorrow(@Named("redis.pool.testOnBorrow") final boolean testOnBorrow) {
        shardedJedisPoolConfig.setTestOnBorrow(testOnBorrow);
    }

    @Override
    public ShardedJedisPoolConfig get() {
        return shardedJedisPoolConfig;
    }
}

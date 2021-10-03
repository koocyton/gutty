package com.doopp.gutty.redis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.ShardedJedis;

import java.time.Duration;

public class ShardedJedisPoolConfig extends GenericObjectPoolConfig<ShardedJedis> {
    public ShardedJedisPoolConfig() {
        this.setTestWhileIdle(true);
        this.setMinEvictableIdleTime(Duration.ofMillis(60000L));
        this.setTimeBetweenEvictionRuns(Duration.ofMillis(30000L));
        this.setNumTestsPerEvictionRun(-1);
    }
}

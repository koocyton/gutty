package com.doopp.gutty.demo;

import com.doopp.gutty.db.HikariDataSourceProvider;
import com.doopp.gutty.demo.filter.ApiFilter;
import com.doopp.gutty.Gutty;
import com.doopp.gutty.json.JacksonMessageConverter;
import com.doopp.gutty.redis.RedisModule;
import com.doopp.gutty.redis.ShardedJedisHelper;
import com.doopp.gutty.view.FreemarkerViewResolver;
import com.github.pagehelper.PageInterceptor;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.mybatis.guice.MyBatisModule;
import org.mybatis.guice.datasource.helper.JdbcHelper;
import org.mybatis.guice.datasource.hikaricp.HikariCPProvider;
import redis.clients.jedis.JedisPoolConfig;

public class MVCApplication {

    public static void main(String[] args) {
        new Gutty()
                .loadProperties(args)
                // .addModules(new AbstractModule() {
                //    @Singleton
                //    @Provides
                //    @Named("executeGroup")
                //    public EventLoopGroup executeGroup() {
                //        return new NioEventLoopGroup();
                //    }
                // })
                .setBasePackages(MVCApplication.class.getPackage().getName())
                .setMessageConverter(JacksonMessageConverter.class)
                .setViewResolver(FreemarkerViewResolver.class)
                .addFilter(ApiFilter.class)
                .addMyBatisModule(HikariCPProvider.class, "com.doopp.gutty.demo.dao", PageInterceptor.class)
                .addModules(new RedisModule() {
                    @Singleton
                    @Provides
                    public ShardedJedisHelper userRedis(JedisPoolConfig jedisPoolConfig, @Named("redis.user.servers") String userServers) {
                        return new ShardedJedisHelper(userServers, jedisPoolConfig);
                    }
                 })
                .start();
    }
}

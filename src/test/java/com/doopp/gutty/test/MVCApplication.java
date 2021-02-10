package com.doopp.gutty.test;

import com.doopp.gutty.test.filter.ApiFilter;
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
                .setBasePackages("com.doopp.gutty.test")
                .setMessageConverter(JacksonMessageConverter.class)
                .setViewResolver(FreemarkerViewResolver.class)
                .addFilter("/api", ApiFilter.class)
                // .setMyBatis(HikariCPProvider.class, "com.doopp.gutty.test.dao", PageInterceptor.class)
                .addModules(
                        new RedisModule() {
                            @Singleton
                            @Provides
                            public ShardedJedisHelper userRedis(JedisPoolConfig jedisPoolConfig, @Named("redis.user.servers") String userServers) {
                                return new ShardedJedisHelper(userServers, jedisPoolConfig);
                            }
                        },
                        new MyBatisModule() {
                            @Override
                            protected void initialize() {
                                bindDataSourceProviderType(HikariCPProvider.class);
                                bindTransactionFactoryType(JdbcTransactionFactory.class);
                                addMapperClasses("com.doopp.gutty.test.dao");
                                addInterceptorClass(PageInterceptor.class);
                            }
                        }
                )
                .start();
    }
}

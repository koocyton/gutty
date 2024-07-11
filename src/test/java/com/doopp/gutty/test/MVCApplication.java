package com.doopp.gutty.test;

import com.doopp.gutty.redis.*;
import com.doopp.gutty.test.filter.ApiFilter;
import com.doopp.gutty.Gutty;
import com.doopp.gutty.json.JacksonMessageConverter;
import com.doopp.gutty.view.FreemarkerViewResolver;
import com.github.pagehelper.PageInterceptor;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.Test;
import org.mybatis.guice.MyBatisModule;
import org.mybatis.guice.datasource.hikaricp.HikariCPProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MVCApplication {


    @Test
    public void testEventLoop() {
        NioEventLoopGroup eventLoop = new NioEventLoopGroup(8);
        AtomicInteger counter = new AtomicInteger(0);
        Long beginTime = System.currentTimeMillis();
        while(true) {
            counter.getAndIncrement();
            if (counter.get()>1000) {
                break;
            }
            final int mm = counter.get();
            eventLoop.next().execute(() -> {
                try {
                    Thread.sleep(2);
                    if (mm > 990) {
                        System.out.println(String.format("testThread %s", mm));
                    }
                }
                catch(Exception e) {
                    System.out.println("zz");
                };
            });
        }
        Long endTime = System.currentTimeMillis();
        System.out.println(String.format("%s - %s  = %s", endTime, beginTime, endTime - beginTime));
    }

    @Test
    public void testThread() {
        Long beginTime = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        for (int ii = 0; ii < 1000; ii++) {
            final int mm = ii;
            executorService.execute(() -> {
                try {
                    Thread.sleep(20000);
                    if (mm > 99) {
                        System.out.println(String.format("testThread %s", mm));
                    }
                }
                catch(Exception ignore) {;};
            });
        }
        Long endTime = System.currentTimeMillis();
        System.out.println(String.format("%s - %s  = %s", endTime, beginTime, endTime - beginTime));
    }

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
                .setMyBatis(HikariCPProvider.class, "com.doopp.gutty.test.dao", PageInterceptor.class, null)
                .addModules(
                        new Module() {

                            @Override
                            public void configure(Binder binder) {
                            }

                            @Singleton
                            @Provides
                            @Named("bossEventLoopGroup")
                            public EventLoopGroup bossEventLoopGroup() {
                                return new NioEventLoopGroup();
                            }

                            @Singleton
                            @Provides
                            @Named("workerEventLoopGroup")
                            public EventLoopGroup workerEventLoopGroup() {
                                return new NioEventLoopGroup();
                            }
                        },
                        new RedisModule() {
                            @Override
                            protected void initialize() {
                                bindShardedJedisPoolConfigProvider(ShardedJedisPoolConfigProvider.class);
                                bindSerializableHelper(JdkSerializableHelper.class);
                            }

                            @Singleton
                            @Provides
                            @Named("userRedis")
                            public ShardedJedisHelper userRedis(ShardedJedisPoolConfig jedisConfig, SerializableHelper serializableHelper, @Named("redis.user.servers") String userServers) {
                                return new ShardedJedisHelper(userServers, jedisConfig, serializableHelper);
                            }

                            @Singleton
                            @Provides
                            @Named("testRedis")
                            public ShardedJedisHelper testRedis(ShardedJedisPoolConfig jedisConfig, SerializableHelper serializableHelper, @Named("redis.test.servers") String userServers) {
                                return new ShardedJedisHelper(userServers, jedisConfig, serializableHelper);
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

package com.doopp.aloha.demo;

import com.doopp.aloha.framework.Gutty;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class MVCApplication {

    public static void main(String[] args) {
        new Gutty()
                .loadProperties(args)
//                .addModules(new AbstractModule() {
//                    @Singleton
//                    @Provides
//                    @Named("executeGroup")
//                    public EventLoopGroup executeGroup() {
//                        return new NioEventLoopGroup();
//                    }
//                })
                .basePackages(MVCApplication.class.getPackage().getName())
                .start();
    }
}

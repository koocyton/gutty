package com.doopp.gutty.demo;

import com.doopp.gutty.framework.Gutty;
import com.doopp.gutty.framework.json.JacksonHttpMessageConverter;

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
                .httpMessageConverter(JacksonHttpMessageConverter.class)
                .start();
    }
}

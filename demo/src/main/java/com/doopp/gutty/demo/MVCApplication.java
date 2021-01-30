package com.doopp.gutty.demo;

import com.doopp.gutty.demo.filter.ApiFilter;
import com.doopp.gutty.Gutty;
import com.doopp.gutty.json.JacksonMessageConverter;
import com.doopp.gutty.view.FreemarkerViewResolver;

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
                .start();
    }
}

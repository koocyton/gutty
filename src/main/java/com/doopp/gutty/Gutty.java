package com.doopp.gutty;

import com.doopp.gutty.annotation.Controller;
import com.doopp.gutty.annotation.Service;
import com.doopp.gutty.annotation.WebSocket;
import com.doopp.gutty.db.LogImplConfigurationSetting;
import com.doopp.gutty.filter.Filter;
import com.doopp.gutty.json.MessageConverter;
import com.doopp.gutty.view.ViewResolver;
import com.google.inject.Module;
import com.google.inject.*;
import com.google.inject.name.Names;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.mybatis.guice.MyBatisModule;

import javax.inject.Provider;
import javax.sql.DataSource;
import javax.ws.rs.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Consumer;

public class Gutty {

    // modules 的集合
    private final List<Module> modules = new ArrayList<>();

    // 项目的包路径
    private final List<String> scanPackages = new ArrayList<>();

    // 组件类 map
    private final Map<Class<?>, Class<?>> componentClassMap = new HashMap<>();

    // filer  uri=>filter map
    private final Map<String, Class<? extends Filter>> filterMap = new HashMap<>();

    // 载入配置
    public Gutty loadProperties(String... propertiesFiles) {
        Properties properties = new Properties();
        for (String propertiesFile : propertiesFiles) {
            try {
                properties.load(new FileInputStream(propertiesFile));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        modules.add(binder -> Names.bindProperties(binder, properties));
        return this;
    }

    // 添加 module 的接口
    public Gutty addModules(Module... modules) {
        Collections.addAll(this.modules, modules);
        return this;
    }

    public Gutty setMyBatis(Class<? extends Provider<DataSource>> dataSourceProviderClazz, String daoPackageName, Class<? extends Interceptor> interceptorsClass, Class<? extends Log> logImpl) {
        modules.add(new MyBatisModule() {
            @Override
            protected void initialize() {
                bindDataSourceProviderType(dataSourceProviderClazz);
                bindTransactionFactoryType(JdbcTransactionFactory.class);
                addMapperClasses(daoPackageName);
                addInterceptorClass(interceptorsClass);
                mapUnderscoreToCamelCase(true);
                if (logImpl!=null) {
                    bindConfigurationSetting(new LogImplConfigurationSetting(logImpl));
                }
            }
        });
        return this;
    }

    // 添加需要扫描的 package 的接口
    public Gutty setScanPackages(String... basePackages) {
        Collections.addAll(this.scanPackages, basePackages);
        return this;
    }

    // Json 处理类
    public Gutty setMessageConverter(Class<? extends MessageConverter> clazz) {
        if (clazz!=null) {
            componentClassMap.put(clazz, MessageConverter.class);
        }
        return this;
    }

    // 模板 处理类
    public Gutty setViewResolver(Class<? extends ViewResolver> clazz) {
        if (clazz!=null) {
            componentClassMap.put(clazz, ViewResolver.class);
        }
        return this;
    }

    // 启动服务
    public void start() {
        // 获取包下的所有类
        componentClassScan();
        // 将有注释的类添加到注入服务里
        componentClassBind();
        // 将类加入到路由中
        controller2Route();
        // launch netty
        startNetty(Guice.createInjector(modules));
    }

    private final List<Consumer<Injector>> injectorConsumerList = new ArrayList<>();

    // 创建 injector 后执行
    public Gutty addInjectorConsumer(Consumer<Injector> injectorConsumer) {
        injectorConsumerList.add(injectorConsumer);
        return this;
    }

    // 启动 netty
    private void startNetty(Injector injector) {
        // 启动 netty
        Netty netty = injector.getInstance(Netty.class);
        // 创建 injector 后执行
        if (injectorConsumerList.size()>0) {
            injectorConsumerList.forEach(injectorConsumer -> injectorConsumer.accept(injector));
        }
        // run netty
        netty.run();
    }

    private static FileSystem jarFileSystem;


    public static <T> T getInstance(Injector injector, Class<T> clazz) {
        return injector.getInstance(clazz);
    }
}

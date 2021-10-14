package com.doopp.gutty;

import com.doopp.gutty.annotation.websocket.Socket;
import com.doopp.gutty.annotation.Controller;
import com.doopp.gutty.annotation.Service;
import com.doopp.gutty.db.LogImplConfigurationSetting;
import com.doopp.gutty.filter.Filter;
import com.doopp.gutty.json.MessageConverter;
import com.doopp.gutty.view.ViewResolver;
import com.google.inject.*;
import com.google.inject.Module;
import com.google.inject.binder.AnnotatedBindingBuilder;
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
import java.nio.file.*;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Consumer;

public class Gutty {

    // modules 的集合
    private final List<Module> modules = new ArrayList<>();

    // 项目的包路径
    private final List<String> basePackages = new ArrayList<>();

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
        // httpHost httpPort && httpsPort
        String httpHost = properties.getProperty("gutty.httpHost");
        String httpPort = properties.getProperty("gutty.httpPort");
        String httpsPort = properties.getProperty("gutty.httpsPort");
        // set to properties
        properties.setProperty("gutty.httpHost",  httpHost==null  ? "127.0.0.1" : httpHost);
        properties.setProperty("gutty.httpPort",  httpPort==null  ? "8080"      : httpPort);
        properties.setProperty("gutty.httpsPort", httpsPort==null ? "8081"      : httpsPort);
        modules.add(binder -> Names.bindProperties(binder, properties));
        // return
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
    public Gutty setBasePackages(String... basePackages) {
        Collections.addAll(this.basePackages, basePackages);
        return this;
    }

    // Json 处理类
    public Gutty setMessageConverter(Class<? extends MessageConverter> clazz) {
        if (clazz!=null) {
            componentClassMap.put(MessageConverter.class, clazz);
        }
        return this;
    }

    // 模板 处理类
    public Gutty setViewResolver(Class<? extends ViewResolver> clazz) {
        if (clazz!=null) {
            componentClassMap.put(ViewResolver.class, clazz);
        }
        return this;
    }

    // 模板 处理类
    public Gutty addFilter(String startUri, Class<? extends Filter> clazz) {
        filterMap.put(startUri, clazz);
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

    // 将 Controller 类加入到路由中
    private void controller2Route() {
        // route map
        Dispatcher dispatcher = Dispatcher.getInstance();
        // loop
        for(Class<?> clazz : componentClassMap.values()) {
            // 只分析 Controller
            if (clazz.isAnnotationPresent(Controller.class)) {
                // controller path
                javax.ws.rs.Path controllerPath = clazz.getAnnotation(javax.ws.rs.Path.class);
                // 类上标注的 PATH
                String controllerPathValue = "";
                // 如果有值
                if (controllerPath != null && controllerPath.value().length() > 0) {
                    controllerPathValue = controllerPath.value();
                }
                // 查询 class.method
                for (Method method : clazz.getMethods()) {
                    // method path
                    javax.ws.rs.Path methodPath = method.getAnnotation(javax.ws.rs.Path.class);
                    if (methodPath==null) {
                        continue;
                    }
                    // 类上标注的 PATH
                    String methodPathValue = (methodPath.value().length() > 0) ? methodPath.value() : "";
                    // GET PUT POST DELETE (httpMethod)
                    Class<? extends Annotation> httpMethodAnnotation = GET.class;
                    if (method.isAnnotationPresent(POST.class)) {
                        httpMethodAnnotation = POST.class;
                    }
                    else if (method.getAnnotation(DELETE.class)!=null) {
                        httpMethodAnnotation = DELETE.class;
                    }
                    else if (method.getAnnotation(PUT.class)!=null) {
                        httpMethodAnnotation = PUT.class;
                    }
                    else if (method.getAnnotation(OPTIONS.class)!=null) {
                        httpMethodAnnotation = OPTIONS.class;
                    }
                    dispatcher.addHttpRoute(httpMethodAnnotation, controllerPathValue + methodPathValue, clazz, method, method.getParameters());
                }
            }
            else if (clazz.isAnnotationPresent(Socket.class)) {
                // socket path
                javax.ws.rs.Path pathAnnotation = clazz.getAnnotation(javax.ws.rs.Path.class);
                // 如果没有值
                if (pathAnnotation == null || pathAnnotation.value().length()<1) {
                    continue;
                }
                dispatcher.addSocketRoute(pathAnnotation.value(), clazz);
            }
        }
    }

    // 将组件类 bind 到 Guice 框架
    private void componentClassBind() {
        modules.add(new Module() {
            @Override
            public void configure(Binder binder) {
                for(Class<?> bindClass : componentClassMap.keySet()) {
                    this.bind(binder, bindClass, componentClassMap.get(bindClass));
                }
            }
            // bind class
            private <T> void bind(Binder binder, Class<T> bindClass, Class<?> toClass) {
                // binder
                Class<? extends T> _toClass = (Class<? extends T>) toClass;
                Service serviceAnnotation = toClass.getAnnotation(Service.class);
                // 如果 service 有设定名称
                if (serviceAnnotation!=null && !serviceAnnotation.value().equals("")) {
                    binder.bind(bindClass).annotatedWith(Names.named(serviceAnnotation.value())).to(_toClass).in(Scopes.SINGLETON);
                }
                // 如果 bindClass 不等 toClass
                else if (!bindClass.equals(toClass)) {
                    binder.bind(bindClass).to(_toClass).in(Scopes.SINGLETON);
                }
                else {
                    // 单例
                    binder.bind(bindClass).in(Scopes.SINGLETON);
                }
            }
            @Provides
            @Singleton
            public Map<String, Class<? extends  Filter>> filterMap() {
                return filterMap;
            }
        });
    }

    // 将 Package 里扫描类
    private void componentClassScan() {
        // loop basePackages
        for(String basePackage : basePackages) {
            // package Path
            Path packagePath = packagePath(basePackage);
            // read
            try {
                Files.walkFileTree(packagePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        String classPath = file.toUri().toString().replace("/", ".");
                        if (classPath.endsWith("$1.class")) {
                            return FileVisitResult.CONTINUE;
                        }
                        String className = classPath.substring(classPath.lastIndexOf(basePackage), classPath.length()-6);
                        try {
                            Class<?> clazz = Class.forName(className);
                            if (clazz.isAnnotationPresent(Service.class) || clazz.isAnnotationPresent(Controller.class) || clazz.isAnnotationPresent(Socket.class)) {
                                if (clazz.getInterfaces().length>=1) {
                                    componentClassMap.put(clazz.getInterfaces()[0], clazz);
                                }
                                else {
                                    componentClassMap.put(clazz, clazz);
                                }
                            }
                        }
                        catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static FileSystem jarFileSystem;

    // 获取资源
    private Path packagePath(String basePackage) {
        URL resourceURL = Gutty.class.getResource("/" + basePackage.replace(".", "/"));
        if (resourceURL.getProtocol().equals("jar")) {
            String[] jarPathInfo = resourceURL.getPath().split("!");
            if (jarPathInfo[0].startsWith("file:")) {
                jarPathInfo[0] = java.io.File.separator.equals("\\") ? jarPathInfo[0].substring(6) : jarPathInfo[0].substring(5);
            }
            try {
                if (jarFileSystem==null) {
                    jarFileSystem = FileSystems.newFileSystem(Paths.get(jarPathInfo[0]), null);
                }
                return jarFileSystem.getPath(jarPathInfo[1]);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }
        return (java.io.File.separator.equals("\\") && resourceURL.getPath().startsWith("/"))
                ? Paths.get(resourceURL.getPath().substring(1))
                : Paths.get(resourceURL.getPath());
    }

    public static <T> T getInstance(Injector injector, Class<T> clazz) {
        try {
            return injector.getInstance(clazz);
        }
        catch (Exception e) {
            return null;
        }
    }
}

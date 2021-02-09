package com.doopp.gutty;

import com.doopp.gutty.annotation.websocket.Socket;
import com.doopp.gutty.annotation.Controller;
import com.doopp.gutty.annotation.Service;
import com.doopp.gutty.filter.Filter;
import com.doopp.gutty.filter.FilterHandler;
import com.doopp.gutty.json.MessageConverter;
import com.doopp.gutty.view.ViewResolver;
import com.google.inject.*;
import com.google.inject.Module;
import com.google.inject.name.Names;
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

    private final List<Module> modules = new ArrayList<>();

    private final List<String> basePackages = new ArrayList<>();

    private final Map<Class<?>, Class<?>> modulesBindClassMap = new HashMap<>();

    private final Map<String, Class<? extends Filter>> uriFilters = new HashMap<>();

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

    public Gutty addMyBatisModule(Class<? extends Provider<DataSource>> dataSourceProviderClazz, String daoPackageName, Class<? extends Interceptor> interceptorsClass) {
        modules.add(new MyBatisModule() {
            @Override
            protected void initialize() {
                bindDataSourceProviderType(dataSourceProviderClazz);
                bindTransactionFactoryType(JdbcTransactionFactory.class);
                addMapperClasses(daoPackageName);
                addInterceptorClass(interceptorsClass);
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
            modulesBindClassMap.put(MessageConverter.class, clazz);
        }
        return this;
    }

    // 模板 处理类
    public Gutty setViewResolver(Class<? extends ViewResolver> clazz) {
        if (clazz!=null) {
            modulesBindClassMap.put(ViewResolver.class, clazz);
        }
        return this;
    }

    // 模板 处理类
    public Gutty addFilter(String startUri, Class<? extends Filter> clazz) {
        uriFilters.put(startUri, clazz);
        return this;
    }

    // 启动服务
    public void start() {
        modules.add(new Module() {
            @Override
            public void configure(Binder binder) {
                modulesBindClassMap.forEach((c1, c2)->{
                    bind(binder, c1, c2);
                });
            }
            private <T> void bind(Binder binder, Class<T> interfaceClazz, Class<?> clazz) {
                if (!Arrays.asList(clazz.getInterfaces()).contains(interfaceClazz)) {
                    return;
                }
                Class<? extends T> clazzT = (Class<? extends T>) clazz;
                binder.bind(interfaceClazz).to(clazzT).in(Scopes.SINGLETON);
            }
            @Provides
            @Singleton
            public FilterHandler filterHandler() {
                return new FilterHandler(uriFilters);
            }
        });
        // 获取包下的所有类
        List<Class<?>> classList = classList();
        // 将有注释的类添加到注入服务里
        annotationClass2Injector(classList);
        // 将类加入到路由中
        class2Route(classList);
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
        // set filters
        netty.setFilters(uriFilters);
        // 创建 injector 后执行
        if (injectorConsumerList.size()>0) {
            injectorConsumerList.forEach(injectorConsumer -> injectorConsumer.accept(injector));
        }
        // run netty
        netty.run();
    }

    // 将 Controller 类加入到路由中
    private void class2Route(List<Class<?>> classList) {
        // route map
        Dispatcher dispatcher = Dispatcher.getInstance();
        // loop
        for(Class<?> clazz : classList) {
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

    // 扫描 @Service 和 @Controller  @Socket
    // 将他们加入到 Module 里给 injector 调用
    private void annotationClass2Injector(List<Class<?>> classList) {
        modules.add(new Module() {
            @Override
            public void configure(Binder binder) {
                for(Class<?> clazz : classList) {
                    if (clazz.isAnnotationPresent(Service.class) || clazz.isAnnotationPresent(Controller.class) || clazz.isAnnotationPresent(Socket.class)) {
                        if (clazz.getInterfaces().length>=1) {
                            for (Class<?> anInterface : clazz.getInterfaces()) {
                                bind(binder, anInterface, clazz);
                            }
                        }
                        else {
                            binder.bind(clazz).in(Scopes.SINGLETON);
                        }
                    }
                }
            }
            // bind class to interface
            private <T> void bind(Binder binder, Class<T> interfaceClazz, Class<?> clazz) {
                if (!Arrays.asList(clazz.getInterfaces()).contains(interfaceClazz)) {
                    return;
                }
                Class<? extends T> clazzT = (Class<? extends T>) clazz;
                Service serviceAnnotation = clazzT.getAnnotation(Service.class);
                if (serviceAnnotation!=null && !serviceAnnotation.value().equals("")) {
                    binder.bind(interfaceClazz)
                            .annotatedWith(Names.named(serviceAnnotation.value()))
                            .to(clazzT)
                            .in(Scopes.SINGLETON);
                }
                else {
                    binder.bind(interfaceClazz).to(clazzT).in(Scopes.SINGLETON);
                }
            }
        });
    }

    // 将 Package 里扫描类
    private List<Class<?>> classList() {
        // init className List
        List<Class<?>> classList = new ArrayList<>();
        // loop basePackages
        for(String basePackage : basePackages) {
            // package Path
            Path packagePath = packagePath(basePackage);
            // read
            try {
                Files.walkFileTree(packagePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        // logger.info("file >>> {}", file.toUri());
                        String classPath = file.toUri().toString().replace("/", ".");
                        if (!classPath.endsWith("$1.class")) {
                            String className = classPath.substring(classPath.lastIndexOf(basePackage), classPath.length()-6);
                            // add class to class list
                            try {
                                classList.add(Class.forName(className));
                            }
                            catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        //
        return classList;
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

    public static <T> boolean hasInstance(Injector injector, Class<T> clazz) {
        try {
            injector.getInstance(clazz);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
}

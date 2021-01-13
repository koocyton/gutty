package com.doopp.aloha.framework;

import com.doopp.aloha.framework.annotation.Controller;
import com.doopp.aloha.framework.annotation.Service;
import com.google.inject.*;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class Gutty {

    private final static Logger logger = LoggerFactory.getLogger(Gutty.class);

    private final List<Module> modules = new ArrayList<>();

    private final List<String> basePackages = new ArrayList<>();

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

    // 添加需要扫描的 package 的接口
    public Gutty basePackages(String... basePackages) {
        Collections.addAll(this.basePackages, basePackages);
        return this;
    }

    // 启动服务
    public void start() {
        // scan packages && add project service
        scanService2Module();
        // load module
        Injector injector = Guice.createInjector(modules);
        // launch netty
        Netty netty = injector.getInstance(Netty.class);
        netty.run();
    }

    // 扫描 @Service 和 @Controller
    // 将他们加入到 Module 里给 injector 调用
    private void scanService2Module() {
        List<Class<?>> classList = scanClasses();
        modules.add(new AbstractModule() {
            @Override
            protected void configure() {
                for(Class<?> clazz : classList) {
                    if (clazz.isAnnotationPresent(Service.class) || clazz.isAnnotationPresent(Controller.class)) {
                        if (clazz.getInterfaces().length>=1) {
                            binder(clazz.getInterfaces()[0], clazz);
                        }
                        else {
                            binder(clazz);
                        }
                    }
                }
            }
            // bind class to interface
            private <T> void binder(Class<T> interfaceClazz, Class<?> clazz) {
                Class<T> clazzT = (Class<T>) clazz;
                bind(interfaceClazz).to(clazzT).in(Scopes.SINGLETON);
                // .annotatedWith(Names.named("JDBC URL"))
            }
            // bind class
            private <T> void binder(Class<T> clazz) {
                bind(clazz).in(Scopes.SINGLETON);
            }
        });
    }

    // 将 Package 里扫描类
    private List<Class<?>> scanClasses() {
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
                        String classPath = file.toUri().getPath().replace("/", ".");
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

    // 获取资源
    private Path packagePath(String basePackage) {
        URL resourceURL = Gutty.class.getResource("/" + basePackage.replace(".", "/"));
        // if (resourceURL.getProtocol().equals("jar")) {
        //     String[] jarPathInfo = resourceURL.getPath().split("!");
        //     if (jarPathInfo[0].startsWith("file:")) {
        //         jarPathInfo[0] = java.io.File.separator.equals("\\") ? jarPathInfo[0].substring(6) : jarPathInfo[0].substring(5);
        //     }
        //     if (jarPathFS.get(jarPathInfo[0])==null || !jarPathFS.get(jarPathInfo[0]).isOpen()) {
        //         java.nio.file.Path jarPath = Paths.get(jarPathInfo[0]);
        //         jarPathFS.put(jarPathInfo[0], FileSystems.newFileSystem(jarPath, null));
        //     }
        //     return jarPathFS.get(jarPathInfo[0]).getPath(jarPathInfo[1]);
        // }
        return Paths.get(resourceURL.getPath());
    }
}

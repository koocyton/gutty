package com.doopp.gutty.util;

import com.doopp.gutty.Dispatcher;
import com.doopp.gutty.Gutty;
import com.doopp.gutty.annotation.Controller;
import com.doopp.gutty.annotation.Service;
import com.doopp.gutty.annotation.WebSocket;
import com.doopp.gutty.filter.Filter;
import com.google.inject.*;
import com.google.inject.Module;
import com.google.inject.name.Names;

import javax.ws.rs.*;
import javax.ws.rs.Path;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

public class GuttyUtil {

    /**
     * 遍历组件，将 Controller 和 websocket 载入到路由里
     * @param componentClassMap 组件 Map
     */
    private Dispatcher setDispatcher(Map<Class<?>, Class<?>> componentClassMap) {
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
            else if (clazz.isAnnotationPresent(WebSocket.class)) {
                // socket path
                javax.ws.rs.Path pathAnnotation = clazz.getAnnotation(javax.ws.rs.Path.class);
                // 如果没有值
                if (pathAnnotation == null || pathAnnotation.value().length()<1) {
                    continue;
                }
                dispatcher.addSocketRoute(pathAnnotation.value(), clazz);
            }
        }
        return dispatcher;
    }

    // 将组件类 bind 到 Guice 框架
    private void componentClassBind(List<Module> modules, Map<Class<?>, Class<?>> componentClassMap) {
        modules.add(new Module() {
            @Override
            public void configure(Binder binder) {
                for(Class<?> bindClass : componentClassMap.keySet()) {
                    this.bind(binder, bindClass, componentClassMap.get(bindClass));
                }
            }
            // bind class
            private <T> void bind(Binder binder, Class<?> toClass, Class<T> bindClass) {
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
            public Map<String, Class<? extends Filter>> filterMap() {
                return filterMap;
            }
        });
    }

    // 将 Package 里扫描类
    private void componentClassScan(List<String> scanPackages, Map<Class<?>, Class<?>> componentClassMap) {
        Map<Class<?>, Map<Class<?>, Class<?>>>
        // loop basePackages
        for(String scanPackage : scanPackages) {
            // package Path
            java.nio.file.Path packagePath = packagePath(scanPackage);
            // read
            try {
                Files.walkFileTree(packagePath, new SimpleFileVisitor<java.nio.file.Path>() {
                    @Override
                    public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs) {
                        String classPath = file.toUri().toString().replace("/", ".");
                        if (classPath.endsWith("$1.class")) {
                            return FileVisitResult.CONTINUE;
                        }
                        String className = classPath.substring(classPath.lastIndexOf(scanPackage), classPath.length()-6);
                        try {
                            Class<?> clazz = Class.forName(className);
                            if (clazz.isAnnotationPresent(Service.class) || clazz.isAnnotationPresent(Controller.class) || clazz.isAnnotationPresent(WebSocket.class)) {
                                if (clazz.getInterfaces().length>=1) {
                                    componentClassMap.put(clazz, clazz.getInterfaces()[0]);
                                }
                                else {
                                    componentClassMap.put(clazz, clazz);
                                }
                            }
                        }
                        catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
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
    private java.nio.file.Path packagePath(String basePackage) {
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
}

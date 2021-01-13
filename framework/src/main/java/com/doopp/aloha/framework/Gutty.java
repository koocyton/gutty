package com.doopp.aloha.framework;

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

    public Gutty addModules(Module... modules) {
        Collections.addAll(this.modules, modules);
        return this;
    }

    public Gutty basePackages(String... basePackages) {
        Collections.addAll(this.basePackages, basePackages);
        return this;
    }

    public void start() {
        // scan packages && add project service
        scanPackages();
        // load module
        Injector injector = Guice.createInjector(modules);
        // launch netty
        Netty netty = injector.getInstance(Netty.class);
        netty.run();
    }

    private void scanPackages() {
        for(String basePackage : basePackages) {
            try {
                // URL resource = this.getClass().getResource("/" + basePackage.replace(".", "/"));
                // System.out.println("resource.toString() : " + resource.toString());
                // System.out.println("resource.toURI() : " + resource.toURI());
                // System.out.println("resource.getFile() : " + resource.getFile());
                // System.out.println("resource.getPath() : " + resource.getPath());
                Path resourcePath = classResourcePath("/" + basePackage.replace(".", "/")).block();
                if (resourcePath==null) {
                    continue;
                }
                // System.out.println(resourcePath);
                Files.walkFileTree(resourcePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        String uriPath = file.toUri().toString();
                        // System.out.println("file.toUri() : " + file.toUri());
                        // System.out.println("file.toRealPath() : " + file.toRealPath());
                        // System.out.println("file.toAbsolutePath() : " + file.toAbsolutePath());
                        if (uriPath.endsWith(".class")) {
                            int startIndexOf = uriPath.indexOf(basePackage.replace(".", "/"));
                            int endIndexOf = uriPath.indexOf(".class");
                            if (startIndexOf<0) {
                                return FileVisitResult.CONTINUE;
                            }
                            String classPath = uriPath.substring(startIndexOf, endIndexOf);
                            String className = classPath.replace("/", ".");
                            logger.info();
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

//    public static Mono<java.nio.file.Path> classResourcePath(String resourceUri) {
//        URL resource = ReactorGuiceServer.class.getResource(resourceUri);
//        return Mono.fromCallable(()->{
//            if (resource.getProtocol().equals("jar")) {
//                String[] jarPathInfo = resource.getPath().split("!");
//                if (jarPathInfo[0].startsWith("file:")) {
//                    jarPathInfo[0] = java.io.File.separator.equals("\\")
//                            ? jarPathInfo[0].substring(6)
//                            : jarPathInfo[0].substring(5);
//                }
//                if (jarPathFS.get(jarPathInfo[0])==null || !jarPathFS.get(jarPathInfo[0]).isOpen()) {
//                    java.nio.file.Path jarPath = Paths.get(jarPathInfo[0]);
//                    jarPathFS.put(jarPathInfo[0], FileSystems.newFileSystem(jarPath, null));
//                }
//                return jarPathFS.get(jarPathInfo[0]).getPath(jarPathInfo[1]);
//            }
//            return Paths.get(resource.toURI());
//        })
//                .subscribeOn(Schedulers.boundedElastic())
//                .onErrorResume(t->Mono.error(new StatusMessageException(404, "Not Found")));
//    }
}

package com.doopp.gutty;

import com.doopp.gutty.annotation.Service;
import com.google.inject.*;
import com.google.inject.Module;

import java.util.Set;

public class InjectHelper {

    private Injector injector;

    private InjectHelper() {

    }

    public InjectHelper create(Set<Module> moduleSet) {
        this.injector = Guice.createInjector(moduleSet);
        return this;
    }

    public void scanInjectClass() {

    }

    class AutoImportModule extends AbstractModule {

        @Override
        public void configure() {
            for(String className : ReactorGuiceServer.classNames) {
                this.binder(className);
            }
        }

        private <T> void binder(String className) {
            try {
                Class<T> clazz = (Class<T>) Class.forName(className);
                if (clazz.isAnnotationPresent(Service.class) && clazz.getInterfaces().length>=1) {
                    Class<T> serviceInterface = (Class<T>) clazz.getInterfaces()[0];
                    bind(serviceInterface).to(clazz).in(Scopes.SINGLETON);
                }
            }
            catch(Exception ignored) {}
        }
    }
}



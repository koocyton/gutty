package com.doopp.gutty;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import java.util.Set;

public class ScanUtil {

    private Injector injector;

    private ScanUtil() {

    }

    public ScanUtil create(Set<Module> moduleSet) {
        this.injector = Guice.createInjector(moduleSet);
        return this;
    }

    public void scanInjectClass() {

    }
}

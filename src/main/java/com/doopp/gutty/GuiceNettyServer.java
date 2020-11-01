package com.doopp.gutty;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import java.util.Set;

public class GuiceNettyServer {

    private InjectHelper injectHelper;

    private NettyServer nettyServer;

    private ScanUtil scanUtil;

    private GuiceNettyServer() {

    }

    public GuiceNettyServer create(Set<Module> moduleSet) {

    }
}

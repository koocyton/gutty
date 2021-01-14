package com.doopp.aloha.demo.service.impl;

import com.doopp.aloha.demo.service.HelloService;
import com.doopp.aloha.framework.annotation.Service;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

@Service
public class HelloServiceImpl implements HelloService {

    @Inject
    @Named("executeGroup")
    private EventLoopGroup executeGroup;

    @Override
    public String hello() {
        Future<String> h = executeGroup.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "hello world !";
            }
        });
        try {
            return h.get();
        }
        catch (InterruptedException | ExecutionException e) {

            return "hello liuyi";
        }
    }
}

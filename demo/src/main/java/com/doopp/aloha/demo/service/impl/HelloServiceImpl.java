package com.doopp.aloha.demo.service.impl;

import com.doopp.aloha.demo.service.HelloService;
import com.doopp.aloha.framework.annotation.Service;

@Service
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello() {
        return "hello kunlun !";
    }

    //    @Override
    //    public String hello() {
    //        Future<String> h = executeGroup.submit(new Callable<String>() {
    //            @Override
    //            public String call() throws Exception {
    //                return "hello world !";
    //            }
    //        });
    //        try {
    //            return h.get();
    //        }
    //        catch (InterruptedException | ExecutionException e) {
    //
    //            return "hello liuyi";
    //        }
    //    }
}

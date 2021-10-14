package com.doopp.gutty.test.service.impl;

import com.doopp.gutty.test.service.HelloService;
import com.doopp.gutty.annotation.Service;

@Service("helloService")
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello() {
        return "hello hello !";
    }
}

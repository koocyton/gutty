package com.doopp.gutty.test.service.impl;

import com.doopp.gutty.annotation.Service;
import com.doopp.gutty.test.service.HelloService;

@Service("testService")
public class TestServiceImpl implements HelloService {

    @Override
    public String hello() {
        return "hello test !";
    }
}

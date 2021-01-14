package com.doopp.aloha.demo.controller;

import com.doopp.aloha.demo.service.HelloService;
import com.doopp.aloha.framework.annotation.Controller;
import com.google.inject.Inject;
import io.netty.channel.EventLoopGroup;

import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/api")
@Controller
public class HelloController {

    @Inject
    private HelloService helloService;

    @GET
    @Path("/hello")
    public String hello() {
        return helloService.hello();
    }

    @GET
    @Path("/hello2")
    public String hello2() {
        return "hello2";
    }
}

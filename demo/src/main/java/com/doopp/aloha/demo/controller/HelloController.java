package com.doopp.aloha.demo.controller;

import com.doopp.aloha.demo.service.HelloService;
import com.doopp.aloha.framework.annotation.Controller;
import com.google.inject.Inject;
import io.netty.channel.EventLoopGroup;

import javax.inject.Named;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/api")
@Controller
public class HelloController {

    @Inject
    @Named("executeGroup")
    private EventLoopGroup executeGroup;

    @Inject
    private HelloService helloService;

    @POST
    @Path("/hello")
    public String hello() {
        return "";
    }
}

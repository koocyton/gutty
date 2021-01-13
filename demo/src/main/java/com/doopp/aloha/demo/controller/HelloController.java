package com.doopp.aloha.demo.controller;

import com.doopp.aloha.framework.annotation.Controller;
import com.google.inject.Inject;
import io.netty.channel.EventLoopGroup;

import javax.inject.Named;
import javax.ws.rs.Path;

@Path("")
@Controller
public class HelloController {

    @Inject
    @Named("executeGroup")
    private EventLoopGroup executeGroup;

    @Path("")
    public String a() {
        return null;
    }
}

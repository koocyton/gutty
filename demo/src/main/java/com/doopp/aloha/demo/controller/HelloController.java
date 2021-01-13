package com.doopp.aloha.demo.controller;

import com.doopp.aloha.framework.annotation.Controller;
import com.google.inject.Inject;
import io.netty.channel.EventLoopGroup;

import javax.ws.rs.Path;

@Path("")
@Controller
public class HelloController {

    @Inject
    private EventLoopGroup workerEventLoopGroup;

    @Path("")
    public String a() {
        workerEventLoopGroup.next().submit(new Runnable() {
            @Override
            public void run() {

            }
        });
        return "abc";
    }
}

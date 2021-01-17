package com.doopp.aloha.demo.controller;

import com.doopp.aloha.demo.service.HelloService;
import com.doopp.aloha.framework.annotation.Controller;
import com.google.inject.Inject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.cookie.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import java.util.List;

@Path("/api")
@Controller
public class HelloController {

    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

    @Inject
    private HelloService helloService;

    @GET
    @Path("/hello")
    public String hello(@CookieParam("user") String user) {
        logger.info(user);
        return helloService.hello();
    }

    @POST
    @Path("/hello")
    public String hello2(@FormParam("liu") List<Integer> liu) {
        logger.info("{}", liu);
        return "hello2";
    }
}

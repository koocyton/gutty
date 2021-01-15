package com.doopp.aloha.demo.controller;

import com.doopp.aloha.demo.service.HelloService;
import com.doopp.aloha.framework.annotation.Controller;
import com.google.inject.Inject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/api")
@Controller
public class HelloController {

    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);
    @Inject
    private HelloService helloService;

    @GET
    @Path("/hello")
    public String hello(HttpRequest httpRequest, HttpResponse httpResponse) {
        logger.info("\nhttpRequest {}\nhttpResponse {}", httpRequest, httpResponse);
        return helloService.hello();
    }

    @GET
    @Path("/hello2")
    public String hello2() {
        return "hello2";
    }
}

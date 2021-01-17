package com.doopp.gutty.demo.controller;

import com.doopp.gutty.demo.service.HelloService;
import com.doopp.gutty.framework.annotation.Controller;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import java.util.Arrays;

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
    public String hello2(@FormParam("liu") Integer[] liu) {
        logger.info("{}", Arrays.asList(liu));
        return "hello2";
    }
}

package com.doopp.gutty.demo.controller;

import com.doopp.gutty.demo.pojo.User;
import com.doopp.gutty.demo.service.HelloService;
import com.doopp.gutty.framework.annotation.Controller;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;

@Path("/api")
@Controller
public class HelloController {

    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

    @Inject
    private HelloService helloService;

    @GET
    @Path("/hello")
    @Produces("application/json")
    public String hello(@CookieParam("user") String user) {
        return helloService.hello();
    }

    @GET
    @Path("/hello/{id}/{name}")
    public String hello3(@PathParam("id") Integer id, @PathParam("name") String name) {
        logger.info("id {}", id);
        logger.info("name {}", name);
        return helloService.hello();
    }

    @POST
    @Path("/hello")
    public String hello2(@FormParam("liu") Integer liu) {
        logger.info("liu {}", liu);
        return "hello2";
    }

    @POST
    @Path("/json")
    @Produces("application/json")
    public User hello2(User user) {
        logger.info("user {}", user);
        return user;
    }
}

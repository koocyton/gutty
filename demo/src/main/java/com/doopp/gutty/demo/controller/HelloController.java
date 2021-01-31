package com.doopp.gutty.demo.controller;

import com.doopp.gutty.demo.dao.UserDao;
import com.doopp.gutty.demo.pojo.User;
import com.doopp.gutty.demo.service.HelloService;
import com.doopp.gutty.annotation.Controller;
import com.doopp.gutty.annotation.FileParam;
import com.doopp.gutty.view.ModelMap;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import java.io.File;
import java.util.List;

@Path("/api")
@Controller
public class HelloController {

    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

    @Inject
    private HelloService helloService;

    @Inject
    private UserDao userDao;

    @GET
    @Path("/template")
    public String template(ModelMap modelMap) {
        modelMap.addAttribute("hello", "hello freemarker !");
        return "hello.template";
    }

    @GET
    @Path("/hello")
    @Produces("application/json")
    public String hello(@CookieParam("user") String user) {
        return helloService.hello();
    }

    @GET
    @Path("/users")
    @Produces("application/json")
    public List<User> users() {
        return userDao.selectAll();
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
    @Produces("application/json")
    public User hello2(@FormParam("liu") String liu) {
        logger.info("liu {}", liu);
        User user = new User();
        user.setName(liu);
        return user;
    }

    @POST
    @Path("/json")
    @Produces("application/json")
    public User hello2(User user) {
        logger.info("user {}", user);
        return user;
    }

    @POST
    @Path("/upload")
    @Produces("application/json")
    public String upload(@FileParam(value = "file", path = "d:/tmp") File uploadFile) {
        logger.info("file {}", uploadFile);
        return "hello";
    }
}

package com.doopp.gutty.test.controller;

import com.doopp.gutty.annotation.RequestAttribute;
import com.doopp.gutty.test.dao.UserDao;
import com.doopp.gutty.test.pojo.User;
import com.doopp.gutty.test.service.HelloService;
import com.doopp.gutty.annotation.Controller;
import com.doopp.gutty.annotation.FileParam;
import com.doopp.gutty.redis.ShardedJedisHelper;
import com.doopp.gutty.view.ModelMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;

@Path("/api")
@Controller
public class HelloController {

    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

    @Inject
    private HelloService helloService;

    @Inject
    private UserDao userDao;

    @Inject
    @Named("userRedis")
    private ShardedJedisHelper userRedis;

    @GET
    @Path("/redis/read")
    @Produces("application/json")
    public User readRedis() {
        User user = userRedis.get("user_redis".getBytes(), User.class);
        return user;
    }

    @GET
    @Path("/redis/write")
    @Produces("application/json")
    public User writeRedis() {
        User user = new User();
        user.setId(System.currentTimeMillis());
        userRedis.set("user_redis".getBytes(), user);
        return user;
    }

    @GET
    @Path("/template")
    public String template(ModelMap modelMap) {
        modelMap.addAttribute("hello", "hello freemarker !");
        return "hello.ff";
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
    public List<User> users(@RequestAttribute("hello") String hello) {
        System.out.println(hello);
        return userDao.selectAll();
    }

    @GET
    @Path("/hello/{id}/{name}")
    public String hello3(@PathParam("id") Integer id, @PathParam("name") String name) {
        return helloService.hello();
    }

    @POST
    @Path("/hello")
    @Produces("application/json")
    public User hello2(@FormParam("liu") String liu) {
        logger.info("liu {}", liu);
        User user = new User();
        user.setNickName(liu);
        return user;
    }

    @POST
    @Path("/json")
    @Produces("application/json")
    public List<String> hello2(@QueryParam("id") String id, String[] user) {
        List<String> sl = Arrays.asList(user);
        System.out.println(id);
        return sl;
    }

    @POST
    @Path("/upload")
    @Produces("application/json")
    public String upload(@FileParam(value = "file", path = "d:/tmp") File uploadFile) {
        logger.info("file {}", uploadFile);
        return "hello";
    }
}

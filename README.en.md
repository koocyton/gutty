```
   _____           _     _           
  / ____|         | |   | |          
 | |  __   _   _  | |_  | |_   _   _ 
 | | |_ | | | | | | __| | __| | | | |
 | |__| | | |_| | | |_  | |_  | |_| |
  \_____|  \__,_|  \__|  \__|  \__, |   When Guice meets Netty
                                __/ |
                               |___/
```

#### Introduction
```
Gutty is a fast web server , use Google Guice and Netty to build ! 
```

#### Function list and completion status

  短连接

- [x] Integrating Guice and netty
- [x] Custom add Guice`s model
- [x] Support @Service @Controller @Socket
- [x] Use @Path config route
- [x] Support @Post @Get @Delete @Put
- [x] Support @Product , json or protobuf
- [x] Controller auto inject HttpRequest HttpResponse ...
- [x] Support Json and Protobuf
- [x] file upload , support @FileParam
- [x] Support Freemarker and Thymeleaf
- [x] Session  RequestAttribute
- [x] support filter

 Websocket

- [x] Integrating Websocket by @Socket
- [x] Use @Path config websocket route
- [x] Support recevie @Open @Close @Message @TextMessage @BinaryMessage @Ping @Pong
- [x] Support WebsocketFrame to revcive params
- [x] protobuf , json auto encode & decode
- [x] support filter
- [x] support Sec-WebSocket-Protocol 


#### import to project
```
<dependency>
    <groupId>com.doopp</groupId>
    <artifactId>gutty</artifactId>
    <version>0.14.13</version>
</dependency>
```

```
compile 'com.doopp:gutty:0.14.13'
```

#### Example
```java
// launch Gutty
public static void main(String[] args) {
    new Gutty().loadProperties(args)
        .setBasePackages("com.doopp.gutty.auth")
        .setMessageConverter(JacksonMessageConverter.class)
        .setViewResolver(FreemarkerViewResolver.class)
        .addFilter("/api", ApiFilter.class)
        .addMyBatisModule(HikariCPProvider.class, "com.doopp.gutty.auth.dao", PageInterceptor.class)
        .addModules(
            new Module() {
                @Override
                public void configure(Binder binder) {
                }
                @Singleton
                @Provides
                @Named("bossEventLoopGroup")
                public EventLoopGroup bossEventLoopGroup() {
                    return new NioEventLoopGroup();
                }
                @Singleton
                @Provides
                @Named("workerEventLoopGroup")
                public EventLoopGroup workerEventLoopGroup() {
                    return new NioEventLoopGroup();
                }
            },
            new RedisModule() {
                @Override
                protected void initialize() {
                    bindShardedJedisPoolConfigProvider(ShardedJedisPoolConfigProvider.class);
                    bindSerializableHelper(JdkSerializableHelper.class);
                }
                @Singleton
                @Provides
                @Named("userRedis")
                public ShardedJedisHelper userRedis(JedisPoolConfig jedisPoolConfig, SerializableHelper serializableHelper, @Named("redis.user.servers") String userServers) {
                    return new ShardedJedisHelper(userServers, jedisPoolConfig, serializableHelper);
                }
                @Singleton
                @Provides
                public ShardedJedisHelper testRedis(JedisPoolConfig jedisPoolConfig, SerializableHelper serializableHelper, @Named("redis.test.servers") String userServers) {
                    return new ShardedJedisHelper(userServers, jedisPoolConfig, serializableHelper);
                }
            }
        )
        .addInjectorConsumer(injector->{
            ScheduledExecutorService newScheduledThreadPool = Executors.newScheduledThreadPool(8);
            newScheduledThreadPool.scheduleWithFixedDelay(injector.getInstance(AgarTask.class), 1000, 16, TimeUnit.MILLISECONDS);
        })
        .start();
}
```

```java
// Controller
@Path("/api")
@Controller
public class HelloController {

    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

    @Inject
    private HelloService helloService;

    @GET
    @Path("/redis/read")
    @Produces("application/json")
    public String readRedis() {
        return userRedis.get("hello");
    }

    @GET
    @Path("/redis/write")
    @Produces("application/json")
    public String writeRedis() {
        Long setValue = System.currentTimeMillis();
        userRedis.set("hello", String.valueOf(setValue));
        return String.valueOf(setValue);
    }

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
        user.setNickName(liu);
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
```

```java
// HelloService.java
public interface HelloService {
    String hello();
}

// HelloServiceImpl.java
@Service
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello() {
        return "hello kunlun !";
    }
}
```

```java
// websocket 
@Socket(subprotocol = "Auth-Token")
@Path("/ws/game") // route
public class HelloSocket {

    private static final Logger logger = LoggerFactory.getLogger(HelloSocket.class);

    @Open
    public void onOpen(FullHttpRequest httpRequest) {
        logger.info("httpRequest {}", httpRequest);
    }

    @Message
    public void onMessage(WebSocketFrame webSocketFrame) {
        logger.info("onMessage : {}", webSocketFrame.getClass());
    }

    @TextMessage
    public void onJsonMessage(@JsonFrame User user) {
        logger.info("user {}", user.getName());
    }

    @BinaryMessage
    public void onProtobufMessage(@ProtobufFrame User user) {
        logger.info("user {}", user);
    }

    @TextMessage
    public void onTextMessage(TextWebSocketFrame textFrame) {
        logger.info("textFrame {}", textFrame.text());
    }

    @BinaryMessage
    public void onBinaryMessage(BinaryWebSocketFrame binaryWebSocketFrame) {
        logger.info("binaryFrame {}", binaryWebSocketFrame.content());
    }

    @Ping
    public void onPing() {
    }

    @Pong
    public void onPong() {
    }

    @Close
    public void onClose() {
    }
}
```

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

#### 简介
```
一直很喜欢 Netty 和 Guice，简单好用，一直想自己动手来整合他们。
Gutty 大概用了一个月的时间来做，参考 Spring 中一些习惯，
通过扫描包完成路由，依赖，长链接配置，或是自动给 Controller 传递值。
也预置了 模板，Redis 和 Mybatis 的便捷接入，
可以按 URI 来添加多个 Filter,
后面还会继续的完善和添加便捷的功能。
```

#### 功能列表和完成状况
```
  短连接

* [x] Guice 和 Netty 整合
* [x] 启动时扫描包，为 @Controller 和 @Socket 的类配置路由
* [x] 自动绑定扫描到的 @Service 的类(普通的绑定 和 按名称绑定)
* [x] @Post @Get @Delete @Put 的 httpMethod 支持
* [x] 通过 @Product 识别返回值是 Json 还是 模板，或是 Protobuf 或是 Binary
* [x] 控制类下的方法参数传入，支持 @CookieParm @QueryParam @PathParam @FormParam
* [x] Controller 识别 和输出 Json 请求
* [x] Controller 识别 Protobuf 请求
* [x] 可以通过 @FileParam 来上传文件
* [x] 支持模板，预置 Freemarker 和 Thymeleaf 的接入
* [x] 支持 @RequestAttribute，可以用来提供 Session 功能
* [x] 适配 uri 的 Filter 支持
* [ ] jdk11 支持

 Websocket

* [x] 整合 Websocket，通过 @Socket 指定类接收长连接数据
* [x] Websocket 的路由通过类的 @Path 完成配置
* [x] 支持 @Open @Close @Message @TextMessage @BinaryMessage @Ping @Pong 完成不同数据包的传递
* [x] 长连接 WebsocketFrame 的值可自动传入到你的 socket handler
* [x] 添加 protobuf， json 的自动编码，解码到变量
* [x] 适配 uri 的 Filter 支持
* [x] @Socket 通过配置 subprotocol 来支持 Sec-WebSocket-Protocol 
```

#### 引入
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

#### 示例
```java
// 驱动 Gutty
// java -jar test.jar test.properties
public static void main(String[] args) {
    new Gutty().loadProperties(args) // 加载配置文件
        // 设定扫描的包路径
        .setBasePackages("com.doopp.gutty.test")
        // Json 支持
        .setMessageConverter(JacksonMessageConverter.class)
        // 模板支持
        .setViewResolver(FreemarkerViewResolver.class)
        // 添加 Filter
        .addFilter("/api", ApiFilter.class)
        // 配置数据库，需要引入 guice-mybatis 包
        .setMyBatis(HikariCPProvider.class, "com.doopp.gutty.test.dao", PageInterceptor.class)
        // 配置多个 redis
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
        // 完成 injector 配置后，执行里面的代码，多用于任务的执行
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
    public List<User> users(@RequestAttribute("hello") String hello) {
        System.out.println(hello);
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
@Path("/ws/game")
public class HelloSocket {

    private static final Logger logger = LoggerFactory.getLogger(HelloSocket.class);

    @Open
    public void onConnect(Channel channel) {
        channel.writeAndFlush(new TextWebSocketFrame("you connected"));
    }

    @TextMessage
    public void onTextMessage(Channel channel) {
        channel.writeAndFlush(new TextWebSocketFrame("hello"));
    }

    @TextMessage
    public void onJsonMessage(@JsonFrame User user) {
        logger.info("user {}", user.getNickName());
    }

    @BinaryMessage
    public void onBinaryMessage(BinaryWebSocketFrame binaryWebSocketFrame) {
        logger.info("binaryFrame {}", binaryWebSocketFrame.content());
    }

    @BinaryMessage
    public void onProtobufMessage(@ProtobufFrame User user) {
        logger.info("user {}", user);
    }

    //@Message
    //public void onMessage(WebSocketFrame webSocketFrame) {
        // logger.info("onMessage : {}", webSocketFrame.getClass());
    //}

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

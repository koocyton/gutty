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
```
  短连接

* [√] Integrating Guice and netty
* [√] Custom add Guice`s model
* [√] Support @Service @Controller @Socket
* [√] Use @Path config route
* [√] Support @Post @Get @Delete @Put
* [√] Support @Product , json or protobuf
* [√] Controller 自动注入 HttpRequest HttpResponse Post 和 Get 参数
* [√] Support Json and Protobuf
* [√] Support Freemarker and Thymeleaf
* [ ] Session  RequestAttribute

 Websocket

* [√] Integrating Websocket by @Socket
* [√] Use @Path config websocket route
* [√] Support recevie @Open @Close @Message @TextMessage @BinaryMessage @Ping @Pong
* [√] Support WebsocketFrame to revcive params
* [√] protobuf , json 的自动编码，解码到变量
* [ ] ... more
```

#### Example
```java
// 驱动 Gutty
public static void main(String[] args) {
    new Gutty().loadProperties(args)
            .basePackages(MVCApplication.class.getPackage().getName())
            .messageConverter(JacksonMessageConverter.class)
            .viewResolver(FreemarkerViewResolver.class)
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
    @Path("/template")
    public String template() {
        return "hello.template";
    }

    @GET
    @Path("/hello/{id}/{name}")
    public String hello(@PathParam("id") Integer id, @PathParam("name") String name) {
        logger.info("id {}", id);
        logger.info("name {}", name);
        return helloService.hello();
    }

    @POST
    @Path("/user")
    @Produces("application/json")
    public User hello2(User user) {
        logger.info("user {}", user);
        return user;
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
@Socket
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

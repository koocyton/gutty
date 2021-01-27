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
接触到 Java 的这三四年里，一直很喜欢 Netty 和 Guice ，想把他们整合起来！
网上有很多的例子，但是这些例子也多是基于 Netty 或 Guice 特性的，缺少值的自动传入和路由注解等功能，这是我最爱的。
去年将 Spring 出的 Reactor-Netty 和 Guice 整合了一遍，因为他已经处理好了路由，还有长连接，
只用想办法把参数自动传入，整合起来省事不少, 但是 Reactor 那个写法确实不习惯，有些方法也不熟悉，用起来常常绕晕自己。
最近有点时间，撸起袖子开始整，其实写的特别慢，特别是开始处理长连接时，几十行，边学习边摸索用了四五天，想明白了写，没想明白玩！
所以，目前代码质量都是基于摸索下成型的，后面空再学习和整理。 
```

#### 功能列表和完成状况
```
  短连接

* [√] Guice 和 Netty 整合
* [√] 可自定义添加 Guice 的 Model
* [√] 在类上加注解（@Service @Controller @Socket）自动扫描完成依赖注入
* [√] 通过 @Path 完成路由的配置，类和方法上的取值会自动连接
* [√] 识别请求的 @Post @Get @Delete @Put 的 httpMethod
* [√] 通过 @Product 识别返回值是 Json 还是 模板，或是 Protobuf 或是 Binary
* [√] Controller 自动注入 HttpRequest HttpResponse Post 和 Get 参数
* [ ] Controller 识别 Json 请求
* [ ] Controller 识别 Protobuf 请求
* [ ] Session  RequestAttribute

 Websocket

* [√] 整合 Websocket，通过 @Socket 指定类接收长连接数据
* [√] Websocket 的路由通过类的 @Path 完成配置
* [√] 通过方法上的注解 @Open @Close @Message @TextMessage @BinaryMessage @Ping @Pong 完成不同数据包的传递
* [√] 长连接 WebsocketFrame 的值可自动传入到你的 socket handler
* [ ] protobuf , json 的自动编码，解码到变量
* [ ] 长连接的参数还需要补充和完善
```

#### 示例
```java
// 驱动 Gutty
public static void main(String[] args) {
    new Gutty().loadProperties(args)
            .basePackages(MVCApplication.class.getPackage().getName())
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
    @Path("/hello/{id}/{name}")
    public String hello(@PathParam("id") Integer id, @PathParam("name") String name) {
        logger.info("id {}", id);
        logger.info("name {}", name);
        return helloService.hello();
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
@Path("/ws/game") // 路由
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

package com.doopp.gutty.framework;

import com.doopp.gutty.framework.netty.Http1RequestHandler;
import com.doopp.gutty.framework.netty.StaticFileRequestHandler;
import com.doopp.gutty.framework.netty.WebSocketServerHandler;
import com.google.inject.*;
import com.google.inject.name.Named;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Netty {

    private static final Logger logger = LoggerFactory.getLogger(Netty.class);

    @Inject
    private Injector injector;

    @Inject
    @Named("gutty.httpHost")
    private String httpHost;

    @Inject
    @Named("gutty.httpPort")
    private Integer httpPort;

    @Inject
    @Named("gutty.httpsPort")
    private Integer httpsPort;

    public void run() {
        // boss event
        EventLoopGroup bossEventLoopGroup = new NioEventLoopGroup();
        // worker event
        EventLoopGroup workerEventLoopGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossEventLoopGroup, workerEventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(channelInitializer())
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            System.out.println(launchScreen("http://" + httpHost + ":" + httpPort + "/index.html"));

            Channel ch80 = b.bind(httpHost, httpPort).sync().channel();
            Channel ch443 = b.bind(httpHost, httpsPort).sync().channel();

            ch80.closeFuture().sync();
            ch443.closeFuture().sync();
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        finally {
            bossEventLoopGroup.shutdownGracefully();
            workerEventLoopGroup.shutdownGracefully();
        }
    }

    private ChannelInitializer<SocketChannel> channelInitializer() {
        // construct ChannelInitializer<SocketChannel>
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                // if (ch.localAddress().getPort() == applicationProperties.i("server.sslPort")) {
                //    SSLContext sslContext = SSLContext.getInstance("TLS");
                //    sslContext.init(getKeyManagers(), null, null);
                //    SSLEngine sslEngine = sslContext.createSSLEngine();
                //    sslEngine.setUseClientMode(false);
                //    ch.pipeline().addLast(new SslHandler(sslEngine));
                // }
                // HttpServerCodec：将请求和应答消息解码为HTTP消息
                pipeline.addLast(new HttpServerCodec());
                // HttpObjectAggregator：将HTTP消息的多个部分合成一条完整的HTTP消息
                pipeline.addLast(new HttpObjectAggregator(65536));
                // that adds support for writing a large data stream
                pipeline.addLast(new ChunkedWriteHandler());
                // websocket request
                // pipeline.addLast(new WebSocketServerProtocolHandler("/ws", true));
                pipeline.addLast(new WebSocketServerHandler(injector));
                // static request
                pipeline.addLast(new StaticFileRequestHandler());
                // http request
                pipeline.addLast(new Http1RequestHandler(injector));
            }
        };
    }

    private String launchScreen(String text) {
        return  "\n\n" +
                "   _____           _     _           \n" +
                "  / ____|         | |   | |          \n" +
                " | |  __   _   _  | |_  | |_   _   _ \n" +
                " | | |_ | | | | | | __| | __| | | | |\n" +
                " | |__| | | |_| | | |_  | |_  | |_| |\n" +
                "  \\_____|  \\__,_|  \\__|  \\__|  \\__, |   " + text +"\n" +
                "                                __/ |\n" +
                "                               |___/\n";
    }
}

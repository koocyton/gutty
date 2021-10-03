package com.doopp.gutty;

import com.doopp.gutty.netty.Http1RequestHandler;
import com.doopp.gutty.netty.StaticFileRequestHandler;
import com.doopp.gutty.netty.WebSocketServerHandler;
import com.google.inject.*;
import com.google.inject.name.Named;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

class Netty {

    @Inject
    private Injector injector;

    @Inject
    @Named("bossEventLoopGroup")
    private EventLoopGroup bossEventLoopGroup;

    @Inject
    @Named("workerEventLoopGroup")
    private EventLoopGroup workerEventLoopGroup;

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
                ch.pipeline().addLast(new HttpServerCodec());
                // HttpObjectAggregator：将HTTP消息的多个部分合成一条完整的HTTP消息
                ch.pipeline().addLast(new HttpObjectAggregator(65536));
                // that adds support for writing a large data stream
                ch.pipeline().addLast(new ChunkedWriteHandler());
                // websocket
                ch.pipeline().addLast(injector.getInstance(WebSocketServerHandler.class));
                // http request
                ch.pipeline().addLast(injector.getInstance(Http1RequestHandler.class));
                // static request
                ch.pipeline().addLast(injector.getInstance(StaticFileRequestHandler.class));
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

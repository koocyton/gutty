package com.doopp.gutty.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

@Slf4j
public class HttpClientTest {

    @Test
    public void httpClient() throws InterruptedException, ExecutionException {
        Bootstrap b = new Bootstrap();
        b.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .remoteAddress(new InetSocketAddress("www.doopp.com" , 80))
                .option(ChannelOption.SO_KEEPALIVE, false)
                // .option(ChannelOption.SO_TIMEOUT, 5000)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        // 客户端 使用HttpResponseDecoder进行解码
                        ch.pipeline().addLast(new HttpResponseDecoder());
                        // 客户端 使用HttpRequestEncoder进行编码
                        // ch.pipeline().addLast(new HttpRequestEncoder());
                        ch.pipeline().addLast(new HttpObjectAggregator(65535));
                        ch.pipeline().addLast(new HttpClientHandler());
                    }
                });
        ChannelFuture f = b.connect().sync();
        f.channel().closeFuture().sync();
    }

    private final static class HttpClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ctx.writeAndFlush(Unpooled.copiedBuffer("netty rocks!", CharsetUtil.UTF_8));
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            System.out.println("client received:" + msg.toString(CharsetUtil.UTF_8));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}

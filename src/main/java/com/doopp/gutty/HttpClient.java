package com.doopp.gutty;

import com.google.inject.Singleton;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Singleton
public class HttpClient {

    public Bootstrap bootstrap(ChannelInboundHandlerAdapter httpClientHandler) {
        Bootstrap b = new Bootstrap();
        b.group(new NioEventLoopGroup());
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, false);
        b.option(ChannelOption.SO_TIMEOUT, 10);
        b.option(ChannelOption.SO_KEEPALIVE, false);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                // 客户端 使用HttpResponseDecoder进行解码
                ch.pipeline().addLast(new HttpResponseDecoder());
                // 客户端 使用HttpRequestEncoder进行编码
                ch.pipeline().addLast(new HttpRequestEncoder());
                ch.pipeline().addLast(httpClientHandler);
            }
        });
        return b;
    }

    private final static class HttpClientHandler extends ChannelInboundHandlerAdapter {

        private CompletableFuture<String> future;

        private HttpClientHandler(CompletableFuture<String> future) {
            this.future = future;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if(msg instanceof HttpContent) {
                HttpContent content = (HttpContent)msg;
                ByteBuf buf = content.content();
                future.complete(buf.toString(CharsetUtil.UTF_8));
                buf.release();
            }
        }
    }

    public String request() {
        final CompletableFuture<String> future = new CompletableFuture<>();
        Bootstrap b = bootstrap(new HttpClientHandler(future));
        b.connect("http://127.0.0.1", 123);
        try {
            return future.get();
        }
        catch (ExecutionException | InterruptedException e) {
            return null;
        }
    }
}

package com.doopp.gutty.framework.netty;

import com.doopp.gutty.framework.Dispatcher;
import com.google.inject.Injector;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketConnectHandler extends SimpleChannelInboundHandler<Object> {

    private final static Logger logger = LoggerFactory.getLogger(WebSocketConnectHandler.class);

    private final Injector injector;

    public WebSocketConnectHandler(Injector injector){
        this.injector = injector;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest){
            onConnect(ctx, (FullHttpRequest) msg);
        }
        else if (msg instanceof TextWebSocketFrame){
            onTextMessage(ctx, (TextWebSocketFrame) msg);
        }
        else if (msg instanceof BinaryWebSocketFrame){
            onBinaryMessage(ctx, (BinaryWebSocketFrame) msg);
        }
        else if (msg instanceof PingWebSocketFrame){
            onPing(ctx, (PingWebSocketFrame) msg);
        }
        else if (msg instanceof PongWebSocketFrame){
            onPong(ctx, (PongWebSocketFrame) msg);
        }
        else if (msg instanceof CloseWebSocketFrame){
            onClose(ctx, (CloseWebSocketFrame) msg);
        }
    }

    private void onConnect(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        if (httpRequest.headers().containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE, true)) {
            Dispatcher dispatcher = Dispatcher.getInstance();

            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            return;
        }
        ctx.fireChannelRead(httpRequest.retain());
    }

    private void onTextMessage(ChannelHandlerContext ctx, TextWebSocketFrame socketFrame) {

    }

    private void onBinaryMessage(ChannelHandlerContext ctx, BinaryWebSocketFrame socketFrame) {

    }

    private void onClose(ChannelHandlerContext ctx, CloseWebSocketFrame socketFrame) {

    }

    private void onPing(ChannelHandlerContext ctx, PingWebSocketFrame socketFrame) {

    }

    private void onPong(ChannelHandlerContext ctx, PongWebSocketFrame socketFrame) {

    }
}

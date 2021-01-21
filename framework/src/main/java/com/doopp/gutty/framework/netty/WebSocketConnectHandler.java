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
            onConnect((FullHttpRequest) msg);
        }
        else if (msg instanceof TextWebSocketFrame){
            onTextMessage((TextWebSocketFrame) msg);
        }
        else if (msg instanceof BinaryWebSocketFrame){
            onBinaryMessage((BinaryWebSocketFrame) msg);
        }
        else if (msg instanceof PingWebSocketFrame){
            onPing((PingWebSocketFrame) msg);
        }
        else if (msg instanceof PongWebSocketFrame){
            onPong((PongWebSocketFrame) msg);
        }
        else if (msg instanceof CloseWebSocketFrame){
            onClose((CloseWebSocketFrame) msg);
        }
    }

    private void onConnect(FullHttpRequest httpRequest) {

    }

    private void onTextMessage(TextWebSocketFrame socketFrame) {

    }

    private void onBinaryMessage(BinaryWebSocketFrame socketFrame) {

    }

    private void onClose(CloseWebSocketFrame socketFrame) {

    }

    private void onPing(PingWebSocketFrame socketFrame) {

    }

    private void onPong(PongWebSocketFrame socketFrame) {

    }
}

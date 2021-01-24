package com.doopp.gutty.framework.netty;

import com.doopp.gutty.framework.Dispatcher;
import com.doopp.gutty.framework.HttpParam;
import com.google.inject.Injector;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

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
            onText(ctx, (TextWebSocketFrame) msg);
        }
        else if (msg instanceof BinaryWebSocketFrame){
            onBinary(ctx, (BinaryWebSocketFrame) msg);
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
        // logger.info("httpRequest {}", httpRequest);
        if (httpRequest.headers().containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE, true)) {
            logger.info("httpRequest {}", httpRequest);
            Dispatcher dispatcher = Dispatcher.getInstance();
            Dispatcher.SocketRoute socketRoute = dispatcher.getSocketRoute(httpRequest.uri());
            if (socketRoute==null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                return;
            }
            // get controller
            Object socket = injector.getInstance(socketRoute.getClazz());
            List<Method> openMethodList = socketRoute.getOpenMethodList();
            for(Method method : openMethodList) {
                logger.info("method {}", method);
                try {
                    if ((method.getParameters().length == 0)) {
                        method.invoke(socket);
                    } else {
                        method.invoke(socket, HttpParam.singleBuilder(ctx, httpRequest).getParams(method.getParameters(), socketRoute.getPathParamMap()));
                    }
                }
                catch(Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return;
        }
        ctx.fireChannelRead(httpRequest.retain());
    }

    private void onText(ChannelHandlerContext ctx, TextWebSocketFrame socketFrame) {

    }

    private void onBinary(ChannelHandlerContext ctx, BinaryWebSocketFrame socketFrame) {

    }

    private void onClose(ChannelHandlerContext ctx, CloseWebSocketFrame socketFrame) {

    }

    private void onPing(ChannelHandlerContext ctx, PingWebSocketFrame socketFrame) {

    }

    private void onPong(ChannelHandlerContext ctx, PongWebSocketFrame socketFrame) {

    }
}

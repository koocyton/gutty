package com.doopp.gutty.framework.netty;

import com.doopp.gutty.framework.Dispatcher;
import com.google.inject.Injector;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class WebSocketConnectHandler extends WebSocketServerProtocolHandler {

    private final static Logger logger = LoggerFactory.getLogger(WebSocketConnectHandler.class);

    private Injector injector;

    public WebSocketConnectHandler(Injector injector){
        super("");
        this.injector = injector;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
        if (frame instanceof CloseWebSocketFrame) {
            WebSocketServerHandshaker handshaker = getHandshaker(ctx.channel());
            if (handshaker != null) {
                frame.retain();
                handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame);
            } else {
                logger.info("{}", ctx);
                ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
            return;
        }
        super.decode(ctx, frame, out);
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

    private static final AttributeKey<WebSocketServerHandshaker> HANDSHAKER_ATTR_KEY =
            AttributeKey.valueOf(WebSocketServerHandshaker.class, "HANDSHAKER");

    static WebSocketServerHandshaker getHandshaker(Channel channel) {
        return channel.attr(HANDSHAKER_ATTR_KEY).get();
    }

    static void setHandshaker(Channel channel, WebSocketServerHandshaker handshaker) {
        channel.attr(HANDSHAKER_ATTR_KEY).set(handshaker);
    }
}

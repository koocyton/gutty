package com.doopp.gutty.framework.netty;

import com.doopp.gutty.framework.Dispatcher;
import com.google.inject.Injector;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketConnectHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final static Logger logger = LoggerFactory.getLogger(WebSocketConnectHandler.class);

    private final Injector injector;

    public WebSocketConnectHandler(Injector injector){
        this.injector = injector;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws Exception {
        if (httpRequest.headers().containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE, true)) {
            Dispatcher dispatcher = Dispatcher.getInstance();
        } else {
            ctx.fireChannelRead(httpRequest.retain());
            return;
        }
    }
}

package com.doopp.gutty.filter;

import com.doopp.gutty.Gutty;
import com.google.inject.Injector;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public class FilterHandler extends SimpleChannelInboundHandler<Object> {

    private final Injector injector;

    public FilterHandler(Injector injector){
        this.injector = injector;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Filter filter = Gutty.getInstance(this.injector, Filter.class);
        if (msg instanceof FullHttpRequest) {
            if (filter==null) {
                ctx.fireChannelRead(((FullHttpRequest) msg).retain());
            }
            else {
                filter.doFilter(ctx, (FullHttpRequest) msg, new FilterChain());
            }
        }
        else if (msg instanceof WebSocketFrame) {
            ctx.fireChannelRead(((WebSocketFrame) msg).retain());
        }
    }
}

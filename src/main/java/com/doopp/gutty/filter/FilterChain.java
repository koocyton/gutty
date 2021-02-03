package com.doopp.gutty.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public class FilterChain {

    public void fireChannelRead(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        ctx.fireChannelRead(httpRequest.retain());
    }
}

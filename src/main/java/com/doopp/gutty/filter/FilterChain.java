package com.doopp.gutty.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public class FilterChain {

    public void doFilter(FullHttpRequest httpRequest, FullHttpResponse httpResponse) {

    }

    public void fireChannelRead(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        ctx.fireChannelRead(httpRequest.retain());
    }
}

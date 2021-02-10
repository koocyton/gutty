package com.doopp.gutty.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public class FilterChain {

    private final FilterHandler filterHandler;

    public FilterChain(FilterHandler filterHandler) {
        this.filterHandler = filterHandler;
    }

    public void doFilter(ChannelHandlerContext ctx, FullHttpRequest httpRequest, FullHttpResponse httpResponse) {
        filterHandler.handleRequest(ctx, httpRequest, httpResponse);
    }
}

package com.doopp.gutty.filter;

import com.doopp.gutty.netty.AbstractFilterHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public class FilterChain {

    private final AbstractFilterHandler<?> filterHandler;

    public FilterChain(AbstractFilterHandler<?> filterHandler) {
        this.filterHandler = filterHandler;
    }

    public void doFilter(ChannelHandlerContext ctx, FullHttpRequest httpRequest, FullHttpResponse httpResponse) throws Exception {
        filterHandler.handleRequest(ctx, httpRequest, httpResponse);
    }
}

package com.doopp.gutty.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public interface Filter {

    void doFilter(ChannelHandlerContext ctx, FullHttpRequest httpRequest);
}

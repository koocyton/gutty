package com.doopp.gutty.filter;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public interface Filter {

    void doFilter(FullHttpRequest httpRequest, FullHttpResponse httpResponse, FilterChain filterChain);
}

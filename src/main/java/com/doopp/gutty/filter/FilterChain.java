package com.doopp.gutty.filter;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import java.util.function.BiConsumer;

public class FilterChain {

    private final BiConsumer<FullHttpRequest, FullHttpResponse> consumer;

    public FilterChain(BiConsumer<FullHttpRequest, FullHttpResponse> consumer) {
        this.consumer = consumer;
    }

    public void doFilter(FullHttpRequest httpRequest, FullHttpResponse httpResponse) {
        this.consumer.accept(httpRequest, httpResponse);
    }
}

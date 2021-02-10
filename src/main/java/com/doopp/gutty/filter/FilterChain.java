package com.doopp.gutty.filter;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import java.util.function.BiConsumer;

public class FilterChain {

    private final BiConsumer<FullHttpRequest, FullHttpResponse> biConsumer;

    public FilterChain(BiConsumer<FullHttpRequest, FullHttpResponse> biConsumer) {
        this.biConsumer = biConsumer;
    }

    public void doFilter(FullHttpRequest httpRequest, FullHttpResponse httpResponse) {
        this.biConsumer.accept(httpRequest, httpResponse);
    }
}

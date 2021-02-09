package com.doopp.gutty.filter;

import com.doopp.gutty.Gutty;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import java.util.Map;
import java.util.function.BiConsumer;

public class FilterHandler {

    @Inject
    private Injector injector;

    private final Map<String, Class<? extends Filter>> uriFilters;

    public FilterHandler(Map<String, Class<? extends Filter>> uriFilters){
        this.uriFilters = uriFilters;
    }

    public void doFilter(FullHttpRequest httpRequest, FullHttpResponse httpResponse, BiConsumer<FullHttpRequest, FullHttpResponse> consumer) {
        if (uriFilters==null || uriFilters.size()<1) {
            consumer.accept(httpRequest, httpResponse);
            return;
        }
        // 检索所有的 filters
        for (String startUri : this.uriFilters.keySet()) {
            String uri = httpRequest.uri();
            // 如果有适配 uri 的 Filter
            if (uri.length()>startUri.length() && uri.startsWith(startUri)) {
                Class<? extends Filter> filterClass = this.uriFilters.get(startUri);
                Filter filter = Gutty.getInstance(this.injector, filterClass);
                if (filter!=null) {
                    filter.doFilter(httpRequest, httpResponse, new FilterChain(consumer));
                    return;
                }
            }
        }
    }
}

package com.doopp.gutty.filter;

import com.doopp.gutty.Gutty;
import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.util.Map;
import java.util.Set;

public class FilterHandler extends SimpleChannelInboundHandler<Object> {

    private final Injector injector;
    private final Map<String, Class<? extends Filter>> uriFilters;

    public FilterHandler(Injector injector, Map<String, Class<? extends Filter>> uriFilters){
        this.injector = injector;
        this.uriFilters = uriFilters;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 如果是 http 请求
        if (msg instanceof FullHttpRequest) {
            // 检索所有的 filters
            for (String startUri : this.uriFilters.keySet()) {
                String uri = ((FullHttpRequest) msg).uri();
                // 如果有适配 uri 的 Filter
                if (uri.length()>startUri.length() && uri.startsWith(startUri)) {
                    Class<? extends Filter> filterClass = this.uriFilters.get(startUri);
                    Filter filter = Gutty.getInstance(this.injector, filterClass);
                    if (filter==null) {
                        ctx.fireChannelRead(((FullHttpRequest) msg).retain());
                    }
                    else {
                        filter.doFilter(ctx, (FullHttpRequest) msg, new FilterChain());
                    }
                }
                else {
                    ctx.fireChannelRead(((FullHttpRequest) msg).retain());
                }
            }
        }
        else if (msg instanceof WebSocketFrame) {
            ctx.fireChannelRead(((WebSocketFrame) msg).retain());
        }
    }
}
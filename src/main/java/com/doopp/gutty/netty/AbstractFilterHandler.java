package com.doopp.gutty.netty;

import com.doopp.gutty.Gutty;
import com.doopp.gutty.NotFoundException;
import com.doopp.gutty.filter.Filter;
import com.doopp.gutty.filter.FilterChain;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public abstract class AbstractFilterHandler<I> extends SimpleChannelInboundHandler<I> {

    @Inject
    private Map<String, Class<? extends Filter>> filterMap;

    @Inject
    private Injector injector;

    protected void handleFilter(ChannelHandlerContext ctx, FullHttpRequest httpRequest, FullHttpResponse httpResponse, AbstractFilterHandler<?> filterHandler) {

        try {
            // 如果 filter map 为空
            if (filterMap == null || filterMap.size() < 1) {
                handleRequest(ctx, httpRequest, httpResponse);
                return;
            }
            // 检索所有的 filters
            for (String startUri : this.filterMap.keySet()) {
                String uri = httpRequest.uri();
                // 如果有适配 uri 的 Filter
                if (uri.length() > startUri.length() && uri.startsWith(startUri)) {
                    Class<? extends Filter> filterClass = this.filterMap.get(startUri);
                    Filter filter = Gutty.getInstance(injector, filterClass);
                    // filter 不能为空
                    if (filter != null) {
                        filter.doFilter(ctx, httpRequest, httpResponse, new FilterChain(filterHandler));
                        return;
                    }
                }
            }
            // 如果没有 filter 能匹配上
            handleRequest(ctx, httpRequest, httpResponse);
        }
        catch (Exception e) {
            sendError(ctx, e, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    protected void sendError(ChannelHandlerContext ctx, Exception e, HttpResponseStatus status) {
        if (!(e instanceof NotFoundException)) {
            e.printStackTrace();
        }
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status);
        response.content().writeBytes(Unpooled.copiedBuffer("".getBytes(CharsetUtil.UTF_8)));
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public abstract void handleRequest(ChannelHandlerContext ctx, FullHttpRequest httpRequest, FullHttpResponse httpResponse);

}

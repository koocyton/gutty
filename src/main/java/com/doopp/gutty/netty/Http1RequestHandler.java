package com.doopp.gutty.netty;

import com.doopp.gutty.Dispatcher;
import com.doopp.gutty.Gutty;
import com.doopp.gutty.NotFoundException;
import com.doopp.gutty.filter.Filter;
import com.doopp.gutty.filter.FilterChain;
import com.google.inject.*;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class Http1RequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final static Logger logger = LoggerFactory.getLogger(Http1RequestHandler.class);

    @Inject
    private Injector injector;

    @Inject
    private Map<String, Class<? extends Filter>> filterMap;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        FullHttpResponse httpResponse = (HttpUtil.is100ContinueExpected(httpRequest))
                ? new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE)
                : new DefaultFullHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.CONTINUE);
        executeFilters(ctx, httpRequest, httpResponse, (req, rep)->{
            try {
                completeRequest(ctx, req, rep);
            }
            catch (NotFoundException e) {
                ctx.fireChannelRead(httpRequest.retain());
            }
            catch (RuntimeException e) {
                throw e;
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void completeRequest(ChannelHandlerContext ctx, FullHttpRequest httpRequest, FullHttpResponse httpResponse) throws Exception {
        // 执行路由
        byte[] result = Dispatcher.getInstance().executeHttpRoute(injector, ctx, httpRequest, httpResponse);
        // 写入内容
        httpResponse.content().writeBytes(Unpooled.copiedBuffer(result));
        // set length
        httpResponse.headers().set(CONTENT_LENGTH, httpResponse.content().readableBytes());
        // keep alive
        if (HttpUtil.isKeepAlive(httpRequest)) {
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        // set status
        httpResponse.setStatus(HttpResponseStatus.OK);
        ctx.write(httpResponse);
        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!HttpUtil.isKeepAlive(httpRequest)) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    static void sendError(ChannelHandlerContext ctx, Exception e, HttpResponseStatus status) {
        if (!(e instanceof NotFoundException)) {
            e.printStackTrace();
        }
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status);
        response.content().writeBytes(Unpooled.copiedBuffer("".getBytes(CharsetUtil.UTF_8)));
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public void executeFilters(ChannelHandlerContext ctx, FullHttpRequest httpRequest, FullHttpResponse httpResponse, BiConsumer<FullHttpRequest, FullHttpResponse> biConsumer) {
        if (filterMap==null || filterMap.size()<1) {
            biConsumer.accept(httpRequest, httpResponse);
            return;
        }
        // 检索所有的 filters
        for (String startUri : this.filterMap.keySet()) {
            String uri = httpRequest.uri();
            // 如果有适配 uri 的 Filter
            if (uri.length()>startUri.length() && uri.startsWith(startUri)) {
                Class<? extends Filter> filterClass = this.filterMap.get(startUri);
                Filter filter = Gutty.getInstance(this.injector, filterClass);
                if (filter!=null) {
                    filter.doFilter(ctx, httpRequest, httpResponse, new FilterChain(biConsumer));
                    return;
                }
            }
            biConsumer.accept(httpRequest, httpResponse);
        }
    }
}

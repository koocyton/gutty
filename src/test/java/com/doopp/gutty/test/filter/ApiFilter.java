package com.doopp.gutty.test.filter;

import com.doopp.gutty.filter.Filter;
import com.doopp.gutty.filter.FilterChain;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;

public class ApiFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ApiFilter.class);

    @Inject
    private Injector injector;

    @Override
    public void doFilter(ChannelHandlerContext ctx, FullHttpRequest httpRequest, FullHttpResponse httpResponse, FilterChain filterChain) {

        // 不过滤的uri
        String[] notFilterUris = new String[]{
                "/index.html",
                "/favicon.ico",
                "/css",
                "/img",
                "/js",
                "/chat",
                "/ws",
        };

        // 请求的uri
        String uri = httpRequest.uri();

        // 是否过滤
        boolean doFilter = true;

        // 如果uri中包含不过滤的uri，则不进行过滤
        for (String notFilterUri : notFilterUris) {
            if (uri.startsWith(notFilterUri)) {
                doFilter = false;
                break;
            }
        }

        // OPTIONS
        if (httpRequest.method().name().equals(HttpMethod.OPTIONS)) {
            return;
        }

        // do filter ... :)
        try {
            if (doFilter) {
                // 从 Header 里拿到 Authentication ( Base64.encode )
                String userToken = httpRequest.headers().get("User-Token");
                if (userToken==null) {
                    throw new RuntimeException("haha");
                }
            }
            filterChain.doFilter(httpRequest, httpResponse);
        }
        catch (Exception e) {
            // FullHttpResponse httpResponse = new DefaultFullHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.OK);
            // writeExceptionResponse(500, httpRequest, httpResponse, "ni cuo le ");
        }
    }

//    private static void writeExceptionResponse(int errorCode, FullHttpRequest httpRequest, FullHttpResponse httpResponse, String errorMessage) {
//        byte[] result = ("{\"code\":" + errorCode + ", \"msg\":\"" + errorMessage + "\", \"data\":null}").getBytes();
//        httpResponse.setStatus(HttpResponseStatus.OK);
//        httpResponse.headers().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8");
//        httpResponse.content().writeBytes(Unpooled.copiedBuffer(result));
//        // set length
//        httpResponse.headers().set(CONTENT_LENGTH, httpResponse.content().readableBytes());
//        if (HttpUtil.isKeepAlive(httpRequest)) {
//            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
//        }
//        ctx.write(httpResponse);
//        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
//        if (!HttpUtil.isKeepAlive(httpRequest)) {
//            future.addListener(ChannelFutureListener.CLOSE);
//        }
//    }
}

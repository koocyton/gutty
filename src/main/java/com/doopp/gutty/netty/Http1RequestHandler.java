package com.doopp.gutty.netty;

import com.doopp.gutty.Dispatcher;
import com.doopp.gutty.NotFoundException;
import com.google.inject.Injector;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class Http1RequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final static Logger logger = LoggerFactory.getLogger(Http1RequestHandler.class);

    private final Injector injector;

    public Http1RequestHandler(Injector injector){
        this.injector = injector;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws Exception {
        // if (HttpUtil.is100ContinueExpected(httpRequest)) {
        //    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        //    ctx.writeAndFlush(response);
        // }

        // init httpResponse
        FullHttpResponse httpResponse = new DefaultFullHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.OK);
        byte[] result;
        try {
            // execute route
            result = Dispatcher.getInstance().executeHttpRoute(injector, ctx, httpRequest, httpResponse);
        }
        catch (NotFoundException e) {
            ctx.fireChannelRead(httpRequest.retain());
            return;
        }
        catch (Exception e) {
            sendError(ctx, e, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            return;
        }
        httpResponse.content().writeBytes(Unpooled.copiedBuffer(result));
        // set length
        httpResponse.headers().set(CONTENT_LENGTH, httpResponse.content().readableBytes());

        if (HttpUtil.isKeepAlive(httpRequest)) {
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        ctx.write(httpResponse);
        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!HttpUtil.isKeepAlive(httpRequest)) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    static void sendError(ChannelHandlerContext ctx, Exception e, HttpResponseStatus status) {
        e.printStackTrace();
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status);
        response.content().writeBytes(Unpooled.copiedBuffer("".getBytes(CharsetUtil.UTF_8)));
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}

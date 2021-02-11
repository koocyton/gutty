package com.doopp.gutty.netty;

import com.doopp.gutty.Dispatcher;
import com.doopp.gutty.NotFoundException;
import com.google.inject.*;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;

import java.lang.reflect.InvocationTargetException;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;

public class Http1RequestHandler extends AbstractFilterHandler<FullHttpRequest> {

    @Inject
    private Injector injector;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        FullHttpResponse httpResponse = (HttpUtil.is100ContinueExpected(httpRequest))
                ? new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE)
                : new DefaultFullHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.CONTINUE);
        handleFilter(ctx, httpRequest, httpResponse, this);
    }

    @Override
    public void handleRequest(ChannelHandlerContext ctx, FullHttpRequest httpRequest, FullHttpResponse httpResponse) {
        byte[] result;
        // 执行路由
        try {
            result = Dispatcher.getInstance().executeHttpRoute(injector, ctx, httpRequest, httpResponse);
        }
        catch (NotFoundException e) {
            ctx.fireChannelRead(httpRequest.retain());
            return;
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
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
}

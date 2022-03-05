package com.doopp.gutty.netty;

import com.doopp.gutty.NotFoundException;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;

public class StaticFileRequestHandler extends AbstractFilterHandler<FullHttpRequest> {

    private static final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        FullHttpResponse httpResponse = (HttpUtil.is100ContinueExpected(httpRequest))
                ? new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE)
                : new DefaultFullHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.CONTINUE);
        handleFilter(ctx, httpRequest, httpResponse, this);
    }

    @Override
    public void handleRequest(ChannelHandlerContext ctx, FullHttpRequest httpRequest, FullHttpResponse httpResponse) {
        String requestUri = httpRequest.uri();
        int indexOf = requestUri.indexOf("?");
        if (indexOf!=-1) {
            requestUri = requestUri.substring(0, indexOf);
        }

        // 获取静态文件
        InputStream ins = getClass().getResourceAsStream("/public" + requestUri);
        if (ins==null) {
            sendError(ctx,  new NotFoundException(""), HttpResponseStatus.NOT_FOUND);
            return;
            // throw new NotFoundException("");
            // ctx.fireChannelRead(httpRequest.retain());
            // Http1RequestHandler.sendError(ctx, new NotFoundException(""), HttpResponseStatus.NOT_FOUND);
            // return;
        }
        // 读取文件
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] bs = new byte[1024];
        int len;
        try {
            while ((len = ins.read(bs)) != -1) {
                bout.write(bs, 0, len);
            }
            ins.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        // init httpResponse
        // FullHttpResponse httpResponse = new DefaultFullHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.OK);
        httpResponse.content().writeBytes(Unpooled.copiedBuffer(bout.toByteArray()));
        // set length
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
        // media type
        String contentType = mimetypesFileTypeMap.getContentType(requestUri);
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        if (contentType.contains("text/")) {
            httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType+";charset=utf-8");
        }
        // keepAlive
        boolean keepAlive = HttpUtil.isKeepAlive(httpRequest);
        if (!keepAlive) {
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        }
        else if (httpRequest.protocolVersion().equals(HttpVersion.HTTP_1_0)) {
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        httpResponse.setStatus(HttpResponseStatus.OK);
        // ctx write
        ctx.write(httpResponse);
        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
}

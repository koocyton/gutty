package com.doopp.gutty.framework.netty;

import com.doopp.gutty.framework.Dispatcher;
import com.doopp.gutty.framework.HttpParam;
import com.google.inject.Injector;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class WebSocketConnectHandler extends SimpleChannelInboundHandler<Object> {

    private final static Logger logger = LoggerFactory.getLogger(WebSocketConnectHandler.class);

    private final Injector injector;

    public WebSocketConnectHandler(Injector injector){
        this.injector = injector;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest){
            onConnect(ctx, (FullHttpRequest) msg);
        }
        else if (msg instanceof WebSocketFrame) {
            callSocketMethod(ctx, msg);
        }
    }

    private void onConnect(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        // is websocket
        if (httpRequest.headers().containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE, true)) {
            Dispatcher dispatcher = Dispatcher.getInstance();
            Dispatcher.SocketRoute socketRoute = dispatcher.getSocketRoute(httpRequest.uri());
            logger.info("{}", ctx);
            if (socketRoute==null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                return;
            }


            logger.info("{}", ctx);
            try {
                if (!GET.equals(httpRequest.method())) {
                    sendHttpResponse(ctx, httpRequest, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN, ctx.alloc().buffer(0)));
                    return;
                }

                final WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                        "ws://127.0.0.1:8681/ws/game", "", true);
                final WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(httpRequest);
                final ChannelPromise localHandshakePromise = ctx.newPromise();
                if (handshaker == null) {
                    WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                } else {
                    // Ensure we set the handshaker and replace this handler before we
                    // trigger the actual handshake. Otherwise we may receive websocket bytes in this handler
                    // before we had a chance to replace it.
                    //
                    // See https://github.com/netty/netty/issues/9471.
                    // WebSocketServerProtocolHandler.setHandshaker(ctx.channel(), handshaker);
                    ctx.pipeline().remove(this);
                    logger.info("{}", ctx);
                    final ChannelFuture handshakeFuture = handshaker.handshake(ctx.channel(), httpRequest);
                    handshakeFuture.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) {
                            if (!future.isSuccess()) {
                                localHandshakePromise.tryFailure(future.cause());
                                ctx.fireExceptionCaught(future.cause());
                            } else {
                                localHandshakePromise.trySuccess();
                                // Kept for compatibility
                                ctx.fireUserEventTriggered(
                                        WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE);
                                //ctx.fireUserEventTriggered(
                                //        new WebSocketServerProtocolHandler.HandshakeComplete(
                                //                httpRequest.uri(), httpRequest.headers(), handshaker.selectedSubprotocol()));
                            }
                        }
                    });
                    applyHandshakeTimeout(ctx);
                }
            } finally {
                httpRequest.release();
            }

            setSocketRoute(ctx, httpRequest);
            callSocketMethod(ctx, httpRequest);
            return;
        }
        ctx.fireChannelRead(httpRequest.retain());
    }

    private void callSocketMethod(ChannelHandlerContext ctx, Object msg) {
        // get socket route
        Dispatcher.SocketRoute socketRoute = getSocketRoute(ctx);
        FullHttpRequest httpRequest = getHttpRequest(ctx);
        if (socketRoute==null) {
            return;
        }
        // get injector instance
        Object socket = injector.getInstance(socketRoute.getClazz());
        // 初始化要调用的方法
        List<Method> callMethodList = socketRoute.getMessageMethodList();
        // on connect
        if (msg instanceof FullHttpRequest){
            callMethodList = socketRoute.getOpenMethodList();
        }
        // Text Frame
        else if (msg instanceof TextWebSocketFrame) {
            if (callMethodList==null) {
                callMethodList = socketRoute.getTextMethodList();
            }
            else if (socketRoute.getTextMethodList()!=null) {
                callMethodList.addAll(socketRoute.getTextMethodList());
            }
        }
        // Binary Frame
        else if (msg instanceof BinaryWebSocketFrame) {
            if (callMethodList==null) {
                callMethodList = socketRoute.getBinaryMethodList();
            }
            else if (socketRoute.getTextMethodList()!=null) {
                callMethodList.addAll(socketRoute.getBinaryMethodList());
            }
        }
        // Ping Frame
        else if (msg instanceof PingWebSocketFrame) {
            if (callMethodList==null) {
                callMethodList = socketRoute.getPingMethodList();
            }
            else if (socketRoute.getTextMethodList()!=null) {
                callMethodList.addAll(socketRoute.getPingMethodList());
            }
        }
        // Pong Frame
        else if (msg instanceof PongWebSocketFrame) {
            if (callMethodList==null) {
                callMethodList = socketRoute.getPongMethodList();
            }
            else if (socketRoute.getTextMethodList()!=null) {
                callMethodList.addAll(socketRoute.getPongMethodList());
            }
        }
        // Close Frame
        else if (msg instanceof CloseWebSocketFrame) {
            if (callMethodList==null) {
                callMethodList = socketRoute.getCloseMethodList();
            }
            else if (socketRoute.getTextMethodList()!=null) {
                callMethodList.addAll(socketRoute.getCloseMethodList());
            }
        }
        for(Method method : callMethodList) {
            try {
                if ((method.getParameters().length == 0)) {
                    method.invoke(socket);
                } else {
                    method.invoke(socket, HttpParam.singleBuilder(ctx, httpRequest).getParams(method.getParameters(), socketRoute.getPathParamMap()));
                }
            }
            catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void setSocketRoute(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        if (ctx!=null && httpRequest!=null && httpRequest.uri()!=null) {
            AttributeKey<FullHttpRequest> requestAttributeKey = AttributeKey.newInstance("FullHttpRequest");
            ctx.channel().attr(requestAttributeKey).set(httpRequest);
        }
    }

    private Dispatcher.SocketRoute getSocketRoute(ChannelHandlerContext ctx) {
        AttributeKey<FullHttpRequest> requestAttributeKey = AttributeKey.newInstance("FullHttpRequest");
        Attribute<FullHttpRequest> fullHttpRequestAttribute = ctx.channel().attr(requestAttributeKey);
        if (fullHttpRequestAttribute==null || fullHttpRequestAttribute.get()==null) {
            return null;
        }
        return Dispatcher.getInstance().getSocketRoute(fullHttpRequestAttribute.get().uri());
    }

    private FullHttpRequest getHttpRequest(ChannelHandlerContext ctx) {
        AttributeKey<FullHttpRequest> requestAttributeKey = AttributeKey.newInstance("FullHttpRequest");
        Attribute<FullHttpRequest> fullHttpRequestAttribute =  ctx.channel().attr(requestAttributeKey);
        if (fullHttpRequestAttribute==null) {
            return null;
        }
        return fullHttpRequestAttribute.get();
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, HttpResponse res) {
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static String getWebSocketLocation(ChannelPipeline cp, HttpRequest req, String path) {
        String protocol = "ws";
        if (cp.get(SslHandler.class) != null) {
            // SSL in use so use Secure WebSockets
            protocol = "wss";
        }
        String host = req.headers().get(HttpHeaderNames.HOST);
        return protocol + "://" + host + path;
    }

    private void applyHandshakeTimeout(ChannelHandlerContext ctx) {
        final ChannelPromise localHandshakePromise = ctx.newPromise();
        final long handshakeTimeoutMillis = 6000000L;
        if (handshakeTimeoutMillis <= 0 || localHandshakePromise.isDone()) {
            return;
        }

        final Future<?> timeoutFuture = ctx.executor().schedule(new Runnable() {
            @Override
            public void run() {
                if (!localHandshakePromise.isDone() &&
                        localHandshakePromise.tryFailure(new WebSocketServerHandshakeException("handshake timed out"))) {
                    ctx.flush()
                            .fireUserEventTriggered(WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_TIMEOUT)
                            .close();
                }
            }
        }, handshakeTimeoutMillis, TimeUnit.MILLISECONDS);

        // Cancel the handshake timeout when handshake is finished.
        localHandshakePromise.addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(Future<Void> f) {
                timeoutFuture.cancel(false);
            }
        });
    }
}

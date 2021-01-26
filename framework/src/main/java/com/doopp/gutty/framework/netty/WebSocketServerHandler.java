package com.doopp.gutty.framework.netty;

import com.doopp.gutty.framework.Dispatcher;
import com.doopp.gutty.framework.HttpParam;
import com.google.inject.Injector;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

    private final static Logger logger = LoggerFactory.getLogger(WebSocketServerHandler.class);

    private final Injector injector;

    public WebSocketServerHandler(Injector injector) {
        this.injector = injector;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            this.onConnect(ctx, (FullHttpRequest) msg);
        }
        callSocketMethod(ctx, msg);
    }

    private void onConnect(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        // is websocket
        if (httpRequest.headers().containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.UPGRADE, true)) {
            // Handshake
            WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                    getWebSocketLocation(httpRequest), null, true, 5 * 1024 * 1024);
            WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(httpRequest);
            if (handshaker == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            } else {
                handshaker.handshake(ctx.channel(), httpRequest);
            }
            return;
        }
        ctx.fireChannelRead(httpRequest.retain());
    }

    private static String getWebSocketLocation(FullHttpRequest req) {
        String location =  req.headers().get(HttpHeaderNames.HOST) + req.uri();
        return "ws://" + location;
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
            ctx.channel().attr(requestAttributeKey).set(httpRequest.retain());
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
        return fullHttpRequestAttribute.get().retain();
    }
}

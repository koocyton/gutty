package com.doopp.gutty.demo.socket;

import com.doopp.gutty.framework.annotation.websocket.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;

@Socket
@Path("/ws/game")
public class HelloSocket {

    private static final Logger logger = LoggerFactory.getLogger(HelloSocket.class);

    @Open
    public void onConnect(FullHttpRequest httpRequest) {
        // logger.info("httpRequest {}", httpRequest);
    }

    @TextMessage
    public void onTextMessage(FullHttpRequest httpRequest, TextWebSocketFrame textFrame) {
        logger.info("textFrame {}", textFrame.text());
    }

    @BinaryMessage
    public void onBinaryMessage(BinaryWebSocketFrame binaryWebSocketFrame) {
        logger.info("binaryFrame {}", binaryWebSocketFrame.content());
    }

    @Message
    public void onMessage(WebSocketFrame webSocketFrame) {
        logger.info("onMessage : {}", webSocketFrame.getClass());
    }

    @Ping
    public void onPing() {

    }

    @Pong
    public void onPong() {

    }

    @Close
    public void onClose() {
    }
}

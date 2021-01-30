package com.doopp.gutty.demo.socket;

import com.doopp.gutty.annotation.websocket.*;
import com.doopp.gutty.demo.pojo.User;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
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

    //@TextMessage
    //public void onTextMessage(FullHttpRequest httpRequest, TextWebSocketFrame textFrame) {
    //    logger.info("textFrame {}", textFrame.text());
    //}

    @TextMessage
    public void onJsonMessage(@JsonFrame User user) {
        logger.info("user {}", user.getName());
    }

    @BinaryMessage
    public void onBinaryMessage(BinaryWebSocketFrame binaryWebSocketFrame) {
        logger.info("binaryFrame {}", binaryWebSocketFrame.content());
    }

    @BinaryMessage
    public void onProtobufMessage(@ProtobufFrame User user) {
        logger.info("user {}", user);
    }

    //@Message
    //public void onMessage(WebSocketFrame webSocketFrame) {
        // logger.info("onMessage : {}", webSocketFrame.getClass());
    //}

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

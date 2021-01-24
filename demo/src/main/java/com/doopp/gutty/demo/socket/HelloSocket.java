package com.doopp.gutty.demo.socket;

import com.doopp.gutty.framework.annotation.websocket.*;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Socket("/ws/game")
public class HelloSocket {

    private static final Logger logger = LoggerFactory.getLogger(HelloSocket.class);

    @Open
    public void onConnect(FullHttpRequest httpRequest) {
        logger.info("httpRequest {}", httpRequest);
    }

    @TextMessage
    public void onTextMessage() {

    }

    @BinaryMessage
    public void onBinaryMessage() {

    }

    @Message
    public void onMessage() {

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

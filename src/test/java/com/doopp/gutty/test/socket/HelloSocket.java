package com.doopp.gutty.test.socket;

import com.doopp.gutty.annotation.websocket.*;
import com.doopp.gutty.test.pojo.User;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;

@Socket()
@Path("/ws/game")
public class HelloSocket {

    private static final Logger logger = LoggerFactory.getLogger(HelloSocket.class);

    @Open
    public void onConnect(Channel channel) {
        channel.writeAndFlush(new TextWebSocketFrame("you connected"));
    }

    @TextMessage
    public void onTextMessage(Channel channel) {
        channel.writeAndFlush(new TextWebSocketFrame("hello"));
    }

    @TextMessage
    public void onJsonMessage(@JsonFrame User user) {
        logger.info("user {}", user.getNickName());
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

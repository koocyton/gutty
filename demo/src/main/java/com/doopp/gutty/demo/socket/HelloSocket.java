package com.doopp.gutty.demo.socket;

import com.doopp.gutty.framework.annotation.websocket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Socket("/ws/game")
public class HelloSocket {

    private static final Logger logger = LoggerFactory.getLogger(HelloSocket.class);

    @Open
    public void onConnect0() {
    }

    @Message
    public void onMessage() {

    }

    @Close
    public void onClose() {
    }
}

package com.doopp.gutty.annotation.websocket;

import javax.ws.rs.HttpMethod;
import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod(HttpMethod.GET)
@Documented
public @interface BinaryMessage {
}


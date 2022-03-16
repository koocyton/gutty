package com.doopp.gutty.annotation.websocket;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Socket {

    String subprotocol() default "";
}


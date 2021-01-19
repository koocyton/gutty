package com.doopp.gutty.framework.annotation.websocket;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Socket {

    String value() default "";
}


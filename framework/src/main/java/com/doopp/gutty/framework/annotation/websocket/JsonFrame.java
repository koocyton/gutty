package com.doopp.gutty.framework.annotation.websocket;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonFrame {
}


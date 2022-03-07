package com.doopp.gutty.annotation;

import com.google.protobuf.Message;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProtobufParam {

    Class<Message> message();
}


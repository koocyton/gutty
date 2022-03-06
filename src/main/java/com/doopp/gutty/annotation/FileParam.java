package com.doopp.gutty.annotation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FileParam {

    String value();

    String path() default "";

    String suffix() default "";

    long maximum() default 10000000; // 10M

    boolean required() default false;
}


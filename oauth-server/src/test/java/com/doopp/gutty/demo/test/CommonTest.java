package com.doopp.gutty.demo.test;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonTest {

    private final static Logger logger = LoggerFactory.getLogger(CommonTest.class);

    @Test
    public void websocketClient() {
        logger.info("hello {}", System.currentTimeMillis());
    }
}

package com.doopp.gutty.framework.view;

import java.util.concurrent.ConcurrentHashMap;

public class ModelMap extends ConcurrentHashMap<String, Object> {

    public void addAttribute(String attributeName, Object attributeValue) {
        this.put(attributeName, attributeValue);
    }
}

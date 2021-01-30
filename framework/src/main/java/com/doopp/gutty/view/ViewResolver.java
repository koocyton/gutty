package com.doopp.gutty.view;

public interface ViewResolver {

    String template(ModelMap modelMap, String templateName);
}

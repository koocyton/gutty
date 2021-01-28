package com.doopp.gutty.framework.view;

public interface ViewResolver {

    String template(ModelMap modelMap, String templateName);
}

package com.doopp.gutty.view;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

public class ThymeleafViewResolver implements ViewResolver {


    // private FileTemplateResolver templateResolver = templateResolver();

    @Override
    public String template(ModelMap modelMap, String templateName) {

        FileTemplateResolver templateResolver = templateResolver();

        // String controllerName = handleObject.getClass().getSimpleName();
        // String templateDirectory = controllerName.toLowerCase().substring(0, controllerName.length() - "handle".length());
        templateResolver.setPrefix(this.getClass().getResource("/template/").getPath() + "/");

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(templateResolver);

        Context context = new Context();
        context.setVariables(modelMap);

        return engine.process(templateName, context);
    }

    // 配置模版
    private FileTemplateResolver templateResolver() {
        FileTemplateResolver templateResolver = new FileTemplateResolver();
        // templateResolver.setPrefix(this.getClass().getResource("/template/" + templateDirectory).getPath() + "/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCacheable(false);
        templateResolver.setTemplateMode("HTML5");
        return templateResolver;
    }
}

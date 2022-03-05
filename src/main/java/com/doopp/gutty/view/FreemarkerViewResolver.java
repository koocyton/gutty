package com.doopp.gutty.view;

import freemarker.template.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class FreemarkerViewResolver implements ViewResolver {

    // private Configuration configuration = templateConfiguration();

    // 输出模版
    @Override
    public String template(ModelMap modelMap, String templateName) {
        Configuration configuration = templateConfiguration();
        try {
            Template template = configuration.getTemplate(templateName.replace(".", "/") + ".html");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            template.process(modelMap, new OutputStreamWriter(outputStream));
            return outputStream.toString("UTF-8");
        }
        catch(IOException | TemplateException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    // 配置模版
    private Configuration templateConfiguration() {
        Version version = new Version("2.3.28");
        DefaultObjectWrapperBuilder defaultObjectWrapperBuilder = new DefaultObjectWrapperBuilder(version);

        Configuration cfg = new Configuration(version);
        cfg.setObjectWrapper(defaultObjectWrapperBuilder.build());
        cfg.setDefaultEncoding("UTF-8");
        // Don't log exceptions inside FreeMarker that it will thrown at you anyway:
        cfg.setLogTemplateExceptions(false);
        // Sets how errors will appear. Here we assume we are developing HTML pages.
        // For production systems TemplateExceptionHandler.RETHROW_HANDLER is better.
        // During web page *development* TemplateExceptionHandler.HTML_DEBUG_HANDLER is better.
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        cfg.setClassForTemplateLoading(this.getClass(), "/template");
        // Bind instance for DI
        // bind(Configuration.class).toInstance(cfg);
        return cfg;
    }
}

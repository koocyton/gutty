package com.doopp.gutty.test;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.Value;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.List;

public class SwaggerModule extends AbstractModule {

    @Provides
    @Singleton
    public Docket adminApi() {

        ParameterBuilder ticketPar = new ParameterBuilder();
        ticketPar.name("Admin-Token")
                .description("Header : Admin token")
                .defaultValue("clientTicket")
                .modelRef(new ModelRef("string"))
                .parameterType("header")
                .required(false).build();

        ParameterBuilder ticketPar2 = new ParameterBuilder();
        ticketPar2.name("Authentication") // Authentication
                .description("Header : Authentication")
                .defaultValue("clientTicket")
                .modelRef(new ModelRef("string"))
                .parameterType("header")
                .required(false).build();

        List<Parameter> pars = new ArrayList<>();
        pars.add(ticketPar.build());
        pars.add(ticketPar2.build());

        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .build()
                .globalOperationParameters(pars)
                .apiInfo(apiInfo());
    }


    private ApiInfo apiInfo() {
        Contact contact = new Contact(
                "liuyi",
                "https://avglife.cn",
                "liuyi@avglife.cn"
        );
        return new ApiInfoBuilder()
                .title("API接口")
                .description("API接口")
                .contact(contact)
                .version("1.0")
                .build();
    }
}

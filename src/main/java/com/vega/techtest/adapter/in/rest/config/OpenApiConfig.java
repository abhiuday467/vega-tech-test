package com.vega.techtest.adapter.in.rest.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Transaction API",
                version = "v1",
                description = "API for submitting and querying transactions.",
                contact = @Contact(name = "Vega Tech Test"),
                license = @License(name = "UNLICENSED")
        )
)
public class OpenApiConfig {
}

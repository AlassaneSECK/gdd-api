package alassane.seck.gddapi.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gddApiOpenAPI() {
        OpenAPI openApi = new OpenAPI()
                .info(new Info()
                        .title("GDD API")
                        .description("Gestion des depenses quotidiennes - documentation OpenAPI generee automatiquement.")
                        .version("v1")
                        .contact(new Contact().name("GDD API Team").email("support@gdd.local"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
        openApi.addServersItem(new Server().url("/").description("Serveur courant"));
        return openApi;
    }

    @Bean
    public GroupedOpenApi gddApiGroup() {
        return GroupedOpenApi.builder()
                .group("gdd-api")
                .packagesToScan("alassane.seck.gddapi.controller")
                .build();
    }
}

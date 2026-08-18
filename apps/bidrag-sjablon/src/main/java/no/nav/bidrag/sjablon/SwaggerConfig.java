package no.nav.bidrag.sjablon;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerConfig implements WebMvcConfigurer {

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("bidrag-sjablon API")
                .description("Mikrotjeneste for sjabloner")
                .version("v0.0.1"))
        .externalDocs(
            new ExternalDocumentation()
                .description("Team Bidrag")
                .url("https://confluence.adeo.no/display/ITTB/Team+Bidrag"));
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/").setViewName("forward:/swagger-ui.html");
  }
}

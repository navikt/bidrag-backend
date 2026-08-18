package no.nav.bidrag.maskinportenclient.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({MaskinportenConfig.class})
@SecurityScheme(bearerFormat = "JWT", name = "bearer-key", scheme = "bearer", type = SecuritySchemeType.HTTP)
@OpenAPIDefinition(
    info = @Info(title = "Bidrag Maskinporten Client", version = "0.2", description = "Tilbyr Maskinporten-tokens for Team Bidrag"),
    security = {@SecurityRequirement(name = "bearer-key")})
public class MaskinportenClientConfiguration { }

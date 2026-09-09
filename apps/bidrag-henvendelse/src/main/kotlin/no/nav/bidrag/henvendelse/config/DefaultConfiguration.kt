package no.nav.bidrag.henvendelse.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import no.nav.bidrag.commons.web.DefaultCorsFilter
import no.nav.bidrag.commons.web.MdcFilter
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import

/**
 * `@SecurityScheme` må stå her for at Swagger-UI skal få en Authorize-knapp. Endepunktene
 * viser til skjemaet med `SecurityRequirement(name = "bearer-key")`, men uten definisjonen
 * er det ingen måte å lime inn et token i UI-et - og da kan ingen prøve APIet derfra.
 */
@EnableAspectJAutoProxy
@Configuration
@EnableOAuth2Client(cacheEnabled = true)
@Import(DefaultCorsFilter::class, MdcFilter::class)
@OpenAPIDefinition(
    info = Info(title = "bidrag-henvendelse", version = "v1"),
    security = [SecurityRequirement(name = "bearer-key")],
)
@SecurityScheme(bearerFormat = "JWT", name = "bearer-key", scheme = "bearer", type = SecuritySchemeType.HTTP)
class DefaultConfiguration

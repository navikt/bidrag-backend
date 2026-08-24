package no.nav.bidrag.dokument.bestilling.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.DefaultCorsFilter
import no.nav.bidrag.commons.web.UserMdcFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.web.filter.CorsFilter

@EnableAspectJAutoProxy
@OpenAPIDefinition(info = Info(title = "bidrag-dokument-bestilling", version = "v1"), security = [SecurityRequirement(name = "bearer-key")])
@SecurityScheme(bearerFormat = "JWT", name = "bearer-key", scheme = "bearer", type = SecuritySchemeType.HTTP)
@Configuration
@Import(AppContext::class)
class DefaultConfig {
    @Bean
    fun corsFilter(): CorsFilter = DefaultCorsFilter()

    @Bean
    fun correlationIdFilter(): CorrelationIdFilter = CorrelationIdFilter()

    @Bean
    fun mdcFilter(): UserMdcFilter = UserMdcFilter()
}

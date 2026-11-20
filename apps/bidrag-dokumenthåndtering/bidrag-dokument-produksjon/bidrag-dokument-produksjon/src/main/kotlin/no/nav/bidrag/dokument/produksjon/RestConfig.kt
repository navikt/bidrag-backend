package no.nav.bidrag.dokument.produksjon

import no.nav.bidrag.commons.service.AppContext
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.boot.restclient.autoconfigure.RestTemplateBuilderConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import java.time.Duration
import java.time.temporal.ChronoUnit

@Configuration
@Import(AppContext::class)
class RestConfig {
    @Bean
    @Primary
    fun restTemplateBuilder(configurer: RestTemplateBuilderConfigurer): RestTemplateBuilder = configurer
        .configure(RestTemplateBuilder())
        .connectTimeout(Duration.of(30, ChronoUnit.SECONDS))
        .readTimeout(Duration.of(120, ChronoUnit.SECONDS))
}

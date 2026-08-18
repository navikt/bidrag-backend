package no.nav.bidrag.organisasjon

import no.nav.bidrag.commons.web.CorrelationIdFilter
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateConfiguration {
    @Bean
    @Scope("prototype")
    fun restTemplate(): RestTemplate = RestTemplateBuilder()
        .additionalInterceptors(
            { request, body, execution ->
                request.headers.add(CorrelationIdFilter.CORRELATION_ID_HEADER, CorrelationIdFilter.fetchCorrelationIdForThread())
                execution.execute(request, body)
            },
        ).build()
}

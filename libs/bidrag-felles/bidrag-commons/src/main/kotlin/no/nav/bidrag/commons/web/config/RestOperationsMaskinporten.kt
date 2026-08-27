package no.nav.bidrag.commons.web.config

import no.nav.bidrag.commons.web.interceptor.MaskinportenBearerTokenClientInterceptor
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import java.time.Duration

@Configuration
@Import(
    RestTemplateBuilderBean::class,
    MaskinportenBearerTokenClientInterceptor::class,
)
class RestOperationsMaskinporten {
    @Bean("maskinporten")
    @Scope("prototype")
    fun restOperationsMaskinporten(
        restTemplateBuilder: RestTemplateBuilder,
        maskinportenBearerTokenClientInterceptor: MaskinportenBearerTokenClientInterceptor,
    ) = restTemplateBuilder
        .additionalInterceptors(maskinportenBearerTokenClientInterceptor)
        .readTimeout(Duration.ofMinutes(1))
        .build()
}

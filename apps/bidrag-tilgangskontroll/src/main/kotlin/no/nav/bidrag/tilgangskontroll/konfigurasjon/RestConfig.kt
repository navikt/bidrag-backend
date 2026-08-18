package no.nav.bidrag.tilgangskontroll.konfigurasjon

import no.nav.bidrag.commons.web.config.ObjectmapperBuilder
import no.nav.bidrag.commons.web.config.RestTemplateBuilderBean
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention
import org.springframework.web.client.RestTemplate

@Configuration
@Import(RestTemplateBuilderBean::class, ObjectmapperBuilder::class)
class RestConfig {
    @Bean
    fun clientRequestObservationConvention() = DefaultClientRequestObservationConvention()

    @Bean
    @Scope("prototype")
    fun baseRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate = restTemplateBuilder.build()
}

package no.nav.bidrag.person

import no.nav.bidrag.commons.CorrelationId.Companion.generateTimestamped
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.observation.ClientRequestObservationConvention
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention
import org.springframework.web.util.DefaultUriBuilderFactory

@Configuration
@EnableSecurityConfiguration
class RestTemplateConfiguration {
    @Bean
    @Scope("prototype")
    @Qualifier("base")
    fun restTemplate() = HttpHeaderRestTemplate().apply {
        addHeaderGenerator(CorrelationIdFilter.CORRELATION_ID_HEADER) { CorrelationIdFilter.fetchCorrelationIdForThread() }
    }

    @Bean
    @Qualifier("krr")
    @Scope("prototype")
    fun krrRestTemplate(
        @Qualifier("base") httpHeaderRestTemplate: HttpHeaderRestTemplate,
        @Value("\${KRR_URL}") digdirKrrUrl: String,
        securityTokenService: SecurityTokenService,
    ) = httpHeaderRestTemplate.apply {
        httpHeaderRestTemplate.interceptors.add(securityTokenService.serviceUserAuthTokenInterceptor("krr")!!)
        httpHeaderRestTemplate.uriTemplateHandler = DefaultUriBuilderFactory(digdirKrrUrl)
        httpHeaderRestTemplate.addHeaderGenerator("Nav-Call-Id") { generateTimestamped("bidrag-person").get() }
    }

    @Bean
    @Qualifier("kontoregister")
    @Scope("prototype")
    fun kontoregisterRestTemplate(
        @Qualifier("base") httpHeaderRestTemplate: HttpHeaderRestTemplate,
        @Value("\${KONTOREGISTER_URL}") kontoregisterUrl: String,
        securityTokenService: SecurityTokenService,
    ) = httpHeaderRestTemplate.apply {
        httpHeaderRestTemplate.interceptors.add(securityTokenService.clientCredentialsTokenInterceptor("kontoregister")!!)
        httpHeaderRestTemplate.uriTemplateHandler = DefaultUriBuilderFactory(kontoregisterUrl)
        httpHeaderRestTemplate.addHeaderGenerator("Nav-Call-Id") { generateTimestamped("bidrag-person").get() }
    }

    @Bean
    @Qualifier("pdl")
    @Scope("prototype")
    fun pdlRestTemplate(
        @Value("\${PDL_URL}") pdlUrl: String,
        securityTokenService: SecurityTokenService,
        @Qualifier("base") httpHeaderRestTemplate: HttpHeaderRestTemplate,
    ) = httpHeaderRestTemplate.apply {
        addHeaderGenerator(HttpHeaders.CONTENT_TYPE) { MediaType.APPLICATION_JSON_VALUE }
        addHeaderGenerator("BEHANDLINGSNUMMER") { "B106" }
        addHeaderGenerator("TEMA") { "BID" }
        uriTemplateHandler = DefaultUriBuilderFactory(pdlUrl)
        interceptors.add(securityTokenService.authTokenInterceptor("pdl"))
    }

    @Bean
    fun clientRequestObservationConvention(): ClientRequestObservationConvention = DefaultClientRequestObservationConvention()
}

package no.nav.bidrag.bidragskalkulator.config

import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.util.Timeout
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.restclient.RestTemplateCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

@Configuration
@Import(RestOperationsAzure::class, AppContext::class, SikkerhetsKontekst::class)
class RestConfig {

    @Bean
    fun defaultRestTemplateCustomizer(): RestTemplateCustomizer = RestTemplateCustomizer { restTemplate ->
        val factory = restTemplate.requestFactory
        if (factory is HttpComponentsClientHttpRequestFactory) {
            // HttpComponentsClientHttpRequestFactory mistet setConnectTimeout i Spring 7 - konfigureres nå via ConnectionConfig på HttpClient-nivå.
            val connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(
                    ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(30_000))
                        .build(),
                )
                .build()
            factory.setHttpClient(HttpClients.custom().setConnectionManager(connectionManager).build())
            factory.setReadTimeout(30_000)
        }
    }

    @Bean
    fun restTemplateWithInterceptor(
        @Qualifier("azure") azureRestTemplate: RestTemplate,
        securityTokenService: SecurityTokenService,
    ): RestTemplate {
        azureRestTemplate.interceptors.add(securityTokenService.navConsumerTokenInterceptor()!!)
        return azureRestTemplate
    }
}

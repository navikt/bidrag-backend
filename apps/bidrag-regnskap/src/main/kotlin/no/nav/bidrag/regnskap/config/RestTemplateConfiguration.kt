package no.nav.bidrag.regnskap.config

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.HttpClientSettings
import org.springframework.boot.restclient.observation.ObservationRestTemplateCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.observation.ClientRequestObservationConvention
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.time.Duration

private val LOGGER = KotlinLogging.logger { }

@Configuration
@EnableSecurityConfiguration
@Import(RestOperationsAzure::class)
class RestTemplateConfiguration {

    @Bean
    fun kotlinModule(): KotlinModule = KotlinModule.Builder().build()

    @Bean("regnskap")
    @Scope("prototype")
    fun baseRestTemplate(
        @Value($$"${NAIS_APP_NAME}") naisAppName: String,
        observationRestTemplateCustomizer: ObservationRestTemplateCustomizer,
    ): RestTemplate {
        val restTemplate = HttpHeaderRestTemplate()
        restTemplate.requestFactory = ClientHttpRequestFactoryBuilder.httpComponents().build(
            HttpClientSettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(30))
                .withReadTimeout(Duration.ofSeconds(60)),
        )
        restTemplate.withDefaultHeaders()
        restTemplate.addHeaderGenerator("Nav-Callid") { CorrelationId.fetchCorrelationIdForThread() }
        restTemplate.addHeaderGenerator("Nav-Consumer-Id") { naisAppName }
        restTemplate.interceptors.add(KravApiRequestInterceptor())
        observationRestTemplateCustomizer.customize(restTemplate)
        return restTemplate
    }

    @Bean
    fun clientRequestObservationConvention(): ClientRequestObservationConvention = DefaultClientRequestObservationConvention()
}

class KravApiRequestInterceptor : ClientHttpRequestInterceptor {
    @Throws(IOException::class)
    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        if (request.uri.path.contains(SkattConsumer.KRAV_PATH)) {
            logRequest(request, body)
        }
        return execution.execute(request, body)
    }

    private fun logRequest(request: HttpRequest, body: ByteArray?) {
        val bodySize = body?.size ?: 0
        LOGGER.debug { "Request URI: ${request.uri}, Method: ${request.method}, Body Size: $bodySize bytes" }
    }
}

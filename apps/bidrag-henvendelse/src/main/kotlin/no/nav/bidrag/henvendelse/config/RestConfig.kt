package no.nav.bidrag.henvendelse.config

import no.nav.bidrag.commons.logging.audit.AuditLogger
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.tilgang.TilgangClient
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.client.observation.ClientRequestObservationConvention
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention
import org.springframework.web.client.RestTemplate

/**
 * `azure`-RestTemplaten fra [RestOperationsAzure] veksler innkommende brukertoken til et
 * on-behalf-of-token per utgående kall, og faller tilbake til client-credentials når kallet
 * har opphav i applikasjonen selv (scheduler, ping). sf-henvendelse-api-proxy avviser
 * maskintoken med 403 utenfor `/kodeverk/`, så henvendelseskallene *må* skje on-behalf-of.
 */
@Configuration
@EnableSecurityConfiguration
@Import(RestOperationsAzure::class, TilgangClient::class, AuditLogger::class)
class RestConfig {
    @Bean
    fun clientRequestObservationConvention(): ClientRequestObservationConvention = DefaultClientRequestObservationConvention()

    /**
     * Egen template for henvendelseskallene, som `azure` pluss [KorrelasjonsIdInterceptor].
     * Interceptoren må registreres etter dem bidrag-commons legger på, siden den overskriver
     * `X-Correlation-ID` de allerede har lagt til - se interceptoren for detaljer.
     *
     * `azure`-bønnen er prototype-scoped (se `@Scope("prototype")` i [RestOperationsAzure]),
     * så instansen vi får her er vår egen å endre på.
     */
    @Bean(BEAN_HENVENDELSE_REST_TEMPLATE)
    fun henvendelseRestTemplate(
        @Qualifier("azure") azureRestTemplate: RestTemplate,
    ): RestTemplate = azureRestTemplate.apply { interceptors.add(KorrelasjonsIdInterceptor()) }

    companion object {
        const val BEAN_HENVENDELSE_REST_TEMPLATE = "henvendelseRestTemplate"
    }
}

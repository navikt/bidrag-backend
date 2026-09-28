package no.nav.bidrag.henvendelse.config

import no.nav.bidrag.commons.logging.audit.AuditLogger
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.tilgang.TilgangClient
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.client.observation.ClientRequestObservationConvention
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention

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
}

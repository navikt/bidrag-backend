package no.nav.bidrag.dokument.journalpost.configuration

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import no.nav.bidrag.commons.ExceptionLogger
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.security.service.OidcTokenManager
import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.commons.web.UserMdcFilter
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpost
import no.nav.bidrag.dokument.journalpost.UrlsForApplication
import no.nav.bidrag.dokument.journalpost.aop.ExceptionHandlerAdvice
import no.nav.bidrag.dokument.journalpost.consumer.BidragPersonConsumer
import no.nav.bidrag.dokument.journalpost.consumer.BrevserverConsumer
import no.nav.bidrag.dokument.journalpost.consumer.NorgConsumer
import no.nav.bidrag.dokument.journalpost.consumer.OppgaveConsumer
import no.nav.bidrag.dokument.journalpost.consumer.SaksbehandlerConsumer
import no.nav.bidrag.dokument.journalpost.dokument.DokumentConsumer
import no.nav.bidrag.dokument.journalpost.dokument.DokumentTilgangConsumer
import no.nav.bidrag.dokument.journalpost.exception.SaksbehandlerIkkeFunnetITokenException
import no.nav.bidrag.dokument.journalpost.model.Discriminator
import no.nav.bidrag.dokument.journalpost.model.ResourceByDiscriminator
import no.nav.bidrag.dokument.journalpost.repository.JournalpostRepository
import no.nav.bidrag.dokument.journalpost.service.DokumentService
import no.nav.bidrag.dokument.journalpost.service.JournalpostService
import no.nav.bidrag.dokument.journalpost.service.SakService
import no.nav.bidrag.dokument.journalpost.service.TokenInformationService
import no.nav.bidrag.dokument.journalpost.service.manager.EndreJournalpostManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.Scope
import org.springframework.core.annotation.Order
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention
import org.springframework.jms.core.JmsTemplate
import org.springframework.web.util.DefaultUriBuilderFactory

@Configuration
@PropertySource("classpath:url.properties")
@OpenAPIDefinition(
    info = Info(title = "bidrag-dokument-journalpost", version = "v1"),
    security = [SecurityRequirement(name = "bearer-key")],
)
@SecurityScheme(bearerFormat = "JWT", name = "bearer-key", scheme = "bearer", type = SecuritySchemeType.HTTP)
@EnableSecurityConfiguration
@Import(RestOperationsAzure::class)
class BidragDokumentJournalpostConfig {
    @Bean
    fun clientRequestObservationConvention() = DefaultClientRequestObservationConvention()

    @Bean
    fun dokumentService(
        @Value($$"${BREVSERVER_URL}") brevserverUrl: String,
        @Value($$"${BREVKLIENT_USERNAME}") systemId: String,
        journalpostService: JournalpostService,
        dokumentTilgangConsumer: DokumentTilgangConsumer,
        dokumentConsumer: DokumentConsumer,
        brevserverConsumer: BrevserverConsumer,
        @Value($$"${BRUK_BREVSERVER_REST:false}") brukBrevserverRest: Boolean,
    ): DokumentService = DokumentService(
        brevserverUrl,
        systemId,
        journalpostService,
        dokumentTilgangConsumer,
        dokumentConsumer,
        brevserverConsumer,
        brukBrevserverRest,
    )

    @Bean
    fun bidragPersonConsumer(
        httpHeaderRestTemplate: HttpHeaderRestTemplate,
        @Value($$"${BIDRAG_PERSON_URL}") bidragPersonUrl: String,
        securityTokenService: SecurityTokenService,
    ): BidragPersonConsumer {
        httpHeaderRestTemplate.interceptors.add(securityTokenService.serviceUserAuthTokenInterceptor("person")!!)
        httpHeaderRestTemplate.uriTemplateHandler = DefaultUriBuilderFactory(bidragPersonUrl)
        LOGGER.info("BidragPersonConsumer med base  url: $bidragPersonUrl")
        return BidragPersonConsumer(httpHeaderRestTemplate)
    }

    @Bean
    fun dokumentTilgangConsumer(
        @Value($$"${BREVKLIENT_USERNAME}") systemId: String,
        @Value($$"${BREVKLIENT_PASSWORD}") systemPassword: String,
        jmsTemplate: JmsTemplate?,
        saksbehandlerOidcTokenManager: SaksbehandlerOidcTokenManager?,
    ): DokumentTilgangConsumer = DokumentTilgangConsumer(systemId, systemPassword, jmsTemplate, saksbehandlerOidcTokenManager)

    @Bean
    @Order(1)
    fun correlationIdFilter() = CorrelationIdFilter()

    @Bean
    @Order(2)
    fun enhetFilter() = EnhetFilter()

    @Bean
    fun userMdcFilter() = UserMdcFilter()

    @Bean
    fun saksbehandlerOidcTokenManager(oidcTokenManager: OidcTokenManager): SaksbehandlerOidcTokenManager = object : SaksbehandlerOidcTokenManager {
        override fun hentSaksbehandler(): String = TokenUtils.hentBruker() ?: throw SaksbehandlerIkkeFunnetITokenException("Fant ikke saksbehandler i token")

        override fun erSystemBruker(): Boolean = TokenUtils.erApplikasjonsbruker()
    }

    @Bean
    fun oppgaveConsumers(
        oppgaveConsumer: OppgaveConsumer,
        oppgaveConsumerForServiceBruker: OppgaveConsumer,
        securityTokenService: SecurityTokenService,
    ): ResourceByDiscriminator<OppgaveConsumer> {
        oppgaveConsumer.leggTilSikkerhet(securityTokenService.authTokenInterceptor("oppgave"))
        oppgaveConsumerForServiceBruker.leggTilSikkerhet(securityTokenService.serviceUserAuthTokenInterceptor("oppgave"))
        val oppgaveConsumers = HashMap<Discriminator, OppgaveConsumer>()
        oppgaveConsumers[Discriminator.REGULAR_USER] = oppgaveConsumer
        oppgaveConsumers[Discriminator.SERVICE_USER] = oppgaveConsumerForServiceBruker
        return ResourceByDiscriminator(oppgaveConsumers)
    }

    @Bean
    fun norgConsumer(
        httpHeaderRestTemplate: HttpHeaderRestTemplate,
        @Value($$"${NORG2_API_V1_URL}") norgUrl: String,
    ): NorgConsumer {
        httpHeaderRestTemplate.uriTemplateHandler = DefaultUriBuilderFactory(norgUrl)
        LOGGER.info("NorgConsumer med base url: $norgUrl")
        return NorgConsumer(httpHeaderRestTemplate)
    }

    @Bean
    fun saksbehandlerConsumer(
        httpHeaderRestTemplate: HttpHeaderRestTemplate,
        @Value($$"${BIDRAG_ORGANISASJON_URL}") bidragOrganisasjonUrl: String,
        securityTokenService: SecurityTokenService,
    ): SaksbehandlerConsumer {
        httpHeaderRestTemplate.interceptors.add(securityTokenService.authTokenInterceptor("organisasjon"))
        httpHeaderRestTemplate.uriTemplateHandler = DefaultUriBuilderFactory(bidragOrganisasjonUrl)
        LOGGER.info("SaksbehandlerConsumer med base url: $bidragOrganisasjonUrl")
        return SaksbehandlerConsumer(httpHeaderRestTemplate)
    }

    @Bean
    fun urlsForApplication(
        @Value($$"${BIDRAG_ORGANISASJON_URL}") bidragOrganisasjonUrl: String,
        @Value($$"${NORG2_API_V1_URL}") norgUrl: String,
        @Value($$"${OPPGAVE_OPPGAVER_URL}") oppgaverUrl: String,
    ): UrlsForApplication = UrlsForApplication(bidragOrganisasjonUrl, norgUrl, oppgaverUrl)

    @Bean
    @Scope("prototype")
    fun endreJournalpostManager(
        applicationEventPublisher: ApplicationEventPublisher,
        journalpostRepository: JournalpostRepository,
        sakService: SakService,
        tokenInformationService: TokenInformationService,
    ): EndreJournalpostManager = EndreJournalpostManager(
        applicationEventPublisher,
        journalpostRepository,
        sakService,
        tokenInformationService,
    )

    interface SaksbehandlerOidcTokenManager {
        fun hentSaksbehandler(): String

        fun erSystemBruker(): Boolean
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(BidragDokumentJournalpost::class.java)
    }
}

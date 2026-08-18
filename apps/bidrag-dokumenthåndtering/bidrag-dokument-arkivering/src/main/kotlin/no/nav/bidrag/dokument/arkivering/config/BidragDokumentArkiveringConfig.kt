package no.nav.bidrag.dokument.arkivering.config

import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.SignedJWT
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.security.service.OidcTokenManager
import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.DefaultCorsFilter
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.commons.web.UserMdcFilter
import no.nav.bidrag.dokument.arkivering.config.BidragDokumentArkiveringConfig.SaksbehandlerOidcTokenManager
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import java.text.ParseException

private val log = KotlinLogging.logger {}

@Configuration
@EnableJwtTokenValidation(ignore = ["org.springdoc"])
@OpenAPIDefinition(
    info = Info(title = "bidrag-dokument-arkivering", version = "v1"),
    security = [SecurityRequirement(name = "bearer-key")],
)
@SecurityScheme(
    bearerFormat = "JWT",
    name = "bearer-key",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
)
@EnableAspectJAutoProxy
@EnableSecurityConfiguration
@Import(
    CorrelationIdFilter::class,
    EnhetFilter::class,
    UserMdcFilter::class,
    DefaultCorsFilter::class,
)
class BidragDokumentArkiveringConfig {
    @Bean
    fun timedAspect(registry: MeterRegistry?): TimedAspect = TimedAspect(registry!!)

    @Bean
    @Qualifier("base")
    @Scope("prototype")
    fun baseRestTemplate(): HttpHeaderRestTemplate {
        val httpHeaderRestTemplate = HttpHeaderRestTemplate()
        httpHeaderRestTemplate.requestFactory = HttpComponentsClientHttpRequestFactory()
        httpHeaderRestTemplate.withDefaultHeaders()
        return httpHeaderRestTemplate
    }

    @Bean
    fun saksbehandlerOidcTokenManager(oidcTokenManager: OidcTokenManager): SaksbehandlerOidcTokenManager = SaksbehandlerOidcTokenManager {
        hentSaksbehandler(
            oidcTokenManager.fetchTokenAsString(),
        )
    }

    fun interface SaksbehandlerOidcTokenManager {
        fun hentSaksbehandler(): String?
    }

    companion object {
        const val DOKUMENT_VARIANT_FORMAT_ARKIV = "ARKIV"
        const val DOKUMENT_FILTYPE_PDFA = "PDFA"
        const val BIDRAG_JOURNALPOSTSTATUS_RESERVERT = "R"
        const val BIDRAG_JOURNALPOSTSTATUS_KLAR_TIL_PRINT = "KP"

        fun hentSaksbehandler(idToken: String?): String {
            log.info { "Skal finne saksbehandler fra token" }
            return try {
                hentSaksbehandler(parseIdToken(idToken))
            } catch (e: Exception) {
                log.error(e) { "${"Klarte ikke parse token"}" }
                if (e is RuntimeException) {
                    throw e
                }
                throw IllegalArgumentException("Klarte ikke å parse token", e)
            }
        }

        private fun hentSaksbehandler(signedJWT: SignedJWT): String = try {
            signedJWT.jwtClaimsSet.subject
        } catch (e: ParseException) {
            throw IllegalStateException("Kunne ikke hente saksbehandler", e)
        }

        @Throws(ParseException::class)
        fun parseIdToken(idToken: String?): SignedJWT = JWTParser.parse(idToken) as SignedJWT
    }
}

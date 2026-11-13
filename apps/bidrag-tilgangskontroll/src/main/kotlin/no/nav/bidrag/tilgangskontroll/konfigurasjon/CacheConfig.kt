package no.nav.bidrag.tilgangskontroll.konfigurasjon

import no.nav.bidrag.cache.nøkkelgenerering.CacheBrukerIdentifikator
import no.nav.bidrag.commons.security.utils.TokenUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class CacheConfig {
    companion object {
        const val SYSTEMBRUKER_ID = "SYSTEM"
    }

    @Bean
    fun cacheBrukerIdentifikator() = CacheBrukerIdentifikator {
        if (TokenUtils.erApplikasjonsbruker()) {
            SYSTEMBRUKER_ID
        } else {
            checkNotNull(TokenUtils.hentSaksbehandlerIdent()) { "Mangler brukeridentitet for brukerbasert cache" }
        }
    }
}

/** Navn og eventuelle egne levetider for cachene. */
object Cache {
    const val PIP_SAK = "PIP_SAK"
    const val PIP_SAK_LEVETID = "10s"
    const val TILGANGSMASKIN_KJERNEREGLER = "TILGANGSMASKIN_KJERNEREGLER"
    const val TILGANGSMASKIN_KJERNEREGLER_BULK = "TILGANGSMASKIN_KJERNEREGLER_BULK"
    const val TILGANGSMASKIN_KOMPLETTEREGLER = "TILGANGSMASKIN_KOMPLETTEREGLER"
    const val TILGANGSMASKIN_KOMPLETTEREGLER_BULK = "TILGANGSMASKIN_KOMPLETTEREGLER_BULK"
    const val BRUKERINFO = "BRUKERINFO"
    const val BRUKERGRUPPER = "BRUKERGRUPPER"
    const val BRUKERE_FOR_ENHET = "BRUKERE_FOR_ENHET"
    const val GRUPPE_DETALJER = "GRUPPE_DETALJER"
    const val GRUPPE_DETALJER_TEMA = "GRUPPE_DETALJER_TEMA"
    const val BRUKERENHETER = "BRUKERENHETER"
}

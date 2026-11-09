package no.nav.bidrag.tilgangskontroll.konfigurasjon

import no.nav.bidrag.cache.konfigurasjon.CacheSettings
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration(proxyBeanMethods = false)
@Profile("nais", "lokal-nais")
class CacheKonfigurasjon {
    private companion object {
        // Bare validerte applikasjonskall bruker denne identiteten.
        const val SYSTEMBRUKER_ID = "SYSTEM"
    }

    @Bean
    fun cacheSettings(@Value($$"${CACHE_VALKEY_INSTANCE}") instans: String): CacheSettings = CacheSettings(
        valkeyInstans = instans,
        cacher = Cache.innstillinger,
        brukerId = {
            if (TokenUtils.erApplikasjonsbruker()) SYSTEMBRUKER_ID else TokenUtils.hentSaksbehandlerIdent()
        },
        // Personident finnes ikke i bidrag-cache og bør derfor legges inn med ønsker prefix her
        nøkkelprefix = { verdi ->
            when (verdi) {
                is Saksnummer -> "saksnummer:${verdi.verdi}"
                is Personident -> "personident:${verdi.verdi}"
                else -> null
            }
        },
    )
}

package no.nav.bidrag.sak.service

import no.nav.bidrag.commons.cache.BrukerCacheable
import no.nav.bidrag.commons.tilgang.TilgangClient
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.sak.domain.Tilgang
import org.springframework.stereotype.Component

@Component
class Tilgangskontroll(
    private val tilgangClient: TilgangClient,
) {
    @BrukerCacheable(
        "saksnummer-tilgang",
    )
    fun harTilgangSaksnummer(saksnummer: Saksnummer): Boolean = tilgangClient.harTilgangSaksnummer(saksnummer)
}

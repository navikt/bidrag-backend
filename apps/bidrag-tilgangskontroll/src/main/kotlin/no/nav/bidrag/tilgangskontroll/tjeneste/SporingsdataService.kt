package no.nav.bidrag.tilgangskontroll.tjeneste

import no.nav.bidrag.tilgangskontroll.konsumer.SakPipKonsumer
import no.nav.bidrag.transport.tilgang.Sporingsdata
import org.springframework.stereotype.Service

@Service
class SporingsdataService(
    private val tilgangskontrollService: TilgangskontrollService,
    private val sakPipKonsumer: SakPipKonsumer,
) {
    fun hentSakSporingsdata(saksnummer: String): Sporingsdata {
        val metadataPip = sakPipKonsumer.hentPipMetadata(saksnummer)
        val tilgang = tilgangskontrollService.sjekkTilgangAlleRollerV2(metadataPip.roller)

        val ekstrafelter =
            metadataPip.roller.mapIndexed { index, it ->
                if (index == 0) {
                    "saksnummer" to metadataPip.saksnummer.verdi
                } else {
                    "ident${index + 1}" to it
                }
            }

        return Sporingsdata(
            personIdent = metadataPip.roller.first(),
            tilgang.harTilgang,
            ekstrafelter,
        )
    }

    fun hentPersonSporingsdata(personIdent: String): Sporingsdata {
        val tilgang = tilgangskontrollService.sjekkTilgangAlleRollerV2(listOf(personIdent))
        return Sporingsdata(personIdent, tilgang.harTilgang)
    }
}

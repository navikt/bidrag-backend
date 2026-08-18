package no.nav.bidrag.sak.service

import no.nav.bidrag.domene.enums.sak.Fogdårsak
import no.nav.bidrag.domene.enums.sak.Tilgangstype
import no.nav.bidrag.domene.enums.vedtak.Behandlingstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.sak.domain.Bidragssak
import no.nav.bidrag.sak.domain.Tilgang
import no.nav.bidrag.sak.integration.organisasjon.BidragOrganisasjonClient
import no.nav.bidrag.transport.organisasjon.EnhetDto
import no.nav.bidrag.transport.organisasjon.HentEnhetRequest
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ArbeidsfordelingService(
    private val bidragOrganisasjonClient: BidragOrganisasjonClient,
) {
    fun utførArbeidsfordeling(sak: Bidragssak) {
        val hentEnhetRequest = lagHentEnhetRequest(sak)
        val enhetDto = bidragOrganisasjonClient.hentEnhetForArbeidsfordelingGeografiskTilknytning(hentEnhetRequest)
        oppdaterAktivEierfogd(sak, enhetDto)
    }

    private fun oppdaterAktivEierfogd(
        sak: Bidragssak,
        enhetDto: EnhetDto,
    ) {
        val aktivEierfogd =
            sak.tilganger.find { it.tilgangTomDato == null && it.type == Tilgangstype.EIER }
        if (aktivEierfogd?.enhetsnummer == enhetDto.nummer.verdi) {
            return
        }
        val nyTilgang = Tilgang(enhetsnummer = enhetDto.nummer.verdi, årsak = Fogdårsak.EIER, bidragssak = sak)
        if (aktivEierfogd == null) {
            sak.tilganger += nyTilgang
            sak.eierfogd = enhetDto.nummer.verdi
            return
        }
        val oppdaterteTilganger = sak.tilganger.filterNot { it == aktivEierfogd }.toMutableSet()
        aktivEierfogd.tilgangTomDato = LocalDate.now()
        aktivEierfogd.bidragssak = sak
        oppdaterteTilganger += setOf(aktivEierfogd, nyTilgang)

        sak.tilganger = oppdaterteTilganger
        sak.eierfogd = enhetDto.nummer.verdi
    }

    private fun lagHentEnhetRequest(sak: Bidragssak): HentEnhetRequest {
        val fødselsnummer: Personident =
            sak.primærrolle.fødselsnummer?.let { Personident(it) }
                ?: error("Primærrolle for sak ${sak.saksnummer} mangler fødselsnummer.")

        return HentEnhetRequest(
            ident = fødselsnummer,
            biidenter =
            sak.roller
                .mapNotNull {
                    if (it.erPerson()) {
                        it.fødselsnummer?.let { fødselsnummer -> Personident(fødselsnummer) }
                    } else {
                        null
                    }
                }.toSet(),
            behandlingstema = sak.arbeidsfordeling.behandlingstema,
            sakskategori = sak.kategori,
            behandlingstype = Behandlingstype.SØKNAD,
        )
    }
}

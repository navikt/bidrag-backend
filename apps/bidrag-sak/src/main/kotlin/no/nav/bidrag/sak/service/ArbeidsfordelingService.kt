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
    /**
     * Setter eierfogd til enheten arbeidsfordelingen i Norg gir for saken, ut fra primærrollens geografi
     * og strengeste diskresjon og skjerming blant nåværende og tidligere parter.
     * Avslutter den aktive eiertilgangen og oppretter en ny når enheten endres.
     */
    fun utførArbeidsfordeling(sak: Bidragssak) {
        oppdaterAktivEierfogd(sak, hentEnhet(sak))
    }

    /**
     * Som [utførArbeidsfordeling], men for en sak som ikke er lagret ennå.
     * Erstatter eiertilgangen i stedet for å avslutte den.
     */
    fun utførArbeidsfordelingPåNySak(sak: Bidragssak) {
        val enhet = hentEnhet(sak).nummer.verdi
        sak.eierfogd = enhet
        val eiertilganger = sak.tilganger.filter { it.type == Tilgangstype.EIER }
        sak.tilganger.removeAll(eiertilganger.toSet())
        sak.tilganger.addAll(eiertilganger.onEach { it.enhetsnummer = enhet })
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

    private fun hentEnhet(sak: Bidragssak): EnhetDto {
        val hovedident = sak.primærrolle?.fødselsnummer?.let(::Personident)
            ?: error("Sak ${sak.saksnummer} har ingen kjent part, og enheten kan ikke bestemmes.")
        return bidragOrganisasjonClient.hentEnhetForArbeidsfordelingGeografiskTilknytning(
            HentEnhetRequest(
                ident = hovedident,
                biidenter = sak.personidenter(),
                arbeidsfordeling = sak.arbeidsfordeling,
                sakskategori = sak.kategori,
                behandlingstype = Behandlingstype.SØKNAD,
            ),
        )
    }

    private fun Bidragssak.personidenter(): Set<Personident> {
        val nåværende = roller.mapNotNull { it.fødselsnummer }
        val tidligere = roller.flatMap { it.rollehistorikk }.flatMap { listOfNotNull(it.rolleFødselsnummer, it.rmRolleFødselsnummer) }
        return (nåværende + tidligere).map(::Personident).filter { it.gyldig() }.toSet()
    }
}

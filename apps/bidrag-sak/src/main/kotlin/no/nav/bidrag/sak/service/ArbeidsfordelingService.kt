package no.nav.bidrag.sak.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Fogdårsak
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.sak.Tilgangstype
import no.nav.bidrag.domene.enums.vedtak.Behandlingstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.sak.domain.Bidragssak
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.sak.domain.Tilgang
import no.nav.bidrag.sak.domain.finnPrimærrolle
import no.nav.bidrag.sak.integration.organisasjon.BidragOrganisasjonClient
import no.nav.bidrag.sak.integration.person.BidragPersonClient
import no.nav.bidrag.transport.organisasjon.EnhetDto
import no.nav.bidrag.transport.organisasjon.HentEnhetRequest
import no.nav.bidrag.transport.sak.RolleDto
import org.springframework.stereotype.Service
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}

@Service
class ArbeidsfordelingService(
    private val bidragOrganisasjonClient: BidragOrganisasjonClient,
    private val bidragPersonClient: BidragPersonClient,
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
     * 🔴 Bisys krever utvidede rettigheter for saker med ansatt = true.
     * Flagget er true når minst én av personene arbeidsfordelingen bruker, er skjermet i skjermingsregisteret.
     * Verdien klienten sender, brukes ikke.
     */
    fun oppdaterAnsatt(sak: Bidragssak) {
        val identer = sak.personidenter()
        val ansatt = identer.isNotEmpty() && bidragPersonClient.hentGraderingsinfo(identer).identerTilSkjerming.values.any { it }
        if (sak.ansatt == ansatt) return
        LOGGER.info { "Endrer ansatt-flagget på sak ${sak.saksnummer} fra ${sak.ansatt} til $ansatt." }
        sak.ansatt = ansatt
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
                biidenter = sak.personidenter() - hovedident,
                arbeidsfordeling = sak.arbeidsfordeling,
                sakskategori = sak.kategori,
                behandlingstype = Behandlingstype.SØKNAD,
            ),
        )
    }

    fun hentEnhetForNySak(
        roller: Set<RolleDto>,
        fødselsdatoer: Map<Personident, LocalDate?>,
        arbeidsfordeling: Arbeidsfordeling,
        sakskategori: Sakskategori,
    ): EnhetDto {
        val hovedident = roller.map {
            Rolle(fødselsnummer = it.fødselsnummer?.verdi, rolleType = it.type, fødselsdato = fødselsdatoer[it.fødselsnummer])
        }.finnPrimærrolle()?.fødselsnummer?.let(::Personident)
            ?: error("Saken har ingen kjent part, og enheten kan ikke bestemmes.")
        val parter = (roller.mapNotNull { it.fødselsnummer } + roller.mapNotNull { it.rmFødselsnummer() }).toSet()
        return bidragOrganisasjonClient.hentEnhetForArbeidsfordelingGeografiskTilknytning(
            HentEnhetRequest(
                ident = hovedident,
                biidenter = parter - hovedident,
                arbeidsfordeling = arbeidsfordeling,
                sakskategori = sakskategori,
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

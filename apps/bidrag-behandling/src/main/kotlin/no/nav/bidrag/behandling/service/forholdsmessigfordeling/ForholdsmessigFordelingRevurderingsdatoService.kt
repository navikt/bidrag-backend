package no.nav.bidrag.behandling.service.forholdsmessigfordeling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.behandling.consumer.BidragBBMConsumer
import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.database.datamodell.Rolle
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordelingSøknadBarn
import no.nav.bidrag.behandling.dto.v2.forholdsmessigfordeling.OppdaterRevurderingsdatoFFRequest
import no.nav.bidrag.behandling.service.VirkningstidspunktService
import no.nav.bidrag.behandling.transformers.maxOfNullable
import no.nav.bidrag.behandling.ugyldigForespørsel
import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.tilStønadstype
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import no.nav.bidrag.transport.behandling.beregning.felles.OppdaterSøknadFomDatoRequest
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}

/**
 * Oppdaterer revurderingsdato (søkt fra dato) for revurderingsbarn etter at forholdsmessig fordeling er opprettet.
 *
 * Flyten for hver FF-søknad som berøres:
 * 1. Får alle barn i søknaden ny dato, oppdateres søkt fra dato direkte i søknaden.
 *    Får barna ulike datoer, beholder søknaden datoen flest barn har fått.
 * 2. Øvrige barn flyttes til en ny FF-søknad med ny dato og feilregistreres i den opprinnelige søknaden.
 * 3. Til slutt oppdateres virkningstidspunktet for hvert barn som har fått ny dato.
 */
class ForholdsmessigFordelingRevurderingsdatoService(
    private val bbmConsumer: BidragBBMConsumer,
    private val søknadService: ForholdsmessigFordelingSøknadService,
    private val virkningstidspunktService: VirkningstidspunktService,
) {
    private data class EndringRevurderingsdato(
        val rolle: Rolle,
        val søknad: ForholdsmessigFordelingSøknadBarn,
        val nyRevurderingsdato: LocalDate,
    ) {
        val søknadsid get() = søknad.søknadsid!!
    }

    fun oppdaterRevurderingsdato(
        behandling: Behandling,
        request: OppdaterRevurderingsdatoFFRequest,
    ) {
        validerKanOppdatereRevurderingsdato(behandling)

        val endringer = request.barn.mapNotNull { finnEndring(behandling, it) }
        if (endringer.isEmpty()) {
            LOGGER.info { "Ingen endring i revurderingsdato for behandling ${behandling.id}" }
            return
        }

        endringer
            .groupBy { it.søknadsid }
            .forEach { (søknadsid, endringerISøknad) -> oppdaterSøknad(behandling, søknadsid, endringerISøknad) }
        endringer.forEach { oppdaterVirkningstidspunkt(behandling, it) }
        søknadService.knyttSammenManglendeSøknadsknytningerIBehandling(behandling)
    }

    // region Validering og utledning av endringer

    private fun validerKanOppdatereRevurderingsdato(behandling: Behandling) {
        if (!behandling.erIForholdsmessigFordeling) {
            ugyldigForespørsel("Behandling ${behandling.id} er ikke i forholdsmessig fordeling")
        }
        if (behandling.erKlageEllerOmgjøring) {
            ugyldigForespørsel("Revurderingsdato kan bare endres for ordinær forholdsmessig fordeling")
        }
        if (behandling.erVedtakFattet || behandling.vedtakDetaljer != null) {
            ugyldigForespørsel("Kan ikke endre revurderingsdato når vedtak er fattet for behandling ${behandling.id}")
        }
    }

    /** Returnerer null hvis barnet allerede har ønsket revurderingsdato */
    private fun finnEndring(
        behandling: Behandling,
        barnRequest: OppdaterRevurderingsdatoFFRequest.OppdaterRevurderingsdatoBarn,
    ): EndringRevurderingsdato? {
        val rolle = finnRevurderingsbarn(behandling, barnRequest)
        val søknad = finnFFSøknad(rolle)
        val nyRevurderingsdato = beregnNyRevurderingsdato(behandling, rolle, barnRequest.revurderingFraDato)

        return if (søknad.søknadFomDato == nyRevurderingsdato) null else EndringRevurderingsdato(rolle, søknad, nyRevurderingsdato)
    }

    private fun finnRevurderingsbarn(
        behandling: Behandling,
        barnRequest: OppdaterRevurderingsdatoFFRequest.OppdaterRevurderingsdatoBarn,
    ): Rolle {
        val rolle =
            behandling.søknadsbarn.find { it.erSammeRolle(barnRequest.ident, barnRequest.stønadstype) }
                ?: ugyldigForespørsel("Fant ikke barn med stønadstype ${barnRequest.stønadstype} i behandling ${behandling.id}")
        if (!rolle.erRevurderingsbarn) {
            ugyldigForespørsel("Revurderingsdato kan bare endres for revurderingsbarn og ikke for søknadsbarn med id ${rolle.id} i behandling ${behandling.id}")
        }
        return rolle
    }

    private fun finnFFSøknad(rolle: Rolle): ForholdsmessigFordelingSøknadBarn = rolle.forholdsmessigFordeling!!
        .søknaderUnderBehandling
        .singleOrNull { it.behandlingstype?.erForholdsmessigFordeling == true }
        ?: ugyldigForespørsel("Fant ikke én FF-søknad under behandling for rolle ${rolle.id}")

    /** Valgt dato settes til første dag i måneden og kan ikke være tidligere enn tidligste tillatte revurderingsdato */
    private fun beregnNyRevurderingsdato(
        behandling: Behandling,
        rolle: Rolle,
        valgtDato: LocalDate,
    ): LocalDate {
        // Revurderingsbarnets egne søknader skal ikke begrense hvor tidlig revurderingsdatoen kan settes
        val tidligsteRevurderingsdato =
            if (rolle.stønadstype == Stønadstype.BIDRAG18AAR) {
                behandling.finnTidligsteRevurderingsdato(rolle.ident!!, rolle.stønadstype)
            } else {
                behandling.eldsteSøktFomDatoSøknadsbarn
            }
        return maxOfNullable(tidligsteRevurderingsdato, valgtDato.withDayOfMonth(1))!!
    }

    // endregion
    // region Oppdatering av søknader

    private fun oppdaterSøknad(
        behandling: Behandling,
        søknadsid: Long,
        endringer: List<EndringRevurderingsdato>,
    ) {
        val søknad = bbmConsumer.hentSøknad(søknadsid)?.søknad ?: ugyldigForespørsel("Fant ikke søknad $søknadsid")
        val nySøktFraDatoISøknaden = finnNySøktFraDatoISøknaden(søknad, endringer)
        val barnSomFlyttesTilNySøknad = endringer.filter { it.nyRevurderingsdato != nySøktFraDatoISøknaden }

        barnSomFlyttesTilNySøknad
            .groupBy { it.nyRevurderingsdato }
            .forEach { (nyRevurderingsdato, barnMedSammeDato) ->
                flyttBarnTilNySøknad(behandling, søknad, barnMedSammeDato, nyRevurderingsdato)
            }

        nySøktFraDatoISøknaden?.let { oppdaterSøktFraDatoISøknad(behandling, søknadsid, it) }
    }

    /**
     * Finner ny søkt fra dato for søknaden, eller null hvis søknaden skal beholde datoen sin.
     *
     * Søknaden kan bare få ny dato når alle barn under behandling i søknaden får ny dato.
     * Da får søknaden datoen flest barn har valgt, og de andre barna flyttes til nye søknader.
     * Hvis bare noen av barna endres, beholder søknaden datoen sin, og alle barna som endres flyttes.
     */
    private fun finnNySøktFraDatoISøknaden(
        søknad: HentSøknad,
        identerBarnSomEndres: List<EndringRevurderingsdato>,
    ): LocalDate? {
        val endredeBarn = identerBarnSomEndres.mapNotNull { it.rolle.ident }.toSet()
        val barnISøknadenSomEndres = søknad.parterUnderBehandling.all { it.personident in endredeBarn }
        if (!barnISøknadenSomEndres) return null

        return identerBarnSomEndres
            .groupingBy { it.nyRevurderingsdato }
            .eachCount()
            .maxBy { (_, antallBarn) -> antallBarn }
            .key
    }

    private fun oppdaterSøktFraDatoISøknad(
        behandling: Behandling,
        søknadsid: Long,
        nyRevurderingsdato: LocalDate,
    ) {
        LOGGER.info { "Oppdaterer søkt fra dato i søknad $søknadsid til $nyRevurderingsdato for behandling ${behandling.id}" }
        bbmConsumer.oppdaterSøknadFomDato(OppdaterSøknadFomDatoRequest(søknadsid, nyRevurderingsdato))
        behandling.roller
            .flatMap { it.forholdsmessigFordeling?.søknader.orEmpty() }
            .filter { it.søknadsid == søknadsid }
            .forEach { it.søknadFomDato = nyRevurderingsdato }
    }

    private fun flyttBarnTilNySøknad(
        behandling: Behandling,
        gammelSøknad: HentSøknad,
        endringer: List<EndringRevurderingsdato>,
        nyRevurderingsdato: LocalDate,
    ) {
        val bmIdent = finnBidragsmottaker(gammelSøknad, endringer)
        val nySøknad = opprettNySøknad(behandling, gammelSøknad, endringer, nyRevurderingsdato, bmIdent)
        LOGGER.info {
            "Flytter ${endringer.size} barn fra søknad ${gammelSøknad.søknadsid} til søknad ${nySøknad.søknadsid} " +
                "med søkt fra dato $nyRevurderingsdato i behandling ${behandling.id}"
        }

        endringer.forEach { feilregistrerBarnFraSøknad(it.rolle, gammelSøknad.søknadsid) }

        val berørteRoller = endringer.map { it.rolle } + listOfNotNull(behandling.bidragspliktig, behandling.roller.find { it.ident == bmIdent })
        leggTilSøknadForRoller(berørteRoller, nySøknad)
    }

    private fun finnBidragsmottaker(
        søknad: HentSøknad,
        endringer: List<EndringRevurderingsdato>,
    ): String = søknad.partISøknadListe.find { it.rolletype == Rolletype.BIDRAGSMOTTAKER }?.personident
        ?: endringer.firstNotNullOfOrNull { it.rolle.forholdsmessigFordeling?.bidragsmottaker }
        ?: ugyldigForespørsel("Fant ikke bidragsmottaker for søknad ${søknad.søknadsid}")

    private fun opprettNySøknad(
        behandling: Behandling,
        gammelSøknad: HentSøknad,
        endringer: List<EndringRevurderingsdato>,
        nyRevurderingsdato: LocalDate,
        bmIdent: String,
    ): ForholdsmessigFordelingSøknadBarn = søknadService
        .opprettSøknad(
            bidragspliktigFnr = behandling.bidragspliktig!!.ident!!,
            barnUtenSøknader =
            endringer.map {
                SakKravhaver(saksnummer = gammelSøknad.saksnummer, kravhaver = it.rolle.ident!!, stønadstype = it.rolle.stønadstype)
            },
            saksnummer = gammelSøknad.saksnummer,
            behandling = behandling,
            behandlerEnhet = gammelSøknad.behandlerenhet ?: behandling.behandlerEnhet,
            stønadstype = gammelSøknad.behandlingstema.tilStønadstype(),
            søktFomDato = nyRevurderingsdato,
            medInnkreving = gammelSøknad.innkreving,
            bmFødselsnummer = bmIdent,
        ).copy(status = Behandlingstatus.UNDER_BEHANDLING)

    private fun feilregistrerBarnFraSøknad(
        rolle: Rolle,
        søknadsid: Long,
    ) {
        søknadService.feilregistrerBarnFraSøknad(rolle, søknadsid)
            ?: error("Klarte ikke å feilregistrere barn (rolle ${rolle.id}) fra søknad $søknadsid")
    }

    private fun leggTilSøknadForRoller(
        roller: List<Rolle>,
        søknad: ForholdsmessigFordelingSøknadBarn,
    ) {
        roller
            .mapNotNull { it.forholdsmessigFordeling }
            .filter { ff -> ff.søknader.none { it.søknadsid == søknad.søknadsid } }
            .forEach { it.søknader.add(søknad.copy()) }
    }

    // endregion

    private fun oppdaterVirkningstidspunkt(
        behandling: Behandling,
        endring: EndringRevurderingsdato,
    ) {
        endring.rolle.forholdsmessigFordeling!!.revurderingsdatoVedOpprettelseAvFF = endring.nyRevurderingsdato
        virkningstidspunktService.oppdaterVirkningstidspunkt(
            endring.rolle.id,
            endring.nyRevurderingsdato,
            behandling,
            tvingEndring = true,
        )
    }
}

package no.nav.bidrag.behandling.service.forholdsmessigfordeling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.behandling.consumer.BidragBBMConsumer
import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.database.datamodell.Rolle
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordelingSøknadBarn
import no.nav.bidrag.behandling.dto.v2.forholdsmessigfordeling.OppdaterRevurderingsdatoFFRequest
import no.nav.bidrag.behandling.service.VirkningstidspunktService
import no.nav.bidrag.behandling.ugyldigForespørsel
import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.tilStønadstype
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import no.nav.bidrag.transport.behandling.beregning.felles.LeggTilBarnIFFSøknadRequest
import no.nav.bidrag.transport.behandling.beregning.felles.OppdaterSøknadFomDatoRequest
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}

/**
 * Oppdaterer revurderingsdato (søkt fra dato) for revurderingsbarn etter at forholdsmessig fordeling er opprettet.
 *
 * Flyten for hver FF-søknad som berøres:
 * 1. Hvis alle barn i søknaden får ny revurderingsdato, oppdateres søkt fra dato på FF søknaden.
 *    HVis barna får ulike revurderingsdatoer, beholder eller oppdateres FF søknaden til søkt fra datoen flest R-barn får.
 * 2. Øvrige barn flyttes til en ny FF-søknad med ny dato og feilregistreres i den opprinnelige FF-søknaden.
 * 3. Til slutt oppdateres virkningstidspunktet for hvert barn som har fått ny revurderingsdato.
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

        val endringerRevurderingsdato = request.barn.mapNotNull { finnEndring(behandling, it) }
        if (endringerRevurderingsdato.isEmpty()) {
            LOGGER.info { "Ingen endring i revurderingsdato for behandling ${behandling.id}" }
            return
        }

        endringerRevurderingsdato
            .groupBy { it.søknadsid }
            .forEach { (søknadsid, endringerISøknad) -> oppdaterEllerOpprettFFSøknad(behandling, søknadsid, endringerISøknad) }
        endringerRevurderingsdato.forEach { oppdaterVirkningstidspunkt(behandling, it) }
        søknadService.knyttSammenManglendeSøknadsknytningerIBehandling(behandling)
    }

    private fun validerKanOppdatereRevurderingsdato(behandling: Behandling) {
        if (!behandling.erIForholdsmessigFordeling) {
            ugyldigForespørsel("Behandling ${behandling.id} er ikke i forholdsmessig fordeling")
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
        val rolle = behandling.søknadsbarn.find { it.erSammeRolle(barnRequest.ident, barnRequest.stønadstype) }?.let {
            if (!it.erRevurderingsbarn) {
                ugyldigForespørsel("Revurderingsdato kan bare endres for revurderingsbarn og ikke for søknadsbarn med id ${it.id} i behandling ${behandling.id}")
            } else {
                it
            }
        } ?: ugyldigForespørsel("Fant ikke barn som det forsøkes å endres for med stønadstype ${barnRequest.stønadstype} i behandling ${behandling.id}")

        val barnetsFFSøknad = finnRolleFFSøknad(rolle)
        val nyRevurderingsdato = barnRequest.nyRevurderingsdato.withDayOfMonth(1)
        val tidligsteRevurderingsdato = behandling.finnTidligsteRevurderingsdato(rolle.ident!!, rolle.stønadstype)
        if (nyRevurderingsdato.isBefore(tidligsteRevurderingsdato)) {
            ugyldigForespørsel("Ny revurderingsdato $nyRevurderingsdato kan ikke være tidligere enn tidligste tillatte revurderingsdato $tidligsteRevurderingsdato for rolle ${rolle.id} i behandling ${behandling.id}")
        }

        return if (barnetsFFSøknad.søknadFomDato == nyRevurderingsdato) null else EndringRevurderingsdato(rolle, barnetsFFSøknad, nyRevurderingsdato)
    }

    /** Barnet tilhører alltid én sak, og FF-søknaden må derfor være i samme sak som barnet */
    private fun finnRolleFFSøknad(rolle: Rolle): ForholdsmessigFordelingSøknadBarn {
        val ffRolle = rolle.forholdsmessigFordeling!!
        return ffRolle.søknaderUnderBehandling
            .filter { it.saksnummer == null || it.saksnummer == ffRolle.tilhørerSak }
            .singleOrNull { it.behandlingstype?.erForholdsmessigFordeling == true }
            ?: ugyldigForespørsel("Fant ikke én FF-søknad under behandling i sak ${ffRolle.tilhørerSak} for rolle ${rolle.id}")
    }

    private fun oppdaterEllerOpprettFFSøknad(
        behandling: Behandling,
        søknadsid: Long,
        endringer: List<EndringRevurderingsdato>,
    ) {
        val søknad = bbmConsumer.hentSøknad(søknadsid)?.søknad ?: ugyldigForespørsel("Fant ikke søknad $søknadsid")
        val nySøktFraDatoISøknaden = finnNySøktFraDatoIFFSøknaden(søknad, endringer)
        val barnSomSkalFlyttesTilNySøknad = endringer.filter { it.nyRevurderingsdato != nySøktFraDatoISøknaden }

        barnSomSkalFlyttesTilNySøknad
            .groupBy { it.nyRevurderingsdato }
            .forEach { (nyRevurderingsdato, barnMedSammeDato) ->
                flyttTilEllerOpprettNyFFSøknad(behandling, søknad, barnMedSammeDato, nyRevurderingsdato)
            }

        if (nySøktFraDatoISøknaden != null) {
            oppdaterSøktFraDatoISøknad(behandling, søknadsid, nySøktFraDatoISøknaden)
        }
    }

    /**
     * Finner ny søkt fra dato for søknaden, eller null hvis søknaden skal beholde datoen sin.
     *
     * Søknaden kan bare få ny dato når alle barn under behandling i søknaden får ny dato.
     * Da får søknaden datoen flest barn har valgt, og de andre barna flyttes til nye søknader.
     * Hvis bare noen av barna endres, beholder søknaden datoen sin, og alle barna som endres flyttes.
     */
    private fun finnNySøktFraDatoIFFSøknaden(
        søknad: HentSøknad,
        identerBarnSomEndres: List<EndringRevurderingsdato>,
    ): LocalDate? {
        val endredeBarn = identerBarnSomEndres.mapNotNull { it.rolle.ident }.toSet()
        val alleBarnISøknadenSomEndres = søknad.parterUnderBehandling.all { it.personident in endredeBarn }
        if (!alleBarnISøknadenSomEndres) return null

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
            .flatMap { it.forholdsmessigFordeling?.søknaderUnderBehandling.orEmpty() }
            .filter { it.søknadsid == søknadsid }
            .forEach { it.søknadFomDato = nyRevurderingsdato }
    }

    private fun flyttTilEllerOpprettNyFFSøknad(
        behandling: Behandling,
        gammelSøknad: HentSøknad,
        endringer: List<EndringRevurderingsdato>,
        nyRevurderingsdato: LocalDate,
    ) {
        val bmIdent = finnBidragsmottaker(gammelSøknad, endringer)
        val eksisterendeSøknadMedSammeSøktFraDato = finnSøknadMedSammeSøktFraDato(behandling, gammelSøknad, nyRevurderingsdato)
        val nyEllerOpprettSøknad = if (eksisterendeSøknadMedSammeSøktFraDato != null) {
            leggTilBarnIEksisterendeSøknad(eksisterendeSøknadMedSammeSøktFraDato, endringer)
            eksisterendeSøknadMedSammeSøktFraDato
        } else {
            opprettNySøknad(behandling, gammelSøknad, endringer, nyRevurderingsdato, bmIdent)
        }

        LOGGER.info {
            "Flytter ${endringer.size} barn fra søknad ${gammelSøknad.søknadsid} til søknad ${nyEllerOpprettSøknad.søknadsid} " +
                "med søkt fra dato $nyRevurderingsdato i behandling ${behandling.id}"
        }

        endringer.forEach { feilregistrerBarnFraSøknad(it.rolle, gammelSøknad.søknadsid) }

        val rollerEndringGjelder = endringer.map { it.rolle } + listOfNotNull(behandling.bidragspliktig, behandling.roller.find { it.ident == bmIdent })
        leggTilSøknadForRoller(rollerEndringGjelder, nyEllerOpprettSøknad)
    }

    private fun finnBidragsmottaker(
        søknad: HentSøknad,
        endringer: List<EndringRevurderingsdato>,
    ): String = søknad.partISøknadListe.find { it.rolletype == Rolletype.BIDRAGSMOTTAKER }?.personident
        ?: endringer.firstNotNullOfOrNull { it.rolle.forholdsmessigFordeling?.bidragsmottaker }
        ?: ugyldigForespørsel("Fant ikke bidragsmottaker for søknad ${søknad.søknadsid}")

    /**
     * Finner en FF-søknad under behandling i samme sak, med samme stønadstype, innkreving og søkt fra dato.
     * Barna flyttes til denne søknaden i stedet for å opprette en ny, slik at det ikke blir duplikate søknader.
     */
    private fun finnSøknadMedSammeSøktFraDato(
        behandling: Behandling,
        gammelSøknad: HentSøknad,
        nyRevurderingsdato: LocalDate,
    ): ForholdsmessigFordelingSøknadBarn? = behandling.roller
        .flatMap { it.forholdsmessigFordeling?.søknaderUnderBehandling.orEmpty() }
        .firstOrNull {
            it.søknadsid != gammelSøknad.søknadsid &&
                it.behandlingstype?.erForholdsmessigFordeling == true &&
                it.saksnummer == gammelSøknad.saksnummer &&
                it.søknadFomDato == nyRevurderingsdato &&
                it.innkreving == gammelSøknad.innkreving &&
                it.behandlingstema?.tilStønadstype() == gammelSøknad.behandlingstema.tilStønadstype()
        }?.copy()

    private fun leggTilBarnIEksisterendeSøknad(
        søknad: ForholdsmessigFordelingSøknadBarn,
        endringer: List<EndringRevurderingsdato>,
    ) {
        endringer
            .map { it.rolle }
            .filter { rolle -> rolle.forholdsmessigFordeling?.søknaderUnderBehandling.orEmpty().none { it.søknadsid == søknad.søknadsid } }
            .forEach { bbmConsumer.leggTilBarnISøknad(LeggTilBarnIFFSøknadRequest(søknad.søknadsid!!, it.ident!!)) }
    }

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
            .filter { ff -> ff.søknaderUnderBehandling.none { it.søknadsid == søknad.søknadsid } }
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

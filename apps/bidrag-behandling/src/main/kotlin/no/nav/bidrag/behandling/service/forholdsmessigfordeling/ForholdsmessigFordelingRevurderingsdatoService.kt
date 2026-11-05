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
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
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
 * Søknaden oppdateres direkte i BBM hvis alle barn under behandling i søknaden får samme nye dato.
 * Hvis bare noen av barna i søknaden endres, eller barna får ulike datoer, flyttes de endrede barna til ny FF-søknad
 * med ny søkt fra dato og feilregistreres fra den opprinnelige søknaden.
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
    )

    fun oppdaterRevurderingsdato(
        behandling: Behandling,
        request: OppdaterRevurderingsdatoFFRequest,
    ) {
        validerKanOppdatereRevurderingsdato(behandling)
        val endringer = request.barn.mapNotNull { tilEndring(behandling, it) }
        if (endringer.isEmpty()) {
            LOGGER.info { "Ingen endring i revurderingsdato for behandling ${behandling.id}" }
            return
        }

        endringer
            .groupBy { it.søknad.søknadsid!! }
            .forEach { (søknadsid, endringerSøknad) -> oppdaterSøknad(behandling, søknadsid, endringerSøknad) }

        endringer.forEach { endring ->
            endring.rolle.forholdsmessigFordeling!!.revurderingsdatoVedOpprettelseAvFF = endring.nyRevurderingsdato
            virkningstidspunktService.oppdaterVirkningstidspunkt(
                endring.rolle.id,
                endring.nyRevurderingsdato,
                behandling,
                tvingEndring = true,
            )
        }
        søknadService.knyttSammenManglendeSøknadsknytningerIBehandling(behandling)
    }

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

    private fun tilEndring(
        behandling: Behandling,
        barnRequest: OppdaterRevurderingsdatoFFRequest.OppdaterRevurderingsdatoBarn,
    ): EndringRevurderingsdato? {
        val rolle =
            behandling.søknadsbarn.find { it.erSammeRolle(barnRequest.ident, barnRequest.stønadstype) }
                ?: ugyldigForespørsel("Fant ikke barn med stønadstype ${barnRequest.stønadstype} i behandling ${behandling.id}")
        if (!rolle.erRevurderingsbarn) {
            ugyldigForespørsel("Revurderingsdato kan bare endres for revurderingsbarn (rolle ${rolle.id})")
        }
        val søknad =
            rolle.forholdsmessigFordeling!!
                .søknaderUnderBehandling
                .singleOrNull { it.erSystemopprettetFFSøknad }
                ?: ugyldigForespørsel("Fant ikke én systemopprettet FF-søknad under behandling for rolle ${rolle.id}")

        // Revurderingsbarnets egne søknader skal ikke begrense hvor tidlig revurderingsdatoen kan settes
        val tidligsteRevurderingsdato =
            if (rolle.stønadstype == Stønadstype.BIDRAG18AAR) {
                behandling.finnTidligsteRevurderingsdato(rolle.ident!!, rolle.stønadstype)
            } else {
                behandling.eldsteSøktFomDatoSøknadsbarn
            }
        val nyRevurderingsdato = maxOfNullable(tidligsteRevurderingsdato, barnRequest.revurderingFraDato.withDayOfMonth(1))!!
        return if (søknad.søknadFomDato == nyRevurderingsdato) null else EndringRevurderingsdato(rolle, søknad, nyRevurderingsdato)
    }

    private fun oppdaterSøknad(
        behandling: Behandling,
        søknadsid: Long,
        endringer: List<EndringRevurderingsdato>,
    ) {
        val søknad = bbmConsumer.hentSøknad(søknadsid)?.søknad ?: ugyldigForespørsel("Fant ikke søknad $søknadsid")
        val endredeBarn = endringer.mapNotNull { it.rolle.ident }.toSet()
        val alleBarnISøknadenEndres = søknad.parterUnderBehandling.all { endredeBarn.contains(it.personident) }

        // Hvis alle barn i søknaden endres oppdateres søknaden med datoen flest barn har fått. Resten flyttes til ny søknad
        val endringerSomOppdatererSøknad =
            if (alleBarnISøknadenEndres) {
                endringer.groupBy { it.nyRevurderingsdato }.values.maxBy { it.size }
            } else {
                emptyList()
            }

        (endringer - endringerSomOppdatererSøknad.toSet())
            .groupBy { it.nyRevurderingsdato }
            .forEach { (nyRevurderingsdato, endringerSomFlyttes) ->
                flyttBarnTilNySøknad(behandling, søknad, endringerSomFlyttes, nyRevurderingsdato)
            }

        endringerSomOppdatererSøknad.firstOrNull()?.let {
            oppdaterSøktFraDatoISøknad(behandling, søknadsid, it.nyRevurderingsdato)
        }
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
        val bmIdent =
            gammelSøknad.partISøknadListe.find { it.rolletype == Rolletype.BIDRAGSMOTTAKER }?.personident
                ?: endringer.firstNotNullOfOrNull { it.rolle.forholdsmessigFordeling?.bidragsmottaker }
                ?: ugyldigForespørsel("Fant ikke bidragsmottaker for søknad ${gammelSøknad.søknadsid}")
        val nySøknad =
            søknadService
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
        LOGGER.info {
            "Flytter ${endringer.size} barn fra søknad ${gammelSøknad.søknadsid} til søknad ${nySøknad.søknadsid} " +
                "med søkt fra dato $nyRevurderingsdato i behandling ${behandling.id}"
        }

        endringer.forEach { endring ->
            søknadService.feilregistrerBarnFraSøknad(endring.rolle, gammelSøknad.søknadsid)
                ?: error("Klarte ikke å feilregistrere barn (rolle ${endring.rolle.id}) fra søknad ${gammelSøknad.søknadsid}")
        }
        (endringer.map { it.rolle } + listOfNotNull(behandling.bidragspliktig, behandling.roller.find { it.ident == bmIdent }))
            .mapNotNull { it.forholdsmessigFordeling }
            .filter { ff -> ff.søknader.none { it.søknadsid == nySøknad.søknadsid } }
            .forEach { it.søknader.add(nySøknad.copy()) }
    }

    private val ForholdsmessigFordelingSøknadBarn.erSystemopprettetFFSøknad get() =
        søknadsid != null &&
            behandlingstype == Behandlingstype.FORHOLDSMESSIG_FORDELING &&
            opprettetAvSystem &&
            !opprettetAvSaksbehandler
}

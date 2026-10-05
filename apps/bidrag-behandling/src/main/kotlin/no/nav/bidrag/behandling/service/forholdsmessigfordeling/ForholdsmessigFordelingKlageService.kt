package no.nav.bidrag.behandling.service.forholdsmessigfordeling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.behandling.config.UnleashFeatures
import no.nav.bidrag.behandling.consumer.BidragBBMConsumer
import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.database.datamodell.Rolle
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordeling
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordelingSøknadBarn
import no.nav.bidrag.behandling.dto.grunnlag.LøpendeBidragGrunnlagForholdsmessigFordeling
import no.nav.bidrag.behandling.dto.v2.forholdsmessigfordeling.OpprettFFRequest
import no.nav.bidrag.behandling.service.BehandlingService
import no.nav.bidrag.behandling.service.GrunnlagService
import no.nav.bidrag.behandling.service.UnderholdService
import no.nav.bidrag.behandling.service.VirkningstidspunktService
import no.nav.bidrag.behandling.service.hentSak
import no.nav.bidrag.behandling.service.hentVedtak
import no.nav.bidrag.behandling.transformers.behandling.oppdaterBehandlingEtterOppdatertRoller
import no.nav.bidrag.behandling.transformers.erOverEllerLik18År
import no.nav.bidrag.behandling.transformers.maxOfNullable
import no.nav.bidrag.behandling.transformers.vedtak.mapping.tilvedtak.finnBeregnTilDato
import no.nav.bidrag.behandling.transformers.vedtak.mapping.tilvedtak.finnBeregningsperiode
import no.nav.bidrag.behandling.ugyldigForespørsel
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.service.forsendelse.bidragsmottaker
import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.behandling.SøknadsknytningStatus
import no.nav.bidrag.domene.enums.behandling.tilStønadstype
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.beregning.felles.Barn
import no.nav.bidrag.transport.behandling.beregning.felles.FeilregistrerSøknadRequest
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import no.nav.bidrag.transport.behandling.beregning.felles.OpprettSøknadRequest
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentSøknadForPerson
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import no.nav.bidrag.transport.felles.toYearMonth
import no.nav.bidrag.transport.søknad.FinnSammenknytningerHovedsøknadResponse
import org.springframework.cglib.core.Local
import java.time.LocalDate

private val KLAGE_LOGGER = KotlinLogging.logger {}

class ForholdsmessigFordelingKlageService(
    private val bbmConsumer: BidragBBMConsumer,
    private val behandlingService: BehandlingService,
    private val grunnlagService: GrunnlagService,
    private val underholdService: UnderholdService,
    private val virkningstidspunktService: VirkningstidspunktService,
    private val kravhaverService: ForholdsmessigFordelingKravhaverService,
    private val søknadService: ForholdsmessigFordelingSøknadService,
    private val overføringService: ForholdsmessigFordelingOverføringService,
) {
    // Feilhåndtering - slett eller gjenopprett klagesøknader basert på påklaget søknad
    // Hvis hovedsøknad slettes så slettes alle relaterte søknader
    // Hvis en annen søknad slettes så gjenopprettes klagesøknaden
    fun slettEllerGjennopprettKlageSøknader(
        behandling: Behandling,
        søknadsidSomSlettes: Long,
    ) {
        val behandlingSlettet = if (behandling.soknadsid == søknadsidSomSlettes) {
            val tilknyttedeSøknader =
                bbmConsumer.finnSammenknytningerHovedsøknad(
                    søknadsidSomSlettes,
                    SøknadsknytningStatus.Aktiv,
                )
            val annenSøknadForSammePåklagetSøknad =
                tilknyttedeSøknader.søknader.find {
                    it.søknadsid != behandling.soknadsid &&
                        it.refSøknadsid == behandling.omgjøringsdetaljer?.soknadRefId
                } ?: tilknyttedeSøknader.søknader
                    .filter { it.søknadsid != behandling.soknadsid && behandling.erSøknadOpprettetEtterHovedsøknad(it.søknadsid) }
                    .minByOrNull { it.søknadsid }
            if (annenSøknadForSammePåklagetSøknad != null) {
                behandling.soknadsid = annenSøknadForSammePåklagetSøknad.søknadsid
                bbmConsumer.fjernSammeknytningHovedsøknad(søknadsidSomSlettes, annenSøknadForSammePåklagetSøknad.søknadsid)
                val søknadSomSlettes = bbmConsumer.hentSøknad(søknadsidSomSlettes)!!.søknad
                val tilknyttedeSøknaderOmgjortSøknad =
                    bbmConsumer.finnSammenknytningerHovedsøknad(
                        behandling.omgjøringsdetaljer!!.soknadRefId!!,
                        SøknadsknytningStatus.Deaktiv,
                    )

                if (tilknyttedeSøknaderOmgjortSøknad.hovedsøknadsid == søknadSomSlettes.refSøknadsid) {
                    // Hovedsøknad ble slettet men behandlinger er ikke lukket. Gjennopprett klagesøknad slik at samme struktur beholdes som i påklaget søknad
                    opprettKlageSøknad(
                        søknadSomSlettes,
                        behandling,
                        emptyList(),
                        behandling.soknadsid,
                        søknadSomSlettes.søknadMottattDato,
                    )
                }

                behandling.roller.filter { it.harSøknad(søknadsidSomSlettes) }.forEach { rolle ->
                    val lagretSøknad = rolle.finnSøknad(søknadsidSomSlettes)!!
                    lagretSøknad.status = Behandlingstatus.FEILREGISTRERT
                }
                false
            } else {
                tilknyttedeSøknader.søknader.forEach { søknad ->
                    bbmConsumer.feilregistrerSøknad(FeilregistrerSøknadRequest(søknad.søknadsid))
                    behandling.roller.filter { it.harSøknad(søknad.søknadsid) }.forEach { rolle ->
                        val lagretSøknad = rolle.finnSøknad(søknad.søknadsid)!!
                        lagretSøknad.status = Behandlingstatus.FEILREGISTRERT
                    }
                }
                bbmConsumer.fjernSammeknytningHovedsøknad(søknadsidSomSlettes)
                behandlingService.logiskSlettBehandling(behandling)
                søknadService.slettAlleSøknaderKnyttetTilBehandling(behandling)
                true
            }
        } else {
            val søknadSomSlettes = bbmConsumer.hentSøknad(søknadsidSomSlettes)!!.søknad
            val erSøknadOpprettetEtterHovedsøknad = behandling.erSøknadOpprettetEtterHovedsøknad(søknadsidSomSlettes)
            if (søknadSomSlettes.refSøknadsid != behandling.soknadsid && !erSøknadOpprettetEtterHovedsøknad) {
                // Var ikke hovedsøknad som ble slettet. Gjennopprett klagesøknad slik at samme struktur beholdes som i påklaget søknad
                opprettKlageSøknad(
                    søknadSomSlettes,
                    behandling,
                    emptyList(),
                    behandling.soknadsid,
                    søknadSomSlettes.søknadMottattDato,
                )
                bbmConsumer.fjernSammenknytning(søknadsidSomSlettes)
            }
            false
        }

        // Gjør dette helt til slutt da det sjekkes om barn var feilregistrert i original søknad ved gjennopprettelse
        behandling.roller.filter { it.harSøknad(søknadsidSomSlettes) }.forEach {
            val søknad = it.finnSøknad(søknadsidSomSlettes)!!
            søknad.status = Behandlingstatus.FEILREGISTRERT
        }
        if (!behandlingSlettet) {
            gjenopprettFFKlagesøknaderErstattetAvSøknad(behandling)
        }
    }

    fun opprettSøknaderForKlageEllerOmgjøring(
        behandling: Behandling,
        opprettetEllerOppdaterSøknadsid: Long,
        request: OpprettFFRequest? = null,
        nyesteLøpendeBidragGrunnlag: List<LøpendeBidragGrunnlagForholdsmessigFordeling> = emptyList(),
    ) {
        if (TokenUtils.hentBruker() != null && !UnleashFeatures.TILGANG_OPPRETTE_FF.isEnabled) {
            KLAGE_LOGGER.info { "Opprettelse av forholdsmessig fordeling er deaktivert" }
            ugyldigForespørsel("Opprettelse av forholdsmessig fordeling er deaktivert")
        }

        val relevanteKravhavere = kravhaverService.hentAlleRelevanteKravhavere(behandling).toMutableSet()
        val bmOgBidragspliktiIdenter = listOfNotNull(behandling.bidragspliktig?.ident, behandling.bidragsmottaker?.ident)

        var hovedsøknadsid = behandling.soknadsid!!

        val behandlerEnhet = kravhaverService.finnEnhetForBarnIBehandling(behandling, request?.opprettetAvEnhet)
        val åpneSøknaderForVedtaksid = hentÅpneSøknaderForVedtak(behandling)
        sammeknyttSøknadHvisNødvendig(hovedsøknadsid, opprettetEllerOppdaterSøknadsid)

        val opprettetSøknad = bbmConsumer.hentSøknad(opprettetEllerOppdaterSøknadsid)!!.søknad
        hovedsøknadsid =
            håndterSlettetHovedsøknad(
                opprettetSøknad,
                behandling,
                åpneSøknaderForVedtaksid,
                hovedsøknadsid,
                opprettetEllerOppdaterSøknadsid,
            )

        oppdaterRollerMedSøknadDetaljer(behandling, opprettetSøknad, bmOgBidragspliktiIdenter, opprettetEllerOppdaterSøknadsid)
        feilregistrerFFKlagesøknaderErstattetAvOpprettetSøknad(behandling)
        val rollerITilknyttedeSøknader = finnAlleBarnIOpprettetSøknader(hovedsøknadsid)

        val søknadsbarnOrdinæreSøknader =
            opprettKlagesøknaderForTilknyttedeSøknader(
                behandling,
                opprettetSøknad,
                relevanteKravhavere,
                åpneSøknaderForVedtaksid,
                hovedsøknadsid,
                rollerITilknyttedeSøknader,
            )

        fjernSøknaderSomIkkeErDelAvKlagebehandlingen(behandling)

        val gjenværendeKravhavere =
            relevanteKravhavere
                .filter { rk -> søknadsbarnOrdinæreSøknader.none { it.first == rk.kravhaver && it.second == rk.stønadstype } }
                .filter { rk -> rollerITilknyttedeSøknader.none { it.kravhaverIdent == rk.kravhaver && it.stønadstype == rk.stønadstype } }
                .toSet()
        opprettRevurderingssøknaderForGjenværendeKravhavere(
            behandling,
            gjenværendeKravhavere,
            behandlerEnhet,
            request,
        )

        behandling.forholdsmessigFordeling =
            ForholdsmessigFordeling(
                erHovedbehandling = true,
                oppprettetAvEnhet = request?.opprettetAvEnhet,
                opprettetAvSaksbehandler = TokenUtils.hentSaksbehandlerIdent(),
            )

        overføringService.giSakTilgangTilEnhet(behandling, behandlerEnhet)
        kravhaverService.opprettGrunnlagLøpendeBidrag(behandling, nyesteLøpendeBidragGrunnlag)
        // Tving ny grunnlagsinnhenting slik at nye roller (bidragsmottaker og barn) får hentet inn grunnlag selv om grunnlag ble nylig ble innhentet for behandlingen.
        behandling.grunnlagSistInnhentet = null
        grunnlagService.oppdatereGrunnlagForBehandling(behandling)
        oppdaterBehandlingEtterOppdatertRoller(
            behandling,
            underholdService,
            virkningstidspunktService,
            behandling.søknadsbarn.map { it.tilOpprettRolleDto() },
            emptyList(),
        )

        opprettVarselForsendelserForKlage(behandling, hovedsøknadsid)
        behandlingService.sendOppdatertHendelse(behandling.id!!, false)
    }

    private fun opprettVarselForsendelserForKlage(
        behandling: Behandling,
        hovedsøknadsid: Long,
    ) {
        behandling.søknadsbarn
            .groupBy {
                it.saksnummer
            }.forEach { (saksnummer, roller) ->
                val hovedsøknad = behandling.hentSøknad(hovedsøknadsid)!!
                val søknad = roller.firstNotNullOfOrNull { it.forholdsmessigFordeling?.eldsteSøknad }
                val barnUnder18År = roller.filter { !it.fødselsdato.erOverEllerLik18År() }
                val bidragsmottaker =
                    roller.firstNotNullOfOrNull { it.bidragsmottaker }?.ident ?: hentSak(saksnummer)?.bidragsmottaker?.fødselsnummer?.verdi
                søknadService.opprettForsendelseForNySøknad(
                    saksnummer,
                    behandling,
                    bidragsmottaker!!,
                    søknad ?: hovedsøknad,
                    barn = barnUnder18År.map { SakKravhaver(saksnummer, kravhaver = it.ident!!, stønadstype = it.stønadstype) },
                )

                val barnOver18År = roller.filter { it.fødselsdato.erOverEllerLik18År() }
                if (barnOver18År.isNotEmpty()) {
                    barnOver18År.forEach {
                        søknadService.opprettForsendelseForNySøknad(
                            saksnummer,
                            behandling,
                            bidragsmottaker!!,
                            søknad ?: hovedsøknad,
                            barn = listOf(SakKravhaver(saksnummer, kravhaver = it.ident!!, stønadstype = it.stønadstype)),
                        )
                    }
                }
            }
    }
    private fun hentÅpneSøknaderForBehandling(behandling: Behandling): List<OpprettetSøknad> {
        val søknader = bbmConsumer
            .hentÅpneSøknaderForBehandling(behandling.id!!).søknader
        return søknader.flatMap {
            it.parterUnderBehandling.filter { it.personident != null }
                .map { p -> OpprettetSøknad(p.personident!!, it.behandlingstema.tilStønadstype(), it.refSøknadsid, it.søknadsid, it.behandlingstype) }
        }.distinct()
    }

    private fun hentÅpneSøknaderForVedtak(behandling: Behandling): List<HentSøknad> = bbmConsumer
        .hentÅpneSøknaderForBp(behandling.bidragspliktig!!.ident!!)
        .åpneSøknader
        .filter { it.refVedtaksid == behandling.omgjøringsdetaljer?.omgjørVedtakId }

    private fun finnAlleBarnIOpprettetSøknader(hovedsøknadsid: Long): List<OpprettetSøknad> {
        val tilknyttedeSøknaderBehandling =
            bbmConsumer.finnSammenknytningerHovedsøknad(
                hovedsøknadsid,
                SøknadsknytningStatus.Aktiv,
            )
        return tilknyttedeSøknaderBehandling.søknader.flatMap {
            it.parterUnderBehandling.filter { it.personident != null }
                .map { p -> OpprettetSøknad(p.personident!!, it.behandlingstema.tilStønadstype(), it.refSøknadsid, it.søknadsid, it.behandlingstype) }
        }.distinct()
    }
    private fun sammeknyttSøknadHvisNødvendig(
        hovedsøknadsid: Long,
        opprettetEllerOppdaterSøknadsid: Long,
    ) {
        val tilknyttedeSøknaderBehandling =
            bbmConsumer.finnSammenknytningerHovedsøknad(
                hovedsøknadsid,
                SøknadsknytningStatus.Aktiv,
            )
        if (tilknyttedeSøknaderBehandling.søknader.none { it.søknadsid == opprettetEllerOppdaterSøknadsid }) {
            bbmConsumer.sammeknyttSøknader(hovedsøknadsid, opprettetEllerOppdaterSøknadsid)
        }
    }

    /**
     * Hvis den opprettede søknaden er avbrutt, opprett en ny klagesøknad.
     * Returnerer oppdatert hovedsøknadsid.
     */
    internal fun håndterSlettetHovedsøknad(
        opprettetSøknad: HentSøknad,
        behandling: Behandling,
        åpneSøknaderForVedtaksid: List<HentSøknad>,
        gjeldeneHovedsøknadsid: Long,
        opprettetEllerOppdaterSøknadsid: Long,
    ): Long {
        if (opprettetSøknad.behandlingStatusType != BehandlingStatusType.AVBRUTT) return gjeldeneHovedsøknadsid

        val hovedsøknad = bbmConsumer.hentSøknad(gjeldeneHovedsøknadsid)
        if (hovedsøknad?.søknad?.behandlingStatusType?.erÅpenStatus == true) return gjeldeneHovedsøknadsid
        val varHovedsøknad = opprettetEllerOppdaterSøknadsid == gjeldeneHovedsøknadsid
        if (varHovedsøknad) {
            val nyHovedsøknadsid = behandling.finnSøknadSomKanBliHovedsøknad(gjeldeneHovedsøknadsid)
            if (nyHovedsøknadsid != null) {
                KLAGE_LOGGER.info {
                    "Hovedsøknad $gjeldeneHovedsøknadsid er avbrutt. Setter søknad $nyHovedsøknadsid som ble opprettet etter hovedsøknaden som ny hovedsøknad i behandling ${behandling.id}"
                }
                bbmConsumer.fjernSammeknytningHovedsøknad(gjeldeneHovedsøknadsid, nyHovedsøknadsid)
                behandling.soknadsid = nyHovedsøknadsid
//                gjenopprettFFKlagesøknaderErstattetAvSøknad(behandling, gjeldeneHovedsøknadsid)
                return nyHovedsøknadsid
            }
        }

        val originalSøknad = bbmConsumer.hentSøknad(behandling.omgjøringsdetaljer!!.soknadRefId!!)!!.søknad
        val nySøknadsid =
            opprettKlageSøknad(
                originalSøknad,
                behandling,
                åpneSøknaderForVedtaksid,
                if (varHovedsøknad) null else gjeldeneHovedsøknadsid,
            )
        if (varHovedsøknad) {
            bbmConsumer.fjernSammeknytningHovedsøknad(gjeldeneHovedsøknadsid, nySøknadsid)
            behandling.soknadsid = nySøknadsid
            return nySøknadsid
        }
        return gjeldeneHovedsøknadsid
    }

    /** Legger til søknadsinfo på roller som tilhører den opprettede søknaden */
    private fun oppdaterRollerMedSøknadDetaljer(
        behandling: Behandling,
        opprettetSøknad: HentSøknad,
        bmOgBidragspliktiIdenter: List<String>,
        opprettetEllerOppdaterSøknadsid: Long,
    ) {
        val opprettetSøknadRoller = opprettetSøknad.partISøknadListe.map { it.personident!! } + bmOgBidragspliktiIdenter
        behandling.roller
            .filter {
                opprettetSøknadRoller.contains(it.ident) &&
                    it.stønadstype == opprettetSøknad.behandlingstema.tilStønadstype() &&
                    !it.harSøknad(opprettetEllerOppdaterSøknadsid)
            }.forEach {
                it.forholdsmessigFordeling!!.søknader.add(
                    ForholdsmessigFordelingSøknadBarn(
                        søknadsid = opprettetEllerOppdaterSøknadsid,
                        behandlingstema = opprettetSøknad.behandlingstema,
                        behandlingstype = opprettetSøknad.behandlingstype,
                        omgjørSøknadsid = opprettetSøknad.refSøknadsid ?: behandling.omgjøringsdetaljer?.soknadRefId,
                        omgjørVedtaksid = opprettetSøknad.refVedtaksid ?: behandling.omgjøringsdetaljer?.omgjørVedtakId,
                        innkreving = opprettetSøknad.innkreving,
                        mottattDato = opprettetSøknad.søknadMottattDato,
                        søktAvType = opprettetSøknad.søktAvType,
                        søknadFomDato = opprettetSøknad.søknadFomDato,
                        saksnummer = opprettetSøknad.saksnummer,
                        status = opprettetSøknad.partISøknadListe.filterBarnUnderBehandling().firstOrNull()?.behandlingstatus ?: Behandlingstatus.UNDER_BEHANDLING,
                        enhet = opprettetSøknad.behandlerenhet ?: behandling.behandlerEnhet,
                        opprettetEtterHovedsøknad = !behandling.erNyBehandlingIkkeOpprettet,
                    ),
                )
            }
    }

    fun kanEndreSøknadStatus(søknadsid: Long): Boolean {
        val behandling =
            behandlingService.hentEksisterendeBehandling(søknadsid)
                ?: bbmConsumer.hentSøknad(søknadsid)?.søknad?.behandlingsid?.let { behandlingService.hentBehandlingById(it) }
                ?: return true
        if (!behandling.erKlageEllerOmgjøring) return true
        return behandling.erSøknadOpprettetEtterHovedsøknad(søknadsid)
    }

    private fun Behandling.erSøknadOpprettetEtterHovedsøknad(søknadsid: Long) = roller.any { it.finnSøknad(søknadsid)?.opprettetEtterHovedsøknad == true }

    /**
     * Hvis et barn med FF-klagesøknad i behandlingen også er med i en tilknyttet søknad som ikke er FF,
     * så feilregistreres FF-klagesøknaden for barnet og den andre søknaden beholdes.
     */
    internal fun feilregistrerFFKlagesøknaderErstattetAvOpprettetSøknad(behandling: Behandling) {
        finnBarnIBådeFFOgKlagesøknad(behandling)
            .groupBy { it.ffSøknadsid }
            .forEach { (ffSøknadsid, barnSomErstattes) -> feilregistrerBarnFraFFSøknad(behandling, ffSøknadsid, barnSomErstattes) }
    }

    private data class BarnIFFOgKlagesøknad(
        val barn: Rolle,
        val ffSøknadsid: Long,
        val klagesøknadsid: Long,
    )

    /** Finner barn som er med i både en åpen FF-søknad i behandlingen og en annen (ikke-FF) søknad tilknyttet hovedsøknaden */
    private fun finnBarnIBådeFFOgKlagesøknad(behandling: Behandling): List<BarnIFFOgKlagesøknad> {
        val hovedsøknadsid = behandling.soknadsid!!
        val (rollerIFFOpprettetSøknader, rollerIKlagesøknader) =
            hentÅpneSøknaderForBehandling(behandling)
                .filter { it.søknadsid != null && it.behandlingstype != null }
                .partition { it.behandlingstype!!.erForholdsmessigFordeling }
        val rollerIFFOpprettetSøknaderMap = rollerIFFOpprettetSøknader.mapNotNull { r ->
            behandling.roller.find { it.erSammeRolle(r.kravhaverIdent, r.stønadstype) }?.let { it to r.søknadsid!! }
        }

        val rollerIFFSøknaderLagret = behandling.søknadsbarn.flatMap { barn ->
            barn.forholdsmessigFordeling
                ?.søknaderUnderBehandling
                ?.filter { it.behandlingstype?.erForholdsmessigFordeling == true && it.søknadsid != null && it.søknadsid != hovedsøknadsid }
                ?.map { barn to it.søknadsid!! }
                .orEmpty()
        }
        val rollerIFFSøknader = (rollerIFFSøknaderLagret + rollerIFFOpprettetSøknaderMap).distinct()

        return rollerIFFSøknader.mapNotNull { (barn, ffSøknadsid) ->
            val klage = rollerIKlagesøknader.find { it.gjelder(barn) } ?: return@mapNotNull null
            BarnIFFOgKlagesøknad(barn, ffSøknadsid = ffSøknadsid, klagesøknadsid = klage.søknadsid!!)
        }
    }

    private fun OpprettetSøknad.gjelder(barn: Rolle) = kravhaverIdent == barn.ident && stønadstype == barn.stønadstype

    /**
     * Feilregistrerer hele FF-søknaden hvis ingen andre barn er igjen i den, ellers kun barna.
     * Markerer at klagesøknaden til barna erstatter FF-søknaden.
     */
    private fun feilregistrerBarnFraFFSøknad(
        behandling: Behandling,
        ffSøknadsid: Long,
        barnSomErstattes: List<BarnIFFOgKlagesøknad>,
    ) {
        KLAGE_LOGGER.info {
            "Barn med FF-klagesøknad $ffSøknadsid har annen søknad i behandling ${behandling.id}. Feilregistrerer FF-klagesøknaden for barna."
        }
        val barn = barnSomErstattes.map { it.barn }
        val harAndreBarnIFFSøknad = behandling.søknadsbarn.any { it !in barn && it.finnSøknad(ffSøknadsid) != null }

        val feilregistrerteBarn =
            if (harAndreBarnIFFSøknad) {
                barn.filter { søknadService.feilregistrerBarnFraSøknad(it, ffSøknadsid) != null }
            } else {
                val ffSøknad = barn.first().finnSøknad(ffSøknadsid) ?: bbmConsumer.hentSøknad(ffSøknadsid)!!.søknad.tilForholdsmessigFordelingSøknad()
                if (søknadService.feilregistrerSøknad(ffSøknad, behandling)) barn else emptyList()
            }

        barnSomErstattes
            .filter { it.barn in feilregistrerteBarn }
            .forEach { it.barn.finnSøknad(it.klagesøknadsid)?.erstatterFFKlagesøknadsid = ffSøknadsid }
    }

    private fun gjennopprettKlagesøknaderIkkeFFEtterAnnenSøknadSlettet(behandling: Behandling) {
        val hovedsøknad = bbmConsumer.hentSøknad(behandling.soknadsid!!)!!.søknad
        val relevanteKravhavere = kravhaverService.hentAlleRelevanteKravhavere(behandling).toMutableSet()
        val åpneSøknaderForVedtaksid = hentÅpneSøknaderForVedtak(behandling)
        val rollerITilknyttedeSøknader = finnAlleBarnIOpprettetSøknader(behandling.soknadsid!!)
        opprettKlagesøknaderForTilknyttedeSøknader(
            behandling,
            hovedsøknad,
            relevanteKravhavere,
            åpneSøknaderForVedtaksid,
            behandling.soknadsid!!,
            rollerITilknyttedeSøknader,
        )
    }
    private fun gjenopprettFFKlagesøknaderErstattetAvSøknad(behandling: Behandling) {
        if (!behandlingService.behandlingFinnes(behandling.id!!)) return
        val relevanteKravhavere = kravhaverService.hentAlleRelevanteKravhavere(behandling).toMutableSet()
        val rollerITilknyttedeSøknader = finnAlleBarnIOpprettetSøknader(behandling.soknadsid!!)
        val behandlerEnhet = kravhaverService.finnEnhetForBarnIBehandling(behandling, behandling.behandlerEnhet)

        val gjenværendeKravhavere =
            relevanteKravhavere
                .filter { rk -> rollerITilknyttedeSøknader.none { it.kravhaverIdent == rk.kravhaver && it.stønadstype == rk.stønadstype } }
                .toSet()
        opprettRevurderingssøknaderForGjenværendeKravhavere(
            behandling,
            gjenværendeKravhavere,
            behandlerEnhet,
            null,
        )
    }

    /**
     * Korrigerer FF-klagesøknader ved synkronisering:
     * - Gjenoppretter FF-klagesøknader hvis søknaden som erstattet dem er lukket (feks slettet uten at det ble fanget opp)
     * - Feilregistrerer FF-klagesøknader for barn som har en åpen søknad opprettet etter hovedsøknaden
     */
    fun korrigerFFKlagesøknaderForSøknaderOpprettetEtterHovedsøknad(behandling: Behandling) {
        if (behandling.soknadsid == null) return
        gjenopprettFFKlagesøknaderErstattetAvSøknad(behandling)
        feilregistrerFFKlagesøknaderErstattetAvOpprettetSøknad(behandling)
    }

    /** Finner eldste åpne søknad som er opprettet etter hovedsøknaden og som kan overta som hovedsøknad */
    private fun Behandling.finnSøknadSomKanBliHovedsøknad(hovedsøknadsid: Long) = roller
        .flatMap { it.forholdsmessigFordeling?.søknaderUnderBehandling ?: emptyList() }
        .filter { it.opprettetEtterHovedsøknad && it.søknadsid != null && it.søknadsid != hovedsøknadsid }
        .mapNotNull { it.søknadsid }
        .minOrNull()

    /**
     * Finner tilknyttede søknader fra påklaget vedtak og oppretter klagesøknader for dem.
     * Returnerer liste over søknadsbarn (ident + stønadstype) som ble håndtert.
     */
    private fun opprettKlagesøknaderForTilknyttedeSøknader(
        behandling: Behandling,
        opprettetSøknad: HentSøknad,
        relevanteKravhavere: Set<SakKravhaver>,
        åpneSøknaderForVedtaksid: List<HentSøknad>,
        hovedsøknadsid: Long,
        rollerITilknyttedeSøknader: List<OpprettetSøknad>,
    ): List<Pair<String?, Stønadstype?>> {
        val vedtak = behandling.omgjøringsdetaljer!!.omgjørVedtakId?.let { hentVedtak(it) }
        val søknaderOpprinneligVedtak =
            vedtak?.stønadsendringListe?.mapNotNull {
                vedtak.grunnlagListe.hentSøknadForPerson(it.kravhaver, it.type)
            } ?: emptyList()
        val tilknyttedeSøknaderOmgjortSøknad =
            bbmConsumer.finnSammenknytningerHovedsøknad(
                behandling.omgjøringsdetaljer!!.soknadRefId!!,
                SøknadsknytningStatus.Deaktiv,
            )

        val tilknyttedeSøknaderOmgjortSøknadFiltrert =
            if (søknaderOpprinneligVedtak.isNotEmpty()) {
                tilknyttedeSøknaderOmgjortSøknad.copy(
                    søknader =
                    tilknyttedeSøknaderOmgjortSøknad.søknader.filter { tilknyttetSøknad ->
                        søknaderOpprinneligVedtak.any { it.søknadsid == tilknyttetSøknad.søknadsid }
                    },
                )
            } else {
                tilknyttedeSøknaderOmgjortSøknad
            }

        val tilknyttetSøknaderIkkeHovedsøknad =
            filtrerTilknyttedeSøknaderForKlage(
                tilknyttedeSøknaderOmgjortSøknadFiltrert,
                relevanteKravhavere,
                behandling.søknadsbarn,
                behandling.soknadsid!!,
            ).filter { søknad ->
                rollerITilknyttedeSøknader.none { søknad.søknadsid == it.refSøknadsid }
            }

        val søknadsbarnOpprettetSøknad =
            opprettetSøknad.parterUnderBehandling.map {
                it.personident to opprettetSøknad.behandlingstema.tilStønadstype()
            }

        val håndterteSøknadsbarn = søknadsbarnOpprettetSøknad.toMutableSet()
        tilknyttetSøknaderIkkeHovedsøknad.forEach { tilknyttetSøknad ->
            val søknadsbarnITilknyttetSøknad =
                tilknyttetSøknad.parterVedtakFattet.map {
                    it.personident to tilknyttetSøknad.behandlingstema.tilStønadstype()
                }
            val alleBarnHarAlleredeSøknad =
                søknadsbarnITilknyttetSøknad.isNotEmpty() && håndterteSøknadsbarn.containsAll(søknadsbarnITilknyttetSøknad)
            if (alleBarnHarAlleredeSøknad) {
                KLAGE_LOGGER.info {
                    "Alle barn i tilknyttet søknad ${tilknyttetSøknad.søknadsid} har allerede en opprettet klagesøknad. " +
                        "Oppretter ikke ny klagesøknad."
                }
                return@forEach
            }
            opprettKlageSøknad(
                tilknyttetSøknad,
                behandling,
                åpneSøknaderForVedtaksid,
                hovedsøknadsid,
            )
            håndterteSøknadsbarn.addAll(søknadsbarnITilknyttetSøknad)
        }

        return håndterteSøknadsbarn.toList()
    }

    /** Filtrerer tilknyttede søknader til kun de som er relevante for klagebehandling */
    private fun filtrerTilknyttedeSøknaderForKlage(
        tilknyttedeSøknaderOmgjortSøknad: FinnSammenknytningerHovedsøknadResponse,
        relevanteKravhavere: Set<SakKravhaver>,
        søknadsbarn: List<Rolle>,
        hovedsøknadsid: Long,
    ): List<HentSøknad> = tilknyttedeSøknaderOmgjortSøknad.søknader
        .filter { !it.behandlingstype.erForholdsmessigFordeling }
        .filter { it.behandlingStatusType == BehandlingStatusType.VEDTAK_FATTET }
        .filter { søknad ->
            val parterISøknad = søknad.parterVedtakFattet.map { it.personident!! }
            val søknadHarIngenRelevanteKravhavere =
                relevanteKravhavere.none {
                    parterISøknad.contains(it.kravhaver) &&
                        it.stønadstype == søknad.behandlingstema.tilStønadstype()
                }
            val søknadsbarnErDelAvSøknad =
                søknadsbarn.any { s ->
                    parterISøknad.contains(s.ident) &&
                        s.stønadstype == søknad.behandlingstema.tilStønadstype()
                }
            (søknad.søknadsid != hovedsøknadsid && søknadsbarnErDelAvSøknad) || søknadHarIngenRelevanteKravhavere
        }

    /** Fjerner søknader uten omgjøringsreferanse fra alle roller */
    private fun fjernSøknaderSomIkkeErDelAvKlagebehandlingen(behandling: Behandling) {
        behandling.roller.forEach {
            it.forholdsmessigFordeling?.søknader?.removeIf { søknad -> søknad.omgjørSøknadsid == null }
        }
    }

    /** Oppretter revurderingssøknader for kravhavere som ikke allerede er håndtert via ordinære søknader */
    private fun opprettRevurderingssøknaderForGjenværendeKravhavere(
        behandling: Behandling,
        relevanteKravhavere: Set<SakKravhaver>,
        behandlerEnhet: String,
        request: OpprettFFRequest?,
    ) {
        val eldsteSøktFomDato = behandling.eldsteSøktFomDatoSøknadsbarn

        val tilknyttedeSøknaderOmgjortSøknad =
            bbmConsumer
                .finnSammenknytningerHovedsøknad(
                    behandling.omgjøringsdetaljer!!.soknadRefId!!,
                    SøknadsknytningStatus.Deaktiv,
                ).søknader
                .filter { it.behandlingStatusType == BehandlingStatusType.VEDTAK_FATTET }

        relevanteKravhavere
            .sortedByDescending { it.stønadstype }
            .groupBy { kravhaver ->
                val søktFomDato =
                    finnSøktFomDatoForKravhaver(
                        relevanteKravhavere,
                        kravhaver,
                        behandling,
                        request,
                    )
                val tilknyttetSøknad =
                    tilknyttedeSøknaderOmgjortSøknad
                        .filter {
                            it.behandlingstema.tilStønadstype() == kravhaver.stønadstype &&
                                it.behandlingstype.erForholdsmessigFordeling
                        }.find { it.parterVedtakFattet.finnBarn(kravhaver.kravhaver) != null }
                Triple(
                    kravhaver.saksnummer!!,
                    kravhaver.stønadstype,
                    maxOfNullable(eldsteSøktFomDato, tilknyttetSøknad?.søknadFomDato ?: søktFomDato)!!,
                )
            }.forEach { (saksnummerLøpendeBidrag, løpendebidragssaker) ->
                val saksnummer = saksnummerLøpendeBidrag.first
                val søktFomDato = saksnummerLøpendeBidrag.third
                søknadService.opprettRollerOgRevurderingssøknadForSak(
                    behandling,
                    saksnummer,
                    løpendebidragssaker,
                    behandlerEnhet,
                    saksnummerLøpendeBidrag.second,
                    søktFomDato,
                    true,

                )
            }
    }

    fun opprettKlageSøknad(
        originalSøknad: HentSøknad,
        behandling: Behandling,
        åpneSøknaderForVedtaksid: List<HentSøknad>,
        hovedsøknadsid: Long?,
        mottattDato: LocalDate? = null,
    ): Long {
        val hovedsøknad = hovedsøknadsid?.let { bbmConsumer.hentSøknad(it)?.søknad }
        val behandlingstype =
            if (originalSøknad.behandlingstype.erForholdsmessigFordeling) {
                Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE
            } else {
                behandling.søknadstype!!
            }
        val barnIOriginalSøknad =
            behandling.roller.filter { it.harSøknad(originalSøknad.søknadsid) }.filter {
                val søknad = it.finnSøknad(originalSøknad.søknadsid)!!
                søknad.status?.lukketStatus == false
            }
        val barnISøknad =
            originalSøknad.partISøknadListe
                .filter { it.rolletype == Rolletype.BARN }
                .filter { barn ->
                    if (barnIOriginalSøknad.isEmpty()) {
                        !barn.behandlingstatus!!.erFeilregistrert
                    } else {
                        barnIOriginalSøknad.any { it.erSammeRolle(barn.personident!!, originalSøknad.behandlingstema.tilStønadstype()) }
                    }
                }

        val barnISøknadIdenter = barnISøknad.map { it.personident }.toSet()
        val løpendeBidraggsakerBP =
            kravhaverService.hentSisteLøpendeStønader(Personident(behandling.bidragspliktig!!.ident!!), behandling.finnBeregningsperiode())
        val åpenFFSøknad =
            åpneSøknaderForVedtaksid.find { søknad ->
                søknad.refSøknadsid == originalSøknad.søknadsid && søknad.behandlingstema == originalSøknad.behandlingstema
            }

        val søktAvType = if (hovedsøknad?.søktAvType == SøktAvType.NAV_BIDRAG) SøktAvType.NAV_BIDRAG else originalSøknad.søktAvType
        val nySøknadId =
            åpenFFSøknad?.søknadsid ?: bbmConsumer
                .opprettSøknader(
                    OpprettSøknadRequest(
                        saksnummer = originalSøknad.saksnummer,
                        behandlingsid = behandling.id,
                        refVedtaksid = behandling.omgjøringsdetaljer?.omgjørVedtakId,
                        refSøknadsid = originalSøknad.søknadsid,
                        behandlingstype = behandlingstype,
                        behandlerenhet = originalSøknad.behandlerenhet ?: behandling.behandlerEnhet,
                        hovedsøknadsid = hovedsøknadsid,
                        søktAv = søktAvType,
                        søknadMottattDato = mottattDato ?: behandling.mottattdato,
                        behandlingstema = originalSøknad.behandlingstema,
                        søknadFomDato = originalSøknad.søknadFomDato!!,
                        innkreving = originalSøknad.innkreving,
                        barnListe = barnISøknad.map { Barn(it.personident!!, it.innbetaltBeløp) },
                    ),
                ).søknadsid
        val forholdsmessigFordelingSøknad =
            ForholdsmessigFordelingSøknadBarn(
                søknadsid = nySøknadId,
                mottattDato = behandling.mottattdato,
                søknadFomDato = originalSøknad.søknadFomDato,
                søktAvType = søktAvType,
                behandlingstype = behandlingstype,
                behandlingstema = originalSøknad.behandlingstema,
                innkreving = originalSøknad.innkreving,
                saksnummer = originalSøknad.saksnummer,
                enhet = originalSøknad.behandlerenhet ?: behandling.behandlerEnhet,
                omgjørSøknadsid = originalSøknad.søknadsid,
                omgjørVedtaksid = behandling.omgjøringsdetaljer?.omgjørVedtakId,
                status = Behandlingstatus.UNDER_BEHANDLING,
            )
        behandling.søknadsbarn
            .filter {
                barnISøknadIdenter.contains(it.ident) && it.stønadstype == originalSøknad.behandlingstema.tilStønadstype() &&
                    !it.harSøknad(nySøknadId)
            }.forEach {
                val løpendeBidrag =
                    løpendeBidraggsakerBP.hentBidragSakForKravhaver(it.ident!!, it.stønadstype)
                it.forholdsmessigFordeling!!.let {
                    it.søknader.add(forholdsmessigFordelingSøknad)
                    it.løperBidragFra = løpendeBidrag?.periodeFra
                    it.løperBidragTil = løpendeBidrag?.periodeTil
                    it.harLøpendeBidrag =
                        løpendeBidrag?.løperBidragEtterDato(behandling.finnBeregnTilDato().toYearMonth()) == true
                }

                it.innkrevingstype = if (originalSøknad.innkreving) Innkrevingstype.MED_INNKREVING else Innkrevingstype.UTEN_INNKREVING
                it.bidragsmottaker!!
                    .forholdsmessigFordeling!!
                    .søknader
                    .add(forholdsmessigFordelingSøknad)
            }
        if (!behandling.bidragspliktig!!.harSøknad(nySøknadId)) {
            behandling.bidragspliktig!!
                .forholdsmessigFordeling!!
                .søknader
                .add(forholdsmessigFordelingSøknad)
        }
        return nySøknadId
    }
}

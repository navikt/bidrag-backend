package no.nav.bidrag.sak.service

import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.unleash.DefaultUnleashContextProvider
import no.nav.bidrag.commons.util.IdentConsumer
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.SøknadGruppeKombinasjon
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Fogdårsak
import no.nav.bidrag.domene.enums.sak.Tilgangstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.sak.config.UnleashFeatures
import no.nav.bidrag.sak.domain.Bidragssak
import no.nav.bidrag.sak.domain.Tilgang
import no.nav.bidrag.sak.dto.FogdhistorikkDto
import no.nav.bidrag.sak.dto.NySakCommandDto
import no.nav.bidrag.sak.dto.NySakResponseDto
import no.nav.bidrag.sak.dto.SakshendelseDto
import no.nav.bidrag.sak.dto.tilFogdhistorikkDto
import no.nav.bidrag.sak.integration.kodeverk.CachedKodeverkService
import no.nav.bidrag.sak.mapper.BidragssakMapper.toBidragssak
import no.nav.bidrag.sak.mapper.BidragssakMapper.toOpprettSakResponse
import no.nav.bidrag.sak.mapper.RolleMapper.toRolleDto
import no.nav.bidrag.sak.repository.BidragssakRepository
import no.nav.bidrag.sak.repository.HendelseRepository
import no.nav.bidrag.sak.repository.RolleRepository
import no.nav.bidrag.sak.repository.VedtakOverføringRepository
import no.nav.bidrag.sak.repository.findByIdOrThrow
import no.nav.bidrag.sak.util.VEDTAK_LINK
import no.nav.bidrag.sak.util.erBisysVedtakOgErOverført
import no.nav.bidrag.sak.util.erKlageberettigetVedtak
import no.nav.bidrag.sak.util.resultatIBisys
import no.nav.bidrag.sak.util.tilEngangsbeløptype
import no.nav.bidrag.sak.util.tilStønadstype
import no.nav.bidrag.sak.util.tilVedtakstype
import no.nav.bidrag.sak.validering.OpprettSakValidator
import no.nav.bidrag.transport.sak.BidragssakDto
import no.nav.bidrag.transport.sak.BidragssakPipDto
import no.nav.bidrag.transport.sak.FjernMidlertidligTilgangRequest
import no.nav.bidrag.transport.sak.OppdaterRollerISakRequest
import no.nav.bidrag.transport.sak.OppdaterSakRequest
import no.nav.bidrag.transport.sak.OppdaterSakResponse
import no.nav.bidrag.transport.sak.OpprettMidlertidligTilgangRequest
import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.OpprettSakResponse
import no.nav.bidrag.transport.sak.RolleDto
import no.nav.bidrag.transport.sak.SamhandlerSakerDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

fun Set<Tilgang>.finnMidlertidligTilgang(
    enhet: String,
    årsak: Fogdårsak?,
) = find {
    it.enhetsnummer == enhet && (årsak == null || it.årsak == årsak) && it.type == Tilgangstype.MIDL
}

@Service
class BidragSakService(
    private val bidragssakRepository: BidragssakRepository,
    private val rolleRepository: RolleRepository,
    private val tilgangClient: Tilgangskontroll,
    private val cachedKodeverkService: CachedKodeverkService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val rolleService: RolleService,
    private val rollehistorikkService: RollehistorikkService,
    private val hendelseService: HendelseService,
    private val identConsumer: IdentConsumer,
    private val opprettSakValidator: OpprettSakValidator,
    private val hendelseRepository: HendelseRepository,
    private val vedtakOverføringRepository: VedtakOverføringRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun finnPipFor(saksnummer: Saksnummer): BidragssakPipDto? {
        val sakInfo =
            bidragssakRepository.findBidragssakPipInfoBySaksnummer(saksnummer.verdi)
                ?: return null

        val roller =
            rolleRepository
                .findPipInfoBySaksnummer(saksnummer.verdi)
                .filter { it.erPerson() }
                .mapNotNull { it.fødselsnummer }
                .distinct()

        return BidragssakPipDto(
            saksnummer = saksnummer,
            avsluttet = sakInfo.avsluttetTidspunkt != null && sakInfo.avsluttetTidspunkt <= LocalDate.now(),
            roller = roller,
        )
    }

    fun finnSakMetadata(
        saksnummer: Saksnummer,
        visRollehistorikk: Boolean,
    ): BidragssakDto? {
        val bidragssak = bidragssakRepository.findBySaksnummer(saksnummer.verdi)
        secureLogger.debug { "Hentet sak metadata for sak $saksnummer: $bidragssak" }
        return bidragssak?.tilBidragSakDto(begrensetTilgang(saksnummer), null, visRollehistorikk)
    }

    @Deprecated("Midlertidig løsning til nye AD-gupper er delt ut til alle sakbehandlere 2026-07-03")
    fun harSkrivetilgang(
        saksnummer: Saksnummer,
        enhet: String,
    ): Boolean {
        val sak = bidragssakRepository.findBySaksnummer(saksnummer.verdi) ?: return false
        val now = LocalDate.now()
        return sak.tilganger.any { tilgang ->
            tilgang.enhetsnummer == enhet &&
                !tilgang.tilgangFomDato.isAfter(now) &&
                (tilgang.tilgangTomDato == null || !tilgang.tilgangTomDato!!.isBefore(now))
        }
    }

    fun finnFogdhistorikk(saksnummer: Saksnummer): List<FogdhistorikkDto> {
        if (begrensetTilgang(saksnummer)) {
            logger.warn("Henter ikke sak fogdhistorikk for sak $saksnummer pga begrenset tilgang")
            return emptyList()
        }
        val bidragssak = bidragssakRepository.findBySaksnummer(saksnummer.verdi)
        secureLogger.debug { "Hentet sak metadata for sak $saksnummer: $bidragssak" }
        return bidragssak?.tilganger?.map { it.tilFogdhistorikkDto() }?.sortedBy { it.tilgangId } ?: emptyList()
    }

    fun finnSakerForSamhandler(samhandlerId: String): SamhandlerSakerDto {
        val saker = rolleRepository.samhandlereForSak(samhandlerId)
        return SamhandlerSakerDto(
            antallSaker = saker.size,
            saksnummere = saker,
        )
    }

    fun finnSakerFor(fodselsnummer: Personident): List<BidragssakDto> = bidragssakRepository
        .findByRoller(identConsumer.hentAlleIdenter(fodselsnummer.verdi))
        .filter {
            DefaultUnleashContextProvider.updateSaksnummer(it.saksnummer)
            !it.erAvsluttet() || UnleashFeatures.TILGANG_TIL_AVSLUTTET_SAK.isEnabled
        }.map { it.tilBidragSakDto(begrensetTilgang(Saksnummer(it.saksnummer)), fodselsnummer) }

    fun nySak(nySakCommandDto: NySakCommandDto): NySakResponseDto {
        val nyttSaksnummer = hentNyttSaksnummerFraDatabase()
        val bidragssak =
            Bidragssak(
                nyttSaksnummer.verdi,
                nySakCommandDto.eierfogd.verdi,
            )
        bidragssak.tilganger.addAll(
            listOf(
                Tilgang(
                    enhetsnummer = nySakCommandDto.eierfogd.verdi,
                    bidragssak = bidragssak,
                ),
            ),
        )
        bidragssakRepository.save(bidragssak)
        secureLogger.info { "(Gammel api) Opprettet sak $bidragssak med saksnummer $nyttSaksnummer for request $nySakCommandDto" }
        return NySakResponseDto(nyttSaksnummer)
    }

    private fun hentNyttSaksnummerFraDatabase(): Saksnummer {
        val maxLopenummer =
            bidragssakRepository.hentMaxLoepenummerSomIkkeOverskrider(
                SaksnummerSerie.hentMaksimumsgrenseForAarstall(),
            )
        val nyttSaksnummer: String =
            if (
                maxLopenummer == null || maxLopenummer < SaksnummerSerie.hentMinimumsgrenseForAarstall()
            ) {
                SaksnummerSerie.hentMinimumsgrenseForAarstall().toString()
            } else {
                (maxLopenummer + 1).toString()
            }
        return Saksnummer(nyttSaksnummer)
    }

    /** Er tilgang til saken begrenset for pålogget saksbehandler */
    fun begrensetTilgang(saksnummer: Saksnummer) = !tilgangClient.harTilgangSaksnummer(saksnummer)

    @Transactional
    fun opprettSak(opprettSakRequest: OpprettSakRequest): OpprettSakResponse {
        opprettSakValidator.valider(opprettSakRequest)

        val bPFnr =
            opprettSakRequest.roller
                .firstOrNull { it.type == Rolletype.BIDRAGSPLIKTIG }
                ?.fødselsnummer
                ?.verdi

        val bMFnr =
            opprettSakRequest.roller
                .firstOrNull { it.type == Rolletype.BIDRAGSMOTTAKER }
                ?.fødselsnummer
                ?.verdi

        // Henter alle BPs bidragssaker. Sjekk mot arbeidsfordeling utelater oppfostringsbidrag.
        // Sjekk at det ikke finnes eksisterende sak med BP og BM fra request. Hvis det finnes så returneres denne sakens saksnummer.
        if (!bMFnr.isNullOrEmpty()) {
            val eksisterendeSaker =
                bPFnr?.let {
                    bidragssakRepository
                        .findByRoller(listOf(it))
                        .filter { it.arbeidsfordeling == Arbeidsfordeling.EIERENHET }
                        .filter { sak ->
                            sak.roller.any {
                                it.rolleType == Rolletype.BIDRAGSMOTTAKER && it.fødselsnummer == bMFnr
                            }
                        }
                } ?: emptyList()

            if (eksisterendeSaker.isNotEmpty()) {
                secureLogger.info {
                    "Opprett sak. Det finnes allerede minst én eksisterende sak for BP: $bPFnr request: $opprettSakRequest}"
                }
                return OpprettSakResponse(Saksnummer(eksisterendeSaker.first().saksnummer))
            }
        }

        val fødselsdatoer = hentFødselsdatoer(opprettSakRequest)
        val saksnummer = hentNyttSaksnummerFraDatabase()
        val bidragssak = opprettSakRequest.toBidragssak(saksnummer, fødselsdatoer)
        val opprettetBidragssak = bidragssakRepository.save(bidragssak)

        val rollerFørOppdatering = opprettetBidragssak.roller.toRolleDto(true)

        val oppdatertBidragssak =
            oppdaterBidragsaksrollerMedReelleMottagere(opprettetBidragssak, opprettSakRequest.roller)
        val rollerOppdatertMedRollehistorikk =
            rollehistorikkService.oppdaterRollehistorikk(
                rollerFørOppdatering,
                opprettSakRequest.roller,
                oppdatertBidragssak,
            )
        oppdatertBidragssak.roller = rollerOppdatertMedRollehistorikk

        hendelseService.opprettKafkaHendelse(null, oppdatertBidragssak)

        secureLogger.info { "(Nytt api) Opprettet sak $bidragssak med saksnummer $saksnummer for request $opprettSakRequest" }
        return oppdatertBidragssak.toOpprettSakResponse()
    }

    @Transactional
    fun fjernMidlertidligTilgangSak(request: FjernMidlertidligTilgangRequest) {
        val sak = bidragssakRepository.findByIdOrThrow(request.saksnummer)
        val tilgang =
            sak.tilganger.finnMidlertidligTilgang(request.enhet, request.årsak) ?: run {
                logger.info(
                    "Fant ikke midlertidlig tilgang med årsak ${request.årsak} for enhet ${request.enhet} til sak ${sak.saksnummer}",
                )
                return
            }
        if (sak.eierfogd == tilgang.enhetsnummer) {
            logger.info("Kan ikke fjerne midlertidlig tilgang for enhet ${request.enhet} som er eierfogd for sak ${sak.saksnummer}")
            return
        }
        tilgang.tilgangTomDato = LocalDate.now().minusDays(1)
        bidragssakRepository.save(sak)
    }

    @Transactional
    fun opprettEllerUtvidMidlertidligTilgangSak(request: OpprettMidlertidligTilgangRequest) {
        val sak = bidragssakRepository.findByIdOrThrow(request.saksnummer)
        if (sak.eierfogd == request.enhet) {
            logger.info("Enhet ${request.enhet} har allerede tilgang til sak ${sak.saksnummer}")
            return
        }
        val eksisterendeTilgang =
            sak.tilganger.finnMidlertidligTilgang(request.enhet, request.årsak)
        if (eksisterendeTilgang != null &&
            (eksisterendeTilgang.tilgangTomDato == null || eksisterendeTilgang.tilgangTomDato!! >= LocalDate.now())
        ) {
            logger.info("Enhet ${request.enhet} har allerede tilgang til sak ${sak.saksnummer}")
            return
        }
        if (eksisterendeTilgang != null) {
            logger.info(
                "Fant en eksisterende tilgang fra før med tilgangTomDato = ${eksisterendeTilgang.tilgangTomDato}. Utvider tilgangen",
            )
            eksisterendeTilgang.tilgangTomDato = request.tilgangTilOgMedDato
            bidragssakRepository.save(sak)
            return
        }
        sak.tilganger.addAll(
            listOf(
                Tilgang(
                    enhetsnummer = request.enhet,
                    tilgangTomDato = request.tilgangTilOgMedDato,
                    bidragssak = sak,
                    årsak = Fogdårsak.MAKO,
                    type = Tilgangstype.MIDL,
                    opprettetAv = TokenUtils.hentSaksbehandlerIdent(),
                ),
            ),
        )
        bidragssakRepository.save(sak)
    }

    @Transactional
    fun oppdaterRollerISak(oppdaterSakRequest: OppdaterRollerISakRequest): OppdaterSakResponse {
        val sak = bidragssakRepository.findByIdOrThrow(oppdaterSakRequest.saksnummer.verdi)
        val rollerFørOppdatering = sak.roller.toRolleDto(true)

        oppdaterSakRequest.roller.forEach { opprettSakValidator.validerRolle(it) }

        sak.apply {
            roller = rolleService.oppdaterRoller(sak, oppdaterSakRequest.roller).toMutableSet()
        }

        bidragssakRepository.save(sak)

        val lagretSak = bidragssakRepository.findByIdOrThrow(oppdaterSakRequest.saksnummer.verdi)
        val oppdatertBidragssak = oppdaterBidragsaksrollerMedReelleMottagere(lagretSak, oppdaterSakRequest.roller)
        val rollerOppdatertMedRollehistorikk =
            rollehistorikkService.oppdaterRollehistorikk(
                rollerFørOppdatering,
                oppdaterSakRequest.roller,
                oppdatertBidragssak,
            )
        oppdatertBidragssak.roller = rollerOppdatertMedRollehistorikk

        hendelseService.opprettKafkaHendelse(sak, oppdatertBidragssak)
        return oppdatertBidragssak.tilOppdaterSakResponse()
    }

    @Transactional
    fun oppdaterSak(oppdaterSakRequest: OppdaterSakRequest): OppdaterSakResponse {
        val sak = bidragssakRepository.findByIdOrThrow(oppdaterSakRequest.saksnummer.verdi)

        require(
            oppdaterSakRequest.landkode == null ||
                cachedKodeverkService
                    .hentLandkoder()
                    .containsKey(oppdaterSakRequest.landkode),
        ) {
            "Bidragssak ${oppdaterSakRequest.saksnummer} forsøkt oppdatert med ugyldig land: ${oppdaterSakRequest.landkode}"
        }

        val rollerFørOppdatering = sak.roller.toRolleDto(true)

        oppdaterSakRequest.roller.forEach { opprettSakValidator.validerRolle(it) }

        sak.apply {
            status = oppdaterSakRequest.status ?: sak.status
            ansatt = oppdaterSakRequest.ansatt ?: sak.ansatt
            inhabilitet = oppdaterSakRequest.inhabilitet ?: sak.inhabilitet
            levdeAdskilt = oppdaterSakRequest.levdeAdskilt ?: sak.levdeAdskilt
            sanertDato = oppdaterSakRequest.sanertDato ?: sak.sanertDato
            arbeidsfordeling = oppdaterSakRequest.arbeidsfordeling ?: sak.arbeidsfordeling
            kategori = oppdaterSakRequest.kategorikode ?: sak.kategori
            land = oppdaterSakRequest.landkode?.verdi ?: sak.land
            konvensjon = oppdaterSakRequest.konvensjonskode ?: sak.konvensjon
            konvensjonsdato = oppdaterSakRequest.konvensjonsdato ?: sak.konvensjonsdato
            ffuReferansenr = oppdaterSakRequest.ffuReferansenr ?: sak.ffuReferansenr
            roller = rolleService.oppdaterRoller(sak, oppdaterSakRequest.roller).toMutableSet()
        }

        arbeidsfordelingService.utførArbeidsfordeling(sak)
//        hendelseService.opprettHendelser(sak, oppdaterSakRequest)  // TODO: Dette fungerer ikke
        bidragssakRepository.save(sak)

        val lagretSak = bidragssakRepository.findByIdOrThrow(oppdaterSakRequest.saksnummer.verdi)
        val oppdatertBidragssak = oppdaterBidragsaksrollerMedReelleMottagere(lagretSak, oppdaterSakRequest.roller)
        val rollerOppdatertMedRollehistorikk =
            rollehistorikkService.oppdaterRollehistorikk(
                rollerFørOppdatering,
                oppdaterSakRequest.roller,
                oppdatertBidragssak,
            )

        oppdatertBidragssak.roller = rollerOppdatertMedRollehistorikk

        hendelseService.opprettKafkaHendelse(sak, oppdatertBidragssak)
        return oppdatertBidragssak.tilOppdaterSakResponse()
    }

    fun finnHendelserForSak(saksnummer: Saksnummer): List<SakshendelseDto> {
        if (begrensetTilgang(saksnummer)) {
            logger.warn("Henter ikke sakshistorikk for sak $saksnummer pga begrenset tilgang")
            return emptyList()
        }

        val barnObjektNumre =
            rolleRepository
                .findByBySaksnummerAndRolleType(saksnummer.verdi, Rolletype.BARN)
                .mapNotNull { it.objektnummer }

        return hendelseRepository
            .findBySaksnummer(saksnummer.verdi)
            .mapNotNull { hendelse ->
                val hendelseType =
                    hendelse.type ?: run {
                        logger.warn(
                            "Hendelse ${hendelse.hendelseId} for sak $saksnummer har ukjent HEND_TYPE — hopper over",
                        )
                        return@mapNotNull null
                    }
                val link =
                    if (hendelse.erVedtak) {
                        VEDTAK_LINK
                    } else if (hendelse.erForskudd) {
                        SøknadGruppeKombinasjon.FORSKUDD.kode
                    } else if (hendelse.erSærbidrag) {
                        SøknadGruppeKombinasjon.SÆRBIDRAG.kode
                    } else if (hendelse.erBidrag) {
                        SøknadGruppeKombinasjon.BIDRAG.kode
                    } else {
                        null
                    }

                val søknadsgruppe = hendelse.grKombKode?.let(SøknadGruppeKombinasjon::fraKode)

                val vedtaksid =
                    hendelse.søknad?.id?.let {
                        vedtakOverføringRepository.finnVedtakIdBidragVedtakForSak(saksnummer, it).firstOrNull()?.toString()
                    }

                SakshendelseDto(
                    hendelseId = hendelse.hendelseId?.toString(),
                    opprettetTidspunkt = hendelse.opprettetTidspunkt,
                    enhet = Enhetsnummer(hendelse.enhet),
                    søknadsgruppe = søknadsgruppe,
                    type = hendelseType,
                    resultat = hendelse.resultat,
                    link = link,
                    søknadsid = hendelse.søknad?.id?.toString(),
                    behandlingsid = hendelse.søknad?.behandlingId,
                    vedtaksid = vedtaksid,
                    erLukket =
                    hendelse.søknad
                        ?.søknadslinjer
                        ?.mapNotNull {
                            Behandlingstatus.fraKode(it.statusKode)?.lukketStatus
                        }?.fold(true) { a, b -> a && b } ?: true,
                    resultatIBisys = hendelse.resultatIBisys(link),
                    erBisysVedtakOgErOverført =
                    hendelse.erBisysVedtakOgErOverført { søknadId ->
                        vedtakOverføringRepository.countVedtakGrunnlagOverførtForSak(saksnummer, søknadId)
                    },
                    erKlageberettigetVedtak = hendelse.erKlageberettigetVedtak(),
                    stonadType = søknadsgruppe?.tilStønadstype(),
                    engangsbelopType = søknadsgruppe?.tilEngangsbeløptype(),
                    fraBbm = hendelse.fraBbm,
                    søktAv =
                    hendelse.søknad
                        ?.blankett
                        ?.soknFraKode
                        ?.let { SøktAvType.fraKode(it) },
                    vedtakType =
                    hendelse.søknad
                        ?.blankett
                        ?.soknType
                        ?.tilVedtakstype(),
                    barnObjektNumre = barnObjektNumre,
                )
            }
    }

    private fun oppdaterBidragsaksrollerMedReelleMottagere(
        bidragssak: Bidragssak,
        requestRolleDtoer: Set<RolleDto>,
    ): Bidragssak {
        val rollerMedReelleMottagere = requestRolleDtoer.filter { it.harRM() }
        rolleService.oppdaterRollerMedReelleMottager(bidragssak.roller, rollerMedReelleMottagere)
        return bidragssakRepository.save(bidragssak)
    }

    private fun hentFødselsdatoer(opprettSakRequest: OpprettSakRequest): Map<Personident, LocalDate?> = rolleService.validerRollerOgHentFødselsdatoer(opprettSakRequest.roller)
}

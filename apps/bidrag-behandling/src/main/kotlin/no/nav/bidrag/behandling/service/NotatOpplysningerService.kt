package no.nav.bidrag.behandling.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.behandling.consumer.BidragDokumentConsumer
import no.nav.bidrag.behandling.consumer.BidragDokumentProduksjonConsumer
import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.database.datamodell.Grunnlag
import no.nav.bidrag.behandling.database.datamodell.Inntekt
import no.nav.bidrag.behandling.database.datamodell.Rolle
import no.nav.bidrag.behandling.database.datamodell.Sivilstand
import no.nav.bidrag.behandling.database.datamodell.barn
import no.nav.bidrag.behandling.database.datamodell.hentSisteAktiv
import no.nav.bidrag.behandling.database.datamodell.konvertereData
import no.nav.bidrag.behandling.dto.v1.behandling.ErLikForAlleBasertPåSak
import no.nav.bidrag.behandling.dto.v1.behandling.RolleDto
import no.nav.bidrag.behandling.dto.v1.beregning.DelberegningBarnetilleggDto
import no.nav.bidrag.behandling.dto.v1.beregning.DelberegningBidragsevneDto
import no.nav.bidrag.behandling.dto.v1.beregning.DelberegningBidragspliktigesBeregnedeTotalbidragDto
import no.nav.bidrag.behandling.dto.v2.behandling.GebyrDetaljerDto
import no.nav.bidrag.behandling.dto.v2.behandling.GebyrRolleV2Dto
import no.nav.bidrag.behandling.dto.v2.behandling.Grunnlagsdatatype
import no.nav.bidrag.behandling.dto.v2.behandling.PersoninfoDto
import no.nav.bidrag.behandling.dto.v2.behandling.SærbidragKategoriDto
import no.nav.bidrag.behandling.dto.v2.behandling.SærbidragUtgifterDto
import no.nav.bidrag.behandling.dto.v2.behandling.SøknadDetaljerDto
import no.nav.bidrag.behandling.dto.v2.behandling.TotalBeregningUtgifterDto
import no.nav.bidrag.behandling.dto.v2.behandling.UtgiftBeregningDto
import no.nav.bidrag.behandling.dto.v2.behandling.UtgiftspostDto
import no.nav.bidrag.behandling.dto.v2.behandling.innhentesForRolle
import no.nav.bidrag.behandling.dto.v2.samvær.SamværBarnDto
import no.nav.bidrag.behandling.dto.v2.underhold.UnderholdDto
import no.nav.bidrag.behandling.dto.v2.underhold.UnderholdskostnadDto
import no.nav.bidrag.behandling.service.NotatService.Companion.henteInntektsnotat
import no.nav.bidrag.behandling.service.NotatService.Companion.henteNotatinnhold
import no.nav.bidrag.behandling.transformers.Dtomapper
import no.nav.bidrag.behandling.transformers.Personinfo
import no.nav.bidrag.behandling.transformers.behandling.filtrerSivilstandGrunnlagEtterVirkningstidspunkt
import no.nav.bidrag.behandling.transformers.behandling.hentAlleBearbeidaBoforhold
import no.nav.bidrag.behandling.transformers.behandling.hentAlleBearbeidaBoforholdTilBMSøknadsbarn
import no.nav.bidrag.behandling.transformers.behandling.hentBeregnetInntekterForRolle
import no.nav.bidrag.behandling.transformers.behandling.kanFatteVedtakBegrunnelse
import no.nav.bidrag.behandling.transformers.behandling.notatTittel
import no.nav.bidrag.behandling.transformers.behandling.tilReferanseId
import no.nav.bidrag.behandling.transformers.ekskluderYtelserFørVirkningstidspunkt
import no.nav.bidrag.behandling.transformers.erBidrag
import no.nav.bidrag.behandling.transformers.erForskudd
import no.nav.bidrag.behandling.transformers.erHistorisk
import no.nav.bidrag.behandling.transformers.finnesLøpendeBidragForRolle
import no.nav.bidrag.behandling.transformers.finnesLøpendeForskuddForRolle
import no.nav.bidrag.behandling.transformers.grunnlag.erBarnTilBMUnder12År
import no.nav.bidrag.behandling.transformers.hentNesteEtterfølgendeVedtak
import no.nav.bidrag.behandling.transformers.inntekt.bestemOpprinneligTomVisningsverdi
import no.nav.bidrag.behandling.transformers.kanSkriveVurderingAvSkolegang
import no.nav.bidrag.behandling.transformers.nærmesteHeltall
import no.nav.bidrag.behandling.transformers.sorterEtterDato
import no.nav.bidrag.behandling.transformers.sorterEtterDatoOgBarn
import no.nav.bidrag.behandling.transformers.sortert
import no.nav.bidrag.behandling.transformers.tilDto
import no.nav.bidrag.behandling.transformers.tilType
import no.nav.bidrag.behandling.transformers.utgift.tilSærbidragKategoriDto
import no.nav.bidrag.behandling.transformers.vedtak.mapping.tilvedtak.finnBeregnTilDatoBehandling
import no.nav.bidrag.behandling.transformers.årsinntekterSortert
import no.nav.bidrag.beregn.inntekt.util.InntektUtil
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.service.finnVisningsnavn
import no.nav.bidrag.commons.service.organisasjon.EnhetProvider
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.barnetilsyn.Skolealder
import no.nav.bidrag.domene.enums.barnetilsyn.Tilsynstype
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.diverse.InntektBeløpstype
import no.nav.bidrag.domene.enums.diverse.Kilde
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.DatoperiodeDto
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.domene.util.visningsnavn
import no.nav.bidrag.transport.behandling.felles.grunnlag.NotatGrunnlag.NotatType
import no.nav.bidrag.transport.behandling.grunnlag.response.ArbeidsforholdGrunnlagDto
import no.nav.bidrag.transport.behandling.grunnlag.response.BarnetilsynGrunnlagDto
import no.nav.bidrag.transport.behandling.grunnlag.response.RelatertPersonGrunnlagDto
import no.nav.bidrag.transport.behandling.grunnlag.response.SivilstandGrunnlagDto
import no.nav.bidrag.transport.behandling.grunnlag.response.TilleggsstønadGrunnlagDto
import no.nav.bidrag.transport.dokument.JournalpostType
import no.nav.bidrag.transport.dokument.OpprettDokumentDto
import no.nav.bidrag.transport.dokument.OpprettJournalpostRequest
import no.nav.bidrag.transport.dokument.OpprettJournalpostResponse
import no.nav.bidrag.transport.dokumentmaler.DokumentmalDelberegningBarnetilleggDto
import no.nav.bidrag.transport.dokumentmaler.DokumentmalDelberegningBidragsevneDto
import no.nav.bidrag.transport.dokumentmaler.DokumentmalDelberegningBidragspliktigesBeregnedeTotalbidragDto
import no.nav.bidrag.transport.dokumentmaler.DokumentmalForholdsmessigFordelingBeregningsdetaljer
import no.nav.bidrag.transport.dokumentmaler.DokumentmalForholdsmessigFordelingBidragTilFordeling
import no.nav.bidrag.transport.dokumentmaler.DokumentmalForholdsmessigFordelingBidragTilFordelingBarn
import no.nav.bidrag.transport.dokumentmaler.DokumentmalManuellVedtak
import no.nav.bidrag.transport.dokumentmaler.DokumentmalPersonDto
import no.nav.bidrag.transport.dokumentmaler.DokumentmalResultatBeregningInntekterDto
import no.nav.bidrag.transport.dokumentmaler.DokumentmalResultatBidragsberegningBarnDto
import no.nav.bidrag.transport.dokumentmaler.DokumentmalResultatBidragsberegningBarnDto.ResultatBarnebidragsberegningPeriodeDto
import no.nav.bidrag.transport.dokumentmaler.DokumentmalResultatBidragsberegningBarnDto.ResultatBarnebidragsberegningPeriodeDto.BidragPeriodeBeregningsdetaljer
import no.nav.bidrag.transport.dokumentmaler.DokumentmalResultatBidragsberegningBarnDto.ResultatBarnebidragsberegningPeriodeDto.BidragPeriodeBeregningsdetaljer.IndeksreguleringDetaljer
import no.nav.bidrag.transport.dokumentmaler.DokumentmalResultatBidragsberegningBarnDto.ResultatBarnebidragsberegningPeriodeDto.BidragPeriodeBeregningsdetaljer.NotatBeregningsdetaljerSamværsfradrag
import no.nav.bidrag.transport.dokumentmaler.DokumentmalSluttberegningBarnebidragDetaljer
import no.nav.bidrag.transport.dokumentmaler.notat.Arbeidsforhold
import no.nav.bidrag.transport.dokumentmaler.notat.EndeligOrkestrertVedtak
import no.nav.bidrag.transport.dokumentmaler.notat.InntekterPerRolle
import no.nav.bidrag.transport.dokumentmaler.notat.NotatBegrunnelseDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatBehandlingDetaljerDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatBeregnetInntektDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatBeregnetPrivatAvtalePeriodeDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatBoforholdDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatBoforholdTilBMMedSøknadsbarn
import no.nav.bidrag.transport.dokumentmaler.notat.NotatErLikForAlleBasertPåSak
import no.nav.bidrag.transport.dokumentmaler.notat.NotatGebyrDetaljerDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatGebyrDetaljerDto.NotatGebyrInntektDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatGebyrSakDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatGebyrV3Dto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatInntektDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatInntekterDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatInntektspostDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatMaksGodkjentBeløpDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatMalType
import no.nav.bidrag.transport.dokumentmaler.notat.NotatOffentligeOpplysningerUnderhold
import no.nav.bidrag.transport.dokumentmaler.notat.NotatOffentligeOpplysningerUnderholdBarn
import no.nav.bidrag.transport.dokumentmaler.notat.NotatOffentligeOpplysningerUnderholdBarn.NotatBarnetilsynOffentligeOpplysninger
import no.nav.bidrag.transport.dokumentmaler.notat.NotatPrivatAvtaleDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatPrivatAvtalePeriodeDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatResultatForskuddBeregningBarnDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatResultatSærbidragsberegningDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatSamværBarnDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatSamværDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatSivilstand
import no.nav.bidrag.transport.dokumentmaler.notat.NotatSærbidragKategoriDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatSærbidragUtgifterDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatTotalBeregningUtgifterDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatUnderholdBarnDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatUnderholdBarnDto.NotatFaktiskTilsynsutgiftDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatUnderholdBarnDto.NotatTilleggsstønadDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatUnderholdDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatUtgiftBeregningDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatUtgiftspostDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatVedtakDetaljerDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatVirkningstidspunktBarnDto
import no.nav.bidrag.transport.dokumentmaler.notat.NotatVirkningstidspunktDto
import no.nav.bidrag.transport.dokumentmaler.notat.OpplysningerBruktTilBeregning
import no.nav.bidrag.transport.dokumentmaler.notat.OpplysningerFraFolkeregisteret
import no.nav.bidrag.transport.dokumentmaler.notat.PeriodeSlåttUtTilFF
import no.nav.bidrag.transport.dokumentmaler.notat.VedtakNotatDto
import no.nav.bidrag.transport.dokumentmaler.notat.VedtakResultatInnhold
import no.nav.bidrag.transport.felles.ifTrue
import no.nav.bidrag.transport.felles.toYearMonth
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import no.nav.bidrag.behandling.dto.v2.behandling.DatoperiodeDto as BehandlingDatoperiodeDto
import no.nav.bidrag.behandling.dto.v2.underhold.DatoperiodeDto as UnderholdDatoperiodeDto

private val log = KotlinLogging.logger {}

@Service
class NotatOpplysningerService(
    private val behandlingService: BehandlingService,
    private val beregningService: BeregningService,
    private val bidragDokumentProduksjonConsumer: BidragDokumentProduksjonConsumer,
    private val bidragDokumentConsumer: BidragDokumentConsumer,
    private val mapper: Dtomapper,
) {
    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    @Transactional
    fun opprettNotat(
        behandlingId: Long,
        oppdaterDatoDokument: Boolean = false,
        saksnummer: String? = null,
    ): String {
        val behandling = behandlingService.hentBehandlingById(behandlingId)
        val notatDto = hentNotatOpplysninger(behandlingId, saksnummer)
        secureLogger.info { "Oppretter notat med opplysninger $notatDto og saksnummer $saksnummer" }
        val notatPdf = bidragDokumentProduksjonConsumer.opprettNotat(notatDto)
        log.info { "Oppretter notat for behandling $behandlingId i sak ${behandling.saksnummer}" }
        val vedtakDetaljer = behandling.vedtakDetaljer
        val forespørsel =
            OpprettJournalpostRequest(
                skalFerdigstilles = true,
                datoDokument = oppdaterDatoDokument.ifTrue { behandling.vedtakstidspunkt },
                journalposttype = JournalpostType.NOTAT,
                journalførendeEnhet = vedtakDetaljer?.vedtakFattetAvEnhet ?: behandling.vedtakFattetAvEnhet ?: behandling.behandlerEnhet,
                tilknyttSaker = saksnummer?.let { listOf(it) } ?: behandling.saker,
                gjelderIdent =
                if (behandling.erForskudd()) {
                    behandling.bidragsmottaker!!.ident
                } else {
                    behandling.bidragspliktig!!.ident
                },
                referanseId = behandling.tilReferanseId(saksnummer),
                saksbehandlerIdent = vedtakDetaljer?.vedtakFattetAv ?: behandling.vedtakFattetAv ?: TokenUtils.hentSaksbehandlerIdent(),
                dokumenter =
                listOf(
                    OpprettDokumentDto(
                        fysiskDokument = notatPdf,
                        tittel = behandling.notatTittel(),
                    ),
                ),
            )
        val response = opprettJournalpostEllerHentEksisterende(forespørsel)
        lagreJournalpostId(behandling, response.journalpostId)
        secureLogger.info {
            "Opprettet notat for behandling $behandlingId i sak ${behandling.saksnummer} " +
                "med journalpostId ${response.journalpostId} med forespørsel $forespørsel"
        }
        log.info {
            "Opprettet notat for behandling $behandlingId i sak ${behandling.saksnummer} " +
                "med journalpostId ${response.journalpostId}"
        }
        return response.journalpostId ?: ""
    }

    // Journalposten kan være opprettet i Joark selv om kallet feilet (f.eks. timeout). Sjekker derfor om den finnes via referanseId.
    private fun opprettJournalpostEllerHentEksisterende(forespørsel: OpprettJournalpostRequest): OpprettJournalpostResponse = try {
        bidragDokumentConsumer.opprettJournalpost(forespørsel)
    } catch (e: Exception) {
        val referanseId = forespørsel.referanseId ?: throw e
        val eksisterendeJournalpostId =
            bidragDokumentConsumer
                .hentJournalpostForEksternReferanseId(referanseId)
                ?.journalpost
                ?.journalpostId
                ?.substringAfter("-")
                ?: throw e
        log.warn(e) {
            "Opprettelse av notat feilet, men fant eksisterende journalpost $eksisterendeJournalpostId med referanseId $referanseId. Bruker denne"
        }
        OpprettJournalpostResponse(journalpostId = eksisterendeJournalpostId)
    }

    private fun lagreJournalpostId(
        behandling: Behandling,
        journalpostId: String?,
    ) {
        behandling.notatJournalpostId = journalpostId
    }

    fun hentNotatOpplysninger(
        behandlingId: Long,
        saksnummer: String? = null,
    ): VedtakNotatDto {
        val behandling = behandlingService.hentBehandlingById(behandlingId)

        return hentNotatOpplysningerForBehandling(behandling, saksnummer)
    }

    /**
     * Henter notatopplysninger for [behandling].
     *
     * Hvis [saksnummer] er satt filtreres opplysningene slik at kun BM/bidragspliktig og søknadsbarn som
     * tilhører det aktuelle saksnummeret (jf. [Rolle.saksnummer]) tas med. Dette er relevant for behandlinger med
     * forholdsmessig fordeling, der en behandling kan omfatte flere BM-er/saker med hvert sitt sett søknadsbarn.
     * Boforhold (husstandsmedlemmer) er alltid det samme uavhengig av saksnummer, men boforhold for BM
     * (`boforholdBMSøknadsbarn`) filtreres på søknadsbarna som tilhører [saksnummer].
     *
     * Hvis [saksnummer] er null (default) tas alle opplysninger med, uavhengig av saksnummer.
     */
    fun hentNotatOpplysningerForBehandling(
        behandling: Behandling,
        saksnummer: String? = null,
    ): VedtakNotatDto {
        val utvalg = NotatUtvalg(behandling, saksnummer)
        val sisteAktiveGrunnlag = behandling.grunnlag.hentSisteAktiv()

        return VedtakNotatDto(
            saksnummer = saksnummer ?: behandling.saksnummer,
            stønadstype = behandling.stonadstype,
            medInnkreving = behandling.innkrevingstype == Innkrevingstype.MED_INNKREVING,
            type = behandling.tilNotatMalType(),
            behandling = behandling.tilNotatBehandlingDetaljer(),
            saksbehandlerNavn = TokenUtils.hentSaksbehandlerIdent()?.let { EnhetProvider.hentSaksbehandlernavn(it) },
            virkningstidspunkt = behandling.byggVirkningstidspunkt(utvalg),
            utgift = mapper.run { behandling.tilUtgiftDto()?.tilNotatUtgiftDto(behandling) },
            samværV2 =
            NotatSamværDto(
                erSammeForAlle = behandling.sammeSamværForAlle,
                sammeSamværForAlleSaker = behandling.sammeSamværForAlleSaker.tilNotatErLikForAlle(),
                barn = behandling.byggSamvær(utvalg),
            ),
            underholdskostnader = behandling.byggUnderholdskostnader(utvalg),
            gebyrV3 = behandling.byggGebyrV3(utvalg),
            boforhold = behandling.byggBoforhold(utvalg, sisteAktiveGrunnlag),
            personer = utvalg.roller.sortedBy { it.fødselsdato }.map(Rolle::tilNotatRolle),
            inntekter = behandling.byggInntekter(utvalg, sisteAktiveGrunnlag),
            vedtak = behandling.hentBeregning(),
            erOrkestrertVedtak = behandling.erKlageEllerOmgjøring && behandling.erBidrag(),
            privatavtale = behandling.byggPrivatavtaler(utvalg),
        )
    }

    private fun Behandling.byggVirkningstidspunkt(utvalg: NotatUtvalg) = NotatVirkningstidspunktDto(
        erLikForAlle = sammeVirkningstidspunktForAlle,
        erLikForAlleBasertPåSak = sammeVirkningstidspunktForAlleSaker.tilNotatErLikForAlle(),
        erVirkningstidspunktLikForAlle = erVirkningstidspunktLiktForAlle,
        erVirkningstidspunktLiktForAlleSaker = erVirkningstidspunktLiktForAlleSaker.tilNotatErLikForAlle(),
        erAvslagForAlle = erAvslagForAlle,
        eldsteVirkningstidspunkt = eldsteVirkningstidspunkt.toYearMonth(),
        barn = tilVirkningstidspunktBarn(utvalg.søknadsbarn),
    )

    private fun Behandling.byggSamvær(utvalg: NotatUtvalg): List<NotatSamværBarnDto> = mapper
        .run { tilSamværDto() }
        ?.filter { utvalg.erKnyttetTilSaksnummer(it.barn.id) }
        ?.tilNotatSamværDto(this)
        ?: emptyList()

    private fun Behandling.byggUnderholdskostnader(utvalg: NotatUtvalg) = NotatUnderholdDto(
        offentligeOpplysninger = tilUnderholdOpplysning(),
        offentligeOpplysningerV2 = tilUnderholdOpplysningV2(),
        underholdskostnaderBarn =
        mapper
            .run { underholdskostnader.tilDtos() }
            .filter { utvalg.erKnyttetTilSaksnummer(it.gjelderBarn.id) }
            .sortedBy { it.gjelderBarn.fødselsdato }
            .map { it.tilNotatUnderholdBarn(this) },
    )

    private fun Behandling.byggGebyrV3(utvalg: NotatUtvalg) = NotatGebyrV3Dto(
        saker =
        mapper
            .run { mapGebyrV3() }
            .saker
            .filter { utvalg.gjelderSak(it.saksnummer) }
            .map { sak ->
                NotatGebyrSakDto(
                    saksnummer = sak.saksnummer,
                    gebyrRoller = sak.gebyrRoller.map { it.tilNotatGebyrDetaljer() },
                    gebyr18År = sak.gebyr18År.map { it.tilNotatGebyrDetaljer() },
                )
            },
    )

    private fun Behandling.byggBoforhold(
        utvalg: NotatUtvalg,
        sisteAktiveGrunnlag: List<Grunnlag>,
    ): NotatBoforholdDto {
        val opplysningerBoforholdBM = sisteAktiveGrunnlag.hentAlleBearbeidaBoforholdTilBMSøknadsbarn(eldsteVirkningstidspunkt)
        val opplysningerBoforhold =
            sisteAktiveGrunnlag.hentAlleBearbeidaBoforhold(
                eldsteVirkningstidspunkt,
                husstandsmedlem,
                Grunnlagsdatatype.BOFORHOLD.innhentesForRolle(this)!!,
            )
        val opplysningerSivilstand =
            sisteAktiveGrunnlag
                .find { it.rolle.id == bidragsmottaker!!.id && it.type == Grunnlagsdatatype.SIVILSTAND && !it.erBearbeidet }
                ?.konvertereData<List<SivilstandGrunnlagDto>>()
                ?.filtrerSivilstandGrunnlagEtterVirkningstidspunkt(eldsteVirkningstidspunkt)
                ?: emptyList()

        return NotatBoforholdDto(
            begrunnelse = tilNotatBoforhold(),
            sivilstand = tilSivilstand(opplysningerSivilstand),
            boforholdBMSøknadsbarn =
            opplysningerBoforholdBM.groupBy { it.gjelderPersonId }.mapNotNull { (gjelderBarnIdent, perioder) ->
                val gjelderBarn = utvalg.søknadsbarn.find { it.ident == gjelderBarnIdent } ?: return@mapNotNull null
                NotatBoforholdTilBMMedSøknadsbarn(
                    gjelderBarn = gjelderBarn.tilNotatRolle(),
                    perioder =
                    perioder.map {
                        OpplysningerFraFolkeregisteret(
                            periode = ÅrMånedsperiode(it.periodeFom, it.periodeTom),
                            status = it.bostatus,
                        )
                    },
                )
            },
            andreVoksneIHusstanden = mapper.tilAndreVoksneIHusstanden(this),
            beregnetBoforhold = mapper.run { tilBeregnetBoforhold() },
            barn =
            husstandsmedlem.barn
                .toSet()
                .sortert()
                .map { mapper.tilBoforholdBarn(it, opplysningerBoforhold) },
        )
    }

    private fun Behandling.byggInntekter(
        utvalg: NotatUtvalg,
        sisteAktiveGrunnlag: List<Grunnlag>,
    ): NotatInntekterDto {
        val alleArbeidsforhold =
            sisteAktiveGrunnlag
                .filter { it.type == Grunnlagsdatatype.ARBEIDSFORHOLD && !it.erBearbeidet }
                .flatMap { it.konvertereData<List<ArbeidsforholdGrunnlagDto>>() ?: emptyList() }
        val rollerMedInntekt = utvalg.roller.filter { it.rolletype != Rolletype.BARN || it.avslag == null }

        fun inntekterPerRolle(
            filtrerBareOffentlige: Boolean = false,
            bareMedIBeregning: Boolean = false,
        ) = rollerMedInntekt
            .sortedBy { it.fødselsdato }
            .map { rolle ->
                hentInntekterForIdent(
                    rolle.ident!!,
                    rolle,
                    alleArbeidsforhold.filter { rolle.ident == it.partPersonId },
                    filtrerBareOffentlige = filtrerBareOffentlige,
                    bareMedIBeregning = bareMedIBeregning,
                )
            }

        return NotatInntekterDto(
            notat = tilNotatInntekt(bidragsmottakerForSak(utvalg.saksnummer)),
            notatPerRolle = rollerMedInntekt.map { tilNotatInntekt(it) }.toSet(),
            inntekterPerRolle = inntekterPerRolle(bareMedIBeregning = true),
            offentligeInntekterPerRolle = inntekterPerRolle(filtrerBareOffentlige = true),
        )
    }

    private fun Behandling.byggPrivatavtaler(utvalg: NotatUtvalg): List<NotatPrivatAvtaleDto> {
        if (!erBidrag()) return emptyList()
        return mapper
            .run { tilPrivatAvtaleDtoV3() }
            .søknadsbarn
            .filter { utvalg.erKnyttetTilSaksnummer(it.gjelderBarn.id) }
            .sortedBy { it.gjelderBarn.fødselsdato }
            .mapNotNull { barn ->
                val privatAvtale = barn.privatAvtale ?: return@mapNotNull null
                NotatPrivatAvtaleDto(
                    gjelderBarn = barn.gjelderBarn.tilNotatRolle(this),
                    begrunnelse = NotatBegrunnelseDto(barn.begrunnelse, barn.begrunnelseFraOpprinneligVedtak),
                    avtaleDato = privatAvtale.avtaleDato,
                    avtaleType = privatAvtale.avtaleType,
                    skalIndeksreguleres = privatAvtale.skalIndeksreguleres,
                    utlandsbidrag = privatAvtale.gjelderUtland,
                    vedtakslisteUtenInnkreving =
                    privatAvtale.manuelleVedtakUtenInnkreving?.map {
                        DokumentmalManuellVedtak(
                            valgt = it.valgt,
                            fattetTidspunkt = it.fattetTidspunkt,
                            virkningsDato = it.virkningsDato,
                            vedtakstype = it.vedtakstype,
                            resultatSistePeriode = it.resultatSistePeriode,
                            privatAvtale = it.privatAvtale,
                            begrensetRevurdering = it.begrensetRevurdering,
                        )
                    } ?: emptyList(),
                    perioder =
                    privatAvtale.perioder.map {
                        NotatPrivatAvtalePeriodeDto(
                            periode = it.periode.tilNotatPeriode(),
                            beløp = it.beløp,
                            samværsklasse = it.samværsklasse,
                            valutakode = it.valutakode,
                        )
                    },
                    beregnetPrivatAvtalePerioder =
                    privatAvtale.beregnetPrivatAvtale?.perioder?.map {
                        NotatBeregnetPrivatAvtalePeriodeDto(
                            periode = DatoperiodeDto(it.periode.fom, it.periode.til),
                            beløp = it.beløp,
                            indeksfaktor = it.indeksprosent,
                        )
                    } ?: emptyList(),
                )
            }
    }

    private fun Behandling.tilUnderholdOpplysningV2(): NotatOffentligeOpplysningerUnderhold {
        val sisteAktiveGrunnlag = grunnlag.hentSisteAktiv()
        val opplysningerAndreBarnTilBM =
            sisteAktiveGrunnlag
                .find { it.rolle.id == bidragsmottaker!!.id && it.type == Grunnlagsdatatype.ANDRE_BARN }
                ?.konvertereData<List<RelatertPersonGrunnlagDto>>()
                ?.filter { it.erBarnTilBMUnder12År(virkningstidspunkt!!) }
                ?: emptyList()

        val opplysningerTilleggstønad =
            sisteAktiveGrunnlag
                .find { it.rolle.id == bidragsmottaker!!.id && it.type == Grunnlagsdatatype.TILLEGGSSTØNAD && !it.erBearbeidet }
                ?.konvertereData<List<TilleggsstønadGrunnlagDto>>()
                ?: emptyList()
        return NotatOffentligeOpplysningerUnderhold(
            tilUnderholdOpplysning(),
            opplysningerAndreBarnTilBM.map {
                val tilgangskontrollertPersoninfo =
                    mapper.tilgangskontrollerePersoninfo(
                        Personinfo(Personident(it.gjelderPersonId!!), null, it.fødselsdato),
                        Saksnummer(saksnummer),
                        true,
                    )
                DokumentmalPersonDto(
                    navn = tilgangskontrollertPersoninfo.navn,
                    fødselsdato = tilgangskontrollertPersoninfo.fødselsdato,
                    ident = tilgangskontrollertPersoninfo.ident,
                    erBeskyttet = tilgangskontrollertPersoninfo.erBeskyttet,
                )
            },
            opplysningerTilleggstønad.find { it.partPersonId == bidragsmottaker!!.ident }?.harInnvilgetVedtak ?: false,
        )
    }

    /** Beregningsfeil gir notat uten resultat, slik at notatet kan opprettes selv om behandlingen ikke kan beregnes. */
    private fun Behandling.hentBeregning(): NotatVedtakDetaljerDto {
        val vedtakDetaljer =
            try {
                when (tilType()) {
                    TypeBehandling.FORSKUDD -> tilNotatVedtakDetaljer(beregnForskuddForNotat())
                    TypeBehandling.SÆRBIDRAG -> tilNotatVedtakDetaljer(listOf(beregnSærbidragForNotat()))
                    TypeBehandling.BIDRAG -> beregnBidragForNotat()
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        return vedtakDetaljer ?: tilNotatVedtakDetaljer(emptyList())
    }

    private fun Behandling.beregnForskuddForNotat() = beregningService.beregneForskudd(this).tilDto().map { beregning ->
        NotatResultatForskuddBeregningBarnDto(
            barn = roller.find { it.ident == beregning.barn.ident!!.verdi }!!.tilNotatRolle(),
            perioder =
            beregning.perioder.map {
                NotatResultatForskuddBeregningBarnDto.NotatResultatPeriodeDto(
                    periode = it.periode,
                    beløp = it.beløp,
                    resultatKode = it.resultatKode,
                    regel = it.regel,
                    sivilstand = it.sivilstand,
                    inntekt = it.inntekt,
                    vedtakstype = vedtakstype,
                    antallBarnIHusstanden = it.antallBarnIHusstanden,
                )
            },
        )
    }

    private fun Behandling.beregnSærbidragForNotat() = beregningService.beregneSærbidrag(this).tilDto(this).let {
        NotatResultatSærbidragsberegningDto(
            periode = it.periode,
            resultat = it.resultat,
            resultatKode = it.resultatKode,
            bpsAndel = it.bpsAndel,
            forskuddssats = it.forskuddssats,
            maksGodkjentBeløp = it.maksGodkjentBeløp,
            beregning =
            it.beregning?.let { beregning ->
                NotatResultatSærbidragsberegningDto.UtgiftBeregningDto(
                    beløpDirekteBetaltAvBp = beregning.beløpDirekteBetaltAvBp,
                    totalBeløpBetaltAvBp = beregning.totalBeløpBetaltAvBp,
                    totalGodkjentBeløp = beregning.totalGodkjentBeløp,
                    totalKravbeløp = beregning.totalKravbeløp,
                    totalGodkjentBeløpBp = beregning.totalGodkjentBeløpBp,
                )
            },
            inntekter =
            it.inntekter?.let { inntekter ->
                DokumentmalResultatBeregningInntekterDto(
                    inntektBM = inntekter.inntektBM,
                    inntektBP = inntekter.inntektBP,
                    inntektBarn = inntekter.inntektBarn,
                    barnEndeligInntekt = inntekter.barnEndeligInntekt,
                )
            },
            delberegningUtgift = it.delberegningUtgift,
            delberegningBidragspliktigesBeregnedeTotalbidrag = it.delberegningBidragspliktigesBeregnedeTotalBidrag?.tilNotatDto(),
            delberegningBidragsevne = it.delberegningBidragsevne?.tilNotatDto(),
            antallBarnIHusstanden = it.antallBarnIHusstanden,
            voksenIHusstanden = it.voksenIHusstanden,
            enesteVoksenIHusstandenErEgetBarn = it.enesteVoksenIHusstandenErEgetBarn,
            erDirekteAvslag = it.erDirekteAvslag,
        )
    }

    private fun Behandling.beregnBidragForNotat(): NotatVedtakDetaljerDto {
        val resultatBeregning = beregningService.beregneBidrag(this).tilDto(kanFatteVedtakBegrunnelse())
        val resultat =
            resultatBeregning.resultatBarn.sortedBy { it.barn.fødselsdatoSortering }.map { beregning ->
                DokumentmalResultatBidragsberegningBarnDto(
                    barn = roller.find { it.erSammeRolle(beregning.barn.ident!!.verdi, beregning.barn.stønadstype) }!!.tilNotatRolle(),
                    indeksår = beregning.indeksår ?: Year.now().value,
                    innkrevesFraDato = beregning.innkrevesFraDato,
                    minstEnPeriodeHarSlåttUtTilFF = resultatBeregning.minstEnPeriodeHarSlåttUtTilFF,
                    perioderSlåttUtTilFF =
                    resultatBeregning.perioderSlåttUtTilFF.map {
                        PeriodeSlåttUtTilFF(
                            periode = it.periode,
                            erEvneJustertNedTil25ProsentAvInntekt = it.erEvneJustertNedTil25ProsentAvInntekt,
                        )
                    },
                    erAvvisning = beregning.erAvvisning,
                    erAvvistRevurdering = beregning.erAvvistRevurdering,
                    orkestrertVedtak =
                    beregning.delvedtak.find { it.endeligVedtak }?.let { endeligVedtak ->
                        EndeligOrkestrertVedtak(
                            type = endeligVedtak.type,
                            perioder = endeligVedtak.perioder.map { it.mapTilNotat(this) },
                        )
                    },
                    perioder = beregning.perioder.map { it.mapTilNotat(this) },
                )
            }
        return tilNotatVedtakDetaljer(resultat).copy(
            kanFatteVedtakForRevurderingsbarn = resultatBeregning.kanFatteVedtakForRevurderingsbarn,
            skalFatteVedtakForRevurderingsbarn = resultatBeregning.skalFatteVedtakForRevurderingsbarn,
            manueltOverstyrtFatteVedtakRevurderingsbarnBegrunnelse =
            hentEllerInitaliserMetadata()
                .hentFatteVedtakRevurderingsbarnInformasjon()
                ?.manueltOverstyrtForslagBegrunnelse,
        )
    }
}

/**
 * Utvalget av roller som skal med i notatet. Er [saksnummer] satt, tas bare bidragspliktig og rollene i den saken med.
 */
private class NotatUtvalg(
    behandling: Behandling,
    val saksnummer: String?,
) {
    val roller: List<Rolle> =
        if (saksnummer == null) {
            behandling.roller.toList()
        } else {
            behandling.roller.filter { it.rolletype == Rolletype.BIDRAGSPLIKTIG || it.saksnummer == saksnummer }
        }
    val søknadsbarn = roller.filter { it.rolletype == Rolletype.BARN }
    private val søknadsbarnIder = søknadsbarn.mapNotNull { it.id }.toSet()

    // Opplysninger uten id (f.eks. andre barn enn søknadsbarn) kan ikke knyttes til en sak og tas derfor alltid med.
    fun erKnyttetTilSaksnummer(id: Long?) = saksnummer == null || id == null || id in søknadsbarnIder

    fun gjelderSak(saksnummerForOpplysning: String?) = saksnummer == null || saksnummerForOpplysning == saksnummer
}

private fun Behandling.tilNotatMalType() = when (tilType()) {
    TypeBehandling.FORSKUDD -> NotatMalType.FORSKUDD
    TypeBehandling.SÆRBIDRAG -> NotatMalType.SÆRBIDRAG
    TypeBehandling.BIDRAG, TypeBehandling.BIDRAG_18_ÅR -> NotatMalType.BIDRAG
}

private fun Behandling.tilNotatVedtakDetaljer(resultat: List<VedtakResultatInnhold>) = NotatVedtakDetaljerDto(
    erFattet = vedtakDetaljer != null || erVedtakFattet,
    fattetTidspunkt = vedtakDetaljer?.vedtakstidspunkt ?: vedtakstidspunkt,
    fattetAvSaksbehandler = (vedtakDetaljer?.vedtakFattetAv ?: vedtakFattetAv)?.let { EnhetProvider.hentSaksbehandlernavn(it) },
    resultat = resultat,
)

private fun List<ErLikForAlleBasertPåSak>.tilNotatErLikForAlle() = map { NotatErLikForAlleBasertPåSak(it.saksnummer, it.erLikForAlle) }

private fun BehandlingDatoperiodeDto.tilNotatPeriode() = DatoperiodeDto(fom, tom)

private fun UnderholdDatoperiodeDto.tilNotatPeriode() = DatoperiodeDto(fom, tom)

private fun GebyrRolleV2Dto.tilNotatGebyrDetaljer() = gebyrDetaljer.tilNotatGebyrDetaljer(rolle)

private fun GebyrDetaljerDto.tilNotatGebyrDetaljer(rolle: RolleDto) = NotatGebyrDetaljerDto(
    søknad = søknad?.tilNotatGebyrSøknadDetaljer(),
    inntekt =
    NotatGebyrInntektDto(
        skattepliktigInntekt = inntekt.skattepliktigInntekt,
        maksBarnetillegg = inntekt.maksBarnetillegg,
    ),
    beregnetIlagtGebyr = beregnetIlagtGebyr,
    beløpGebyrsats = beløpGebyrsats,
    endeligIlagtGebyr = endeligIlagtGebyr,
    begrunnelse = begrunnelse,
    rolle = rolle.tilNotatRolle(),
)

private fun SøknadDetaljerDto.tilNotatGebyrSøknadDetaljer() = NotatGebyrDetaljerDto.NotatGebyrSøknadDetaljerDto(
    søknadsid = søknadsid,
    mottattDato = mottattDato,
    saksnummer = saksnummer,
    søktAvType = søktAvType,
    søknadFomDato = søktFomDato,
    behandlingstype = behandlingstype,
    behandlingstema = behandlingstema,
)

private fun UnderholdDto.tilNotatUnderholdBarn(behandling: Behandling) = NotatUnderholdBarnDto(
    gjelderBarn = gjelderBarn.tilNotatRolle(behandling),
    harTilsynsordning = harTilsynsordning,
    begrunnelse = NotatBegrunnelseDto(begrunnelse, begrunnelseFraOpprinneligVedtak),
    stønadTilBarnetilsyn =
    stønadTilBarnetilsyn.map {
        NotatUnderholdBarnDto.NotatStønadTilBarnetilsynDto(
            periode = it.periode.tilNotatPeriode(),
            skolealder = it.skolealder ?: Skolealder.IKKE_ANGITT,
            tilsynstype = it.tilsynstype ?: Tilsynstype.IKKE_ANGITT,
            kilde = it.kilde,
        )
    },
    tilleggsstønad =
    tilleggsstønad.map {
        NotatTilleggsstønadDto(
            periode = it.periode.tilNotatPeriode(),
            dagsats = it.dagsats,
            beløp = it.beløp,
            beløpstype = it.beløpstype,
            total = it.total,
        )
    },
    forpleining =
    forpleining.map {
        NotatUnderholdBarnDto.NotatForpleiningDto(
            periode = it.periode.tilNotatPeriode(),
            beløp = it.beløp,
        )
    },
    underholdskostnad =
    underholdskostnad.map {
        NotatUnderholdBarnDto.NotatUnderholdskostnadBeregningDto(
            periode = it.periode.tilNotatPeriode(),
            forbruk = it.forbruk,
            boutgifter = it.boutgifter,
            stønadTilBarnetilsyn = it.stønadTilBarnetilsyn,
            tilsynsutgifter = it.tilsynsutgifter,
            barnetrygd = it.barnetrygd,
            forpleining = it.forpleining,
            total = it.total,
            beregningsdetaljer = it.beregningsdetaljer?.tilNotatBeregningsdetaljer(behandling),
        )
    },
    faktiskTilsynsutgift =
    faktiskTilsynsutgift.map {
        NotatFaktiskTilsynsutgiftDto(
            periode = it.periode.tilNotatPeriode(),
            utgift = it.utgift,
            total = it.total,
            kostpenger = it.kostpenger,
            kommentar = it.kommentar,
        )
    },
)

private fun UnderholdskostnadDto.UnderholdskostnadPeriodeBeregningsdetaljer.tilNotatBeregningsdetaljer(behandling: Behandling) = NotatUnderholdBarnDto.NotatUnderholdskostnadPeriodeBeregningsdetaljer(
    tilsynsutgifterBarn =
    tilsynsutgifterBarn.map {
        NotatUnderholdBarnDto.NotatTilsynsutgiftBarn(
            gjelderBarn = it.gjelderBarn.tilNotatRolle(behandling),
            totalTilsynsutgift = it.totalTilsynsutgift,
            beløp = it.beløp,
            kostpenger = it.kostpenger,
            tilleggsstønad = it.tilleggsstønad,
            beløpstype = it.beløpstype,
            tilleggsstønadBeløp = it.tilleggsstønadBeløp,
            tilleggsstønadDagsats = it.tilleggsstønadDagsats,
            faktiskUtgiftBeregnet = it.faktiskUtgiftBeregnet,
        )
    },
    justertBruttoTilsynsutgift = justertBruttoTilsynsutgift,
    erBegrensetAvMaksTilsyn = erBegrensetAvMaksTilsyn,
    bruttoTilsynsutgift = bruttoTilsynsutgift,
    fordelingFaktor = fordelingFaktor,
    maksfradragAndel = BigDecimal.ZERO,
    skattefradrag = skattefradrag,
    skattefradragMaksFradrag = skattefradragMaksFradrag,
    skattefradragPerBarn = skattefradragPerBarn,
    skattefradragTotalTilsynsutgift = skattefradragTotalTilsynsutgift,
    skattesatsFaktor = skattesatsFaktor,
    sumTilsynsutgifter = sumTilsynsutgifter,
    totalTilsynsutgift = totalTilsynsutgift,
    antallBarnBMUnderTolvÅr = antallBarnBMUnderTolvÅr,
    antallBarnMedTilsynsutgifter = antallBarnMedTilsynsutgifter,
    antallBarnBMBeregnet = antallBarnBMBeregnet,
    nettoTilsynsutgift = nettoTilsynsutgift,
    sjablonMaksFradrag = sjablonMaksFradrag,
    sjablonMaksTilsynsutgift = sjablonMaksTilsynsutgift,
)

private fun no.nav.bidrag.behandling.dto.v1.beregning.ResultatBarnebidragsberegningPeriodeDto.mapTilNotat(behandling: Behandling) = ResultatBarnebidragsberegningPeriodeDto(
    periode = periode,
    erSistePeriode = erSistePeriode,
    underholdskostnad = underholdskostnad,
    bpsAndelU = bpsAndelU,
    bpsAndelBeløp = bpsAndelBeløp,
    samværsfradrag = samværsfradrag,
    beregnetBidrag = beregnetBidrag,
    faktiskBidrag = faktiskBidrag,
    resultatKode = resultatKode,
    erDirekteAvslag = erDirekteAvslag,
    erOpphør = erOpphør,
    delvedtakstypeVisningsnavn = delvedtakstypeVisningsnavn,
    resultatkodeVisningsnavn = resultatkodeVisningsnavn ?: "",
    klageOmgjøringDetaljer =
    klageOmgjøringDetaljer?.let {
        ResultatBarnebidragsberegningPeriodeDto.KlageOmgjøringDetaljer(
            kanOpprette35c = it.kanOpprette35c,
            skalOpprette35c = it.skalOpprette35c,
        )
    },
    vedtakstype = vedtakstype,
    beregningsdetaljer =
    beregningsdetaljer?.let {
        BidragPeriodeBeregningsdetaljer(
            bpHarEvne = it.bpHarEvne,
            antallBarnIHusstanden = it.antallBarnIHusstanden,
            forskuddssats = it.forskuddssats,
            barnetilleggBM = it.barnetilleggBM.tilNotatDto(),
            indeksreguleringDetaljer =
            it.indeksreguleringDetaljer?.let {
                IndeksreguleringDetaljer(
                    it.sluttberegning,
                    it.faktor,
                )
            },
            barnetilleggBP = it.barnetilleggBP.tilNotatDto(),
            voksenIHusstanden = it.voksenIHusstanden,
            endringUnderGrense = it.endringUnderGrense,
            sluttberegningAldersjustering = it.sluttberegningAldersjustering,
            enesteVoksenIHusstandenErEgetBarn = it.enesteVoksenIHusstandenErEgetBarn,
            bpsAndel = it.bpsAndel,
            inntekter =
            it.inntekter?.let {
                DokumentmalResultatBeregningInntekterDto(
                    inntektBM = it.inntektBM,
                    inntektBP = it.inntektBP,
                    inntektBarn = it.inntektBarn,
                    barnEndeligInntekt = it.barnEndeligInntekt,
                )
            },
            delberegningBidragsevne =
            it.delberegningBidragsevne?.tilNotatDto(),
            samværsfradrag =
            it.samværsfradrag?.let {
                NotatBeregningsdetaljerSamværsfradrag(
                    samværsfradrag = it.samværsfradrag,
                    samværsklasse = it.samværsklasse,
                    gjennomsnittligSamværPerMåned = it.gjennomsnittligSamværPerMåned,
                )
            },
            forholdsmessigFordelingBeregningsdetaljer =
            it.forholdsmessigFordeling?.let {
                DokumentmalForholdsmessigFordelingBeregningsdetaljer(
                    beregningFordelingAvBidragSjekkEvnesprekk =
                    it.beregningFordelingAvBidragSjekkEvnesprekk?.let {
                        DokumentmalForholdsmessigFordelingBidragTilFordeling(
                            sumBidragTilFordeling = it.sumBidragTilFordeling,
                            sumBidragTilFordelingSøknadsbarn = it.sumBidragTilFordelingSøknadsbarn,
                            sumBidragTilFordelingIkkeSøknadsbarn = it.sumBidragTilFordelingIkkeSøknadsbarn,
                            sumBidragTilFordelingPrivatAvtale = it.sumBidragTilFordelingPrivatAvtale,
                            sumBidragSomIkkeKanFordeles = it.sumBidragSomIkkeKanFordeles,
                            bidragTilFordelingAlle =
                            it.bidragTilFordelingAlle.map {
                                DokumentmalForholdsmessigFordelingBidragTilFordelingBarn(
                                    utenlandskbidrag = it.utenlandskbidrag,
                                    oppfostringsbidrag = it.oppfostringsbidrag,
                                    privatAvtale = it.privatAvtale,
                                    erSøknadsbarn = it.erSøknadsbarn,
                                    bidragTilFordeling = it.bidragTilFordeling,
                                    beregnetBidrag =
                                    it.beregnetBidrag?.let {
                                        DokumentmalForholdsmessigFordelingBidragTilFordelingBarn
                                            .BeregnetBidragBarnDto(
                                                saksnummer = it.saksnummer,
                                                samværsfradrag = it.samværsfradrag,
                                                samværsklasse = it.samværsklasse,
                                                valutakode = it.valutakode,
                                                valutakurs = it.valutakurs,
                                                reduksjonUnderholdskostnad = it.reduksjonUnderholdskostnad,
                                                indeksreguleringFaktor = it.indeksreguleringFaktor,
                                                beregnetBidrag = it.beregnetBidrag,
                                                beregnetBeløp = it.beregnetBeløp,
                                                faktiskBeløp = it.faktiskBeløp,
                                                løpendeBeløp = it.løpendeBeløp,
                                            )
                                    },
                                    barn = it.barn.tilNotatRolle(behandling),
                                )
                            },
                            finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn =
                            it
                                .finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn,
                            sumPrioriterteBidragTilFordeling = it.sumPrioriterteBidragTilFordeling,
                        )
                    },
                    beregningFordelingAvBidrag =
                    it.beregningFordelingAvBidrag.let {
                        DokumentmalForholdsmessigFordelingBidragTilFordeling(
                            sumBidragTilFordeling = it.sumBidragTilFordeling,
                            sumBidragTilFordelingSøknadsbarn = it.sumBidragTilFordelingSøknadsbarn,
                            sumBidragTilFordelingIkkeSøknadsbarn = it.sumBidragTilFordelingIkkeSøknadsbarn,
                            sumBidragTilFordelingPrivatAvtale = it.sumBidragTilFordelingPrivatAvtale,
                            sumBidragSomIkkeKanFordeles = it.sumBidragSomIkkeKanFordeles,
                            finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn =
                            it
                                .finnesBarnMedLøpendeBidragSomIkkeErSøknadsbarn,
                            sumPrioriterteBidragTilFordeling = it.sumPrioriterteBidragTilFordeling,
                            bidragTilFordelingAlle =
                            it.bidragTilFordelingAlle.map {
                                DokumentmalForholdsmessigFordelingBidragTilFordelingBarn(
                                    utenlandskbidrag = it.utenlandskbidrag,
                                    oppfostringsbidrag = it.oppfostringsbidrag,
                                    privatAvtale = it.privatAvtale,
                                    erSøknadsbarn = it.erSøknadsbarn,
                                    bidragTilFordeling = it.bidragTilFordeling,
                                    beregnetBidrag =
                                    it.beregnetBidrag?.let {
                                        DokumentmalForholdsmessigFordelingBidragTilFordelingBarn
                                            .BeregnetBidragBarnDto(
                                                saksnummer = it.saksnummer,
                                                samværsfradrag = it.samværsfradrag,
                                                samværsklasse = it.samværsklasse,
                                                valutakode = it.valutakode,
                                                valutakurs = it.valutakurs,
                                                reduksjonUnderholdskostnad = it.reduksjonUnderholdskostnad,
                                                indeksreguleringFaktor = it.indeksreguleringFaktor,
                                                beregnetBidrag = it.beregnetBidrag,
                                                beregnetBeløp = it.beregnetBeløp,
                                                faktiskBeløp = it.faktiskBeløp,
                                                løpendeBeløp = it.løpendeBeløp,
                                            )
                                    },
                                    barn = it.barn.tilNotatRolle(behandling),
                                )
                            },
                        )
                    },
                    bidragTilFordelingForBarnet = it.bidragTilFordelingForBarnet,
                    andelAvSumBidragTilFordelingFaktor = it.andelAvSumBidragTilFordelingFaktor,
                    andelAvEvneBeløp = it.andelAvEvneBeløp,
                    bidragEtterFordeling = it.bidragEtterFordeling,
                    harBPFullEvne = it.harBPFullEvne,
                    erKompletteGrunnlagForAlleLøpendeBidrag = it.erKompletteGrunnlagForAlleLøpendeBidrag,
                    erForholdsmessigFordelt = it.erForholdsmessigFordelt,
                    sumBidragTilFordelingJustertForPrioriterteBidrag = it.sumBidragTilFordelingJustertForPrioriterteBidrag,
                    evneJustertForPrioriterteBidrag = it.evneJustertForPrioriterteBidrag,
                    bidragTilFordelingAlle =
                    it.beregningFordelingAvBidrag.bidragTilFordelingAlle.map {
                        DokumentmalForholdsmessigFordelingBidragTilFordelingBarn(
                            utenlandskbidrag = it.utenlandskbidrag,
                            oppfostringsbidrag = it.oppfostringsbidrag,
                            privatAvtale = it.privatAvtale,
                            erSøknadsbarn = it.erSøknadsbarn,
                            bidragTilFordeling = it.bidragTilFordeling,
                            beregnetBidrag =
                            it.beregnetBidrag?.let {
                                DokumentmalForholdsmessigFordelingBidragTilFordelingBarn.BeregnetBidragBarnDto(
                                    saksnummer = it.saksnummer,
                                    samværsfradrag = it.samværsfradrag,
                                    samværsklasse = it.samværsklasse,
                                    valutakode = it.valutakode,
                                    valutakurs = it.valutakurs,
                                    reduksjonUnderholdskostnad = it.reduksjonUnderholdskostnad,
                                    indeksreguleringFaktor = it.indeksreguleringFaktor,
                                    beregnetBidrag = it.beregnetBidrag,
                                    beregnetBeløp = it.beregnetBeløp,
                                    faktiskBeløp = it.faktiskBeløp,
                                    løpendeBeløp = it.løpendeBeløp,
                                )
                            },
                            barn = it.barn.tilNotatRolle(behandling),
                        )
                    },
                )
            },
            sluttberegning =
            it.sluttberegning?.let {
                DokumentmalSluttberegningBarnebidragDetaljer(
                    beregnetBeløp = it.beregnetBeløp,
                    resultatBeløp = it.resultatBeløp,
                    uMinusNettoBarnetilleggBM = it.uMinusNettoBarnetilleggBM,
                    bruttoBidragEtterBarnetilleggBM = it.bruttoBidragEtterBarnetilleggBM,
                    nettoBidragEtterBarnetilleggBM = it.nettoBidragEtterBarnetilleggBM,
                    bruttoBidragJustertForEvneOg25Prosent = it.bruttoBidragJustertForEvneOg25Prosent,
                    bruttoBidragEtterBegrensetRevurdering = it.bruttoBidragEtterBegrensetRevurdering,
                    bruttoBidragEtterBarnetilleggBP = it.bruttoBidragEtterBarnetilleggBP,
                    nettoBidragEtterSamværsfradrag = it.nettoBidragEtterSamværsfradrag,
                    bpAndelAvUVedDeltBostedFaktor = it.bpAndelAvUVedDeltBostedFaktor,
                    bpAndelAvUVedDeltBostedBeløp = it.bpAndelAvUVedDeltBostedBeløp,
                    løpendeForskudd = it.løpendeForskudd,
                    løpendeBidrag = it.løpendeBidrag,
                    barnetErSelvforsørget = it.barnetErSelvforsørget,
                    bidragJustertForDeltBosted = it.bidragJustertForDeltBosted,
                    bidragJustertForNettoBarnetilleggBP = it.bidragJustertForNettoBarnetilleggBP,
                    bidragJustertForNettoBarnetilleggBM = it.bidragJustertForNettoBarnetilleggBM,
                    bidragJustertNedTilEvne = it.bidragJustertNedTilEvne,
                    bidragJustertNedTil25ProsentAvInntekt = it.bidragJustertNedTil25ProsentAvInntekt,
                    bidragJustertTilForskuddssats = it.bidragJustertTilForskuddssats,
                    bidragJustertManueltTilForskuddssats = it.bidragJustertManueltTilForskuddssats,
                    begrensetRevurderingUtført = it.begrensetRevurderingUtført,
                    ikkeOmsorgForBarnet = it.ikkeOmsorgForBarnet,
                    bpEvneVedForholdsmessigFordeling = it.bpEvneVedForholdsmessigFordeling,
                    bpAndelAvUVedForholdsmessigFordelingFaktor = it.bpAndelAvUVedForholdsmessigFordelingFaktor,
                    bpSumAndelAvU = it.bpSumAndelAvU,
                    resultat = it.resultat,
                    resultatVisningsnavn = it.resultatVisningsnavn,
                )
            },
            delberegningUnderholdskostnad = it.delberegningUnderholdskostnad,
            delberegningBidragspliktigesBeregnedeTotalBidrag =
            it.delberegningBidragspliktigesBeregnedeTotalBidrag
                ?.tilNotatDto(),
        )
    },
)

private fun DelberegningBarnetilleggDto.tilNotatDto() = DokumentmalDelberegningBarnetilleggDto(
    barnetillegg =
    barnetillegg.map {
        DokumentmalDelberegningBarnetilleggDto.DokumentmalBarnetilleggDetaljerDto(
            bruttoBeløp = it.bruttoBeløp,
            nettoBeløp = it.nettoBeløp,
            skattefaktor = it.skattefaktor,
            visningsnavn = it.visningsnavn,
        )
    },
    sumNettoBeløp = sumNettoBeløp,
    sumBruttoBeløp = sumBruttoBeløp,
    skattFaktor = skattFaktor,
    delberegningSkattesats = delberegningSkattesats,
)

private fun Behandling.tilNotatBoforhold(): NotatBegrunnelseDto = NotatBegrunnelseDto(
    innhold = henteNotatinnhold(this, NotatType.BOFORHOLD),
    innholdFraOmgjortVedtak = henteNotatinnhold(this, NotatType.BOFORHOLD, null, false),
    gjelder = Grunnlagsdatatype.BOFORHOLD.innhentesForRolle(this)!!.tilNotatRolle(),
)

private fun Behandling.tilNotatVurderingAvSkolegang(rolle: Rolle? = null) = NotatBegrunnelseDto(
    innhold =
    henteNotatinnhold(this, NotatType.VIRKNINGSTIDSPUNKT_VURDERING_AV_SKOLEGANG, rolle ?: søknadsbarn.first()).ifEmpty {
        if (!erIForholdsmessigFordeling) {
            henteNotatinnhold(this, NotatType.VIRKNINGSTIDSPUNKT_VURDERING_AV_SKOLEGANG)
        } else {
            null
        }
    },
    innholdFraOmgjortVedtak =
    henteNotatinnhold(this, NotatType.VIRKNINGSTIDSPUNKT_VURDERING_AV_SKOLEGANG, rolle ?: søknadsbarn.first(), false).ifEmpty {
        if (!erIForholdsmessigFordeling) {
            henteNotatinnhold(this, NotatType.VIRKNINGSTIDSPUNKT_VURDERING_AV_SKOLEGANG, null, false)
        } else {
            null
        }
    },
    gjelder = this.bidragsmottaker!!.tilNotatRolle(),
)

private fun Behandling.tilNotatVirkningstidspunkt(rolle: Rolle? = null) = NotatBegrunnelseDto(
    innhold =
    henteNotatinnhold(this, NotatType.VIRKNINGSTIDSPUNKT, rolle ?: søknadsbarn.first()).ifEmpty {
        if (!erIForholdsmessigFordeling) {
            henteNotatinnhold(this, NotatType.VIRKNINGSTIDSPUNKT)
        } else {
            null
        }
    },
    innholdFraOmgjortVedtak =
    henteNotatinnhold(this, NotatType.VIRKNINGSTIDSPUNKT, rolle ?: søknadsbarn.first(), false).ifEmpty {
        if (!erIForholdsmessigFordeling) {
            henteNotatinnhold(this, NotatType.VIRKNINGSTIDSPUNKT, null, false)
        } else {
            null
        }
    },
    gjelder = this.bidragsmottaker!!.tilNotatRolle(),
)

private fun Behandling.tilNotatInntekt(rolle: Rolle): NotatBegrunnelseDto = NotatBegrunnelseDto(
    innhold = henteInntektsnotat(this, rolle.id!!),
    innholdFraOmgjortVedtak = henteInntektsnotat(this, rolle.id!!, false),
    gjelder = rolle.tilNotatRolle(),
)

private fun Behandling.tilSivilstand(sivilstandOpplysninger: List<SivilstandGrunnlagDto>) = NotatSivilstand(
    opplysningerBruktTilBeregning =
    sivilstand
        .sortedBy { it.datoFom }
        .map(Sivilstand::tilSivilstandsperiode),
    opplysningerFraFolkeregisteret =
    sivilstandOpplysninger
        .map { periode ->
            OpplysningerFraFolkeregisteret(
                periode =
                ÅrMånedsperiode(
                    periode.gyldigFom ?: LocalDate.MIN,
                    null,
                ),
                status = periode.type,
            )
        }.sortedBy { it.periode.fom },
)

private fun Sivilstand.tilSivilstandsperiode() = OpplysningerBruktTilBeregning(
    periode =
    ÅrMånedsperiode(
        datoFom,
        datoTom,
    ),
    status = sivilstand,
    kilde = kilde,
)

private fun SærbidragUtgifterDto.tilNotatUtgiftDto(behandling: Behandling) = NotatSærbidragUtgifterDto(
    beregning = beregning?.tilNotatBeregningDto(),
    begrunnelse =
    NotatBegrunnelseDto(
        innhold = begrunnelse.innhold,
        innholdFraOmgjortVedtak = begrunnelseFraOpprinneligVedtak?.innhold,
        gjelder = behandling.bidragsmottaker!!.tilNotatRolle(),
    ),
    utgifter = utgifter.map { it.tilNotatDto() },
    maksGodkjentBeløp =
    maksGodkjentBeløp?.let {
        NotatMaksGodkjentBeløpDto(
            taMed = it.taMed,
            beløp = it.beløp,
            begrunnelse = it.begrunnelse,
        )
    },
    totalBeregning = totalBeregning.map { it.tilNotatDto() },
)

private fun DelberegningBidragsevneDto.tilNotatDto() = DokumentmalDelberegningBidragsevneDto(
    sumInntekt25Prosent = sumInntekt25Prosent,
    bidragsevne = bidragsevne,
    utgifter =
    DokumentmalDelberegningBidragsevneDto.DokumentmalBidragsevneUtgifterBolig(
        boutgiftBeløp = utgifter.boutgiftBeløp,
        borMedAndreVoksne = utgifter.borMedAndreVoksne,
        underholdBeløp = utgifter.underholdBeløp,
    ),
    skatt =
    DokumentmalDelberegningBidragsevneDto.DokumentmalSkattBeregning(
        skattAlminneligInntekt = skatt.skattAlminneligInntekt,
        sumSkatt = skatt.sumSkatt,
        trinnskatt = skatt.trinnskatt,
        trygdeavgift = skatt.trygdeavgift,
    ),
    underholdEgneBarnIHusstand =
    DokumentmalDelberegningBidragsevneDto.DokumentmalUnderholdEgneBarnIHusstand(
        antallBarnIHusstanden = underholdEgneBarnIHusstand.antallBarnIHusstanden,
        årsbeløp = underholdEgneBarnIHusstand.årsbeløp,
        sjablon = underholdEgneBarnIHusstand.sjablon,
    ),
)

private fun Behandling.tilUnderholdOpplysning(): List<NotatOffentligeOpplysningerUnderholdBarn> {
    val opplysningerBarnetilsyn =
        grunnlag
            .hentSisteAktiv()
            .find { it.rolle.id == bidragsmottaker!!.id && it.type == Grunnlagsdatatype.BARNETILSYN && !it.erBearbeidet }
            ?.konvertereData<List<BarnetilsynGrunnlagDto>>()
            ?: emptyList()

    val opplysningerTilleggstønad =
        grunnlag
            .hentSisteAktiv()
            .find { it.rolle.id == bidragsmottaker!!.id && it.type == Grunnlagsdatatype.TILLEGGSSTØNAD && !it.erBearbeidet }
            ?.konvertereData<List<TilleggsstønadGrunnlagDto>>()
            ?: emptyList()
    return søknadsbarn.filter { it.avslag == null }.map { rolle ->
        NotatOffentligeOpplysningerUnderholdBarn(
            gjelder = rolle.behandling.bidragsmottaker!!.tilNotatRolle(),
            gjelderBarn = rolle.tilNotatRolle(),
            barnetilsyn =
            opplysningerBarnetilsyn.filter { it.barnPersonId == rolle.ident }.map {
                NotatBarnetilsynOffentligeOpplysninger(
                    periode = ÅrMånedsperiode(it.periodeFra, it.periodeTil),
                )
            },
            harTilleggsstønad =
            opplysningerTilleggstønad.find { it.partPersonId == rolle.ident }?.harInnvilgetVedtak ?: false,
        )
    }
}

private fun TotalBeregningUtgifterDto.tilNotatDto() = NotatTotalBeregningUtgifterDto(
    betaltAvBp,
    utgiftstype,
    totalKravbeløp,
    totalGodkjentBeløp,
)

private fun UtgiftspostDto.tilNotatDto() = NotatUtgiftspostDto(
    begrunnelse = begrunnelse,
    dato = dato,
    type = type,
    kravbeløp = kravbeløp,
    godkjentBeløp = godkjentBeløp,
    betaltAvBp = betaltAvBp,
)

private fun UtgiftBeregningDto.tilNotatBeregningDto() = NotatUtgiftBeregningDto(
    beløpDirekteBetaltAvBp = beløpDirekteBetaltAvBp,
    totalBeløpBetaltAvBp = totalBeløpBetaltAvBp,
    totalGodkjentBeløp = totalGodkjentBeløp,
    totalKravbeløp = totalKravbeløp,
    totalGodkjentBeløpBp = totalGodkjentBeløpBp,
)

private fun SærbidragKategoriDto.tilNotatSærbidragKategoriDto() = NotatSærbidragKategoriDto(
    kategori = kategori,
    beskrivelse = beskrivelse,
)

private fun Behandling.tilNotatBehandlingDetaljer() = NotatBehandlingDetaljerDto(
    søknadstype = vedtakstype.name,
    vedtakstype = vedtakstype,
    innkreving = innkrevingstype == Innkrevingstype.MED_INNKREVING,
    opprinneligVedtakstype = omgjøringsdetaljer?.opprinneligVedtakstype,
    søktAv = soknadFra,
    mottattDato = mottattdato,
    klageMottattDato = omgjøringsdetaljer?.klageMottattdato,
    søktFraDato = YearMonth.from(søktFomDato),
    virkningstidspunkt = virkningstidspunkt,
    avslag = avslag,
    kategori = tilSærbidragKategoriDto().tilNotatSærbidragKategoriDto(),
)

private fun Behandling.tilVirkningstidspunktBarn(søknadsbarnForNotat: List<Rolle> = søknadsbarn) = søknadsbarnForNotat.sortedBy { it.fødselsdato }.map {
    val innkrevingstype = it.innkrevingstype ?: innkrevingstype
    val eldsteSøknad = it.forholdsmessigFordeling?.eldsteSøknad
    NotatVirkningstidspunktBarnDto(
        rolle = it.tilNotatRolle(),
        stønadstype = it.stønadstypeBarnEllerBehandling,
        behandlingstype = eldsteSøknad?.behandlingstype ?: søknadstype,
        søknadstype = eldsteSøknad?.behandlingstype?.name ?: søknadstype?.name,
        vedtakstype = vedtakstype,
        søktAv = eldsteSøknad?.søktAvType ?: soknadFra,
        avslag = it.avslag ?: avslag,
        årsak = it.årsak ?: årsak,
        opphørsdato = it.opphørsdato?.toYearMonth(),
        mottattDato = eldsteSøknad?.mottattDato ?: mottattdato,
        søktFraDato = YearMonth.from(eldsteSøknad?.søknadFomDato ?: søktFomDato),
        virkningstidspunkt = it.virkningstidspunktRolle,
        begrunnelse = tilNotatVirkningstidspunkt(it),
        begrunnelseVurderingAvSkolegang = if (kanSkriveVurderingAvSkolegang(it)) tilNotatVurderingAvSkolegang(it) else null,
        beregnTilDato = YearMonth.from(finnBeregnTilDatoBehandling(it)),
        beregnTil = it.beregnTil,
        innkreving = innkrevingstype == Innkrevingstype.MED_INNKREVING,
        etterfølgendeVedtakVirkningstidspunkt = hentNesteEtterfølgendeVedtak(it)?.virkningstidspunkt,
    )
}

private fun RolleDto.tilNotatRolle() = DokumentmalPersonDto(
    rolle = rolletype,
    navn = navn,
    fødselsdato = fødselsdato,
    ident = ident?.let { Personident(it) },
    saksnummer = saksnummer,
    bidragsmottakerIdent = bidragsmottaker,
    harLøpendeBidrag = harLøpendeBidrag,
    harLøpendeForskudd = harLøpendeForskudd,
)

private fun PersoninfoDto.tilNotatRolle(behandling: Behandling): DokumentmalPersonDto {
    val rolle = behandling.roller.find { it.erSammeRolle(it.ident!!, it.stønadstype) }
    return DokumentmalPersonDto(
        rolle = if (medIBehandlingen == true) rolle?.rolletype else null,
        navn = ident?.let { hentPersonVisningsnavn(it.verdi) } ?: navn,
        fødselsdato = fødselsdato,
        ident = ident,
        bidragsmottakerIdent = rolle?.bidragsmottaker?.ident,
        saksnummer = rolle?.saksnummer,
        revurdering = rolle?.erRevurderingsbarn == true || erRevurderingsbarn,
        harLøpendeForskudd = rolle?.let { behandling.finnesLøpendeForskuddForRolle(rolle) } == true,
        harLøpendeBidrag = rolle?.let { behandling.finnesLøpendeBidragForRolle(rolle) } == true,
    )
}

private fun Rolle.tilNotatRolle() = DokumentmalPersonDto(
    rolle = rolletype,
    navn = hentPersonVisningsnavn(ident),
    fødselsdato = fødselsdato,
    opphørsdato = opphørsdato,
    ident = ident?.let { Personident(it) },
    innbetaltBeløp = innbetaltBeløp,
    saksnummer = saksnummer,
    bidragsmottakerIdent = bidragsmottaker?.ident,
    revurdering = forholdsmessigFordeling?.erRevurdering == true,
    harLøpendeForskudd = behandling.finnesLøpendeForskuddForRolle(this),
    harLøpendeBidrag = behandling.finnesLøpendeBidragForRolle(this),
)

private fun Inntekt.tilNotatInntektDto() = NotatInntektDto(
    beløp =
    maxOf(
        belop.nærmesteHeltall,
        BigDecimal.ZERO,
    ),
    // Kapitalinntekt kan ha negativ verdi. Dette skal ikke vises i frontend
    periode = periode,
    opprinneligPeriode =
    opprinneligFom?.let {
        ÅrMånedsperiode(
            it,
            bestemOpprinneligTomVisningsverdi(),
        )
    },
    type = type,
    kilde = kilde,
    medIBeregning = taMed,
    gjelderBarn =
    gjelderBarn
        ?.let { gjelderBarn ->
            behandling?.roller?.find { it.erSammeRolle(gjelderBarn, gjelderBarnRolle?.stønadstype) }
        }?.tilNotatRolle(),
    historisk = erHistorisk(behandling!!.inntekter),
    inntektsposter =
    inntektsposter
        .map {
            NotatInntektspostDto(
                it.kode,
                it.inntektstype,
                InntektUtil.kapitalinntektFaktor(it.kode) * it.beløp.nærmesteHeltall,
                beløpstype = it.beløpstype ?: InntektBeløpstype.ÅRSBELØP,
                skattefaktor = it.skattefaktor,
                visningsnavn = it.inntektstype?.visningsnavn?.intern ?: finnVisningsnavn(it.kode),
            )
        }.sortedByDescending { it.beløp },
)

private fun List<Inntekt>.inntekterForRolle(rolle: Rolle) = filter { it.erSammeRolle(rolle) }

private fun List<Inntekt>.filtrerKilde(filtrerBareOffentlige: Boolean = false) = filter { !filtrerBareOffentlige || it.kilde == Kilde.OFFENTLIG }

private fun List<SamværBarnDto>.tilNotatSamværDto(behandling: Behandling) = map { samvær ->
    val gjelderBarn = behandling.søknadsbarn.find { it.erSammeRolle(samvær.barn.ident!!, samvær.barn.stønadstype) }!!
    NotatSamværBarnDto(
        gjelderBarn = gjelderBarn.tilNotatRolle(),
        perioder =
        samvær.perioder.map {
            NotatSamværBarnDto.NotatSamværsperiodeDto(
                periode = it.periode.tilNotatPeriode(),
                samværsklasse = it.samværsklasse,
                gjennomsnittligSamværPerMåned = it.gjennomsnittligSamværPerMåned,
                beregning = it.beregning,
            )
        },
        begrunnelse =
        NotatBegrunnelseDto(
            innhold = samvær.begrunnelse?.innhold,
            innholdFraOmgjortVedtak = samvær.begrunnelseFraOpprinneligVedtak?.innhold,
            gjelder = gjelderBarn.tilNotatRolle(),
        ),
    )
}

private fun Behandling.hentInntekterForIdent(
    ident: String,
    rolle: Rolle,
    arbeidsforhold: List<ArbeidsforholdGrunnlagDto>,
    filtrerBareOffentlige: Boolean = false,
    bareMedIBeregning: Boolean = false,
) = InntekterPerRolle(
    gjelder = rolle.tilNotatRolle(),
    beregnetInntekter =
    if (filtrerBareOffentlige) {
        emptyList()
    } else {
        hentBeregnetInntekterForRolle(rolle)
            .filter { it.inntektGjelderBarnIdent != null }
            .map { inntektPerBarn ->
                NotatBeregnetInntektDto(
                    roller.find { it.ident == inntektPerBarn.inntektGjelderBarnIdent!!.verdi }!!.tilNotatRolle(),
                    inntektPerBarn.summertInntektListe,
                )
            }
    },
    årsinntekter =
    inntekter
        .årsinntekterSortert(!filtrerBareOffentlige, true)
        .inntekterForRolle(rolle)
        .ekskluderYtelserFørVirkningstidspunkt()
        .filtrerKilde(filtrerBareOffentlige)
        .filter { !bareMedIBeregning || it.taMed }
        .map {
            it.tilNotatInntektDto()
        },
    barnetillegg =
    inntekter
        .filter { it.type == Inntektsrapportering.BARNETILLEGG }
        .inntekterForRolle(rolle)
        .filtrerKilde(filtrerBareOffentlige)
        .ekskluderYtelserFørVirkningstidspunkt()
        .sorterEtterDatoOgBarn()
        .filter { !bareMedIBeregning || it.taMed }
        .map {
            it.tilNotatInntektDto()
        },
    småbarnstillegg =
    inntekter
        .sortedBy { it.datoFom }
        .filter { it.type == Inntektsrapportering.SMÅBARNSTILLEGG }
        .inntekterForRolle(rolle)
        .filtrerKilde(filtrerBareOffentlige)
        .filter { !bareMedIBeregning || it.taMed }
        .ekskluderYtelserFørVirkningstidspunkt()
        .sorterEtterDato()
        .map {
            it.tilNotatInntektDto()
        },
    kontantstøtte =
    inntekter
        .filter { it.type == Inntektsrapportering.KONTANTSTØTTE }
        .inntekterForRolle(rolle)
        .filtrerKilde(filtrerBareOffentlige)
        .filter { !bareMedIBeregning || it.taMed }
        .ekskluderYtelserFørVirkningstidspunkt()
        .sorterEtterDatoOgBarn()
        .map {
            it.tilNotatInntektDto()
        },
    utvidetBarnetrygd =
    inntekter
        .filter { it.type == Inntektsrapportering.UTVIDET_BARNETRYGD }
        .inntekterForRolle(rolle)
        .filtrerKilde(filtrerBareOffentlige)
        .filter { !bareMedIBeregning || it.taMed }
        .ekskluderYtelserFørVirkningstidspunkt()
        .sorterEtterDato()
        .map {
            it.tilNotatInntektDto()
        },
    arbeidsforhold =
    arbeidsforhold
        .filter { it.partPersonId == ident }
        .map {
            Arbeidsforhold(
                periode = ÅrMånedsperiode(it.startdato!!, it.sluttdato),
                arbeidsgiver = it.arbeidsgiverNavn ?: "-",
                stillingProsent =
                it.ansettelsesdetaljerListe
                    ?.firstOrNull()
                    ?.avtaltStillingsprosent
                    ?.toString(),
                lønnsendringDato = it.ansettelsesdetaljerListe?.firstOrNull()?.sisteLønnsendringDato,
            )
        },
)

private fun DelberegningBidragspliktigesBeregnedeTotalbidragDto.tilNotatDto() = DokumentmalDelberegningBidragspliktigesBeregnedeTotalbidragDto(
    bidragspliktigesBeregnedeTotalbidrag = bidragspliktigesBeregnedeTotalbidrag,
    periode = periode,
    beregnetBidragPerBarnListe =
    beregnetBidragPerBarnListe.map {
        DokumentmalDelberegningBidragspliktigesBeregnedeTotalbidragDto
            .NotatBeregnetBidragPerBarnDto(
                beregnetBidragPerBarn = it.beregnetBidragPerBarn,
                personidentBarn = it.personidentBarn,
            )
    },
)

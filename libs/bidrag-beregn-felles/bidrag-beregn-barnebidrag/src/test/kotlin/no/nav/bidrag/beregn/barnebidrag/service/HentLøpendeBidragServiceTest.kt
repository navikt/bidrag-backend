package no.nav.bidrag.beregn.barnebidrag.service

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.beregn.barnebidrag.felles.FellesTest
import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningPersonConsumer
import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningVedtakConsumer
import no.nav.bidrag.beregn.barnebidrag.service.external.VedtakService
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.HentLøpendeBidragService
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.LøpendeBidragOgBeregninger
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.filtrerVedtakMotBeregningsperiode
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.tilBeregnGrunnlag
import no.nav.bidrag.beregn.barnebidrag.testdata.hentVedtak3
import no.nav.bidrag.beregn.barnebidrag.testdata.hentVedtak5
import no.nav.bidrag.beregn.barnebidrag.testdata.opprettBeregningResponsBBM
import no.nav.bidrag.beregn.barnebidrag.testdata.opprettBidragBeregningResponsDto
import no.nav.bidrag.beregn.barnebidrag.testdata.opprettFlereVedtakBBMOgBehandlingBarn1
import no.nav.bidrag.beregn.barnebidrag.testdata.opprettFlereVedtakBBMOgBehandlingBarn2
import no.nav.bidrag.beregn.barnebidrag.testdata.opprettFlereVedtakBBMOgBehandlingBarn3
import no.nav.bidrag.beregn.barnebidrag.testdata.opprettGrunnlagobjektForBidragspliktig
import no.nav.bidrag.beregn.barnebidrag.testdata.opprettGrunnlagsobjektForSøknadsbarn
import no.nav.bidrag.beregn.barnebidrag.testdata.opprettLøpendeBidrag
import no.nav.bidrag.beregn.barnebidrag.testdata.opprettLøpendeOppfostringsOgUtlandsBidrag
import no.nav.bidrag.beregn.barnebidrag.testdata.opprettVedtakDtoForLøpendeBidrag
import no.nav.bidrag.beregn.barnebidrag.testdata.opprettVedtakForStønad
import no.nav.bidrag.beregn.barnebidrag.testdata.opprettVedtakForStønadBidragsberegning
import no.nav.bidrag.beregn.barnebidrag.testdata.opprettVedtakOppfostringBarn1
import no.nav.bidrag.beregn.barnebidrag.testdata.opprettVedtakUtlandBarn2
import no.nav.bidrag.beregn.barnebidrag.testdata.personIdentAnnetbarn
import no.nav.bidrag.beregn.barnebidrag.testdata.personIdentAnnetbarn2
import no.nav.bidrag.beregn.barnebidrag.testdata.personIdentBidragsmottaker
import no.nav.bidrag.beregn.barnebidrag.testdata.personIdentBidragsmottaker2
import no.nav.bidrag.beregn.barnebidrag.testdata.personIdentBidragspliktig
import no.nav.bidrag.beregn.barnebidrag.testdata.personIdentSøknadsbarn1
import no.nav.bidrag.beregn.barnebidrag.testdata.personIdentSøknadsbarn2
import no.nav.bidrag.beregn.barnebidrag.testdata.saksnummer
import no.nav.bidrag.beregn.barnebidrag.testdata.saksnummer2
import no.nav.bidrag.beregn.barnebidrag.testdata.saksnummer3
import no.nav.bidrag.beregn.vedtak.Vedtaksfiltrering
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.vedtak.BehandlingsrefKilde
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.belopshistorikk.response.BidragPeriode
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidrag
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidragPeriodeResponse
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregningGrunnlagV2
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BidragsberegningOrkestratorRequestV2
import no.nav.bidrag.transport.behandling.beregning.felles.BidragBeregningResponsDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.LøpendeBidragPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåEgenReferanse
import no.nav.bidrag.transport.behandling.vedtak.request.HentManuelleVedtakRequest
import no.nav.bidrag.transport.behandling.vedtak.response.BehandlingsreferanseDto
import no.nav.bidrag.transport.behandling.vedtak.response.StønadsendringDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakForStønad
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakPeriodeDto
import no.nav.bidrag.transport.person.PersonStønad
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

internal class HentLøpendeBidragServiceTest : FellesTest() {
    private lateinit var vedtakService: VedtakService
    private lateinit var hentLøpendeBidragService: HentLøpendeBidragService
    private lateinit var personConsumer: BeregningPersonConsumer
    private lateinit var vedtakFilter: Vedtaksfiltrering
    private lateinit var vedtakConsumer: BeregningVedtakConsumer

    @BeforeEach
    fun setup() {
        vedtakService = mockk()
        hentLøpendeBidragService = HentLøpendeBidragService(vedtakService)
        personConsumer = mockk()
        vedtakFilter = mockk()
        vedtakConsumer = mockk()
        every { personConsumer.hentFødselsdatoForPerson(any()) }.answers {
            LocalDate.now().minusYears(30)
        }
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal hente løpende bidrag for behandling uten manuelle vedtak`() {
        val beregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 12))
        val bidragspliktigIdent = Personident(personIdentBidragspliktig)

        val bidragPeriode = BidragPeriode(
            periode = ÅrMånedsperiode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 1)),
            løpendeBeløp = BigDecimal.valueOf(5000),
            valutakode = "NOK",
        )

        val løpendeBidrag = LøpendeBidrag(
            sak = Saksnummer(saksnummer),
            type = Stønadstype.BIDRAG,
            kravhaver = Personident(personIdentSøknadsbarn1),
            mottaker = Personident(personIdentBidragsmottaker),
            periodeListe = listOf(bidragPeriode),
        )

        every {
            vedtakService.hentAlleStønaderForBidragspliktig(any())
        } returns LøpendeBidragPeriodeResponse(listOf(løpendeBidrag))

        every {
            vedtakService.finnAlleManuelleVedtakForBp(any())
        } returns emptyList()

        val søknadsbarn1Referanse = "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1"

        val beregningBarnListe = listOf(
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarn1Referanse,
                periode = ÅrMånedsperiode("2024-07", "2025-06"),
                beregningsperiode = ÅrMånedsperiode("2024-07", "2025-06"),
                virkningstidspunkt = YearMonth.parse("2024-07"),
                opphørsdato = YearMonth.parse("2024-12"),
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
        )

        val bidragberegningOrkestratorRequestV2 = BidragsberegningOrkestratorRequestV2(
            beregningsperiode = beregningsperiode,
            beregningBarn = beregningBarnListe,
            grunnlagsliste = opprettGrunnlagsobjektForSøknadsbarn(
                mapOf(
                    Personident(personIdentSøknadsbarn1) to "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1",
                ),
            ) + opprettGrunnlagobjektForBidragspliktig(personIdentBidragspliktig),
        )

        val resultat = hentLøpendeBidragService.hentLøpendeBidragForBehandling(
            bidragspliktigIdent,
            beregningsperiode,
            bidragberegningOrkestratorRequestV2,
        )

        assertAll(
            { assertThat(resultat).isNotNull() },
            { assertThat(resultat.løpendeBidragListe).hasSize(1) },
            { assertThat(resultat.løpendeBidragListe[0].sak).isEqualTo(Saksnummer(saksnummer)) },
            { assertThat(resultat.beregnetBeløpListe.beregningListe).isEmpty() },
        )

        verify { vedtakService.hentAlleStønaderForBidragspliktig(any()) }
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal hente løpende bidrag med manuelle vedtak fra BBM`() {
        val beregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 7), YearMonth.of(2025, 6))
        val bidragspliktigIdent = Personident(personIdentBidragspliktig)

        val bidragPeriode = BidragPeriode(
            periode = ÅrMånedsperiode(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 12, 1)),
            løpendeBeløp = BigDecimal.valueOf(5160),
            valutakode = "NOK",
        )

        val løpendeBidrag = LøpendeBidrag(
            sak = Saksnummer(saksnummer),
            type = Stønadstype.BIDRAG,
            kravhaver = Personident(personIdentSøknadsbarn1),
            mottaker = Personident(personIdentBidragsmottaker),
            periodeListe = listOf(bidragPeriode),
        )

        val vedtakForStønad = opprettVedtakForStønad(
            kravhaver = personIdentSøknadsbarn1,
            stønadstype = Stønadstype.BIDRAG,
        )

        val beregningRespons = opprettBidragBeregningResponsDto(
            kravhaver = personIdentSøknadsbarn1,
            sak = saksnummer,
            beregnetBeløp = BigDecimal.valueOf(5160),
            faktiskBeløp = BigDecimal.valueOf(4500),
        )

        every {
            vedtakService.hentAlleStønaderForBidragspliktig(any())
        } returns LøpendeBidragPeriodeResponse(listOf(løpendeBidrag))

        every {
            vedtakService.finnAlleManuelleVedtakForBp(any())
        } returns listOf(vedtakForStønad)

        every {
            vedtakService.hentAlleBeregningerFraBBM(any())
        } returns BidragBeregningResponsDto(beregningRespons)

        val søknadsbarn1Referanse = "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1"

        val beregningBarnListe = listOf(
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarn1Referanse,
                periode = ÅrMånedsperiode("2024-09", "2025-06"),
                beregningsperiode = ÅrMånedsperiode("2024-09", "2025-06"),
                virkningstidspunkt = YearMonth.parse("2024-09"),
                opphørsdato = null,
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
        )

        val bidragberegningOrkestratorRequestV2 = BidragsberegningOrkestratorRequestV2(
            beregningsperiode = beregningsperiode,
            beregningBarn = beregningBarnListe,
            grunnlagsliste = opprettGrunnlagsobjektForSøknadsbarn(
                mapOf(
                    Personident(personIdentSøknadsbarn1) to "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1",
                ),
            ) + opprettGrunnlagobjektForBidragspliktig(personIdentBidragspliktig),
        )

        val resultat = hentLøpendeBidragService.hentLøpendeBidragForBehandling(
            bidragspliktigIdent,
            beregningsperiode,
            bidragberegningOrkestratorRequestV2,
        )

        assertAll(
            { assertThat(resultat).isNotNull() },
            { assertThat(resultat.løpendeBidragListe).hasSize(1) },
            { assertThat(resultat.beregnetBeløpListe.beregningListe).hasSize(1) },
            { assertThat(resultat.beregnetBeløpListe.beregningListe[0].beregnetBeløp).isEqualTo(BigDecimal.valueOf(5160)) },
        )

        verify { vedtakService.hentAlleBeregningerFraBBM(any()) }
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal hente løpende bidrag med manuelle vedtak fra bidrag-vedtak`() {
        val beregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 5), YearMonth.of(2025, 6))
        val bidragspliktigIdent = Personident(personIdentBidragspliktig)
        val beregnetBeløp = BigDecimal.valueOf(5160)
        val resultatBeløp = BigDecimal.valueOf(5000)
        val bruttoBidragEtterBarnetilleggBM = BigDecimal.valueOf(2000)
        val bruttoBidragEtterBarnetilleggBP = BigDecimal.valueOf(1000)

        val løpendeBidrag = LøpendeBidrag(
            sak = Saksnummer(saksnummer),
            type = Stønadstype.BIDRAG,
            kravhaver = Personident(personIdentSøknadsbarn1),
            mottaker = Personident(personIdentBidragsmottaker),
            periodeListe = listOf(
                BidragPeriode(
                    periode = ÅrMånedsperiode(LocalDate.of(2024, 5, 1), null),
                    løpendeBeløp = beregnetBeløp,
                    valutakode = "NOK",
                ),
            ),
        )

        val vedtakForStønad = opprettVedtakForStønadBidragsberegning(
            skyldner = personIdentBidragspliktig,
            kravhaver = personIdentSøknadsbarn1,
            mottaker = personIdentBidragsmottaker,
            sak = saksnummer,
            beregnetBeløp = beregnetBeløp,
        )

        val vedtakDto = opprettVedtakDtoForLøpendeBidrag(
            skyldner = personIdentBidragspliktig,
            kravhaver = personIdentSøknadsbarn1,
            mottaker = personIdentBidragsmottaker,
            sak = saksnummer,
            beregnetBeløp = beregnetBeløp,
            resultatBeløp = resultatBeløp,
        )

        every {
            vedtakService.hentAlleStønaderForBidragspliktig(any())
        } returns LøpendeBidragPeriodeResponse(listOf(løpendeBidrag))

        every {
            vedtakService.finnAlleManuelleVedtakForBp(any())
        } returns listOf(vedtakForStønad)

        every {
            vedtakService.hentVedtak(vedtakForStønad.vedtaksid)
        } returns vedtakDto

        val søknadsbarn1Referanse = "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1"

        val beregningBarnListe = listOf(
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarn1Referanse,
                periode = ÅrMånedsperiode("2024-07", "2025-06"),
                beregningsperiode = ÅrMånedsperiode("2024-07", "2025-06"),
                virkningstidspunkt = YearMonth.parse("2024-07"),
                opphørsdato = YearMonth.parse("2024-12"),
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
        )

        val bidragberegningOrkestratorRequestV2 = BidragsberegningOrkestratorRequestV2(
            beregningsperiode = beregningsperiode,
            beregningBarn = beregningBarnListe,
            grunnlagsliste = opprettGrunnlagsobjektForSøknadsbarn(
                mapOf(
                    Personident(personIdentSøknadsbarn1) to "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1",
                ),
            ) + opprettGrunnlagobjektForBidragspliktig(personIdentBidragspliktig),
        )

        val resultat = hentLøpendeBidragService.hentLøpendeBidragForBehandling(
            bidragspliktigIdent,
            beregningsperiode,
            bidragberegningOrkestratorRequestV2,
        )

        assertAll(
            { assertThat(resultat).isNotNull() },
            { assertThat(resultat.løpendeBidragListe).hasSize(1) },
            { assertThat(resultat.beregnetBeløpListe.beregningListe).hasSize(1) },
            { assertThat(resultat.beregnetBeløpListe.beregningListe[0].beregnetBeløp).isEqualTo(beregnetBeløp) },
            { assertThat(resultat.beregnetBeløpListe.beregningListe[0].faktiskBeløp).isEqualTo(resultatBeløp) },
            { assertThat(resultat.beregnetBeløpListe.beregningListe[0].samværsklasse).isEqualTo(Samværsklasse.SAMVÆRSKLASSE_0) },
            { assertThat(resultat.beregnetBeløpListe.beregningListe[0].bruttoBidragEtterBarnetilleggBM).isEqualTo(bruttoBidragEtterBarnetilleggBM) },
            { assertThat(resultat.beregnetBeløpListe.beregningListe[0].bruttoBidragEtterBarnetilleggBP).isEqualTo(bruttoBidragEtterBarnetilleggBP) },
            { assertThat(resultat.beregnetBeløpListe.beregningListe[0].erVedtakKildeBBM).isFalse },
        )

        verify { vedtakService.hentVedtak(vedtakForStønad.vedtaksid) }
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal filtrere bort perioder utenfor beregningsperioden`() {
        val beregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 6), YearMonth.of(2024, 12))
        val bidragspliktigIdent = Personident(personIdentBidragspliktig)

        val løpendeBidrag = LøpendeBidrag(
            sak = Saksnummer(saksnummer),
            type = Stønadstype.BIDRAG,
            kravhaver = Personident(personIdentSøknadsbarn1),
            mottaker = Personident(personIdentBidragsmottaker),
            periodeListe = listOf(
                BidragPeriode(
                    periode = ÅrMånedsperiode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 5, 1)),
                    løpendeBeløp = BigDecimal.valueOf(3000),
                    valutakode = "NOK",
                ),
                BidragPeriode(
                    periode = ÅrMånedsperiode(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 12, 1)),
                    løpendeBeløp = BigDecimal.valueOf(5000),
                    valutakode = "NOK",
                ),
                BidragPeriode(
                    periode = ÅrMånedsperiode(LocalDate.of(2025, 1, 1), null),
                    løpendeBeløp = BigDecimal.valueOf(6000),
                    valutakode = "NOK",
                ),
            ),
        )

        every {
            vedtakService.hentAlleStønaderForBidragspliktig(any())
        } returns LøpendeBidragPeriodeResponse(listOf(løpendeBidrag))

        every {
            vedtakService.finnAlleManuelleVedtakForBp(any())
        } returns emptyList()

        val søknadsbarn1Referanse = "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1"

        val beregningBarnListe = listOf(
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarn1Referanse,
                periode = ÅrMånedsperiode("2024-07", "2025-06"),
                beregningsperiode = ÅrMånedsperiode("2024-07", "2025-06"),
                virkningstidspunkt = YearMonth.parse("2024-07"),
                opphørsdato = YearMonth.parse("2024-12"),
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
        )

        val bidragberegningOrkestratorRequestV2 = BidragsberegningOrkestratorRequestV2(
            beregningsperiode = beregningsperiode,
            beregningBarn = beregningBarnListe,
            grunnlagsliste = opprettGrunnlagsobjektForSøknadsbarn(
                mapOf(
                    Personident(personIdentSøknadsbarn1) to "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1",
                ),
            ) + opprettGrunnlagobjektForBidragspliktig(personIdentBidragspliktig),
        )

        val resultat = hentLøpendeBidragService.hentLøpendeBidragForBehandling(
            bidragspliktigIdent,
            beregningsperiode,
            bidragberegningOrkestratorRequestV2,
        )

        assertAll(
            { assertThat(resultat).isNotNull() },
            { assertThat(resultat.løpendeBidragListe).hasSize(1) },
            { assertThat(resultat.løpendeBidragListe[0].periodeListe).hasSize(1) },
            { assertThat(resultat.løpendeBidragListe[0].periodeListe[0].periode.fom).isEqualTo(YearMonth.of(2024, 6)) },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal returnere tom liste når ingen stønader finnes`() {
        val beregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 12))
        val bidragspliktigIdent = Personident(personIdentBidragspliktig)

        every {
            vedtakService.hentAlleStønaderForBidragspliktig(any())
        } returns LøpendeBidragPeriodeResponse(emptyList())

        val søknadsbarn1Referanse = "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1"

        val beregningBarnListe = listOf(
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarn1Referanse,
                periode = ÅrMånedsperiode("2024-07", "2025-06"),
                beregningsperiode = ÅrMånedsperiode("2024-07", "2025-06"),
                virkningstidspunkt = YearMonth.parse("2024-07"),
                opphørsdato = YearMonth.parse("2024-12"),
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
        )

        val bidragberegningOrkestratorRequestV2 = BidragsberegningOrkestratorRequestV2(
            beregningsperiode = beregningsperiode,
            beregningBarn = beregningBarnListe,
            grunnlagsliste = opprettGrunnlagsobjektForSøknadsbarn(
                mapOf(
                    Personident(personIdentSøknadsbarn1) to "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1",
                ),
            ) + opprettGrunnlagobjektForBidragspliktig(personIdentBidragspliktig),
        )

        val resultat = hentLøpendeBidragService.hentLøpendeBidragForBehandling(
            bidragspliktigIdent,
            beregningsperiode,
            bidragberegningOrkestratorRequestV2,
        )

        assertAll(
            { assertThat(resultat).isNotNull() },
            { assertThat(resultat.løpendeBidragListe).isEmpty() },
            { assertThat(resultat.beregnetBeløpListe.beregningListe).isEmpty() },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `filtrerVedtakMotBeregningsperiode skal returnere vedtak som dekker starten av beregningsperioden`() {
        val beregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 7), YearMonth.of(2025, 6))
        val stønadOgVedtakListe: Map<Stønadsid, List<VedtakForStønad>> = mapOf(
            Stønadsid(
                type = Stønadstype.BIDRAG,
                kravhaver = Personident(personIdentSøknadsbarn1),
                skyldner = Personident(personIdentBidragspliktig),
                sak = Saksnummer(saksnummer),
            ) to listOf(
                opprettVedtakForStønadBidragsberegning(
                    skyldner = personIdentBidragspliktig,
                    kravhaver = personIdentSøknadsbarn1,
                    mottaker = personIdentBidragsmottaker,
                    sak = saksnummer,
                    beregnetBeløp = BigDecimal.valueOf(5000),
                ),
            ),
        )

        val resultat = stønadOgVedtakListe.filtrerVedtakMotBeregningsperiode(beregningsperiode)

        assertAll(
            { assertThat(resultat).hasSize(1) },
            { assertThat(resultat[0].stønadsendring.periodeListe[0].periode.fom).isEqualTo(YearMonth.of(2024, 7)) },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal håndtere flere kravhavere`() {
        val bpReferanse = "person_PERSON_BIDRAGSPLIKTIG_12345"
        val bMIdent1 = Personident(personIdentBidragsmottaker)
        val bMIdent2 = Personident(personIdentBidragsmottaker2)
        val søknadsbarn1Ident = Personident(personIdentSøknadsbarn1)
        val søknadsbarn2Ident = Personident(personIdentSøknadsbarn2)
        val annetBarnIdent = Personident(personIdentAnnetbarn)
        val søknadsbarnIdentMap = mapOf(
            søknadsbarn1Ident to "person_PERSON_SØKNADSBARN_${søknadsbarn1Ident.verdi}",
            søknadsbarn2Ident to "person_PERSON_SØKNADSBARN_${søknadsbarn2Ident.verdi}",
        )
        val søknadsbarnListe = listOf(
            PersonStønad(
                ident = søknadsbarn1Ident.verdi,
                stønadstype = Stønadstype.BIDRAG,
            ),
            PersonStønad(
                ident = søknadsbarn2Ident.verdi,
                stønadstype = Stønadstype.BIDRAG,
            ),
        )
        val løpendeBarnFødselsdatoMap = mapOf(
            søknadsbarn1Ident to LocalDate.now().minusYears(10),
            søknadsbarn2Ident to LocalDate.now().minusYears(8),
            annetBarnIdent to LocalDate.now().minusYears(12),
        )
        val beregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 6), YearMonth.of(2024, 12))

        val løpendeBidragListe = listOf(
            LøpendeBidrag(
                sak = Saksnummer(saksnummer),
                type = Stønadstype.BIDRAG,
                kravhaver = søknadsbarn1Ident,
                mottaker = bMIdent1,
                periodeListe = listOf(
                    BidragPeriode(
                        periode = ÅrMånedsperiode(YearMonth.of(2024, 7), YearMonth.of(2024, 12)),
                        løpendeBeløp = BigDecimal.valueOf(5000),
                        valutakode = "NOK",
                    ),
                ),
            ),
            LøpendeBidrag(
                sak = Saksnummer(saksnummer),
                type = Stønadstype.BIDRAG,
                kravhaver = søknadsbarn2Ident,
                mottaker = bMIdent1,
                periodeListe = listOf(
                    BidragPeriode(
                        periode = ÅrMånedsperiode(YearMonth.of(2024, 6), YearMonth.of(2024, 12)),
                        løpendeBeløp = BigDecimal.valueOf(3000),
                        valutakode = "NOK",
                    ),
                ),
            ),
            LøpendeBidrag(
                sak = Saksnummer(saksnummer2),
                type = Stønadstype.BIDRAG18AAR,
                kravhaver = annetBarnIdent,
                mottaker = bMIdent2,
                periodeListe = listOf(
                    BidragPeriode(
                        periode = ÅrMånedsperiode(YearMonth.of(2024, 8), YearMonth.of(2024, 10)),
                        løpendeBeløp = BigDecimal.valueOf(1700),
                        valutakode = "NOK",
                    ),
                ),
            ),
        )

        val beregninger = listOf(
            BidragBeregningResponsDto.BidragBeregning(
                periode = ÅrMånedsperiode(YearMonth.of(2024, 7), null),
                saksnummer = saksnummer,
                personidentBarn = søknadsbarn1Ident,
                datoSøknad = LocalDate.now(),
                beregnetBeløp = BigDecimal.valueOf(5160),
                faktiskBeløp = BigDecimal.valueOf(5000),
                beløpSamvær = BigDecimal.ZERO,
                stønadstype = Stønadstype.BIDRAG,
                samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1,
            ),
            BidragBeregningResponsDto.BidragBeregning(
                periode = ÅrMånedsperiode(YearMonth.of(2024, 6), null),
                saksnummer = saksnummer,
                personidentBarn = søknadsbarn2Ident,
                datoSøknad = LocalDate.now(),
                beregnetBeløp = BigDecimal.valueOf(3200),
                faktiskBeløp = BigDecimal.valueOf(3000),
                beløpSamvær = BigDecimal.ZERO,
                stønadstype = Stønadstype.BIDRAG,
                samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
            ),
            BidragBeregningResponsDto.BidragBeregning(
                periode = ÅrMånedsperiode(YearMonth.of(2024, 8), null),
                saksnummer = saksnummer2,
                personidentBarn = annetBarnIdent,
                datoSøknad = LocalDate.now(),
                beregnetBeløp = BigDecimal.valueOf(1500),
                faktiskBeløp = BigDecimal.valueOf(1600),
                beløpSamvær = BigDecimal.valueOf(1000),
                stønadstype = Stønadstype.BIDRAG18AAR,
                samværsklasse = Samværsklasse.SAMVÆRSKLASSE_2,
            ),
        )

        val løpendeBidragOgBeregninger = LøpendeBidragOgBeregninger(
            beregnetBeløpListe = BidragBeregningResponsDto(beregninger),
            løpendeBidragListe = løpendeBidragListe,
        )

        val beregningBarnListe = listOf(
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarnIdentMap[søknadsbarn1Ident]!!,
                periode = ÅrMånedsperiode("2024-06", "2024-12"),
                beregningsperiode = ÅrMånedsperiode("2024-06", "2024-12"),
                virkningstidspunkt = YearMonth.parse("2024-06"),
                opphørsdato = null,
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarnIdentMap[søknadsbarn2Ident]!!,
                periode = ÅrMånedsperiode("2024-06", "2024-12"),
                beregningsperiode = ÅrMånedsperiode("2024-06", "2024-12"),
                virkningstidspunkt = YearMonth.parse("2024-06"),
                opphørsdato = null,
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
        )

        val bidragberegningOrkestratorRequestV2 = BidragsberegningOrkestratorRequestV2(
            beregningsperiode = beregningsperiode,
            beregningBarn = beregningBarnListe,
            grunnlagsliste = opprettGrunnlagsobjektForSøknadsbarn(søknadsbarnIdentMap) +
                opprettGrunnlagobjektForBidragspliktig(personIdentBidragspliktig),

        )

        val resultat = løpendeBidragOgBeregninger.tilBeregnGrunnlag(
            bpReferanse = bpReferanse,
            søknadsbarnListe = søknadsbarnListe,
            løpendeBarnFødselsdatoMap = løpendeBarnFødselsdatoMap,
            personConsumer = personConsumer,
            request = bidragberegningOrkestratorRequestV2,
            utlandssakerListe = emptyList(),
        )

        assertSoftly {
            resultat shouldHaveSize 1
            resultat[0].stønadstype shouldBe Stønadstype.BIDRAG18AAR
            resultat[0].grunnlagListe shouldHaveSize 3
        }
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal hente beregning fra BBM når vedtak er fattet i Bisys`() {
        val beregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 7), YearMonth.of(2025, 6))
        val bidragspliktigIdent = Personident(personIdentBidragspliktig)

        val løpendeBidrag = LøpendeBidrag(
            sak = Saksnummer(saksnummer),
            type = Stønadstype.BIDRAG,
            kravhaver = Personident(personIdentSøknadsbarn1),
            mottaker = Personident(personIdentBidragsmottaker),
            periodeListe = listOf(
                BidragPeriode(
                    periode = ÅrMånedsperiode(LocalDate.of(2024, 7, 1), null),
                    løpendeBeløp = BigDecimal.valueOf(5000),
                    valutakode = "NOK",
                ),
            ),
        )

        val vedtakForStønad = opprettVedtakForStønad(
            kravhaver = personIdentSøknadsbarn1,
            stønadstype = Stønadstype.BIDRAG,
        )

        val beregningRespons = opprettBidragBeregningResponsDto(
            kravhaver = personIdentSøknadsbarn1,
            sak = saksnummer,
            beregnetBeløp = BigDecimal.valueOf(5000),
            faktiskBeløp = BigDecimal.valueOf(4500),
        )

        every {
            vedtakService.hentAlleStønaderForBidragspliktig(any())
        } returns LøpendeBidragPeriodeResponse(listOf(løpendeBidrag))

        every {
            vedtakService.finnAlleManuelleVedtakForBp(any())
        } returns listOf(vedtakForStønad)

        every {
            vedtakService.hentAlleBeregningerFraBBM(any())
        } returns BidragBeregningResponsDto(beregningRespons)

        val søknadsbarn1Referanse = "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1"

        val beregningBarnListe = listOf(
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarn1Referanse,
                periode = ÅrMånedsperiode("2024-09", "2025-06"),
                beregningsperiode = ÅrMånedsperiode("2024-09", "2025-06"),
                virkningstidspunkt = YearMonth.parse("2024-09"),
                opphørsdato = null,
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
        )

        val bidragberegningOrkestratorRequestV2 = BidragsberegningOrkestratorRequestV2(
            beregningsperiode = beregningsperiode,
            beregningBarn = beregningBarnListe,
            grunnlagsliste = opprettGrunnlagsobjektForSøknadsbarn(
                mapOf(
                    Personident(personIdentSøknadsbarn1) to "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1",
                ),
            ) + opprettGrunnlagobjektForBidragspliktig(personIdentBidragspliktig),
        )

        val resultat = hentLøpendeBidragService.hentLøpendeBidragForBehandling(
            bidragspliktigIdent,
            beregningsperiode,
            bidragberegningOrkestratorRequestV2,
        )

        assertAll(
            { assertThat(resultat.beregnetBeløpListe.beregningListe).hasSize(1) },
            { assertThat(resultat.beregnetBeløpListe.beregningListe[0].beregnetBeløp).isEqualTo(BigDecimal.valueOf(5000)) },
            { assertThat(resultat.beregnetBeløpListe.beregningListe[0].faktiskBeløp).isEqualTo(BigDecimal.valueOf(4500)) },
            { assertThat(resultat.beregnetBeløpListe.beregningListe[0].beløpSamvær).isEqualTo(BigDecimal.ZERO) },
            { assertThat(resultat.beregnetBeløpListe.beregningListe[0].samværsklasse).isEqualTo(Samværsklasse.SAMVÆRSKLASSE_0) },
        )

        verify(exactly = 1) { vedtakService.hentAlleBeregningerFraBBM(any()) }
        verify(exactly = 0) { vedtakService.hentVedtak(any()) }
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal hente beregning fra bidrag-vedtak når vedtak har behandlingsreferanse`() {
        val beregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 7), YearMonth.of(2025, 6))
        val bidragspliktigIdent = Personident(personIdentBidragspliktig)
        val beregnetBeløp = BigDecimal.valueOf(5160)
        val resultatBeløp = BigDecimal.valueOf(5000)

        val løpendeBidrag = LøpendeBidrag(
            sak = Saksnummer(saksnummer),
            type = Stønadstype.BIDRAG,
            kravhaver = Personident(personIdentSøknadsbarn1),
            mottaker = Personident(personIdentBidragsmottaker),
            periodeListe = listOf(
                BidragPeriode(
                    periode = ÅrMånedsperiode(LocalDate.of(2024, 7, 1), null),
                    løpendeBeløp = beregnetBeløp,
                    valutakode = "NOK",
                ),
            ),
        )

        val vedtakForStønad = opprettVedtakForStønadBidragsberegning(
            skyldner = personIdentBidragspliktig,
            kravhaver = personIdentSøknadsbarn1,
            mottaker = personIdentBidragsmottaker,
            sak = saksnummer,
            beregnetBeløp = beregnetBeløp,
        )

        val vedtakDto = opprettVedtakDtoForLøpendeBidrag(
            skyldner = personIdentBidragspliktig,
            kravhaver = personIdentSøknadsbarn1,
            mottaker = personIdentBidragsmottaker,
            sak = saksnummer,
            beregnetBeløp = beregnetBeløp,
            resultatBeløp = resultatBeløp,
        )

        every {
            vedtakService.hentAlleStønaderForBidragspliktig(any())
        } returns LøpendeBidragPeriodeResponse(listOf(løpendeBidrag))

        every {
            vedtakService.finnAlleManuelleVedtakForBp(any())
        } returns listOf(vedtakForStønad)

        every {
            vedtakService.hentVedtak(vedtakForStønad.vedtaksid)
        } returns vedtakDto

        val søknadsbarn1Referanse = "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1"

        val beregningBarnListe = listOf(
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarn1Referanse,
                periode = ÅrMånedsperiode("2024-09", "2025-06"),
                beregningsperiode = ÅrMånedsperiode("2024-09", "2025-06"),
                virkningstidspunkt = YearMonth.parse("2024-09"),
                opphørsdato = null,
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
        )

        val bidragberegningOrkestratorRequestV2 = BidragsberegningOrkestratorRequestV2(
            beregningsperiode = beregningsperiode,
            beregningBarn = beregningBarnListe,
            grunnlagsliste = opprettGrunnlagsobjektForSøknadsbarn(
                mapOf(
                    Personident(personIdentSøknadsbarn1) to "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1",
                ),
            ) + opprettGrunnlagobjektForBidragspliktig(personIdentBidragspliktig),
        )

        val resultat = hentLøpendeBidragService.hentLøpendeBidragForBehandling(
            bidragspliktigIdent,
            beregningsperiode,
            bidragberegningOrkestratorRequestV2,
        )

        assertAll(
            { assertThat(resultat.beregnetBeløpListe.beregningListe).hasSize(1) },
            { assertThat(resultat.beregnetBeløpListe.beregningListe[0].beregnetBeløp).isEqualTo(beregnetBeløp) },
            { assertThat(resultat.beregnetBeløpListe.beregningListe[0].faktiskBeløp).isEqualTo(resultatBeløp) },
        )

        verify(exactly = 1) { vedtakService.hentVedtak(vedtakForStønad.vedtaksid) }
        verify(exactly = 0) { vedtakService.hentAlleBeregningerFraBBM(any()) }
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal returnere tom beregningsliste når ingen manuelle vedtak finnes`() {
        val beregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 12))
        val bidragspliktigIdent = Personident(personIdentBidragspliktig)

        val løpendeBidrag = LøpendeBidrag(
            sak = Saksnummer(saksnummer),
            type = Stønadstype.BIDRAG,
            kravhaver = Personident(personIdentSøknadsbarn1),
            mottaker = Personident(personIdentBidragsmottaker),
            periodeListe = listOf(
                BidragPeriode(
                    periode = ÅrMånedsperiode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 1)),
                    løpendeBeløp = BigDecimal.valueOf(5000),
                    valutakode = "NOK",
                ),
            ),
        )

        every {
            vedtakService.hentAlleStønaderForBidragspliktig(any())
        } returns LøpendeBidragPeriodeResponse(listOf(løpendeBidrag))

        every {
            vedtakService.finnAlleManuelleVedtakForBp(any())
        } returns emptyList()

        val søknadsbarn1Referanse = "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1"

        val beregningBarnListe = listOf(
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarn1Referanse,
                periode = ÅrMånedsperiode("2024-07", "2025-06"),
                beregningsperiode = ÅrMånedsperiode("2024-07", "2025-06"),
                virkningstidspunkt = YearMonth.parse("2024-07"),
                opphørsdato = YearMonth.parse("2024-12"),
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
        )

        val bidragberegningOrkestratorRequestV2 = BidragsberegningOrkestratorRequestV2(
            beregningsperiode = beregningsperiode,
            beregningBarn = beregningBarnListe,
            grunnlagsliste = opprettGrunnlagsobjektForSøknadsbarn(
                mapOf(
                    Personident(personIdentSøknadsbarn1) to "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1",
                ),
            ) + opprettGrunnlagobjektForBidragspliktig(personIdentBidragspliktig),
        )

        val resultat = hentLøpendeBidragService.hentLøpendeBidragForBehandling(
            bidragspliktigIdent,
            beregningsperiode,
            bidragberegningOrkestratorRequestV2,
        )

        assertAll(
            { assertThat(resultat.beregnetBeløpListe.beregningListe).isEmpty() },
        )

        verify(exactly = 0) { vedtakService.hentAlleBeregningerFraBBM(any()) }
        verify(exactly = 0) { vedtakService.hentVedtak(any()) }
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal returnere tom beregningsliste når vedtak ikke finnes i bidrag-vedtak`() {
        val beregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 7), YearMonth.of(2025, 6))
        val bidragspliktigIdent = Personident(personIdentBidragspliktig)

        val løpendeBidrag = LøpendeBidrag(
            sak = Saksnummer(saksnummer),
            type = Stønadstype.BIDRAG,
            kravhaver = Personident(personIdentSøknadsbarn1),
            mottaker = Personident(personIdentBidragsmottaker),
            periodeListe = listOf(
                BidragPeriode(
                    periode = ÅrMånedsperiode(LocalDate.of(2024, 7, 1), null),
                    løpendeBeløp = BigDecimal.valueOf(5000),
                    valutakode = "NOK",
                ),
            ),
        )

        val vedtakForStønad = opprettVedtakForStønadBidragsberegning(
            skyldner = personIdentBidragspliktig,
            kravhaver = personIdentSøknadsbarn1,
            mottaker = personIdentBidragsmottaker,
            sak = saksnummer,
            beregnetBeløp = BigDecimal.valueOf(5000),
        )

        every {
            vedtakService.hentAlleStønaderForBidragspliktig(any())
        } returns LøpendeBidragPeriodeResponse(listOf(løpendeBidrag))

        every {
            vedtakService.finnAlleManuelleVedtakForBp(any())
        } returns listOf(vedtakForStønad)

        every {
            vedtakService.hentVedtak(vedtakForStønad.vedtaksid)
        } returns null

        val søknadsbarn1Referanse = "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1"

        val beregningBarnListe = listOf(
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarn1Referanse,
                periode = ÅrMånedsperiode("2024-07", "2025-06"),
                beregningsperiode = ÅrMånedsperiode("2024-07", "2025-06"),
                virkningstidspunkt = YearMonth.parse("2024-07"),
                opphørsdato = YearMonth.parse("2024-12"),
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
        )

        val bidragberegningOrkestratorRequestV2 = BidragsberegningOrkestratorRequestV2(
            beregningsperiode = beregningsperiode,
            beregningBarn = beregningBarnListe,
            grunnlagsliste = opprettGrunnlagsobjektForSøknadsbarn(
                mapOf(
                    Personident(personIdentSøknadsbarn1) to "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1",
                ),
            ) + opprettGrunnlagobjektForBidragspliktig(personIdentBidragspliktig),
        )

        val resultat = hentLøpendeBidragService.hentLøpendeBidragForBehandling(
            bidragspliktigIdent,
            beregningsperiode,
            bidragberegningOrkestratorRequestV2,
        )

        assertAll(
            { assertThat(resultat.beregnetBeløpListe.beregningListe).isEmpty() },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal slå sammen beregninger fra BBM og bidrag-vedtak`() {
        val beregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 7), YearMonth.of(2025, 6))
        val bidragspliktigIdent = Personident(personIdentBidragspliktig)

        val løpendeBidragListe = listOf(
            LøpendeBidrag(
                sak = Saksnummer(saksnummer),
                type = Stønadstype.BIDRAG,
                kravhaver = Personident(personIdentSøknadsbarn1),
                mottaker = Personident(personIdentBidragsmottaker),
                periodeListe = listOf(
                    BidragPeriode(
                        periode = ÅrMånedsperiode(LocalDate.of(2024, 7, 1), null),
                        løpendeBeløp = BigDecimal.valueOf(5000),
                        valutakode = "NOK",
                    ),
                ),
            ),
            LøpendeBidrag(
                sak = Saksnummer(saksnummer),
                type = Stønadstype.BIDRAG,
                kravhaver = Personident(personIdentSøknadsbarn2),
                mottaker = Personident(personIdentBidragsmottaker),
                periodeListe = listOf(
                    BidragPeriode(
                        periode = ÅrMånedsperiode(LocalDate.of(2024, 7, 1), null),
                        løpendeBeløp = BigDecimal.valueOf(3000),
                        valutakode = "NOK",
                    ),
                ),
            ),
        )

        val vedtakForStønadFraBBM = opprettVedtakForStønad(
            kravhaver = personIdentSøknadsbarn1,
            stønadstype = Stønadstype.BIDRAG,
        )

        val vedtakForStønadFraBidragVedtak = opprettVedtakForStønadBidragsberegning(
            skyldner = personIdentBidragspliktig,
            kravhaver = personIdentSøknadsbarn2,
            mottaker = personIdentBidragsmottaker,
            sak = saksnummer,
            beregnetBeløp = BigDecimal.valueOf(3000),
        )

        val vedtakDto = opprettVedtakDtoForLøpendeBidrag(
            skyldner = personIdentBidragspliktig,
            kravhaver = personIdentSøknadsbarn2,
            mottaker = personIdentBidragsmottaker,
            sak = saksnummer,
            beregnetBeløp = BigDecimal.valueOf(3200),
            resultatBeløp = BigDecimal.valueOf(3000),
        )

        val beregningResponsFraBBM = opprettBidragBeregningResponsDto(
            kravhaver = personIdentSøknadsbarn1,
            sak = saksnummer,
            beregnetBeløp = BigDecimal.valueOf(5000),
            faktiskBeløp = BigDecimal.valueOf(4500),
        )

        every {
            vedtakService.hentAlleStønaderForBidragspliktig(any())
        } returns LøpendeBidragPeriodeResponse(løpendeBidragListe)

        every {
            vedtakService.finnAlleManuelleVedtakForBp(any())
        } returns listOf(vedtakForStønadFraBBM, vedtakForStønadFraBidragVedtak)

        every {
            vedtakService.hentAlleBeregningerFraBBM(any())
        } returns BidragBeregningResponsDto(beregningResponsFraBBM)

        every {
            vedtakService.hentVedtak(vedtakForStønadFraBidragVedtak.vedtaksid)
        } returns vedtakDto

        val søknadsbarn1Referanse = "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1"

        val beregningBarnListe = listOf(
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarn1Referanse,
                periode = ÅrMånedsperiode("2024-10", "2025-06"),
                beregningsperiode = ÅrMånedsperiode("2024-10", "2025-06"),
                virkningstidspunkt = YearMonth.parse("2024-10"),
                opphørsdato = null,
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
        )

        val bidragberegningOrkestratorRequestV2 = BidragsberegningOrkestratorRequestV2(
            beregningsperiode = beregningsperiode,
            beregningBarn = beregningBarnListe,
            grunnlagsliste = opprettGrunnlagsobjektForSøknadsbarn(
                mapOf(
                    Personident(personIdentSøknadsbarn1) to "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1",
                ),
            ) + opprettGrunnlagobjektForBidragspliktig(personIdentBidragspliktig),
        )

        val resultat = hentLøpendeBidragService.hentLøpendeBidragForBehandling(
            bidragspliktigIdent,
            beregningsperiode,
            bidragberegningOrkestratorRequestV2,
        )

        assertAll(
            { assertThat(resultat.beregnetBeløpListe.beregningListe).hasSize(2) },
        )

        verify(exactly = 1) { vedtakService.hentAlleBeregningerFraBBM(any()) }
        verify(exactly = 1) { vedtakService.hentVedtak(any()) }
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal håndtere kombinasjon av manuelle vedtak fra BBM og bidrag-vedtak`() {
        val beregningsperiode = ÅrMånedsperiode(YearMonth.of(2023, 7), YearMonth.of(2025, 6))
        val bidragspliktigIdent = Personident(personIdentBidragspliktig)
        val søknadsbarn1Referanse = "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1"
        val søknadsbarn2Referanse = "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn2"

        val løpendeBidragListe = opprettLøpendeBidrag()

        every {
            vedtakService.hentAlleStønaderForBidragspliktig(any())
        } returns LøpendeBidragPeriodeResponse(løpendeBidragListe)

        every {
            vedtakService.finnAlleManuelleVedtakForBp(
                HentManuelleVedtakRequest(
                    skyldner = Personident(personIdentBidragspliktig),
                ),
            )
        } returns opprettFlereVedtakBBMOgBehandlingBarn1() + opprettFlereVedtakBBMOgBehandlingBarn2() + opprettFlereVedtakBBMOgBehandlingBarn3()

        every {
            vedtakService.hentAlleBeregningerFraBBM(any())
        } returns BidragBeregningResponsDto(opprettBeregningResponsBBM())

        every {
            vedtakService.hentVedtak(vedtaksId = 3)
        } returns hentVedtak3()

        every {
            vedtakService.hentVedtak(vedtaksId = 5)
        } returns hentVedtak5()

        val beregningBarnListe = listOf(
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarn1Referanse,
                periode = ÅrMånedsperiode("2023-07", "2025-06"),
                beregningsperiode = ÅrMånedsperiode("2023-07", "2025-06"),
                virkningstidspunkt = YearMonth.parse("2023-07"),
                opphørsdato = null,
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarn2Referanse,
                periode = ÅrMånedsperiode("2024-08", "2025-06"),
                beregningsperiode = ÅrMånedsperiode("2024-08", "2025-06"),
                virkningstidspunkt = YearMonth.parse("2024-08"),
                opphørsdato = null,
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
        )

        val bidragberegningOrkestratorRequestV2 = BidragsberegningOrkestratorRequestV2(
            beregningsperiode = beregningsperiode,
            beregningBarn = beregningBarnListe,
            grunnlagsliste = opprettGrunnlagsobjektForSøknadsbarn(
                mapOf(
                    Personident(personIdentSøknadsbarn1) to "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1",
                    Personident(personIdentSøknadsbarn2) to "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn2",
                ),
            ) + opprettGrunnlagobjektForBidragspliktig(personIdentBidragspliktig),
        )

        val løpendeBidragOgBeregninger = hentLøpendeBidragService.hentLøpendeBidragForBehandling(
            bidragspliktigIdent,
            beregningsperiode,
            bidragberegningOrkestratorRequestV2,
        )

        val resultatGrunnlagsobjekter = løpendeBidragOgBeregninger.tilBeregnGrunnlag(
            bpReferanse = "bPReferanse",
            søknadsbarnListe = listOf(
                PersonStønad(personIdentSøknadsbarn1, Stønadstype.BIDRAG),
                PersonStønad(personIdentSøknadsbarn2, Stønadstype.BIDRAG),
            ),
            løpendeBarnFødselsdatoMap = mapOf(
                Personident(personIdentAnnetbarn) to LocalDate.now().minusYears(10),
            ),
            personConsumer = personConsumer,
            request = bidragberegningOrkestratorRequestV2,
            utlandssakerListe = listOf("1234567"),
        ).sortedBy { it.søknadsbarnReferanse }

        val løpendeBidragPeriodeListe = resultatGrunnlagsobjekter.flatMap { it.grunnlagListe }
            .filtrerOgKonverterBasertPåEgenReferanse<LøpendeBidragPeriode>(Grunnlagstype.LØPENDE_BIDRAG_PERIODE)
            .map {
                LøpendeBidragPeriode(
                    periode = it.innhold.periode,
                    saksnummer = it.innhold.saksnummer,
                    stønadstype = it.innhold.stønadstype,
                    løpendeBeløp = it.innhold.løpendeBeløp,
                    valutakode = it.innhold.valutakode,
                    samværsklasse = it.innhold.samværsklasse,
                    beregnetBeløp = it.innhold.beregnetBeløp,
                    faktiskBeløp = it.innhold.faktiskBeløp,
                    sakskategori = it.innhold.sakskategori,
                    manueltRegistrert = it.innhold.manueltRegistrert,
                )
            }

        assertAll(
            //

            { assertThat(resultatGrunnlagsobjekter).hasSize(2) },
            { assertThat(resultatGrunnlagsobjekter[0].periode.fom).isEqualTo(YearMonth.of(2023, 7)) },
            { assertThat(resultatGrunnlagsobjekter[0].periode.til).isEqualTo(YearMonth.of(2025, 6)) },
            { assertThat(resultatGrunnlagsobjekter[0].søknadsbarnReferanse).contains("person_PERSON_BARN_BIDRAGSPLIKTIG") },
            { assertThat(resultatGrunnlagsobjekter[0].stønadstype).isEqualTo(Stønadstype.BIDRAG) },
            { assertThat(resultatGrunnlagsobjekter[0].grunnlagListe).hasSize(4) },

            { assertThat(resultatGrunnlagsobjekter[1].periode.fom).isEqualTo(YearMonth.of(2024, 5)) },
            { assertThat(resultatGrunnlagsobjekter[1].periode.til).isEqualTo(YearMonth.of(2024, 8)) },
            { assertThat(resultatGrunnlagsobjekter[1].søknadsbarnReferanse).isEqualTo("person_PERSON_SØKNADSBARN_44444") },
            { assertThat(resultatGrunnlagsobjekter[1].stønadstype).isEqualTo(Stønadstype.BIDRAG) },
            { assertThat(resultatGrunnlagsobjekter[1].grunnlagListe).hasSize(4) },

            { assertThat(løpendeBidragPeriodeListe[0].periode.fom).isEqualTo(YearMonth.of(2023, 7)) },
            { assertThat(løpendeBidragPeriodeListe[0].periode.til).isEqualTo(YearMonth.of(2024, 2)) },
            { assertThat(løpendeBidragPeriodeListe[0].saksnummer.verdi).isEqualTo(saksnummer2) },
            { assertThat(løpendeBidragPeriodeListe[0].løpendeBeløp).isEqualTo(BigDecimal.valueOf(52020)) },
            { assertThat(løpendeBidragPeriodeListe[0].samværsklasse).isNull() },
            { assertThat(løpendeBidragPeriodeListe[0].beregnetBeløp).isEqualTo(BigDecimal.valueOf(52020)) },
            { assertThat(løpendeBidragPeriodeListe[0].faktiskBeløp).isEqualTo(BigDecimal.valueOf(52020)) },
            { assertThat(løpendeBidragPeriodeListe[0].sakskategori).isEqualTo(Sakskategori.UTLAND) },

            { assertThat(løpendeBidragPeriodeListe[1].periode.fom).isEqualTo(YearMonth.of(2024, 2)) },
            { assertThat(løpendeBidragPeriodeListe[1].periode.til).isEqualTo(YearMonth.of(2025, 6)) },
            { assertThat(løpendeBidragPeriodeListe[1].saksnummer.verdi).isEqualTo(saksnummer2) },
            { assertThat(løpendeBidragPeriodeListe[1].løpendeBeløp).isEqualTo(BigDecimal.valueOf(52420)) },
            { assertThat(løpendeBidragPeriodeListe[1].samværsklasse).isNull() },
            { assertThat(løpendeBidragPeriodeListe[1].beregnetBeløp).isEqualTo(BigDecimal.valueOf(52420)) },
            { assertThat(løpendeBidragPeriodeListe[1].faktiskBeløp).isEqualTo(BigDecimal.valueOf(52420)) },
            { assertThat(løpendeBidragPeriodeListe[1].sakskategori).isEqualTo(Sakskategori.UTLAND) },

            { assertThat(løpendeBidragPeriodeListe[2].periode.fom).isEqualTo(YearMonth.of(2024, 5)) },
            { assertThat(løpendeBidragPeriodeListe[2].periode.til).isEqualTo(YearMonth.of(2024, 7)) },
            { assertThat(løpendeBidragPeriodeListe[2].saksnummer.verdi).isEqualTo(saksnummer) },
            { assertThat(løpendeBidragPeriodeListe[2].løpendeBeløp).isEqualTo(BigDecimal.valueOf(42450)) },
            { assertThat(løpendeBidragPeriodeListe[2].samværsklasse).isEqualTo(Samværsklasse.SAMVÆRSKLASSE_0) },
            { assertThat(løpendeBidragPeriodeListe[2].beregnetBeløp).isEqualTo(BigDecimal.valueOf(42450)) },
            { assertThat(løpendeBidragPeriodeListe[2].faktiskBeløp).isEqualTo(BigDecimal.valueOf(42450)) },
            { assertThat(løpendeBidragPeriodeListe[2].sakskategori).isEqualTo(Sakskategori.NASJONAL) },

            { assertThat(løpendeBidragPeriodeListe[3].periode.fom).isEqualTo(YearMonth.of(2024, 7)) },
            { assertThat(løpendeBidragPeriodeListe[3].periode.til).isEqualTo(YearMonth.of(2024, 8)) },
            { assertThat(løpendeBidragPeriodeListe[3].saksnummer.verdi).isEqualTo(saksnummer) },
            { assertThat(løpendeBidragPeriodeListe[3].løpendeBeløp).isEqualTo(BigDecimal.valueOf(42470)) },
            { assertThat(løpendeBidragPeriodeListe[3].samværsklasse).isEqualTo(Samværsklasse.SAMVÆRSKLASSE_0) },
            { assertThat(løpendeBidragPeriodeListe[3].beregnetBeløp).isEqualTo(BigDecimal.valueOf(42470)) },
            { assertThat(løpendeBidragPeriodeListe[3].faktiskBeløp).isEqualTo(BigDecimal.valueOf(42470)) },
            { assertThat(løpendeBidragPeriodeListe[3].sakskategori).isEqualTo(Sakskategori.NASJONAL) },

        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal begrense løpende bidrag til opphørsdato`() {
        val beregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 1), YearMonth.of(2025, 6))
        val bidragspliktigIdent = Personident(personIdentBidragspliktig)
        val søknadsbarn1Referanse = "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1"

        val løpendeBidragListe = listOf(
            LøpendeBidrag(
                sak = Saksnummer(saksnummer),
                type = Stønadstype.BIDRAG,
                kravhaver = Personident(personIdentSøknadsbarn1),
                mottaker = Personident(personIdentBidragsmottaker),
                periodeListe = listOf(
                    BidragPeriode(
                        periode = ÅrMånedsperiode(YearMonth.of(2024, 5), null),
                        løpendeBeløp = BigDecimal.valueOf(32450),
                        valutakode = "NOK",
                    ),
                ),
            ),
        )

        every {
            vedtakService.hentAlleStønaderForBidragspliktig(any())
        } returns LøpendeBidragPeriodeResponse(løpendeBidragListe)

        every {
            vedtakService.finnAlleManuelleVedtakForBp(
                HentManuelleVedtakRequest(
                    skyldner = Personident(personIdentBidragspliktig),
                ),
            )
        } returns listOf(
            VedtakForStønad(
                vedtaksid = 3,
                type = Vedtakstype.FASTSETTELSE,
                kilde = Vedtakskilde.MANUELT,
                vedtakstidspunkt = LocalDateTime.now().minusMonths(5),
                behandlingsreferanser =
                listOf(
                    BehandlingsreferanseDto(
                        kilde = BehandlingsrefKilde.BISYS_SØKNAD,
                        referanse = "3",
                    ),
                    BehandlingsreferanseDto(
                        kilde = BehandlingsrefKilde.BEHANDLING_ID,
                        referanse = "33",
                    ),
                ),
                kildeapplikasjon = "",
                stønadsendring =
                StønadsendringDto(
                    type = Stønadstype.BIDRAG,
                    sak = Saksnummer(saksnummer),
                    skyldner = Personident(personIdentBidragspliktig),
                    kravhaver = Personident(personIdentSøknadsbarn1),
                    mottaker = Personident(personIdentBidragsmottaker),
                    førsteIndeksreguleringsår = 0,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    beslutning = Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    eksternReferanse = "123456",
                    grunnlagReferanseListe = emptyList(),
                    sisteVedtaksid = null,
                    periodeListe = listOf(
                        VedtakPeriodeDto(
                            periode = ÅrMånedsperiode(LocalDate.parse("2024-05-01"), null),
                            beløp = BigDecimal(32450),
                            valutakode = "NOK",
                            resultatkode = "KBB",
                            delytelseId = null,
                            grunnlagReferanseListe = emptyList(),
                        ),
                    ),
                ),
            ),
        )

        every {
            vedtakService.hentVedtak(vedtaksId = 3)
        } returns hentVedtak3()

        val beregningBarnListe = listOf(
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarn1Referanse,
                periode = ÅrMånedsperiode("2024-08", "2025-06"),
                beregningsperiode = ÅrMånedsperiode("2024-09", "2025-06"),
                virkningstidspunkt = YearMonth.parse("2024-09"),
                opphørsdato = YearMonth.parse("2024-07"),
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
        )

        val bidragberegningOrkestratorRequestV2 = BidragsberegningOrkestratorRequestV2(
            beregningsperiode = beregningsperiode,
            beregningBarn = beregningBarnListe,
            grunnlagsliste = opprettGrunnlagsobjektForSøknadsbarn(
                mapOf(
                    Personident(personIdentSøknadsbarn1) to "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1",
                ),
            ) + opprettGrunnlagobjektForBidragspliktig(personIdentBidragspliktig),
        )

        val løpendeBidragOgBeregninger = hentLøpendeBidragService.hentLøpendeBidragForBehandling(
            bidragspliktigIdent,
            beregningsperiode,
            bidragberegningOrkestratorRequestV2,
        )

        val resultatGrunnlagsobjekter = løpendeBidragOgBeregninger.tilBeregnGrunnlag(
            bpReferanse = "bPReferanse",
            søknadsbarnListe = listOf(
                PersonStønad(ident = personIdentSøknadsbarn1, stønadstype = Stønadstype.BIDRAG),
            ),
            løpendeBarnFødselsdatoMap = emptyMap(),
            personConsumer = personConsumer,
            request = bidragberegningOrkestratorRequestV2,
            utlandssakerListe = emptyList(),
        ).sortedBy { it.søknadsbarnReferanse }

        val løpendeBidragPeriodeListe = resultatGrunnlagsobjekter.flatMap { it.grunnlagListe }
            .filtrerOgKonverterBasertPåEgenReferanse<LøpendeBidragPeriode>(Grunnlagstype.LØPENDE_BIDRAG_PERIODE)
            .map {
                LøpendeBidragPeriode(
                    periode = it.innhold.periode,
                    saksnummer = it.innhold.saksnummer,
                    stønadstype = it.innhold.stønadstype,
                    løpendeBeløp = it.innhold.løpendeBeløp,
                    valutakode = it.innhold.valutakode,
                    samværsklasse = it.innhold.samværsklasse,
                    beregnetBeløp = it.innhold.beregnetBeløp,
                    faktiskBeløp = it.innhold.faktiskBeløp,
                    manueltRegistrert = it.innhold.manueltRegistrert,
                )
            }

        assertAll(
            //
            { assertThat(løpendeBidragPeriodeListe[0].periode.fom).isEqualTo(YearMonth.of(2024, 5)) },
            { assertThat(løpendeBidragPeriodeListe[0].periode.til).isEqualTo(YearMonth.of(2024, 7)) },
            { assertThat(løpendeBidragPeriodeListe[0].saksnummer.verdi).isEqualTo(saksnummer) },
            { assertThat(løpendeBidragPeriodeListe[0].løpendeBeløp).isEqualTo(BigDecimal.valueOf(32450)) },
            { assertThat(løpendeBidragPeriodeListe[0].samværsklasse).isEqualTo(Samværsklasse.SAMVÆRSKLASSE_0) },
            { assertThat(løpendeBidragPeriodeListe[0].beregnetBeløp).isEqualTo(BigDecimal.valueOf(32450)) },
            { assertThat(løpendeBidragPeriodeListe[0].faktiskBeløp).isEqualTo(BigDecimal.valueOf(32450)) },

        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal ikke begrense løpende bidrag til opphørsdato når den er etter beregningsperiodens start`() {
        val beregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 6), YearMonth.of(2025, 6))
        val bidragspliktigIdent = Personident(personIdentBidragspliktig)
        val søknadsbarn1Referanse = "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1"

        val løpendeBidragListe = listOf(
            LøpendeBidrag(
                sak = Saksnummer(saksnummer),
                type = Stønadstype.BIDRAG,
                kravhaver = Personident(personIdentSøknadsbarn1),
                mottaker = Personident(personIdentBidragsmottaker),
                periodeListe = listOf(
                    BidragPeriode(
                        periode = ÅrMånedsperiode(YearMonth.of(2024, 5), null),
                        løpendeBeløp = BigDecimal.valueOf(32450),
                        valutakode = "NOK",
                    ),
                ),
            ),
        )

        every {
            vedtakService.hentAlleStønaderForBidragspliktig(any())
        } returns LøpendeBidragPeriodeResponse(løpendeBidragListe)

        every {
            vedtakService.finnAlleManuelleVedtakForBp(
                HentManuelleVedtakRequest(
                    skyldner = Personident(personIdentBidragspliktig),
                ),
            )
        } returns listOf(
            VedtakForStønad(
                vedtaksid = 3,
                type = Vedtakstype.FASTSETTELSE,
                kilde = Vedtakskilde.MANUELT,
                vedtakstidspunkt = LocalDateTime.now().minusMonths(5),
                behandlingsreferanser =
                listOf(
                    BehandlingsreferanseDto(
                        kilde = BehandlingsrefKilde.BISYS_SØKNAD,
                        referanse = "3",
                    ),
                    BehandlingsreferanseDto(
                        kilde = BehandlingsrefKilde.BEHANDLING_ID,
                        referanse = "33",
                    ),
                ),
                kildeapplikasjon = "",
                stønadsendring =
                StønadsendringDto(
                    type = Stønadstype.BIDRAG,
                    sak = Saksnummer(saksnummer),
                    skyldner = Personident(personIdentBidragspliktig),
                    kravhaver = Personident(personIdentSøknadsbarn1),
                    mottaker = Personident(personIdentBidragsmottaker),
                    førsteIndeksreguleringsår = 0,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    beslutning = Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    eksternReferanse = "123456",
                    grunnlagReferanseListe = emptyList(),
                    sisteVedtaksid = null,
                    periodeListe = listOf(
                        VedtakPeriodeDto(
                            periode = ÅrMånedsperiode(LocalDate.parse("2024-05-01"), null),
                            beløp = BigDecimal(32450),
                            valutakode = "NOK",
                            resultatkode = "KBB",
                            delytelseId = null,
                            grunnlagReferanseListe = emptyList(),
                        ),
                    ),
                ),
            ),
        )

        every {
            vedtakService.hentVedtak(vedtaksId = 3)
        } returns hentVedtak3()

        val beregningBarnListe = listOf(
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarn1Referanse,
                periode = ÅrMånedsperiode("2024-06", "2025-06"),
                beregningsperiode = ÅrMånedsperiode("2024-08", "2025-06"),
                virkningstidspunkt = YearMonth.parse("2024-12"),
                opphørsdato = YearMonth.parse("2024-12"),
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
        )

        val bidragberegningOrkestratorRequestV2 = BidragsberegningOrkestratorRequestV2(
            beregningsperiode = beregningsperiode,
            beregningBarn = beregningBarnListe,
            grunnlagsliste = opprettGrunnlagsobjektForSøknadsbarn(
                mapOf(
                    Personident(personIdentSøknadsbarn1) to "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1",
                ),
            ) + opprettGrunnlagobjektForBidragspliktig(personIdentBidragspliktig),
        )

        val løpendeBidragOgBeregninger = hentLøpendeBidragService.hentLøpendeBidragForBehandling(
            bidragspliktigIdent,
            beregningsperiode,
            bidragberegningOrkestratorRequestV2,
        )

        val resultatGrunnlagsobjekter = løpendeBidragOgBeregninger.tilBeregnGrunnlag(
            bpReferanse = "bPReferanse",
            søknadsbarnListe = listOf(
                PersonStønad(ident = personIdentSøknadsbarn1, stønadstype = Stønadstype.BIDRAG),
            ),
            løpendeBarnFødselsdatoMap = emptyMap(),
            personConsumer = personConsumer,
            request = bidragberegningOrkestratorRequestV2,
            utlandssakerListe = emptyList(),
        ).sortedBy { it.søknadsbarnReferanse }

        val løpendeBidragPeriodeListe = resultatGrunnlagsobjekter.flatMap { it.grunnlagListe }
            .filtrerOgKonverterBasertPåEgenReferanse<LøpendeBidragPeriode>(Grunnlagstype.LØPENDE_BIDRAG_PERIODE)
            .map {
                LøpendeBidragPeriode(
                    periode = it.innhold.periode,
                    saksnummer = it.innhold.saksnummer,
                    stønadstype = it.innhold.stønadstype,
                    løpendeBeløp = it.innhold.løpendeBeløp,
                    valutakode = it.innhold.valutakode,
                    samværsklasse = it.innhold.samværsklasse,
                    beregnetBeløp = it.innhold.beregnetBeløp,
                    faktiskBeløp = it.innhold.faktiskBeløp,
                    manueltRegistrert = it.innhold.manueltRegistrert,
                )
            }

        assertAll(
            //
            { assertThat(løpendeBidragPeriodeListe[0].periode.fom).isEqualTo(YearMonth.of(2024, 6)) },
            { assertThat(løpendeBidragPeriodeListe[0].periode.til).isEqualTo(YearMonth.of(2024, 8)) },
            { assertThat(løpendeBidragPeriodeListe[0].saksnummer.verdi).isEqualTo(saksnummer) },
            { assertThat(løpendeBidragPeriodeListe[0].løpendeBeløp).isEqualTo(BigDecimal.valueOf(32450)) },
            { assertThat(løpendeBidragPeriodeListe[0].samværsklasse).isEqualTo(Samværsklasse.SAMVÆRSKLASSE_0) },
            { assertThat(løpendeBidragPeriodeListe[0].beregnetBeløp).isEqualTo(BigDecimal.valueOf(32450)) },
            { assertThat(løpendeBidragPeriodeListe[0].faktiskBeløp).isEqualTo(BigDecimal.valueOf(32450)) },

        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal ikke hente løpende bidrag for barn med beregningsperiodeBarn_fom lik beregningsperiode_fom for alle barn `() {
        val beregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 7), YearMonth.of(2025, 6))
        val bidragspliktigIdent = Personident(personIdentBidragspliktig)
        val søknadsbarn1Referanse = "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1"
        val annetBarnReferanse = "person_innhentet_barn_$personIdentAnnetbarn"

        val løpendeBidragListe = listOf(
            LøpendeBidrag(
                sak = Saksnummer(saksnummer),
                type = Stønadstype.BIDRAG,
                kravhaver = Personident(personIdentSøknadsbarn1),
                mottaker = Personident(personIdentBidragsmottaker),
                periodeListe = listOf(
                    BidragPeriode(
                        periode = ÅrMånedsperiode(YearMonth.of(2024, 5), null),
                        løpendeBeløp = BigDecimal.valueOf(1111),
                        valutakode = "NOK",
                    ),
                ),
            ),
            LøpendeBidrag(
                sak = Saksnummer(saksnummer),
                type = Stønadstype.BIDRAG,
                kravhaver = Personident(personIdentAnnetbarn),
                mottaker = Personident(personIdentBidragsmottaker),
                periodeListe = listOf(
                    BidragPeriode(
                        periode = ÅrMånedsperiode(YearMonth.of(2024, 5), null),
                        løpendeBeløp = BigDecimal.valueOf(2222),
                        valutakode = "NOK",
                    ),
                ),
            ),
        )

        every {
            vedtakService.hentAlleStønaderForBidragspliktig(any())
        } returns LøpendeBidragPeriodeResponse(løpendeBidragListe)

        every {
            vedtakService.finnAlleManuelleVedtakForBp(
                HentManuelleVedtakRequest(
                    skyldner = Personident(personIdentBidragspliktig),
                ),
            )
        } returns listOf(
            VedtakForStønad(
                vedtaksid = 3,
                type = Vedtakstype.FASTSETTELSE,
                kilde = Vedtakskilde.MANUELT,
                vedtakstidspunkt = LocalDateTime.now().minusMonths(5),
                behandlingsreferanser =
                listOf(
                    BehandlingsreferanseDto(
                        kilde = BehandlingsrefKilde.BISYS_SØKNAD,
                        referanse = "3",
                    ),
                    BehandlingsreferanseDto(
                        kilde = BehandlingsrefKilde.BEHANDLING_ID,
                        referanse = "33",
                    ),
                ),
                kildeapplikasjon = "",
                stønadsendring =
                StønadsendringDto(
                    type = Stønadstype.BIDRAG,
                    sak = Saksnummer(saksnummer),
                    skyldner = Personident(personIdentBidragspliktig),
                    kravhaver = Personident(personIdentSøknadsbarn1),
                    mottaker = Personident(personIdentBidragsmottaker),
                    førsteIndeksreguleringsår = 0,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    beslutning = Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    eksternReferanse = "123456",
                    grunnlagReferanseListe = emptyList(),
                    sisteVedtaksid = null,
                    periodeListe = listOf(
                        VedtakPeriodeDto(
                            periode = ÅrMånedsperiode(LocalDate.parse("2024-05-01"), null),
                            beløp = BigDecimal(1111),
                            valutakode = "NOK",
                            resultatkode = "KBB",
                            delytelseId = null,
                            grunnlagReferanseListe = emptyList(),
                        ),
                    ),
                ),
            ),
        )

        every {
            vedtakService.finnAlleManuelleVedtakForBp(
                HentManuelleVedtakRequest(
                    skyldner = Personident(personIdentBidragspliktig),
                ),
            )
        } returns listOf(
            VedtakForStønad(
                vedtaksid = 5,
                type = Vedtakstype.FASTSETTELSE,
                kilde = Vedtakskilde.MANUELT,
                vedtakstidspunkt = LocalDateTime.now().minusMonths(5),
                behandlingsreferanser =
                listOf(
                    BehandlingsreferanseDto(
                        kilde = BehandlingsrefKilde.BISYS_SØKNAD,
                        referanse = "5",
                    ),
                    BehandlingsreferanseDto(
                        kilde = BehandlingsrefKilde.BEHANDLING_ID,
                        referanse = "55",
                    ),
                ),
                kildeapplikasjon = "",
                stønadsendring =
                StønadsendringDto(
                    type = Stønadstype.BIDRAG,
                    sak = Saksnummer(saksnummer),
                    skyldner = Personident(personIdentBidragspliktig),
                    kravhaver = Personident(personIdentAnnetbarn),
                    mottaker = Personident(personIdentBidragsmottaker),
                    førsteIndeksreguleringsår = 0,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    beslutning = Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    eksternReferanse = "123456",
                    grunnlagReferanseListe = emptyList(),
                    sisteVedtaksid = null,
                    periodeListe = listOf(
                        VedtakPeriodeDto(
                            periode = ÅrMånedsperiode(LocalDate.parse("2024-05-01"), null),
                            beløp = BigDecimal(2222),
                            valutakode = "NOK",
                            resultatkode = "KBB",
                            delytelseId = null,
                            grunnlagReferanseListe = emptyList(),
                        ),
                    ),
                ),
            ),
        )

        every {
            vedtakService.hentVedtak(vedtaksId = 3)
        } returns hentVedtak3()

        every {
            vedtakService.hentVedtak(vedtaksId = 5)
        } returns hentVedtak5()

        val beregningBarnListe = listOf(
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarn1Referanse,
                periode = ÅrMånedsperiode("2024-07", "2025-06"),
                beregningsperiode = ÅrMånedsperiode("2024-07", "2025-06"),
                virkningstidspunkt = YearMonth.parse("2024-07"),
                opphørsdato = null,
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
            BeregningGrunnlagV2(
                søknadsbarnreferanse = annetBarnReferanse,
                periode = ÅrMånedsperiode("2024-12", "2025-06"),
                beregningsperiode = ÅrMånedsperiode("2024-12", "2025-06"),
                virkningstidspunkt = YearMonth.parse("2024-12"),
                opphørsdato = null,
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
        )

        val bidragberegningOrkestratorRequestV2 = BidragsberegningOrkestratorRequestV2(
            beregningsperiode = beregningsperiode,
            beregningBarn = beregningBarnListe,
            grunnlagsliste = opprettGrunnlagsobjektForSøknadsbarn(
                mapOf(
                    Personident(personIdentSøknadsbarn1) to "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1",
                ),
            ) + opprettGrunnlagobjektForBidragspliktig(personIdentBidragspliktig),
        )

        val løpendeBidragOgBeregninger = hentLøpendeBidragService.hentLøpendeBidragForBehandling(
            bidragspliktigIdent,
            beregningsperiode,
            bidragberegningOrkestratorRequestV2,
        )

        assertAll(
            { assertThat(løpendeBidragOgBeregninger.løpendeBidragListe).hasSize(1) },
            { assertThat(løpendeBidragOgBeregninger.løpendeBidragListe.first().kravhaver.verdi).isEqualTo(personIdentAnnetbarn) },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal håndtere løpende bidrag uten beregningsdata, utland og oppfostring`() {
        val beregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 5), YearMonth.of(2025, 6))
        val bidragspliktigIdent = Personident(personIdentBidragspliktig)
        val søknadsbarn1Referanse = "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1"

        val løpendeBidragListe = opprettLøpendeOppfostringsOgUtlandsBidrag()

        every {
            vedtakService.hentAlleStønaderForBidragspliktig(any())
        } returns LøpendeBidragPeriodeResponse(løpendeBidragListe)

        every {
            vedtakService.finnAlleManuelleVedtakForBp(
                HentManuelleVedtakRequest(
                    skyldner = Personident(personIdentBidragspliktig),
                ),
            )
        } returns opprettVedtakOppfostringBarn1() + opprettVedtakUtlandBarn2()

        every {
            vedtakService.hentAlleBeregningerFraBBM(any())
        } returns BidragBeregningResponsDto(emptyList())

        val beregningBarnListe = listOf(
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarn1Referanse,
                periode = ÅrMånedsperiode("2024-02", "2025-06"),
                beregningsperiode = ÅrMånedsperiode("2024-02", "2025-06"),
                virkningstidspunkt = YearMonth.parse("2024-02"),
                opphørsdato = null,
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
        )

        val bidragberegningOrkestratorRequestV2 = BidragsberegningOrkestratorRequestV2(
            beregningsperiode = beregningsperiode,
            beregningBarn = beregningBarnListe,
            grunnlagsliste = opprettGrunnlagsobjektForSøknadsbarn(
                mapOf(
                    Personident(personIdentSøknadsbarn1) to "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1",
                ),
            ) + opprettGrunnlagobjektForBidragspliktig(personIdentBidragspliktig),
        )

        val løpendeBidragOgBeregninger = hentLøpendeBidragService.hentLøpendeBidragForBehandling(
            bidragspliktigIdent,
            beregningsperiode,
            bidragberegningOrkestratorRequestV2,
        )

        val resultatGrunnlagsobjekter = løpendeBidragOgBeregninger.tilBeregnGrunnlag(
            bpReferanse = "bPReferanse",
            søknadsbarnListe = listOf(
                PersonStønad(ident = personIdentSøknadsbarn1, stønadstype = Stønadstype.BIDRAG),
            ),
            løpendeBarnFødselsdatoMap = mapOf(
                Personident(personIdentAnnetbarn) to LocalDate.now().minusYears(10),
                Personident(personIdentAnnetbarn2) to LocalDate.now().minusYears(17),
            ),
            personConsumer = personConsumer,
            request = bidragberegningOrkestratorRequestV2,
            utlandssakerListe = listOf(saksnummer3),
        ).sortedBy { it.søknadsbarnReferanse }

        val løpendeBidragPeriodeListe = resultatGrunnlagsobjekter.flatMap { it.grunnlagListe }
            .filtrerOgKonverterBasertPåEgenReferanse<LøpendeBidragPeriode>(Grunnlagstype.LØPENDE_BIDRAG_PERIODE)
            .map {
                LøpendeBidragPeriode(
                    periode = it.innhold.periode,
                    saksnummer = it.innhold.saksnummer,
                    stønadstype = it.innhold.stønadstype,
                    løpendeBeløp = it.innhold.løpendeBeløp,
                    valutakode = it.innhold.valutakode,
                    samværsklasse = it.innhold.samværsklasse,
                    beregnetBeløp = it.innhold.beregnetBeløp,
                    faktiskBeløp = it.innhold.faktiskBeløp,
                    sakskategori = it.innhold.sakskategori,
                    manueltRegistrert = it.innhold.manueltRegistrert,
                )
            }.sortedBy { it.saksnummer }

        assertAll(
            //
            { assertThat(løpendeBidragPeriodeListe[0].periode.fom).isEqualTo(YearMonth.of(2024, 5)) },
            { assertThat(løpendeBidragPeriodeListe[0].periode.til).isEqualTo(YearMonth.of(2025, 6)) },
            { assertThat(løpendeBidragPeriodeListe[0].saksnummer.verdi).isEqualTo(saksnummer2) },
            { assertThat(løpendeBidragPeriodeListe[0].løpendeBeløp).isEqualTo(BigDecimal.valueOf(1000)) },
            { assertThat(løpendeBidragPeriodeListe[0].samværsklasse).isNull() },
            { assertThat(løpendeBidragPeriodeListe[0].beregnetBeløp).isEqualTo(BigDecimal.valueOf(1000)) },
            { assertThat(løpendeBidragPeriodeListe[0].faktiskBeløp).isEqualTo(BigDecimal.valueOf(1000)) },
            { assertThat(løpendeBidragPeriodeListe[0].sakskategori).isEqualTo(Sakskategori.NASJONAL) },

            { assertThat(løpendeBidragPeriodeListe[1].periode.fom).isEqualTo(YearMonth.of(2024, 5)) },
            { assertThat(løpendeBidragPeriodeListe[1].periode.til).isEqualTo(YearMonth.of(2025, 6)) },
            { assertThat(løpendeBidragPeriodeListe[1].saksnummer.verdi).isEqualTo(saksnummer3) },
            { assertThat(løpendeBidragPeriodeListe[1].løpendeBeløp).isEqualTo(BigDecimal.valueOf(2000)) },
            { assertThat(løpendeBidragPeriodeListe[1].samværsklasse).isNull() },
            { assertThat(løpendeBidragPeriodeListe[1].beregnetBeløp).isEqualTo(BigDecimal.valueOf(2000)) },
            { assertThat(løpendeBidragPeriodeListe[1].faktiskBeløp).isEqualTo(BigDecimal.valueOf(2000)) },
            { assertThat(løpendeBidragPeriodeListe[1].sakskategori).isEqualTo(Sakskategori.UTLAND) },

        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal hente filtrere bort manuelle vedtak for stønader som ikke er funnet som løpende bidrag`() {
        val beregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 7), YearMonth.of(2025, 6))
        val bidragspliktigIdent = Personident(personIdentBidragspliktig)

        val bidragPeriode = BidragPeriode(
            periode = ÅrMånedsperiode(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 12, 1)),
            løpendeBeløp = BigDecimal.valueOf(5160),
            valutakode = "NOK",
        )

        val løpendeBidrag = LøpendeBidrag(
            sak = Saksnummer(saksnummer),
            type = Stønadstype.BIDRAG,
            kravhaver = Personident(personIdentSøknadsbarn1),
            mottaker = Personident(personIdentBidragsmottaker),
            periodeListe = listOf(bidragPeriode),
        )

        val vedtakForStønad = opprettVedtakForStønad(
            kravhaver = personIdentSøknadsbarn1,
            stønadstype = Stønadstype.BIDRAG,
        )

        // Vedtak for stønad som skal ignoreres. Er ikke løpende bidrag.
        val vedtakForStønad2 = opprettVedtakForStønad(
            kravhaver = personIdentSøknadsbarn2,
            stønadstype = Stønadstype.BIDRAG,
        )

        val beregningRespons = opprettBidragBeregningResponsDto(
            kravhaver = personIdentSøknadsbarn1,
            sak = saksnummer,
            beregnetBeløp = BigDecimal.valueOf(5160),
            faktiskBeløp = BigDecimal.valueOf(4500),
        )

        every {
            vedtakService.hentAlleStønaderForBidragspliktig(any())
        } returns LøpendeBidragPeriodeResponse(listOf(løpendeBidrag))

        every {
            vedtakService.finnAlleManuelleVedtakForBp(any())
        } returns listOf(vedtakForStønad, vedtakForStønad2)

        every {
            vedtakService.hentAlleBeregningerFraBBM(any())
        } returns BidragBeregningResponsDto(beregningRespons)

        val søknadsbarn1Referanse = "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1"

        val beregningBarnListe = listOf(
            BeregningGrunnlagV2(
                søknadsbarnreferanse = søknadsbarn1Referanse,
                periode = ÅrMånedsperiode("2024-09", "2025-06"),
                beregningsperiode = ÅrMånedsperiode("2024-09", "2025-06"),
                virkningstidspunkt = YearMonth.parse("2024-09"),
                opphørsdato = null,
                stønadstype = Stønadstype.BIDRAG,
                erDirekteAvslag = false,
                omgjøringOrkestratorGrunnlag = null,
            ),
        )

        val bidragberegningOrkestratorRequestV2 = BidragsberegningOrkestratorRequestV2(
            beregningsperiode = beregningsperiode,
            beregningBarn = beregningBarnListe,
            grunnlagsliste = opprettGrunnlagsobjektForSøknadsbarn(
                mapOf(
                    Personident(personIdentSøknadsbarn1) to "person_PERSON_SØKNADSBARN_$personIdentSøknadsbarn1",
                ),
            ) + opprettGrunnlagobjektForBidragspliktig(personIdentBidragspliktig),
        )

        val resultat = hentLøpendeBidragService.hentLøpendeBidragForBehandling(
            bidragspliktigIdent,
            beregningsperiode,
            bidragberegningOrkestratorRequestV2,
        )

        assertAll(
            { assertThat(resultat).isNotNull() },
            { assertThat(resultat.løpendeBidragListe).hasSize(1) },
            { assertThat(resultat.beregnetBeløpListe.beregningListe).hasSize(1) },
            { assertThat(resultat.beregnetBeløpListe.beregningListe[0].beregnetBeløp).isEqualTo(BigDecimal.valueOf(5160)) },
        )

        verify { vedtakService.hentAlleBeregningerFraBBM(any()) }
    }
}

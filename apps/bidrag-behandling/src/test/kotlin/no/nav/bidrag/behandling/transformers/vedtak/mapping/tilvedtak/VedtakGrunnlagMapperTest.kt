package no.nav.bidrag.behandling.transformers.vedtak.mapping.tilvedtak

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.behandling.consumer.BidragPersonConsumer
import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.database.datamodell.GebyrRolle
import no.nav.bidrag.behandling.database.datamodell.GrunnlagFraVedtak
import no.nav.bidrag.behandling.database.datamodell.Inntekt
import no.nav.bidrag.behandling.database.datamodell.Rolle
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordeling
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordelingRolle
import no.nav.bidrag.behandling.dto.v2.behandling.Grunnlagsdatatype
import no.nav.bidrag.behandling.dto.v2.behandling.LesemodusVedtak
import no.nav.bidrag.behandling.service.BarnebidragGrunnlagInnhenting
import no.nav.bidrag.behandling.service.BeregningEvnevurderingService
import no.nav.bidrag.behandling.service.PersonService
import no.nav.bidrag.behandling.transformers.beregning.ValiderBeregning
import no.nav.bidrag.behandling.transformers.filtrerOgJusterFraVirkningstidspunkt
import no.nav.bidrag.behandling.utils.stubPersonConsumer
import no.nav.bidrag.behandling.utils.testdata.TestDataPerson
import no.nav.bidrag.behandling.utils.testdata.leggTilGrunnlagBeløpshistorikk
import no.nav.bidrag.behandling.utils.testdata.leggTilPrivatAvtale
import no.nav.bidrag.behandling.utils.testdata.opprettGyldigBehandlingForBeregningOgVedtak
import no.nav.bidrag.behandling.utils.testdata.opprettPrivatAvtale
import no.nav.bidrag.behandling.utils.testdata.opprettRolle
import no.nav.bidrag.behandling.utils.testdata.opprettSakForBehandling
import no.nav.bidrag.behandling.utils.testdata.opprettStønadPeriodeDto
import no.nav.bidrag.behandling.utils.testdata.testdataBM
import no.nav.bidrag.behandling.utils.testdata.testdataBarn1
import no.nav.bidrag.behandling.utils.testdata.testdataBarn2
import no.nav.bidrag.behandling.utils.validerHarGrunnlag
import no.nav.bidrag.behandling.utils.validerHarReferanseTilSjablon
import no.nav.bidrag.beregn.barnebidrag.BeregnGebyrApi
import no.nav.bidrag.beregn.barnebidrag.BeregnSamværsklasseApi
import no.nav.bidrag.commons.web.mock.stubSjablonProvider
import no.nav.bidrag.commons.web.mock.stubSjablonService
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.diverse.Kilde
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.privatavtale.PrivatAvtaleType
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sjablon.SjablonTallNavn
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakPeriodeDto
import no.nav.bidrag.transport.felles.toYearMonth
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
class VedtakGrunnlagMapperTest {
    lateinit var vedtakGrunnlagMapper: VedtakGrunnlagMapper

    lateinit var personConsumer: BidragPersonConsumer

    @MockK
    lateinit var barnebidragGrunnlagInnhenting: BarnebidragGrunnlagInnhenting

    @MockK
    lateinit var evnevurderingService: BeregningEvnevurderingService

    @BeforeEach
    fun init() {
        personConsumer = stubPersonConsumer()
        val personService = PersonService(personConsumer)
        stubSjablonProvider()
        val behandlingTilGrunnlagMappingV2 = BehandlingTilGrunnlagMappingV2(personService, BeregnSamværsklasseApi(stubSjablonService()))
        vedtakGrunnlagMapper =
            VedtakGrunnlagMapper(
                behandlingTilGrunnlagMappingV2,
                ValiderBeregning(),
                evnevurderingService,
                barnebidragGrunnlagInnhenting,
                personService,
                BeregnGebyrApi(stubSjablonService()),
            )
    }

    @Test
    fun `skal finne riktig rolle i sak når identoppslaget gir 403`() {
        val sak = opprettSakForBehandling(opprettGyldigBehandlingForBeregningOgVedtak(true, typeBehandling = TypeBehandling.BIDRAG))
        val søktRolle = sak.roller.last()
        every { personConsumer.hentPerson(any<String>()) } throws HttpClientErrorException(HttpStatus.FORBIDDEN)

        val rolle = with(vedtakGrunnlagMapper) { sak.hentRolleMedFnr(søktRolle.fødselsnummer!!.verdi) }

        sak.roller shouldHaveAtLeastSize 2
        rolle shouldBe søktRolle
    }

    @Test
    fun `skal beregne gebyr`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(true, typeBehandling = TypeBehandling.BIDRAG)
        behandling.inntekter =
            mutableSetOf(
                Inntekt(
                    belop = BigDecimal(2000),
                    datoFom = behandling.virkningstidspunkt,
                    datoTom = null,
                    rolle = behandling.bidragsmottaker!!,
                    taMed = true,
                    gjelderBarn = behandling.søknadsbarn.first().ident,
                    kilde = Kilde.MANUELL,
                    behandling = behandling,
                    type = Inntektsrapportering.BARNETILLEGG,
                    id = 1,
                ),
                Inntekt(
                    belop = BigDecimal(900000),
                    datoFom = behandling.virkningstidspunkt,
                    datoTom = null,
                    rolle = behandling.bidragsmottaker!!,
                    taMed = true,
                    gjelderBarn = behandling.søknadsbarn.first().ident,
                    kilde = Kilde.MANUELL,
                    behandling = behandling,
                    type = Inntektsrapportering.AINNTEKT_BEREGNET_12MND,
                    id = 1,
                ),
            )

        val grunnlag =
            with(vedtakGrunnlagMapper) {
                behandling.beregnetInntekterGrunnlagForRolle(behandling.bidragsmottaker!!, false)
            }
        grunnlag.filter { it.type == Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE } shouldHaveSize 2

        val resultat = vedtakGrunnlagMapper.beregnGebyr(behandling, behandling.bidragsmottaker!!)
        assertSoftly(resultat) {
            ilagtGebyr shouldBe true
            skattepliktigInntekt shouldBe BigDecimal(900000)
            maksBarnetillegg shouldBe BigDecimal(2000)
            beløpGebyrsats shouldBe BigDecimal(1345)
            resultatkode shouldBe Resultatkode.GEBYR_ILAGT
            grunnlagsreferanseListeEngangsbeløp shouldHaveSize 2
            grunnlagsreferanseListeEngangsbeløp shouldContain grunnlagsliste.find { it.type == Grunnlagstype.SLUTTBEREGNING_GEBYR }!!.referanse
            grunnlagsliste shouldHaveSize 7
            grunnlagsliste.validerHarGrunnlag(Grunnlagstype.SLUTTBEREGNING_GEBYR)
            grunnlagsliste.validerHarGrunnlag(Grunnlagstype.SJABLON_SJABLONTALL, antall = 2)
            grunnlagsliste.validerHarReferanseTilSjablon(SjablonTallNavn.NEDRE_INNTEKTSGRENSE_GEBYR_BELØP)
            grunnlagsliste.validerHarReferanseTilSjablon(SjablonTallNavn.FASTSETTELSESGEBYR_BELØP)
            grunnlagsliste.validerHarGrunnlag(Grunnlagstype.DELBEREGNING_INNTEKTSBASERT_GEBYR, antall = 1)
            grunnlagsliste.validerHarGrunnlag(Grunnlagstype.DELBEREGNING_SUM_INNTEKT, antall = 1)
        }
    }

    @Test
    fun `skal beregne gebyr ved avslag`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(true, typeBehandling = TypeBehandling.BIDRAG)
        behandling.avslag = Resultatkode.AVSLAG
        behandling.inntekter =
            mutableSetOf(
                Inntekt(
                    belop = BigDecimal(2000),
                    datoFom = behandling.virkningstidspunkt,
                    datoTom = null,
                    rolle = behandling.bidragsmottaker!!,
                    taMed = true,
                    gjelderBarn = behandling.søknadsbarn.first().ident,
                    kilde = Kilde.MANUELL,
                    behandling = behandling,
                    type = Inntektsrapportering.BARNETILLEGG,
                    id = 1,
                ),
                Inntekt(
                    belop = BigDecimal(900000),
                    datoFom = behandling.virkningstidspunkt,
                    datoTom = null,
                    rolle = behandling.bidragsmottaker!!,
                    taMed = true,
                    gjelderBarn = behandling.søknadsbarn.first().ident,
                    kilde = Kilde.MANUELL,
                    behandling = behandling,
                    type = Inntektsrapportering.AINNTEKT_BEREGNET_12MND,
                    id = 1,
                ),
            )

        val resultat = vedtakGrunnlagMapper.beregnGebyr(behandling, behandling.bidragsmottaker!!)
        assertSoftly(resultat) {
            ilagtGebyr shouldBe true
            skattepliktigInntekt shouldBe BigDecimal(900000)
            maksBarnetillegg shouldBe BigDecimal(2000)
            beløpGebyrsats shouldBe BigDecimal(1345)
            resultatkode shouldBe Resultatkode.GEBYR_ILAGT
            grunnlagsreferanseListeEngangsbeløp shouldHaveSize 2
            grunnlagsreferanseListeEngangsbeløp shouldContain grunnlagsliste.find { it.type == Grunnlagstype.SLUTTBEREGNING_GEBYR }!!.referanse
            grunnlagsliste shouldHaveSize 7
            grunnlagsliste.validerHarGrunnlag(Grunnlagstype.SLUTTBEREGNING_GEBYR)
            grunnlagsliste.validerHarGrunnlag(Grunnlagstype.SJABLON_SJABLONTALL, antall = 2)
            grunnlagsliste.validerHarReferanseTilSjablon(SjablonTallNavn.NEDRE_INNTEKTSGRENSE_GEBYR_BELØP)
            grunnlagsliste.validerHarReferanseTilSjablon(SjablonTallNavn.FASTSETTELSESGEBYR_BELØP)
            grunnlagsliste.validerHarGrunnlag(Grunnlagstype.DELBEREGNING_INNTEKTSBASERT_GEBYR, antall = 1)
            grunnlagsliste.validerHarGrunnlag(Grunnlagstype.DELBEREGNING_SUM_INNTEKT, antall = 1)
        }
    }

    @Test
    fun `skal beregne gebyr ved avslag når manuelt gebyr er lagt inn`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(true, typeBehandling = TypeBehandling.BIDRAG)
        behandling.avslag = Resultatkode.AVSLAG
        behandling.inntekter =
            mutableSetOf(
                Inntekt(
                    belop = BigDecimal(2000),
                    datoFom = behandling.virkningstidspunkt,
                    datoTom = null,
                    rolle = behandling.bidragsmottaker!!,
                    taMed = true,
                    gjelderBarn = behandling.søknadsbarn.first().ident,
                    kilde = Kilde.MANUELL,
                    behandling = behandling,
                    type = Inntektsrapportering.BARNETILLEGG,
                    id = 1,
                ),
                Inntekt(
                    belop = BigDecimal(900000),
                    datoFom = behandling.virkningstidspunkt,
                    datoTom = null,
                    rolle = behandling.bidragsmottaker!!,
                    taMed = true,
                    gjelderBarn = behandling.søknadsbarn.first().ident,
                    kilde = Kilde.MANUELL,
                    behandling = behandling,
                    type = Inntektsrapportering.AINNTEKT_BEREGNET_12MND,
                    id = 1,
                ),
            )

        behandling.bidragsmottaker!!.gebyr = GebyrRolle(overstyrGebyr = true, ilagtGebyr = true, begrunnelse = "test")
        val resultat = vedtakGrunnlagMapper.beregnGebyr(behandling, behandling.bidragsmottaker!!)
        assertSoftly(resultat) {
            ilagtGebyr shouldBe true
            skattepliktigInntekt shouldBe BigDecimal(900000)
            maksBarnetillegg shouldBe BigDecimal(2000)
            beløpGebyrsats shouldBe BigDecimal(1345)
            resultatkode shouldBe Resultatkode.GEBYR_ILAGT
            grunnlagsreferanseListeEngangsbeløp shouldHaveSize 2
            grunnlagsreferanseListeEngangsbeløp shouldContain grunnlagsliste.find { it.type == Grunnlagstype.SLUTTBEREGNING_GEBYR }!!.referanse
            grunnlagsliste shouldHaveSize 7
            grunnlagsliste.validerHarGrunnlag(Grunnlagstype.SLUTTBEREGNING_GEBYR)
            grunnlagsliste.validerHarGrunnlag(Grunnlagstype.SJABLON_SJABLONTALL, antall = 2)
            grunnlagsliste.validerHarReferanseTilSjablon(SjablonTallNavn.NEDRE_INNTEKTSGRENSE_GEBYR_BELØP)
            grunnlagsliste.validerHarReferanseTilSjablon(SjablonTallNavn.FASTSETTELSESGEBYR_BELØP)
            grunnlagsliste.validerHarGrunnlag(Grunnlagstype.DELBEREGNING_INNTEKTSBASERT_GEBYR, antall = 1)
            grunnlagsliste.validerHarGrunnlag(Grunnlagstype.DELBEREGNING_SUM_INNTEKT, antall = 1)
        }
    }

    @Test
    fun `skal returnere false når perioder dekker hele intervallet uten hull`() {
        val perioder =
            listOf(
                ÅrMånedsperiode(YearMonth.of(2025, 1), YearMonth.of(2025, 4)),
                ÅrMånedsperiode(YearMonth.of(2025, 4), YearMonth.of(2025, 7)),
            )

        val harHull = perioder.inneholderPerioderUtenInnkreving(YearMonth.of(2025, 1), YearMonth.of(2025, 6))

        harHull shouldBe false
    }

    @Test
    fun `skal returnere true når perioder har hull i intervallet`() {
        val perioder =
            listOf(
                ÅrMånedsperiode(YearMonth.of(2025, 1), YearMonth.of(2025, 3)),
                ÅrMånedsperiode(YearMonth.of(2025, 4), YearMonth.of(2025, 7)),
            )

        val harHull = perioder.inneholderPerioderUtenInnkreving(YearMonth.of(2025, 1), YearMonth.of(2025, 6))

        harHull shouldBe true
    }

    @Test
    fun `skal returnere true når perioder ikke dekker frem til beregnTil`() {
        val perioder =
            listOf(
                ÅrMånedsperiode(YearMonth.of(2025, 1), YearMonth.of(2025, 3)),
            )

        val harHull = perioder.inneholderPerioderUtenInnkreving(YearMonth.of(2025, 1), YearMonth.of(2025, 6))

        harHull shouldBe true
    }

    @Test
    fun `skal slå sammen perioder som er rett etter hverandre`() {
        val perioder =
            listOf(
                ÅrMånedsperiode(YearMonth.of(2025, 4), YearMonth.of(2025, 6)),
                ÅrMånedsperiode(YearMonth.of(2025, 1), YearMonth.of(2025, 4)),
                ÅrMånedsperiode(YearMonth.of(2025, 6), YearMonth.of(2025, 8)),
            )

        val sammenslåttePerioder = perioder.slåSammenEtterfølgendePerioder()

        sammenslåttePerioder shouldBe listOf(ÅrMånedsperiode(YearMonth.of(2025, 1), YearMonth.of(2025, 8)))
    }

    @Test
    fun `skal ikke slå sammen perioder når det er hull mellom dem`() {
        val perioder =
            listOf(
                ÅrMånedsperiode(YearMonth.of(2025, 1), YearMonth.of(2025, 3)),
                ÅrMånedsperiode(YearMonth.of(2025, 4), YearMonth.of(2025, 6)),
            )

        val sammenslåttePerioder = perioder.slåSammenEtterfølgendePerioder()

        sammenslåttePerioder shouldBe perioder
    }

    @Test
    fun `skal filtrere bort perioder før virkningstidspunkt og justere fom ved overlapp`() {
        val perioder =
            listOf(
                ÅrMånedsperiode(YearMonth.of(2025, 7), YearMonth.of(2025, 8)),
                ÅrMånedsperiode(YearMonth.of(2025, 7), YearMonth.of(2025, 10)),
                ÅrMånedsperiode(YearMonth.of(2026, 2), null),
            )

        val resultat = perioder.filtrerOgJusterFraVirkningstidspunkt(ÅrMånedsperiode(YearMonth.of(2025, 8), null))

        resultat shouldBe
            listOf(
                ÅrMånedsperiode(YearMonth.of(2025, 8), YearMonth.of(2025, 10)),
                ÅrMånedsperiode(YearMonth.of(2026, 2), null),
            )
    }

    @Test
    fun `skal filtrere bort perioder før oppøhr og justere fom ved overlapp`() {
        val perioder =
            listOf(
                ÅrMånedsperiode(YearMonth.of(2025, 7), YearMonth.of(2025, 8)),
                ÅrMånedsperiode(YearMonth.of(2025, 7), YearMonth.of(2025, 10)),
                ÅrMånedsperiode(YearMonth.of(2026, 2), null),
            )

        val resultat = perioder.filtrerOgJusterFraVirkningstidspunkt(ÅrMånedsperiode(YearMonth.of(2025, 8), YearMonth.of(2025, 10)))

        resultat shouldBe
            listOf(
                ÅrMånedsperiode(YearMonth.of(2025, 8), YearMonth.of(2025, 10)),
            )
    }

    @Test
    fun `skal beholde åpen periode men justere fom til virkningstidspunkt`() {
        val perioder =
            listOf(
                ÅrMånedsperiode(YearMonth.of(2025, 1), null),
            )

        val resultat = perioder.filtrerOgJusterFraVirkningstidspunkt(ÅrMånedsperiode(YearMonth.of(2025, 8), null))

        resultat shouldBe listOf(ÅrMånedsperiode(YearMonth.of(2025, 8), null))
    }

    @Nested
    inner class FinnBeregnFra {
        private val annenBidragsmottaker =
            TestDataPerson(
                navn = "Annen Mappe",
                ident = "99887766554",
                rolletype = Rolletype.BIDRAGSMOTTAKER,
                fødselsdato = LocalDate.parse("1980-01-01"),
            )

        @Test
        fun `barn født før virkningstidspunkt beregnes fra eldste virkningstidspunkt i FF`() {
            val behandling = opprettGyldigBehandlingForBeregningOgVedtak(true, typeBehandling = TypeBehandling.BIDRAG)
            behandling.forholdsmessigFordeling = ForholdsmessigFordeling(null, true)
            val søknadsbarn = behandling.søknadsbarn.first()
            søknadsbarn.stønadstype = Stønadstype.BIDRAG
            søknadsbarn.fødselsdato = LocalDate.of(2015, 4, 10)

            søknadsbarn.finnBeregnFra() shouldBe behandling.eldsteVirkningstidspunkt.toYearMonth()
        }

        @Test
        fun `BM beregnes fra tidligste virkningstidspunkt blant egne barn`() {
            val behandling = opprettFFBehandlingMedToBidragsmottakere()

            behandling.roller.first { it.ident == annenBidragsmottaker.ident }.finnBeregnFra() shouldBe YearMonth.of(2025, 6)
        }

        @Test
        fun `BP beregnes fra tidligste virkningstidspunkt blant alle barn`() {
            val behandling = opprettFFBehandlingMedToBidragsmottakere()

            behandling.bidragspliktig!!.finnBeregnFra() shouldBe YearMonth.of(2025, 1)
        }

        @Test
        fun `FF - barn beregnes fra eldste virkningstidspunkt når bidrag løper fra før`() {
            val behandling = opprettFFBehandlingDerBarn2HarSenereVirkningstidspunkt()
            behandling.leggTilGrunnlagBeløpshistorikk(
                Grunnlagsdatatype.BELØPSHISTORIKK_BIDRAG,
                behandling.barn2(),
                listOf(opprettStønadPeriodeDto(ÅrMånedsperiode(YearMonth.of(2024, 1), null))),
            )

            behandling.barn2().finnBeregnFra() shouldBe YearMonth.of(2025, 1)
        }

        @Test
        fun `skal bruke beløpshistorikk i lesemodus for klage når bidrag løper før barnets virkningstidspunkt i FF`() {
            val behandling = opprettFFBehandlingDerBarn2HarSenereVirkningstidspunkt()
            behandling.vedtakstype = Vedtakstype.KLAGE
            behandling.lesemodusVedtak =
                LesemodusVedtak(
                    erAvvist = false,
                    inneholderBareRevurderingsbarn = false,
                    opprettetAvBatch = false,
                    erOrkestrertVedtak = false,
                    fattetTidspunkt = LocalDate.of(2025, 6, 1).atStartOfDay(),
                )
            behandling.leggTilGrunnlagBeløpshistorikk(
                Grunnlagsdatatype.BELØPSHISTORIKK_BIDRAG,
                behandling.barn2(),
                listOf(opprettStønadPeriodeDto(ÅrMånedsperiode(YearMonth.of(2025, 1), null))),
                nesteIndeksår = 2026,
            )

            val beregnFra = behandling.barn2().finnBeregnFra()

            beregnFra shouldBe YearMonth.of(2025, 1)
        }

        @Test
        fun `FF - barn beregnes fra eget virkningstidspunkt når privat avtale sluttet før eldste virkningstidspunkt`() {
            val behandling = opprettFFBehandlingDerBarn2HarSenereVirkningstidspunkt()
            behandling.leggTilPrivatAvtale(testdataBarn2, YearMonth.of(2024, 1), YearMonth.of(2024, 12))

            behandling.barn2().finnBeregnFra() shouldBe YearMonth.of(2025, 6)
        }

        @Test
        fun `FF - barn beregnes fra eldste virkningstidspunkt når privat avtale løper fra før`() {
            val behandling = opprettFFBehandlingDerBarn2HarSenereVirkningstidspunkt()
            behandling.leggTilPrivatAvtale(testdataBarn2, YearMonth.of(2020, 1), null)

            behandling.barn2().finnBeregnFra() shouldBe YearMonth.of(2025, 1)
        }

        @Test
        fun `FF - barn beregnes fra eget virkningstidspunkt når valgt vedtak fra Nav har opphørt`() {
            val behandling = opprettFFBehandlingDerBarn2HarSenereVirkningstidspunkt()
            behandling.leggTilVedtakFraNav(
                testdataBarn2,
                opprettVedtakPeriode(ÅrMånedsperiode(YearMonth.of(2024, 1), YearMonth.of(2025, 1)), BigDecimal(1000)),
                opprettVedtakPeriode(ÅrMånedsperiode(YearMonth.of(2025, 1), null), null),
            )

            behandling.barn2().finnBeregnFra() shouldBe YearMonth.of(2025, 6)
        }

        @Test
        fun `FF - barn beregnes fra eldste virkningstidspunkt når valgt vedtak fra Nav løper fra før`() {
            val behandling = opprettFFBehandlingDerBarn2HarSenereVirkningstidspunkt()
            behandling.leggTilVedtakFraNav(
                testdataBarn2,
                opprettVedtakPeriode(ÅrMånedsperiode(YearMonth.of(2024, 1), null), BigDecimal(1000)),
            )

            behandling.barn2().finnBeregnFra() shouldBe YearMonth.of(2025, 1)
        }

        private fun opprettFFBehandlingDerBarn2HarSenereVirkningstidspunkt(): Behandling {
            val behandling = opprettGyldigBehandlingForBeregningOgVedtak(true, typeBehandling = TypeBehandling.BIDRAG, andreBarn = listOf(testdataBarn2))
            behandling.forholdsmessigFordeling = ForholdsmessigFordeling(null, true)
            behandling.søknadsbarn.forEach { it.stønadstype = Stønadstype.BIDRAG }
            behandling.søknadsbarn.first { it.ident == testdataBarn1.ident }.virkningstidspunkt = LocalDate.parse("2025-01-01")
            behandling.barn2().virkningstidspunkt = LocalDate.parse("2025-06-01")
            return behandling
        }

        private fun opprettFFBehandlingMedToBidragsmottakere(): Behandling {
            val behandling = opprettFFBehandlingDerBarn2HarSenereVirkningstidspunkt()
            behandling.roller.add(opprettRolle(behandling, annenBidragsmottaker))
            behandling.søknadsbarn.first { it.ident == testdataBarn1.ident }.tilhørerBidragsmottaker(testdataBM)
            behandling.barn2().tilhørerBidragsmottaker(annenBidragsmottaker)
            return behandling
        }

        private fun Behandling.barn2() = søknadsbarn.first { it.ident == testdataBarn2.ident }

        private fun Rolle.tilhørerBidragsmottaker(bidragsmottaker: TestDataPerson) {
            forholdsmessigFordeling =
                ForholdsmessigFordelingRolle(
                    tilhørerSak = behandling.saksnummer,
                    behandlerenhet = null,
                    delAvOpprinneligBehandling = true,
                    erRevurdering = false,
                    bidragsmottaker = bidragsmottaker.ident,
                )
        }

        private fun Behandling.leggTilVedtakFraNav(
            barn: TestDataPerson,
            vararg perioder: VedtakPeriodeDto,
        ) {
            val privatAvtale = opprettPrivatAvtale(this, barn)
            privatAvtale.avtaleType = PrivatAvtaleType.VEDTAK_FRA_NAV
            privatAvtale.grunnlagFraVedtak = GrunnlagFraVedtak(vedtak = 1, perioder = perioder.toList())
            this.privatAvtale.add(privatAvtale)
        }

        private fun opprettVedtakPeriode(
            periode: ÅrMånedsperiode,
            beløp: BigDecimal?,
        ) = VedtakPeriodeDto(
            periode = periode,
            beløp = beløp,
            valutakode = null,
            resultatkode = "",
            delytelseId = null,
            grunnlagReferanseListe = emptyList(),
        )
    }

    @Test
    fun `skal sette opphørsdato til virkningstidspunkt ved avvisning uten eksisterende opphør`() {
        every { barnebidragGrunnlagInnhenting.byggGrunnlagBeløpshistorikk(any(), any()) } returns emptySet()
        // Må ha et søknadsbarn uten avslag for at behandlingen ikke skal regnes som direkte avslag
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(true, typeBehandling = TypeBehandling.BIDRAG, andreBarn = listOf(testdataBarn2))
        val søknadsbarn = behandling.søknadsbarn.first { it.ident == testdataBarn1.ident }
        søknadsbarn.avslag = Resultatkode.BIDRAGSMOTTAKER_HAR_OMSORG_FOR_BARNET
        søknadsbarn.virkningstidspunkt = LocalDate.parse("2023-04-01")
        søknadsbarn.opphørsdato = LocalDate.parse("2023-08-01")

        val grunnlag = vedtakGrunnlagMapper.byggGrunnlagForBeregning(behandling, søknadsbarn).beregnGrunnlag

        grunnlag.opphørsdato shouldBe YearMonth.of(2023, 4)
    }

    @Test
    fun `skal sette opphørsdato til opphørsdato på rolle ved avvisning uten eksisterende opphør og virkningstidspunkt`() {
        every { barnebidragGrunnlagInnhenting.byggGrunnlagBeløpshistorikk(any(), any()) } returns emptySet()
        // Må ha et søknadsbarn uten avslag for at behandlingen ikke skal regnes som direkte avslag
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(true, typeBehandling = TypeBehandling.BIDRAG, andreBarn = listOf(testdataBarn2))
        val søknadsbarn = behandling.søknadsbarn.first { it.ident == testdataBarn1.ident }
        søknadsbarn.avslag = Resultatkode.BIDRAGSMOTTAKER_HAR_OMSORG_FOR_BARNET
        søknadsbarn.virkningstidspunkt = null
        søknadsbarn.opphørsdato = LocalDate.parse("2023-08-01")

        val grunnlag = vedtakGrunnlagMapper.byggGrunnlagForBeregning(behandling, søknadsbarn).beregnGrunnlag

        grunnlag.opphørsdato shouldBe YearMonth.of(2023, 8)
    }

    @Test
    fun `Skal bruke beregn fra som beregn til dato ved avvisning uten eksisterende opphør eller opphørsdato`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(true, typeBehandling = TypeBehandling.BIDRAG)
        val søknadsbarn = behandling.søknadsbarn.first()
        søknadsbarn.avslag = Resultatkode.BIDRAGSMOTTAKER_HAR_OMSORG_FOR_BARNET
        søknadsbarn.opphørsdato = null

        behandling.finnBeregnTilDatoBehandling(søknadsbarn) shouldBe søknadsbarn.finnBeregnFra().atDay(1)
    }

    @Test
    fun `Skal bruke opphørsdato som beregn til dato ved avvisning uten eksisterende opphør`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(true, typeBehandling = TypeBehandling.BIDRAG)
        val søknadsbarn = behandling.søknadsbarn.first()
        søknadsbarn.avslag = Resultatkode.BIDRAGSMOTTAKER_HAR_OMSORG_FOR_BARNET
        søknadsbarn.opphørsdato = LocalDate.parse("2023-06-01")

        behandling.finnBeregnTilDatoBehandling(søknadsbarn) shouldBe LocalDate.parse("2023-06-01")
    }

    @Test
    fun `Skal bruke opphørsdato fra eksisterende vedtak som beregn til dato ved avvisning`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(true, typeBehandling = TypeBehandling.BIDRAG)
        val søknadsbarn = behandling.søknadsbarn.first()
        søknadsbarn.avslag = Resultatkode.BIDRAGSMOTTAKER_HAR_OMSORG_FOR_BARNET
        søknadsbarn.opphørsdato = LocalDate.parse("2023-06-01")
        behandling.leggTilGrunnlagBeløpshistorikk(
            Grunnlagsdatatype.BELØPSHISTORIKK_BIDRAG,
            søknadsbarn,
            listOf(opprettStønadPeriodeDto(ÅrMånedsperiode(LocalDate.parse("2022-01-01"), LocalDate.parse("2024-03-01")))),
        )

        behandling.finnBeregnTilDatoBehandling(søknadsbarn) shouldBe LocalDate.parse("2024-03-01")
    }
}

package no.nav.bidrag.behandling.transformers.vedtak.mapping.tilvedtak

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.behandling.database.datamodell.GebyrRolle
import no.nav.bidrag.behandling.database.datamodell.Inntekt
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordeling
import no.nav.bidrag.behandling.service.BarnebidragGrunnlagInnhenting
import no.nav.bidrag.behandling.service.BeregningEvnevurderingService
import no.nav.bidrag.behandling.service.PersonService
import no.nav.bidrag.behandling.transformers.beregning.ValiderBeregning
import no.nav.bidrag.behandling.transformers.filtrerOgJusterFraVirkningstidspunkt
import no.nav.bidrag.behandling.utils.stubPersonConsumer
import no.nav.bidrag.behandling.dto.v2.behandling.Grunnlagsdatatype
import no.nav.bidrag.behandling.utils.testdata.leggTilGrunnlagBeløpshistorikk
import no.nav.bidrag.behandling.utils.testdata.opprettStønadPeriodeDto
import no.nav.bidrag.behandling.utils.testdata.opprettGyldigBehandlingForBeregningOgVedtak
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
import no.nav.bidrag.domene.enums.sjablon.SjablonTallNavn
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.felles.toYearMonth
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
class VedtakGrunnlagMapperTest {
    lateinit var vedtakGrunnlagMapper: VedtakGrunnlagMapper

    @MockK
    lateinit var barnebidragGrunnlagInnhenting: BarnebidragGrunnlagInnhenting

    @MockK
    lateinit var evnevurderingService: BeregningEvnevurderingService

    @BeforeEach
    fun init() {
        val personService = PersonService(stubPersonConsumer())
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

        behandling.bidragsmottaker!!.gebyr = GebyrRolle(true, true, "test")
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

        val sammenslåttePerioder = perioder.`slåSammenEtterfølgendePerioder`()

        sammenslåttePerioder shouldBe listOf(ÅrMånedsperiode(YearMonth.of(2025, 1), YearMonth.of(2025, 8)))
    }

    @Test
    fun `skal ikke slå sammen perioder når det er hull mellom dem`() {
        val perioder =
            listOf(
                ÅrMånedsperiode(YearMonth.of(2025, 1), YearMonth.of(2025, 3)),
                ÅrMånedsperiode(YearMonth.of(2025, 4), YearMonth.of(2025, 6)),
            )

        val sammenslåttePerioder = perioder.`slåSammenEtterfølgendePerioder`()

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

    @Test
    fun `finnBeregnFra skal ikke starte før barnets fødsel ved forholdsmessig fordeling`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(true, typeBehandling = TypeBehandling.BIDRAG)
        behandling.forholdsmessigFordeling = ForholdsmessigFordeling(null, true)
        val søknadsbarn = behandling.søknadsbarn.first()
        søknadsbarn.stønadstype = Stønadstype.BIDRAG
        søknadsbarn.fødselsdato = LocalDate.of(2023, 6, 15)

        søknadsbarn.finnBeregnFra() shouldBe YearMonth.of(2023, 6)
    }

    @Test
    fun `finnBeregnFra skal bruke eldste virkningstidspunkt når barnet er født før virkning ved forholdsmessig fordeling`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(true, typeBehandling = TypeBehandling.BIDRAG)
        behandling.forholdsmessigFordeling = ForholdsmessigFordeling(null, true)
        val søknadsbarn = behandling.søknadsbarn.first()
        søknadsbarn.stønadstype = Stønadstype.BIDRAG
        søknadsbarn.fødselsdato = LocalDate.of(2015, 4, 10)

        søknadsbarn.finnBeregnFra() shouldBe behandling.eldsteVirkningstidspunkt.toYearMonth()
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

        val grunnlag = vedtakGrunnlagMapper.byggGrunnlagForBeregning(behandling, søknadsbarn).beregnGrunnlag!!

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

        val grunnlag = vedtakGrunnlagMapper.byggGrunnlagForBeregning(behandling, søknadsbarn).beregnGrunnlag!!

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

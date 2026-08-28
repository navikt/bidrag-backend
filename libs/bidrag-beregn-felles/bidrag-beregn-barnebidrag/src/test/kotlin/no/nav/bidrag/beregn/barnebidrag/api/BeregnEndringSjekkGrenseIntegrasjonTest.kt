package no.nav.bidrag.beregn.barnebidrag.api

import io.mockk.mockkObject
import no.nav.bidrag.beregn.barnebidrag.felles.FellesTest
import no.nav.bidrag.beregn.barnebidrag.service.beregning.BeregnBarnebidragService
import no.nav.bidrag.beregn.barnebidrag.unleash.BarnebidragUnleashFeatures
import no.nav.bidrag.beregn.barnebidrag.unleash.disableUnleashFeature
import no.nav.bidrag.beregn.barnebidrag.unleash.enableUnleashFeature
import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider
import no.nav.bidrag.commons.web.mock.stubSjablonProvider
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultatV2
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningEndringSjekkGrense
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningEndringSjekkGrensePeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningBarnebidragV2
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåEgenReferanse
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Testklasse som dekker ulike resultatscenarioer fra BeregnEndringSjekkGrenseService
 * og konsekvenser for endelig respons fra beregnBarnebidragV2.
 *
 * Scenarioer med ny12ProsentRegel = false (standard):
 * A) Endring over grense → beregnet bidrag brukes som endelig beløp
 * B) Endring under grense → beløpshistorikk brukes som endelig beløp
 * C) Eget tiltak → 12%-sjekk hoppes over, beregnet bidrag brukes direkte
 * D) Førstegangsfastsettelse (ingen beløpshistorikk/søknad) → endring over grense = true
 * E) Klage → 12%-sjekk hoppes over, beregnet bidrag brukes direkte
 *
 * Scenarioer med ny12ProsentRegel = true (beregnV2 – periodisert grensevurdering):
 * F) Over grense i alle perioder → beregnet bidrag brukes i alle perioder
 * G) Under grense i alle perioder → beløpshistorikk brukes i alle perioder
 * H) Delvis over grense → beløpshistorikk i første del, beregnet bidrag i andre del (to unike resultatperioder)
 * I) Førstegangsfastsettelse → endring over grense = true (alle løpende er null)
 */
@ExtendWith(MockitoExtension::class)
internal class BeregnEndringSjekkGrenseIntegrasjonTest : FellesTest() {

    private val beregningsperiode = ÅrMånedsperiode(fom = YearMonth.parse("2024-08"), til = YearMonth.parse("2025-03"))

    @Mock
    private lateinit var api: BeregnBarnebidragService

    @BeforeEach
    fun initMock() {
        stubSjablonProvider()
        mockkObject(UnleashFeaturesProvider)
        api = BeregnBarnebidragService()
    }

    @Test
    @DisplayName("Endring sjekk grense - Scenario A: Endring over grense - beregnet bidrag brukes som endelig beløp")
    fun `endring over grense - beregnet bidrag brukes`() {
        // Løpende bidrag = 2000, beregnet bidrag er vesentlig høyere → endring > 12% → over grense
        disableUnleashFeature(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT)
        val resultat = utførBeregning(filnavn = "barnebidragV2_endringSjekkGrense_overGrense.json")
        val barn = resultat.single()
        val grunnlagListe = barn.beregnetBarnebidragResultat.grunnlagListe
        val perioder = barn.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe

        // Henter endring sjekk grense resultat
        val endringSjekkGrense = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningEndringSjekkGrense>(Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE)

        // Henter endring sjekk grense periode
        val endringSjekkGrensePeriode = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningEndringSjekkGrensePeriode>(
                Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE_PERIODE,
            )

        // Henter sluttberegning for å sammenligne beregnet beløp med endelig beløp
        val sluttberegning = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<SluttberegningBarnebidragV2>(Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG)

        assertAll(
            // Endring sjekk grense totalt: endring ER over grense
            { assertThat(endringSjekkGrense).hasSize(1) },
            { assertThat(endringSjekkGrense[0].innhold.endringErOverGrense).isTrue() },

            // Endring sjekk grense per periode finnes
            { assertThat(endringSjekkGrensePeriode).isNotEmpty() },

            // Sluttberegning finnes
            { assertThat(sluttberegning).isNotEmpty() },

            // Resultatperioder finnes
            { assertThat(perioder).isNotEmpty() },

            // Endelig beløp = beregnet beløp (ikke beløpshistorikk=2000)
            { assertThat(perioder[0].resultat.beløp).isNotNull() },
            { assertThat(perioder[0].resultat.beløp).isNotEqualTo(BigDecimal.valueOf(2000)) },

            // Endelig beløp skal matche sluttberegningens resultatBeløp
            {
                assertThat(perioder[0].resultat.beløp).isEqualTo(
                    sluttberegning.first().innhold.resultatBeløp,
                )
            },
        )
    }

    @Test
    @DisplayName("Endring sjekk grense - Scenario B: Endring under grense - beløpshistorikk brukes som endelig beløp")
    fun `endring under grense - beløpshistorikk brukes`() {
        // Løpende bidrag = 5000, beregnet bidrag er i nærheten (~4850) → endring < 12% → under grense
        disableUnleashFeature(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT)
        val resultat = utførBeregning(filnavn = "barnebidragV2_endringSjekkGrense_underGrense.json")
        val barn = resultat.single()
        val grunnlagListe = barn.beregnetBarnebidragResultat.grunnlagListe
        val perioder = barn.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe

        val endringSjekkGrense = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningEndringSjekkGrense>(Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE)

        val endringSjekkGrensePeriode = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningEndringSjekkGrensePeriode>(
                Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE_PERIODE,
            )

        val sluttberegning = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<SluttberegningBarnebidragV2>(Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG)

        assertAll(
            // Endring sjekk grense totalt: endring er IKKE over grense
            { assertThat(endringSjekkGrense).hasSize(1) },
            { assertThat(endringSjekkGrense[0].innhold.endringErOverGrense).isFalse() },

            // Endring sjekk grense per periode finnes
            { assertThat(endringSjekkGrensePeriode).isNotEmpty() },

            // Sluttberegning finnes (men brukes ikke som endelig beløp)
            { assertThat(sluttberegning).isNotEmpty() },

            // Resultatperioder finnes
            { assertThat(perioder).isNotEmpty() },

            // Endelig beløp = beløpshistorikk (5000), IKKE beregnet beløp
            { assertThat(perioder[0].resultat.beløp).isEqualTo(BigDecimal.valueOf(5000)) },
        )
    }

    @Test
    @DisplayName("Endring sjekk grense - Scenario C: Eget tiltak - 12%-sjekk hoppes over")
    fun `eget tiltak - 12 prosent sjekk hoppes over`() {
        // egetTiltak=true → 12%-sjekk hoppes over → beregnet bidrag brukes direkte, ingen endring sjekk grense
        disableUnleashFeature(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT)
        val resultat = utførBeregning(filnavn = "barnebidragV2_endringSjekkGrense_egetTiltak.json")
        val barn = resultat.single()
        val grunnlagListe = barn.beregnetBarnebidragResultat.grunnlagListe
        val perioder = barn.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe

        val endringSjekkGrense = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningEndringSjekkGrense>(Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE)

        val endringSjekkGrensePeriode = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningEndringSjekkGrensePeriode>(
                Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE_PERIODE,
            )

        val sluttberegning = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<SluttberegningBarnebidragV2>(Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG)

        assertAll(
            // Ingen endring sjekk grense delberegninger (hoppes over pga egetTiltak)
            { assertThat(endringSjekkGrense).isEmpty() },
            { assertThat(endringSjekkGrensePeriode).isEmpty() },

            // Sluttberegning finnes
            { assertThat(sluttberegning).isNotEmpty() },

            // Resultatperioder finnes
            { assertThat(perioder).isNotEmpty() },

            // Endelig beløp = beregnet beløp fra sluttberegning (ikke beløpshistorikk=2000)
            { assertThat(perioder[0].resultat.beløp).isNotNull() },
            { assertThat(perioder[0].resultat.beløp).isNotEqualTo(BigDecimal.valueOf(2000)) },
            {
                assertThat(perioder[0].resultat.beløp).isEqualTo(
                    sluttberegning.first().innhold.resultatBeløp,
                )
            },
        )
    }

    @Test
    @DisplayName("Endring sjekk grense - Scenario D: Førstegangsfastsettelse - ingen beløpshistorikk - endring over grense")
    fun `førstegangsfastsettelse - endring over grense`() {
        // Ingen SØKNAD og ingen BELØPSHISTORIKK → endring over grense = true (all løpende null + all beregnet null)
        // I praksis betyr dette at skalSjekkeMotMinimumsgrenseForEndring returnerer true (default), men alle løpende er null
        disableUnleashFeature(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT)
        val resultat = utførBeregning(filnavn = "barnebidragV2_endringSjekkGrense_førstegangsfastsettelse.json")
        val barn = resultat.single()
        val grunnlagListe = barn.beregnetBarnebidragResultat.grunnlagListe
        val perioder = barn.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe

        val endringSjekkGrense = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningEndringSjekkGrense>(Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE)

        val sluttberegning = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<SluttberegningBarnebidragV2>(Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG)

        assertAll(
            // Endring sjekk grense: endring ER over grense (fordi alle løpende er null)
            { assertThat(endringSjekkGrense).hasSize(1) },
            { assertThat(endringSjekkGrense[0].innhold.endringErOverGrense).isTrue() },

            // Sluttberegning finnes
            { assertThat(sluttberegning).isNotEmpty() },

            // Resultatperioder finnes
            { assertThat(perioder).isNotEmpty() },

            // Endelig beløp = beregnet beløp (over grense → sluttberegning)
            { assertThat(perioder[0].resultat.beløp).isNotNull() },
            {
                assertThat(perioder[0].resultat.beløp).isEqualTo(
                    sluttberegning.first().innhold.resultatBeløp,
                )
            },
        )
    }

    @Test
    @DisplayName("Endring sjekk grense - Scenario E: Klage - 12%-sjekk hoppes over")
    fun `klage - 12 prosent sjekk hoppes over`() {
        // klageMottattDato != null → 12%-sjekk hoppes over → beregnet bidrag brukes direkte
        disableUnleashFeature(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT)
        val resultat = utførBeregning(filnavn = "barnebidragV2_endringSjekkGrense_klage.json")
        val barn = resultat.single()
        val grunnlagListe = barn.beregnetBarnebidragResultat.grunnlagListe
        val perioder = barn.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe

        val endringSjekkGrense = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningEndringSjekkGrense>(Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE)

        val endringSjekkGrensePeriode = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningEndringSjekkGrensePeriode>(
                Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE_PERIODE,
            )

        val sluttberegning = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<SluttberegningBarnebidragV2>(Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG)

        assertAll(
            // Ingen endring sjekk grense delberegninger (hoppes over pga klage)
            { assertThat(endringSjekkGrense).isEmpty() },
            { assertThat(endringSjekkGrensePeriode).isEmpty() },

            // Sluttberegning finnes
            { assertThat(sluttberegning).isNotEmpty() },

            // Resultatperioder finnes
            { assertThat(perioder).isNotEmpty() },

            // Endelig beløp = beregnet beløp fra sluttberegning (ikke beløpshistorikk=2000)
            { assertThat(perioder[0].resultat.beløp).isNotNull() },
            { assertThat(perioder[0].resultat.beløp).isNotEqualTo(BigDecimal.valueOf(2000)) },
            {
                assertThat(perioder[0].resultat.beløp).isEqualTo(
                    sluttberegning.first().innhold.resultatBeløp,
                )
            },
        )
    }

    // ---- Scenarioer med ny12ProsentRegel = true ----

    @Test
    @DisplayName("ny12ProsentRegel - Scenario F: Endring over grense i alle perioder - beregnet bidrag brukes")
    fun `ny12ProsentRegel - over grense i alle perioder`() {
        // Løpende bidrag = 2000, beregnet bidrag er vesentlig høyere → endring > 12% i alle perioder
        enableUnleashFeature(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT)
        val resultat = utførBeregning(filnavn = "barnebidragV2_endringSjekkGrense_overGrense.json")
        val barn = resultat.single()
        val grunnlagListe = barn.beregnetBarnebidragResultat.grunnlagListe
        val perioder = barn.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe

        val endringSjekkGrense = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningEndringSjekkGrense>(Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE)

        val sluttberegning = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<SluttberegningBarnebidragV2>(Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG)

        assertAll(
            // Med ny12ProsentRegel returnerer beregnV2 samme resultat i alle perioder når endring er over grense i første periode
            { assertThat(endringSjekkGrense).hasSize(2) },
            { assertThat(endringSjekkGrense[0].innhold.endringErOverGrense).isTrue() },
            { assertThat(endringSjekkGrense[1].innhold.endringErOverGrense).isTrue() },

            // Resultatperioder finnes
            { assertThat(perioder).isNotEmpty() },

            // Endelig beløp = beregnet beløp (over grense → sluttberegning)
            { assertThat(perioder[0].resultat.beløp).isNotNull() },
            { assertThat(perioder[0].resultat.beløp).isNotEqualTo(BigDecimal.valueOf(2000)) },
            {
                assertThat(perioder[0].resultat.beløp).isEqualTo(
                    sluttberegning.first().innhold.resultatBeløp,
                )
            },
        )
    }

    @Test
    @DisplayName("ny12ProsentRegel - Scenario G: Endring under grense i alle perioder - beløpshistorikk brukes")
    fun `ny12ProsentRegel - under grense i alle perioder`() {
        // Løpende bidrag = 5000, beregnet bidrag er i nærheten → endring < 12% i alle perioder
        enableUnleashFeature(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT)
        val resultat = utførBeregning(filnavn = "barnebidragV2_endringSjekkGrense_underGrense.json")
        val barn = resultat.single()
        val grunnlagListe = barn.beregnetBarnebidragResultat.grunnlagListe
        val perioder = barn.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe

        val endringSjekkGrense = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningEndringSjekkGrense>(Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE)

        assertAll(
            // Med ny12ProsentRegel returnerer beregnV2 samme resultat i alle perioder når alle perioder er under grense
            { assertThat(endringSjekkGrense).hasSize(2) },
            { assertThat(endringSjekkGrense[0].innhold.endringErOverGrense).isFalse() },
            { assertThat(endringSjekkGrense[1].innhold.endringErOverGrense).isFalse() },

            // Resultatperioder finnes
            { assertThat(perioder).isNotEmpty() },

            // Endelig beløp = beløpshistorikk (5000), IKKE beregnet beløp
            { assertThat(perioder[0].resultat.beløp).isEqualTo(BigDecimal.valueOf(5000)) },
        )
    }

    @Test
    @DisplayName("ny12ProsentRegel - Scenario H: Delvis over grense - beløpshistorikk i første del, beregnet i andre del")
    fun `ny12ProsentRegel - delvis over grense`() {
        // Periode 1 (2024-08 - 2024-11): BP inntekt 500 000 → beregnet bidrag nær 5000 → under grense
        // Periode 2 (2024-11 - 2025-01): BP inntekt 900 000 → beregnet bidrag vesentlig høyere → over grense
        // Periode 3 (2025-01 - 2025-03): BP inntekt 500 000 → beregnet bidrag nær 5000 → under grense
        // ny12ProsentRegel = true → beregnV2 returnerer to perioder (2024-08 - 2024-11 under; 2024-11 - 2025-03 over)
        enableUnleashFeature(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT)
        val resultat = utførBeregning(filnavn = "barnebidragV2_endringSjekkGrense_ny12ProsentRegel_delvisOverGrense.json")
        val barn = resultat.single()
        val grunnlagListe = barn.beregnetBarnebidragResultat.grunnlagListe
        val perioder = barn.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe

        val endringSjekkGrense = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningEndringSjekkGrense>(Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE)

        val endringSjekkGrensePeriode = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningEndringSjekkGrensePeriode>(
                Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE_PERIODE,
            )

        val sluttberegning = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<SluttberegningBarnebidragV2>(Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG)

        assertAll(
            // beregnV2 returnerer tre DelberegningEndringSjekkGrense: en under grense og to over grense
            { assertThat(endringSjekkGrense).hasSize(3) },
            { assertThat(endringSjekkGrense[0].innhold.endringErOverGrense).isFalse() },
            { assertThat(endringSjekkGrense[1].innhold.endringErOverGrense).isTrue() },
            { assertThat(endringSjekkGrense[2].innhold.endringErOverGrense).isTrue() },

            // Første periode: under grense → fom = 2024-08
            {
                assertThat(endringSjekkGrense[0].innhold.periode.fom).isEqualTo(
                    YearMonth.parse("2024-08"),
                )
            },
            // Andre periode: over grense → fom = 2024-11
            {
                assertThat(endringSjekkGrense[1].innhold.periode.fom).isEqualTo(
                    YearMonth.parse("2024-11"),
                )
            },

            // Endring sjekk grense per periode finnes (minst 2 perioder)
            { assertThat(endringSjekkGrensePeriode.size).isGreaterThanOrEqualTo(2) },

            // Sluttberegning finnes (minst 2 perioder)
            { assertThat(sluttberegning.size).isGreaterThanOrEqualTo(2) },

            // Resultatperioder finnes (minst 2 perioder)
            { assertThat(perioder.size).isGreaterThanOrEqualTo(2) },

            // Første resultatperiode (under grense): endelig beløp = beløpshistorikk (5000)
            { assertThat(perioder[0].resultat.beløp).isEqualTo(BigDecimal.valueOf(5000)) },

            // Andre resultatperiode (over grense): endelig beløp = beregnet beløp fra sluttberegning
            { assertThat(perioder[1].resultat.beløp).isNotNull() },
            { assertThat(perioder[1].resultat.beløp).isNotEqualTo(BigDecimal.valueOf(5000)) },
        )
    }

    @Test
    @DisplayName("ny12ProsentRegel - Scenario I: Førstegangsfastsettelse - ingen beløpshistorikk")
    fun `ny12ProsentRegel - førstegangsfastsettelse`() {
        // Ingen beløpshistorikk → beregnV2 returnerer over grense = true
        enableUnleashFeature(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT)
        val resultat = utførBeregning("barnebidragV2_endringSjekkGrense_førstegangsfastsettelse.json")
        val barn = resultat.single()
        val grunnlagListe = barn.beregnetBarnebidragResultat.grunnlagListe
        val perioder = barn.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe

        val endringSjekkGrense = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningEndringSjekkGrense>(Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE)

        val sluttberegning = grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<SluttberegningBarnebidragV2>(Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG)

        assertAll(
            // beregnV2 returnerer samme resultat i alle perioder med endring over grense (alle løpende er null)
            { assertThat(endringSjekkGrense).hasSize(2) },
            { assertThat(endringSjekkGrense[0].innhold.endringErOverGrense).isTrue() },
            { assertThat(endringSjekkGrense[1].innhold.endringErOverGrense).isTrue() },

            // Resultatperioder finnes
            { assertThat(perioder).isNotEmpty() },

            // Endelig beløp = beregnet beløp
            { assertThat(perioder[0].resultat.beløp).isNotNull() },
            {
                assertThat(perioder[0].resultat.beløp).isEqualTo(
                    sluttberegning.first().innhold.resultatBeløp,
                )
            },
        )
    }

    private fun utførBeregning(
        filnavn: String,
    ): List<BeregnetBarnebidragResultatV2> {
        val requestSøknadsbarn: List<BeregnGrunnlag> =
            lesFilOgByggRequestGenerisk("src/test/resources/testfiler/barnebidrag/$filnavn")

        val barnebidragResultat = api.beregnBarnebidragV2(
            beregningsperiode = beregningsperiode,
            grunnlagSøknadsbarnListe = requestSøknadsbarn,
        )

        println(commonObjectmapper.writeValueAsString(barnebidragResultat))
        return barnebidragResultat
    }
}

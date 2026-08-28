package no.nav.bidrag.beregn.barnebidrag.api

import io.mockk.mockkObject
import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.beregn.barnebidrag.felles.FellesTest
import no.nav.bidrag.beregn.barnebidrag.unleash.BarnebidragUnleashFeatures
import no.nav.bidrag.beregn.barnebidrag.unleash.disableUnleashFeature
import no.nav.bidrag.beregn.barnebidrag.unleash.enableUnleashFeature
import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningEndringSjekkGrense
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåEgenReferanse
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.YearMonth

@ExtendWith(MockitoExtension::class)
internal class BeregnEndringSjekkGrenseTest : FellesTest() {
    private lateinit var filnavn: String
    private var forventetEndringErOverGrense: Boolean? = false
    private var forventetAntallResultatPerioder: Int = 1
    private var forventetAntallResultatVerdier: Int = 1

    @Mock
    private lateinit var api: BeregnBarnebidragApi

    @BeforeEach
    fun initMock() {
        mockkObject(UnleashFeaturesProvider)
        api = BeregnBarnebidragApi()
    }

    @Test
    @DisplayName("Endring sjekk grense gamle regler - eksempel 1 - minst en av periodene er over grense => en resultatperiode over grense")
    fun testEndringSjekkGrense_Eksempel01() {
        disableUnleashFeature(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT)
        filnavn = "src/test/resources/testfiler/endringsjekkgrense/endring_sjekk_grense_eksempel1.json"
        forventetEndringErOverGrense = true
        forventetAntallResultatPerioder = 1
        utførBeregningerOgEvaluerResultat()
    }

    @Test
    @DisplayName("Endring sjekk grense gamle regler - eksempel 2 - alle periodene er under grense => en resultatperiode under grense")
    fun testEndringSjekkGrense_Eksempel02() {
        disableUnleashFeature(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT)
        filnavn = "src/test/resources/testfiler/endringsjekkgrense/endring_sjekk_grense_eksempel2.json"
        forventetEndringErOverGrense = false
        forventetAntallResultatPerioder = 1
        utførBeregningerOgEvaluerResultat()
    }

    @Test
    @DisplayName(
        "Endring sjekk grense gamle regler - eksempel 3 - førstegangsfastsettelse og avslag i alle perioder => " +
            "en resultatperiode over grense",
    )
    fun testEndringSjekkGrense_Eksempel03() {
        disableUnleashFeature(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT)
        filnavn = "src/test/resources/testfiler/endringsjekkgrense/endring_sjekk_grense_eksempel3.json"
        forventetEndringErOverGrense = true
        forventetAntallResultatPerioder = 1
        utførBeregningerOgEvaluerResultat()
    }

    @Test
    @DisplayName("Endring sjekk grense nye regler - eksempel 4 - første periode er over grense => alle resultatperioder over grense")
    fun testEndringSjekkGrense_Eksempel04() {
        enableUnleashFeature(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT)
        filnavn = "src/test/resources/testfiler/endringsjekkgrense/endring_sjekk_grense_eksempel4.json"
        forventetEndringErOverGrense = true
        forventetAntallResultatPerioder = 3
        utførBeregningerOgEvaluerResultat()
    }

    @Test
    @DisplayName("Endring sjekk grense nye regler - eksempel 5 - alle perioder er under grense => alle resultatperioder under grense")
    fun testEndringSjekkGrense_Eksempel05() {
        enableUnleashFeature(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT)
        filnavn = "src/test/resources/testfiler/endringsjekkgrense/endring_sjekk_grense_eksempel5.json"
        forventetEndringErOverGrense = false
        forventetAntallResultatPerioder = 3
        utførBeregningerOgEvaluerResultat()
    }

    @Test
    @DisplayName(
        "Endring sjekk grense nye regler - eksempel 6 - første periode er under grense og det finnes minst en periode over grense => " +
            "første resultatperiode under grense og resten over grense",
    )
    fun testEndringSjekkGrense_Eksempel06() {
        enableUnleashFeature(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT)
        filnavn = "src/test/resources/testfiler/endringsjekkgrense/endring_sjekk_grense_eksempel6.json"
        forventetEndringErOverGrense = null
        forventetAntallResultatPerioder = 3
        forventetAntallResultatVerdier = 2
        utførBeregningerOgEvaluerResultat()
    }

    @Test
    @DisplayName(
        "Endring sjekk grense nye regler - eksempel 7 - førstegangsfastsettelse og avslag i alle perioder => " +
            "alle resultatperioder over grense",
    )
    fun testEndringSjekkGrense_Eksempel07() {
        enableUnleashFeature(BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT)
        filnavn = "src/test/resources/testfiler/endringsjekkgrense/endring_sjekk_grense_eksempel7.json"
        forventetEndringErOverGrense = true
        forventetAntallResultatPerioder = 3
        utførBeregningerOgEvaluerResultat()
    }

    private fun utførBeregningerOgEvaluerResultat() {
        val request = lesFilOgByggRequest(filnavn)
        val endringSjekkGrenseResultat = api.beregnEndringSjekkGrense(beregnGrunnlag = request)
        println(commonObjectmapper.writeValueAsString(endringSjekkGrenseResultat))

        val alleReferanser = hentAlleReferanser(endringSjekkGrenseResultat)
        val alleRefererteReferanser = hentAlleRefererteReferanser(endringSjekkGrenseResultat)

        val endringSjekkGrenseResultatListe = endringSjekkGrenseResultat
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningEndringSjekkGrense>(Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE)
            .map {
                DelberegningEndringSjekkGrense(
                    periode = it.innhold.periode,
                    endringErOverGrense = it.innhold.endringErOverGrense,
                )
            }

        val antallEndringSjekkGrensePeriode = endringSjekkGrenseResultat
            .filter { it.type == Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE_PERIODE }
            .size

        if (endringSjekkGrenseResultatListe.size == 1) {
            assertAll(
                { assertThat(endringSjekkGrenseResultat).isNotNull },
                { assertThat(endringSjekkGrenseResultatListe).isNotNull },
                { assertThat(endringSjekkGrenseResultatListe).hasSize(forventetAntallResultatPerioder) },

                // Resultat
                { assertThat(endringSjekkGrenseResultatListe[0].periode).isEqualTo(ÅrMånedsperiode(YearMonth.parse("2024-08"), null)) },
                { assertThat(endringSjekkGrenseResultatListe[0].endringErOverGrense).isEqualTo(forventetEndringErOverGrense) },

                // Grunnlag
                { assertThat(antallEndringSjekkGrensePeriode).isEqualTo(3) },

                // Referanser
                { assertThat(alleReferanser).containsAll(alleRefererteReferanser) },
            )
        } else if (endringSjekkGrenseResultatListe.size == 3 && forventetEndringErOverGrense != null) {
            assertAll(
                { assertThat(endringSjekkGrenseResultat).isNotNull },
                { assertThat(endringSjekkGrenseResultatListe).isNotNull },
                { assertThat(endringSjekkGrenseResultatListe).hasSize(forventetAntallResultatPerioder) },

                // Resultat
                { assertThat(endringSjekkGrenseResultatListe[0].periode).isEqualTo(ÅrMånedsperiode(YearMonth.parse("2024-08"), YearMonth.parse("2024-09"))) },
                { assertThat(endringSjekkGrenseResultatListe[0].endringErOverGrense).isEqualTo(forventetEndringErOverGrense) },
                { assertThat(endringSjekkGrenseResultatListe[1].periode).isEqualTo(ÅrMånedsperiode(YearMonth.parse("2024-09"), YearMonth.parse("2024-10"))) },
                { assertThat(endringSjekkGrenseResultatListe[1].endringErOverGrense).isEqualTo(forventetEndringErOverGrense) },
                { assertThat(endringSjekkGrenseResultatListe[2].periode).isEqualTo(ÅrMånedsperiode(YearMonth.parse("2024-10"), null)) },
                { assertThat(endringSjekkGrenseResultatListe[2].endringErOverGrense).isEqualTo(forventetEndringErOverGrense) },

                // Grunnlag
                { assertThat(antallEndringSjekkGrensePeriode).isEqualTo(3) },

                // Referanser
                { assertThat(alleReferanser).containsAll(alleRefererteReferanser) },
            )
        } else if (endringSjekkGrenseResultatListe.size == 3) {
            assertAll(
                { assertThat(endringSjekkGrenseResultat).isNotNull },
                { assertThat(endringSjekkGrenseResultatListe).isNotNull },
                { assertThat(endringSjekkGrenseResultatListe).hasSize(forventetAntallResultatPerioder) },

                // Resultat
                { assertThat(endringSjekkGrenseResultatListe[0].periode).isEqualTo(ÅrMånedsperiode(YearMonth.parse("2024-08"), YearMonth.parse("2024-09"))) },
                { assertThat(endringSjekkGrenseResultatListe[0].endringErOverGrense).isFalse },
                { assertThat(endringSjekkGrenseResultatListe[1].periode).isEqualTo(ÅrMånedsperiode(YearMonth.parse("2024-09"), YearMonth.parse("2024-10"))) },
                { assertThat(endringSjekkGrenseResultatListe[1].endringErOverGrense).isTrue },
                { assertThat(endringSjekkGrenseResultatListe[2].periode).isEqualTo(ÅrMånedsperiode(YearMonth.parse("2024-10"), null)) },
                { assertThat(endringSjekkGrenseResultatListe[2].endringErOverGrense).isTrue },

                // Grunnlag
                { assertThat(antallEndringSjekkGrensePeriode).isEqualTo(3) },

                // Referanser
                { assertThat(alleReferanser).containsAll(alleRefererteReferanser) },
            )
        } else {
            fail("Uventet antall perioder i resultatet: $endringSjekkGrenseResultatListe")
        }
    }
}

package no.nav.bidrag.beregn.barnebidrag.api

import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.beregn.barnebidrag.felles.FellesTest
import no.nav.bidrag.commons.web.mock.stubSjablonProvider
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningNettoBarnetillegg
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

@ExtendWith(MockitoExtension::class)
internal class BeregnNettoBarnetilleggTest : FellesTest() {
    private lateinit var filnavn: String

    @Mock
    private lateinit var api: BeregnBarnebidragApi

    @BeforeEach
    fun initMock() {
        stubSjablonProvider()
        api = BeregnBarnebidragApi()
    }

    @Test
    @DisplayName("Netto barnetillegg - eksempel 1")
    fun testNettoBarnetillegg_Eksempel01() {
        filnavn = "src/test/resources/testfiler/nettobarnetillegg/netto_barnetillegg_eksempel1.json"
        val rolle = Grunnlagstype.PERSON_BIDRAGSPLIKTIG
        val resultat = utførBeregningerOgEvaluerResultatNettoBarnetillegg(rolle)

        assertAll(
            { assertThat(resultat).hasSize(1) },
            { assertThat(resultat[0].summertBruttoBarnetillegg).isEqualTo(BigDecimal.valueOf(1700.00).setScale(2)) },
            { assertThat(resultat[0].summertNettoBarnetillegg).isEqualTo(BigDecimal.valueOf(1105.00).setScale(2)) },
            { assertThat(resultat[0].barnetilleggTypeListe[0].barnetilleggType).isEqualTo(Inntektstype.BARNETILLEGG_PENSJON) },
            { assertThat(resultat[0].barnetilleggTypeListe[0].bruttoBarnetillegg).isEqualTo(BigDecimal.valueOf(1700.00).setScale(2)) },
            { assertThat(resultat[0].barnetilleggTypeListe[0].nettoBarnetillegg).isEqualTo(BigDecimal.valueOf(1105.00).setScale(2)) },
        )
    }

    @Test
    @DisplayName("Netto barnetillegg - eksempel 2 - skal ikke beregne for motsatt part")
    fun testNettoBarnetillegg_Eksempel02() {
        filnavn = "src/test/resources/testfiler/nettobarnetillegg/netto_barnetillegg_eksempel2.json"
        val rolleBP = Grunnlagstype.PERSON_BIDRAGSPLIKTIG
        val resultatBP = utførBeregningerOgEvaluerResultatNettoBarnetillegg(rolleBP)

        val rolleBM = Grunnlagstype.PERSON_BIDRAGSMOTTAKER
        val resultatBM = utførBeregningerOgEvaluerResultatNettoBarnetillegg(rolleBM)

        assertAll(
            { assertThat(resultatBP).hasSize(1) },
            { assertThat(resultatBP[0].summertBruttoBarnetillegg).isEqualTo(BigDecimal.valueOf(1700.00).setScale(2)) },
            { assertThat(resultatBP[0].summertNettoBarnetillegg).isEqualTo(BigDecimal.valueOf(1105.00).setScale(2)) },

            { assertThat(resultatBP[0].barnetilleggTypeListe[0].barnetilleggType).isEqualTo(Inntektstype.BARNETILLEGG_PENSJON) },
            { assertThat(resultatBP[0].barnetilleggTypeListe[0].bruttoBarnetillegg).isEqualTo(BigDecimal.valueOf(1700.00).setScale(2)) },
            { assertThat(resultatBP[0].barnetilleggTypeListe[0].nettoBarnetillegg).isEqualTo(BigDecimal.valueOf(1105.00).setScale(2)) },

            { assertThat(resultatBM).hasSize(1) },
            { assertThat(resultatBM[0].summertBruttoBarnetillegg).isEqualTo(BigDecimal.valueOf(800.00).setScale(2)) },
            { assertThat(resultatBM[0].summertNettoBarnetillegg).isEqualTo(BigDecimal.valueOf(560.00).setScale(2)) },

            { assertThat(resultatBM[0].barnetilleggTypeListe[0].barnetilleggType).isEqualTo(Inntektstype.BARNETILLEGG_PENSJON) },
            { assertThat(resultatBM[0].barnetilleggTypeListe[0].bruttoBarnetillegg).isEqualTo(BigDecimal.valueOf(300.00).setScale(2)) },
            { assertThat(resultatBM[0].barnetilleggTypeListe[0].nettoBarnetillegg).isEqualTo(BigDecimal.valueOf(210.00).setScale(2)) },
            { assertThat(resultatBM[0].barnetilleggTypeListe[1].barnetilleggType).isEqualTo(Inntektstype.BARNETILLEGG_DAGPENGER) },
            { assertThat(resultatBM[0].barnetilleggTypeListe[1].bruttoBarnetillegg).isEqualTo(BigDecimal.valueOf(500.00).setScale(2)) },
            { assertThat(resultatBM[0].barnetilleggTypeListe[1].nettoBarnetillegg).isEqualTo(BigDecimal.valueOf(350.00).setScale(2)) },
        )
    }

    @Test
    @DisplayName("Netto barnetillegg - eksempel 3 - skattesats mangler i grunnlaget (settes til 0)")
    fun testNettoBarnetillegg_Eksempel03() {
        filnavn = "src/test/resources/testfiler/nettobarnetillegg/netto_barnetillegg_eksempel3.json"

        val rolleBM = Grunnlagstype.PERSON_BIDRAGSMOTTAKER
        val resultatBM = utførBeregningerOgEvaluerResultatNettoBarnetillegg(rolleBM)

        assertAll(
            { assertThat(resultatBM).hasSize(1) },
            { assertThat(resultatBM[0].summertBruttoBarnetillegg).isEqualTo(BigDecimal.valueOf(800.00).setScale(2)) },
            { assertThat(resultatBM[0].summertNettoBarnetillegg).isEqualTo(BigDecimal.valueOf(800.00).setScale(2)) },

            { assertThat(resultatBM[0].barnetilleggTypeListe[0].barnetilleggType).isEqualTo(Inntektstype.BARNETILLEGG_PENSJON) },
            { assertThat(resultatBM[0].barnetilleggTypeListe[0].bruttoBarnetillegg).isEqualTo(BigDecimal.valueOf(300.00).setScale(2)) },
            { assertThat(resultatBM[0].barnetilleggTypeListe[0].nettoBarnetillegg).isEqualTo(BigDecimal.valueOf(300.00).setScale(2)) },
            { assertThat(resultatBM[0].barnetilleggTypeListe[1].barnetilleggType).isEqualTo(Inntektstype.BARNETILLEGG_DAGPENGER) },
            { assertThat(resultatBM[0].barnetilleggTypeListe[1].bruttoBarnetillegg).isEqualTo(BigDecimal.valueOf(500.00).setScale(2)) },
            { assertThat(resultatBM[0].barnetilleggTypeListe[1].nettoBarnetillegg).isEqualTo(BigDecimal.valueOf(500.00).setScale(2)) },
        )
    }

    @Test
    @DisplayName("Netto barnetillegg - eksempel 4 - periodisering ")
    fun testNettoBarnetillegg_Eksempel04() {
        filnavn = "src/test/resources/testfiler/nettobarnetillegg/netto_barnetillegg_eksempel4_periodisering.json"

        val rolleBM = Grunnlagstype.PERSON_BIDRAGSMOTTAKER
        val resultat = utførBeregningerOgEvaluerResultatNettoBarnetillegg(rolleBM)

        assertAll(
            { assertThat(resultat).hasSize(3) },

            { assertThat(resultat[0].periode).isEqualTo(ÅrMånedsperiode("2024-02", "2024-03")) },
            { assertThat(resultat[0].summertBruttoBarnetillegg).isEqualTo(BigDecimal.valueOf(300.00).setScale(2)) },

            { assertThat(resultat[1].periode).isEqualTo(ÅrMånedsperiode("2024-03", "2024-06")) },
            { assertThat(resultat[1].summertBruttoBarnetillegg).isEqualTo(BigDecimal.valueOf(800.00).setScale(2)) },

            { assertThat(resultat[2].periode).isEqualTo(ÅrMånedsperiode("2024-06", "2024-08")) },
            { assertThat(resultat[2].summertBruttoBarnetillegg).isEqualTo(BigDecimal.valueOf(500.00).setScale(2)) },

        )
    }

    @Test
    @DisplayName("Netto barnetillegg - eksempel 5 - tiltakspenger")
    fun testNettoBarnetillegg_Eksempel05() {
        filnavn = "src/test/resources/testfiler/nettobarnetillegg/netto_barnetillegg_eksempel5.json"
        val rolle = Grunnlagstype.PERSON_BIDRAGSPLIKTIG
        val resultat = utførBeregningerOgEvaluerResultatNettoBarnetillegg(rolle)

        assertAll(
            { assertThat(resultat).hasSize(1) },
            { assertThat(resultat[0].summertBruttoBarnetillegg).isEqualTo(BigDecimal.valueOf(1700).setScale(2)) },
            { assertThat(resultat[0].summertNettoBarnetillegg).isEqualTo(BigDecimal.valueOf(2105).setScale(2)) },
            { assertThat(resultat[0].barnetilleggTypeListe).hasSize(2) },

            { assertThat(resultat[0].barnetilleggTypeListe[0].barnetilleggType).isEqualTo(Inntektstype.BARNETILLEGG_PENSJON) },
            { assertThat(resultat[0].barnetilleggTypeListe[0].bruttoBarnetillegg).isEqualTo(BigDecimal.valueOf(1700).setScale(2)) },
            { assertThat(resultat[0].barnetilleggTypeListe[0].nettoBarnetillegg).isEqualTo(BigDecimal.valueOf(1105).setScale(2)) },
            { assertThat(resultat[0].barnetilleggTypeListe[1].barnetilleggType).isEqualTo(Inntektstype.BARNETILLEGG_TILTAKSPENGER) },
            { assertThat(resultat[0].barnetilleggTypeListe[1].bruttoBarnetillegg).isEqualTo(BigDecimal.ZERO.setScale(2)) },
            { assertThat(resultat[0].barnetilleggTypeListe[1].nettoBarnetillegg).isEqualTo(BigDecimal.valueOf(1000).setScale(2)) },
        )
    }

    private fun utførBeregningerOgEvaluerResultatNettoBarnetillegg(rolle: Grunnlagstype): List<DelberegningNettoBarnetillegg> {
        val request = lesFilOgByggRequest(filnavn)
        val nettoBarnetilleggResultat = api.beregnNettoBarnetillegg(request, rolle)
        println(commonObjectmapper.writeValueAsString(nettoBarnetilleggResultat))

        val alleReferanser = hentAlleReferanser(nettoBarnetilleggResultat)
        val alleRefererteReferanser = hentAlleRefererteReferanser(nettoBarnetilleggResultat)

        val nettoBarnetilleggResultatListe = nettoBarnetilleggResultat
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningNettoBarnetillegg>(Grunnlagstype.DELBEREGNING_NETTO_BARNETILLEGG)
            .map {
                DelberegningNettoBarnetillegg(
                    periode = it.innhold.periode,
                    summertBruttoBarnetillegg = it.innhold.summertBruttoBarnetillegg,
                    summertNettoBarnetillegg = it.innhold.summertNettoBarnetillegg,
                    barnetilleggTypeListe = it.innhold.barnetilleggTypeListe,
                )
            }

        assertAll(
            { assertThat(nettoBarnetilleggResultat).isNotNull },
            { assertThat(alleReferanser).containsAll(alleRefererteReferanser) },
        )
        return nettoBarnetilleggResultatListe
    }
}

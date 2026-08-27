package no.nav.bidrag.beregn.barnebidrag.api

import io.mockk.every
import no.nav.bidrag.beregn.barnebidrag.BeregnIndeksreguleringPrivatAvtaleApi
import no.nav.bidrag.beregn.barnebidrag.felles.FellesTest
import no.nav.bidrag.commons.service.sjablon.SjablonProvider
import no.nav.bidrag.commons.service.sjablon.Sjablontall
import no.nav.bidrag.commons.web.mock.stubSjablonProvider
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningIndeksreguleringPrivatAvtale
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
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockitoExtension::class)
internal class BeregnIndeksreguleringPrivatAvtaleTest : FellesTest() {
    private lateinit var filnavn: String

    @Mock
    private lateinit var api: BeregnIndeksreguleringPrivatAvtaleApi

    @BeforeEach
    fun initMock() {
        stubSjablonProvider()
        api = BeregnIndeksreguleringPrivatAvtaleApi()
    }

    @Test
    @DisplayName("Privat avtale - uten indeksregulering")
    fun testIndeksreguleringPrivatAvtaleUtenIndeksregulering() {
        filnavn = "src/test/resources/testfiler/indeksreguleringprivatavtale/privat_avtale_uten_indeksregulering.json"
        val resultat = utførBeregningerOgEvaluerResultatIndeksreguleringPrivatAvtale()

        assertAll(
            { assertThat(resultat).hasSize(3) },

            // Resultat
            { assertThat(resultat[0].periode).isEqualTo(ÅrMånedsperiode("2023-01", "2024-09")) },
            { assertThat(resultat[0].indeksregulertBeløp.compareTo(BigDecimal.valueOf(100.00))).isEqualTo(0) },
            { assertThat(resultat[0].indeksreguleringFaktor).isNull() },

            { assertThat(resultat[1].periode).isEqualTo(ÅrMånedsperiode("2024-09", "2024-11")) },
            { assertThat(resultat[1].indeksregulertBeløp.compareTo(BigDecimal.valueOf(150.00))).isEqualTo(0) },
            { assertThat(resultat[1].indeksreguleringFaktor).isNull() },

            { assertThat(resultat[2].periode).isEqualTo(ÅrMånedsperiode(YearMonth.parse("2024-11"), null)) },
            { assertThat(resultat[2].indeksregulertBeløp.compareTo(BigDecimal.valueOf(210.00))).isEqualTo(0) },
            { assertThat(resultat[2].indeksreguleringFaktor).isNull() },
        )
    }

    @Test
    @DisplayName("Privat avtale - med indeksregulering")
    fun testIndeksreguleringPrivatAvtaleMedIndeksregulering() {
        filnavn = "src/test/resources/testfiler/indeksreguleringprivatavtale/privat_avtale_med_indeksregulering.json"
        val resultat = utførBeregningerOgEvaluerResultatIndeksreguleringPrivatAvtale()

        assertAll(
            { assertThat(resultat).hasSize(6) },

            // Resultat
            { assertThat(resultat[0].periode).isEqualTo(ÅrMånedsperiode("2021-01", "2022-01")) },
            { assertThat(resultat[0].indeksregulertBeløp.compareTo(BigDecimal.valueOf(500.00))).isEqualTo(0) },
            { assertThat(resultat[0].indeksreguleringFaktor).isNull() },

            { assertThat(resultat[1].periode).isEqualTo(ÅrMånedsperiode("2022-01", "2023-07")) },
            { assertThat(resultat[1].indeksregulertBeløp.compareTo(BigDecimal.valueOf(1000.00))).isEqualTo(0) },
            { assertThat(resultat[1].indeksreguleringFaktor).isNull() },

            { assertThat(resultat[2].periode).isEqualTo(ÅrMånedsperiode("2023-07", "2024-07")) },
            { assertThat(resultat[2].indeksregulertBeløp.compareTo(BigDecimal.valueOf(1070.00))).isEqualTo(0) },
            { assertThat(resultat[2].indeksreguleringFaktor?.compareTo(BigDecimal.valueOf(0.0700))).isEqualTo(0) },

            { assertThat(resultat[3].periode).isEqualTo(ÅrMånedsperiode(YearMonth.parse("2024-07"), YearMonth.parse("2025-07"))) },
            { assertThat(resultat[3].indeksregulertBeløp.compareTo(BigDecimal.valueOf(1120.00))).isEqualTo(0) },
            { assertThat(resultat[3].indeksreguleringFaktor?.compareTo(BigDecimal.valueOf(0.0470))).isEqualTo(0) },
        )
    }

    @Test
    @DisplayName("Privat avtale - med indeksregulering der tildato er satt. Skal da returnere uten å indeksregulere. Skal egentlig ikke skje.")
    fun testIndeksreguleringPrivatAvtaleMedIndeksreguleringTildatoSatt() {
        filnavn = "src/test/resources/testfiler/indeksreguleringprivatavtale/privat_avtale_med_indeksregulering_siste_periode_med_satt_tildato.json"
        val resultat = utførBeregningerOgEvaluerResultatIndeksreguleringPrivatAvtale()

        assertAll(
            { assertThat(resultat).hasSize(2) },

            // Resultat
            { assertThat(resultat[0].periode).isEqualTo(ÅrMånedsperiode("2021-01", "2022-01")) },
            { assertThat(resultat[0].indeksregulertBeløp.compareTo(BigDecimal.valueOf(500.00))).isEqualTo(0) },
            { assertThat(resultat[0].indeksreguleringFaktor).isNull() },

            { assertThat(resultat[1].periode).isEqualTo(ÅrMånedsperiode("2022-01", "2023-10")) },
            { assertThat(resultat[1].indeksregulertBeløp.compareTo(BigDecimal.valueOf(1000.00))).isEqualTo(0) },
            { assertThat(resultat[1].indeksreguleringFaktor).isNull() },
        )
    }

    @Test
    @DisplayName("Privat avtale - med indeksregulering Test periode hentes fra privatavtaleperioder")
    fun testIndeksreguleringPrivatAvtaleMedIndeksreguleringPerioderFraPrivatAvtalePerioder() {
        filnavn = "src/test/resources/testfiler/indeksreguleringprivatavtale/privat_avtale_med_indeksregulering_periode.json"
        val resultat = utførBeregningerOgEvaluerResultatIndeksreguleringPrivatAvtale()

        assertAll(
            { assertThat(resultat).hasSize(4) },

            // Resultat
            { assertThat(resultat[0].periode).isEqualTo(ÅrMånedsperiode("2023-06", "2024-07")) },
            { assertThat(resultat[0].indeksregulertBeløp.compareTo(BigDecimal.valueOf(2000.00))).isEqualTo(0) },
            { assertThat(resultat[0].indeksreguleringFaktor).isNull() },

            { assertThat(resultat[1].periode).isEqualTo(ÅrMånedsperiode(YearMonth.parse("2024-07"), YearMonth.parse("2025-07"))) },
            { assertThat(resultat[1].indeksregulertBeløp.compareTo(BigDecimal.valueOf(2090.00))).isEqualTo(0) },
            { assertThat(resultat[1].indeksreguleringFaktor?.compareTo(BigDecimal.valueOf(0.0470))).isEqualTo(0) },
        )
    }

    @Test
    @DisplayName("Privat avtale - test periode ikke indeksreguleres hvis det ikke finnes sjablon i perioden")
    fun testIngenIndeksreguleringUtenSjablonverdi() {
        filnavn = "src/test/resources/testfiler/indeksreguleringprivatavtale/privat_avtale_med_indeksregulering_sjablon.json"

        every {
            SjablonProvider.hentSjablontall()
        } returns dummySjablonSjablontallListe()

        val resultat = api.beregnIndeksreguleringPrivatAvtaleV2(lesFilOgByggRequest(filnavn))
        println(commonObjectmapper.writeValueAsString(resultat))

        val resultatListe = resultat
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningIndeksreguleringPrivatAvtale>(Grunnlagstype.DELBEREGNING_INDEKSREGULERING_PRIVAT_AVTALE)

        assertAll(
            { assertThat(resultatListe).hasSize(2) },

            // Resultat
            { assertThat(resultatListe[0].innhold.periode).isEqualTo(ÅrMånedsperiode("2025-04", "2026-07")) },
            { assertThat(resultatListe[0].innhold.indeksregulertBeløp.compareTo(BigDecimal.valueOf(1000.00))).isEqualTo(0) },
            { assertThat(resultatListe[0].innhold.indeksreguleringFaktor).isNull() },

            { assertThat(resultatListe[1].innhold.periode).isEqualTo(ÅrMånedsperiode(YearMonth.parse("2026-07"), null)) },
            { assertThat(resultatListe[1].innhold.indeksregulertBeløp.compareTo(BigDecimal.valueOf(1000.00))).isEqualTo(0) },
            { assertThat(resultatListe[1].innhold.indeksreguleringFaktor).isNull() },
        )
    }

    // Bygger opp liste av sjablonverdier
    fun dummySjablonSjablontallListe(): List<Sjablontall> {
        val sjablonSjablontallListe = mutableListOf<Sjablontall>()
        sjablonSjablontallListe.add(
            Sjablontall(
                typeSjablon = "0050",
                datoFom = LocalDate.parse("2024-07-01"),
                datoTom = LocalDate.parse("2025-07-01"),
                verdi = BigDecimal.valueOf(2),
            ),
        )
        sjablonSjablontallListe.add(
            Sjablontall(
                typeSjablon = "0050",
                datoFom = LocalDate.parse("2025-07-01"),
                datoTom = LocalDate.parse("9999-12-31"),
                verdi = BigDecimal.valueOf(1),
            ),
        )
        return sjablonSjablontallListe
    }

    private fun utførBeregningerOgEvaluerResultatIndeksreguleringPrivatAvtale(): List<DelberegningIndeksreguleringPrivatAvtale> {
        var request = lesFilOgByggRequest(filnavn)
        request = request.copy(
            periode = request.periode.copy(
                til = YearMonth.now().plusMonths(1),
            ),
        )
        val resultat = api.beregnIndeksreguleringPrivatAvtaleV2(request)
        println(commonObjectmapper.writeValueAsString(resultat))

        val alleReferanser = hentAlleReferanser(resultat)
        val alleRefererteReferanser = hentAlleRefererteReferanser(resultat)

        val resultatListe = resultat
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningIndeksreguleringPrivatAvtale>(Grunnlagstype.DELBEREGNING_INDEKSREGULERING_PRIVAT_AVTALE)
            .map {
                DelberegningIndeksreguleringPrivatAvtale(
                    periode = it.innhold.periode,
                    nesteIndeksreguleringsår = it.innhold.nesteIndeksreguleringsår,
                    indeksregulertBeløp = it.innhold.indeksregulertBeløp,
                    valutakode = it.innhold.valutakode,
                    indeksreguleringFaktor = it.innhold.indeksreguleringFaktor,
                )
            }

        assertAll(
            { assertThat(resultat).isNotNull },
            { assertThat(alleReferanser).containsAll(alleRefererteReferanser) },
        )
        return resultatListe
    }
}

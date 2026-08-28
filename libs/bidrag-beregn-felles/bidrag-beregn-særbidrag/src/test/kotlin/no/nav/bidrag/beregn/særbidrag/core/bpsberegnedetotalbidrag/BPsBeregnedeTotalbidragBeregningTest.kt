package no.nav.bidrag.beregn.særbidrag.core.bpsberegnedetotalbidrag

import no.nav.bidrag.beregn.særbidrag.TestUtil
import no.nav.bidrag.beregn.særbidrag.core.bpsberegnedetotalbidrag.beregning.BPsBeregnedeTotalbidragBeregning
import no.nav.bidrag.beregn.særbidrag.core.bpsberegnedetotalbidrag.dto.LøpendeBidragCore
import no.nav.bidrag.beregn.særbidrag.core.bpsberegnedetotalbidrag.dto.LøpendeBidragGrunnlagCore
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Collections.emptyList

internal class BPsBeregnedeTotalbidragBeregningTest {

    private val sjablonPeriodeListe = TestUtil.byggSjablonPeriodeListe()
    private val bPsBeregnedeTotalbidrag = BPsBeregnedeTotalbidragBeregning()

    @DisplayName("Beregning med flere saker og barn der siste manuelle vedtak er beregnet i BBM")
    @Test
    fun beregningKildeBBM() {
        val grunnlag = LøpendeBidragGrunnlagCore(
            beregnDatoFra = LocalDate.of(2020, 8, 1),
            beregnDatoTil = LocalDate.of(2020, 9, 1),
            referanse = "referanse",
            løpendeBidragCoreListe = listOf(
                LøpendeBidragCore(
                    saksnummer = Saksnummer("1"),
                    fødselsdatoBarn = LocalDate.of(2000, 5, 4),
                    personidentBarn = Personident(genererFødselsnummer(LocalDate.of(2000, 5, 4))),
                    referanseBarn = "referanseBarn",
                    løpendeBeløp = BigDecimal.valueOf(1200),
                    valutakode = "NOK",
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1, // 528
                    beregnetBeløp = BigDecimal.valueOf(1004),
                    faktiskBeløp = BigDecimal.valueOf(900),
                    vedtaksid = null,
                    bruttoBidragEtterBarnetilleggBM = null,
                    bruttoBidragEtterBarnetilleggBP = null,
                    erVedtakKildeBBM = true,
                ),
                LøpendeBidragCore(
                    saksnummer = Saksnummer("2"),
                    fødselsdatoBarn = LocalDate.of(2001, 5, 4),
                    personidentBarn = Personident("040501678901"),
                    referanseBarn = "referanseBarn2",
                    løpendeBeløp = BigDecimal.valueOf(1350),
                    valutakode = "NOK",
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_2, // 1749
                    beregnetBeløp = BigDecimal.valueOf(1164),
                    faktiskBeløp = BigDecimal.valueOf(1010),
                    vedtaksid = null,
                    bruttoBidragEtterBarnetilleggBM = null,
                    bruttoBidragEtterBarnetilleggBP = null,
                    erVedtakKildeBBM = true,
                ),
                LøpendeBidragCore(
                    saksnummer = Saksnummer("3"),
                    fødselsdatoBarn = LocalDate.of(2002, 5, 4),
                    personidentBarn = Personident(genererFødselsnummer(LocalDate.of(2002, 5, 4))),
                    referanseBarn = "referanseBarn3",
                    løpendeBeløp = BigDecimal.valueOf(2140),
                    valutakode = "NOK",
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_3, // 3528
                    beregnetBeløp = BigDecimal.valueOf(1725),
                    faktiskBeløp = BigDecimal.valueOf(1700),
                    vedtaksid = null,
                    bruttoBidragEtterBarnetilleggBM = null,
                    bruttoBidragEtterBarnetilleggBP = null,
                    erVedtakKildeBBM = true,
                ),
            ),
            grunnlagsreferanseListe = emptyList(),
            sjablonPeriodeListe = sjablonPeriodeListe,
        )
        val resultat = bPsBeregnedeTotalbidrag.beregn(grunnlag)

        assertThat(resultat.bPsBeregnedeTotalbidrag).isEqualTo(BigDecimal.valueOf(10775.00).setScale(2))
    }

    @DisplayName("Beregning med flere saker og barn der siste manuelle vedtak er beregnet i ny løsning")
    @Test
    fun beregningKildeIkkeBBM() {
        val grunnlag = LøpendeBidragGrunnlagCore(
            beregnDatoFra = LocalDate.of(2020, 8, 1),
            beregnDatoTil = LocalDate.of(2020, 9, 1),
            referanse = "referanse",
            løpendeBidragCoreListe = listOf(
                LøpendeBidragCore(
                    saksnummer = Saksnummer("1"),
                    fødselsdatoBarn = LocalDate.of(2000, 5, 4),
                    personidentBarn = Personident("1"),
                    referanseBarn = "referanseBarn",
                    løpendeBeløp = BigDecimal.valueOf(1200),
                    valutakode = "NOK",
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1, // 528
                    beregnetBeløp = BigDecimal.valueOf(1004),
                    faktiskBeløp = BigDecimal.valueOf(900),
                    vedtaksid = 12345,
                    bruttoBidragEtterBarnetilleggBM = BigDecimal.valueOf(200),
                    bruttoBidragEtterBarnetilleggBP = BigDecimal.valueOf(100),
                    erVedtakKildeBBM = false,

                ),
                LøpendeBidragCore(
                    saksnummer = Saksnummer("2"),
                    fødselsdatoBarn = LocalDate.of(2001, 5, 4),
                    personidentBarn = Personident("2"),
                    referanseBarn = "referanseBarn2",
                    løpendeBeløp = BigDecimal.valueOf(1350),
                    valutakode = "NOK",
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_2, // 1749
                    beregnetBeløp = BigDecimal.valueOf(1164),
                    faktiskBeløp = BigDecimal.valueOf(1010),
                    vedtaksid = 23456,
                    bruttoBidragEtterBarnetilleggBM = BigDecimal.valueOf(300),
                    bruttoBidragEtterBarnetilleggBP = BigDecimal.valueOf(200),
                    erVedtakKildeBBM = false,
                ),
                LøpendeBidragCore(
                    saksnummer = Saksnummer("3"),
                    fødselsdatoBarn = LocalDate.of(2002, 5, 4),
                    personidentBarn = Personident("3"),
                    referanseBarn = "referanseBarn3",
                    løpendeBeløp = BigDecimal.valueOf(2140),
                    valutakode = "NOK",
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_3, // 3528
                    beregnetBeløp = BigDecimal.valueOf(1725),
                    faktiskBeløp = BigDecimal.valueOf(1700),
                    vedtaksid = 34567,
                    bruttoBidragEtterBarnetilleggBM = BigDecimal.valueOf(400),
                    bruttoBidragEtterBarnetilleggBP = BigDecimal.valueOf(300),
                    erVedtakKildeBBM = false,
                ),
            ),
            grunnlagsreferanseListe = emptyList(),
            sjablonPeriodeListe = sjablonPeriodeListe,
        )
        val resultat = bPsBeregnedeTotalbidrag.beregn(grunnlag)

        assertThat(resultat.bPsBeregnedeTotalbidrag).isEqualTo(BigDecimal.valueOf(10795.00).setScale(2))
    }
}

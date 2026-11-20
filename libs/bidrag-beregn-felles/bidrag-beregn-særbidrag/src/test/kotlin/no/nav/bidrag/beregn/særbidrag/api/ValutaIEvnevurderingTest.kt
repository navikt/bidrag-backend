package no.nav.bidrag.beregn.særbidrag.api

import com.fasterxml.jackson.databind.node.POJONode
import io.mockk.unmockkObject
import no.nav.bidrag.beregn.core.exception.UgyldigInputException
import no.nav.bidrag.beregn.særbidrag.BeregnSærbidragApi
import no.nav.bidrag.beregn.særbidrag.testdata.SjablonApiStub
import no.nav.bidrag.commons.service.sjablon.SjablonProvider
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragsevne
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragspliktigesBeregnedeTotalbidrag
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.LøpendeBidrag
import no.nav.bidrag.transport.behandling.felles.grunnlag.LøpendeBidragGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.ValutaPar
import no.nav.bidrag.transport.behandling.felles.grunnlag.ValutakursGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Path

internal class ValutaIEvnevurderingTest {
    private val api = BeregnSærbidragApi()

    @BeforeEach
    fun settOppSjabloner() {
        SjablonApiStub().settOppSjablonStub()
    }

    @AfterEach
    fun fjernSjablonStub() {
        unmockkObject(SjablonProvider.Companion)
    }

    @Test
    fun `skal vurdere løpende NOK-bidrag uten kursgrunnlag`() {
        val grunnlag = grunnlag(Valutakode.NOK)

        val resultat = api.beregn(grunnlag, Vedtakstype.FASTSETTELSE)

        val total = resultat.grunnlagListe.single { it.type == Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_BEREGNEDE_TOTALBIDRAG }
            .innholdTilObjekt<DelberegningBidragspliktigesBeregnedeTotalbidrag>()
        assertThat(total.bidragspliktigesBeregnedeTotalbidrag).isEqualTo(BigDecimal("1000.00"))
        assertThat(resultat.beregnetSærbidragPeriodeListe.single().resultat.resultatkode).isEqualTo(Resultatkode.SÆRBIDRAG_INNVILGET)
    }

    @ParameterizedTest
    @CsvSource("6, 6000.00, SÆRBIDRAG_INNVILGET", "12, 12000.00, SÆRBIDRAG_IKKE_FULL_BIDRAGSEVNE")
    fun `skal bruke eurobidrag i NOK uten å endre selve bidragsevnen`(kurs: String, totalbidrag: String, resultatkode: Resultatkode) {
        val grunnlag = grunnlag(Valutakode.EUR, BigDecimal(kurs))

        val resultat = api.beregn(grunnlag, Vedtakstype.FASTSETTELSE)
        val total = resultat.grunnlagListe.single { it.type == Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_BEREGNEDE_TOTALBIDRAG }
            .innholdTilObjekt<DelberegningBidragspliktigesBeregnedeTotalbidrag>()
        val evne = resultat.grunnlagListe.single { it.type == Grunnlagstype.DELBEREGNING_BIDRAGSEVNE }
            .innholdTilObjekt<DelberegningBidragsevne>()
        val original = grunnlag.grunnlagListe.single { it.type == Grunnlagstype.LØPENDE_BIDRAG }
            .innholdTilObjekt<LøpendeBidragGrunnlag>().løpendeBidragListe.single()

        assertSoftly {
            it.assertThat(total.bidragspliktigesBeregnedeTotalbidrag).isEqualTo(BigDecimal(totalbidrag))
            it.assertThat(evne.beløp).isEqualTo(BigDecimal("11069.14"))
            it.assertThat(resultat.beregnetSærbidragPeriodeListe.single().resultat.resultatkode).isEqualTo(resultatkode)
            it.assertThat(original.løpendeBeløp).isEqualTo(BigDecimal("1000"))
            it.assertThat(original.valutakode).isEqualTo(Valutakode.EUR)
        }
    }

    @Test
    fun `skal legge samværsfradrag i NOK til eurobidraget uten å omregne fradraget`() {
        val grunnlag = grunnlag(
            valuta = Valutakode.EUR,
            kurs = BigDecimal("6"),
            samværsklasse = Samværsklasse.SAMVÆRSKLASSE_1,
        )

        val resultat = api.beregn(grunnlag, Vedtakstype.FASTSETTELSE)
        val total = resultat.grunnlagListe.single { it.type == Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_BEREGNEDE_TOTALBIDRAG }
            .innholdTilObjekt<DelberegningBidragspliktigesBeregnedeTotalbidrag>()
        val bidragPerBarn = total.beregnetBidragPerBarnListe.single()

        assertSoftly {
            it.assertThat(bidragPerBarn.samværsfradrag).isEqualByComparingTo("623")
            it.assertThat(bidragPerBarn.beregnetBidragNOK).isEqualByComparingTo("6623")
            it.assertThat(total.bidragspliktigesBeregnedeTotalbidrag).isEqualByComparingTo("6623")
            it.assertThat(bidragPerBarn.løpendeBeløp).isEqualByComparingTo("1000")
            it.assertThat(bidragPerBarn.valutakode).isEqualTo(Valutakode.EUR)
        }
    }

    @Test
    fun `skal avvise eurobidrag uten kurs fremfor å vurdere eurobeløpet som NOK`() {
        val grunnlag = grunnlag(Valutakode.EUR)

        val feil = assertThrows<UgyldigInputException> {
            api.beregn(grunnlag, Vedtakstype.FASTSETTELSE)
        }
        assertThat(feil).hasMessageContaining("Kurs kreves for å regne ut bidrag i NOK")
    }

    @Test
    fun `skal bruke kurs fra EUR til NOK selv om den inverse kursen står først`() {
        val grunnlag = grunnlag(
            valuta = Valutakode.EUR,
            valutakursListe = listOf(
                ValutaPar(Valutakode.NOK, Valutakode.EUR, BigDecimal("0.1666666667")),
                ValutaPar(Valutakode.EUR, Valutakode.NOK, BigDecimal("6")),
            ),
        )

        val resultat = api.beregn(grunnlag, Vedtakstype.FASTSETTELSE)
        val total = resultat.grunnlagListe.single { it.type == Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_BEREGNEDE_TOTALBIDRAG }
            .innholdTilObjekt<DelberegningBidragspliktigesBeregnedeTotalbidrag>()

        assertSoftly {
            it.assertThat(total.beregnetBidragPerBarnListe.single().beregnetBidragNOK).isEqualByComparingTo("6000")
            it.assertThat(total.bidragspliktigesBeregnedeTotalbidrag).isEqualByComparingTo("6000")
        }
    }

    @Test
    fun `skal avvise eurobidrag når bare kurs fra NOK til EUR finnes`() {
        val grunnlag = grunnlag(
            valuta = Valutakode.EUR,
            valutakursListe = listOf(
                ValutaPar(Valutakode.NOK, Valutakode.EUR, BigDecimal("0.1666666667")),
            ),
        )

        val feil = assertThrows<UgyldigInputException> {
            api.beregn(grunnlag, Vedtakstype.FASTSETTELSE)
        }
        assertThat(feil).hasMessageContaining("Kurs kreves for å regne ut bidrag i NOK")
    }

    private fun grunnlag(
        valuta: Valutakode,
        kurs: BigDecimal? = null,
        samværsklasse: Samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
        valutakursListe: List<ValutaPar> = kurs?.let {
            listOf(
                ValutaPar(valuta, Valutakode.NOK, it),
                ValutaPar(Valutakode.NOK, valuta, BigDecimal.ONE.divide(it, 10, RoundingMode.HALF_UP)),
            )
        } ?: emptyList(),
    ): BeregnGrunnlag {
        val original = commonObjectmapper.readValue(
            Files.readString(Path.of("src/test/resources/testfiler/særbidrag_eksempel1.json")),
            BeregnGrunnlag::class.java,
        )
        val løpende = LøpendeBidrag(
            saksnummer = Saksnummer("1"),
            type = Stønadstype.BIDRAG,
            løpendeBeløp = BigDecimal("1000"),
            valutakode = valuta,
            samværsklasse = samværsklasse,
            beregnetBeløp = BigDecimal("1000"),
            faktiskBeløp = BigDecimal("1000"),
            gjelderBarn = original.søknadsbarnReferanse,
        )
        return original.copy(
            grunnlagListe = original.grunnlagListe.map {
                if (it.type == Grunnlagstype.LØPENDE_BIDRAG) {
                    it.copy(innhold = POJONode(LøpendeBidragGrunnlag(listOf(løpende))))
                } else {
                    it
                }
            } + listOfNotNull(
                valutakursListe.takeIf { it.isNotEmpty() }?.let {
                    GrunnlagDto(
                        referanse = "kurs",
                        type = Grunnlagstype.VALUTAKURS_GRUNNLAG,
                        innhold = POJONode(
                            ValutakursGrunnlag(it),
                        ),
                    )
                },
            ),
        )
    }
}

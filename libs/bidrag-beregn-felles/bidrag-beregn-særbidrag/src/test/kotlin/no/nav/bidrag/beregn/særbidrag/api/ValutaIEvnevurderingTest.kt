package no.nav.bidrag.beregn.særbidrag.api

import com.fasterxml.jackson.databind.node.ObjectNode
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
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
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
import java.time.YearMonth

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
    fun `skal avvise valutakursgrunnlag uten periode ved beregning av særbidrag`() {
        val original = grunnlag(Valutakode.EUR, BigDecimal("6"))
        val grunnlag = original.copy(
            grunnlagListe = original.grunnlagListe.map {
                if (it.type == Grunnlagstype.VALUTAKURS_GRUNNLAG) {
                    it.copy(innhold = POJONode(it.innholdTilObjekt<ValutakursGrunnlag>().copy(periode = null)))
                } else {
                    it
                }
            },
        )

        val feil = assertThrows<IllegalArgumentException> {
            api.beregn(grunnlag, Vedtakstype.FASTSETTELSE)
        }
        assertThat(feil).hasMessage("Valutakursgrunnlaget må ha en periode ved beregning")
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

    @ParameterizedTest
    @CsvSource("2020-07", "2021-01")
    fun `skal velge kurs for beregningsperioden uavhengig av rekkefølge og dagens dato`(halvårsstart: YearMonth) {
        val beregningsperiode = ÅrMånedsperiode(halvårsstart, halvårsstart.plusMonths(1))
        val riktigKurs = kursgrunnlag(ÅrMånedsperiode(halvårsstart, halvårsstart.plusMonths(6)), "6")
        val grunnlag = grunnlagForPeriode(
            beregningsperiode,
            kursgrunnlag(ÅrMånedsperiode(halvårsstart.plusMonths(6), halvårsstart.plusMonths(12)), "18"),
            kursgrunnlag(ÅrMånedsperiode(halvårsstart.minusMonths(6), halvårsstart), "12"),
            riktigKurs,
        )

        val resultat = api.beregn(grunnlag, Vedtakstype.FASTSETTELSE)
        val total = resultat.grunnlagListe.single { it.type == Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_BEREGNEDE_TOTALBIDRAG }
            .innholdTilObjekt<DelberegningBidragspliktigesBeregnedeTotalbidrag>()

        assertSoftly {
            it.assertThat(total.periode).isEqualTo(beregningsperiode)
            it.assertThat(total.bidragspliktigesBeregnedeTotalbidrag).isEqualByComparingTo("6000")
            it.assertThat(total.beregnetBidragPerBarnListe.single().valutakursGrunnlagsreferanse).isEqualTo(riktigKurs.referanse)
        }
    }

    @ParameterizedTest
    @CsvSource("2021-07", "2021-01")
    fun `skal dele beregningsperioden ved halvårsgrensen og bruke kursen for hver delperiode`(halvårsstart: YearMonth) {
        val førstePeriode = ÅrMånedsperiode(halvårsstart.minusMonths(1), halvårsstart)
        val andrePeriode = ÅrMånedsperiode(halvårsstart, halvårsstart.plusMonths(1))
        val førsteKurs = kursgrunnlag(ÅrMånedsperiode(halvårsstart.minusMonths(6), halvårsstart), "6")
        val andreKurs = kursgrunnlag(ÅrMånedsperiode(halvårsstart, halvårsstart.plusMonths(6)), "12")
        val grunnlag = grunnlagForPeriode(
            ÅrMånedsperiode(førstePeriode.fom, andrePeriode.til),
            andreKurs,
            førsteKurs,
        )

        val resultat = api.beregn(grunnlag, Vedtakstype.FASTSETTELSE)
        val totaler = resultat.grunnlagListe
            .filter { it.type == Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_BEREGNEDE_TOTALBIDRAG }
            .map { it.innholdTilObjekt<DelberegningBidragspliktigesBeregnedeTotalbidrag>() }
            .sortedBy { it.periode.fom }

        assertThat(totaler.map { it.periode }).containsExactly(førstePeriode, andrePeriode)
        assertSoftly {
            it.assertThat(totaler[0].bidragspliktigesBeregnedeTotalbidrag).isEqualByComparingTo("6000")
            it.assertThat(totaler[1].bidragspliktigesBeregnedeTotalbidrag).isEqualByComparingTo("12000")
            it.assertThat(totaler[0].beregnetBidragPerBarnListe.single().valutakursGrunnlagsreferanse).isEqualTo(førsteKurs.referanse)
            it.assertThat(totaler[1].beregnetBidragPerBarnListe.single().valutakursGrunnlagsreferanse).isEqualTo(andreKurs.referanse)
        }
        val totalgrunnlag = resultat.grunnlagListe
            .filter { it.type == Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_BEREGNEDE_TOTALBIDRAG }
            .sortedBy { it.innholdTilObjekt<DelberegningBidragspliktigesBeregnedeTotalbidrag>().periode.fom }
        assertSoftly {
            it.assertThat(totalgrunnlag.map { total -> total.referanse }).doesNotHaveDuplicates()
            it.assertThat(totalgrunnlag[0].grunnlagsreferanseListe).contains(førsteKurs.referanse).doesNotContain(andreKurs.referanse)
            it.assertThat(totalgrunnlag[1].grunnlagsreferanseListe).contains(andreKurs.referanse).doesNotContain(førsteKurs.referanse)
            it.assertThat(resultat.beregnetSærbidragPeriodeListe.map { periode -> periode.periode }).containsExactly(førstePeriode, andrePeriode)
            it.assertThat(resultat.beregnetSærbidragPeriodeListe.map { periode -> periode.resultat.resultatkode }).containsExactly(
                Resultatkode.SÆRBIDRAG_INNVILGET,
                Resultatkode.SÆRBIDRAG_IKKE_FULL_BIDRAGSEVNE,
            )
        }
    }

    @ParameterizedTest
    @CsvSource("2020-07", "2021-01")
    fun `skal avvise beregningen når kurs mangler etter halvårsgrensen`(halvårsstart: YearMonth) {
        val grunnlag = grunnlagForPeriode(
            ÅrMånedsperiode(halvårsstart.minusMonths(1), halvårsstart.plusMonths(1)),
            kursgrunnlag(ÅrMånedsperiode(halvårsstart.minusMonths(6), halvårsstart), "6"),
        )

        val feil = assertThrows<UgyldigInputException> {
            api.beregn(grunnlag, Vedtakstype.FASTSETTELSE)
        }

        assertThat(feil).hasMessageContaining("Kurs kreves for å regne ut bidrag i NOK")
    }

    @Test
    fun `skal summere NOK og eurobidrag en gang per delperiode`() {
        val førstePeriode = ÅrMånedsperiode("2020-06", "2020-07")
        val andrePeriode = ÅrMånedsperiode("2020-07", "2020-08")
        val original = grunnlagForPeriode(
            ÅrMånedsperiode(førstePeriode.fom, andrePeriode.til),
            kursgrunnlag(førstePeriode, "6"),
            kursgrunnlag(andrePeriode, "12"),
        )
        val grunnlag = original.copy(
            grunnlagListe = original.grunnlagListe.map {
                if (it.type == Grunnlagstype.LØPENDE_BIDRAG) {
                    val løpende = it.innholdTilObjekt<LøpendeBidragGrunnlag>()
                    it.copy(
                        innhold = POJONode(
                            løpende.copy(
                                løpendeBidragListe = løpende.løpendeBidragListe + løpende.løpendeBidragListe.single().copy(
                                    saksnummer = Saksnummer("2"),
                                    valutakode = Valutakode.NOK,
                                ),
                            ),
                        ),
                    )
                } else {
                    it
                }
            },
        )

        val resultat = api.beregn(grunnlag, Vedtakstype.FASTSETTELSE)
        val totaler = resultat.grunnlagListe
            .filter { it.type == Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_BEREGNEDE_TOTALBIDRAG }
            .map { it.innholdTilObjekt<DelberegningBidragspliktigesBeregnedeTotalbidrag>() }
            .sortedBy { it.periode.fom }

        assertThat(totaler.map { it.bidragspliktigesBeregnedeTotalbidrag }).containsExactly(BigDecimal("7000.00"), BigDecimal("13000.00"))
        totaler.forEach {
            assertThat(it.beregnetBidragPerBarnListe).hasSize(2)
            val nokbidrag = it.beregnetBidragPerBarnListe.single { barn -> barn.valutakode == Valutakode.NOK }
            assertThat(nokbidrag.beregnetBidragNOK).isEqualByComparingTo("1000")
            assertThat(nokbidrag.valutakursGrunnlagsreferanse).isNull()
        }
    }

    @Test
    fun `skal ikke dele NOK-bidrag på grunn av ubrukte valutakurser`() {
        val periode = ÅrMånedsperiode("2020-06", "2020-08")
        val grunnlag = grunnlagForPeriode(
            periode,
            kursgrunnlag(ÅrMånedsperiode("2020-01", "2020-07"), "6"),
            kursgrunnlag(ÅrMånedsperiode("2020-07", "2021-01"), "12"),
            valuta = Valutakode.NOK,
        )

        val resultat = api.beregn(grunnlag, Vedtakstype.FASTSETTELSE)
        val totalgrunnlag = resultat.grunnlagListe.single {
            it.type == Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_BEREGNEDE_TOTALBIDRAG
        }
        val total = totalgrunnlag.innholdTilObjekt<DelberegningBidragspliktigesBeregnedeTotalbidrag>()

        assertSoftly {
            it.assertThat(total.periode).isEqualTo(periode)
            it.assertThat(total.bidragspliktigesBeregnedeTotalbidrag).isEqualByComparingTo("1000")
            it.assertThat(total.beregnetBidragPerBarnListe.single().valutakursGrunnlagsreferanse).isNull()
            it.assertThat(resultat.grunnlagListe).noneMatch { grunnlag -> grunnlag.type == Grunnlagstype.VALUTAKURS_GRUNNLAG }
        }
    }

    @Test
    fun `skal avvise overlappende kurser for samme valuta`() {
        val grunnlag = grunnlagForPeriode(
            ÅrMånedsperiode("2020-06", "2020-08"),
            kursgrunnlag(ÅrMånedsperiode("2020-01", "2021-01"), "6"),
            kursgrunnlag(ÅrMånedsperiode("2020-07", "2021-01"), "12"),
        )

        val feil = assertThrows<UgyldigInputException> {
            api.beregn(grunnlag, Vedtakstype.FASTSETTELSE)
        }

        assertThat(feil).hasMessageContaining("Flere valutakurser dekker samme valuta og beregningsperiode")
    }

    @ParameterizedTest
    @CsvSource("0", "-1")
    fun `skal avvise kurs som ikke er positiv`(kurs: String) {
        val periode = ÅrMånedsperiode("2020-08", "2020-09")
        val grunnlag = grunnlagForPeriode(periode, kursgrunnlag(periode, kurs))

        val feil = assertThrows<UgyldigInputException> {
            api.beregn(grunnlag, Vedtakstype.FASTSETTELSE)
        }

        assertThat(feil).hasMessageContaining("Valutakurs til NOK må være større enn null")
    }

    private fun grunnlagForPeriode(
        periode: ÅrMånedsperiode,
        vararg valutakurser: GrunnlagDto,
        valuta: Valutakode = Valutakode.EUR,
    ): BeregnGrunnlag {
        val original = grunnlag(valuta)
        return original.copy(
            periode = periode,
            grunnlagListe = original.grunnlagListe.map {
                if (it.innhold.has("periode")) {
                    it.copy(
                        innhold = it.innhold.deepCopy<ObjectNode>()
                            .set<ObjectNode>("periode", commonObjectmapper.valueToTree(periode)),
                    )
                } else {
                    it
                }
            } + valutakurser,
        )
    }

    private fun kursgrunnlag(periode: ÅrMånedsperiode, kurs: String) = GrunnlagDto(
        referanse = "kurs_${periode.fom}",
        type = Grunnlagstype.VALUTAKURS_GRUNNLAG,
        innhold = commonObjectmapper.valueToTree<ObjectNode>(
            mapOf(
                "periode" to periode,
                "valutakursListe" to listOf(
                    ValutaPar(Valutakode.EUR, Valutakode.NOK, BigDecimal(kurs)),
                ),
            ),
        ),
    )

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
                            ValutakursGrunnlag(
                                periode = original.periode,
                                valutakursListe = it,
                            ),
                        ),
                    )
                },
            ),
        )
    }
}

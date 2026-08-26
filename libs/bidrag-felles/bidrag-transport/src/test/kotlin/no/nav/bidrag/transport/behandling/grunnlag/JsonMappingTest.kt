package no.nav.bidrag.transport.behandling.grunnlag

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.person.Familierelasjon
import no.nav.bidrag.domene.tid.Datoperiode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragspliktigesAndel
import no.nav.bidrag.transport.behandling.felles.grunnlag.InnhentetHusstandsmedlem
import no.nav.bidrag.transport.behandling.felles.grunnlag.LøpendeBidrag
import no.nav.bidrag.transport.behandling.grunnlag.response.BorISammeHusstandDto
import no.nav.bidrag.transport.behandling.grunnlag.response.RelatertPersonGrunnlagDto
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class JsonMappingTest {
    @Test
    fun `skal returnere andelProsent`() {
        val delberegningObjekt =
            DelberegningBidragspliktigesAndel(
                endeligAndelFaktor = 0.833333333333.toBigDecimal(),
                andelBeløp = BigDecimal(3993),
                beregnetAndelFaktor = 0.85.toBigDecimal(),
                barnetErSelvforsørget = false,
                barnEndeligInntekt = BigDecimal.ZERO,
                periode = ÅrMånedsperiode(LocalDate.parse("2024-08-01"), LocalDate.parse("2024-08-31")),
            )
        delberegningObjekt.andelProsent shouldBe 83.33.toBigDecimal()
        delberegningObjekt.erAndelRedusert shouldBe true

        delberegningObjekt
            .copy(
                endeligAndelFaktor = 0.4432.toBigDecimal(),
            ).andelProsent shouldBe 44.32.toBigDecimal()

        delberegningObjekt
            .copy(
                endeligAndelFaktor = 83.333333333.toBigDecimal(),
            ).andelProsent shouldBe 83.33.toBigDecimal()
    }

    @Test
    fun `skal deserialisere DelberegningBidragspliktigesAndelSærbidrag`() {
        @Language("JSON")
        val json =
            """
            {
             "periode": {
              "fom": "2024-07",
              "til": "2024-08"
            },
            "andelBeløp": 5796,
            "andelProsent": 64.44,
            "beregnetAndelFaktor": 0.64,
            "barnEndeligInntekt": 0,
            "barnetErSelvforsørget": false
                }
            """.trimIndent()

        val delberegning: DelberegningBidragspliktigesAndel = commonObjectmapper.readValue(json)

        delberegning.endeligAndelFaktor shouldBe 64.44.toBigDecimal()
        delberegning.andelProsent shouldBe 64.44.toBigDecimal()
        delberegning.erAndelRedusert shouldBe false

        val delberegningObjekt =
            DelberegningBidragspliktigesAndel(
                endeligAndelFaktor = 0.833333333333.toBigDecimal(),
                andelBeløp = BigDecimal(3993),
                beregnetAndelFaktor = 0.85.toBigDecimal(),
                barnEndeligInntekt = BigDecimal.ZERO,
                barnetErSelvforsørget = false,
                periode = ÅrMånedsperiode(LocalDate.parse("2024-08-01"), LocalDate.parse("2024-08-31")),
            )
        delberegningObjekt.andelProsent shouldBe 83.33.toBigDecimal()
        val delberegningJson: String = commonObjectmapper.writerWithDefaultPrettyPrinter().writeValueAsString(delberegningObjekt)

        @Language("JSON")
        val jsonResult =
            """
            {
  "periode" : {
    "fom" : "2024-08",
    "til" : "2024-08"
  },
  "endeligAndelFaktor" : 0.833333333333,
  "andelBeløp" : 3993,
  "beregnetAndelFaktor" : 0.85,
  "barnEndeligInntekt" : 0,
  "barnetErSelvforsørget" : false
}
            """.trimIndent().trimStart()

        val jsonResultFormattert =
            commonObjectmapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(commonObjectmapper.readValue(jsonResult, Any::class.java))

        delberegningJson shouldBe jsonResultFormattert
    }

    @Test
    fun `skal deserialisere RelatertPersonGrunnlagDto med deprekerte verdier`() {
        @Language("JSON")
        val json =
            """
            [
            
            {
                "navn": "Testperson1",
                "erBarnAvBmBp": true,
                "fødselsdato": "2018-01-04",
                "partPersonId": "1234",
                "relatertPersonPersonId": "55555",
                "borISammeHusstandDtoListe": [
                  {
                    "periodeFra": "2018-01-04"
                  }
                ]
            },
              {
                "navn": "Testperson2",
                "erBarnAvBmBp": false,
                "fødselsdato": "2018-01-04",
                "partPersonId": "1234",
                "relatertPersonPersonId": "66666",
                "borISammeHusstandDtoListe": [
                  {
                    "periodeFra": "2018-01-04"
                  }
                ]
            }
            ]
            """.trimIndent()
        val relatertPersonObjekt: List<RelatertPersonGrunnlagDto> = commonObjectmapper.readValue(json)

        assertSoftly(relatertPersonObjekt[0]) {
            erBarnAvBmBp shouldBe true
            erBarn shouldBe true
            relasjon shouldBe Familierelasjon.BARN
            relatertPersonPersonId shouldBe "55555"
            gjelderPersonId shouldBe "55555"
        }
        assertSoftly(relatertPersonObjekt[1]) {
            erBarnAvBmBp shouldBe false
            erBarn shouldBe false
            relasjon shouldBe Familierelasjon.UKJENT
            relatertPersonPersonId shouldBe "66666"
            gjelderPersonId shouldBe "66666"
        }
    }

    @Test
    fun `skal serialisere RelatertPersonGrunnlagDto med deprekerte verdier`() {
        val relatertPersonGrunnlagListe =
            listOf(
                RelatertPersonGrunnlagDto(
                    navn = "Testperson1",
                    erBarnAvBmBp = true,
                    fødselsdato = LocalDate.parse("2023-01-01"),
                    partPersonId = "1234",
                    relatertPersonPersonId = "55555",
                    borISammeHusstandDtoListe =
                        listOf(
                            BorISammeHusstandDto(
                                periodeFra = LocalDate.parse("2023-01-01"),
                                periodeTil = LocalDate.parse("2024-01-01"),
                            ),
                        ),
                ),
                RelatertPersonGrunnlagDto(
                    navn = "Testperson2",
                    erBarnAvBmBp = false,
                    fødselsdato = LocalDate.parse("2023-01-01"),
                    partPersonId = "1234",
                    relatertPersonPersonId = "666666",
                    borISammeHusstandDtoListe =
                        listOf(
                            BorISammeHusstandDto(
                                periodeFra = LocalDate.parse("2023-01-01"),
                                periodeTil = LocalDate.parse("2024-01-01"),
                            ),
                        ),
                ),
            )
        val jsonString = commonObjectmapper.writerWithDefaultPrettyPrinter().writeValueAsString(relatertPersonGrunnlagListe)

        @Language("JSON")
        val expectedJson =
            """
            [ {
              "partPersonId" : "1234",
              "gjelderPersonId" : "55555",
              "navn" : "Testperson1",
              "fødselsdato" : "2023-01-01",
              "relasjon" : "BARN",
              "borISammeHusstandDtoListe" : [ {
                "periodeFra" : "2023-01-01",
                "periodeTil" : "2024-01-01"
              } ]
            }, {
              "partPersonId" : "1234",
              "gjelderPersonId" : "666666",
              "navn" : "Testperson2",
              "fødselsdato" : "2023-01-01",
              "relasjon" : "UKJENT",
              "borISammeHusstandDtoListe" : [ {
                "periodeFra" : "2023-01-01",
                "periodeTil" : "2024-01-01"
              } ]
            } ]
            """.trimIndent()

        val expectedJsonFormattert =
            commonObjectmapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(commonObjectmapper.readValue(expectedJson, Any::class.java))

        jsonString shouldBe expectedJsonFormattert

        val relatertPersonObjekt: List<RelatertPersonGrunnlagDto> = commonObjectmapper.readValue(jsonString)

        assertSoftly(relatertPersonObjekt[0]) {
            erBarnAvBmBp shouldBe true
            erBarn shouldBe true
            relasjon shouldBe Familierelasjon.BARN
            relatertPersonPersonId shouldBe "55555"
            gjelderPersonId shouldBe "55555"
        }
        assertSoftly(relatertPersonObjekt[1]) {
            erBarnAvBmBp shouldBe false
            erBarn shouldBe false
            relasjon shouldBe Familierelasjon.UKJENT
            relatertPersonPersonId shouldBe "666666"
            gjelderPersonId shouldBe "666666"
        }
    }

    @Test
    fun `skal deserialisere HusstandsmedlemPDL med deprekerte verdier`() {
        @Language("JSON")
        val json =
            """
            [
            
            {
                "navn": "Testperson1",
                "erBarnAvBmBp": true,
                "fødselsdato": "2018-01-04",
                "relatertPerson": "REF_ID",
                "perioder": [
                  {
                    "fom": "2018-01-04",
                    "til": null
                  }
                ]
            },
              {
                "navn": "Testperson2",
                "erBarnAvBmBp": false,
                "fødselsdato": "2018-01-04",
                "relatertPerson": "REF_ID2",
                "perioder": [
                  {
                    "fom": "2018-01-04",
                     "til": null
                  }
                ]
            }
            ]
            """.trimIndent()
        val relatertPersonObjekt: List<InnhentetHusstandsmedlem.HusstandsmedlemPDL> = commonObjectmapper.readValue(json)

        assertSoftly(relatertPersonObjekt[0]) {
            erBarnAvBmBp shouldBe true
            erBarn shouldBe true
            relasjon shouldBe Familierelasjon.BARN
            gjelderPerson shouldBe "REF_ID"
            relatertPerson shouldBe "REF_ID"
        }
        assertSoftly(relatertPersonObjekt[1]) {
            erBarnAvBmBp shouldBe false
            erBarn shouldBe false
            relasjon shouldBe Familierelasjon.UKJENT
            gjelderPerson shouldBe "REF_ID2"
            relatertPerson shouldBe "REF_ID2"
        }
    }

    @Test
    fun `skal serialisere HusstandsmedlemPDL med deprekerte verdier`() {
        val relatertPersonGrunnlagListe =
            listOf(
                InnhentetHusstandsmedlem.HusstandsmedlemPDL(
                    navn = "Testperson1",
                    erBarnAvBmBp = true,
                    fødselsdato = LocalDate.parse("2023-01-01"),
                    relatertPerson = "REF1",
                    perioder =
                        listOf(
                            Datoperiode(
                                LocalDate.parse("2023-01-01"),
                                null,
                            ),
                        ),
                ),
                InnhentetHusstandsmedlem.HusstandsmedlemPDL(
                    navn = "Testperson2",
                    erBarnAvBmBp = false,
                    fødselsdato = LocalDate.parse("2023-01-01"),
                    relatertPerson = "REF2",
                    perioder =
                        listOf(
                            Datoperiode(
                                LocalDate.parse("2023-01-01"),
                                null,
                            ),
                        ),
                ),
            )
        val jsonString = commonObjectmapper.writerWithDefaultPrettyPrinter().writeValueAsString(relatertPersonGrunnlagListe)

        @Language("JSON")
        val expectedJson =
            """
            [ {
              "gjelderPerson" : "REF1",
              "relasjon" : "BARN",
              "navn" : "Testperson1",
              "fødselsdato" : "2023-01-01",
              "perioder" : [ {
                "fom" : "2023-01-01",
                "til" : null
              } ]
            }, {
              "gjelderPerson" : "REF2",
              "relasjon" : "UKJENT",
              "navn" : "Testperson2",
              "fødselsdato" : "2023-01-01",
              "perioder" : [ {
                "fom" : "2023-01-01",
                "til" : null
              } ]
            } ]
            """.trimIndent()

        val expectedJsonFormattert =
            commonObjectmapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(commonObjectmapper.readValue(expectedJson, Any::class.java))

        jsonString shouldBe expectedJsonFormattert

        val husstandsmedlemPDLList: List<InnhentetHusstandsmedlem.HusstandsmedlemPDL> = commonObjectmapper.readValue(jsonString)

        assertSoftly(husstandsmedlemPDLList[0]) {
            erBarnAvBmBp shouldBe true
            erBarn shouldBe true
            relasjon shouldBe Familierelasjon.BARN
            gjelderPerson shouldBe "REF1"
            relatertPerson shouldBe "REF1"
        }
        assertSoftly(husstandsmedlemPDLList[1]) {
            erBarnAvBmBp shouldBe false
            erBarn shouldBe false
            relasjon shouldBe Familierelasjon.UKJENT
            gjelderPerson shouldBe "REF2"
            relatertPerson shouldBe "REF2"
        }
    }

    @Test
    fun `Skal deserialisere Samværsklasse INGEN_SAMVÆR i LøpendeBidrag`() {
        assertSoftly("Med INGEN_SAMVÆR") {
            @Language("JSON")
            val json =
                """
                {
                    "type": "BIDRAG",
                    "samværsklasse": "INGEN_SAMVÆR",
                    "saksnummer": "123",
                    "løpendeBeløp": 0,
                    "beregnetBeløp": 0,
                    "faktiskBeløp": 0,
                    "valutakode": "NOK",
                    "gjelderBarn": ""
                }
                """.trimIndent()

            val samværsklasseAntallDager: LøpendeBidrag = commonObjectmapper.readValue(json)
            assertSoftly(samværsklasseAntallDager) {
                samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_0
            }
        }

        assertSoftly("Med SAMVÆRSKLASSE_0") {
            @Language("JSON")
            val json =
                """
                {
                    "type": "BIDRAG",
                    "samværsklasse": "SAMVÆRSKLASSE_0",
                    "saksnummer": "123",
                    "løpendeBeløp": 0,
                    "beregnetBeløp": 0,
                    "faktiskBeløp": 0,
                    "valutakode": "NOK",
                    "gjelderBarn": ""
                }
                """.trimIndent()

            val samværsklasseAntallDager: LøpendeBidrag = commonObjectmapper.readValue(json)
            assertSoftly(samværsklasseAntallDager) {
                samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_0
            }
        }
    }

    @Test
    fun `Skal Grunnalgstype til default verdi hvis ukjent`() {
        val grunnlagstype: Grunnlagstype =
            commonObjectmapper
                .configure(
                    DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE,
                    true,
                ).readValue("\"TESTTEST\"")
        grunnlagstype shouldBe Grunnlagstype.UKJENT
    }

    @Test
    fun `Skal Grunnalgstype konvertering til SJABLON_SJABLONTALL`() {
        val grunnlagstype: Grunnlagstype =
            commonObjectmapper.readValue("\"SJABLON\"")
        grunnlagstype shouldBe Grunnlagstype.SJABLON_SJABLONTALL
    }
}

package no.nav.bidrag.transport.behandling.beregning.felles

import com.fasterxml.jackson.databind.node.POJONode
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.BostatusPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåEgenReferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåFremmedReferanse
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class BeregningFellesTest {
    @Test
    fun `skal konvertere grunnlag`() {
        val personreferanse = "person_bm_1"
        val beregnGrunnlag =
            BeregnGrunnlag(
                periode = ÅrMånedsperiode(LocalDate.parse("2020-01-01"), null),
                søknadsbarnReferanse = "",
                grunnlagListe =
                    listOf(
                        GrunnlagDto(
                            referanse = personreferanse,
                            type = Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                            innhold =
                                POJONode(
                                    Person(Personident("123123123"), fødselsdato = LocalDate.parse("2023-01-01")),
                                ),
                        ),
                    ),
            )

        val person =
            beregnGrunnlag.grunnlagListe.filtrerOgKonverterBasertPåEgenReferanse<Person>(
                Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                personreferanse,
            )
        person shouldHaveSize 1
        person
            .firstOrNull()
            ?.innhold
            ?.ident
            ?.verdi shouldBe "123123123"
    }

    @Test
    fun `skal hente grunnlag basert på fremmed referanse`() {
        val personreferanse = "person_bm_1"
        val beregnGrunnlag =
            BeregnGrunnlag(
                periode = ÅrMånedsperiode(LocalDate.parse("2020-01-01"), null),
                søknadsbarnReferanse = "",
                grunnlagListe =
                    listOf(
                        GrunnlagDto(
                            referanse = personreferanse,
                            type = Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                            innhold =
                                POJONode(
                                    Person(Personident("123123123"), fødselsdato = LocalDate.parse("2023-01-01")),
                                ),
                        ),
                        GrunnlagDto(
                            referanse = "inntekt_1",
                            type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                            gjelderReferanse = personreferanse,
                            innhold =
                                POJONode(
                                    InntektsrapporteringPeriode(
                                        periode = ÅrMånedsperiode(LocalDate.parse("2020-01-01"), null),
                                        beløp = BigDecimal.ONE,
                                        inntektsrapportering = Inntektsrapportering.AINNTEKT,
                                        manueltRegistrert = false,
                                        valgt = true,
                                    ),
                                ),
                        ),
                        GrunnlagDto(
                            referanse = "inntekt_2",
                            type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                            gjelderReferanse = personreferanse,
                            innhold =
                                POJONode(
                                    InntektsrapporteringPeriode(
                                        periode = ÅrMånedsperiode(LocalDate.parse("2022-01-01"), null),
                                        beløp = BigDecimal.ONE,
                                        inntektsrapportering = Inntektsrapportering.AINNTEKT,
                                        manueltRegistrert = false,
                                        valgt = true,
                                    ),
                                ),
                        ),
                        GrunnlagDto(
                            referanse = personreferanse,
                            type = Grunnlagstype.BOSTATUS_PERIODE,
                            gjelderReferanse = personreferanse,
                            innhold =
                                POJONode(
                                    BostatusPeriode(
                                        periode = ÅrMånedsperiode(LocalDate.parse("2020-01-01"), null),
                                        manueltRegistrert = false,
                                        bostatus = Bostatuskode.MED_FORELDER,
                                        relatertTilPart = "person_bm_2",
                                    ),
                                ),
                        ),
                    ),
            )

        val grunnlagListe =
            beregnGrunnlag.grunnlagListe.filtrerOgKonverterBasertPåFremmedReferanse<InntektsrapporteringPeriode>(
                Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                personreferanse,
            )
        grunnlagListe shouldHaveSize 2
    }
}

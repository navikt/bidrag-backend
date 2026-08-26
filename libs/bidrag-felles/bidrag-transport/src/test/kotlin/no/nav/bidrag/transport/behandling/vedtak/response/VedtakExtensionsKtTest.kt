package no.nav.bidrag.transport.behandling.vedtak.response

import com.fasterxml.jackson.databind.node.POJONode
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.Datoperiode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.AldersjusteringDetaljerGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import no.nav.bidrag.transport.behandling.felles.grunnlag.VirkningstidspunktGrunnlag
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class VedtakExtensionsKtTest {
    @Test
    fun `skal oppprette visningsnavn begrunnelse for aldersjustering`() {
        val barn =
            GrunnlagDto(
                referanse = "barn",
                type = Grunnlagstype.PERSON_SØKNADSBARN,
                innhold =
                    POJONode(
                        Person(
                            fødselsdato = LocalDate.parse("2021-01-01"),
                        ),
                    ),
            )
        val aldersjusteringGrunnlag =
            AldersjusteringDetaljerGrunnlag(
                periode = ÅrMånedsperiode(YearMonth.now().withMonth(7), null),
                grunnlagFraVedtak = 1,
                aldersjustert = true,
                aldersjusteresManuelt = false,
                begrunnelser =
                    listOf(
                        "SISTE_VEDTAK_ER_JUSTERT_NED_TIL_EVNE",
                        "SISTE_VEDTAK_ER_JUSTERT_NED_TIL_25_PROSENT_AV_INNTEKT",
                    ),
            )
        val grunnlag =
            GrunnlagDto(
                type = Grunnlagstype.ALDERSJUSTERING_DETALJER,
                innhold = POJONode(aldersjusteringGrunnlag),
                referanse = "test",
                gjelderBarnReferanse = barn.referanse,
            )

        val stønadsendringDto = opprettStønadsendring(listOf(grunnlag.referanse))
        val vedtakDto = opprettVedtakDto(listOf(barn, grunnlag))
        aldersjusteringGrunnlag.begrunnelserVisningsnavn shouldBe
            "Siste vedtak er justert ned til evne, siste vedtak er justert ned til 25 prosent av inntekt"
        tilAldersjusteringResultattekst(vedtakDto, stønadsendringDto) shouldBe
            "Bidraget til barn født 01.01.2021 ble ikke aldersjustert. Siste vedtak er justert ned til evne"
    }

    @Test
    fun `skal oppprette visningsnavn begrunnelse for manuell aldersjustering`() {
        val barn =
            GrunnlagDto(
                referanse = "barn",
                type = Grunnlagstype.PERSON_SØKNADSBARN,
                innhold =
                    POJONode(
                        Person(
                            fødselsdato = LocalDate.parse("2021-01-01"),
                        ),
                    ),
            )
        val aldersjusteringGrunnlag =
            AldersjusteringDetaljerGrunnlag(
                periode = ÅrMånedsperiode(YearMonth.now().withMonth(7), null),
                grunnlagFraVedtak = 1,
                aldersjustert = true,
                aldersjusteresManuelt = true,
                begrunnelser = listOf("SISTE_VEDTAK_ER_INNVILGET_VEDTAK"),
            )
        val grunnlag =
            GrunnlagDto(
                type = Grunnlagstype.ALDERSJUSTERING_DETALJER,
                innhold = POJONode(aldersjusteringGrunnlag),
                referanse = "test",
                gjelderBarnReferanse = barn.referanse,
            )

        val stønadsendringDto = opprettStønadsendring(listOf(grunnlag.referanse))
        val vedtakDto = opprettVedtakDto(listOf(barn, grunnlag))
        aldersjusteringGrunnlag.begrunnelserVisningsnavn shouldBe
            "Siste vedtak er innvilget vedtak"
        tilAldersjusteringResultattekst(vedtakDto, stønadsendringDto) shouldBe
            "Bidraget til barn født 01.01.2021 skal aldersjusteres manuelt. Siste vedtak er innvilget vedtak"
    }

    @Test
    fun `skal returnere særbidragperiode fra virkningstidspunkt`() {
        val virkningstidspunkt = YearMonth.parse("2021-04")

        val vedtakDto =
            VedtakDto(
                stønadsendringListe = emptyList(),
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                grunnlagListe =
                    listOf(
                        GrunnlagDto(
                            referanse = "1",
                            type = Grunnlagstype.VIRKNINGSTIDSPUNKT,
                            innhold =
                                POJONode(
                                    VirkningstidspunktGrunnlag(
                                        virkningstidspunkt = virkningstidspunkt.atDay(1),
                                    ),
                                ),
                        ),
                    ),
                enhetsnummer = Enhetsnummer("1234"),
                vedtakstidspunkt = LocalDateTime.parse("2020-01-01T00:00:00"),
                unikReferanse = null,
                fastsattILand = "NO",
                innkrevingUtsattTilDato = null,
                kilde = Vedtakskilde.MANUELT,
                kildeapplikasjon = "APP",
                opprettetAv = "USER",
                opprettetAvNavn = "User Name",
                opprettetTidspunkt = LocalDateTime.now(),
                type = Vedtakstype.FASTSETTELSE,
                vedtaksid = 1,
            )

        vedtakDto.særbidragsperiode shouldBe Datoperiode(virkningstidspunkt.atDay(1), virkningstidspunkt.atEndOfMonth())
    }

    @Test
    fun `skal returnere særbidragperiode fra engangsbeløp`() {
        val virkningstidspunkt = YearMonth.parse("2021-04")

        val vedtakDto =
            VedtakDto(
                vedtaksid = 1,
                stønadsendringListe = emptyList(),
                engangsbeløpListe =
                    listOf(
                        EngangsbeløpDto(
                            type = Engangsbeløptype.SÆRBIDRAG,
                            sak = Saksnummer("1234"),
                            beløp = BigDecimal(100),
                            resultatkode = Resultatkode.AVSLAG.name,
                            beslutning = Beslutningstype.ENDRING,
                            delytelseId = "DelytelseId",
                            eksternReferanse = "EksternReferanse",
                            grunnlagReferanseListe = emptyList(),
                            innkreving = Innkrevingstype.MED_INNKREVING,
                            kravhaver = Personident(""),
                            mottaker = Personident(""),
                            omgjørVedtakId = null,
                            referanse = "Referanse",
                            skyldner = Personident(""),
                            valutakode = "Valutakode",
                        ),
                    ),
                behandlingsreferanseListe = emptyList(),
                grunnlagListe = emptyList(),
                enhetsnummer = Enhetsnummer("1234"),
                vedtakstidspunkt = virkningstidspunkt.atDay(1).atStartOfDay(),
                unikReferanse = null,
                fastsattILand = "NO",
                innkrevingUtsattTilDato = null,
                kilde = Vedtakskilde.MANUELT,
                kildeapplikasjon = "APP",
                opprettetAv = "USER",
                opprettetAvNavn = "User Name",
                opprettetTidspunkt = LocalDateTime.now(),
                type = Vedtakstype.FASTSETTELSE,
            )

        vedtakDto.særbidragsperiode shouldBe Datoperiode(virkningstidspunkt.atDay(1), virkningstidspunkt.atEndOfMonth())
    }

    @Test
    fun `skal returnere null særbidragperiode fra engangsbeløp hvis klage`() {
        val virkningstidspunkt = YearMonth.parse("2021-04")

        val vedtakDto =
            VedtakDto(
                stønadsendringListe = emptyList(),
                engangsbeløpListe =
                    listOf(
                        EngangsbeløpDto(
                            type = Engangsbeløptype.SÆRBIDRAG,
                            sak = Saksnummer("1234"),
                            beløp = BigDecimal(100),
                            resultatkode = Resultatkode.AVSLAG.name,
                            beslutning = Beslutningstype.ENDRING,
                            delytelseId = "DelytelseId",
                            eksternReferanse = "EksternReferanse",
                            grunnlagReferanseListe = emptyList(),
                            innkreving = Innkrevingstype.MED_INNKREVING,
                            kravhaver = Personident(""),
                            mottaker = Personident(""),
                            omgjørVedtakId = 1,
                            referanse = "Referanse",
                            skyldner = Personident(""),
                            valutakode = "Valutakode",
                        ),
                    ),
                behandlingsreferanseListe = emptyList(),
                grunnlagListe = emptyList(),
                enhetsnummer = Enhetsnummer("1234"),
                vedtakstidspunkt = virkningstidspunkt.atDay(1).atStartOfDay(),
                unikReferanse = null,
                fastsattILand = "NO",
                innkrevingUtsattTilDato = null,
                kilde = Vedtakskilde.MANUELT,
                kildeapplikasjon = "APP",
                opprettetAv = "USER",
                opprettetAvNavn = "User Name",
                opprettetTidspunkt = LocalDateTime.now(),
                type = Vedtakstype.FASTSETTELSE,
                vedtaksid = 1,
            )

        vedtakDto.særbidragsperiode shouldBe null
    }

    @Test
    fun `skal håndtere at stønadsendringDtos periodeliste er tom`() {
        val barn =
            GrunnlagDto(
                referanse = "barn",
                type = Grunnlagstype.PERSON_SØKNADSBARN,
                innhold =
                    POJONode(
                        Person(
                            fødselsdato = LocalDate.parse("2021-01-01"),
                        ),
                    ),
            )
        val aldersjusteringGrunnlag =
            AldersjusteringDetaljerGrunnlag(
                periode = ÅrMånedsperiode(YearMonth.now().withMonth(7), null),
                grunnlagFraVedtak = 1,
                aldersjustert = true,
                aldersjusteresManuelt = false,
                begrunnelser =
                    listOf(
                        "SISTE_VEDTAK_ER_JUSTERT_NED_TIL_EVNE",
                        "SISTE_VEDTAK_ER_JUSTERT_NED_TIL_25_PROSENT_AV_INNTEKT",
                    ),
            )
        val grunnlag =
            GrunnlagDto(
                type = Grunnlagstype.ALDERSJUSTERING_DETALJER,
                innhold = POJONode(aldersjusteringGrunnlag),
                referanse = "test",
                gjelderBarnReferanse = barn.referanse,
            )

        val stønadsendringDto = opprettStønadsendring(listOf(grunnlag.referanse))

        shouldNotThrow<NoSuchElementException> { stønadsendringDto.finnSistePeriode() }
    }
}

private fun opprettStønadsendring(grunnlagListe: List<String>) =
    StønadsendringDto(
        kravhaver = Personident(""),
        skyldner = Personident(""),
        mottaker = Personident(","),
        sak = Saksnummer(""),
        type = Stønadstype.BIDRAG,
        beslutning = Beslutningstype.AVVIST,
        innkreving = Innkrevingstype.UTEN_INNKREVING,
        førsteIndeksreguleringsår = 1,
        omgjørVedtakId = null,
        eksternReferanse = "",
        grunnlagReferanseListe = grunnlagListe,
        periodeListe = emptyList(),
        sisteVedtaksid = null,
    )

private fun opprettVedtakDto(grunnlagListe: List<GrunnlagDto>) =
    VedtakDto(
        stønadsendringListe = emptyList(),
        engangsbeløpListe = emptyList(),
        behandlingsreferanseListe = emptyList(),
        grunnlagListe = grunnlagListe,
        enhetsnummer = Enhetsnummer("1234"),
        vedtakstidspunkt = LocalDateTime.parse("2020-01-01T00:00:00"),
        unikReferanse = null,
        fastsattILand = "NO",
        innkrevingUtsattTilDato = null,
        kilde = Vedtakskilde.MANUELT,
        kildeapplikasjon = "APP",
        opprettetAv = "USER",
        opprettetAvNavn = "User Name",
        opprettetTidspunkt = LocalDateTime.now(),
        type = Vedtakstype.ALDERSJUSTERING,
        vedtaksid = 1,
    )

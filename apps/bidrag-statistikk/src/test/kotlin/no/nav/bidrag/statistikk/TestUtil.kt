package no.nav.bidrag.statistikk

import com.fasterxml.jackson.databind.node.POJONode
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.enums.person.AldersgruppeForskudd
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.domene.enums.person.Sivilstandskode
import no.nav.bidrag.domene.enums.vedtak.BehandlingsrefKilde
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.Barnetillegg
import no.nav.bidrag.transport.behandling.felles.grunnlag.BostatusPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBarnIHusstand
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragsevne
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragspliktigesAndel
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningNettoBarnetillegg
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningSamværsfradrag
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningSumInntekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningUnderholdskostnad
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningVoksneIHustand
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.KopiDelberegningBidragspliktigesAndel
import no.nav.bidrag.transport.behandling.felles.grunnlag.KopiSamværsperiodeGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import no.nav.bidrag.transport.behandling.felles.grunnlag.SamværsperiodeGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.SivilstandPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningBarnebidrag
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningBarnebidragAldersjustering
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningForskudd
import no.nav.bidrag.transport.behandling.vedtak.Behandlingsreferanse
import no.nav.bidrag.transport.behandling.vedtak.Periode
import no.nav.bidrag.transport.behandling.vedtak.Sporingsdata
import no.nav.bidrag.transport.behandling.vedtak.Stønadsendring
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.behandling.vedtak.response.EngangsbeløpDto
import no.nav.bidrag.transport.behandling.vedtak.response.StønadsendringDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakPeriodeDto
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.junit.jupiter.api.Assertions
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class TestUtil {

    companion object {
        fun aClosedJsonResponse(): ResponseDefinitionBuilder = aResponse()
            .withHeader(HttpHeaders.CONNECTION, "close")
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")

        fun stubHenteVedtak(responseObjekt: VedtakDto? = null, navnResponsfil: String = "vedtak_response.json", status: HttpStatus = HttpStatus.OK) {
            val response =
                if (responseObjekt != null) {
                    aClosedJsonResponse()
                        .withStatus(status.value())
                        .withBody(
                            commonObjectmapper.writeValueAsString(
                                responseObjekt,
                            ),
                        )
                } else {
                    aClosedJsonResponse()
                        .withStatus(status.value())
                        .withBodyFile(navnResponsfil)
                }
            WireMock.stubFor(
                WireMock.get(urlMatching("/vedtak/vedtak(.*)")).willReturn(
                    response,
                ),
            )
        }

        fun byggVedtakHendelse(): VedtakHendelse = VedtakHendelse(
            kilde = Vedtakskilde.MANUELT,
            type = Vedtakstype.ENDRING,
            id = 1,
            opprettetAv = "ABCDEFG",
            opprettetAvNavn = "",
            kildeapplikasjon = "",
            vedtakstidspunkt = LocalDateTime.parse("2020-01-01T23:34:55.869121094"),
            enhetsnummer = Enhetsnummer("ABCD"),
            innkrevingUtsattTilDato = LocalDate.now(),
            fastsattILand = "NO",
            opprettetTidspunkt = LocalDateTime.parse("2021-07-06T09:31:25.007971200"),
            stønadsendringListe = listOf(
                Stønadsendring(
                    type = Stønadstype.FORSKUDD,
                    sak = Saksnummer("B"),
                    skyldner = Personident("C"),
                    kravhaver = Personident("D"),
                    mottaker = Personident("E"),
                    førsteIndeksreguleringsår = 2024,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    eksternReferanse = null,
                    listOf(
                        Periode(
                            periode = ÅrMånedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 7)),
                            beløp = BigDecimal.valueOf(1),
                            valutakode = "NOK",
                            resultatkode = "A",
                            delytelseId = "delytelseId1",
                        ),
                        Periode(
                            periode = ÅrMånedsperiode(YearMonth.of(2024, 7), null),
                            beløp = BigDecimal.valueOf(2),
                            valutakode = "NOK",
                            resultatkode = "A",
                            delytelseId = "delytelseId1",
                        ),
                    ),
                ),
            ),
            engangsbeløpListe = emptyList(),
            behandlingsreferanseListe = listOf(
                Behandlingsreferanse(
                    kilde = BehandlingsrefKilde.BISYS_SØKNAD.toString(),
                    referanse = "referanse1",
                ),
            ),
            sporingsdata = Sporingsdata("test"),
        )

        fun byggVedtakDtoForskudd(): VedtakDto = VedtakDto(
            kilde = Vedtakskilde.MANUELT,
            type = Vedtakstype.ENDRING,
            opprettetAv = "ABCDEFG",
            opprettetAvNavn = "",
            kildeapplikasjon = "",
            vedtakstidspunkt = LocalDateTime.parse("2020-01-01T23:34:55.869121094"),
            unikReferanse = null,
            enhetsnummer = Enhetsnummer("ABCD"),
            innkrevingUtsattTilDato = LocalDate.now(),
            fastsattILand = "NO",
            opprettetTidspunkt = LocalDateTime.parse("2021-07-06T09:31:25.007971200"),
            grunnlagListe = listOf(
                GrunnlagDto(
                    referanse = "person_PERSON_BIDRAGSMOTTAKER_19830916_827",
                    type = Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                    innhold = POJONode(
                        Person(
                            ident = Personident("16498311338"),
                            fødselsdato = LocalDate.parse("1983-07-18"),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                ),
                GrunnlagDto(
                    referanse = "person_PERSON_SØKNADSBARN_20180718_826",
                    type = Grunnlagstype.PERSON_SØKNADSBARN,
                    innhold = POJONode(
                        Person(
                            ident = Personident("12345678901"),
                            navn = "Ola Nordmann",
                            fødselsdato = LocalDate.parse("2018-07-18"),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                ),
                GrunnlagDto(
                    referanse = "sluttberegning_person_PERSON_SØKNADSBARN_20180718_826_202407",
                    type = Grunnlagstype.SLUTTBEREGNING_FORSKUDD,
                    innhold = POJONode(
                        SluttberegningForskudd(
                            beløp = BigDecimal.ONE,
                            resultatKode = Resultatkode.ORDINÆRT_FORSKUDD_75_PROSENT,
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            aldersgruppe = AldersgruppeForskudd.ALDER_0_10_ÅR,
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "bostatus_person_PERSON_SØKNADSBARN_20180718_826_20240201",
                        "delberegning_DELBEREGNING_BARN_I_HUSSTAND_person_PERSON_BIDRAGSMOTTAKER_19830916_827_person_PERSON_SØKNADSBARN_20180718_826_202402",
                        "delberegning_DELBEREGNING_SUM_INNTEKT_person_PERSON_BIDRAGSMOTTAKER_19900916_827_person_PERSON_SØKNADSBARN_20150718_826_202503-",
                        "person_PERSON_SØKNADSBARN_20180718_826",
                        "sivilstand_person_PERSON_BIDRAGSMOTTAKER_19830916_827_20240101",
                    ),
                    gjelderReferanse = "person_PERSON_SØKNADSBARN_20180718_826",
                ),
                GrunnlagDto(
                    referanse = "sivilstand_person_PERSON_BIDRAGSMOTTAKER_19830916_827_20240101",
                    type = Grunnlagstype.SIVILSTAND_PERIODE,
                    innhold = POJONode(
                        SivilstandPeriode(
                            manueltRegistrert = true,
                            sivilstand = Sivilstandskode.BOR_ALENE_MED_BARN,
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                    gjelderReferanse = "person_PERSON_BIDRAGSMOTTAKER_19830916_827",
                ),
                GrunnlagDto(
                    referanse = "inntekt_LØNN_MANUELT_BEREGNET_person_PERSON_BIDRAGSMOTTAKER_19830916_827_20240101_2868",
                    type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                    innhold = POJONode(
                        InntektsrapporteringPeriode(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            manueltRegistrert = true,
                            valgt = true,
                            inntektsrapportering = Inntektsrapportering.LØNN_MANUELT_BEREGNET,
                            beløp = BigDecimal.valueOf(1000),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                    gjelderReferanse = "person_PERSON_BIDRAGSMOTTAKER_19830916_827",
                ),
                GrunnlagDto(
                    referanse = "delberegning_DELBEREGNING_SUM_INNTEKT_person_PERSON_BIDRAGSMOTTAKER_19900916_827_person_PERSON_SØKNADSBARN_20150718_826_202503-",
                    type = Grunnlagstype.DELBEREGNING_SUM_INNTEKT,
                    innhold = POJONode(
                        DelberegningSumInntekt(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            totalinntekt = BigDecimal.valueOf(1000),
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "inntekt_LØNN_MANUELT_BEREGNET_person_PERSON_BIDRAGSMOTTAKER_19830916_827_20240101_2868",
                    ),
                    gjelderReferanse = "person_PERSON_BIDRAGSMOTTAKER_19830916_827",
                ),
                GrunnlagDto(
                    referanse = "delberegning_DELBEREGNING_BARN_I_HUSSTAND_person_PERSON_BIDRAGSMOTTAKER_19830916_827_person_PERSON_SØKNADSBARN_20180718_826_202402",
                    type = Grunnlagstype.DELBEREGNING_BARN_I_HUSSTAND,
                    innhold = POJONode(
                        DelberegningBarnIHusstand(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            antallBarn = 1.0,
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "bostatus_person_PERSON_HUSSTANDSMEDLEM_20151210_964_20240201",
                        "bostatus_person_PERSON_SØKNADSBARN_20180718_826_20240201",
                    ),
                ),
                GrunnlagDto(
                    referanse = "bostatus_person_PERSON_SØKNADSBARN_20180718_826_20240201",
                    type = Grunnlagstype.BOSTATUS_PERIODE,
                    innhold = POJONode(
                        BostatusPeriode(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            bostatus = Bostatuskode.MED_FORELDER,
                            relatertTilPart = "person_PERSON_BIDRAGSMOTTAKER_19830916_827",
                            manueltRegistrert = false,
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "innhentet_husstandsmedlem_person_PERSON_BIDRAGSMOTTAKER_19830916_827_person_PERSON_SØKNADSBARN_20180718_826",
                    ),
                    gjelderReferanse = "person_PERSON_SØKNADSBARN_20180718_826",
                ),
            ),
            stønadsendringListe = listOf(
                StønadsendringDto(
                    type = Stønadstype.FORSKUDD,
                    sak = Saksnummer("B"),
                    skyldner = Personident("C"),
                    kravhaver = Personident("D"),
                    mottaker = Personident("E"),
                    sisteVedtaksid = null,
                    førsteIndeksreguleringsår = 2024,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    eksternReferanse = null,
                    grunnlagReferanseListe = emptyList(),
                    periodeListe = listOf(
                        VedtakPeriodeDto(
                            periode = ÅrMånedsperiode(YearMonth.of(2024, 7), YearMonth.of(2024, 8)),
                            beløp = BigDecimal.valueOf(2),
                            valutakode = "NOK",
                            resultatkode = "A",
                            delytelseId = "delytelseId1",
                            grunnlagReferanseListe = listOf(
                                "sluttberegning_person_PERSON_SØKNADSBARN_20180718_826_202407",
                            ),
                        ),
                    ),
                ),
                StønadsendringDto(
                    type = Stønadstype.FORSKUDD,
                    sak = Saksnummer("B"),
                    skyldner = Personident("C"),
                    kravhaver = Personident("D"),
                    mottaker = Personident("E"),
                    sisteVedtaksid = null,
                    førsteIndeksreguleringsår = 2024,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    eksternReferanse = null,
                    grunnlagReferanseListe = emptyList(),
                    periodeListe = listOf(
                        VedtakPeriodeDto(
                            periode = ÅrMånedsperiode(YearMonth.of(2024, 8), null),
                            beløp = BigDecimal.valueOf(2),
                            valutakode = "NOK",
                            resultatkode = "A",
                            delytelseId = "delytelseId1",
                            grunnlagReferanseListe = listOf(
                                "sluttberegning_person_PERSON_SØKNADSBARN_20180718_826_202407",
                            ),
                        ),
                    ),
                ),
            ),
            engangsbeløpListe = emptyList(),
            behandlingsreferanseListe = emptyList(),
        )

        fun byggVedtakDtoUtenGrunnlag(): VedtakDto = VedtakDto(
            kilde = Vedtakskilde.MANUELT,
            type = Vedtakstype.ENDRING,
            opprettetAv = "ABCDEFG",
            opprettetAvNavn = "",
            kildeapplikasjon = "",
            vedtakstidspunkt = LocalDateTime.parse("2020-01-01T23:34:55.869121094"),
            unikReferanse = null,
            enhetsnummer = Enhetsnummer("ABCD"),
            innkrevingUtsattTilDato = LocalDate.now(),
            fastsattILand = "NO",
            opprettetTidspunkt = LocalDateTime.parse("2021-07-06T09:31:25.007971200"),
            grunnlagListe = emptyList(),
            stønadsendringListe = listOf(
                StønadsendringDto(
                    type = Stønadstype.FORSKUDD,
                    sak = Saksnummer("B"),
                    skyldner = Personident("C"),
                    kravhaver = Personident("D"),
                    mottaker = Personident("E"),
                    sisteVedtaksid = null,
                    førsteIndeksreguleringsår = 2024,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    beslutning = Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    eksternReferanse = null,
                    grunnlagReferanseListe = emptyList(),
                    periodeListe = listOf(
                        VedtakPeriodeDto(
                            periode = ÅrMånedsperiode(YearMonth.of(2024, 7), null),
                            beløp = BigDecimal.valueOf(2),
                            valutakode = "NOK",
                            resultatkode = "A",
                            delytelseId = "delytelseId1",
                            grunnlagReferanseListe = listOf(
                                "sluttberegning_person_PERSON_SØKNADSBARN_20180718_826_202407",
                            ),
                        ),
                    ),
                ),
                StønadsendringDto(
                    type = Stønadstype.BIDRAG,
                    sak = Saksnummer("B"),
                    skyldner = Personident("C"),
                    kravhaver = Personident("D"),
                    mottaker = Personident("E"),
                    sisteVedtaksid = null,
                    førsteIndeksreguleringsår = 2024,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    beslutning = Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    eksternReferanse = null,
                    grunnlagReferanseListe = emptyList(),
                    periodeListe = listOf(
                        VedtakPeriodeDto(
                            periode = ÅrMånedsperiode(YearMonth.of(2024, 7), null),
                            beløp = BigDecimal.valueOf(2),
                            valutakode = "NOK",
                            resultatkode = "A",
                            delytelseId = "delytelseId1",
                            grunnlagReferanseListe = listOf(
                                "sluttberegning_person_PERSON_SØKNADSBARN_20180718_826_202407",
                            ),
                        ),
                    ),
                ),
            ),
            engangsbeløpListe = emptyList(),
            behandlingsreferanseListe = emptyList(),
        )

        fun byggVedtakDtoUtenForskuddOgBidrag(): VedtakDto = VedtakDto(
            kilde = Vedtakskilde.MANUELT,
            type = Vedtakstype.ENDRING,
            opprettetAv = "ABCDEFG",
            opprettetAvNavn = "",
            kildeapplikasjon = "",
            vedtakstidspunkt = LocalDateTime.parse("2020-01-01T23:34:55.869121094"),
            unikReferanse = null,
            enhetsnummer = Enhetsnummer("ABCD"),
            innkrevingUtsattTilDato = LocalDate.now(),
            fastsattILand = "NO",
            opprettetTidspunkt = LocalDateTime.parse("2021-07-06T09:31:25.007971200"),
            grunnlagListe = emptyList(),
            stønadsendringListe = emptyList(),
            engangsbeløpListe = listOf(
                EngangsbeløpDto(
                    type = Engangsbeløptype.SAERTILSKUDD,
                    sak = Saksnummer("B"),
                    skyldner = Personident("C"),
                    kravhaver = Personident("D"),
                    mottaker = Personident("E"),
                    beløp = BigDecimal.ZERO,
                    valutakode = "NOK",
                    resultatkode = "A",
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    referanse = "",
                    delytelseId = "delytelseId1",
                    eksternReferanse = null,
                    grunnlagReferanseListe = emptyList(),
                    betaltBeløp = BigDecimal.ZERO,
                ),
            ),
            behandlingsreferanseListe = emptyList(),
        )

        fun byggVedtakDtoBidrag(kildeapplikasjon: String): VedtakDto = VedtakDto(
            kilde = Vedtakskilde.MANUELT,
            type = Vedtakstype.ENDRING,
            opprettetAv = "ABCDEFG",
            opprettetAvNavn = "",
            kildeapplikasjon = kildeapplikasjon,
            vedtakstidspunkt = LocalDateTime.parse("2020-01-01T23:34:55.869121094"),
            unikReferanse = null,
            enhetsnummer = Enhetsnummer("ABCD"),
            innkrevingUtsattTilDato = LocalDate.now(),
            fastsattILand = "NO",
            opprettetTidspunkt = LocalDateTime.parse("2020-01-01T23:34:55.869121094"),
            grunnlagListe = listOf(
                GrunnlagDto(
                    referanse = "person_PERSON_BIDRAGSPLIKTIG_19800417_825",
                    type = Grunnlagstype.PERSON_BIDRAGSPLIKTIG,
                    innhold = POJONode(
                        Person(
                            ident = Personident("98765432109"),
                            fødselsdato = LocalDate.parse("2001-02-17"),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                ),
                GrunnlagDto(
                    referanse = "person_PERSON_BIDRAGSMOTTAKER_19900916_827",
                    type = Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                    innhold = POJONode(
                        Person(
                            ident = Personident("16498311338"),
                            fødselsdato = LocalDate.parse("1983-07-18"),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                ),
                GrunnlagDto(
                    referanse = "person_PERSON_SØKNADSBARN_20150718_826",
                    type = Grunnlagstype.PERSON_SØKNADSBARN,
                    innhold = POJONode(
                        Person(
                            ident = Personident("12345678901"),
                            navn = "Ola Nordmann",
                            fødselsdato = LocalDate.parse("2018-07-18"),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                ),
                GrunnlagDto(
                    referanse = "sluttberegning_person_PERSON_SØKNADSBARN_20150718_826_202501",
                    type = Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG,
                    innhold = POJONode(
                        SluttberegningBarnebidrag(
                            periode = ÅrMånedsperiode(YearMonth.of(2025, 1), YearMonth.of(2025, 3)),
                            beregnetBeløp = BigDecimal.ONE,
                            resultatBeløp = BigDecimal.valueOf(100),
                            uMinusNettoBarnetilleggBM = BigDecimal.ONE,
                            bruttoBidragEtterBarnetilleggBM = BigDecimal.ONE,
                            nettoBidragEtterBarnetilleggBM = BigDecimal.ONE,
                            bruttoBidragJustertForEvneOg25Prosent = BigDecimal.ONE,
                            bruttoBidragEtterBarnetilleggBP = BigDecimal.ONE,
                            nettoBidragEtterSamværsfradrag = BigDecimal.ONE,
                            bpAndelAvUVedDeltBostedFaktor = BigDecimal.ONE,
                            bpAndelAvUVedDeltBostedBeløp = BigDecimal.ONE,
                            barnetErSelvforsørget = true,
                            bidragJustertForDeltBosted = true,
                            bidragJustertForNettoBarnetilleggBP = true,
                            bidragJustertForNettoBarnetilleggBM = true,
                            bidragJustertNedTilEvne = true,
                            bidragJustertNedTil25ProsentAvInntekt = true,
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "DELBEREGNING_NETTO_BARNETILLEGG_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202501_202503",
                        "delberegning_DELBEREGNING_BIDRAGSEVNE_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202402_202503",
                        "DELBEREGNING_UNDERHOLDSKOSTNAD_person_PERSON_BIDRAGSMOTTAKER_19900916_827_person_PERSON_SØKNADSBARN_20150718_826_202401",
                        "DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL_UNDERHOLDSKOSTNAD_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202401",
                        "DELBEREGNING_SAMVÆRSFRADRAG_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202401",
                        "DELBEREGNING_VOKSNE_I_HUSSTAND_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202401",
                        "SAMVÆRSPERIODE_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_20240201",
                        "person_PERSON_BIDRAGSPLIKTIG_19800417_825",
                        "person_PERSON_BIDRAGSMOTTAKER_19900916_827",
                        "person_PERSON_SØKNADSBARN_20150718_826",
                    ),
                    gjelderBarnReferanse = "person_PERSON_SØKNADSBARN_20150718_826",
                ),
                GrunnlagDto(
                    referanse = "sluttberegning_person_PERSON_SØKNADSBARN_20150718_826_202503-",
                    type = Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG,
                    innhold = POJONode(
                        SluttberegningBarnebidrag(
                            periode = ÅrMånedsperiode(YearMonth.of(2025, 3), null),
                            beregnetBeløp = BigDecimal.ONE,
                            resultatBeløp = BigDecimal.valueOf(200),
                            uMinusNettoBarnetilleggBM = BigDecimal.ONE,
                            bruttoBidragEtterBarnetilleggBM = BigDecimal.ONE,
                            nettoBidragEtterBarnetilleggBM = BigDecimal.ONE,
                            bruttoBidragJustertForEvneOg25Prosent = BigDecimal.ONE,
                            bruttoBidragEtterBarnetilleggBP = BigDecimal.ONE,
                            nettoBidragEtterSamværsfradrag = BigDecimal.ONE,
                            bpAndelAvUVedDeltBostedFaktor = BigDecimal.ONE,
                            bpAndelAvUVedDeltBostedBeløp = BigDecimal.ONE,
                            barnetErSelvforsørget = true,
                            bidragJustertForDeltBosted = true,
                            bidragJustertForNettoBarnetilleggBP = true,
                            bidragJustertForNettoBarnetilleggBM = true,
                            bidragJustertNedTilEvne = true,
                            bidragJustertNedTil25ProsentAvInntekt = true,
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "DELBEREGNING_NETTO_BARNETILLEGG_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202503-",
                        "delberegning_DELBEREGNING_BIDRAGSEVNE_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202503",
                        "DELBEREGNING_UNDERHOLDSKOSTNAD_person_PERSON_BIDRAGSMOTTAKER_19900916_827_person_PERSON_SØKNADSBARN_20150718_826_202401",
                        "DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL_UNDERHOLDSKOSTNAD_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202503",
                        "DELBEREGNING_SAMVÆRSFRADRAG_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202401",
                        "DELBEREGNING_VOKSNE_I_HUSSTAND_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202401",
                        "SAMVÆRSPERIODE_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_20240201",
                        "person_PERSON_BIDRAGSPLIKTIG_19800417_825",
                        "person_PERSON_BIDRAGSMOTTAKER_19900916_827",
                        "person_PERSON_SØKNADSBARN_20150718_826",
                    ),
                    gjelderBarnReferanse = "person_PERSON_SØKNADSBARN_20150718_826",
                ),
                GrunnlagDto(
                    referanse = "inntekt_LØNN_MANUELT_BEREGNET_person_PERSON_BIDRAGSPLIKTIG_19800417_825_20240101_2868",
                    type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                    innhold = POJONode(
                        InntektsrapporteringPeriode(
                            periode = ÅrMånedsperiode(YearMonth.of(2025, 1), null),
                            manueltRegistrert = true,
                            valgt = true,
                            inntektsrapportering = Inntektsrapportering.LØNN_MANUELT_BEREGNET,
                            beløp = BigDecimal.valueOf(2000),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                    gjelderReferanse = "person_PERSON_BIDRAGSPLIKTIG_19800417_825",
                ),
                GrunnlagDto(
                    referanse = "inntekt_SAKSBEHANDLER_BEREGNET_INNTEKT_person_PERSON_BIDRAGSPLIKTIG_19800417_825_20240101_2868",
                    type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                    innhold = POJONode(
                        InntektsrapporteringPeriode(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            manueltRegistrert = true,
                            valgt = false,
                            inntektsrapportering = Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT,
                            beløp = BigDecimal.valueOf(17000),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                    gjelderReferanse = "person_PERSON_BIDRAGSPLIKTIG_19800417_825",
                ),
                GrunnlagDto(
                    referanse = "delberegning_DELBEREGNING_SUM_INNTEKT_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202501_202503",
                    type = Grunnlagstype.DELBEREGNING_SUM_INNTEKT,
                    innhold = POJONode(
                        DelberegningSumInntekt(
                            periode = ÅrMånedsperiode(YearMonth.of(2025, 1), YearMonth.of(2025, 3)),
                            totalinntekt = BigDecimal.valueOf(2000),
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "inntekt_LØNN_MANUELT_BEREGNET_person_PERSON_BIDRAGSPLIKTIG_19800417_825_20240101_2868",
                        "INNTEKT_RAPPORTERING_PERIODE_BARNETILLEGG-person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202407_202503",
                    ),
                    gjelderReferanse = "person_PERSON_BIDRAGSPLIKTIG_19800417_825",
                ),
                GrunnlagDto(
                    referanse = "delberegning_DELBEREGNING_SUM_INNTEKT_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202503-",
                    type = Grunnlagstype.DELBEREGNING_SUM_INNTEKT,
                    innhold = POJONode(
                        DelberegningSumInntekt(
                            periode = ÅrMånedsperiode(YearMonth.of(2025, 3), null),
                            totalinntekt = BigDecimal.valueOf(2000),
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "inntekt_LØNN_MANUELT_BEREGNET_person_PERSON_BIDRAGSPLIKTIG_19800417_825_20240101_2868",
                        "INNTEKT_RAPPORTERING_PERIODE_BARNETILLEGG-person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202503-",
                    ),
                    gjelderReferanse = "person_PERSON_BIDRAGSPLIKTIG_19800417_825",
                ),
                GrunnlagDto(
                    referanse = "inntekt_LØNN_MANUELT_BEREGNET_person_PERSON_BIDRAGSMOTTAKER_19900916_827_20240101_2868",
                    type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                    innhold = POJONode(
                        InntektsrapporteringPeriode(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            manueltRegistrert = true,
                            valgt = true,
                            inntektsrapportering = Inntektsrapportering.LØNN_MANUELT_BEREGNET,
                            beløp = BigDecimal.valueOf(2500),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                    gjelderReferanse = "person_PERSON_BIDRAGSMOTTAKER_19900916_827",
                ),
                GrunnlagDto(
                    referanse = "delberegning_DELBEREGNING_SUM_INNTEKT_person_PERSON_BIDRAGSMOTTAKER_19900916_827_person_PERSON_SØKNADSBARN_20150718_826_202503-",
                    type = Grunnlagstype.DELBEREGNING_SUM_INNTEKT,
                    innhold = POJONode(
                        DelberegningSumInntekt(
                            periode = ÅrMånedsperiode(YearMonth.of(2025, 3), null),
                            totalinntekt = BigDecimal.valueOf(2500),
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "inntekt_LØNN_MANUELT_BEREGNET_person_PERSON_BIDRAGSMOTTAKER_19900916_827_20240101_2868",
                    ),
                    gjelderReferanse = "person_PERSON_BIDRAGSMOTTAKER_19900916_827",
                ),
                GrunnlagDto(
                    referanse = "delberegning_DELBEREGNING_BIDRAGSEVNE_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202402_202503",
                    type = Grunnlagstype.DELBEREGNING_BIDRAGSEVNE,
                    innhold = POJONode(
                        DelberegningBidragsevne(
                            periode = ÅrMånedsperiode(YearMonth.of(2024, 2), YearMonth.of(2025, 3)),
                            beløp = BigDecimal.valueOf(3500),
                            skatt = DelberegningBidragsevne.Skatt(
                                minstefradrag = BigDecimal.valueOf(1000),
                                skattAlminneligInntekt = BigDecimal.valueOf(1000),
                                trinnskatt = BigDecimal.valueOf(1000),
                                trygdeavgift = BigDecimal.valueOf(100),
                                sumSkatt = BigDecimal.valueOf(50),
                                sumSkattFaktor = BigDecimal.valueOf(100),
                            ),
                            underholdBarnEgenHusstand = BigDecimal.valueOf(100),
                            sumInntekt25Prosent = BigDecimal.valueOf(100),
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "delberegning_DELBEREGNING_SUM_INNTEKT_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202501_202503",
                    ),
                    gjelderReferanse = "person_PERSON_BIDRAGSPLIKTIG_19800417_825",
                ),
                GrunnlagDto(
                    referanse = "delberegning_DELBEREGNING_BIDRAGSEVNE_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202503",
                    type = Grunnlagstype.DELBEREGNING_BIDRAGSEVNE,
                    innhold = POJONode(
                        DelberegningBidragsevne(
                            periode = ÅrMånedsperiode(YearMonth.of(2025, 3), null),
                            beløp = BigDecimal.valueOf(3500),
                            skatt = DelberegningBidragsevne.Skatt(
                                minstefradrag = BigDecimal.valueOf(1000),
                                skattAlminneligInntekt = BigDecimal.valueOf(1000),
                                trinnskatt = BigDecimal.valueOf(1000),
                                trygdeavgift = BigDecimal.valueOf(100),
                                sumSkatt = BigDecimal.valueOf(50),
                                sumSkattFaktor = BigDecimal.valueOf(100),
                            ),
                            underholdBarnEgenHusstand = BigDecimal.valueOf(100),
                            sumInntekt25Prosent = BigDecimal.valueOf(100),
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "delberegning_DELBEREGNING_SUM_INNTEKT_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202503-",
                    ),
                    gjelderReferanse = "person_PERSON_BIDRAGSPLIKTIG_19800417_825",
                ),
                GrunnlagDto(
                    referanse = "DELBEREGNING_UNDERHOLDSKOSTNAD_person_PERSON_BIDRAGSMOTTAKER_19900916_827_person_PERSON_SØKNADSBARN_20150718_826_202401",
                    type = Grunnlagstype.DELBEREGNING_UNDERHOLDSKOSTNAD,
                    innhold = POJONode(
                        DelberegningUnderholdskostnad(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            forbruksutgift = BigDecimal.valueOf(1000),
                            boutgift = BigDecimal.valueOf(1000),
                            barnetilsynMedStønad = BigDecimal.valueOf(100),
                            nettoTilsynsutgift = BigDecimal.valueOf(100),
                            barnetrygd = BigDecimal.valueOf(100),
                            underholdskostnad = BigDecimal.valueOf(500),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                    gjelderReferanse = "person_PERSON_SØKNADSBARN_20150718_826",
                ),
                GrunnlagDto(
                    referanse = "DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL_UNDERHOLDSKOSTNAD_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202401",
                    type = Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL,
                    innhold = POJONode(
                        DelberegningBidragspliktigesAndel(
                            periode = ÅrMånedsperiode(YearMonth.of(2024, 1), YearMonth.of(2025, 3)),
                            endeligAndelFaktor = BigDecimal.valueOf(1000),
                            andelBeløp = BigDecimal.valueOf(400),
                            beregnetAndelFaktor = BigDecimal.valueOf(100),
                            barnEndeligInntekt = BigDecimal.valueOf(100),
                            barnetErSelvforsørget = false,
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "delberegning_DELBEREGNING_SUM_INNTEKT_person_PERSON_BIDRAGSMOTTAKER_19900916_827_person_PERSON_SØKNADSBARN_20150718_826_202503-",
                        "delberegning_DELBEREGNING_SUM_INNTEKT_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202501_202503",
                        "DELBEREGNING_UNDERHOLDSKOSTNAD_person_PERSON_BIDRAGSMOTTAKER_19900916_827_person_PERSON_SØKNADSBARN_20150718_826_202401",
                    ),
                    gjelderReferanse = "person_PERSON_SØKNADSBARN_20150718_826",
                ),
                GrunnlagDto(
                    referanse = "DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL_UNDERHOLDSKOSTNAD_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202503",
                    type = Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL,
                    innhold = POJONode(
                        DelberegningBidragspliktigesAndel(
                            periode = ÅrMånedsperiode(YearMonth.of(2025, 3), null),
                            endeligAndelFaktor = BigDecimal.valueOf(1000),
                            andelBeløp = BigDecimal.valueOf(200),
                            beregnetAndelFaktor = BigDecimal.valueOf(100),
                            barnEndeligInntekt = BigDecimal.valueOf(100),
                            barnetErSelvforsørget = false,
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "delberegning_DELBEREGNING_SUM_INNTEKT_person_PERSON_BIDRAGSMOTTAKER_19900916_827_person_PERSON_SØKNADSBARN_20150718_826_202503-",
                        "delberegning_DELBEREGNING_SUM_INNTEKT_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202503-",
                        "DELBEREGNING_UNDERHOLDSKOSTNAD_person_PERSON_BIDRAGSMOTTAKER_19900916_827_person_PERSON_SØKNADSBARN_20150718_826_202401",
                    ),
                    gjelderReferanse = "person_PERSON_SØKNADSBARN_20150718_826",
                ),
                GrunnlagDto(
                    referanse = "DELBEREGNING_SAMVÆRSFRADRAG_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202401",
                    type = Grunnlagstype.DELBEREGNING_SAMVÆRSFRADRAG,
                    innhold = POJONode(
                        DelberegningSamværsfradrag(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            beløp = BigDecimal.valueOf(150),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                    gjelderReferanse = "person_PERSON_SØKNADSBARN_20150718_826",
                ),
                GrunnlagDto(
                    referanse = "INNTEKT_RAPPORTERING_PERIODE_BARNETILLEGG-person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202407_202503",
                    type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                    innhold = POJONode(
                        InntektsrapporteringPeriode(
                            periode = ÅrMånedsperiode(YearMonth.of(2024, 7), YearMonth.of(2025, 3)),
                            manueltRegistrert = true,
                            valgt = true,
                            inntektsrapportering = Inntektsrapportering.BARNETILLEGG,
                            beløp = BigDecimal.valueOf(1000),
                            gjelderBarn = "person_PERSON_SØKNADSBARN_20150718_826",
                            inntektspostListe = listOf(
                                InntektsrapporteringPeriode.Inntektspost(
                                    kode = "BARNETILLEGG",
                                    inntektstype = Inntektstype.BARNETILLEGG_SUMMERT,
                                    beløp = BigDecimal.valueOf(1000),
                                ),
                            ),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                    gjelderReferanse = "person_PERSON_BIDRAGSPLIKTIG_19800417_825",
                    gjelderBarnReferanse = "person_PERSON_SØKNADSBARN_20150718_826",
                ),
                GrunnlagDto(
                    referanse = "INNTEKT_RAPPORTERING_PERIODE_BARNETILLEGG-person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202503-",
                    type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                    innhold = POJONode(
                        InntektsrapporteringPeriode(
                            periode = ÅrMånedsperiode(YearMonth.of(2025, 3), null),
                            manueltRegistrert = true,
                            valgt = true,
                            inntektsrapportering = Inntektsrapportering.BARNETILLEGG,
                            beløp = BigDecimal.valueOf(1700),
                            gjelderBarn = "person_PERSON_SØKNADSBARN_20150718_826",
                            inntektspostListe = listOf(
                                InntektsrapporteringPeriode.Inntektspost(
                                    kode = "BARNETILLEGG",
                                    inntektstype = Inntektstype.BARNETILLEGG_SUMMERT,
                                    beløp = BigDecimal.valueOf(1700),
                                ),
                            ),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                    gjelderReferanse = "person_PERSON_BIDRAGSPLIKTIG_19800417_825",
                    gjelderBarnReferanse = "person_PERSON_SØKNADSBARN_20150718_826",
                ),
                GrunnlagDto(
                    referanse = "DELBEREGNING_NETTO_BARNETILLEGG_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202501_202503",
                    type = Grunnlagstype.DELBEREGNING_NETTO_BARNETILLEGG,
                    innhold = POJONode(
                        DelberegningNettoBarnetillegg(
                            periode = ÅrMånedsperiode(YearMonth.of(2025, 1), YearMonth.of(2025, 3)),
                            summertNettoBarnetillegg = BigDecimal.valueOf(500),
                            summertBruttoBarnetillegg = BigDecimal.valueOf(1000),
                            barnetilleggTypeListe = listOf(
                                Barnetillegg(
                                    barnetilleggType = Inntektstype.BARNETILLEGG_SUMMERT,
                                    bruttoBarnetillegg = BigDecimal.valueOf(500),
                                    nettoBarnetillegg = BigDecimal.valueOf(1000),
                                ),
                            ),
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "INNTEKT_RAPPORTERING_PERIODE_BARNETILLEGG-person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202407_202503",
                    ),
                    gjelderReferanse = "person_PERSON_BIDRAGSPLIKTIG_19800417_825",
                    gjelderBarnReferanse = "person_PERSON_SØKNADSBARN_20150718_826",
                ),
                GrunnlagDto(
                    referanse = "DELBEREGNING_NETTO_BARNETILLEGG_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202503-",
                    type = Grunnlagstype.DELBEREGNING_NETTO_BARNETILLEGG,
                    innhold = POJONode(
                        DelberegningNettoBarnetillegg(
                            periode = ÅrMånedsperiode(YearMonth.of(2025, 3), null),
                            summertNettoBarnetillegg = BigDecimal.valueOf(1200),
                            summertBruttoBarnetillegg = BigDecimal.valueOf(1700),
                            barnetilleggTypeListe = listOf(
                                Barnetillegg(
                                    barnetilleggType = Inntektstype.BARNETILLEGG_SUMMERT,
                                    bruttoBarnetillegg = BigDecimal.valueOf(1200),
                                    nettoBarnetillegg = BigDecimal.valueOf(1700),
                                ),
                            ),
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "INNTEKT_RAPPORTERING_PERIODE_BARNETILLEGG-person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202503-",
                    ),
                    gjelderReferanse = "person_PERSON_BIDRAGSPLIKTIG_19800417_825",
                    gjelderBarnReferanse = "person_PERSON_SØKNADSBARN_20150718_826",
                ),
                GrunnlagDto(
                    referanse = "DELBEREGNING_NETTO_BARNETILLEGG_person_PERSON_BIDRAGSMOTTAKER_19900916_827_person_PERSON_SØKNADSBARN_20150718_826_202401",
                    type = Grunnlagstype.DELBEREGNING_NETTO_BARNETILLEGG,
                    innhold = POJONode(
                        DelberegningNettoBarnetillegg(
                            periode = ÅrMånedsperiode(YearMonth.of(2025, 3), null),
                            summertNettoBarnetillegg = BigDecimal.valueOf(10),
                            summertBruttoBarnetillegg = BigDecimal.valueOf(15),
                            barnetilleggTypeListe = listOf(
                                Barnetillegg(
                                    barnetilleggType = Inntektstype.BARNETILLEGG_SUMMERT,
                                    bruttoBarnetillegg = BigDecimal.valueOf(10),
                                    nettoBarnetillegg = BigDecimal.valueOf(15),
                                ),
                            ),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                    gjelderReferanse = "person_PERSON_BIDRAGSMOTTAKER_19900916_827",
                    gjelderBarnReferanse = "person_PERSON_SØKNADSBARN_20150718_826",
                ),
                GrunnlagDto(
                    referanse = "DELBEREGNING_VOKSNE_I_HUSSTAND_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_202401",
                    type = Grunnlagstype.DELBEREGNING_VOKSNE_I_HUSSTAND,
                    innhold = POJONode(
                        DelberegningVoksneIHustand(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            borMedAndreVoksne = true,
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                    gjelderReferanse = "person_PERSON_BIDRAGSPLIKTIG_19800417_825",
                    gjelderBarnReferanse = "person_PERSON_SØKNADSBARN_20150718_826",
                ),
                GrunnlagDto(
                    referanse = "SAMVÆRSPERIODE_person_PERSON_BIDRAGSPLIKTIG_19800417_825_person_PERSON_SØKNADSBARN_20150718_826_20240201",
                    type = Grunnlagstype.SAMVÆRSPERIODE,
                    innhold = POJONode(
                        SamværsperiodeGrunnlag(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            samværsklasse = Samværsklasse.DELT_BOSTED,
                            manueltRegistrert = false,
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "innhentet_husstandsmedlem_person_PERSON_BIDRAGSMOTTAKER_19900916_827_person_PERSON_SØKNADSBARN_20150718_826",
                    ),
                    gjelderBarnReferanse = "person_PERSON_SØKNADSBARN_20150718_826",
                    gjelderReferanse = "person_PERSON_BIDRAGSPLIKTIG_19800417_825",
                ),
            ),
            stønadsendringListe = listOf(
                StønadsendringDto(
                    type = Stønadstype.BIDRAG,
                    sak = Saksnummer("1234567"),
                    skyldner = Personident("98765432109"),
                    kravhaver = Personident("12345678901"),
                    mottaker = Personident("16498311338"),
                    sisteVedtaksid = null,
                    førsteIndeksreguleringsår = 2026,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    beslutning = Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    eksternReferanse = null,
                    grunnlagReferanseListe = emptyList(),
                    periodeListe = listOf(
                        VedtakPeriodeDto(
                            periode = ÅrMånedsperiode(YearMonth.of(2025, 1), YearMonth.of(2025, 3)),
                            beløp = BigDecimal.valueOf(100),
                            valutakode = "NOK",
                            resultatkode = "A",
                            delytelseId = "delytelseId1",
                            grunnlagReferanseListe = listOf(
                                "sluttberegning_person_PERSON_SØKNADSBARN_20150718_826_202501",
                            ),
                        ),
                        VedtakPeriodeDto(
                            periode = ÅrMånedsperiode(YearMonth.of(2025, 3), null),
                            beløp = BigDecimal.valueOf(200),
                            valutakode = "NOK",
                            resultatkode = "A",
                            delytelseId = "delytelseId1",
                            grunnlagReferanseListe = listOf(
                                "sluttberegning_person_PERSON_SØKNADSBARN_20150718_826_202503-",
                            ),
                        ),
                    ),
                ),
            ),
            engangsbeløpListe = emptyList(),
            behandlingsreferanseListe = emptyList(),
        )

        fun byggVedtakDtoBidragUtenGrunnlag(): VedtakDto = VedtakDto(
            kilde = Vedtakskilde.MANUELT,
            type = Vedtakstype.ENDRING,
            opprettetAv = "ABCDEFG",
            opprettetAvNavn = "",
            kildeapplikasjon = "bidrag-behandling",
            vedtakstidspunkt = LocalDateTime.parse("2020-01-01T23:34:55.869121094"),
            unikReferanse = null,
            enhetsnummer = Enhetsnummer("ABCD"),
            innkrevingUtsattTilDato = LocalDate.now(),
            fastsattILand = "NO",
            opprettetTidspunkt = LocalDateTime.parse("2020-01-01T23:34:55.869121094"),
            grunnlagListe = emptyList(),
            stønadsendringListe = listOf(
                StønadsendringDto(
                    type = Stønadstype.BIDRAG,
                    sak = Saksnummer("1234567"),
                    skyldner = Personident("98765432109"),
                    kravhaver = Personident("12345678901"),
                    mottaker = Personident("16498311338"),
                    sisteVedtaksid = null,
                    førsteIndeksreguleringsår = 2024,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    beslutning = Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    eksternReferanse = null,
                    grunnlagReferanseListe = emptyList(),
                    periodeListe = listOf(
                        VedtakPeriodeDto(
                            periode = ÅrMånedsperiode(YearMonth.of(2024, 7), YearMonth.of(2024, 8)),
                            beløp = BigDecimal.valueOf(2),
                            valutakode = "NOK",
                            resultatkode = "A",
                            delytelseId = "delytelseId1",
                            grunnlagReferanseListe = listOf(
                                "sluttberegning_person_PERSON_SØKNADSBARN_20180718_826_202407",
                            ),
                        ),
                    ),
                ),
            ),
            engangsbeløpListe = emptyList(),
            behandlingsreferanseListe = emptyList(),
        )

        fun byggVedtakDtoAldersjusteringBidrag(): VedtakDto = VedtakDto(
            kilde = Vedtakskilde.AUTOMATISK,
            type = Vedtakstype.ALDERSJUSTERING,
            opprettetAv = "ABCDEFG",
            opprettetAvNavn = "",
            kildeapplikasjon = "bidrag-automatisk-jobb",
            vedtakstidspunkt = LocalDateTime.parse("2020-01-01T23:34:55.869121094"),
            unikReferanse = null,
            enhetsnummer = Enhetsnummer("ABCD"),
            innkrevingUtsattTilDato = LocalDate.now(),
            fastsattILand = "NO",
            opprettetTidspunkt = LocalDateTime.parse("2020-01-01T23:34:55.869121094"),
            grunnlagListe = listOf(
                GrunnlagDto(
                    referanse = "person_PERSON_BIDRAGSPLIKTIG_20010217_825",
                    type = Grunnlagstype.PERSON_BIDRAGSPLIKTIG,
                    innhold = POJONode(
                        Person(
                            ident = Personident("98765432109"),
                            fødselsdato = LocalDate.parse("2001-02-17"),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                ),
                GrunnlagDto(
                    referanse = "person_PERSON_BIDRAGSMOTTAKER_19830916_827",
                    type = Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                    innhold = POJONode(
                        Person(
                            ident = Personident("16498311338"),
                            fødselsdato = LocalDate.parse("1983-07-18"),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                ),
                GrunnlagDto(
                    referanse = "person_PERSON_SØKNADSBARN_20180718_826",
                    type = Grunnlagstype.PERSON_SØKNADSBARN,
                    innhold = POJONode(
                        Person(
                            ident = Personident("12345678901"),
                            navn = "Ola Nordmann",
                            fødselsdato = LocalDate.parse("2018-07-18"),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                ),
                GrunnlagDto(
                    referanse = "sluttberegning_person_PERSON_SØKNADSBARN_20180718_826_202407",
                    type = Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG_ALDERSJUSTERING,
                    innhold = POJONode(
                        SluttberegningBarnebidragAldersjustering(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            beregnetBeløp = BigDecimal.valueOf(1),
                            resultatBeløp = BigDecimal.valueOf(2),
                            bpAndelBeløp = BigDecimal.valueOf(3),
                            bpAndelFaktorVedDeltBosted = BigDecimal.valueOf(0.6),
                            deltBosted = false,
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "DELBEREGNING_UNDERHOLDSKOSTNAD_person_PERSON_BIDRAGSMOTTAKER_19830916_827_person_PERSON_SØKNADSBARN_20180718_826_202401",
                        "delberegning_KOPI_DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL_UNDERHOLDSKOSTNAD_person_PERSON_BIDRAGSPLIKTIG_20010217_825_person_PERSON_SØKNADSBARN_20180718_826_202401",
                        "DELBEREGNING_SAMVÆRSFRADRAG_person_PERSON_BIDRAGSPLIKTIG_20010217_825_person_PERSON_SØKNADSBARN_20180718_826_202401",
                        "person_PERSON_BIDRAGSPLIKTIG_20010217_825",
                        "person_PERSON_BIDRAGSMOTTAKER_19830916_827",
                        "person_PERSON_SØKNADSBARN_20180718_826",
                        "DELBEREGNING_KOPI_SAMVÆRSPERIODE_person_PERSON_BIDRAGSPLIKTIG_20010217_825_person_PERSON_SØKNADSBARN_20180718_826_20240201",
                    ),
                    gjelderBarnReferanse = "person_PERSON_SØKNADSBARN_20180718_826",
                ),
                GrunnlagDto(
                    referanse = "DELBEREGNING_UNDERHOLDSKOSTNAD_person_PERSON_BIDRAGSMOTTAKER_19830916_827_person_PERSON_SØKNADSBARN_20180718_826_202401",
                    type = Grunnlagstype.DELBEREGNING_UNDERHOLDSKOSTNAD,
                    innhold = POJONode(
                        DelberegningUnderholdskostnad(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            forbruksutgift = BigDecimal.valueOf(1000),
                            boutgift = BigDecimal.valueOf(1000),
                            barnetilsynMedStønad = BigDecimal.valueOf(100),
                            nettoTilsynsutgift = BigDecimal.valueOf(100),
                            barnetrygd = BigDecimal.valueOf(100),
                            underholdskostnad = BigDecimal.valueOf(500),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                    gjelderReferanse = "person_PERSON_SØKNADSBARN_20180718_826",
                ),
                GrunnlagDto(
                    referanse = "delberegning_KOPI_DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL_UNDERHOLDSKOSTNAD_person_PERSON_BIDRAGSPLIKTIG_20010217_825_person_PERSON_SØKNADSBARN_20180718_826_202401",
                    type = Grunnlagstype.KOPI_DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL,
                    innhold = POJONode(
                        KopiDelberegningBidragspliktigesAndel(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            fraVedtakId = 1,
                            endeligAndelFaktor = BigDecimal.valueOf(0.6),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                    gjelderReferanse = "person_PERSON_SØKNADSBARN_20180718_826",
                ),
                GrunnlagDto(
                    referanse = "DELBEREGNING_SAMVÆRSFRADRAG_person_PERSON_BIDRAGSPLIKTIG_20010217_825_person_PERSON_SØKNADSBARN_20180718_826_202401",
                    type = Grunnlagstype.DELBEREGNING_SAMVÆRSFRADRAG,
                    innhold = POJONode(
                        DelberegningSamværsfradrag(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            beløp = BigDecimal.valueOf(150),
                        ),
                    ),
                    grunnlagsreferanseListe = emptyList(),
                    gjelderReferanse = "person_PERSON_SØKNADSBARN_20180718_826",
                ),
                GrunnlagDto(
                    referanse = "DELBEREGNING_KOPI_SAMVÆRSPERIODE_person_PERSON_BIDRAGSPLIKTIG_20010217_825_person_PERSON_SØKNADSBARN_20180718_826_20240201",
                    type = Grunnlagstype.KOPI_SAMVÆRSPERIODE,
                    innhold = POJONode(
                        KopiSamværsperiodeGrunnlag(
                            periode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
                            fraVedtakId = 1,
                            samværsklasse = Samværsklasse.SAMVÆRSKLASSE_2,
                        ),
                    ),
                    grunnlagsreferanseListe = listOf(
                        "innhentet_husstandsmedlem_person_PERSON_BIDRAGSMOTTAKER_19830916_827_person_PERSON_SØKNADSBARN_20180718_826",
                    ),
                    gjelderBarnReferanse = "person_PERSON_SØKNADSBARN_20180718_826",
                    gjelderReferanse = "person_PERSON_BIDRAGSPLIKTIG_20010217_825",
                ),
            ),
            stønadsendringListe = listOf(
                StønadsendringDto(
                    type = Stønadstype.BIDRAG,
                    sak = Saksnummer("1234567"),
                    skyldner = Personident("98765432109"),
                    kravhaver = Personident("12345678901"),
                    mottaker = Personident("16498311338"),
                    sisteVedtaksid = null,
                    førsteIndeksreguleringsår = 2024,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    beslutning = Beslutningstype.ENDRING,
                    omgjørVedtakId = null,
                    eksternReferanse = null,
                    grunnlagReferanseListe = emptyList(),
                    periodeListe = listOf(
                        VedtakPeriodeDto(
                            periode = ÅrMånedsperiode(YearMonth.of(2024, 7), YearMonth.of(2024, 8)),
                            beløp = BigDecimal.valueOf(2),
                            valutakode = "NOK",
                            resultatkode = "A",
                            delytelseId = "delytelseId1",
                            grunnlagReferanseListe = listOf(
                                "sluttberegning_person_PERSON_SØKNADSBARN_20180718_826_202407",
                            ),
                        ),
                    ),
                ),
            ),
            engangsbeløpListe = emptyList(),
            behandlingsreferanseListe = emptyList(),
        )

        fun lesVedtakDtoFraFil(filnavn: String): VedtakDto {
            var json = ""

            // Les inn fil med request-data (json)
            try {
                json = Files.readString(Paths.get(filnavn))
            } catch (e: Exception) {
                Assertions.fail("Klarte ikke å lese fil: $filnavn")
            }

            // Lag request
            return commonObjectmapper.findAndRegisterModules().readValue(json, VedtakDto::class.java)
        }
    }
}

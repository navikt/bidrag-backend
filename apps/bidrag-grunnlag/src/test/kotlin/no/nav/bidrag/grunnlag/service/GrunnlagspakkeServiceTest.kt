package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.enums.inntekt.Skattegrunnlagstype
import no.nav.bidrag.domene.enums.person.SivilstandskodePDL
import no.nav.bidrag.domene.enums.vedtak.Formål
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.grunnlag.BidragGrunnlagTest
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.bo.AinntektBo
import no.nav.bidrag.grunnlag.bo.AinntektspostBo
import no.nav.bidrag.grunnlag.bo.BarnetilleggBo
import no.nav.bidrag.grunnlag.bo.RelatertPersonBo
import no.nav.bidrag.grunnlag.bo.SivilstandBo
import no.nav.bidrag.grunnlag.bo.SkattegrunnlagBo
import no.nav.bidrag.grunnlag.bo.SkattegrunnlagspostBo
import no.nav.bidrag.grunnlag.bo.UtvidetBarnetrygdOgSmaabarnstilleggBo
import no.nav.bidrag.grunnlag.persistence.repository.GrunnlagspakkeRepository
import no.nav.bidrag.grunnlag.stubHentSaksbehandler
import no.nav.bidrag.transport.behandling.grunnlag.request.OpprettGrunnlagspakkeRequestDto
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("GrunnlagspakkeServiceTest")
@ActiveProfiles(BidragGrunnlagTest.TEST_PROFILE)
@SpringBootTest(
    classes = [BidragGrunnlagTest::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@EnableMockOAuth2Server
@EnableWireMock(
    ConfigureWireMock(name = "my-service", port = 0),
)
class GrunnlagspakkeServiceTest {

    private val relatertPersonId1 = genererFødselsnummer()

    @Autowired
    private lateinit var grunnlagspakkeRepository: GrunnlagspakkeRepository

    @Autowired
    private lateinit var grunnlagspakkeService: GrunnlagspakkeService

    @Autowired
    private lateinit var persistenceService: PersistenceService

    @BeforeEach
    fun `init`() {
        // Sletter alle forekomster
        grunnlagspakkeRepository.deleteAll()
        stubHentSaksbehandler()
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `Test på å opprette ny grunnlagspakke`() {
        val opprettGrunnlagspakkeRequestDto = OpprettGrunnlagspakkeRequestDto(
            Formål.FORSKUDD,
            "X123456",
        )

        val nyGrunnlagspakkeOpprettet =
            grunnlagspakkeService.opprettGrunnlagspakke(opprettGrunnlagspakkeRequestDto)

        assertAll(
            Executable { assertThat(nyGrunnlagspakkeOpprettet).isNotNull },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `Test på å lukke en grunnlagspakke`() {
        val opprettGrunnlagspakkeRequestDto =
            OpprettGrunnlagspakkeRequestDto(Formål.FORSKUDD, "X123456")
        val grunnlagspakkeIdOpprettet =
            grunnlagspakkeService.opprettGrunnlagspakke(opprettGrunnlagspakkeRequestDto)
        val lukketGrunnlagspakke = grunnlagspakkeService.lukkGrunnlagspakke(grunnlagspakkeIdOpprettet)

        assertAll(
            Executable { assertThat(grunnlagspakkeIdOpprettet).isNotNull },
            Executable { assertThat(lukketGrunnlagspakke).isEqualTo(grunnlagspakkeIdOpprettet) },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `Test på å hente grunnlagspakke med aktive og inaktive inntekter + andre grunnlag`() {
        val opprettGrunnlagspakkeRequestDto =
            OpprettGrunnlagspakkeRequestDto(Formål.FORSKUDD, "X123456")
        val grunnlagspakkeIdOpprettet =
            grunnlagspakkeService.opprettGrunnlagspakke(opprettGrunnlagspakkeRequestDto)

        val ainntektBo = AinntektBo(
            grunnlagspakkeId = grunnlagspakkeIdOpprettet,
            personId = "1234567",
            periodeFra = LocalDate.parse("2021-06-01"),
            periodeTil = LocalDate.parse("2021-07-01"),
            aktiv = true,
            brukFra = LocalDateTime.now(),
            brukTil = null,
            hentetTidspunkt = LocalDateTime.now(),
        )

        val opprettetAinntekt = persistenceService.opprettAinntekt(ainntektBo)

        persistenceService.opprettAinntektspost(
            AinntektspostBo(
                inntektId = opprettetAinntekt.inntektId,
                utbetalingsperiode = "202106",
                opptjeningsperiodeFra = LocalDate.parse("2021-05-01"),
                opptjeningsperiodeTil = LocalDate.parse("2021-06-01"),
                opplysningspliktigId = "1234567890",
                virksomhetId = "222444666",
                inntektType = "Loenn",
                fordelType = "Kontantytelse",
                beskrivelse = "Loenn/fastloenn",
                belop = BigDecimal.valueOf(17000.01),
                etterbetalingsperiodeFra = LocalDate.of(2020, 9, 1),
                etterbetalingsperiodeTil = LocalDate.of(2020, 10, 1),
            ),
        )
        persistenceService.opprettAinntektspost(
            AinntektspostBo(
                inntektId = opprettetAinntekt.inntektId,
                utbetalingsperiode = "202106",
                opptjeningsperiodeFra = LocalDate.parse("2020-01-01"),
                opptjeningsperiodeTil = LocalDate.parse("2021-01-01"),
                opplysningspliktigId = "1234567890",
                virksomhetId = "222444666",
                inntektType = "Loenn",
                fordelType = "Kontantytelse",
                beskrivelse = "Loenn/ferieLoenn",
                belop = BigDecimal.valueOf(50000.01),
                etterbetalingsperiodeFra = LocalDate.of(2020, 10, 1),
                etterbetalingsperiodeTil = LocalDate.of(2020, 11, 1),
            ),
        )

        // tester at inntekt som er merket med aktiv = false ikke hentes
        val inaktivInntektDto = AinntektBo(
            grunnlagspakkeId = grunnlagspakkeIdOpprettet,
            personId = "1234567",
            periodeFra = LocalDate.parse("2020-06-01"),
            periodeTil = LocalDate.parse("2020-07-01"),
            aktiv = false,
            hentetTidspunkt = LocalDateTime.now(),
            brukFra = LocalDateTime.now(),
            brukTil = null,
        )

        val inaktivAinntekt = persistenceService.opprettAinntekt(inaktivInntektDto)

        persistenceService.opprettAinntektspost(
            AinntektspostBo(
                inntektId = inaktivAinntekt.inntektId,
                utbetalingsperiode = "202006",
                opptjeningsperiodeFra = LocalDate.parse("2020-05-01"),
                opptjeningsperiodeTil = LocalDate.parse("2020-06-01"),
                opplysningspliktigId = "1234567890",
                virksomhetId = "222444666",
                inntektType = "Loenn",
                fordelType = "Kontantytelse",
                beskrivelse = "Loenn/fastloenn",
                belop = BigDecimal.valueOf(50000.01),
                etterbetalingsperiodeFra = LocalDate.of(2021, 10, 1),
                etterbetalingsperiodeTil = LocalDate.of(2021, 11, 1),
            ),
        )

        // Legger inn inntekt for person nr 2
        val inntektDto2 = AinntektBo(
            grunnlagspakkeId = grunnlagspakkeIdOpprettet,
            personId = "999999",
            periodeFra = LocalDate.parse("2021-07-01"),
            periodeTil = LocalDate.parse("2021-08-01"),
            aktiv = true,
            hentetTidspunkt = LocalDateTime.now(),
            brukFra = LocalDateTime.now(),
            brukTil = null,
        )

        val opprettetAinntekt2 = persistenceService.opprettAinntekt(inntektDto2)

        persistenceService.opprettAinntektspost(
            AinntektspostBo(
                inntektId = opprettetAinntekt2.inntektId,
                utbetalingsperiode = "202107",
                opptjeningsperiodeFra = LocalDate.parse("2021-06-01"),
                opptjeningsperiodeTil = LocalDate.parse("2021-07-01"),
                opplysningspliktigId = "9876543210",
                virksomhetId = "666444222",
                inntektType = "Loenn",
                fordelType = "Kontantytelse",
                beskrivelse = "Loenn/fastloenn",
                belop = BigDecimal.valueOf(666000.01),
                etterbetalingsperiodeFra = LocalDate.of(2021, 1, 1),
                etterbetalingsperiodeTil = LocalDate.of(2021, 2, 1),
            ),
        )

        // Legger inn inntekt fra Skatt
        val skattegrunnlagBo = SkattegrunnlagBo(
            grunnlagspakkeId = grunnlagspakkeIdOpprettet,
            personId = "345678",
            periodeFra = LocalDate.parse("2021-01-01"),
            periodeTil = LocalDate.parse("2022-01-01"),
            aktiv = true,
            brukFra = LocalDateTime.now(),
            brukTil = null,
            hentetTidspunkt = LocalDateTime.now(),
        )

        val opprettetSkattegrunnlag = persistenceService.opprettSkattegrunnlag(skattegrunnlagBo)

        persistenceService.opprettSkattegrunnlagspost(
            SkattegrunnlagspostBo(
                skattegrunnlagId = opprettetSkattegrunnlag.skattegrunnlagId,
                skattegrunnlagType = Skattegrunnlagstype.ORDINÆR.toString(),
                inntektType = "Loenn",
                belop = BigDecimal.valueOf(23456.01),
            ),
        )

        // Legger inn utvidet barnetrygd og småbarnstillegg
        persistenceService.opprettUtvidetBarnetrygdOgSmaabarnstillegg(
            UtvidetBarnetrygdOgSmaabarnstilleggBo(
                grunnlagspakkeId = grunnlagspakkeIdOpprettet,
                personId = "22334455",
                type = "Utvidet barnetrygd",
                periodeFra = LocalDate.parse("2021-05-01"),
                periodeTil = LocalDate.parse("2021-06-01"),
                aktiv = true,
                brukFra = LocalDateTime.now(),
                brukTil = null,
                belop = BigDecimal.valueOf(12468.01),
                manueltBeregnet = false,
                deltBosted = false,
                hentetTidspunkt = LocalDateTime.now(),
            ),
        )

        // Legger inn barnetillegg
        persistenceService.opprettBarnetillegg(
            BarnetilleggBo(
                grunnlagspakkeId = grunnlagspakkeIdOpprettet,
                partPersonId = "22334455",
                barnPersonId = "1234567",
                barnetilleggType = Inntektstype.BARNETILLEGG_PENSJON.toString(),
                periodeFra = LocalDate.parse("2021-05-01"),
                periodeTil = LocalDate.parse("2021-06-01"),
                belopBrutto = BigDecimal.valueOf(1000.01),
            ),
        )

        val grunnlagspakkeFunnet =
            grunnlagspakkeService.hentGrunnlagspakke(grunnlagspakkeIdOpprettet)

        assertAll(
            Executable { assertThat(grunnlagspakkeFunnet).isNotNull },
            Executable { assertThat(grunnlagspakkeFunnet.grunnlagspakkeId).isEqualTo(grunnlagspakkeIdOpprettet) },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe.size).isEqualTo(2) },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].personId).isEqualTo("1234567") },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].periodeFra).isEqualTo(LocalDate.parse("2021-06-01")) },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].periodeTil).isEqualTo(LocalDate.parse("2021-07-01")) },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].aktiv).isEqualTo(true) },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].brukTil).isNull() },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[1].personId).isEqualTo("999999") },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[1].periodeFra).isEqualTo(LocalDate.parse("2021-07-01")) },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[1].periodeTil).isEqualTo(LocalDate.parse("2021-08-01")) },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[1].aktiv).isEqualTo(true) },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[1].brukTil).isNull() },

            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe.size).isEqualTo(2) },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[0].utbetalingsperiode).isEqualTo("202106") },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[0].opptjeningsperiodeFra,
                ).isEqualTo(LocalDate.parse("2021-05-01"))
            },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[0].opptjeningsperiodeTil,
                ).isEqualTo(LocalDate.parse("2021-06-01"))
            },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[0].opplysningspliktigId).isEqualTo("1234567890") },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[0].virksomhetId).isEqualTo("222444666") },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[0].kategori).isEqualTo("Loenn") },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[0].fordelType).isEqualTo("Kontantytelse") },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[0].beskrivelse).isEqualTo("Loenn/fastloenn") },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[0].beløp).isEqualTo(BigDecimal.valueOf(17000.01)) },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[0].etterbetalingsperiodeFra,
                ).isEqualTo(LocalDate.parse("2020-09-01"))
            },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[0].etterbetalingsperiodeTil,
                ).isEqualTo(LocalDate.parse("2020-10-01"))
            },

            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[1].utbetalingsperiode).isEqualTo("202106") },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[1].opptjeningsperiodeFra,
                ).isEqualTo(LocalDate.parse("2020-01-01"))
            },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[1].opptjeningsperiodeTil,
                ).isEqualTo(LocalDate.parse("2021-01-01"))
            },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[1].opplysningspliktigId).isEqualTo("1234567890") },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[1].virksomhetId).isEqualTo("222444666") },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[1].kategori).isEqualTo("Loenn") },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[1].fordelType).isEqualTo("Kontantytelse") },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[1].beskrivelse).isEqualTo("Loenn/ferieLoenn") },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[1].beløp).isEqualTo(BigDecimal.valueOf(50000.01)) },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[1].etterbetalingsperiodeFra,
                ).isEqualTo(LocalDate.parse("2020-10-01"))
            },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.ainntektListe[0].ainntektspostListe[1].etterbetalingsperiodeTil,
                ).isEqualTo(LocalDate.parse("2020-11-01"))
            },

            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[1].ainntektspostListe.size).isEqualTo(1) },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[1].ainntektspostListe[0].utbetalingsperiode).isEqualTo("202107") },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.ainntektListe[1].ainntektspostListe[0].opptjeningsperiodeFra,
                ).isEqualTo(LocalDate.parse("2021-06-01"))
            },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.ainntektListe[1].ainntektspostListe[0].opptjeningsperiodeTil,
                ).isEqualTo(LocalDate.parse("2021-07-01"))
            },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[1].ainntektspostListe[0].opplysningspliktigId).isEqualTo("9876543210") },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[1].ainntektspostListe[0].virksomhetId).isEqualTo("666444222") },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[1].ainntektspostListe[0].kategori).isEqualTo("Loenn") },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[1].ainntektspostListe[0].fordelType).isEqualTo("Kontantytelse") },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[1].ainntektspostListe[0].beskrivelse).isEqualTo("Loenn/fastloenn") },
            Executable { assertThat(grunnlagspakkeFunnet.ainntektListe[1].ainntektspostListe[0].beløp).isEqualTo(BigDecimal.valueOf(666000.01)) },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.ainntektListe[1].ainntektspostListe[0].etterbetalingsperiodeFra,
                ).isEqualTo(LocalDate.parse("2021-01-01"))
            },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.ainntektListe[1].ainntektspostListe[0].etterbetalingsperiodeTil,
                ).isEqualTo(LocalDate.parse("2021-02-01"))
            },

            Executable { assertThat(grunnlagspakkeFunnet.skattegrunnlagListe[0].skattegrunnlagListe.size).isEqualTo(1) },
            Executable { assertThat(grunnlagspakkeFunnet.skattegrunnlagListe[0].personId).isEqualTo("345678") },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.skattegrunnlagListe[0].skattegrunnlagListe[0].skattegrunnlagType,
                ).isEqualTo(Skattegrunnlagstype.ORDINÆR.toString())
            },
            Executable { assertThat(grunnlagspakkeFunnet.skattegrunnlagListe[0].skattegrunnlagListe[0].kode).isEqualTo("Loenn") },
            Executable { assertThat(grunnlagspakkeFunnet.skattegrunnlagListe[0].aktiv).isEqualTo(true) },
            Executable { assertThat(grunnlagspakkeFunnet.skattegrunnlagListe[0].brukTil).isNull() },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.skattegrunnlagListe[0].skattegrunnlagListe[0].beløp,
                ).isEqualTo(BigDecimal.valueOf(23456.01))
            },

            Executable { assertThat(grunnlagspakkeFunnet.ubstListe.size).isEqualTo(1) },
            Executable { assertThat(grunnlagspakkeFunnet.ubstListe[0].personId).isEqualTo("22334455") },
            Executable { assertThat(grunnlagspakkeFunnet.ubstListe[0].type).isEqualTo("Utvidet barnetrygd") },
            Executable { assertThat(grunnlagspakkeFunnet.ubstListe[0].periodeFra).isEqualTo(LocalDate.parse("2021-05-01")) },
            Executable { assertThat(grunnlagspakkeFunnet.ubstListe[0].periodeTil).isEqualTo(LocalDate.parse("2021-06-01")) },
            Executable { assertThat(grunnlagspakkeFunnet.ubstListe[0].aktiv).isEqualTo(true) },
            Executable { assertThat(grunnlagspakkeFunnet.ubstListe[0].brukTil).isNull() },
            Executable { assertThat(grunnlagspakkeFunnet.ubstListe[0].belop).isEqualTo(BigDecimal.valueOf(12468.01)) },

            Executable { assertThat(grunnlagspakkeFunnet.barnetilleggListe.size).isEqualTo(1) },
            Executable { assertThat(grunnlagspakkeFunnet.barnetilleggListe[0].partPersonId).isEqualTo("22334455") },
            Executable { assertThat(grunnlagspakkeFunnet.barnetilleggListe[0].barnPersonId).isEqualTo("1234567") },
            Executable { assertThat(grunnlagspakkeFunnet.barnetilleggListe[0].barnetilleggType).isEqualTo(Inntektstype.BARNETILLEGG_PENSJON.toString()) },
            Executable { assertThat(grunnlagspakkeFunnet.barnetilleggListe[0].periodeFra).isEqualTo(LocalDate.parse("2021-05-01")) },
            Executable { assertThat(grunnlagspakkeFunnet.barnetilleggListe[0].periodeTil).isEqualTo(LocalDate.parse("2021-06-01")) },
            Executable { assertThat(grunnlagspakkeFunnet.barnetilleggListe[0].aktiv).isEqualTo(true) },
            Executable { assertThat(grunnlagspakkeFunnet.barnetilleggListe[0].brukTil).isNull() },
            Executable { assertThat(grunnlagspakkeFunnet.barnetilleggListe[0].belopBrutto).isEqualTo(BigDecimal.valueOf(1000.01)) },

        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `Test på å oppdatere en grunnlagspakke`() {
        val opprettGrunnlagspakkeRequestDto = OpprettGrunnlagspakkeRequestDto(
            Formål.FORSKUDD,
            "X123456",
        )

        val grunnlagspakkeId =
            grunnlagspakkeService.opprettGrunnlagspakke(opprettGrunnlagspakkeRequestDto)

        val oppdatertGrunnlagspakke = grunnlagspakkeService.oppdaterGrunnlagspakke(
            grunnlagspakkeId,
            TestUtil.byggOppdaterGrunnlagspakkeRequestBarnetillegg(),
        )

        assertAll(
            { assertThat(oppdatertGrunnlagspakke.grunnlagspakkeId).isEqualTo(grunnlagspakkeId) },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `Test på å hente grunnlagspakke med pdl-data`() {
        val opprettGrunnlagspakkeRequestDto = OpprettGrunnlagspakkeRequestDto(Formål.FORSKUDD, "X123456")
        val grunnlagspakkeIdOpprettet =
            grunnlagspakkeService.opprettGrunnlagspakke(opprettGrunnlagspakkeRequestDto)

        // Legger først inn to barn med der kun det første er husstandsmedlem
        persistenceService.opprettRelatertPerson(
            RelatertPersonBo(
                grunnlagspakkeId = grunnlagspakkeIdOpprettet,
                partPersonId = "12345678901",
                relatertPersonPersonId = relatertPersonId1,
                navn = "Svett Elefant",
                fodselsdato = LocalDate.parse("2017-05-17"),
                erBarnAvBmBp = true,
                husstandsmedlemPeriodeFra = LocalDate.parse("2010-05-01"),
                husstandsmedlemPeriodeTil = LocalDate.parse("2020-06-01"),
                aktiv = true,
                brukFra = LocalDateTime.now(),
                brukTil = null,
                hentetTidspunkt = LocalDateTime.now(),
            ),
        )
        persistenceService.opprettRelatertPerson(
            RelatertPersonBo(
                grunnlagspakkeId = grunnlagspakkeIdOpprettet,
                partPersonId = "12345678901",
                relatertPersonPersonId = relatertPersonId1,
                navn = "Svett Elefant",
                fodselsdato = LocalDate.parse("2017-05-17"),
                erBarnAvBmBp = true,
                husstandsmedlemPeriodeFra = LocalDate.parse("2021-05-01"),
                husstandsmedlemPeriodeTil = null,
                aktiv = true,
                brukFra = LocalDateTime.now(),
                brukTil = null,
                hentetTidspunkt = LocalDateTime.now(),
            ),
        )
        persistenceService.opprettRelatertPerson(
            RelatertPersonBo(
                grunnlagspakkeId = grunnlagspakkeIdOpprettet,
                partPersonId = "12345678901",
                relatertPersonPersonId = "98798798765",
                navn = "Trang Poncho",
                fodselsdato = LocalDate.parse("2000-12-02"),
                erBarnAvBmBp = true,
                husstandsmedlemPeriodeFra = null,
                husstandsmedlemPeriodeTil = null,
                aktiv = true,
                brukFra = LocalDateTime.now(),
                brukTil = null,
                hentetTidspunkt = LocalDateTime.now(),
            ),
        )

        // Legger inn voksent husstandsmedlem
        persistenceService.opprettRelatertPerson(
            RelatertPersonBo(
                grunnlagspakkeId = grunnlagspakkeIdOpprettet,
                partPersonId = "12345678901",
                relatertPersonPersonId = "99988877766",
                navn = "Klam Trøffel",
                fodselsdato = LocalDate.parse("1997-02-12"),
                erBarnAvBmBp = false,
                husstandsmedlemPeriodeFra = null,
                husstandsmedlemPeriodeTil = LocalDate.parse("2020-09-01"),
                aktiv = true,
                brukFra = LocalDateTime.now(),
                brukTil = null,
                hentetTidspunkt = LocalDateTime.now(),
            ),
        )

        // Legger inn sivilstand
        persistenceService.opprettSivilstand(
            SivilstandBo(
                grunnlagspakkeId = grunnlagspakkeIdOpprettet,
                personId = "12345678901",
                periodeFra = LocalDate.parse("2021-05-01"),
                periodeTil = LocalDate.parse("2021-06-01"),
                sivilstand = "UGIFT",
                aktiv = true,
                brukFra = LocalDateTime.now(),
                brukTil = null,
                hentetTidspunkt = LocalDateTime.now(),
            ),
        )

        val grunnlagspakkeFunnet =
            grunnlagspakkeService.hentGrunnlagspakke(grunnlagspakkeIdOpprettet)

        assertAll(
            Executable { assertThat(grunnlagspakkeFunnet).isNotNull },
            Executable { assertThat(grunnlagspakkeFunnet.grunnlagspakkeId).isEqualTo(grunnlagspakkeIdOpprettet) },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe.size).isEqualTo(3) },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[0].partPersonId).isEqualTo("12345678901") },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[0].relatertPersonPersonId).isEqualTo(relatertPersonId1) },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[0].navn).isEqualTo("Svett Elefant") },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[0].fodselsdato).isEqualTo(LocalDate.parse("2017-05-17")) },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[0].erBarnAvBmBp).isTrue() },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[0].hentetTidspunkt).isNotNull() },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[0].aktiv).isTrue() },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[0].borISammeHusstandDtoListe[0].periodeFra,
                ).isEqualTo(LocalDate.parse("2010-05-01"))
            },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[0].borISammeHusstandDtoListe[0].periodeTil,
                ).isEqualTo(LocalDate.parse("2020-06-01"))
            },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[0].borISammeHusstandDtoListe[1].periodeFra,
                ).isEqualTo(LocalDate.parse("2021-05-01"))
            },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[0].borISammeHusstandDtoListe[1].periodeTil).isNull() },

            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[1].relatertPersonPersonId).isEqualTo("98798798765") },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[1].navn).isEqualTo("Trang Poncho") },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[1].fodselsdato).isEqualTo(LocalDate.parse("2000-12-02")) },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[1].erBarnAvBmBp).isTrue() },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[1].hentetTidspunkt).isNotNull() },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[1].borISammeHusstandDtoListe).isEmpty() },

            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[2].partPersonId).isEqualTo("12345678901") },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[2].relatertPersonPersonId).isEqualTo("99988877766") },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[2].navn).isEqualTo("Klam Trøffel") },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[2].fodselsdato).isEqualTo(LocalDate.parse("1997-02-12")) },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[2].erBarnAvBmBp).isFalse() },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[2].hentetTidspunkt).isNotNull() },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[2].aktiv).isTrue() },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[2].borISammeHusstandDtoListe[0].periodeFra).isNull() },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[2].borISammeHusstandDtoListe[0].periodeTil,
                ).isEqualTo(LocalDate.parse("2020-09-01"))
            },

            Executable { assertThat(grunnlagspakkeFunnet.sivilstandListe.size).isEqualTo(1) },
            Executable { assertThat(grunnlagspakkeFunnet.sivilstandListe[0].periodeFra).isEqualTo(LocalDate.parse("2021-05-01")) },
            Executable { assertThat(grunnlagspakkeFunnet.sivilstandListe[0].periodeTil).isEqualTo(LocalDate.parse("2021-06-01")) },
            Executable { assertThat(grunnlagspakkeFunnet.sivilstandListe[0].sivilstand).isEqualTo(SivilstandskodePDL.UGIFT) },
            Executable { assertThat(grunnlagspakkeFunnet.sivilstandListe[0].hentetTidspunkt).isNotNull() },

        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `Test på å hente husstandsmedlemmer for to ulik personer i samme grunnlagspakke`() {
        val opprettGrunnlagspakkeRequestDto = OpprettGrunnlagspakkeRequestDto(Formål.FORSKUDD, "X123456")
        val grunnlagspakkeIdOpprettet =
            grunnlagspakkeService.opprettGrunnlagspakke(opprettGrunnlagspakkeRequestDto)

        // Legger først inn to barn med der kun det første er husstandsmedlem
        persistenceService.opprettRelatertPerson(
            RelatertPersonBo(
                grunnlagspakkeId = grunnlagspakkeIdOpprettet,
                partPersonId = "12345678901",
                relatertPersonPersonId = relatertPersonId1,
                navn = "Svett Elefant",
                fodselsdato = LocalDate.parse("2017-05-17"),
                erBarnAvBmBp = true,
                husstandsmedlemPeriodeFra = LocalDate.parse("2010-05-01"),
                husstandsmedlemPeriodeTil = LocalDate.parse("2020-06-01"),
                aktiv = true,
                brukFra = LocalDateTime.now(),
                brukTil = null,
                hentetTidspunkt = LocalDateTime.now(),
            ),
        )

        persistenceService.opprettRelatertPerson(
            RelatertPersonBo(
                grunnlagspakkeId = grunnlagspakkeIdOpprettet,
                partPersonId = "23456789012",
                relatertPersonPersonId = "98798798765",
                navn = "Trang Poncho",
                fodselsdato = LocalDate.parse("2000-12-02"),
                erBarnAvBmBp = true,
                husstandsmedlemPeriodeFra = LocalDate.parse("2020-09-01"),
                husstandsmedlemPeriodeTil = LocalDate.parse("2020-11-01"),
                aktiv = true,
                brukFra = LocalDateTime.now(),
                brukTil = null,
                hentetTidspunkt = LocalDateTime.now(),
            ),
        )

        persistenceService.opprettRelatertPerson(
            RelatertPersonBo(
                grunnlagspakkeId = grunnlagspakkeIdOpprettet,
                partPersonId = "23456789012",
                relatertPersonPersonId = "98798798765",
                navn = "Trang Poncho",
                fodselsdato = LocalDate.parse("2000-12-02"),
                erBarnAvBmBp = true,
                husstandsmedlemPeriodeFra = LocalDate.parse("2022-09-01"),
                husstandsmedlemPeriodeTil = null,
                aktiv = true,
                brukFra = LocalDateTime.now(),
                brukTil = null,
                hentetTidspunkt = LocalDateTime.now(),
            ),
        )

        val grunnlagspakkeFunnet =
            grunnlagspakkeService.hentGrunnlagspakke(grunnlagspakkeIdOpprettet)

        assertAll(
            Executable { assertThat(grunnlagspakkeFunnet).isNotNull },
            Executable { assertThat(grunnlagspakkeFunnet.grunnlagspakkeId).isEqualTo(grunnlagspakkeIdOpprettet) },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe.size).isEqualTo(2) },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[0].partPersonId).isEqualTo("12345678901") },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[1].partPersonId).isEqualTo("23456789012") },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[0].relatertPersonPersonId).isEqualTo(relatertPersonId1) },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[1].relatertPersonPersonId).isEqualTo("98798798765") },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[0].borISammeHusstandDtoListe[0].periodeFra,
                ).isEqualTo(LocalDate.parse("2010-05-01"))
            },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[0].borISammeHusstandDtoListe[0].periodeTil,
                ).isEqualTo(LocalDate.parse("2020-06-01"))
            },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[1].borISammeHusstandDtoListe[0].periodeFra,
                ).isEqualTo(LocalDate.parse("2020-09-01"))
            },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[1].borISammeHusstandDtoListe[0].periodeTil,
                ).isEqualTo(LocalDate.parse("2020-11-01"))
            },
            Executable {
                assertThat(
                    grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[1].borISammeHusstandDtoListe[1].periodeFra,
                ).isEqualTo(LocalDate.parse("2022-09-01"))
            },
            Executable { assertThat(grunnlagspakkeFunnet.husstandmedlemmerOgEgneBarnListe[1].borISammeHusstandDtoListe[1].periodeTil).isNull() },

        )
    }
}

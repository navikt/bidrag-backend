package no.nav.bidrag.behandling.controller.behandling

import com.fasterxml.jackson.databind.node.POJONode
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import no.nav.bidrag.behandling.dto.v1.behandling.OppdaterBeregnTilDatoRequestDto
import no.nav.bidrag.behandling.dto.v1.behandling.OppdaterOpphørsdatoRequestDto
import no.nav.bidrag.behandling.dto.v1.behandling.OpprettBehandlingFraVedtakRequest
import no.nav.bidrag.behandling.dto.v1.behandling.OpprettBehandlingResponse
import no.nav.bidrag.behandling.dto.v2.behandling.BehandlingDtoV2
import no.nav.bidrag.behandling.utils.testdata.lagVedtaksdata
import no.nav.bidrag.behandling.utils.testdata.testdataBM
import no.nav.bidrag.behandling.utils.testdata.testdataBP
import no.nav.bidrag.behandling.utils.testdata.testdataBarn1
import no.nav.bidrag.behandling.utils.testdata.testdataBarn2
import no.nav.bidrag.commons.web.mock.sjablonTallResponse
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.BeregnTil
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.enums.vedtak.VirkningstidspunktÅrsakstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.ForpleiningPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.test.assertNotNull

@Suppress("NonAsciiCharacters")
class OppretteBehandlingFraVedtakTest : BehandlingControllerTest() {
    @Test
    @Transactional
    fun `skal opprette en behandling fra vedtak`() {
        stubUtils.stubHenteGrunnlag(
            navnResponsfil = "grunnlagresponse.json",
            rolleIdent = testdataBM.ident,
        )
        stubUtils.stubHenteGrunnlag(
            tomRespons = true,
            rolleIdent = testdataBarn1.ident,
        )
        stubUtils.stubHenteGrunnlag(
            tomRespons = true,
            rolleIdent = testdataBarn2.ident,
        )
        stubUtils.stubHenteVedtak()

        // hvis
        val behandlingRes =
            httpHeaderTestRestTemplate.exchange(
                "${rootUriV2()}/behandling/vedtak/12333",
                HttpMethod.POST,
                HttpEntity(
                    OpprettBehandlingFraVedtakRequest(
                        vedtakstype = Vedtakstype.KLAGE,
                        søknadFra = SøktAvType.BIDRAGSMOTTAKER,
                        søktFomDato = LocalDate.parse("2020-01-01"),
                        mottattdato = LocalDate.parse("2024-01-01"),
                        behandlerenhet = "4444",
                        saksnummer = "1234567",
                        søknadsreferanseid = 111,
                        søknadsid = 12323,
                    ),
                ),
                OpprettBehandlingResponse::class.java,
            )

        // så
        Assertions.assertEquals(HttpStatus.OK, behandlingRes.statusCode)

        val behandling = behandlingRepository.findBehandlingById(behandlingRes.body!!.id).get()

        assertNotNull(behandling)
        assertSoftly(behandling) {
            roller shouldHaveSize 3
            inntekter shouldHaveSize 15
            innkrevingstype shouldBe Innkrevingstype.MED_INNKREVING
            omgjøringsdetaljer!!.omgjortVedtakstidspunktListe shouldHaveSize 1
            omgjøringsdetaljer!!.omgjortVedtakstidspunktListe shouldContain LocalDateTime.parse("2024-02-23T15:34:27.275019")
            omgjøringsdetaljer?.omgjørVedtakId shouldBe 12333
            grunnlag.filter { it.aktiv == null }.shouldHaveSize(10)
            sivilstand shouldHaveSize 2
            // TODO: Boforhold grunnlag inneholder sju unike husstandsmedlemmer - fikse stub-vedtaksdata slik at tallene stemmer
            husstandsmedlem shouldHaveSize 7
            søktFomDato shouldBe LocalDate.parse("2020-01-01")
            vedtakstype shouldBe Vedtakstype.KLAGE
            årsak shouldBe VirkningstidspunktÅrsakstype.FRA_SØKNADSTIDSPUNKT
            avslag shouldBe null
            omgjøringsdetaljer?.soknadRefId shouldBe 111
            soknadsid shouldBe 12323
            notater shouldHaveSize 7
            notater.filter { it.erDelAvBehandlingen == false }.shouldHaveSize(7)
        }

        stubUtils.Verify().hentGrunnlagKalt(1, testdataBM.tilRolle(behandling))
        stubUtils.Verify().hentGrunnlagKalt(1, testdataBarn2.tilRolle(behandling))
        stubUtils.Verify().hentGrunnlagKalt(1, testdataBarn1.tilRolle(behandling))
    }

    @Test
    fun `skal opprette en behandling fra vedtak med forpleining`() {
        listOf(testdataBM, testdataBP, testdataBarn1, testdataBarn2).forEach {
            stubUtils.stubHenteGrunnlag(tomRespons = true, rolleIdent = it.ident)
        }
        val vedtak = lagVedtaksdata("fattetvedtak/bidrag-innvilget")
        val søknadsbarnReferanse = "person_PERSON_SØKNADSBARN_20130615_285"
        val forpleiningGrunnlag =
            GrunnlagDto(
                referanse = "FORPLEINING_PERIODE_${søknadsbarnReferanse}_20240501_20240731",
                type = Grunnlagstype.FORPLEINING_PERIODE,
                innhold =
                POJONode(
                    ForpleiningPeriode(
                        periode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-08")),
                        beløp = BigDecimal(5000),
                        manueltRegistrert = true,
                    ),
                ),
                gjelderReferanse = "person_PERSON_BIDRAGSMOTTAKER_19790718_284",
                gjelderBarnReferanse = søknadsbarnReferanse,
            )
        stubUtils.stubHenteVedtak(responseObjekt = vedtak.copy(grunnlagListe = vedtak.grunnlagListe + forpleiningGrunnlag))

        val behandlingRes =
            httpHeaderTestRestTemplate.exchange(
                "${rootUriV2()}/behandling/vedtak/12333",
                HttpMethod.POST,
                HttpEntity(
                    OpprettBehandlingFraVedtakRequest(
                        vedtakstype = Vedtakstype.KLAGE,
                        søknadFra = SøktAvType.BIDRAGSPLIKTIG,
                        søktFomDato = LocalDate.parse("2024-02-01"),
                        mottattdato = LocalDate.parse("2024-11-01"),
                        behandlerenhet = "4806",
                        saksnummer = "1234567",
                        søknadsreferanseid = 111,
                        søknadsid = 12324,
                    ),
                ),
                OpprettBehandlingResponse::class.java,
            )

        behandlingRes.statusCode shouldBe HttpStatus.OK
        val behandling = behandlingRepository.findBehandlingById(behandlingRes.body!!.id).get()
        assertSoftly(behandling.underholdskostnader.find { !it.gjelderAndreBarn }!!) {
            harTilsynsordning shouldBe true
            forpleining shouldHaveSize 1
            forpleining.first().fom shouldBe LocalDate.parse("2024-05-01")
            forpleining.first().tom shouldBe LocalDate.parse("2024-07-31")
        }
    }

    @Test
    fun `skal beholde underholdsperioder når beregnTil endres til inneværende måned etter opprettelse fra vedtak`() {
        every { sjablonService.hentSjablontall() } returns sjablonTallResponse()
        listOf(testdataBM, testdataBP, testdataBarn1, testdataBarn2).forEach {
            stubUtils.stubHenteGrunnlag(tomRespons = true, rolleIdent = it.ident)
        }
        val vedtak = lagVedtaksdata("fattetvedtak/bidrag-innvilget")
        val søknadsbarnReferanse = "person_PERSON_SØKNADSBARN_20130615_285"
        val forpleiningGrunnlag =
            GrunnlagDto(
                referanse = "FORPLEINING_PERIODE_${søknadsbarnReferanse}_20240501_20240731",
                type = Grunnlagstype.FORPLEINING_PERIODE,
                innhold =
                POJONode(
                    ForpleiningPeriode(
                        periode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-08")),
                        beløp = BigDecimal(5000),
                        manueltRegistrert = true,
                    ),
                ),
                gjelderReferanse = "person_PERSON_BIDRAGSMOTTAKER_19790718_284",
                gjelderBarnReferanse = søknadsbarnReferanse,
            )
        stubUtils.stubHenteVedtak(responseObjekt = vedtak.copy(grunnlagListe = vedtak.grunnlagListe + forpleiningGrunnlag))

        val behandlingRes =
            httpHeaderTestRestTemplate.exchange(
                "${rootUriV2()}/behandling/vedtak/12333",
                HttpMethod.POST,
                HttpEntity(
                    OpprettBehandlingFraVedtakRequest(
                        vedtakstype = Vedtakstype.KLAGE,
                        søknadFra = SøktAvType.BIDRAGSPLIKTIG,
                        søktFomDato = LocalDate.parse("2024-02-01"),
                        mottattdato = LocalDate.parse("2024-11-01"),
                        behandlerenhet = "4806",
                        saksnummer = "1234567",
                        søknadsreferanseid = 111,
                        søknadsid = 12325,
                    ),
                ),
                OpprettBehandlingResponse::class.java,
            )
        behandlingRes.statusCode shouldBe HttpStatus.OK
        val behandlingsid = behandlingRes.body!!.id
        behandlingRepository
            .findBehandlingById(behandlingsid)
            .get()
            .underholdskostnader
            .find { !it.gjelderAndreBarn }!!
            .forpleining shouldHaveSize 1

        // Saksbehandler velger «Ut nåværende måned» i klagen eller omgjøringen
        val oppdaterRes =
            httpHeaderTestRestTemplate.exchange(
                "${rootUriV2()}/behandling/$behandlingsid/beregntildato",
                HttpMethod.PUT,
                HttpEntity(OppdaterBeregnTilDatoRequestDto(idRolle = null, beregnTil = BeregnTil.INNEVÆRENDE_MÅNED)),
                BehandlingDtoV2::class.java,
            )

        oppdaterRes.statusCode shouldBe HttpStatus.OK
        val behandling = behandlingRepository.findBehandlingById(behandlingsid).get()
        assertSoftly(behandling.underholdskostnader.find { !it.gjelderAndreBarn }!!) {
            harTilsynsordning shouldBe true
            barnetilsyn shouldHaveSize 1
            faktiskeTilsynsutgifter shouldHaveSize 2
            tilleggsstønad shouldHaveSize 1
            forpleining shouldHaveSize 1
            forpleining.firstOrNull()?.fom shouldBe LocalDate.parse("2024-05-01")
            forpleining.firstOrNull()?.tom shouldBe LocalDate.parse("2024-07-31")
        }
    }

    @Test
    fun `skal beholde underholdsperioder når opphørsdato settes og slettes`() {
        every { sjablonService.hentSjablontall() } returns sjablonTallResponse()
        listOf(testdataBM, testdataBP, testdataBarn1, testdataBarn2).forEach {
            stubUtils.stubHenteGrunnlag(tomRespons = true, rolleIdent = it.ident)
        }
        val vedtak = lagVedtaksdata("fattetvedtak/bidrag-innvilget")
        val søknadsbarnReferanse = "person_PERSON_SØKNADSBARN_20130615_285"
        val forpleiningGrunnlag =
            GrunnlagDto(
                referanse = "FORPLEINING_PERIODE_${søknadsbarnReferanse}_20240501_20240731",
                type = Grunnlagstype.FORPLEINING_PERIODE,
                innhold =
                POJONode(
                    ForpleiningPeriode(
                        periode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-08")),
                        beløp = BigDecimal(5000),
                        manueltRegistrert = true,
                    ),
                ),
                gjelderReferanse = "person_PERSON_BIDRAGSMOTTAKER_19790718_284",
                gjelderBarnReferanse = søknadsbarnReferanse,
            )
        stubUtils.stubHenteVedtak(responseObjekt = vedtak.copy(grunnlagListe = vedtak.grunnlagListe + forpleiningGrunnlag))

        val behandlingsid =
            httpHeaderTestRestTemplate
                .exchange(
                    "${rootUriV2()}/behandling/vedtak/12333",
                    HttpMethod.POST,
                    HttpEntity(
                        OpprettBehandlingFraVedtakRequest(
                            vedtakstype = Vedtakstype.KLAGE,
                            søknadFra = SøktAvType.BIDRAGSPLIKTIG,
                            søktFomDato = LocalDate.parse("2024-02-01"),
                            mottattdato = LocalDate.parse("2024-11-01"),
                            behandlerenhet = "4806",
                            saksnummer = "1234567",
                            søknadsreferanseid = 111,
                            søknadsid = 12326,
                        ),
                    ),
                    OpprettBehandlingResponse::class.java,
                ).body!!
                .id
        val idRolle = behandlingRepository.findBehandlingById(behandlingsid).get().søknadsbarn.first().id

        listOf(LocalDate.parse("2024-12-01"), null).forEach { opphørsdato ->
            httpHeaderTestRestTemplate
                .exchange(
                    "${rootUriV2()}/behandling/$behandlingsid/opphorsdato",
                    HttpMethod.PUT,
                    HttpEntity(OppdaterOpphørsdatoRequestDto(idRolle = idRolle, opphørsdato = opphørsdato)),
                    BehandlingDtoV2::class.java,
                ).statusCode shouldBe HttpStatus.OK
        }

        val behandling = behandlingRepository.findBehandlingById(behandlingsid).get()
        assertSoftly(behandling.underholdskostnader.find { !it.gjelderAndreBarn }!!) {
            barnetilsyn shouldHaveSize 1
            faktiskeTilsynsutgifter shouldHaveSize 2
            tilleggsstønad shouldHaveSize 1
            forpleining shouldHaveSize 1
        }
    }

    @Test
    fun `Skal ikke opprette behandling hvis vedtak ikke finnes`() {
        stubUtils.stubHenteVedtak(status = HttpStatus.NOT_FOUND)
        val behandlingRes =
            httpHeaderTestRestTemplate.exchange(
                "${rootUriV2()}/behandling/vedtak/12333",
                HttpMethod.POST,
                HttpEntity(
                    OpprettBehandlingFraVedtakRequest(
                        vedtakstype = Vedtakstype.KLAGE,
                        søknadFra = SøktAvType.BIDRAGSMOTTAKER,
                        søktFomDato = LocalDate.parse("2020-01-01"),
                        mottattdato = LocalDate.parse("2024-01-01"),
                        behandlerenhet = "4444",
                        saksnummer = "1234567",
                        søknadsreferanseid = 111,
                        søknadsid = 12325553,
                    ),
                ),
                Void::class.java,
            )

        behandlingRes.statusCode shouldBe HttpStatus.NOT_FOUND
    }
}

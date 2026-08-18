package no.nav.bidrag.organisasjon.consumer

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockkClass
import io.mockk.verify
import no.nav.bidrag.domene.enums.diverse.Tema
import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.organisasjon.consumer.dto.ArbeidsfordelingEnheterBestMatchRequest
import no.nav.bidrag.organisasjon.consumer.dto.ArbeidsfordelingEnheterBestMatchResponse
import no.nav.bidrag.organisasjon.consumer.dto.ArbeidsfordelingEnheterRequest
import no.nav.bidrag.organisasjon.consumer.dto.ArbeidsfordelingEnheterRequestBody
import no.nav.bidrag.organisasjon.consumer.dto.ArbeidsfordelingEnheterResponse
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange

@ExtendWith(MockKExtension::class)
@Disabled("")
internal class Norg2ConsumerTest {
    private var restTemplateMock: RestTemplate = mockkClass(RestTemplate::class, relaxed = true)

    private var norg2Consumer: Norg2Consumer = Norg2Consumer("", restTemplateMock)

    @Test
    fun `skal bruke riktige parametre i sti til tjeneste arbeidsfordeling enheter best match`() {
        every {
            restTemplateMock.exchange<List<ArbeidsfordelingEnheterBestMatchResponse>>(
                any<String>(),
                any(),
                any(),
            )
        } returns ResponseEntity(HttpStatus.NO_CONTENT)
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val arbeidsfordelingRequest = lagArbeidsfordelingEnheterBestMatchDummyRequest()
        norg2Consumer.finnArbeidsfordelingEnheterBestMatch(arbeidsfordelingRequest)
        verify {
            restTemplateMock
                .exchange<List<ArbeidsfordelingEnheterBestMatchResponse>>(
                    eq("/arbeidsfordeling/enheter/bestmatch"),
                    eq(HttpMethod.POST),
                    eq(HttpEntity(arbeidsfordelingRequest, headers)),
                )
        }
    }

    @Test
    fun `sjekk at respons fra tjeneste arbeidsfordeling enheter best match mappes korrekt`() {
        every {
            restTemplateMock.exchange<List<ArbeidsfordelingEnheterBestMatchResponse>>(
                any<String>(),
                any(),
                any(),
            )
        } returns responseEntityBestMatchResponse()
        val arbeidsfordelingRequest = lagArbeidsfordelingEnheterBestMatchDummyRequest()
        val arbeidsfordelingResponse = norg2Consumer.finnArbeidsfordelingEnheterBestMatch(arbeidsfordelingRequest)
        assertSoftly {
            arbeidsfordelingResponse shouldNotBe null
            arbeidsfordelingResponse!!.size shouldBe 1
            arbeidsfordelingResponse[0].enhetNr.verdi shouldBe "EnhetId"
            arbeidsfordelingResponse[0].navn shouldBe "EnhetNavn"
        }
    }

    @Test
    fun `skal bruke riktige parametre i sti til tjeneste arbeidsfordeling enheter liste`() {
        every {
            restTemplateMock.exchange<List<ArbeidsfordelingEnheterResponse>>(
                any<String>(),
                any(),
                any(),
            )
        } returns ResponseEntity(HttpStatus.NO_CONTENT)
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val arbeidsfordelingRequest = lagArbeidsfordelingEnheterListeDummyRequest()
        norg2Consumer.finnArbeidsfordelingEnheterListe(arbeidsfordelingRequest)
        verify {
            restTemplateMock
                .exchange<List<ArbeidsfordelingEnheterResponse>>(
                    eq("/arbeidsfordeling/enheter?enhetstype=FPY&enhetstype=KO&enhetstype=KLAGE"),
                    eq(HttpMethod.POST),
                    eq(HttpEntity(ArbeidsfordelingEnheterRequestBody(arbeidsfordelingRequest.tema), headers)),
                )
        }
    }

    @Test
    fun `sjekk at respons fra tjeneste arbeidsfordeling enheter liste mappes korrekt`() {
        every {
            restTemplateMock.exchange<List<ArbeidsfordelingEnheterResponse>>(any<String>(), any(), any())
        } returns responseEntityEnheterListeResponse()
        val arbeidsfordelingRequest = lagArbeidsfordelingEnheterListeDummyRequest()
        val arbeidsfordelingResponse = norg2Consumer.finnArbeidsfordelingEnheterListe(arbeidsfordelingRequest)
        assertSoftly {
            arbeidsfordelingResponse.responseEntity.statusCode shouldBe HttpStatus.OK
            arbeidsfordelingResponse.responseEntity.body shouldNotBe null
            val responseBody = arbeidsfordelingResponse.responseEntity.body
            responseBody?.size shouldBe 1
            responseBody?.first()?.enhetNr?.verdi shouldBe "EnhetId"
            responseBody?.first()?.navn shouldBe "EnhetNavn"
            responseBody?.first()?.type shouldBe "EnhetType"
        }
    }

    @Test
    fun `sjekk at hent enhet kontaktinfo fungerer som den skal`() {
        listOf(2103, 4250, 4293, 4291, 4294, 4803, 4806, 4812, 4817, 4820, 4833, 4849, 4860, 4865, 4883)
            .forEach {
                val kontaktinfo =
                    norg2Consumer.hentEnhetKontaktinfo(
                        it.toString(),
                    )
                kontaktinfo.shouldNotBeNull()
            }
    }

    private fun lagArbeidsfordelingEnheterBestMatchDummyRequest(): ArbeidsfordelingEnheterBestMatchRequest = ArbeidsfordelingEnheterBestMatchRequest(
        Diskresjonskode.SVAL,
        Tema.TEMA_BIDRAG.name,
        "OSLO",
        true,
        "JFR",
        null,
    )

    private fun lagArbeidsfordelingEnheterListeDummyRequest(): ArbeidsfordelingEnheterRequest = ArbeidsfordelingEnheterRequest(listOf(FORVALTNING, SPESIALENHETER, KLAGE), BIDRAG)

    companion object {
        private const val FORVALTNING = "FPY"
        private const val SPESIALENHETER = "KO"
        private const val KLAGE = "KLAGE"
        private const val BIDRAG = "BID"

        private fun responseEntityBestMatchResponse(): ResponseEntity<List<ArbeidsfordelingEnheterBestMatchResponse>> {
            val arbeidsfordelingEnhetResponse = listOf(ArbeidsfordelingEnheterBestMatchResponse(Enhetsnummer("EnhetId"), "EnhetNavn"))
            return ResponseEntity(arbeidsfordelingEnhetResponse, HttpStatus.OK)
        }

        private fun responseEntityEnheterListeResponse(): ResponseEntity<List<ArbeidsfordelingEnheterResponse>> {
            val arbeidsfordelingEnheterResponse =
                listOf(ArbeidsfordelingEnheterResponse(Enhetsnummer("EnhetId"), "EnhetNavn", "EnhetType"))
            return ResponseEntity(arbeidsfordelingEnheterResponse, HttpStatus.OK)
        }
    }
}

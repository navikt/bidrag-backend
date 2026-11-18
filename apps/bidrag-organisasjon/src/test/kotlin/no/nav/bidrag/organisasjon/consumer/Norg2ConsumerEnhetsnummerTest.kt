package no.nav.bidrag.organisasjon.consumer

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockkClass
import io.mockk.verify
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.organisasjon.consumer.dto.EnhetInfoResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

@ExtendWith(MockKExtension::class)
internal class Norg2ConsumerEnhetsnummerTest {
    private val restTemplateMock: RestTemplate = mockkClass(RestTemplate::class, relaxed = true)

    private val norg2Consumer = Norg2Consumer("https://norg2.intern.nav.no/api/v1", restTemplateMock)

    @Test
    fun `skal kalle NORG2 med enhetsnummer utledet fra sanitizeren`() {
        every {
            restTemplateMock.exchange(any<String>(), any(), any(), EnhetInfoResponse::class.java)
        } returns ResponseEntity(EnhetInfoResponse(enhetNr = Enhetsnummer("4806")), HttpStatus.OK)

        val enhetInfo = norg2Consumer.hentEnhetInfo(Enhetsnummer("4806"))

        enhetInfo.enhetNr.verdi shouldBe "4806"
        verify(exactly = 1) {
            restTemplateMock.exchange("/enhet/4806", HttpMethod.GET, any(), EnhetInfoResponse::class.java)
        }
    }

    @Test
    fun `skal bevare ledende nuller når enhetsnummeret utledes via heltall`() {
        every {
            restTemplateMock.exchange(any<String>(), any(), any(), EnhetInfoResponse::class.java)
        } returns ResponseEntity(EnhetInfoResponse(enhetNr = Enhetsnummer("0042")), HttpStatus.OK)

        norg2Consumer.hentEnhetInfo(Enhetsnummer("0042"))

        verify(exactly = 1) {
            restTemplateMock.exchange("/enhet/0042", HttpMethod.GET, any(), EnhetInfoResponse::class.java)
        }
    }

    @Test
    fun `skal avvise ugyldige enhetsnummer i hentEnhetInfo uten å kalle NORG2`() {
        ugyldigeEnhetsnummer().forEach { ugyldig ->
            shouldThrow<IllegalArgumentException> {
                norg2Consumer.hentEnhetInfo(Enhetsnummer(ugyldig))
            }
        }

        verify(exactly = 0) {
            restTemplateMock.exchange(any<String>(), any(), any(), EnhetInfoResponse::class.java)
        }
    }

    @Test
    fun `skal avvise ugyldige enhetsnummer i hentArbeidsfordelingForEnhet uten å kalle NORG2`() {
        ugyldigeEnhetsnummer().forEach { ugyldig ->
            shouldThrow<IllegalArgumentException> {
                norg2Consumer.hentArbeidsfordelingForEnhet(Enhetsnummer(ugyldig))
            }
        }
    }

    @Test
    fun `feilmelding skal ikke eksponere brukerinput`() {
        val exception =
            shouldThrow<IllegalArgumentException> {
                norg2Consumer.hentEnhetInfo(Enhetsnummer("http://angriper.example.com"))
            }

        exception.message shouldBe "Ugyldig enhetsnummer, må bestå av nøyaktig fire siffer"
    }

    private fun ugyldigeEnhetsnummer() = listOf(
        "../../etc/passwd",
        "4806/../../enhet",
        "http://angriper.example.com",
        "//angriper.example.com",
        "4806 ",
        "48061",
        "480",
        "abcd",
        "",
        "4806\u0000",
        "4806\n/enhet/1234",
        "4".repeat(5000),
    )
}

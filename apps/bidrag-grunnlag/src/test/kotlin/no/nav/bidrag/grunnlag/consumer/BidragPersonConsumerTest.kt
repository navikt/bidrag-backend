package no.nav.bidrag.grunnlag.consumer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import no.nav.bidrag.domene.enums.person.Familierelasjon
import no.nav.bidrag.domene.enums.person.SivilstandskodePDL
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.grunnlag.consumer.bidragperson.BidragPersonConsumer
import no.nav.bidrag.grunnlag.consumer.bidragperson.api.HusstandsmedlemmerRequest
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.transport.person.ForelderBarnRelasjon
import no.nav.bidrag.transport.person.ForelderBarnRelasjonDto
import no.nav.bidrag.transport.person.HentePersonidenterRequest
import no.nav.bidrag.transport.person.Husstand
import no.nav.bidrag.transport.person.Husstandsmedlem
import no.nav.bidrag.transport.person.HusstandsmedlemmerDto
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.NavnFødselDødDto
import no.nav.bidrag.transport.person.PersonRequest
import no.nav.bidrag.transport.person.PersonidentDto
import no.nav.bidrag.transport.person.SivilstandPdlDto
import no.nav.bidrag.transport.person.SivilstandPdlHistorikkDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
@DisplayName("BidragPersonConsumerTest")
internal class BidragPersonConsumerTest {
    @MockK
    private lateinit var restTemplateMock: RestTemplate

    @MockK
    private lateinit var grunnlagConsumerMock: GrunnlagConsumer

    private lateinit var bidragPersonConsumer: BidragPersonConsumer

    @BeforeEach
    fun setup() {
        bidragPersonConsumer = BidragPersonConsumer(
            URI("http://localhost"),
            restTemplateMock,
            grunnlagConsumerMock,
        )
    }

    @Test
    fun `hentNavnFødselOgDød skal returnere ok respons`() {
        val personident = Personident("12345678901")
        val request = PersonRequest(personident)
        val response =
            NavnFødselDødDto(navn = "Fornavn Etternavn", fødselsdato = LocalDate.now(), fødselsår = LocalDate.now().year, dødsdato = LocalDate.now())
        val httpEntity = HttpEntity(request)
        val responseEntity = ResponseEntity(response, HttpStatus.OK)

        every { grunnlagConsumerMock.initHttpEntity(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<NavnFødselDødDto>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/navnfoedseldoed"),
                HttpMethod.POST,
                httpEntity,
                NavnFødselDødDto::class.java,
            )
        } returns responseEntity

        // Consumer-kall
        val restResponse = bidragPersonConsumer.hentNavnFødselOgDød(personident)

        // Assertions
        restResponse is RestResponse.Success
        (restResponse as RestResponse.Success).body shouldBe response
    }

    @Test
    fun `hentNavnFødselOgDød skal håndtere exception`() {
        val personident = Personident("12345678901")
        val request = PersonRequest(personident)
        val httpEntity = HttpEntity(request)

        every { grunnlagConsumerMock.initHttpEntity(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<NavnFødselDødDto>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/navnfoedseldoed"),
                HttpMethod.POST,
                httpEntity,
                NavnFødselDødDto::class.java,
            )
        } throws HttpClientErrorException(HttpStatus.BAD_REQUEST)

        // Consumer-kall
        val restResponse = bidragPersonConsumer.hentNavnFødselOgDød(personident)

        // Assertions
        restResponse is RestResponse.Failure
        (restResponse as RestResponse.Failure).statusCode shouldBe HttpStatus.BAD_REQUEST
    }

    @Test
    fun `hentForelderBarnRelasjon skal returnere ok respons`() {
        val personident = Personident("12345678901")
        val request = PersonRequest(personident)
        val response = ForelderBarnRelasjonDto(
            forelderBarnRelasjon = listOf(
                ForelderBarnRelasjon(
                    minRolleForPerson = Familierelasjon.BARN,
                    relatertPersonsIdent = personident,
                    relatertPersonsRolle = Familierelasjon.FORELDER,
                ),
            ),
        )
        val httpEntity = HttpEntity(request)
        val responseEntity = ResponseEntity(response, HttpStatus.OK)

        every { grunnlagConsumerMock.initHttpEntity(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<NavnFødselDødDto>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/forelderbarnrelasjon"),
                HttpMethod.POST,
                httpEntity,
                ForelderBarnRelasjonDto::class.java,
            )
        } returns responseEntity

        // Consumer-kall
        val restResponse = bidragPersonConsumer.hentForelderBarnRelasjon(personident)

        // Assertions
        restResponse is RestResponse.Success
        (restResponse as RestResponse.Success).body shouldBe response
    }

    @Test
    fun `hentHusstandsmedlemmer skal returnere ok respons`() {
        val personident = Personident("12345678901")
        val request = HusstandsmedlemmerRequest(personRequest = PersonRequest(ident = personident), periodeFra = LocalDate.now())
        val response = HusstandsmedlemmerDto(
            husstandListe = listOf(
                Husstand(
                    gyldigFraOgMed = LocalDate.now(),
                    gyldigTilOgMed = LocalDate.now(),
                    adressenavn = "Adresse",
                    husnummer = "1",
                    husbokstav = "A",
                    bruksenhetsnummer = "1",
                    postnummer = "1111",
                    bydelsnummer = "1",
                    kommunenummer = "1",
                    matrikkelId = 1,
                    husstandsmedlemListe = listOf(
                        Husstandsmedlem(
                            gyldigFraOgMed = LocalDate.now(),
                            gyldigTilOgMed = LocalDate.now(),
                            personId = personident,
                            navn = "Navn",
                            fødselsdato = LocalDate.now(),
                            dødsdato = LocalDate.now(),
                        ),
                    ),
                ),
            ),
        )
        val httpEntity = HttpEntity(request)
        val responseEntity = ResponseEntity(response, HttpStatus.OK)

        every { grunnlagConsumerMock.initHttpEntity(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<HusstandsmedlemmerDto>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/husstandsmedlemmer"),
                HttpMethod.POST,
                httpEntity,
                HusstandsmedlemmerDto::class.java,
            )
        } returns responseEntity

        // Consumer-kall
        val restResponse = bidragPersonConsumer.hentHusstandsmedlemmer(request)

        // Assertions
        restResponse is RestResponse.Success
        (restResponse as RestResponse.Success).body shouldBe response
    }

    @Test
    fun `hentSivilstand skal returnere ok respons`() {
        val personident = Personident("12345678901")
        val request = PersonRequest(personident)
        val response = SivilstandPdlHistorikkDto(
            sivilstandPdlDto = listOf(
                SivilstandPdlDto(
                    type = SivilstandskodePDL.GIFT,
                    gyldigFom = LocalDate.now(),
                    relatertVedSivilstand = "",
                    bekreftelsesdato = LocalDate.now(),
                    master = "",
                    registrert = LocalDateTime.now(),
                    historisk = false,
                ),
            ),
        )
        val httpEntity = HttpEntity(request)
        val responseEntity = ResponseEntity(response, HttpStatus.OK)

        every { grunnlagConsumerMock.initHttpEntity(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<SivilstandPdlHistorikkDto>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/sivilstand"),
                HttpMethod.POST,
                httpEntity,
                SivilstandPdlHistorikkDto::class.java,
            )
        } returns responseEntity

        // Consumer-kall
        val restResponse = bidragPersonConsumer.hentSivilstand(personident)

        // Assertions
        restResponse is RestResponse.Success
        (restResponse as RestResponse.Success).body shouldBe response
    }

    @Test
    fun `hentPersonidenter skal returnere ok respons`() {
        val personident = Personident("12345678901")
        val request = HentePersonidenterRequest(personident.verdi, setOf(Identgruppe.FOLKEREGISTERIDENT), false)
        val response = listOf(
            PersonidentDto(
                ident = personident.verdi,
                historisk = false,
                gruppe = Identgruppe.FOLKEREGISTERIDENT,
            ),
        )
        val httpEntity = HttpEntity(request)
        val responseEntity = ResponseEntity(response, HttpStatus.OK)
        val responseType = object : ParameterizedTypeReference<List<PersonidentDto>>() {}

        every { grunnlagConsumerMock.initHttpEntity(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<List<PersonidentDto>>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/personidenter"),
                HttpMethod.POST,
                httpEntity,
                responseType,
            )
        } returns responseEntity

        // Consumer-kall
        val restResponse = bidragPersonConsumer.hentPersonidenter(personident, false)

        // Assertions
        restResponse is RestResponse.Success
        (restResponse as RestResponse.Success).body shouldBe response
    }
}

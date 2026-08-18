package no.nav.bidrag.organisasjon.controller

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import no.nav.bidrag.organisasjon.BidragOrganisasjonTest
import no.nav.bidrag.organisasjon.TEST
import no.nav.bidrag.organisasjon.TestRestTemplateConfiguration
import no.nav.bidrag.organisasjon.WEB_ENV_TEST
import no.nav.bidrag.organisasjon.testdata.ENHET_NR
import no.nav.bidrag.organisasjon.testdata.StubUtils
import no.nav.bidrag.transport.organisasjon.EnhetKontaktinfoDto
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.resttestclient.exchange
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock

@ActiveProfiles(TEST, WEB_ENV_TEST)
@SpringBootTest(classes = [BidragOrganisasjonTest::class, StubUtils::class], webEnvironment = WebEnvironment.RANDOM_PORT)
@EnableMockOAuth2Server
@EnableWireMock(ConfigureWireMock(port = 0))
@AutoConfigureTestRestTemplate
internal class OrganisasjonControllerTest {
    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var testRestTemplateConfiguration: TestRestTemplateConfiguration

    @Autowired
    lateinit var stubs: StubUtils

    @LocalServerPort
    private val port = 0

    @Value($$"${server.servlet.context-path}")
    lateinit var contextPath: String

    @Test
    fun shouldGetEnhetKontaktinfo() {
        stubs.stubEnhetKontaktinfo()
        stubs.stubEnhetInfo()
        val response =
            testRestTemplate.exchange<EnhetKontaktinfoDto>(
                "${rootUri()}/${OrganisasjonController.ENDPOINT_ENHET_KONTAKTINFO}/$ENHET_NR",
                HttpMethod.GET,
                testRestTemplateConfiguration.initHttpEntity<Any>(),
            )

        val body = response.body
        assertSoftly {
            response.statusCode shouldBe HttpStatus.OK
            body?.nummer?.verdi shouldBe ENHET_NR
            body?.navn shouldBe "Nav familie- og pensjonsytelser Drammen"
            body?.telefonnummer shouldBe "55553333"
            body?.postadresse?.postnummer shouldBe "3007"
            body?.postadresse?.poststed shouldBe "Drammen"
            body?.postadresse?.adresselinje1 shouldBe "Postboks 1583"
            body?.postadresse?.land shouldBe "Norway"
        }
    }

    @Test
    fun shouldGetEnhetKontaktinfo2830() {
        stubs.stubEnhetKontaktinfo()
        stubs.stubEnhetInfo()
        val response =
            testRestTemplate.exchange<EnhetKontaktinfoDto>(
                "${rootUri()}/${OrganisasjonController.ENDPOINT_ENHET_KONTAKTINFO}/2830",
                HttpMethod.GET,
                testRestTemplateConfiguration.initHttpEntity<Any>(),
            )

        val body = response.body
        assertSoftly {
            response.statusCode shouldBe HttpStatus.OK
            body?.nummer?.verdi shouldBe "2830"
            body?.navn shouldBe "DIR ytelsesavdelingen"
            body?.telefonnummer shouldBe "55553333"
            body?.postadresse?.postnummer shouldBe "8601"
            body?.postadresse?.poststed shouldBe "Mo i Rana"
            body?.postadresse?.adresselinje1 shouldBe "Postboks 354"
            body?.postadresse?.land shouldBe "Norge"
        }
    }

    @Test
    fun shouldGetEnhetKontaktinfoInEnglish() {
        stubs.stubEnhetKontaktinfo()
        stubs.stubEnhetInfo()
        val response =
            testRestTemplate.exchange<EnhetKontaktinfoDto>(
                "${rootUri()}/${OrganisasjonController.ENDPOINT_ENHET_KONTAKTINFO}/4865/EN",
                HttpMethod.GET,
                testRestTemplateConfiguration.initHttpEntity<Any>(),
            )

        val body = response.body
        assertSoftly {
            response.statusCode shouldBe HttpStatus.OK
            body?.nummer?.verdi shouldBe "4865"
            body?.navn shouldBe "Nav Family Benefits and Pensions - Child Support"
            body?.telefonnummer shouldBe "21073700"
            body?.postadresse?.postnummer shouldBe "0607"
            body?.postadresse?.poststed shouldBe "Oslo"
            body?.postadresse?.adresselinje1 shouldBe "Postboks 6600 Etterstad"
            body?.postadresse?.land shouldBe "Norway"
        }
    }

    private fun rootUri(): String = "http://localhost:$port/$contextPath"
}

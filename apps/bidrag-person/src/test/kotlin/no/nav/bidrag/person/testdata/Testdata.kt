package no.nav.bidrag.person.testdata

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import no.nav.bidrag.commons.service.KodeverkBeskrivelse
import no.nav.bidrag.commons.service.KodeverkBetydning
import no.nav.bidrag.commons.service.KodeverkKoderBetydningerResponse
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

var date1 = LocalDate.of(2020, 1, 1)
var fom = LocalDate.of(2020, 1, 1)
var tom = LocalDate.of(2020, 1, 1)
var date3: LocalDate = LocalDate.of(2025, 1, 1)
var dateTime1: LocalDateTime = LocalDateTime.of(2020, 1, 1, 1, 1)
var dateTime2: LocalDateTime = LocalDateTime.of(2022, 1, 1, 1, 1)
var dateTime3: LocalDateTime = LocalDateTime.of(2025, 1, 1, 1, 1)

fun createKodeverkResponse(): KodeverkKoderBetydningerResponse = KodeverkKoderBetydningerResponse(
    betydninger =
    mapOf(
        POSTNUMMER to
            listOf(
                KodeverkBetydning(
                    gyldigFra = fom,
                    gyldigTil = tom,
                    beskrivelser =
                    mapOf(
                        "nb" to
                            KodeverkBeskrivelse(
                                term = POSTSTED,
                                tekst = POSTSTED,
                            ),
                    ),
                ),
            ),
    ),
)

fun mockKodeverkResponse() {
    val resttemplateMock = mockk<RestTemplate>("RestTemplate", true)
    mockkConstructor(RestTemplateBuilder::class)
    every { anyConstructed<RestTemplateBuilder>().build() } returns resttemplateMock
    every {
        resttemplateMock.getForEntity<KodeverkKoderBetydningerResponse>(any<String>())
    } returns ResponseEntity.of(Optional.of(createKodeverkResponse()))
}

package no.nav.bidrag.organisasjon.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.bidrag.commons.web.HttpResponse
import no.nav.bidrag.domene.enums.diverse.Tema
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.organisasjon.consumer.EntraConsumer
import no.nav.bidrag.organisasjon.consumer.Norg2Consumer
import no.nav.bidrag.organisasjon.consumer.PersonConsumer
import no.nav.bidrag.organisasjon.consumer.SkjermingConsumer
import no.nav.bidrag.organisasjon.consumer.dto.ArbeidsfordelingEnheterBestMatchRequest
import no.nav.bidrag.organisasjon.consumer.dto.ArbeidsfordelingEnheterBestMatchResponse
import no.nav.bidrag.organisasjon.consumer.dto.ArbeidsfordelingEnheterResponse
import no.nav.bidrag.organisasjon.service.OrganisasjonService.Companion.BEHANDLINGSTYPE_UTLAND
import no.nav.bidrag.transport.organisasjon.HentEnhetRequest
import no.nav.bidrag.transport.person.GeografiskTilknytningDto
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

internal class OrganisasjonServiceTest {
    private val norg2ConsumerMock: Norg2Consumer = mockk(relaxed = true)

    private val personConsumerMock: PersonConsumer = mockk(relaxed = true)

    private val skjermingConsumerMock: SkjermingConsumer = mockk(relaxed = true)
    private val entraConsumer: EntraConsumer = mockk(relaxed = true)

    private val organisasjonService: OrganisasjonService =
        OrganisasjonService(
            entraConsumer,
            norg2ConsumerMock,
            personConsumerMock,
            skjermingConsumerMock,
        )

    @Nested
    inner class HentArbeidsfordelingJournalforendeEnheter {
        @Test
        fun `Skal hente liste over journalførende enheter fra arbeidsfordeling`() {
            val enhetsliste =
                listOf(
                    ArbeidsfordelingEnheterResponse(Enhetsnummer("100001596"), "Bærum", KLAGE),
                    ArbeidsfordelingEnheterResponse(Enhetsnummer("100001796"), "Drammen", FORVALTNING),
                )
            every { norg2ConsumerMock.finnArbeidsfordelingEnheterListe(any()) }.returns(HttpResponse.from(HttpStatus.OK, enhetsliste))

            val response = organisasjonService.hentArbeidsfordelingJournalforendeEnheter()

            response.responseEntity.statusCode shouldBe HttpStatus.OK
            response.responseEntity.body?.size shouldBe 2
            response.responseEntity.body?.get(0)?.nummer?.verdi shouldBe "100001596"
            response.responseEntity.body?.get(0)?.navn shouldBe "Bærum"
            response.responseEntity.body?.get(0)?.type shouldBe "Klage"
            response.responseEntity.body?.get(1)?.nummer?.verdi shouldBe "100001796"
            response.responseEntity.body?.get(1)?.navn shouldBe "Drammen"
            response.responseEntity.body?.get(1)?.type shouldBe "Forvaltning"
        }

        @Test
        fun `Skal returnere no content hvis tom liste over journalførende enheter returneres fra arbeidsfordeling`() {
            every { norg2ConsumerMock.finnArbeidsfordelingEnheterListe(any()) }.returns(HttpResponse.from(HttpStatus.OK, emptyList()))

            val response = organisasjonService.hentArbeidsfordelingJournalforendeEnheter()

            response.responseEntity.statusCode shouldBe HttpStatus.NO_CONTENT
            response.responseEntity.body shouldBe null
        }

        @Test
        fun `Skal returnere no content hvis body er null returneres fra arbeidsfordeling journalførende enheter`() {
            every { norg2ConsumerMock.finnArbeidsfordelingEnheterListe(any()) }.returns(HttpResponse.from(HttpStatus.OK, null))

            val response = organisasjonService.hentArbeidsfordelingJournalforendeEnheter()

            response.responseEntity.statusCode shouldBe HttpStatus.NO_CONTENT
            response.responseEntity.body shouldBe null
        }
    }

    @Nested
    inner class HentArbeidsfordelingGeografiskTilknytningEnheter {
        @Test
        fun `Skal hente liste over enheter fra arbeidsfordeling basert på geografisk tilknytning`() {
            val personGTResponse =
                GeografiskTilknytningDto(
                    ident = IDENT,
                    geografiskTilknytning = "SWE",
                    erUtland = true,
                )
            val arbeidsfordelingResponse = listOf(ArbeidsfordelingEnheterBestMatchResponse(Enhetsnummer("EnhetId"), "EnhetNavn"))
            val afRequestCaptor = slot<ArbeidsfordelingEnheterBestMatchRequest>()
            every { personConsumerMock.hentPersonGeografiskTilknytning(IDENT) }.returns(personGTResponse)
            every { skjermingConsumerMock.erPersonSkjermet(any()) }.returns(true)
            every { norg2ConsumerMock.finnArbeidsfordelingEnheterBestMatch(capture(afRequestCaptor)) }.returns(arbeidsfordelingResponse)

            val enhetDto = organisasjonService.hentArbeidsfordelingGeografiskTilknytningEnheter(IDENT)

            enhetDto!!.nummer.verdi shouldBe ("EnhetId")
            enhetDto.navn shouldBe ("EnhetNavn")
            afRequestCaptor.captured.geografiskOmraade shouldBe personGTResponse.geografiskTilknytning
            afRequestCaptor.captured.diskresjonskode shouldBe personGTResponse.diskresjonskode
            afRequestCaptor.captured.skjermet shouldBe true
            afRequestCaptor.captured.tema shouldBe Tema.TEMA_BIDRAG.verdi
        }

        @Test
        fun `Skal hente arbeidsfordeling basert geografisk tilknytning for utland`() {
            val personGTResponse =
                GeografiskTilknytningDto(
                    ident = IDENT,
                    geografiskTilknytning = "SWE",
                    erUtland = true,
                )
            val arbeidsfordelingResponse = listOf(ArbeidsfordelingEnheterBestMatchResponse(Enhetsnummer("EnhetId"), "EnhetNavn"))
            val afRequestCaptor = slot<ArbeidsfordelingEnheterBestMatchRequest>()
            every { personConsumerMock.hentPersonGeografiskTilknytning(IDENT) }.returns(personGTResponse)
            every { skjermingConsumerMock.erPersonSkjermet(any()) }.returns(false)
            every { norg2ConsumerMock.finnArbeidsfordelingEnheterBestMatch(capture(afRequestCaptor)) }.returns(arbeidsfordelingResponse)

            val response = organisasjonService.hentArbeidsfordelingGeografiskTilknytningEnheter(IDENT)

            response!!.nummer.verdi shouldBe "EnhetId"
            response.navn shouldBe "EnhetNavn"
            afRequestCaptor.captured.geografiskOmraade shouldBe (personGTResponse.geografiskTilknytning)
            afRequestCaptor.captured.diskresjonskode shouldBe null
            afRequestCaptor.captured.skjermet shouldBe false
            afRequestCaptor.captured.behandlingstype shouldBe BEHANDLINGSTYPE_UTLAND
            afRequestCaptor.captured.tema shouldBe Tema.TEMA_BIDRAG.verdi
        }

        @Test
        fun `Skal returnere no content hvis tom liste returneres fra arbeidsfordeling basert på geografisk tilknytning`() {
            val personGTResponse =
                GeografiskTilknytningDto(
                    ident = IDENT,
                    geografiskTilknytning = "SWE",
                    erUtland = true,
                )
            val afRequestCaptor = slot<ArbeidsfordelingEnheterBestMatchRequest>()
            every { personConsumerMock.hentPersonGeografiskTilknytning(IDENT) }.returns(personGTResponse)
            every { skjermingConsumerMock.erPersonSkjermet(any()) }.returns(true)
            every { norg2ConsumerMock.finnArbeidsfordelingEnheterBestMatch(capture(afRequestCaptor)) }.returns(
                emptyList(),
            )

            val response = organisasjonService.hentArbeidsfordelingGeografiskTilknytningEnheter(IDENT)

            response shouldBe null
            afRequestCaptor.captured.geografiskOmraade shouldBe personGTResponse.geografiskTilknytning
            afRequestCaptor.captured.diskresjonskode shouldBe personGTResponse.diskresjonskode
            afRequestCaptor.captured.skjermet shouldBe true
            afRequestCaptor.captured.tema shouldBe Tema.TEMA_BIDRAG.verdi
        }

        @Test
        fun `Skal returnere no content hvis body er null returneres fra arbeidsfordeling basert på geografisk tilknytning`() {
            val personGTResponse =
                GeografiskTilknytningDto(
                    ident = IDENT,
                    geografiskTilknytning = "SWE",
                    erUtland = true,
                )
            val afRequestCaptor = slot<ArbeidsfordelingEnheterBestMatchRequest>()
            every { personConsumerMock.hentPersonGeografiskTilknytning(IDENT) }.returns(personGTResponse)
            every { skjermingConsumerMock.erPersonSkjermet(any()) }.returns(true)
            every { norg2ConsumerMock.finnArbeidsfordelingEnheterBestMatch(capture(afRequestCaptor)) }.returns(null)

            val response = organisasjonService.hentArbeidsfordelingGeografiskTilknytningEnheter(IDENT)

            response shouldBe null
            afRequestCaptor.captured.geografiskOmraade shouldBe personGTResponse.geografiskTilknytning
            afRequestCaptor.captured.diskresjonskode shouldBe personGTResponse.diskresjonskode
            afRequestCaptor.captured.skjermet shouldBe true
            afRequestCaptor.captured.tema shouldBe Tema.TEMA_BIDRAG.verdi
        }
    }

    @Nested
    inner class HentArbeidsfordelingGeografiskTilknytningEnhet {
        @Test
        fun hentArbeidsfordelingGeografiskTilknytningEnhet_ReturnererNullHvisTomListeReturneresFraArbeidsfordelingBasertPaaGeografiskTilknytning() {
            val personGTResponse =
                GeografiskTilknytningDto(
                    ident = IDENT,
                    geografiskTilknytning = "SWE",
                    erUtland = true,
                )
            every { personConsumerMock.hentPersonGeografiskTilknytning(IDENT) }.returns(personGTResponse)
            every { skjermingConsumerMock.erPersonSkjermet(any()) }.returns(true)
            every { norg2ConsumerMock.finnArbeidsfordelingEnheterBestMatch(any()) }.returns(emptyList())

            val response = organisasjonService.hentArbeidsfordelingGeografiskTilknytningEnhet(HentEnhetRequest(IDENT))

            response shouldBe null
        }

        @Test
        fun hentArbeidsfordelingGeografiskTilknytningEnhet_ReturnererNullHvisBodyErNullReturneresFraHentPersonGeografiskTilknytning() {
            val personGTResponse =
                GeografiskTilknytningDto(
                    ident = IDENT,
                    geografiskTilknytning = "SWE",
                    erUtland = true,
                )
            every { personConsumerMock.hentPersonGeografiskTilknytning(IDENT) }.returns(personGTResponse)
            every { skjermingConsumerMock.erPersonSkjermet(any()) }.returns(true)
            every { norg2ConsumerMock.finnArbeidsfordelingEnheterBestMatch(any()) }.returns(null)

            val response = organisasjonService.hentArbeidsfordelingGeografiskTilknytningEnhet(HentEnhetRequest(IDENT))

            response shouldBe null
        }
    }

    companion object {
        private val SB_IDENT = "X123456"
        private val SB_NAVN = "Sylfest Strutle"
        private val IDENT = Personident("12345678900")
        private const val FORVALTNING = "FPY"
        private const val KLAGE = "KLAGE"
    }
}

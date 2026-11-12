package no.nav.bidrag.organisasjon.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.bidrag.commons.web.HttpResponse
import no.nav.bidrag.domene.enums.diverse.Tema
import no.nav.bidrag.domene.enums.person.Gradering
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.vedtak.Behandlingstype
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.generer.testdata.person.genererPersonident
import no.nav.bidrag.organisasjon.consumer.EntraConsumer
import no.nav.bidrag.organisasjon.consumer.Norg2Consumer
import no.nav.bidrag.organisasjon.consumer.PersonConsumer
import no.nav.bidrag.organisasjon.consumer.SkjermingConsumer
import no.nav.bidrag.organisasjon.consumer.dto.ArbeidsfordelingEnheterBestMatchRequest
import no.nav.bidrag.organisasjon.consumer.dto.ArbeidsfordelingEnheterBestMatchResponse
import no.nav.bidrag.organisasjon.consumer.dto.ArbeidsfordelingEnheterResponse
import no.nav.bidrag.organisasjon.service.OrganisasjonService.Companion.BEHANDLINGSTYPE_UTLAND
import no.nav.bidrag.transport.organisasjon.ArbeidsfordelingSakRequest
import no.nav.bidrag.transport.organisasjon.EnhetDetaljerDto
import no.nav.bidrag.transport.organisasjon.EnhetDto
import no.nav.bidrag.transport.organisasjon.HentEnhetRequest
import no.nav.bidrag.transport.person.GeografiskTilknytningDto
import no.nav.bidrag.transport.person.Graderingsinfo
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

        @Test
        fun `Skal returnere settekontor når saken har arbeidsfordeling settekontor`() {
            mockNorgTreffMedSettekontor()

            val response = organisasjonService.hentArbeidsfordelingGeografiskTilknytningEnhet(
                HentEnhetRequest(IDENT, arbeidsfordeling = Arbeidsfordeling.SETTEKONTOR),
            )

            response shouldBe EnhetDto(Enhetsnummer("4806"), "NAV Familie- og pensjonsytelser Drammen")
        }

        @Test
        fun `Skal returnere Norg-enheten når saken ikke har arbeidsfordeling settekontor`() {
            mockNorgTreffMedSettekontor()

            val response = organisasjonService.hentArbeidsfordelingGeografiskTilknytningEnhet(
                HentEnhetRequest(IDENT, arbeidsfordeling = Arbeidsfordeling.EIERENHET),
            )

            response shouldBe EnhetDto(Enhetsnummer("4812"), "NAV Familie- og pensjonsytelser Bergen")
        }

        @Test
        fun `Skal beholde enheten fra Norg når settekontor ikke er konfigurert`() {
            mockNorgTreffMedSettekontor()
            every { norg2ConsumerMock.hentEnhetKontaktinfo("4812") } returns
                EnhetDetaljerDto(enhetId = "4812", navn = "NAV Familie- og pensjonsytelser Bergen", settekontorEnhetId = null)

            val response = organisasjonService.hentArbeidsfordelingGeografiskTilknytningEnhet(
                HentEnhetRequest(IDENT, arbeidsfordeling = Arbeidsfordeling.SETTEKONTOR),
            )

            response shouldBe EnhetDto(Enhetsnummer("4812"), "NAV Familie- og pensjonsytelser Bergen")
        }

        @Test
        fun `Skal gi skjermet når en av partene er skjermet`() {
            mockNorgTreffMedSettekontor()
            mockGraderingsinfo(skjermetAnnenPart = true)

            val response = organisasjonService.hentArbeidsfordelingForSak(arbeidsfordelingSakRequest(Arbeidsfordeling.FARSKAP))

            response?.skjermet shouldBe true
        }

        @Test
        fun `Skal gi ikke skjermet når ingen av partene er skjermet`() {
            mockNorgTreffMedSettekontor()
            mockGraderingsinfo(skjermetAnnenPart = false)

            val response = organisasjonService.hentArbeidsfordelingForSak(arbeidsfordelingSakRequest(Arbeidsfordeling.FARSKAP))

            response?.skjermet shouldBe false
        }

        @Test
        fun `Skal beholde behandlingstema og Norg-enheten for farskap`() {
            mockNorgTreffMedSettekontor()
            mockGraderingsinfo(skjermetAnnenPart = false)
            val norgRequest = slot<ArbeidsfordelingEnheterBestMatchRequest>()
            every { norg2ConsumerMock.finnArbeidsfordelingEnheterBestMatch(capture(norgRequest)) } returns
                listOf(ArbeidsfordelingEnheterBestMatchResponse(Enhetsnummer("4812"), "NAV Familie- og pensjonsytelser Bergen"))

            val response = organisasjonService.hentArbeidsfordelingForSak(arbeidsfordelingSakRequest(Arbeidsfordeling.FARSKAP))

            response?.enhet?.nummer shouldBe Enhetsnummer("4812")
            norgRequest.captured.behandlingstema shouldBe Arbeidsfordeling.FARSKAP.behandlingstema
        }

        @Test
        fun `Skal gi settekontor for INH uten skjerming`() {
            mockNorgTreffMedSettekontor()
            mockGraderingsinfo(skjermetAnnenPart = false)

            val response = organisasjonService.hentArbeidsfordelingForSak(arbeidsfordelingSakRequest(Arbeidsfordeling.SETTEKONTOR))

            response?.enhet?.nummer shouldBe Enhetsnummer("4806")
        }

        @Test
        fun `Skal beholde Norg-enheten for EEN`() {
            mockNorgTreffMedSettekontor()
            mockGraderingsinfo(skjermetAnnenPart = false)

            val response = organisasjonService.hentArbeidsfordelingForSak(arbeidsfordelingSakRequest(Arbeidsfordeling.EIERENHET))

            response?.enhet?.nummer shouldBe Enhetsnummer("4812")
        }

        @Test
        fun `Skal beholde Norg-enheten når INH mangler kontaktinformasjon`() {
            mockNorgTreffMedSettekontor()
            mockGraderingsinfo(skjermetAnnenPart = false)
            every { norg2ConsumerMock.hentEnhetKontaktinfo("4812") } returns null

            val response = organisasjonService.hentArbeidsfordelingForSak(arbeidsfordelingSakRequest(Arbeidsfordeling.SETTEKONTOR))

            response?.enhet?.nummer shouldBe Enhetsnummer("4812")
        }

        @Test
        fun `Skal beholde enheten fra Norg når inhabil sak har skjermet part`() {
            mockNorgTreffMedSettekontor()
            mockGraderingsinfo(skjermetAnnenPart = true)

            val response = organisasjonService.hentArbeidsfordelingForSak(arbeidsfordelingSakRequest(Arbeidsfordeling.SETTEKONTOR))

            response?.enhet?.nummer shouldBe Enhetsnummer("4812")
        }

        @Test
        fun `Skal beholde enheten fra Norg når inhabil sak har part med adressebeskyttelse`() {
            mockNorgTreffMedSettekontor()
            every { personConsumerMock.hentGraderingsinfo(setOf(ANNEN_PART, IDENT)) } returns
                Graderingsinfo(
                    identerTilGradering = mapOf(IDENT to null, ANNEN_PART to Gradering.FORTROLIG),
                    identerTilSkjerming = mapOf(IDENT to false, ANNEN_PART to false),
                )

            val response = organisasjonService.hentArbeidsfordelingForSak(arbeidsfordelingSakRequest(Arbeidsfordeling.SETTEKONTOR))

            response?.enhet?.nummer shouldBe Enhetsnummer("4812")
        }

        private fun mockGraderingsinfo(skjermetAnnenPart: Boolean) {
            every { personConsumerMock.hentGraderingsinfo(setOf(ANNEN_PART, IDENT)) } returns
                Graderingsinfo(identerTilGradering = emptyMap(), identerTilSkjerming = mapOf(IDENT to false, ANNEN_PART to skjermetAnnenPart))
        }

        private fun arbeidsfordelingSakRequest(arbeidsfordeling: Arbeidsfordeling) = ArbeidsfordelingSakRequest(
            hovedident = IDENT,
            parter = setOf(IDENT, ANNEN_PART),
            arbeidsfordeling = arbeidsfordeling,
            sakskategori = Sakskategori.NASJONAL,
            behandlingstype = Behandlingstype.SØKNAD,
        )

        private fun mockNorgTreffMedSettekontor() {
            every { personConsumerMock.hentPersonGeografiskTilknytning(IDENT) } returns
                GeografiskTilknytningDto(ident = IDENT, geografiskTilknytning = "4601", erUtland = false)
            every { norg2ConsumerMock.finnArbeidsfordelingEnheterBestMatch(any()) } returns
                listOf(ArbeidsfordelingEnheterBestMatchResponse(Enhetsnummer("4812"), "NAV Familie- og pensjonsytelser Bergen"))
            every { norg2ConsumerMock.hentEnhetKontaktinfo("4812") } returns
                EnhetDetaljerDto(enhetId = "4812", navn = "NAV Familie- og pensjonsytelser Bergen", settekontorEnhetId = "4806")
            every { norg2ConsumerMock.hentEnhetKontaktinfo("4806") } returns
                EnhetDetaljerDto(enhetId = "4806", navn = "NAV Familie- og pensjonsytelser Drammen", settekontorEnhetId = null)
        }
    }

    companion object {
        private val SB_IDENT = "X123456"
        private val SB_NAVN = "Sylfest Strutle"
        private val IDENT = genererPersonident()
        private val ANNEN_PART = genererPersonident()
        private const val FORVALTNING = "FPY"
        private const val KLAGE = "KLAGE"
    }
}

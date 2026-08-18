package no.nav.bidrag.dokument.journalpost.controller

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.date.shouldHaveSameDayAs
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import no.nav.bidrag.dokument.journalpost.entity.Journalpost
import no.nav.bidrag.dokument.journalpost.entity.KodeBrevBygger
import no.nav.bidrag.dokument.journalpost.model.Dokstatus
import no.nav.bidrag.dokument.journalpost.model.JP_ARKIVDEL
import no.nav.bidrag.dokument.journalpost.model.JP_SYSTEMID_BISYS
import no.nav.bidrag.transport.dokument.AktorDto
import no.nav.bidrag.transport.dokument.AvsenderMottakerDto
import no.nav.bidrag.transport.dokument.JournalpostType
import no.nav.bidrag.transport.dokument.OpprettDokumentDto
import no.nav.bidrag.transport.dokument.OpprettJournalpostRequest
import no.nav.bidrag.transport.dokument.OpprettJournalpostResponse
import org.junit.Ignore
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.time.LocalDate
import java.time.LocalDateTime

@Disabled("")
internal class JournalpostControllerOpprettTest : AbstractControllerTestKotlin() {
    @BeforeEach
    fun initKodeBrev() {
        testDataManager.opprett(
            KodeBrevBygger
                .enGyldigBrevkode()
                .medKode("BITEST")
                .medKravtype("VE")
                .medDekode("Et laaangt brev"),
        )
    }

    @Test
    @Disabled
    fun `skal opprette utgaanede journalpost`() {
        val tilknyttSak = "1231231"
        val mottakerId = "123213123"
        val gjelderId = "123213355555"
        stubUtils.stubHentPerson(mottakerId)
        stubUtils.stubHentSaksbehandler()
        stubUtils.stubHentEnhet()
        val opprettJournalpost = createOpprettJournalpostRequest(mottakerId, gjelderId, tilknyttSak, brevkode = "BITEST")
        val opprettResponse =
            httpHeaderTestRestTemplate.exchange(
                baseUrl() + "/journalpost",
                HttpMethod.POST,
                HttpEntity(opprettJournalpost),
                OpprettJournalpostResponse::class.java,
            )
        opprettResponse.statusCode shouldBe HttpStatus.OK

        val responseBody = opprettResponse.body!!

        responseBody.journalpostId shouldNotBe null
        responseBody.dokumenter shouldHaveSize 1

        val journalpost =
            testDataManager.hent(responseBody.journalpostId?.toInt(), Journalpost::class.java).orElseThrow {
                RuntimeException("Fant ikke journalpost ${responseBody.journalpostId}")
            }

        journalpost.dokumentreferanse shouldNotBe null
        journalpost.dokumentreferanse shouldBe responseBody.dokumenter[0].dokumentreferanse
        journalpost.brukerid shouldBe "audlocalhost"
        journalpost.beskrivelse shouldBe "Tittel på dokument"
        journalpost.dokumentType shouldBe "U"
        journalpost.avsender shouldBe "Etternavn"
        journalpost.avsenderFornavn shouldBe "Fornavn Mellomnavn"
        journalpost.journalforendeEnhet shouldBe "4806"
        journalpost.journalforendeEnhetNavn shouldBe "NAV Familie- og pensjonsytelser Drammen"
        journalpost.brevkode shouldBe "BITEST"
        journalpost.filnavn shouldBe "refid"
        journalpost.tjeneste shouldBe "AN"
        journalpost.kravtype shouldBe "VE"
        journalpost.arkivdel shouldBe JP_ARKIVDEL
        journalpost.systemId shouldBe JP_SYSTEMID_BISYS
        journalpost.dokstatus shouldBe Dokstatus.DOKBESKRIVELSE_STATUS
        journalpost.journalstatus shouldBe "D"
        journalpost.journalfortAv shouldBe "Saksbehandler Navnesen"
        journalpost.mottakerId shouldBe mottakerId
        journalpost.gjelder shouldBe gjelderId
        journalpost.journalsaker shouldHaveSize 1
        journalpost.journalsaker[0].saksnummer shouldBe tilknyttSak

        journalpost.journaldato shouldHaveSameDayAs LocalDate.now()
        journalpost.dokumentdato shouldHaveSameDayAs LocalDate.now()
        journalpost.arkiveringstidspunkt shouldHaveSameDayAs LocalDateTime.now()

        stubUtils.Verify().verifyHentSaksbehandlerCalledWith("audlocalhost")
        stubUtils.Verify().verifyHentEnhetCalledWith("4806")
    }

    @Test
    @Ignore
    fun `skal opprette utgaanede journalpost when kodeverk and enhet not found`() {
        val tilknyttSak = "1231231"
        val mottakerId = "123213123"
        val gjelderId = "123213355555"
        stubUtils.stubHentPerson(mottakerId)
        stubUtils.stubHentSaksbehandler()
        stubUtils.stubHentEnhet(status = HttpStatus.NOT_FOUND)
        val opprettJournalpost = createOpprettJournalpostRequest(mottakerId, gjelderId, tilknyttSak, brevkode = "BITEST_NOT_EXISTING")
        val opprettResponse =
            httpHeaderTestRestTemplate.exchange(
                baseUrl() + "/journalpost",
                HttpMethod.POST,
                HttpEntity(opprettJournalpost),
                OpprettJournalpostResponse::class.java,
            )
        opprettResponse.statusCode shouldBe HttpStatus.OK

        val responseBody = opprettResponse.body!!

        responseBody.journalpostId shouldNotBe null
        responseBody.dokumenter shouldHaveSize 1

        val journalpost =
            testDataManager.hent(responseBody.journalpostId?.toInt(), Journalpost::class.java).orElseThrow {
                RuntimeException("Fant ikke journalpost ${responseBody.journalpostId}")
            }

        journalpost.journalforendeEnhet shouldBe "4806"
        journalpost.journalforendeEnhetNavn shouldBe ""
        journalpost.brevkode shouldBe "BITEST_NOT_EXISTING"
        journalpost.kravtype shouldBe "AN"

        stubUtils.Verify().verifyHentSaksbehandlerCalledWith("audlocalhost")
        stubUtils.Verify().verifyHentEnhetCalledWith("4806")
    }

    @Test
    @Ignore
    fun `skal opprette journalpost med type notat`() {
        val tilknyttSak = "1231231"
        val mottakerId = "123213123"
        val gjelderId = "123213355555"
        stubUtils.stubHentPerson(mottakerId)
        stubUtils.stubHentSaksbehandler()
        val opprettJournalpost =
            createOpprettJournalpostRequest(mottakerId, gjelderId, tilknyttSak, journalposttype = JournalpostType.NOTAT)
        val opprettResponse =
            httpHeaderTestRestTemplate.exchange(
                baseUrl() + "/journalpost",
                HttpMethod.POST,
                HttpEntity(opprettJournalpost),
                OpprettJournalpostResponse::class.java,
            )
        opprettResponse.statusCode shouldBe HttpStatus.OK

        val responseBody = opprettResponse.body!!

        responseBody.journalpostId shouldNotBe null
        responseBody.dokumenter shouldHaveSize 1

        val journalpost =
            testDataManager.hent(responseBody.journalpostId?.toInt(), Journalpost::class.java).orElseThrow {
                RuntimeException("Fant ikke journalpost ${responseBody.journalpostId}")
            }

        journalpost.dokumentType shouldBe "X"
        journalpost.avsender shouldBe null
        journalpost.avsenderFornavn shouldBe null
        journalpost.mottakerId shouldBe null
    }

    @Test
    @Ignore
    fun `skal opprette utgaanede journalpost med saksbehandlerident fra input`() {
        stubUtils.stubHentPerson()
        stubUtils.stubHentSaksbehandler()
        val opprettJournalpost = createOpprettJournalpostRequest(saksbehandlerIdent = "Z99999")
        val opprettResponse =
            httpHeaderTestRestTemplate.exchange(
                baseUrl() + "/journalpost",
                HttpMethod.POST,
                HttpEntity(opprettJournalpost),
                OpprettJournalpostResponse::class.java,
            )
        opprettResponse.statusCode shouldBe HttpStatus.OK

        val responseBody = opprettResponse.body!!

        responseBody.journalpostId shouldBe "1"
        responseBody.dokumenter shouldHaveSize 1

        val journalpost =
            testDataManager.hent(responseBody.journalpostId?.toInt(), Journalpost::class.java).orElseThrow {
                RuntimeException("Fant ikke journalpost ${responseBody.journalpostId}")
            }

        journalpost.brukerid shouldBe "Z99999"
        journalpost.journalfortAv shouldBe "Saksbehandler Navnesen"

        stubUtils.Verify().verifyHentSaksbehandlerCalledWith("Z99999")
    }

    @Test
    fun `skal feile med json parse exception hvis ugyldig input`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val opprettResponse =
            httpHeaderTestRestTemplate.exchange(
                baseUrl() + "/journalpost",
                HttpMethod.POST,
                HttpEntity(
                    "{\"tittel\":\"Journalpost tittel\"," +
                        "\"gjelder\":{\"ident\":\"123213355555\",\"type\":null}," +
                        "\"avsenderMottaker\":{\"navn\":null,\"ident\":\"123213123\",\"type\":\"UKJENT\"}," +
                        "\"dokumenter\":[{\"tittel\":null,\"brevkode\":\"BI01\",\"dokumentreferanse\":null,\"dokument\":null}]," +
                        "\"tilknyttSaker\":[\"1231231\"]," +
                        "\"behandlingstema\":\"BEHTEM\"," +
                        "\"tema\":null," +
                        "\"journalposttype\":\"UTGAAENDE\"," +
                        "\"referanseId\":\"refid\"," +
                        "\"journalfoerendeEnhet\":\"4806\"}",
                    headers,
                ),
                Void::class.java,
            )
        opprettResponse.statusCode shouldBe HttpStatus.BAD_REQUEST
        opprettResponse.headers[HttpHeaders.WARNING]?.get(0) shouldContain "Ugyldig json input"
    }

    @Nested
    inner class InputValidation {
        @Test
        fun `skal feile validering hvis journalpost opprettet uten dokumenter`() {
            val tilknyttSak = "1231231"
            val mottakerId = "123213123"
            val gjelderId = "123213355555"
            stubUtils.stubHentPerson(mottakerId)
            stubUtils.stubHentSaksbehandler()
            val opprettJournalpost =
                OpprettJournalpostRequest(
                    avsenderMottaker = AvsenderMottakerDto(ident = mottakerId),
                    journalposttype = JournalpostType.UTGAAENDE,
                    journalfoerendeEnhet = "4806",
                    gjelder = AktorDto(gjelderId),
                    dokumenter = listOf(),
                    tilknyttSaker = listOf(tilknyttSak),
                )

            val opprettResponse =
                httpHeaderTestRestTemplate.exchange(
                    baseUrl() + "/journalpost",
                    HttpMethod.POST,
                    HttpEntity(opprettJournalpost),
                    Void::class.java,
                )
            opprettResponse.statusCode shouldBe HttpStatus.BAD_REQUEST
            opprettResponse.headers[HttpHeaders.WARNING]?.get(0) shouldContain "Journalpost må knyttes til et dokument"
        }

        @Test
        fun `skal feile validering hvis journalpost opprettet uten mottaker`() {
            val mottakerId = "123213123"
            stubUtils.stubHentPerson(mottakerId)
            stubUtils.stubHentSaksbehandler()
            val opprettJournalpost =
                OpprettJournalpostRequest(
                    journalposttype = JournalpostType.UTGAAENDE,
                    journalfoerendeEnhet = "4806",
                    dokumenter = listOf(OpprettDokumentDto("", brevkode = "BI01")),
                )

            val opprettResponse =
                httpHeaderTestRestTemplate.exchange(
                    baseUrl() + "/journalpost",
                    HttpMethod.POST,
                    HttpEntity(opprettJournalpost),
                    Void::class.java,
                )
            opprettResponse.statusCode shouldBe HttpStatus.BAD_REQUEST
            opprettResponse.headers[HttpHeaders.WARNING]?.get(0) shouldContain "Journalpost må knyttes til minst en sak"
            opprettResponse.headers[HttpHeaders.WARNING]?.get(0) shouldContain "Gjelder ident kan ikke være tom"
            opprettResponse.headers[HttpHeaders.WARNING]?.get(0) shouldContain "Mottaker ident kan ikke være tom"
            opprettResponse.headers[HttpHeaders.WARNING]?.get(0) shouldContain "Dokumentet journalpost knyttes må ha satt tittel"
        }
    }

    private fun createOpprettJournalpostRequest(
        mottakerId: String = "123213213",
        gjelderId: String = "213123213",
        tilknyttSak: String = "222222222",
        brevkode: String = "BITEST",
        journalposttype: JournalpostType = JournalpostType.UTGAAENDE,
        saksbehandlerIdent: String? = null,
    ): OpprettJournalpostRequest = OpprettJournalpostRequest(
        avsenderMottaker = AvsenderMottakerDto(ident = mottakerId),
        journalposttype = journalposttype,
        journalfoerendeEnhet = "4806",
        gjelder = AktorDto(gjelderId),
        dokumenter = listOf(OpprettDokumentDto("Tittel på dokument", brevkode = brevkode)),
        tilknyttSaker = listOf(tilknyttSak),
        behandlingstema = "BEHTEM",
        referanseId = "refid",
        tittel = "Journalpost tittel",
        saksbehandlerIdent = saksbehandlerIdent,
    )
}

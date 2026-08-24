package no.nav.bidrag.dokument.bestilling.api

import com.ninjasquad.springmockk.MockkSpyBean
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.verify
import no.nav.bidrag.dokument.bestilling.api.dto.DokumentBestillingForespørsel
import no.nav.bidrag.dokument.bestilling.api.dto.DokumentBestillingResponse
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DataGrunnlag
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMal
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMalBucket
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMalType
import no.nav.bidrag.dokument.bestilling.bestilling.dto.alleDokumentmaler
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.BrevserverProducer
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.DokumentProducer
import no.nav.bidrag.dokument.bestilling.consumer.dto.fornavnEtternavn
import no.nav.bidrag.dokument.bestilling.utils.BARN1
import no.nav.bidrag.dokument.bestilling.utils.BARN2
import no.nav.bidrag.dokument.bestilling.utils.BM1
import no.nav.bidrag.dokument.bestilling.utils.BP1
import no.nav.bidrag.dokument.bestilling.utils.SAKSBEHANDLER_IDENT
import no.nav.bidrag.dokument.bestilling.utils.createEnhetKontaktInformasjon
import no.nav.bidrag.dokument.bestilling.utils.createOpprettJournalpostResponse
import no.nav.bidrag.dokument.bestilling.utils.createPostAdresseResponse
import no.nav.bidrag.domene.enums.rolle.Rolletype
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import kotlin.jvm.JvmStatic

@Disabled
class DokumentBestillingBrevkodeTest : AbstractControllerTest() {
    @MockkSpyBean
    lateinit var dokumentProducer: BrevserverProducer

    private val ignoreBrevkoders =
        listOf("BI01A50", "BI01A01", "BI01A04")

    companion object {
        @JvmStatic
        fun alleDokumentmaler() = alleDokumentmaler

        @JvmStatic
        fun brevkoderUtgaaende() = alleDokumentmaler.filter { it.type != DokumentMalType.NOTAT }

        @JvmStatic
        fun brevkoderEnhetKontaktinfo() =
            brevkoderUtgaaende()
                .filter { it !is DokumentMalBucket }
                .filter { it.inneholderDatagrunnlag(DataGrunnlag.ENHET_KONTAKT_INFO) }

        @JvmStatic
        fun brevkoderVedtak() = brevkoderUtgaaende().filter { it.type == DokumentMalType.VEDTAK }
    }

    @ParameterizedTest(name = "{index} - Should add default values with sak, saksbehandler, mottaker and gjelder for brevkode {argumentsWithNames}")
    @MethodSource("alleDokumentmaler")
    fun `Should add default values with sak, saksbehandler, mottaker and gjelder`(dokumentMal: DokumentMal) {
        if (!dokumentMal.enabled || ignoreBrevkoders.contains(dokumentMal.kode) || dokumentMal is DokumentMalBucket) {
            print("brevkode ${dokumentMal.kode} ikke støttet, ignorerer testing")
            return
        }
        stubDefaultValues()
        val bmAdresse = createPostAdresseResponse()
        val tittel = "Tittel på dokument"
        val saksnummer = "123213"
        val mottakerId = BM1.ident.verdi
        val gjelderId = BP1.ident.verdi

        stubUtils.stubHentAdresse(postAdresse = bmAdresse)

        stubUtils.stubOpprettJournalpost(createOpprettJournalpostResponse(dokumentReferanse = "DOKREF_1"))

        val request =
            DokumentBestillingForespørsel(
                mottakerId = mottakerId,
                gjelderId = gjelderId,
                saksnummer = saksnummer,
                tittel = tittel,
                enhet = "4806",
                spraak = "NB",
            )

        val response =
            httpHeaderTestRestTemplate.exchange(
                "${rootUri()}/bestill/${dokumentMal.kode}",
                HttpMethod.POST,
                HttpEntity(request),
                DokumentBestillingResponse::class.java,
            )

        response.statusCode shouldBe HttpStatus.OK

        verify(exactly = 1) {
            dokumentProducer.produser(
                withArg { bestilling ->
                    assertSoftly {
                        bestilling.mottaker?.spraak shouldBe "NB"
                        bestilling.mottaker?.navn shouldBe BM1.visningsnavn
                        bestilling.mottaker?.fodselsnummer shouldBe BM1.ident.verdi
                        bestilling.mottaker?.rolle shouldBe Rolletype.BIDRAGSMOTTAKER
                        bestilling.mottaker?.fodselsdato shouldBe BM1.fødselsdato
                        bestilling.mottaker?.adresse shouldNotBe null

                        bestilling.gjelder?.fodselsnummer shouldBe gjelderId
                        bestilling.gjelder?.rolle shouldBe Rolletype.BIDRAGSPLIKTIG

                        bestilling.saksbehandler?.ident shouldBe SAKSBEHANDLER_IDENT
                        bestilling.saksbehandler?.navn shouldBe "Saksbehandler Mellomnavn Saksbehandlersen"

                        bestilling.spraak shouldBe "NB"
                        bestilling.saksnummer shouldBe saksnummer
                        bestilling.tittel shouldBe tittel
                        bestilling.enhet shouldBe "4806"
                        bestilling.rmISak shouldBe false
                    }
                },
                dokumentMal,
            )
        }
    }

    @ParameterizedTest(name = "{index} - Should add roller to utgaaende brev with brevkode {argumentsWithNames}")
    @MethodSource("brevkoderUtgaaende")
    @Disabled
    fun `Should add roller to utgaaende brev`(dokumentMal: DokumentMal) {
        if (!dokumentMal.enabled || ignoreBrevkoders.contains(dokumentMal.kode) || dokumentMal is DokumentMalBucket) {
            print("brevkode ${dokumentMal.kode} ikke støttet, ignorerer testing")
            return
        }
        stubDefaultValues()
        val bmAdresse = createPostAdresseResponse()
        val tittel = "Tittel på dokument"
        val saksnummer = "123213"
        val mottakerId = BM1.ident.verdi
        val gjelderId = BP1.ident.verdi

        stubUtils.stubHentAdresse(postAdresse = bmAdresse)
        stubUtils.stubHentVedtak()

        stubUtils.stubOpprettJournalpost(createOpprettJournalpostResponse(dokumentReferanse = "DOKREF_1"))

        val request =
            DokumentBestillingForespørsel(
                mottakerId = mottakerId,
                gjelderId = gjelderId,
                saksnummer = saksnummer,
                tittel = tittel,
                enhet = "4806",
                spraak = "NB",
                vedtakId = 1,
            )

        val response =
            httpHeaderTestRestTemplate.exchange(
                "${rootUri()}/bestill/${dokumentMal.kode}",
                HttpMethod.POST,
                HttpEntity(request),
                DokumentBestillingResponse::class.java,
            )

        response.statusCode shouldBe HttpStatus.OK

        verify(exactly = 1) {
            dokumentProducer.produser(
                withArg { bestilling ->
                    assertSoftly {
                        bestilling.roller shouldHaveSize 4
                        bestilling.roller.bidragsmottaker?.fodselsnummer shouldBe BM1.ident.verdi
                        bestilling.roller.bidragsmottaker?.navn shouldBe BM1.fornavnEtternavn()
                        bestilling.roller.bidragsmottaker?.fodselsdato shouldBe BM1.fødselsdato
                        bestilling.roller.bidragsmottaker?.landkode shouldBe "NO"
                        bestilling.roller.bidragsmottaker?.landkode3 shouldBe "NOR"

                        bestilling.roller.bidragspliktig?.fodselsnummer shouldBe BP1.ident.verdi
                        bestilling.roller.bidragspliktig?.navn shouldBe BP1.fornavnEtternavn()
                        bestilling.roller.bidragspliktig?.fodselsdato shouldBe BP1.fødselsdato
                        bestilling.roller.bidragspliktig?.landkode shouldBe "NO"
                        bestilling.roller.bidragspliktig?.landkode3 shouldBe "NOR"

                        bestilling.roller.barn shouldHaveSize 2
                        bestilling.roller.barn[0].fodselsnummer shouldBe BARN2.ident.verdi
                        bestilling.roller.barn[0].fodselsdato shouldBe BARN2.fødselsdato
                        bestilling.roller.barn[0].fornavn shouldBe BARN2.fornavn
                        bestilling.roller.barn[0].navn shouldBe BARN2.fornavnEtternavn()

                        bestilling.roller.barn[1].fodselsnummer shouldBe BARN1.ident.verdi
                        bestilling.roller.barn[1].fodselsdato shouldBe BARN1.fødselsdato
                        bestilling.roller.barn[1].fornavn shouldBe BARN1.fornavn
                        bestilling.roller.barn[1].navn shouldBe BARN1.fornavnEtternavn()
                    }
                },
                dokumentMal,
            )
        }
    }

    @ParameterizedTest(name = "{index} - Should add enhet kontaktinfo for brevkode {argumentsWithNames}")
    @MethodSource("brevkoderEnhetKontaktinfo")
    fun `Should add enhet kontaktinfo`(dokumentMal: DokumentMal) {
        if (!dokumentMal.enabled || ignoreBrevkoders.contains(dokumentMal.kode) || dokumentMal is DokumentMalBucket) {
            print("brevkode ${dokumentMal.kode} ikke støttet, ignorerer testing")
            return
        }
        stubDefaultValues()
        val bmAdresse = createPostAdresseResponse()
        val tittel = "Tittel på dokument"
        val saksnummer = "123213"
        val mottakerId = BM1.ident
        val gjelderId = BP1.ident
        val enhetKontaktInfo = createEnhetKontaktInformasjon()

        stubUtils.stubHentAdresse(postAdresse = bmAdresse)

        stubUtils.stubOpprettJournalpost(createOpprettJournalpostResponse(dokumentReferanse = "DOKREF_1"))

        val request =
            DokumentBestillingForespørsel(
                mottakerId = mottakerId.verdi,
                gjelderId = gjelderId.verdi,
                saksnummer = saksnummer,
                tittel = tittel,
                enhet = "4806",
                spraak = "NB",
            )
        val response =
            httpHeaderTestRestTemplate.exchange(
                "${rootUri()}/bestill/${dokumentMal.kode}",
                HttpMethod.POST,
                HttpEntity(request),
                DokumentBestillingResponse::class.java,
            )

        response.statusCode shouldBe HttpStatus.OK

        verify(exactly = 1) {
            dokumentProducer.produser(
                withArg { bestilling ->
                    assertSoftly {
                        bestilling.kontaktInfo?.navn shouldBe enhetKontaktInfo.enhetNavn
                        bestilling.kontaktInfo?.telefonnummer shouldBe enhetKontaktInfo.telefonnummer
                        bestilling.kontaktInfo?.enhetId shouldBe enhetKontaktInfo.enhetIdent
                        bestilling.kontaktInfo?.postadresse?.adresselinje1 shouldBe enhetKontaktInfo.postadresse?.adresselinje1
                        bestilling.kontaktInfo?.postadresse?.adresselinje2 shouldBe enhetKontaktInfo.postadresse?.adresselinje2
                        bestilling.kontaktInfo?.postadresse?.postnummer shouldBe enhetKontaktInfo.postadresse?.postnummer
                        bestilling.kontaktInfo?.postadresse?.poststed shouldBe enhetKontaktInfo.postadresse?.poststed
                        bestilling.kontaktInfo?.postadresse?.land shouldBe null
                    }
                },
                dokumentMal,
            )
        }
    }
}

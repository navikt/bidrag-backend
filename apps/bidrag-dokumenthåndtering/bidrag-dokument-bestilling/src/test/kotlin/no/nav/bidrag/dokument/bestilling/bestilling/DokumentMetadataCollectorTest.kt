package no.nav.bidrag.dokument.bestilling.bestilling

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.dokument.bestilling.api.dto.DokumentBestillingForespørsel
import no.nav.bidrag.dokument.bestilling.api.dto.MottakerAdresseTo
import no.nav.bidrag.dokument.bestilling.api.dto.MottakerTo
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMal
import no.nav.bidrag.dokument.bestilling.bestilling.dto.hentDokumentMal
import no.nav.bidrag.dokument.bestilling.config.SaksbehandlerInfoManager
import no.nav.bidrag.dokument.bestilling.consumer.BidragBehandlingConsumer
import no.nav.bidrag.dokument.bestilling.consumer.BidragVedtakConsumer
import no.nav.bidrag.dokument.bestilling.consumer.KodeverkConsumer
import no.nav.bidrag.dokument.bestilling.consumer.SjablonConsumer
import no.nav.bidrag.dokument.bestilling.consumer.dto.KodeverkResponse
import no.nav.bidrag.dokument.bestilling.consumer.dto.SjablongerDto
import no.nav.bidrag.dokument.bestilling.consumer.dto.fornavnEtternavn
import no.nav.bidrag.dokument.bestilling.model.Saksbehandler
import no.nav.bidrag.dokument.bestilling.model.typeRef
import no.nav.bidrag.dokument.bestilling.tjenester.BehandlingService
import no.nav.bidrag.dokument.bestilling.tjenester.KodeverkService
import no.nav.bidrag.dokument.bestilling.tjenester.OrganisasjonService
import no.nav.bidrag.dokument.bestilling.tjenester.PersonService
import no.nav.bidrag.dokument.bestilling.tjenester.ReskontroService
import no.nav.bidrag.dokument.bestilling.tjenester.SakService
import no.nav.bidrag.dokument.bestilling.tjenester.SjablongService
import no.nav.bidrag.dokument.bestilling.tjenester.VedtakService
import no.nav.bidrag.dokument.bestilling.utils.ANNEN_MOTTAKER
import no.nav.bidrag.dokument.bestilling.utils.BARN1
import no.nav.bidrag.dokument.bestilling.utils.BARN2
import no.nav.bidrag.dokument.bestilling.utils.BARN3
import no.nav.bidrag.dokument.bestilling.utils.BM1
import no.nav.bidrag.dokument.bestilling.utils.BP1
import no.nav.bidrag.dokument.bestilling.utils.DEFAULT_SAKSNUMMER
import no.nav.bidrag.dokument.bestilling.utils.DEFAULT_TITLE_DOKUMENT
import no.nav.bidrag.dokument.bestilling.utils.SAKSBEHANDLER_IDENT
import no.nav.bidrag.dokument.bestilling.utils.SAKSBEHANDLER_NAVN
import no.nav.bidrag.dokument.bestilling.utils.SAMHANDLER_IDENT
import no.nav.bidrag.dokument.bestilling.utils.SAMHANDLER_INFO
import no.nav.bidrag.dokument.bestilling.utils.SAMHANDLER_MOTTAKER_ADRESSE
import no.nav.bidrag.dokument.bestilling.utils.createEnhetKontaktInformasjon
import no.nav.bidrag.dokument.bestilling.utils.createPostAdresseResponse
import no.nav.bidrag.dokument.bestilling.utils.createSakResponse
import no.nav.bidrag.dokument.bestilling.utils.readFile
import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.land.Landkode2
import no.nav.bidrag.domene.land.Landkode3
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.transport.dokumentmaler.DokumentBestilling
import no.nav.bidrag.transport.sak.RolleDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.client.HttpStatusCodeException
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class DokumentMetadataCollectorTest {
    @MockK
    lateinit var personService: PersonService

    @MockK
    lateinit var sakService: SakService

    @MockK
    lateinit var kodeverkConsumer: KodeverkConsumer

    @MockK
    lateinit var vedtakConsumer: BidragVedtakConsumer

    @MockK
    lateinit var bidragBehandlingConsumer: BidragBehandlingConsumer

    @MockK
    lateinit var sjablonConsumer: SjablonConsumer

    @MockK
    lateinit var saksbehandlerInfoManager: SaksbehandlerInfoManager

    @MockK
    lateinit var organisasjonService: OrganisasjonService

    @MockK
    lateinit var reskontroService: ReskontroService

    @InjectMockKs
    lateinit var kodeverkService: KodeverkService

    @InjectMockKs
    lateinit var sjablongService: SjablongService

    @InjectMockKs
    lateinit var vedtakService: VedtakService

    @MockK
    lateinit var behandlingService: BehandlingService

    lateinit var metadataCollector: DokumentMetadataCollector

    @BeforeEach
    fun initMocks() {
        val kodeverkResponse =
            ObjectMapper()
                .findAndRegisterModules()
                .readValue(readFile("api/landkoder.json"), KodeverkResponse::class.java)
        val kodeverkISO2Response =
            ObjectMapper()
                .findAndRegisterModules()
                .readValue(readFile("api/landkoderISO2.json"), KodeverkResponse::class.java)
        val sjablonResponse =
            ObjectMapper()
                .findAndRegisterModules()
                .readValue(readFile("api/sjablon_all.json"), typeRef<SjablongerDto>())
        every { kodeverkConsumer.hentLandkoder() } returns kodeverkResponse
        every { kodeverkConsumer.hentLandkoderISO2() } returns kodeverkISO2Response
        every { sjablonConsumer.hentSjablonger() } returns sjablonResponse
        metadataCollector = withMetadataCollector()
    }

    private fun withMetadataCollector() =
        DokumentMetadataCollector(
            personService,
            sakService,
            kodeverkService,
            vedtakService,
            behandlingService,
            sjablongService,
            saksbehandlerInfoManager,
            organisasjonService,
        )

    @AfterEach
    fun resetMocks() {
        clearAllMocks()
    }

    fun mockDefaultValues() {
        every { personService.hentPerson(BM1.ident.verdi, any()) } returns BM1
        every { personService.hentPerson(BP1.ident.verdi, any()) } returns BP1
        every { personService.hentPerson(BARN1.ident.verdi, any()) } returns BARN1
        every { personService.hentPerson(BARN2.ident.verdi, any()) } returns BARN2
        every { personService.hentSpråk(any()) } returns "NB"
        every { personService.hentPersonAdresse(any(), any()) } returns createPostAdresseResponse()

        every {
            organisasjonService.hentEnhetKontaktInfo(
                any(),
                any(),
            )
        } returns createEnhetKontaktInformasjon()
        every { sakService.hentSak(any()) } returns createSakResponse()
        every { saksbehandlerInfoManager.hentSaksbehandler() } returns
            Saksbehandler(
                SAKSBEHANDLER_IDENT,
                SAKSBEHANDLER_NAVN,
            )
        every { saksbehandlerInfoManager.hentSaksbehandlerBrukerId() } returns SAKSBEHANDLER_IDENT
    }

    @Test
    fun `should map to bestilling request`() {
        mockDefaultValues()
        val adresseResponse = createPostAdresseResponse()
        val defaultKontaktinfo = createEnhetKontaktInformasjon()
        every { personService.hentPersonAdresse(any(), any()) } returns adresseResponse
        val request =
            DokumentBestillingForespørsel(
                mottakerId = BM1.ident.verdi,
                gjelderId = BM1.ident.verdi,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
            )
        val bestilling = mapToBestillingsdata(request)
        assertSoftly {
            bestilling.mottaker?.spraak shouldBe "NB"
            bestilling.mottaker?.navn shouldBe BM1.visningsnavn
            bestilling.mottaker?.fodselsnummer shouldBe BM1.ident.verdi
            bestilling.mottaker?.rolle shouldBe Rolletype.BIDRAGSMOTTAKER
            bestilling.mottaker?.fodselsdato shouldBe BM1.fødselsdato
            bestilling.mottaker?.adresse?.adresselinje1 shouldBe adresseResponse.adresselinje1
            bestilling.mottaker?.adresse?.adresselinje2 shouldBe adresseResponse.adresselinje2
            bestilling.mottaker?.adresse?.adresselinje3 shouldBe "3030 Drammen"
            bestilling.mottaker?.adresse?.adresselinje4 shouldBe null
            bestilling.mottaker?.adresse?.bruksenhetsnummer shouldBe adresseResponse.bruksenhetsnummer
            bestilling.mottaker?.adresse?.postnummer shouldBe adresseResponse.postnummer
            bestilling.mottaker?.adresse?.poststed shouldBe adresseResponse.poststed
            bestilling.mottaker?.adresse?.landkode shouldBe adresseResponse.land.verdi
            bestilling.mottaker?.adresse?.landkode3 shouldBe adresseResponse.land3.verdi
            bestilling.mottaker?.adresse?.land shouldBe "NORGE"

            bestilling.gjelder?.fodselsnummer shouldBe BM1.ident.verdi
            bestilling.gjelder?.rolle shouldBe Rolletype.BIDRAGSMOTTAKER

            bestilling.kontaktInfo?.navn shouldBe defaultKontaktinfo.enhetNavn
            bestilling.kontaktInfo?.telefonnummer shouldBe defaultKontaktinfo.telefonnummer
            bestilling.kontaktInfo?.enhetId shouldBe defaultKontaktinfo.enhetIdent
            bestilling.kontaktInfo?.postadresse?.adresselinje1 shouldBe defaultKontaktinfo.postadresse?.adresselinje1
            bestilling.kontaktInfo?.postadresse?.adresselinje2 shouldBe defaultKontaktinfo.postadresse?.adresselinje2
            bestilling.kontaktInfo?.postadresse?.postnummer shouldBe defaultKontaktinfo.postadresse?.postnummer
            bestilling.kontaktInfo?.postadresse?.poststed shouldBe defaultKontaktinfo.postadresse?.poststed
            bestilling.kontaktInfo?.postadresse?.land shouldBe null

            bestilling.saksbehandler?.ident shouldBe SAKSBEHANDLER_IDENT
            bestilling.saksbehandler?.navn shouldBe "Saksbehandler Mellomnavn Saksbehandlersen"

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

            bestilling.spraak shouldBe "NB"
            bestilling.saksnummer shouldBe DEFAULT_SAKSNUMMER
            bestilling.tittel shouldBe DEFAULT_TITLE_DOKUMENT
            bestilling.enhet shouldBe "4806"
            bestilling.rmISak shouldBe false
        }
    }

    @Test
    fun `should map when person missing adresse`() {
        mockDefaultValues()
        every { personService.hentPersonAdresse(any(), any()) } returns null
        val request =
            DokumentBestillingForespørsel(
                mottakerId = BM1.ident.verdi,
                gjelderId = BM1.ident.verdi,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
            )
        val bestilling = mapToBestillingsdata(request)
        assertSoftly {
            bestilling.mottaker?.adresse shouldBe null
            bestilling.roller.bidragspliktig?.landkode3 shouldBe null
            bestilling.roller.bidragsmottaker?.landkode3 shouldBe null
        }
    }

    @Test
    fun `should add saksbehandler from request when available`() {
        mockDefaultValues()
        val saksbehandlerId = "Z123213"
        every { saksbehandlerInfoManager.hentSaksbehandler(any()) } returns
            Saksbehandler(
                saksbehandlerId,
                "Navn saksbehandler",
            )

        val request =
            DokumentBestillingForespørsel(
                mottakerId = BM1.ident.verdi,
                gjelderId = BM1.ident.verdi,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
                saksbehandler = Saksbehandler(saksbehandlerId),
            )
        val bestilling = mapToBestillingsdata(request)
        assertSoftly {
            bestilling.saksbehandler?.ident shouldBe saksbehandlerId
            bestilling.saksbehandler?.navn shouldBe "Navn saksbehandler"
        }
    }

    @Test
    fun `should add saksbehandler ident and name from request when available`() {
        mockDefaultValues()
        val saksbehandlerId = "Z123213"
        val request =
            DokumentBestillingForespørsel(
                mottakerId = BM1.ident.verdi,
                gjelderId = BM1.ident.verdi,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
                saksbehandler = Saksbehandler(saksbehandlerId, "Navn fra request"),
            )
        val bestilling = mapToBestillingsdata(request)
        assertSoftly {
            bestilling.saksbehandler?.ident shouldBe saksbehandlerId
            bestilling.saksbehandler?.navn shouldBe "Navn fra request"
        }
    }

    @Test
    fun `should map mottaker adresse with bruksenhetsnummer standard value`() {
        mockDefaultValues()
        val adresseResponse =
            createPostAdresseResponse()
                .copy(bruksenhetsnummer = "H0101")
        every { personService.hentPersonAdresse(any(), any()) } returns adresseResponse
        val request =
            DokumentBestillingForespørsel(
                mottakerId = BM1.ident.verdi,
                gjelderId = BM1.ident.verdi,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
            )
        val bestilling = mapToBestillingsdata(request)
        assertSoftly {
            bestilling.mottaker?.adresse?.adresselinje1 shouldBe adresseResponse.adresselinje1
            bestilling.mottaker?.adresse?.adresselinje2 shouldBe adresseResponse.adresselinje2
            bestilling.mottaker?.adresse?.adresselinje3 shouldBe "3030 Drammen"
            bestilling.mottaker?.adresse?.adresselinje4 shouldBe null
            bestilling.mottaker?.adresse?.bruksenhetsnummer shouldBe null
            bestilling.mottaker?.adresse?.postnummer shouldBe adresseResponse.postnummer
            bestilling.mottaker?.adresse?.poststed shouldBe adresseResponse.poststed
            bestilling.mottaker?.adresse?.landkode shouldBe adresseResponse.land.verdi
            bestilling.mottaker?.adresse?.landkode3 shouldBe adresseResponse.land3.verdi
            bestilling.mottaker?.adresse?.land shouldBe "NORGE"
        }
    }

    @Test
    fun `should map mottaker add country name to adresselinje 4 when land not Norway`() {
        mockDefaultValues()
        val adresseResponse =
            createPostAdresseResponse()
                .copy(bruksenhetsnummer = null, land = Landkode2("TR"), land3 = Landkode3("TUR"))
        every { personService.hentPersonAdresse(any(), any()) } returns adresseResponse
        val request =
            DokumentBestillingForespørsel(
                mottakerId = BM1.ident.verdi,
                gjelderId = BM1.ident.verdi,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
            )
        val bestilling = mapToBestillingsdata(request)
        assertSoftly {
            bestilling.mottaker?.adresse?.adresselinje1 shouldBe adresseResponse.adresselinje1
            bestilling.mottaker?.adresse?.adresselinje2 shouldBe adresseResponse.adresselinje2
            bestilling.mottaker?.adresse?.adresselinje3 shouldBe "3030 Drammen"
            bestilling.mottaker?.adresse?.adresselinje4 shouldBe "TYRKIA"
            bestilling.mottaker?.adresse?.bruksenhetsnummer shouldBe null
            bestilling.mottaker?.adresse?.postnummer shouldBe adresseResponse.postnummer
            bestilling.mottaker?.adresse?.poststed shouldBe adresseResponse.poststed
            bestilling.mottaker?.adresse?.landkode shouldBe adresseResponse.land.verdi
            bestilling.mottaker?.adresse?.landkode3 shouldBe adresseResponse.land3.verdi
            bestilling.mottaker?.adresse?.land shouldBe "TYRKIA"
        }
    }

    @Test
    fun `should map mottaker with kortnavn when availabe`() {
        mockDefaultValues()
        val mottaker = BM1.copy(visningsnavn = "Etternavn, Kortnavn")
        every { personService.hentPerson(mottaker.ident.verdi, any()) } returns mottaker
        val request =
            DokumentBestillingForespørsel(
                mottakerId = mottaker.ident.verdi,
                gjelderId = BM1.ident.verdi,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
            )
        val bestilling = mapToBestillingsdata(request)
        bestilling.mottaker?.navn shouldBe mottaker.visningsnavn
    }

    @Test
    fun `should map with mottaker kontaktinformasjon`() {
        mockDefaultValues()

        val request =
            DokumentBestillingForespørsel(
                mottaker =
                    MottakerTo(
                        ident = SAMHANDLER_IDENT,
                        navn = "Samhandler samhandlersen",
                        språk = "NB",
                        adresse = SAMHANDLER_MOTTAKER_ADRESSE,
                    ),
                gjelderId = BM1.ident.verdi,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
            )
        val bestilling = mapToBestillingsdata(request)
        val adresseResponse = SAMHANDLER_MOTTAKER_ADRESSE
        assertSoftly {
            bestilling.mottaker?.spraak shouldBe "NB"
            bestilling.mottaker?.navn shouldBe SAMHANDLER_INFO.navn
            bestilling.mottaker?.fodselsnummer shouldBe SAMHANDLER_IDENT
            bestilling.mottaker?.rolle shouldBe null
            bestilling.mottaker?.fodselsdato shouldBe null
            bestilling.mottaker?.adresse?.adresselinje1 shouldBe adresseResponse.adresselinje1
            bestilling.mottaker?.adresse?.adresselinje2 shouldBe adresseResponse.adresselinje2
            bestilling.mottaker?.adresse?.adresselinje3 shouldBe "3000 Samhandler adresselinje 3"
            bestilling.mottaker?.adresse?.adresselinje4 shouldBe null
            bestilling.mottaker?.adresse?.bruksenhetsnummer shouldBe null
            bestilling.mottaker?.adresse?.postnummer shouldBe adresseResponse.postnummer
            bestilling.mottaker?.adresse?.poststed shouldBe null
            bestilling.mottaker?.adresse?.landkode shouldBe "NO"
            bestilling.mottaker?.adresse?.landkode3 shouldBe adresseResponse.landkode3
            bestilling.mottaker?.adresse?.land shouldBe "NORGE"
        }
    }

    @Test
    fun `should map with samhandler mottaker`() {
        mockDefaultValues()

        val request =
            DokumentBestillingForespørsel(
                mottakerId = SAMHANDLER_IDENT,
                gjelderId = BM1.ident.verdi,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
                samhandlerInformasjon = SAMHANDLER_INFO,
            )
        val bestilling = mapToBestillingsdata(request)
        val adresseResponse = SAMHANDLER_INFO.adresse
        assertSoftly {
            bestilling.mottaker?.spraak shouldBe "NB"
            bestilling.mottaker?.navn shouldBe SAMHANDLER_INFO.navn
            bestilling.mottaker?.fodselsnummer shouldBe SAMHANDLER_IDENT
            bestilling.mottaker?.rolle shouldBe null
            bestilling.mottaker?.fodselsdato shouldBe null
            bestilling.mottaker?.adresse?.adresselinje1 shouldBe adresseResponse?.adresselinje1
            bestilling.mottaker?.adresse?.adresselinje2 shouldBe adresseResponse?.adresselinje2
            bestilling.mottaker?.adresse?.adresselinje3 shouldBe "3000 Samhandler adresselinje 3"
            bestilling.mottaker?.adresse?.adresselinje4 shouldBe null
            bestilling.mottaker?.adresse?.bruksenhetsnummer shouldBe null
            bestilling.mottaker?.adresse?.postnummer shouldBe adresseResponse?.postnummer
            bestilling.mottaker?.adresse?.poststed shouldBe null
            bestilling.mottaker?.adresse?.landkode shouldBe adresseResponse?.landkode
            bestilling.mottaker?.adresse?.landkode3 shouldBe adresseResponse?.landkode
            bestilling.mottaker?.adresse?.land shouldBe "NORGE"
        }
    }

    @Test
    fun `should map with samhandler mottaker having utenlandsk adresse and long adresselinje3`() {
        mockDefaultValues()

        val samhandlerInfo =
            SAMHANDLER_INFO.copy(
                adresse =
                    SAMHANDLER_INFO.adresse?.copy(
                        adresselinje3 = "En veldig lang adresselinje3 som er litt lenger",
                        landkode = "TUR",
                    ),
            )
        val request =
            DokumentBestillingForespørsel(
                mottakerId = SAMHANDLER_IDENT,
                gjelderId = BM1.ident.verdi,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
                samhandlerInformasjon = samhandlerInfo,
            )
        val bestilling = mapToBestillingsdata(request)
        val adresseResponse = samhandlerInfo.adresse
        assertSoftly {
            bestilling.mottaker?.spraak shouldBe "NB"
            bestilling.mottaker?.navn shouldBe SAMHANDLER_INFO.navn
            bestilling.mottaker?.fodselsnummer shouldBe SAMHANDLER_IDENT
            bestilling.mottaker?.rolle shouldBe null
            bestilling.mottaker?.fodselsdato shouldBe null
            bestilling.mottaker?.adresse?.adresselinje1 shouldBe adresseResponse?.adresselinje1
            bestilling.mottaker?.adresse?.adresselinje2 shouldBe adresseResponse?.adresselinje2
            bestilling.mottaker?.adresse?.adresselinje3 shouldBe "3000 En veldig lang adresselin"
            bestilling.mottaker?.adresse?.adresselinje4 shouldBe null
            bestilling.mottaker?.adresse?.bruksenhetsnummer shouldBe null
            bestilling.mottaker?.adresse?.postnummer shouldBe adresseResponse?.postnummer
            bestilling.mottaker?.adresse?.poststed shouldBe null
            bestilling.mottaker?.adresse?.landkode shouldBe adresseResponse?.landkode
            bestilling.mottaker?.adresse?.landkode3 shouldBe adresseResponse?.landkode
            bestilling.mottaker?.adresse?.land shouldBe "TYRKIA"
        }
    }

    @Test
    fun `should map with mottakerid not in roller`() {
        mockDefaultValues()
        every { personService.hentPerson(ANNEN_MOTTAKER.ident.verdi, any()) } returns ANNEN_MOTTAKER

        val request =
            DokumentBestillingForespørsel(
                mottakerId = ANNEN_MOTTAKER.ident.verdi,
                gjelderId = BM1.ident.verdi,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
            )
        val bestilling = mapToBestillingsdata(request)
        assertSoftly {
            bestilling.mottaker?.spraak shouldBe "NB"
            bestilling.mottaker?.navn shouldBe ANNEN_MOTTAKER.navn
            bestilling.mottaker?.fodselsnummer shouldBe ANNEN_MOTTAKER.ident.verdi
            bestilling.mottaker?.rolle shouldBe null
            bestilling.mottaker?.fodselsdato shouldBe ANNEN_MOTTAKER.fødselsdato
        }
    }

    @Test
    fun `should pick gjelder from roller when gjelderId is null`() {
        mockDefaultValues()
        every { personService.hentPerson(ANNEN_MOTTAKER.ident.verdi, any()) } returns ANNEN_MOTTAKER

        val request =
            DokumentBestillingForespørsel(
                mottakerId = ANNEN_MOTTAKER.ident.verdi,
                gjelderId = null,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
            )
        val bestilling = mapToBestillingsdata(request)
        assertSoftly {
            bestilling.gjelder?.fodselsnummer shouldBe BM1.ident.verdi
            bestilling.gjelder?.rolle shouldBe Rolletype.BIDRAGSMOTTAKER
        }
    }

    @Test
    fun `should pick gjelder from roller when gjelderId is not in roller`() {
        mockDefaultValues()
        every { personService.hentPerson(ANNEN_MOTTAKER.ident.verdi, any()) } returns ANNEN_MOTTAKER

        val request =
            DokumentBestillingForespørsel(
                mottakerId = ANNEN_MOTTAKER.ident.verdi,
                gjelderId = "3123213213",
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
            )
        val bestilling = mapToBestillingsdata(request)
        assertSoftly {
            bestilling.gjelder?.fodselsnummer shouldBe BM1.ident.verdi
            bestilling.gjelder?.rolle shouldBe Rolletype.BIDRAGSMOTTAKER
        }
    }

    @Test
    fun `should pick gjelder as BP from roller when gjelderId is not in roller and BM not exists`() {
        mockDefaultValues()
        val saksnummer = "22222"
        val barn1Dod = BARN1.copy(dødsdato = LocalDate.of(2022, 1, 1))
        val sak =
            createSakResponse().copy(
                roller =
                    listOf(
                        RolleDto(
                            fødselsnummer = BP1.ident,
                            type = Rolletype.BIDRAGSPLIKTIG,
                        ),
                        RolleDto(
                            fødselsnummer = barn1Dod.ident,
                            type = Rolletype.BARN,
                        ),
                        RolleDto(
                            fødselsnummer = BARN2.ident,
                            type = Rolletype.BARN,
                        ),
                    ),
            )
        every { sakService.hentSak(saksnummer) } returns sak
        every { personService.hentPerson(barn1Dod.ident.verdi, any()) } returns barn1Dod

        val request =
            DokumentBestillingForespørsel(
                mottakerId = BM1.ident.verdi,
                gjelderId = "213213213213213",
                saksnummer = saksnummer,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
                samhandlerInformasjon = null,
            )
        val bestilling = mapToBestillingsdata(request)
        assertSoftly {
            bestilling.gjelder?.fodselsnummer shouldBe BP1.ident.verdi
            bestilling.gjelder?.rolle shouldBe Rolletype.BIDRAGSPLIKTIG
        }
    }

    @Test
    fun `should use enhet from sak when request is missing enhet`() {
        mockDefaultValues()
        val sakresponse = createSakResponse().copy(eierfogd = Enhetsnummer("4888"))
        every { sakService.hentSak(any()) } returns sakresponse
        val request =
            DokumentBestillingForespørsel(
                mottakerId = BM1.ident.verdi,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = null,
                spraak = "NB",
                samhandlerInformasjon = null,
            )
        val bestilling = mapToBestillingsdata(request)
        bestilling.enhet shouldBe "4888"
    }

    @Test
    fun `should not throw when mottaker is samhandler but is missing samhandlerInformasjon`() {
        mockDefaultValues()

        val request =
            DokumentBestillingForespørsel(
                gjelderId = BM1.ident.verdi,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
                samhandlerInformasjon = null,
                mottaker =
                    MottakerTo(
                        ident = SAMHANDLER_IDENT,
                        navn = SAKSBEHANDLER_NAVN,
                        adresse = SAMHANDLER_MOTTAKER_ADRESSE,
                    ),
            )
        val bestilling = mapToBestillingsdata(request)
        assertSoftly {
            bestilling.mottaker?.spraak shouldBe "NB"
            bestilling.mottaker?.navn shouldBe SAKSBEHANDLER_NAVN
            bestilling.mottaker?.fodselsnummer shouldBe SAMHANDLER_IDENT
            bestilling.mottaker?.rolle shouldBe null
            bestilling.mottaker?.adresse!!.adresselinje1 shouldBe SAMHANDLER_MOTTAKER_ADRESSE.adresselinje1
            bestilling.mottaker?.adresse!!.adresselinje2 shouldBe SAMHANDLER_MOTTAKER_ADRESSE.adresselinje2
            bestilling.mottaker?.adresse?.adresselinje3 shouldBe "3000 Samhandler adresselinje 3"
            bestilling.mottaker?.adresse!!.postnummer shouldBe SAMHANDLER_MOTTAKER_ADRESSE.postnummer
            bestilling.mottaker?.adresse!!.land shouldBe "NORGE"
        }
    }

    @Test
    fun `should not throw when mottaker is samhandler but is missing adresse`() {
        mockDefaultValues()

        val request =
            DokumentBestillingForespørsel(
                gjelderId = BM1.ident.verdi,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
                samhandlerInformasjon = null,
                mottaker =
                    MottakerTo(
                        ident = SAMHANDLER_IDENT,
                        navn = SAKSBEHANDLER_NAVN,
                    ),
            )
        val bestilling = mapToBestillingsdata(request)
        assertSoftly {
            bestilling.mottaker?.spraak shouldBe "NB"
            bestilling.mottaker?.navn shouldBe SAKSBEHANDLER_NAVN
            bestilling.mottaker?.fodselsnummer shouldBe SAMHANDLER_IDENT
            bestilling.mottaker?.rolle shouldBe null
            bestilling.mottaker?.adresse shouldBe null
        }
    }

    @Test
    fun `should throw when sak is not found`() {
        mockDefaultValues()
        val saksnummer = "111111"
        every { sakService.hentSak(saksnummer) } returns null

        val request =
            DokumentBestillingForespørsel(
                mottakerId = "123213213",
                saksnummer = saksnummer,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
                samhandlerInformasjon = null,
            )
        shouldThrowWithMessage<HttpStatusCodeException>("400 Fant ikke sak med id $saksnummer") {
            mapToBestillingsdata(
                request,
            )
        }
    }

    @Test
    fun `should map with roller having kode6 diskresjon`() {
        mockDefaultValues()
        val barn1Kode6 = BARN1.copy(diskresjonskode = Diskresjonskode.SPSF)
        val barn2Kode6 = BARN2.copy(diskresjonskode = Diskresjonskode.SPSF, fødselsdato = null)
        val bmKode6 = BM1.copy(diskresjonskode = Diskresjonskode.SPSF)
        val bpKode6 = BP1.copy(diskresjonskode = Diskresjonskode.SPSF)
        every { personService.hentPerson(BM1.ident.verdi, any()) } returns bmKode6
        every { personService.hentPerson(BP1.ident.verdi, any()) } returns bpKode6
        every { personService.hentPerson(BARN1.ident.verdi, any()) } returns barn1Kode6
        every { personService.hentPerson(BARN2.ident.verdi, any()) } returns barn2Kode6

        val request =
            DokumentBestillingForespørsel(
                mottakerId = BM1.ident.verdi,
                gjelderId = BM1.ident.verdi,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
            )
        val bestilling = mapToBestillingsdata(request)
        val adresseResponse = createPostAdresseResponse()
        assertSoftly {
            bestilling.mottaker?.spraak shouldBe "NB"
            bestilling.mottaker?.navn shouldBe bmKode6.visningsnavn
            bestilling.mottaker?.fodselsnummer shouldBe bmKode6.ident.verdi
            bestilling.mottaker?.rolle shouldBe Rolletype.BIDRAGSMOTTAKER
            bestilling.mottaker?.fodselsdato shouldBe BM1.fødselsdato
            bestilling.mottaker?.adresse?.adresselinje1 shouldBe adresseResponse.adresselinje1
            bestilling.mottaker?.adresse?.adresselinje2 shouldBe adresseResponse.adresselinje2
            bestilling.mottaker?.adresse?.adresselinje3 shouldBe "3030 Drammen"
            bestilling.mottaker?.adresse?.adresselinje4 shouldBe null
            bestilling.mottaker?.adresse?.bruksenhetsnummer shouldBe adresseResponse.bruksenhetsnummer
            bestilling.mottaker?.adresse?.postnummer shouldBe adresseResponse.postnummer
            bestilling.mottaker?.adresse?.poststed shouldBe adresseResponse.poststed
            bestilling.mottaker?.adresse?.landkode shouldBe adresseResponse.land.verdi
            bestilling.mottaker?.adresse?.landkode3 shouldBe adresseResponse.land3.verdi
            bestilling.mottaker?.adresse?.land shouldBe "NORGE"

            bestilling.roller shouldHaveSize 4
            bestilling.roller.bidragsmottaker?.fodselsnummer shouldBe bmKode6.ident.verdi
            bestilling.roller.bidragsmottaker?.navn shouldBe ""
            bestilling.roller.bidragsmottaker?.fodselsdato shouldBe null
            bestilling.roller.bidragsmottaker?.landkode shouldBe "NO"
            bestilling.roller.bidragsmottaker?.landkode3 shouldBe "NOR"

            bestilling.roller.bidragspliktig?.fodselsnummer shouldBe bpKode6.ident.verdi
            bestilling.roller.bidragspliktig?.navn shouldBe ""
            bestilling.roller.bidragspliktig?.fodselsdato shouldBe null
            bestilling.roller.bidragspliktig?.landkode shouldBe "NO"
            bestilling.roller.bidragspliktig?.landkode3 shouldBe "NOR"

            bestilling.roller.barn shouldHaveSize 2
            bestilling.roller.barn[0].fodselsnummer shouldBe barn1Kode6.ident.verdi
            bestilling.roller.barn[0].fodselsdato shouldBe null
            bestilling.roller.barn[0].fornavn shouldBe "(BARN FØDT I ${barn1Kode6.fødselsdato?.year})"
            bestilling.roller.barn[0].navn shouldBe "(BARN FØDT I ${barn1Kode6.fødselsdato?.year})"

            bestilling.roller.barn[1].fodselsnummer shouldBe barn2Kode6.ident.verdi
            bestilling.roller.barn[1].fodselsdato shouldBe null
            bestilling.roller.barn[1].fornavn shouldBe "(BARN)"
            bestilling.roller.barn[1].navn shouldBe "(BARN)"
        }
    }

    @Test
    fun `should map with roller having kode6 diskresjon with english language`() {
        mockDefaultValues()
        val barn1Kode6 = BARN1.copy(diskresjonskode = Diskresjonskode.SPSF)
        val barn2Kode6 = BARN2.copy(diskresjonskode = Diskresjonskode.SPSF, fødselsdato = null)
        val bmKode6 = BM1.copy(diskresjonskode = Diskresjonskode.SPSF)
        val bpKode6 = BP1.copy(diskresjonskode = Diskresjonskode.SPSF)
        every { personService.hentPerson(BM1.ident.verdi, any()) } returns bmKode6
        every { personService.hentPerson(BP1.ident.verdi, any()) } returns bpKode6
        every { personService.hentPerson(BARN1.ident.verdi, any()) } returns barn1Kode6
        every { personService.hentPerson(BARN2.ident.verdi, any()) } returns barn2Kode6

        val request =
            DokumentBestillingForespørsel(
                mottakerId = BM1.ident.verdi,
                gjelderId = BM1.ident.verdi,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "EN",
            )
        val bestilling = mapToBestillingsdata(request)
        assertSoftly {
            bestilling.roller.barn[0].fornavn shouldBe "(CHILD BORN IN ${barn1Kode6.fødselsdato?.year})"
            bestilling.roller.barn[0].navn shouldBe "(CHILD BORN IN ${barn1Kode6.fødselsdato?.year})"

            bestilling.roller.barn[1].fornavn shouldBe "(CHILD)"
            bestilling.roller.barn[1].navn shouldBe "(CHILD)"
        }
    }

    @Test
    fun `should map with roller having kode6 diskresjon with nynorsk language`() {
        mockDefaultValues()
        val barn1Kode6 = BARN1.copy(diskresjonskode = Diskresjonskode.SPSF)
        val barn2Kode6 = BARN2.copy(diskresjonskode = Diskresjonskode.SPSF, fødselsdato = null)
        val bmKode6 = BM1.copy(diskresjonskode = Diskresjonskode.SPSF)
        val bpKode6 = BP1.copy(diskresjonskode = Diskresjonskode.SPSF)
        every { personService.hentPerson(BM1.ident.verdi, any()) } returns bmKode6
        every { personService.hentPerson(BP1.ident.verdi, any()) } returns bpKode6
        every { personService.hentPerson(BARN1.ident.verdi, any()) } returns barn1Kode6
        every { personService.hentPerson(BARN2.ident.verdi, any()) } returns barn2Kode6

        val request =
            DokumentBestillingForespørsel(
                mottakerId = BM1.ident.verdi,
                gjelderId = BM1.ident.verdi,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NN",
            )
        val bestilling = mapToBestillingsdata(request)
        assertSoftly {
            bestilling.roller.barn[0].fornavn shouldBe "(BARN FØDD I ${barn1Kode6.fødselsdato?.year})"
            bestilling.roller.barn[0].navn shouldBe "(BARN FØDD I ${barn1Kode6.fødselsdato?.year})"

            bestilling.roller.barn[1].fornavn shouldBe "(BARN)"
            bestilling.roller.barn[1].navn shouldBe "(BARN)"
        }
    }

    @Nested
    inner class MapRoller {
        @Test
        fun `should set rmISak when sak has rolle RM`() {
            mockDefaultValues()
            val rmIdent = "13123123"
            val saksnummer = "22222"
            val originalSakResponse = createSakResponse()
            val roller = originalSakResponse.roller.toMutableList()
            roller.add(
                RolleDto(
                    fødselsnummer = Personident(rmIdent),
                    type = Rolletype.REELMOTTAKER,
                ),
            )
            val sak =
                originalSakResponse.copy(
                    roller = roller,
                )
            every { sakService.hentSak(saksnummer) } returns sak

            val request =
                DokumentBestillingForespørsel(
                    mottakerId = BM1.ident.verdi,
                    saksnummer = saksnummer,
                    tittel = DEFAULT_TITLE_DOKUMENT,
                    enhet = "4806",
                    spraak = "NB",
                    samhandlerInformasjon = null,
                )
            val bestilling = mapToBestillingsdata(request)
            bestilling.rmISak shouldBe true
        }

        @Test
        fun `should map when bidragsmottaker is missing in sak`() {
            mockDefaultValues()
            val saksnummer = "22222"
            val sak =
                createSakResponse().copy(
                    roller =
                        listOf(
                            RolleDto(
                                fødselsnummer = BP1.ident,
                                type = Rolletype.BIDRAGSPLIKTIG,
                            ),
                            RolleDto(
                                fødselsnummer = BARN1.ident,
                                type = Rolletype.BARN,
                            ),
                            RolleDto(
                                fødselsnummer = BARN2.ident,
                                type = Rolletype.BARN,
                            ),
                        ),
                )
            every { sakService.hentSak(saksnummer) } returns sak

            val request =
                DokumentBestillingForespørsel(
                    mottakerId = BM1.ident.verdi,
                    saksnummer = saksnummer,
                    tittel = DEFAULT_TITLE_DOKUMENT,
                    enhet = "4806",
                    spraak = "NB",
                    samhandlerInformasjon = null,
                )
            val bestilling = mapToBestillingsdata(request)
            bestilling.roller.bidragsmottaker shouldBe null
            bestilling.roller.bidragspliktig shouldNotBe null
            bestilling.roller.barn shouldHaveSize 2
        }

        @Test
        fun `should map when bidragsplitkig is missing in sak`() {
            mockDefaultValues()
            val saksnummer = "22222"
            val sak =
                createSakResponse().copy(
                    roller =
                        listOf(
                            RolleDto(
                                fødselsnummer = BM1.ident,
                                type = Rolletype.BIDRAGSMOTTAKER,
                            ),
                            RolleDto(
                                fødselsnummer = BARN1.ident,
                                type = Rolletype.BARN,
                            ),
                            RolleDto(
                                fødselsnummer = BARN2.ident,
                                type = Rolletype.BARN,
                            ),
                        ),
                )
            every { sakService.hentSak(saksnummer) } returns sak

            val request =
                DokumentBestillingForespørsel(
                    mottakerId = BM1.ident.verdi,
                    saksnummer = saksnummer,
                    tittel = DEFAULT_TITLE_DOKUMENT,
                    enhet = "4806",
                    spraak = "NB",
                    samhandlerInformasjon = null,
                )
            val bestilling = mapToBestillingsdata(request)
            bestilling.roller.bidragspliktig shouldBe null
            bestilling.roller.bidragsmottaker shouldNotBe null
            bestilling.roller.barn shouldHaveSize 2
        }

        @Test
        fun `should map when barn is missing in sak`() {
            mockDefaultValues()
            val saksnummer = "22222"
            val sak =
                createSakResponse().copy(
                    roller =
                        listOf(
                            RolleDto(
                                fødselsnummer = BM1.ident,
                                type = Rolletype.BIDRAGSMOTTAKER,
                            ),
                        ),
                )
            every { sakService.hentSak(saksnummer) } returns sak

            val request =
                DokumentBestillingForespørsel(
                    mottakerId = BM1.ident.verdi,
                    saksnummer = saksnummer,
                    tittel = DEFAULT_TITLE_DOKUMENT,
                    enhet = "4806",
                    spraak = "NB",
                    samhandlerInformasjon = null,
                )
            val bestilling = mapToBestillingsdata(request)
            bestilling.roller.bidragspliktig shouldBe null
            bestilling.roller.bidragsmottaker shouldNotBe null
            bestilling.roller.barn shouldHaveSize 0
        }

        @Test
        fun `should not add dead barn to roller`() {
            mockDefaultValues()
            val saksnummer = "22222"
            val barn1Dod = BARN1.copy(dødsdato = LocalDate.of(2022, 1, 1))
            val sak =
                createSakResponse().copy(
                    roller =
                        listOf(
                            RolleDto(
                                fødselsnummer = BM1.ident,
                                type = Rolletype.BIDRAGSMOTTAKER,
                            ),
                            RolleDto(
                                fødselsnummer = barn1Dod.ident,
                                type = Rolletype.BARN,
                            ),
                            RolleDto(
                                fødselsnummer = BARN2.ident,
                                type = Rolletype.BARN,
                            ),
                        ),
                )
            every { sakService.hentSak(saksnummer) } returns sak
            every { personService.hentPerson(barn1Dod.ident.verdi, any()) } returns barn1Dod

            val request =
                DokumentBestillingForespørsel(
                    mottakerId = BM1.ident.verdi,
                    saksnummer = saksnummer,
                    tittel = DEFAULT_TITLE_DOKUMENT,
                    enhet = "4806",
                    spraak = "NB",
                    samhandlerInformasjon = null,
                )
            val bestilling = mapToBestillingsdata(request)
            bestilling.roller.bidragspliktig shouldBe null
            bestilling.roller.bidragsmottaker shouldNotBe null
            bestilling.roller.barn shouldHaveSize 1
        }

        @Test
        fun `skal legge til alle barn`() {
            mockDefaultValues()
            val saksnummer = "22222"
            val sak =
                createSakResponse().copy(
                    roller =
                        listOf(
                            RolleDto(
                                fødselsnummer = BM1.ident,
                                type = Rolletype.BIDRAGSMOTTAKER,
                            ),
                            RolleDto(
                                fødselsnummer = BARN1.ident,
                                type = Rolletype.BARN,
                            ),
                            RolleDto(
                                fødselsnummer = BARN2.ident,
                                type = Rolletype.BARN,
                            ),
                        ),
                )
            every { sakService.hentSak(saksnummer) } returns sak

            val request =
                DokumentBestillingForespørsel(
                    mottakerId = BM1.ident.verdi,
                    saksnummer = saksnummer,
                    tittel = DEFAULT_TITLE_DOKUMENT,
                    enhet = "4806",
                    spraak = "NB",
                    samhandlerInformasjon = null,
                )
            val bestilling = mapToBestillingsdata(request)
            bestilling.roller.bidragspliktig shouldBe null
            bestilling.roller.bidragsmottaker shouldNotBe null
            bestilling.roller.barn shouldHaveSize 2
            bestilling.roller.barn[1].fodselsnummer shouldBe BARN1.ident.verdi
            bestilling.roller.barn[0].fodselsnummer shouldBe BARN2.ident.verdi
        }

        @Test
        fun `skal bare legge til barn hvis forespørsel inneholder barnIBehandling`() {
            mockDefaultValues()
            val saksnummer = "22222"
            val sak =
                createSakResponse().copy(
                    roller =
                        listOf(
                            RolleDto(
                                fødselsnummer = BM1.ident,
                                type = Rolletype.BIDRAGSMOTTAKER,
                            ),
                            RolleDto(
                                fødselsnummer = BARN1.ident,
                                type = Rolletype.BARN,
                            ),
                            RolleDto(
                                fødselsnummer = BARN2.ident,
                                type = Rolletype.BARN,
                            ),
                        ),
                )
            every { sakService.hentSak(saksnummer) } returns sak

            val request =
                DokumentBestillingForespørsel(
                    mottakerId = BM1.ident.verdi,
                    saksnummer = saksnummer,
                    tittel = DEFAULT_TITLE_DOKUMENT,
                    enhet = "4806",
                    spraak = "NB",
                    samhandlerInformasjon = null,
                    barnIBehandling = listOf(BARN1.ident.verdi),
                )
            val bestilling = mapToBestillingsdata(request)
            bestilling.roller.bidragspliktig shouldBe null
            bestilling.roller.bidragsmottaker shouldNotBe null
            bestilling.roller.barn shouldHaveSize 1
            bestilling.roller.barn[0].fodselsnummer shouldBe BARN1.ident.verdi
        }
    }

    @Test
    fun `should sort barn by born date`() {
        mockDefaultValues()
        val saksnummer = "22222"
        val barn1 = BARN1.copy(fødselsdato = LocalDate.of(2020, 1, 2))
        val barn2 = BARN2.copy(fødselsdato = LocalDate.of(2020, 5, 15))
        val barn3 = BARN3.copy(fødselsdato = LocalDate.of(2021, 7, 20))
        val barn4 =
            BARN3.copy(
                ident = Personident("1231231233333333"),
                fødselsdato = LocalDate.of(2022, 3, 15),
            )
        val sak =
            createSakResponse().copy(
                roller =
                    listOf(
                        RolleDto(
                            fødselsnummer = BM1.ident,
                            type = Rolletype.BIDRAGSMOTTAKER,
                        ),
                        RolleDto(
                            fødselsnummer = barn1.ident,
                            type = Rolletype.BARN,
                        ),
                        RolleDto(
                            fødselsnummer = barn2.ident,
                            type = Rolletype.BARN,
                        ),
                        RolleDto(
                            fødselsnummer = barn3.ident,
                            type = Rolletype.BARN,
                        ),
                        RolleDto(
                            fødselsnummer = barn4.ident,
                            type = Rolletype.BARN,
                        ),
                    ),
            )
        every { sakService.hentSak(saksnummer) } returns sak
        every { personService.hentPerson(barn1.ident.verdi, any()) } returns barn1
        every { personService.hentPerson(barn2.ident.verdi, any()) } returns barn2
        every { personService.hentPerson(barn3.ident.verdi, any()) } returns barn3
        every { personService.hentPerson(barn4.ident.verdi, any()) } returns barn4

        val request =
            DokumentBestillingForespørsel(
                mottakerId = BM1.ident.verdi,
                saksnummer = saksnummer,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
                samhandlerInformasjon = null,
            )
        val bestilling = mapToBestillingsdata(request)
        assertSoftly {
            bestilling.roller.bidragspliktig shouldBe null
            bestilling.roller.bidragsmottaker shouldNotBe null
            bestilling.roller.barn shouldHaveSize 4
            withClue("Første barn må være eldste barn født ${barn1.fødselsdato}") {
                bestilling.roller.barn[0].fodselsnummer shouldBe barn1.ident.verdi
            }
            withClue("Andre barn må være nest eldste barn født ${barn2.fødselsdato}") {
                bestilling.roller.barn[1].fodselsnummer shouldBe barn2.ident.verdi
            }
            withClue("Tredje barn må være barn født ${barn3.fødselsdato}") {
                bestilling.roller.barn[2].fodselsnummer shouldBe barn3.ident.verdi
            }
            withClue("Fjerde barn må være barn født ${barn4.fødselsdato}") {
                bestilling.roller.barn[3].fodselsnummer shouldBe barn4.ident.verdi
            }
        }
    }

    @Test
    fun `should map with only adresse and not mottakerid`() {
        mockDefaultValues()
        val request =
            DokumentBestillingForespørsel(
                gjelderId = BM1.ident.verdi,
                saksnummer = DEFAULT_SAKSNUMMER,
                tittel = DEFAULT_TITLE_DOKUMENT,
                enhet = "4806",
                spraak = "NB",
                mottaker =
                    MottakerTo(
                        navn = "Mottaker Mottakersen",
                        språk = "NB",
                        adresse =
                            MottakerAdresseTo(
                                adresselinje1 = "adresselinje 1",
                                adresselinje2 = "adresselinje 2",
                                adresselinje3 = "adresselinje 3",
                                postnummer = "3030",
                                poststed = "Drammen",
                                landkode = "NO",
                            ),
                    ),
            )
        val bestilling = mapToBestillingsdata(request)
        assertSoftly {
            bestilling.mottaker?.spraak shouldBe "NB"
            bestilling.mottaker?.navn shouldBe "Mottaker Mottakersen"
            bestilling.mottaker?.fodselsnummer shouldBe ""
            bestilling.mottaker?.rolle shouldBe null
            bestilling.mottaker?.fodselsdato shouldBe null
            bestilling.mottaker?.adresse?.adresselinje1 shouldBe "adresselinje 1"
            bestilling.mottaker?.adresse?.adresselinje2 shouldBe "adresselinje 2"
            bestilling.mottaker?.adresse?.adresselinje3 shouldBe "adresselinje 3"
            bestilling.mottaker?.adresse?.adresselinje4 shouldBe "3030 Drammen"
            bestilling.mottaker?.adresse?.bruksenhetsnummer shouldBe null
            bestilling.mottaker?.adresse?.postnummer shouldBe "3030"
            bestilling.mottaker?.adresse?.poststed shouldBe "Drammen"
            bestilling.mottaker?.adresse?.landkode shouldBe "NO"
            bestilling.mottaker?.adresse?.landkode3 shouldBe ""
            bestilling.mottaker?.adresse?.land shouldBe "NORGE"
        }
    }

    private fun mapToBestillingsdata(
        request: DokumentBestillingForespørsel,
        dokumentMal: DokumentMal = hentDokumentMal("BI01S02")!!,
    ): DokumentBestilling = metadataCollector.collect(request, dokumentMal)
}

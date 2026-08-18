package no.nav.bidrag.samhandler.service

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import jakarta.persistence.EntityManager
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.samhandler.OffentligIdType
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.domene.land.Landkode3
import no.nav.bidrag.samhandler.mapper.SamhandlerMapper
import no.nav.bidrag.samhandler.persistence.entity.Samhandler
import no.nav.bidrag.samhandler.persistence.repository.SamhandlerRepository
import no.nav.bidrag.samhandler.util.KontonummerUtils
import no.nav.bidrag.transport.samhandler.AdresseDto
import no.nav.bidrag.transport.samhandler.KontonummerDto
import no.nav.bidrag.transport.samhandler.SamhandlerDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpStatusCodeException
import java.util.Optional

@ExtendWith(MockKExtension::class)
class SamhandlerServiceTest {
    @RelaxedMockK
    private lateinit var samhandlerRepository: SamhandlerRepository

    @RelaxedMockK
    private lateinit var kafkaService: KafkaService

    @RelaxedMockK
    private lateinit var entityManager: EntityManager

    @InjectMockKs
    private lateinit var samhandlerService: SamhandlerService

    private lateinit var ident: Ident
    private lateinit var samhandler: Samhandler
    private lateinit var samhandlerDto: SamhandlerDto

    @BeforeEach
    fun setUp() {
        ident = Ident("12345678901")
        samhandler =
            Samhandler(
                id = 0,
                ident = "12345678901",
                offentligId = "12345678901",
                offentligIdType = OffentligIdType.ORG,
                navn = "Test Samhandler",
            )
        samhandlerDto = SamhandlerMapper.mapTilSamhandlerDto(samhandler)
        mockkObject(KontonummerUtils)
    }

    @Test
    fun `hentSamhandler skal returnere SamhandlerDto når samhandler finnes`() {
        every { samhandlerRepository.findByIdent(ident.verdi) } returns samhandler
        val result = samhandlerService.hentSamhandler(ident, true)
        result shouldNotBe null
        verify { samhandlerRepository.findByIdent(ident.verdi) }
    }

    @Test
    fun `opprettSamhandler skal lagre og returnere id`() {
        every { samhandlerRepository.save(any()) } returns samhandler
        val result = samhandlerService.opprettSamhandler(samhandlerDto)
        result shouldBe samhandler.id
        verify { samhandlerRepository.save(any()) }
    }

    @Test
    fun `hentSamhandlerPåId skal returnere SamhandlerDto når samhandler finnes`() {
        every { samhandlerRepository.findById(samhandler.id!!) } returns Optional.of(samhandler)
        val result = samhandlerService.hentSamhandlerPåId(samhandler.id!!)
        result shouldNotBe null
        verify { samhandlerRepository.findById(samhandler.id!!) }
    }

    @Test
    fun `hentSamhandlerPåId skal returnere null når samhandler ikke finnes`() {
        every { samhandlerRepository.findById(samhandler.id!!) } returns Optional.empty()
        val result = samhandlerService.hentSamhandlerPåId(samhandler.id!!)
        result shouldBe null
        verify { samhandlerRepository.findById(samhandler.id!!) }
    }

    @Test
    fun `oppdaterSamhandler skal returnere ResponseEntity med bad request når samhandlerId er null`() {
        val invalidSamhandlerDto = samhandlerDto.copy(samhandlerId = null)
        val result = shouldThrow<HttpStatusCodeException> { samhandlerService.oppdaterSamhandler(invalidSamhandlerDto) }
        result.responseBodyAsString shouldContain "SamhandlerId må angis ved oppdatering av samhandler"
    }

    @Test
    fun `oppdaterSamhandler skal returnere ResponseEntity med not found når samhandler ikke finnes`() {
        every { samhandlerRepository.findByIdent(samhandlerDto.samhandlerId!!.verdi) } returns null
        val result = samhandlerService.oppdaterSamhandler(samhandlerDto)
        result shouldBe ResponseEntity.notFound().build<Any>()
    }

    @Test
    fun `oppdaterSamhandler skal oppdatere og returnere ResponseEntity med SamhandlerDto`() {
        every { samhandlerRepository.save(any()) } returns samhandler
        every { samhandlerRepository.findByIdent(samhandlerDto.samhandlerId!!.verdi) } returns samhandler
        val result = samhandlerService.oppdaterSamhandler(samhandlerDto)
        result shouldBe ResponseEntity.ok(SamhandlerMapper.mapTilSamhandlerDto(samhandler))
    }

    @Test
    fun `validerInput returnerer bad request om adresselinje 2 eller 3 er fylt ut før 1`() {
        val adresse =
            AdresseDto(
                adresselinje1 = null,
                adresselinje2 = "Linje2",
                adresselinje3 = null,
                postnummer = null,
                poststed = null,
                land = null,
            )
        val dto = samhandlerDto.copy(adresse = adresse, språk = Språk.NB)
        val result = shouldThrow<HttpStatusCodeException> { samhandlerService.validerInput(dto) }
        result.responseBodyAsString shouldContain "Adresselinje1 må fylles ut før adresselinje 2 eller adresselinje 3."
    }

    @Test
    fun `validerInput returnerer bad request om landkode for adresse mangler`() {
        val adresse =
            AdresseDto(
                adresselinje1 = "Linje1",
                adresselinje2 = null,
                adresselinje3 = null,
                postnummer = null,
                poststed = null,
                land = null,
            )
        val dto = samhandlerDto.copy(adresse = adresse, språk = Språk.NB)
        val result = shouldThrow<HttpStatusCodeException> { samhandlerService.validerInput(dto) }
        result.responseBodyAsString shouldContain "Landkode med 3 tegn må angis for adresse."
    }

    @Test
    fun `validerInput returnerer bad request om landkode for adresse ugylid`() {
        val adresse =
            AdresseDto(
                adresselinje1 = "Linje1",
                adresselinje2 = null,
                adresselinje3 = null,
                postnummer = null,
                poststed = null,
                land = Landkode3(""),
            )
        val dto = samhandlerDto.copy(adresse = adresse, språk = Språk.NB)
        val result = shouldThrow<HttpStatusCodeException> { samhandlerService.validerInput(dto) }
        result.responseBodyAsString shouldContain "Landkode med 3 tegn må angis for adresse."
    }

    @Test
    fun `validerInput returnerer bad request om norsk adresse mangler postnummer eller poststed`() {
        val adresse =
            AdresseDto(
                adresselinje1 = "Linje1",
                adresselinje2 = null,
                adresselinje3 = null,
                postnummer = null,
                poststed = null,
                land = Landkode3("NOR"),
            )
        val dto = samhandlerDto.copy(adresse = adresse, språk = Språk.NB)
        val result = shouldThrow<HttpStatusCodeException> { samhandlerService.validerInput(dto) }
        result.responseBodyAsString shouldContain "Postnummer og poststed må angis for norske adresser."
    }

    @Test
    fun `validerInput returnerer bad request om kontonummer er for kort`() {
        val kontonummer =
            KontonummerDto(
                norskKontonummer = "123",
                iban = null,
                swift = null,
                banknavn = null,
                landkodeBank = null,
                valutakode = Valutakode.NOK,
                bankCode = null,
            )
        val dto = samhandlerDto.copy(kontonummer = kontonummer, språk = Språk.NB)
        val result = shouldThrow<HttpStatusCodeException> { samhandlerService.validerInput(dto) }
        result.responseBodyAsString shouldContain "Norsk kontonummer må være 11 tegn langt."
    }

    @Test
    fun `validerInput returnerer bad request om kontonummer er på feil format`() {
        val kontonummer =
            KontonummerDto(
                norskKontonummer = "12345678901",
                iban = null,
                swift = null,
                banknavn = null,
                landkodeBank = null,
                valutakode = Valutakode.NOK,
                bankCode = null,
            )
        every { KontonummerUtils.erGyldigKontonummerMod11("12345678901") } returns false
        val dto = samhandlerDto.copy(kontonummer = kontonummer, språk = Språk.NB)
        val result = shouldThrow<HttpStatusCodeException> { samhandlerService.validerInput(dto) }
        result.responseBodyAsString shouldContain "Det er angitt et ugyldig norsk kontonummer."
    }

    @Test
    fun `validerInput returnerer bad request om landkodeBank er uglydig`() {
        val kontonummer =
            KontonummerDto(
                norskKontonummer = null,
                iban = null,
                swift = null,
                banknavn = null,
                landkodeBank = Landkode3("NO"),
                valutakode = Valutakode.NOK,
                bankCode = null,
            )
        val dto = samhandlerDto.copy(kontonummer = kontonummer, språk = Språk.NB)
        val result = shouldThrow<HttpStatusCodeException> { samhandlerService.validerInput(dto) }
        result.responseBodyAsString shouldContain "Landkode for bank NO må ha 3 tegn."
    }

    @Test
    fun `validerInput returnerer bad request om språk er null`() {
        val dto =
            samhandlerDto.copy(språk = null, kontonummer = KontonummerDto(iban = "123", valutakode = Valutakode.NOK))
        val result = shouldThrow<HttpStatusCodeException> { samhandlerService.validerInput(dto) }
        result.responseBodyAsString shouldContain "Språk må angis."
    }

    @Test
    fun `validerInput returnerer null om alt er gydlig`() {
        val adresse =
            AdresseDto(
                adresselinje1 = "Linje1",
                adresselinje2 = null,
                adresselinje3 = null,
                postnummer = "1234",
                poststed = "Oslo",
                land = Landkode3("NOR"),
            )
        val kontonummer =

            KontonummerDto(
                norskKontonummer = "12345678901",
                iban = null,
                swift = null,
                banknavn = null,
                landkodeBank = Landkode3("NOR"),
                valutakode = Valutakode.NOK,
                bankCode = null,
            )
        every { KontonummerUtils.erGyldigKontonummerMod11("12345678901") } returns true
        val dto = samhandlerDto.copy(adresse = adresse, kontonummer = kontonummer, språk = Språk.NB)
        shouldNotThrow<HttpStatusCodeException> { samhandlerService.validerInput(dto) }
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
}

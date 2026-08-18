package no.nav.bidrag.sak.repository

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotHaveSize
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Fogdårsak
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.sak.Tilgangstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.sak.SpringTestRunner
import no.nav.bidrag.sak.domain.Bidragssak
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.sak.domain.Tilgang
import no.nav.bidrag.sak.service.BidragSakService
import no.nav.bidrag.transport.sak.BidragssakDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

internal class BidragSakDb2IntegrationIT : SpringTestRunner() {
    @Autowired
    private lateinit var bidragSakService: BidragSakService

    @Autowired
    private lateinit var bidragssakRepository: BidragssakRepository

    private val saksnummer = Saksnummer("1800020")

    private val fodselsnummer = Personident("321321")

    @BeforeEach
    fun setUp() {
        every { identConsumer.hentAlleIdenter(any()) }.answers { listOf(firstArg()) }
        val bidragssak = Bidragssak(saksnummer = saksnummer.verdi, eierfogd = "1701")
        val rolle = Rolle(fødselsnummer = fodselsnummer.verdi, rolleType = Rolletype.BIDRAGSMOTTAKER, bidragssak = bidragssak)
        bidragssak.roller.add(rolle)

        bidragssakRepository.save(bidragssak)
    }

    @Test
    fun `skal finne bidragssak for pip`() {
        val muligPip = bidragSakService.finnPipFor(saksnummer)

        muligPip shouldNotBe null
    }

    @Test
    fun `skal finne bidragssaker for foedselsnummer`() {
        val bidragssaker = bidragSakService.finnSakerFor(fodselsnummer)

        bidragssaker shouldNotHaveSize 0
    }

    @Test
    fun `skal ikke finne sak for FR rolle`() {
        val bidragssak = Bidragssak(saksnummer = "147258", eierfogd = "1701")
        val rolle = Rolle(fødselsnummer = fodselsnummer.verdi, rolleType = Rolletype.FEILREGISTRERT, bidragssak = bidragssak)
        bidragssak.roller.add(rolle)
        bidragssakRepository.save(bidragssak)

        val bidragssaker = bidragSakService.finnSakerFor(fodselsnummer)

        bidragssaker shouldNotHaveSize 0
        bidragssaker.forEach { (_, saksnummer): BidragssakDto ->
            saksnummer shouldNotBe "147258"
        }
    }

    @Test
    fun `skal finne maks saksnummer innenfor grense`() {
        val maxSaksnummer = bidragssakRepository.hentMaxLoepenummerSomIkkeOverskrider(1900000)!!

        maxSaksnummer shouldBeInRange 1800001..1899999
    }

    @Test
    fun `skal generere saksnummer`() {
        val saksnummer =
            Saksnummer((bidragssakRepository.hentMaxLoepenummerSomIkkeOverskrider(1900000)!! + 1).toString())
        bidragssakRepository.save(Bidragssak(saksnummer.verdi, "1701"))
        val nyBidragssak =
            bidragssakRepository.findByIdOrNull(saksnummer.verdi) ?: error("ny bidragssak mangler")

        nyBidragssak.eierfogd shouldBe "1701"
        nyBidragssak.fogdFomDato shouldBe LocalDate.now()
        nyBidragssak.status shouldBe Bidragssakstatus.NY
        nyBidragssak.ansatt shouldBe false
        nyBidragssak.inhabilitet shouldBe false
        nyBidragssak.levdeAdskilt shouldBe false
        nyBidragssak.opprettetDato shouldBe LocalDate.now()
        nyBidragssak.kategori shouldBe Sakskategori.NASJONAL
    }

    @Test
    fun `skal gi tilgang til eierfogden`() {
        val saksnummer =
            Saksnummer((bidragssakRepository.hentMaxLoepenummerSomIkkeOverskrider(1900000)!! + 1).toString())
        val bidragssak = Bidragssak(saksnummer.verdi, "1701")
        val tilgang = Tilgang(enhetsnummer = "1701", bidragssak = bidragssak)
        bidragssak.tilganger.add(tilgang)
        bidragssakRepository.save(bidragssak)

        val tilgangResultat = bidragssakRepository.findByIdOrThrow(saksnummer.verdi).tilganger.first()

        tilgangResultat.enhetsnummer shouldBe "1701"
        tilgangResultat.tilgangFomDato shouldBe LocalDate.now()
        tilgangResultat.årsak shouldBe Fogdårsak.EIER
        tilgangResultat.type shouldBe Tilgangstype.EIER
    }

    @Test
    fun `skal ikke finne saker som er sanerte`() {
        val fodselsnummerForSanertSak = Personident("241179")
        val fodselsnummerForSakSomIkkeErSanert = fodselsnummer
        val bidragssak =
            Bidragssak(
                "963258",
                "1701",
            )
        val rolle = Rolle(fødselsnummer = "161275", rolleType = Rolletype.BIDRAGSMOTTAKER, bidragssak = bidragssak)
        bidragssak.roller.add(rolle)
        bidragssakRepository.save(bidragssak)

        val sanerteSaker = bidragSakService.finnSakerFor(fodselsnummerForSanertSak)
        val saker = bidragSakService.finnSakerFor(fodselsnummerForSakSomIkkeErSanert)

        sanerteSaker shouldHaveSize 0
        saker shouldNotHaveSize 0
    }
}

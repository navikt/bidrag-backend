package no.nav.bidrag.sak.domain

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.sak.BidragSakProfiles
import no.nav.bidrag.sak.util.FnrGenerator
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(BidragSakProfiles.TEST)
class BidragssakTest {
    private var bidragssak: Bidragssak? = null

    @Test
    fun `should map dto`() {
        val fnr = FnrGenerator.generer()
        bidragssak = createBidragssak("007", mutableSetOf(createRolle(fnr, Rolletype.BIDRAGSMOTTAKER)))
        val bidragSakDto = bidragssak!!.tilBidragSakDto(false)
        bidragSakDto shouldNotBe null
        bidragSakDto.saksnummer shouldBe Saksnummer("007")
        bidragSakDto.roller.size shouldBe 1
        bidragSakDto.roller[0].fødselsnummer shouldBe Personident(fnr)
        bidragSakDto.roller[0].type shouldBe Rolletype.BIDRAGSMOTTAKER
    }

    @Test
    fun `skal ikke mappe roller som ikke har gyldige verdier og som ikke er personer`() {
        val gyldigFnr = FnrGenerator.generer()
        val rolleMedSamhandlerIdent =
            Rolle(rolleType = Rolletype.BIDRAGSMOTTAKER, samhandlerIdent = "1")
        bidragssak =
            createBidragssak(
                "1701",
                mutableSetOf(
                    createRolle("121252ABCDE", Rolletype.REELMOTTAKER),
                    createRolle("98765432101", Rolletype.BIDRAGSMOTTAKER),
                    createRolle("87654321012", Rolletype.BIDRAGSMOTTAKER),
                    rolleMedSamhandlerIdent,
                    // FR = feilregistrert...
                    createRolle(FnrGenerator.generer(), Rolletype.FEILREGISTRERT),
                    createRolle(gyldigFnr, Rolletype.BIDRAGSPLIKTIG),
                ),
            )

        val bidragSakDto = bidragssak!!.tilBidragSakDto(false)

        bidragSakDto shouldNotBe null
        bidragSakDto.roller.size shouldBe 1
        bidragSakDto.roller[0].fødselsnummer shouldBe Personident(gyldigFnr)
        bidragSakDto.roller[0].type shouldBe Rolletype.BIDRAGSPLIKTIG
    }

    @Test
    fun `skal ikke returnere samme fødselsnummer flere ganger`() {
        val gyldigFnr = FnrGenerator.generer()
        bidragssak =
            createBidragssak(
                "1701",
                mutableSetOf(
                    createRolle(gyldigFnr, Rolletype.BIDRAGSPLIKTIG),
                    createRolle(gyldigFnr, Rolletype.REELMOTTAKER),
                ),
            )

        val bidragSakPipDto = bidragssak!!.tilBidragSakPipDto()

        bidragSakPipDto shouldNotBe null
        bidragSakPipDto.roller shouldContainExactly listOf(gyldigFnr)
    }

    companion object {
        fun createBidragssak(
            saksnummer: String,
            roller: MutableSet<Rolle>,
        ): Bidragssak = createBidragssak(saksnummer, Sakskategori.NASJONAL, roller)

        fun createBidragssak(
            saksnummer: String,
            kategori: Sakskategori,
            roller: MutableSet<Rolle>,
        ): Bidragssak = Bidragssak(
            saksnummer = saksnummer,
            eierfogd = "1234",
            status = Bidragssakstatus.NY,
            roller = roller,
            kategori = kategori,
        )

        fun createBidragssak(
            saksnummer: String,
            eierfogd: String,
            saksstatus: Bidragssakstatus,
            roller: MutableSet<Rolle>,
        ): Bidragssak = Bidragssak(
            saksnummer = saksnummer,
            status = saksstatus,
            eierfogd = eierfogd,
            roller = roller,
        )

        fun createRolle(
            fødselsnummer: String?,
            rolleType: Rolletype,
        ): Rolle = Rolle(fødselsnummer = fødselsnummer, rolleType = rolleType)
    }
}

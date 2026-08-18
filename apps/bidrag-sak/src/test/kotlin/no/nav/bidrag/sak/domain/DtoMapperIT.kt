package no.nav.bidrag.sak.domain

import io.kotest.matchers.string.shouldContain
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.sak.SpringTestRunner
import no.nav.bidrag.sak.domain.BidragssakTest.Companion.createBidragssak
import no.nav.bidrag.sak.domain.BidragssakTest.Companion.createRolle
import no.nav.bidrag.sak.util.FnrGenerator
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.junit.jupiter.api.Test

internal class DtoMapperIT : SpringTestRunner() {
    @Test
    fun `skal mappe bidragssak til json`() {
        val fnr = FnrGenerator.generer()
        val bidragssak =
            createBidragssak(
                "69",
                Sakskategori.NASJONAL,
                mutableSetOf(createRolle(fnr, Rolletype.BARN)),
            )
        val json = commonObjectmapper.writeValueAsString(bidragssak.tilBidragSakDto(false))
        json shouldContain "\"saksnummer\":\"69\""
        json shouldContain "\"kategori\":\"N\""
        json shouldContain "\"roller\":["
        json shouldContain "\"foedselsnummer\":\"$fnr\""
        json shouldContain "\"rolleType\":\"BA\""
        json shouldContain "]"
    }
}

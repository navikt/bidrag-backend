package no.nav.bidrag.sak.repository

import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.sak.SpringTestRunner
import no.nav.bidrag.sak.domain.Bidragssak
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.sak.domain.Tilgang
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class BidragssakRepositoryIT : SpringTestRunner() {
    @Autowired
    private lateinit var repository: BidragssakRepository

    @Test
    fun `insert klarer å lagre en forekomst av Bidragssak`() {
        val bidragssak =
            Bidragssak(
                saksnummer = "321321",
                eierfogd = "1701",
                fogdFomDato = LocalDate.of(2020, 1, 1),
                opprettetDato = LocalDate.of(2021, 2, 2),
            )

        bidragssak.roller.addAll(listOf(Rolle(rolleType = Rolletype.BIDRAGSMOTTAKER, bidragssak = bidragssak)))
        bidragssak.tilganger.addAll(listOf(Tilgang(enhetsnummer = "1701", bidragssak = bidragssak)))

        val lagretHendelse = repository.save(bidragssak)

        val hentetHendelse = repository.findByIdOrThrow(lagretHendelse.saksnummer)
        hentetHendelse.saksnummer shouldBe bidragssak.saksnummer
    }
}

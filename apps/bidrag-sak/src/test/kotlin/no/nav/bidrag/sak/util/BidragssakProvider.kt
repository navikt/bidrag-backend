package no.nav.bidrag.sak.util

import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.sak.domain.Bidragssak
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.sak.repository.BidragssakRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BidragssakProvider(
    private val repository: BidragssakRepository,
) {
    @Transactional
    fun lagreSakTilDatabase(saksnummer: Saksnummer = Saksnummer("1234567")): Saksnummer {
        // Do nothing entry already exists in store
        if (repository.findBySaksnummer(saksnummer.verdi) != null) {
            return saksnummer
        }
        val bidragssak = Bidragssak(saksnummer.verdi, eierfogd = "1234")
        val rolle =
            Rolle(
                fødselsnummer = genererFødselsnummer(),
                rolleType = Rolletype.BIDRAGSMOTTAKER,
                bidragssak = bidragssak,
            )
        bidragssak.roller.add(rolle)
        repository.save(bidragssak)
        return saksnummer
    }
}

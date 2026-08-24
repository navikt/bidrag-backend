package no.nav.bidrag.dokument.bestilling.tjenester

import no.nav.bidrag.dokument.bestilling.consumer.BidragSakConsumer
import no.nav.bidrag.transport.sak.BidragssakDto
import org.springframework.stereotype.Service

@Service
class SakService(
    private val bidragSakConsumer: BidragSakConsumer,
) {
    fun hentSak(saksnr: String): BidragssakDto? = bidragSakConsumer.hentSak(saksnr)
}

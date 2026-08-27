package no.nav.bidrag.commons.service.consumers

import no.nav.bidrag.transport.sak.BidragssakDto

interface FellesSakConsumer {
    fun hentSak(saksnr: String): BidragssakDto
}

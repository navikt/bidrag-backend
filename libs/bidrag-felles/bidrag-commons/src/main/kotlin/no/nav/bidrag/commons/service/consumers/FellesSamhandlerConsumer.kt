package no.nav.bidrag.commons.service.consumers

import no.nav.bidrag.transport.samhandler.SamhandlerDto

interface FellesSamhandlerConsumer {
    fun hentSamhandler(samhandlerId: String): SamhandlerDto?
}

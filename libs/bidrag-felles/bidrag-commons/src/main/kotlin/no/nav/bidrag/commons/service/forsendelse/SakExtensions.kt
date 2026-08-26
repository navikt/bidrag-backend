package no.nav.bidrag.commons.service.forsendelse

import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.transport.sak.BidragssakDto

val BidragssakDto.bidragsmottaker get() = roller.find { it.type == Rolletype.BIDRAGSMOTTAKER }
val BidragssakDto.bidragspliktig get() = roller.find { it.type == Rolletype.BIDRAGSPLIKTIG }

package no.nav.bidrag.beregn.barnebidrag.service.external

import no.nav.bidrag.transport.behandling.beregning.felles.BidragBeregningRequestDto
import no.nav.bidrag.transport.behandling.beregning.felles.BidragBeregningResponsDto
import no.nav.bidrag.transport.behandling.beregning.felles.HentBeregningForSøknaderRequest
import no.nav.bidrag.transport.behandling.beregning.felles.HentBeregningForSøknaderRespons

interface BeregningBBMConsumer {
    fun hentBeregning(request: BidragBeregningRequestDto): BidragBeregningResponsDto

    fun hentAlleBeregninger(request: BidragBeregningRequestDto): BidragBeregningResponsDto

    fun hentBeregningForSøknader(request: HentBeregningForSøknaderRequest): HentBeregningForSøknaderRespons
}

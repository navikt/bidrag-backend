package no.nav.bidrag.beregn.core.exception

import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultatV2
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BidragsberegningOrkestratorResponseV2

class BidragsberegningFeiletTekniskException(val melding: String, val data: BidragsberegningOrkestratorResponseV2) : RuntimeException(melding)

package no.nav.bidrag.beregn.core.exception

import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultatV2

class IkkeFullBidragsevneOgOppfostringsbidragBeregningException(val melding: String, val data: List<BeregnetBarnebidragResultatV2>) :
    RuntimeException(melding)

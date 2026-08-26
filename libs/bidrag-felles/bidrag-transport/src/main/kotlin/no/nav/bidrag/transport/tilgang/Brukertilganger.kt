package no.nav.bidrag.transport.tilgang

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.behandling.Behandlingstema

@Schema(description = "Brukertilganger for en bruker")
data class Brukertilganger(
    @param:Schema(
        description = "Indikerer om brukeren har tilgang til Bisys.",
    )
    val bisysTilgang: Boolean,
    @param:Schema(
        description = "Indikerer om brukeren har tilgang til utlandssaker.",
    )
    val utlandTilgang: Boolean,
    @param:Schema(
        description = "Indikerer om brukeren har tilgang til å lese saker.",
    )
    val leseSakTilgang: Boolean,
    @param:Schema(
        description = "Indikerer om brukeren har tilgang til å behandle saker.",
    )
    val behandleSakTilgang: Boolean,
    @param:Schema(
        description = "Indikerer om brukeren har tilgang til foreldreskapssaker.",
    )
    val foreldreskapTilgang: Boolean,
    @param:Schema(
        description = "Indikerer om brukeren har administrativ tilgang.",
    )
    val administrasjonTilgang: Boolean,
    @param:Schema(
        description = "Liste over behandlingstemaer brukeren har tilgang til.",
    )
    val behandlingstemaer: List<Behandlingstema>,
)

package no.nav.bidrag.transport.tilgang

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer

data class TilgangskontrollResponse(
    @param:Schema(
        description =
            "Indikerer om brukeren har tilgang. " +
                "Hvis tilgang er avslått, vil detaljer inneholde mer informasjon.",
    )
    val harTilgang: Boolean,
    @param:Schema(
        description =
            "Liste over detaljer om tilgangsbeslutninger, inkludert opprinnelse og begrunnelse. " +
                "Vil kun settes om tilgang avslås.",
    )
    val detaljer: List<TilgangskontrollResponseDetaljer> = emptyList(),
)

data class TilgangskontrollResponseDetaljer(
    val harTilgang: Boolean,
    val begrunnelse: String,
    val opprinnelseTilgangsbeslutning: OpprinnelseTilgangsbeslutning,
)

enum class OpprinnelseTilgangsbeslutning {
    TILGANGSMASKIN,
    GRAPH,
    BIDRAG_SAK_PIP,
    BIDRAG_TILGANGSKONTROLL,
}

data class TilgangTilSakRequest(
    val saksnummer: Saksnummer,
)

data class TilgangTilPersonRequest(
    val personident: Personident,
)

data class TilgangTilTemaRequest(
    val tema: String,
    val navIdent: String? = null,
)

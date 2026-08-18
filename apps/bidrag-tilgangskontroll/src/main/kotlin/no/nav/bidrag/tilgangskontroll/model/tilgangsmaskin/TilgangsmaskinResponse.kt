package no.nav.bidrag.tilgangskontroll.model.tilgangsmaskin

import io.swagger.v3.oas.annotations.media.Schema

data class TilgangsmaskinBulkResponse(
    val antattId: String?,
    val resultater: List<TilgangsmaskinResultat>,
)

data class TilgangsmaskinResultat(
    val brukerId: String?,
    val status: Int?,
    val detaljer: TilgangsmaskinResultatDetaljer?,
)

data class TilgangsmaskinResultatDetaljer(
    val type: String?,
    val title: String?,
    val status: Int?,
    val instance: String?,
    val brukerIdent: String?,
    val navIdent: String?,
    val begrunnelse: String?,
    val traceId: String?,
    val kanOverstyres: Boolean?,
)

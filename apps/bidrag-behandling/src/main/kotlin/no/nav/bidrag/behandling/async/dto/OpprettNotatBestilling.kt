package no.nav.bidrag.behandling.async.dto

data class OpprettNotatBestilling(
    val behandlingId: Long,
    val saksnummer: String? = null,
)

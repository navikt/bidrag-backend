package no.nav.bidrag.behandling.async.dto

data class OpprettSøknaderKlageOmgjøringBestilling(
    val behandlingId: Long,
    val søknadsid: Long,
    val opprettetAvEnhet: String? = null,
    val fjernSøknaderFraPåklagetVedtak: Boolean = false,
    val waitForCommit: Boolean = true,
)

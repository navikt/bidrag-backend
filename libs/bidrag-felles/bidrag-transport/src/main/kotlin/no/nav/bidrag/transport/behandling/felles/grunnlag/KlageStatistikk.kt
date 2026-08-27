package no.nav.bidrag.transport.behandling.felles.grunnlag

data class KlageStatistikk(
    val årsakskode: String,
    val årsakskodeUnderkategori: String,
) : GrunnlagInnhold

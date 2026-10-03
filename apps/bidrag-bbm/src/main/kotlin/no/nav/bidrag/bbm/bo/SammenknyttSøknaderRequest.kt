package no.nav.bidrag.bbm.bo

import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad

data class SammenknyttSøknaderRequest(
    val hovedsøknadsid: Long,
    val referertSøknadsid: Long,
)

data class SlettSammenknytningForSøknadRequest(
    val søknadsid: Long,
)

data class SlettHovedsøknadRequest(
    val eksisterendeHovedsøknadsid: Long,
    val nyHovedsøknadsid: Long? = null,
    val feilregistrerFFSøknader: Boolean = false,
)

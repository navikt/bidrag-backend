package no.nav.bidrag.transport.søknad

import no.nav.bidrag.domene.enums.behandling.SøknadsknytningStatus
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad

data class FinnSammenknytningerHovedsøknadRequest(
    val søknadsid: Long,
    val status: SøknadsknytningStatus = SøknadsknytningStatus.Aktiv,
    val statuser: List<SøknadsknytningStatus> = emptyList(),
)

data class FinnSammenknytningerHovedsøknadResponse(
    val hovedsøknadsid: Long? = null,
    val søknader: List<HentSøknad>,
)

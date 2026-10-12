package no.nav.bidrag.behandling.database.repository

import no.nav.bidrag.behandling.database.datamodell.extensions.LasterGrunnlagDetaljer

interface CustomBehandlingRepository {
    fun hentLasterGrunnlagStatus(id: Long): LasterGrunnlagDetaljer?

    /**
     * Øker versjonen til behandlingen og eventuell FF-hovedbehandling når transaksjonen committes.
     * Hvis en annen transaksjon har oppdatert behandlingen i mellomtiden så feiler commit med optimistisk låsefeil.
     * Må kalles før behandlingen endres.
     */
    fun låsBehandlingForOppdatering(behandlingId: Long)
}

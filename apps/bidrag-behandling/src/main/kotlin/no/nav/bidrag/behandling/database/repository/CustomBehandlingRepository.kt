package no.nav.bidrag.behandling.database.repository

import no.nav.bidrag.behandling.database.datamodell.extensions.LasterGrunnlagDetaljer

interface CustomBehandlingRepository {
    fun hentLasterGrunnlagStatus(id: Long): LasterGrunnlagDetaljer?

    /** Flusher og tømmer persistence context slik at neste lasting henter siste tilstand fra databasen. Billigere enn refresh. */
    fun tømPersistenceContext()
}

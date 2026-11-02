package no.nav.bidrag.behandling.database.repository

import no.nav.bidrag.behandling.database.datamodell.extensions.LasterGrunnlagDetaljer

interface CustomBehandlingRepository {
    fun hentLasterGrunnlagStatus(id: Long): LasterGrunnlagDetaljer?

    /** Flusher persistence context slik at nye entiteter (f.eks. roller) får tildelt id. */
    fun flush()

    /** Flusher og tømmer persistence context slik at neste lasting henter siste tilstand fra databasen. Billigere enn refresh. */
    fun tømPersistenceContext()
}

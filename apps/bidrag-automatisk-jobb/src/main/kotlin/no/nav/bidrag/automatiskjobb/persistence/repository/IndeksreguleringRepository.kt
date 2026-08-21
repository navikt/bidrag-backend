package no.nav.bidrag.automatiskjobb.persistence.repository

import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface IndeksreguleringRepository : JpaRepository<Indeksregulering, Int> {
    fun findByBarnAndStønadstypeAndÅr(
        barn: Barn,
        stønadstype: Stønadstype,
        år: Int,
    ): Indeksregulering?

    @Suppress("Brukt i batch")
    fun findAllByStønadstypeInAndÅr(
        stønadstyper: Collection<Stønadstype>,
        år: Int,
        pageable: Pageable,
    ): Page<Indeksregulering>

    fun findAllByStatusAndBehandlingstypeAndStønadstypeInAndÅr(
        status: Status,
        behandlingstype: Behandlingstype,
        stønadstyper: Collection<Stønadstype>,
        år: Int,
    ): List<Indeksregulering>

    // Keyset-paginering (id > sisteId) i stedet for offset, siden status endres av batch-prosessoren
    // under kjøring. Offset-paginering ville da hoppe over rader mellom sidene, se
    // FattVedtakIndeksreguleringBidragBatchReader.
    fun findAllByStatusAndBehandlingstypeAndVedtakIsNotNullAndÅrAndStønadstypeInAndIdGreaterThanOrderByIdAsc(
        status: Status,
        behandlingstype: Behandlingstype,
        år: Int,
        stønadstyper: Collection<Stønadstype>,
        id: Int,
        pageable: Pageable,
    ): List<Indeksregulering>

    fun deleteAllByÅr(år: Int)

    fun findAllByStatusAndÅr(
        status: Status,
        år: Int,
    ): List<Indeksregulering>
}

package no.nav.bidrag.grunnlag.persistence.repository

import jakarta.persistence.LockModeType
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.grunnlag.persistence.entity.Valutakursgrunnlag
import no.nav.bidrag.grunnlag.persistence.entity.ValutakursgrunnlagStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface ValutakursgrunnlagRepository : JpaRepository<Valutakursgrunnlag, Int> {
    fun findByStatus(status: ValutakursgrunnlagStatus, pageable: Pageable): Page<Valutakursgrunnlag>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByBasisvalutaAndBrukFra(basisvaluta: Valutakode, brukFra: LocalDateTime): Valutakursgrunnlag?

    @Query("SELECT vg FROM Valutakursgrunnlag vg WHERE vg.basisvaluta = :valutakode AND vg.brukFra <= :dato AND vg.brukTil > :dato")
    fun hentValutakursgrunnlag(valutakode: Valutakode, dato: LocalDateTime): Valutakursgrunnlag?
}

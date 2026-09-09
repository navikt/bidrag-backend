package no.nav.bidrag.grunnlag.persistence.repository

import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.grunnlag.persistence.entity.Valutakursgrunnlag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface ValutakursgrunnlagRepository : JpaRepository<Valutakursgrunnlag, Int> {
    @Query("SELECT vg FROM Valutakursgrunnlag vg WHERE vg.basisvaluta = :valutakode AND vg.brukFra <= :dato AND vg.brukTil >  :dato")
    fun hentValutakursgrunnlag(valutakode: Valutakode, dato: LocalDate): Valutakursgrunnlag
}
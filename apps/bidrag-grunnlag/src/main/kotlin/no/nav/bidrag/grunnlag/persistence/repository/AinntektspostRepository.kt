package no.nav.bidrag.grunnlag.persistence.repository

import no.nav.bidrag.grunnlag.persistence.entity.Ainntektspost
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AinntektspostRepository : JpaRepository<Ainntektspost, Int> {

    @Query(
        "select ainp from Ainntektspost ainp where ainp.inntektId = :inntektId order by ainp.utbetalingsperiode, ainp.inntektType",
    )
    fun hentInntektsposter(inntektId: Int): List<Ainntektspost>
}

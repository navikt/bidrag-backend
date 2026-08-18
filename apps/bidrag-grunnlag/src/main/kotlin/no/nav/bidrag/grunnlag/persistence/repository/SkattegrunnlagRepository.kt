package no.nav.bidrag.grunnlag.persistence.repository

import no.nav.bidrag.grunnlag.persistence.entity.Skattegrunnlag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SkattegrunnlagRepository : JpaRepository<Skattegrunnlag, Int> {

    @Query(
        "select sg from Skattegrunnlag sg where sg.grunnlagspakkeId = :grunnlagspakkeId and sg.aktiv = true order by sg.personId, sg.periodeFra",
    )
    fun hentSkattegrunnlag(grunnlagspakkeId: Int): List<Skattegrunnlag>
}

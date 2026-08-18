package no.nav.bidrag.grunnlag.persistence.repository

import no.nav.bidrag.grunnlag.persistence.entity.Skattegrunnlagspost
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SkattegrunnlagspostRepository : JpaRepository<Skattegrunnlagspost, Int> {

    @Query(
        "select sgp from Skattegrunnlagspost sgp where sgp.skattegrunnlagId = :inntektId order by sgp.skattegrunnlagType, sgp.inntektType",
    )
    fun hentSkattegrunnlagsposter(inntektId: Int): List<Skattegrunnlagspost>
}

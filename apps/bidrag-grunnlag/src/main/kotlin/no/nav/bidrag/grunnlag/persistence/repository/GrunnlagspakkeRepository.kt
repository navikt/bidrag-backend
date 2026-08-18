package no.nav.bidrag.grunnlag.persistence.repository

import no.nav.bidrag.grunnlag.persistence.entity.Grunnlagspakke
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface GrunnlagspakkeRepository : JpaRepository<Grunnlagspakke, Int> {

    @Modifying
    @Query(
        "update Grunnlagspakke gp set gp.gyldigTil = current_date where gp.grunnlagspakkeId = :grunnlagspakkeId",
    )
    fun lukkGrunnlagspakke(grunnlagspakkeId: Int)

    @Query(
        "select gp.formaal from Grunnlagspakke gp where gp.grunnlagspakkeId = :grunnlagspakkeId",
    )
    fun hentFormaalGrunnlagspakke(grunnlagspakkeId: Int): String

    @Modifying
    @Query(
        "update Grunnlagspakke gp set gp.endretTimestamp = :timestampOppdatering where gp.grunnlagspakkeId = :grunnlagspakkeId",
    )
    fun oppdaterEndretTimestamp(grunnlagspakkeId: Int, timestampOppdatering: LocalDateTime)
}

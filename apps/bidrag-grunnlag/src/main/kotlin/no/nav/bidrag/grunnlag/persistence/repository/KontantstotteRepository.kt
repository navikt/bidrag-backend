package no.nav.bidrag.grunnlag.persistence.repository

import no.nav.bidrag.grunnlag.persistence.entity.Kontantstotte
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface KontantstotteRepository : JpaRepository<Kontantstotte, Int> {

    @Query(
        "select ks " +
            "from Kontantstotte ks " +
            "where ks.grunnlagspakkeId = :grunnlagspakkeId and ks.aktiv = true " +
            "order by ks.partPersonId, ks.periodeFra, ks.barnPersonId",
    )
    fun hentKontantstotte(grunnlagspakkeId: Int): List<Kontantstotte>

    @Modifying
    @Query(
        "update Kontantstotte ks " +
            "set ks.aktiv = false, ks.brukTil = :timestampOppdatering " +
            "where ks.grunnlagspakkeId = :grunnlagspakkeId and ks.partPersonId in :personIdListe and ks.aktiv = true",
    )
    fun oppdaterEksisterendeKontantstotteTilInaktiv(grunnlagspakkeId: Int, personIdListe: List<String>, timestampOppdatering: LocalDateTime)
}

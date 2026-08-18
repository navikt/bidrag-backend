package no.nav.bidrag.grunnlag.persistence.repository

import no.nav.bidrag.grunnlag.persistence.entity.Barnetilsyn
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface BarnetilsynRepository : JpaRepository<Barnetilsyn, Int> {

    @Query(
        "select bts " +
            "from Barnetilsyn bts " +
            "where bts.grunnlagspakkeId = :grunnlagspakkeId and bts.aktiv = true " +
            "order by bts.partPersonId, bts.periodeFra, bts.barnPersonId",
    )
    fun hentBarnetilsyn(grunnlagspakkeId: Int): List<Barnetilsyn>

    @Modifying
    @Query(
        "update Barnetilsyn bts " +
            "set bts.aktiv = false, bts.brukTil = :timestampOppdatering " +
            "where bts.grunnlagspakkeId = :grunnlagspakkeId and bts.partPersonId in :personIdListe and bts.aktiv = true",
    )
    fun oppdaterEksisterendeBarnetilsynTilInaktiv(grunnlagspakkeId: Int, personIdListe: List<String>, timestampOppdatering: LocalDateTime)
}

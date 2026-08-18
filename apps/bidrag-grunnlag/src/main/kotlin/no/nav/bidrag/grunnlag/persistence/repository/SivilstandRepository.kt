package no.nav.bidrag.grunnlag.persistence.repository

import no.nav.bidrag.grunnlag.persistence.entity.Sivilstand
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface SivilstandRepository : JpaRepository<Sivilstand, Int> {

    @Query(
        "select si from Sivilstand si where si.grunnlagspakkeId = :grunnlagspakkeId and si.aktiv = true order by si.personId, si.periodeFra",
    )
    fun hentSivilstand(grunnlagspakkeId: Int): List<Sivilstand>

    @Modifying
    @Query(
        "update Sivilstand sst " +
            "set sst.aktiv = false, sst.brukTil = :timestampOppdatering " +
            "where sst.grunnlagspakkeId = :grunnlagspakkeId and sst.personId in :personIdListe and sst.aktiv = true",
    )
    fun oppdaterEksisterendeSivilstandTilInaktiv(grunnlagspakkeId: Int, personIdListe: List<String>, timestampOppdatering: LocalDateTime)
}

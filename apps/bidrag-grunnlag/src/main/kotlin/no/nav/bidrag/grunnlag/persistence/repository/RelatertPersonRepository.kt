package no.nav.bidrag.grunnlag.persistence.repository

import no.nav.bidrag.grunnlag.persistence.entity.RelatertPerson
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface RelatertPersonRepository : JpaRepository<RelatertPerson, Int> {

    @Query(
        "select hm " +
            "from RelatertPerson hm " +
            "where hm.grunnlagspakkeId = :grunnlagspakkeId and hm.aktiv = true " +
            "order by hm.partPersonId, hm.relatertPersonPersonId, hm.navn, hm.fodselsdato, hm.husstandsmedlemPeriodeFra",
    )
    fun hentRelatertePersoner(grunnlagspakkeId: Int): List<RelatertPerson>

    @Modifying
    @Query(
        "update RelatertPerson hm " +
            "set hm.aktiv = false, hm.brukTil = :timestampOppdatering " +
            "where hm.grunnlagspakkeId = :grunnlagspakkeId and hm.partPersonId in :personIdListe and hm.aktiv = true",
    )
    fun oppdaterEksisterendeRelatertPersonTilInaktiv(grunnlagspakkeId: Int, personIdListe: List<String>, timestampOppdatering: LocalDateTime)
}

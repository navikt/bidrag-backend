package no.nav.bidrag.grunnlag.persistence.repository

import no.nav.bidrag.grunnlag.persistence.entity.UtvidetBarnetrygdOgSmaabarnstillegg
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface UtvidetBarnetrygdOgSmaabarnstilleggRepository : JpaRepository<UtvidetBarnetrygdOgSmaabarnstillegg, Int> {

    @Query(
        "select ubst " +
            "from UtvidetBarnetrygdOgSmaabarnstillegg ubst " +
            "where ubst.grunnlagspakkeId = :grunnlagspakkeId and ubst.aktiv = true " +
            "order by ubst.personId, ubst.periodeFra",
    )
    fun hentUbst(grunnlagspakkeId: Int): List<UtvidetBarnetrygdOgSmaabarnstillegg>

    @Modifying
    @Query(
        "update UtvidetBarnetrygdOgSmaabarnstillegg ubst " +
            "set ubst.aktiv = false, ubst.brukTil = :timestampOppdatering " +
            "where ubst.grunnlagspakkeId = :grunnlagspakkeId and ubst.personId in :personIdListe and ubst.aktiv = true",
    )
    fun oppdaterEksisterendeUtvidetBarnetrygOgSmaabarnstilleggTilInaktiv(
        grunnlagspakkeId: Int,
        personIdListe: List<String>,
        timestampOppdatering: LocalDateTime,
    )
}

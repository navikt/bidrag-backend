package no.nav.bidrag.grunnlag.persistence.repository

import no.nav.bidrag.grunnlag.persistence.entity.Ainntekt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AinntektRepository : JpaRepository<Ainntekt, Int> {

    @Query(
        "select ain from Ainntekt ain where ain.grunnlagspakkeId = :grunnlagspakkeId and ain.aktiv = true order by ain.personId, ain.periodeFra",
    )
    fun hentAinntekter(grunnlagspakkeId: Int): List<Ainntekt>
}

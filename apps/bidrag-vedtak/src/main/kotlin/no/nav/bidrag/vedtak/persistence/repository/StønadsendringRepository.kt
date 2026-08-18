package no.nav.bidrag.vedtak.persistence.repository

import no.nav.bidrag.vedtak.persistence.entity.Stønadsendring
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface StønadsendringRepository : CrudRepository<Stønadsendring, Int> {

    @Query(
        "select st from Stønadsendring st where st.vedtak.id = :vedtaksid order by st.id",
    )
    fun hentAlleStønadsendringerForVedtak(vedtaksid: Int): List<Stønadsendring>

    // Sletter angitt stønadsendring
    @Modifying
    @Query(
        "delete from Stønadsendring se where se.id = :stønadsendringsid",
    )
    fun slettStønadsendring(stønadsendringsid: Int): Int

    @Query(
        "select st from Stønadsendring st join fetch st.vedtak where st.sak = :saksnr and st.type = :type and st.skyldner in :skyldnerListe " +
            "and st.kravhaver in :kravhaverListe and st.vedtak.vedtakstidspunkt is not null order by st.vedtak.id",
    )
    fun hentVedtakForStønadMedIdenthistorikk(
        saksnr: String,
        type: String,
        skyldnerListe: List<String>,
        kravhaverListe: List<String>,
    ): List<Stønadsendring>

    @Query(
        "select st from Stønadsendring st where st.skyldner in :skyldnerListe " +
            "and st.innkreving = :medInnkreving and st.beslutning = :endring and st.vedtak.kilde = :manuelt " +
            "and st.vedtak.type != :indeksregulering and st.vedtak.type != :aldersjustering " +
            "order by st.vedtak.id",
    )
    fun hentVedtakForBpMedIdenthistorikk(
        @Param("medInnkreving") medInnkreving: String,
        @Param("endring") endring: String,
        @Param("manuelt") manuelt: String,
        @Param("indeksregulering") indeksregulering: String,
        @Param("aldersjustering") aldersjustering: String,
        @Param("skyldnerListe") skyldnerListe: List<String>,
    ): List<Stønadsendring>
}

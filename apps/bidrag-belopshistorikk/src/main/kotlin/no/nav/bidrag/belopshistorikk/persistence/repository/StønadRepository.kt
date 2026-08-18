package no.nav.bidrag.belopshistorikk.persistence.repository

import no.nav.bidrag.belopshistorikk.persistence.entity.Stønad
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface StønadRepository : CrudRepository<Stønad, Int> {
    @Query(
        "select st from Stønad st where st.type = :stønadstype and st.skyldner in :skyldnerIdentListe and st.kravhaver in :kravhaverIdentListe " +
            "and st.sak = :sak",
    )
    fun finnStønad(stønadstype: String, skyldnerIdentListe: List<String>, kravhaverIdentListe: List<String>, sak: String): List<Stønad>

    @Query(
        "update Stønad st set st.endretAv = :opprettetAv, st.endretTidspunkt = CURRENT_TIMESTAMP, " +
            "st.nesteIndeksreguleringsår = :nesteIndeksreguleringsår where st.stønadsid = :stønadsid",
    )
    @Modifying
    fun oppdaterStønad(stønadsid: Int, opprettetAv: String, nesteIndeksreguleringsår: Int?)

    @Query(
        "update Stønad st set st.mottaker = :mottaker, st.endretAv = :opprettetAv, st.endretTidspunkt = CURRENT_TIMESTAMP " +
            "where st.stønadsid = :stønadsid",
    )
    @Modifying
    fun endreMottakerForStønad(stønadsid: Int, mottaker: String, opprettetAv: String)

    @Query(
        "select st from Stønad st where st.sak = :sak order by st.stønadsid",
    )
    fun finnStønaderForSak(sak: String): List<Stønad>

    @Query(
        "select st from Stønad st where st.skyldner in :skyldnerIdentListe and st.type in ('BIDRAG', 'BIDRAG18AAR', 'OPPFOSTRINGSBIDRAG') " +
            "order by st.stønadsid",
    )
    fun finnBidragssakerForSkyldner(skyldnerIdentListe: List<String>): List<Stønad>

    @Query(
        "select st from Stønad st where st.skyldner in :skyldnerIdentListe order by st.stønadsid",
    )
    fun finnAlleStønaderForSkyldner(skyldnerIdentListe: List<String>): List<Stønad>
}

package no.nav.bidrag.belopshistorikk.persistence.repository

import no.nav.bidrag.belopshistorikk.persistence.entity.Engangsbeløp
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.time.LocalDateTime

interface EngangsbeløpRepository : CrudRepository<Engangsbeløp, Int> {
    @Query(
        "select eb from Engangsbeløp eb where eb.type = :engangsbeløpstype and eb.skyldner in :skyldnerIdentListe " +
            "and eb.kravhaver in :kravhaverIdentListe and eb.sak = :sak and eb.referanse = :referanse and eb.gjortUgyldigAvVedtaksid is null",
    )
    fun finnEngangsbeløp(
        engangsbeløpstype: String,
        skyldnerIdentListe: List<String>,
        kravhaverIdentListe: List<String>,
        sak: String,
        referanse: String,
    ): List<Engangsbeløp>

    @Query("select eb from Engangsbeløp eb where eb.sak = :sak and eb.gjortUgyldigAvVedtaksid is null")
    fun finnEngangsbeløpForSak(sak: String): List<Engangsbeløp>

    @Query(
        "select eb from Engangsbeløp eb where eb.type = :engangsbeløpstype and eb.skyldner = :skyldner and eb.kravhaver = :kravhaver " +
            "and eb.sak = :sak and eb.referanse = :referanse",
    )
    fun finnHistoriskeEngangsbeløp(engangsbeløpstype: String, skyldner: String, kravhaver: String, sak: String, referanse: String): List<Engangsbeløp>

    @Query(
        "update Engangsbeløp eb set eb.mottaker = :mottaker, eb.endretAv = :opprettetAv, eb.endretTidspunkt = CURRENT_TIMESTAMP " +
            "where eb.engangsbeløpsid = :engangsbeløpsid",
    )
    @Modifying
    fun endreMottakerForEngangsbeløp(engangsbeløpsid: Int, mottaker: String, opprettetAv: String)

    @Query(
        "update Engangsbeløp eb set eb.gyldigTil = :vedtakstidspunkt, eb.gjortUgyldigAvVedtaksid = :gjortUgyldigAvVedtaksid, " +
            "eb.endretAv = :endretAv, eb.endretTidspunkt = CURRENT_TIMESTAMP " +
            "where eb.engangsbeløpsid = :engangsbeløpsid",
    )
    @Modifying
    fun settEngangsbeløpSomUgyldig(engangsbeløpsid: Int, gjortUgyldigAvVedtaksid: Int, vedtakstidspunkt: LocalDateTime, endretAv: String)
}

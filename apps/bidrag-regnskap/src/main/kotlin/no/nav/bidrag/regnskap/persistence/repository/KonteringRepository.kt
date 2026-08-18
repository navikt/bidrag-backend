package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.transport.regnskap.avstemning.SumPrSak
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.time.LocalDateTime

interface KonteringRepository : JpaRepository<Kontering, Int> {

    fun findAllByOverføringstidspunktIsNull(): List<Kontering>

    fun findAllByOverføringsperiodeOrOverføringstidspunktIsNull(overforingsperiode: String): List<Kontering>

    @Query(
        value = "SELECT * FROM konteringer WHERE date(overforingstidspunkt) = ?1",
        nativeQuery = true,
    )
    fun hentAlleKonteringerForDato(dato: LocalDate): List<Kontering>

    @Query(
        value = "SELECT * FROM konteringer WHERE date(overforingstidspunkt) = ?1 AND overforingstidspunkt >= ?2 AND overforingstidspunkt < ?3",
        nativeQuery = true,
    )
    fun hentAlleKonteringerForDato(dato: LocalDate, fomTidspunkt: LocalDateTime, tomTidspunkt: LocalDateTime): List<Kontering>

    fun findAllByBehandlingsstatusOkTidspunktIsNullAndOverføringstidspunktIsNotNullAndSisteReferansekodeIsNotNull(): List<Kontering>

    fun findAllByBehandlingsstatusOkTidspunktIsNullAndOverføringstidspunktIsNotNullAndSisteReferansekodeIsIn(
        sisteReferansekoder: List<String>,
    ): List<Kontering>

    fun findAllByBehandlingsstatusOkTidspunktIsNull(): List<Kontering>

    fun findAllBySisteReferansekode(sisteReferansekode: String): List<Kontering>

    @Query(
        value = """
        SELECT new no.nav.bidrag.transport.regnskap.avstemning.SumPrSak(
            o.sakId, COUNT(op), SUM(op.beløp)
        )
        FROM oppdragsperioder op
        JOIN op.oppdrag o
        WHERE o.stønadType = :stonadType
        AND :overforingsperiode >= op.periodeFra
        AND (:overforingsperiode < op.periodeTil OR op.periodeTil IS NULL)
        GROUP BY o.sakId
    """,
    )
    fun hentSakSumForStønadOgPeriode(
        @Param("stonadType") stonadType: String,
        @Param("overforingsperiode") overforingsperiode: LocalDate,
    ): List<SumPrSak>
}

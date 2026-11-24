package no.nav.bidrag.arbeidsflyt.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "sak")
data class Sak(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,
    @Version
    @Column(name = "versjon")
    val versjon: Long = 0,
    @Column(name = "saksnummer")
    val saksnummer: String,
    @Column(name = "eierfogd")
    var eierfogd: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "flytter_fra_eierfogder")
    var flytterFraEierfogder: Set<String> = emptySet(),
    @Column(name = "opprettet_tidspunkt")
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "endret_tidspunkt")
    var endretTidspunkt: LocalDateTime = LocalDateTime.now(),
)

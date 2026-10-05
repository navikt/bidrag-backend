package no.nav.bidrag.arbeidsflyt.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.transport.sak.BidragssakDto
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "sak")
data class Sak(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,
    @Column(name = "saksnummer")
    val saksnummer: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "sak")
    var sak: BidragssakDto,
    @Column(name = "kategori")
    @Enumerated(EnumType.STRING)
    var kategori: Sakskategori,
    @Column(name = "eierfogd")
    var eierfogd: String,
    @Column(name = "saksstatus")
    @Enumerated(EnumType.STRING)
    var saksstatus: Bidragssakstatus,
    @Column(name = "arbeidsfordeling")
    @Enumerated(EnumType.STRING)
    var arbeidsfordeling: Arbeidsfordeling,
    @Column(name = "opprettet_dato")
    var opprettetDato: LocalDate,
    @Column(name = "avsluttet")
    var avsluttet: Boolean = false,
    @Column(name = "opprettet_tidspunkt")
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "endret_tidspunkt")
    var endretTidspunkt: LocalDateTime = LocalDateTime.now(),
)

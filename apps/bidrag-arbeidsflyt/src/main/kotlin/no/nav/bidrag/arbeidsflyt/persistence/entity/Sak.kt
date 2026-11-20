package no.nav.bidrag.arbeidsflyt.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
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
    @Column(name = "flytter_til_eierfogd")
    var flytterTilEierfogd: String? = null,
    @Column(name = "opprettet_tidspunkt")
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "endret_tidspunkt")
    var endretTidspunkt: LocalDateTime = LocalDateTime.now(),
)

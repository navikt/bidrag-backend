package no.nav.bidrag.regnskap.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity(name = "endre_mottaker")
@Table(name = "endre_mottaker")
data class EndreMottaker(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "vedtak_id")
    val vedtakId: Int,

    @Column(name = "saksnummer")
    val saksnummer: String,

    @Column(name = "barn_ident")
    val barnIdent: String,

    @Column(name = "ny_mottaker_ident")
    val nyMottakerIdent: String,

    @Column(name = "overfort_til_skatt_tidspunkt")
    var overførtTilSkattTidspunkt: LocalDateTime? = null,

    @Column(name = "godkjent_av_skatt_tidspunkt")
    var godkjentAvSkattTidspunkt: LocalDateTime? = null,

    @Column(name = "feilmelding_fra_skatt")
    var feilmeldingFraSkatt: String? = null,

    @Column(name = "opprettet_tidspunkt")
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
) {

    // Ikke inkluder identer (PII) i toString.
    override fun toString(): String = this::class.simpleName +
        "(id = $id, " +
        "vedtakId = $vedtakId, " +
        "saksnummer = $saksnummer, " +
        "overførtTilSkattTidspunkt = $overførtTilSkattTidspunkt, " +
        "godkjentAvSkattTidspunkt = $godkjentAvSkattTidspunkt, " +
        "opprettetTidspunkt = $opprettetTidspunkt)"
}

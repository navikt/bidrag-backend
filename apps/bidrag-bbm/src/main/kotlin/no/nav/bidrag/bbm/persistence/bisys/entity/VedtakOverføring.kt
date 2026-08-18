package no.nav.bidrag.bbm.persistence.bisys.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "t_vedtak_overforing")
@Suppress("unused")
open class VedtakOverføring(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Int? = null,
    @Column(name = "saksnr", nullable = false)
    val saksnr: String? = null,
    @Column(name = "sokn_id")
    val soknadId: Int? = null,
    @Column(name = "rolle_hist_id")
    val rolleHistorikkId: Int? = null,
    @Column(name = "vedtak_timestamp_bisys", nullable = false)
    val vedtakTimestampBisys: LocalDateTime? = null,
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    val status: VedtakOverføringStatus? = null,
    @Column(name = "notat", nullable = true)
    val notat: String? = null,
    @Column(name = "vedtak_id_bidrag_vedtak")
    val vedtakIdBidragVedtak: Int? = null,
    @Column(name = "opprettet_timestamp", nullable = false)
    val opprettetTimestamp: LocalDateTime? = null,
    @Column(name = "endret_timestamp")
    val endretTimestamp: LocalDateTime? = null,
    @Column(name = "vedtak_overfort_med_grunnlag")
    val vedtakOverfortMedGrunnlagTimestamp: LocalDateTime? = null,
)

enum class VedtakOverføringStatus {
    UBEHANDLET,
    FEILET,
    OVERFORT,
    UTEN_VEDTAK,
    UNDERSOKNAD,
    INNLESING_FEIL,
}

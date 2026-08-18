package no.nav.bidrag.sak.domain

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import no.nav.bidrag.domene.sak.Saksnummer
import java.time.OffsetDateTime

@Entity(name = "T_VEDTAK_OVERFORING")
class VedtakOverføring(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    val id: Int? = null,
    @Column(name = "saksnr", nullable = false)
    @Convert(converter = SaksnummerConverter::class)
    val saksnr: Saksnummer? = null,
    @Column(name = "sokn_id")
    val soknadId: Int? = null,
    @Column(name = "vedtak_id_bidrag_vedtak")
    val vedtakIdBidragVedtak: Int? = null,
    @Column(name = "vedtak_overfort_med_grunnlag")
    val vedtakOverfortMedGrunnlagTimestamp: OffsetDateTime?,
)

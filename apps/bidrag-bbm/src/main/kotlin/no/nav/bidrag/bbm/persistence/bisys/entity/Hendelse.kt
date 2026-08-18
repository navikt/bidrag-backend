package no.nav.bidrag.bbm.persistence.bisys.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "T_HENDELSE")
@Suppress("unused")
open class Hendelse(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HENDELSE_ID")
    open var hendelsesid: Long? = null,
    @Column(name = "SAKSNR")
    open var saksnummer: String,
    @Column(name = "HEND_TYPE")
    open var hendelsestype: String,
    @Column(name = "OPPRETTET_DATO")
    open var opprettetDato: LocalDateTime,
    @Column(name = "ENHET")
    open var enhet: String,
    @Column(name = "SOKN_TYPE")
    open var søknadstype: String? = null,
    @Column(name = "OPPRETTET_AV")
    open var opprettetAv: String? = null,
    @Column(name = "GR_KOMB_KODE")
    open var gruppeKombinasjonskode: String? = null,
    @Column(name = "BLANKETT_ID")
    open var blankettid: Long? = null,
    @Column(name = "SOKN_ID")
    open var søknadsid: Long? = null,
    @Column(name = "FRA_BBM")
    open var fraBbm: Char = '0',
    @Column(name = "SYS_OPPR_DATO")
    open var systemOpprettetDato: LocalDateTime,
)

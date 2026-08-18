package no.nav.bidrag.bbm.persistence.bisys.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "T_SOKNAD_LINJE")
@Suppress("unused")
open class Søknadslinje(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SOKN_LINJE_ID")
    open var søknadslinjeid: Long? = null,
    @Column(name = "SOKN_ID")
    open var søknadsid: Long,
    @Column(name = "ROLLE_ID")
    open var rolleid: Long,
    @Column(name = "INNBETALT_BELOP")
    open var innbetaltBeløp: BigDecimal? = null,
    @Column(name = "SOKN_STAT_KODE")
    open var søknadStatuskode: String,
    @Column(name = "STATUS_DATO")
    open var statusdato: LocalDate? = null,
    @Column(name = "GR_KOMB_KODE")
    open var gruppeKombinasjonskode: String,
    @Column(name = "SAKSNR")
    open var saksnummer: String,
    @Column(name = "ENGANGSBELOP_REFERANSE")
    open var engangsbeløpReferanse: String? = null,
)

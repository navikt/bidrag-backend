package no.nav.bidrag.bbm.persistence.bisys.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "T_SOKNAD")
@Suppress("unused")
open class Søknad(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SOKN_ID")
    open var søknadsid: Long? = null,
    @Column(name = "BLANKETT_ID")
    open var blankettid: Long,
    @Column(name = "SOKN_MOTT_DATO")
    open var søknadMottattDato: LocalDate,
    @Column(name = "SOKN_FOM_DATO")
    open var søknadFomDato: LocalDate? = null,
    @Column(name = "SOKN_GR_KODE")
    open var søknadsgruppekode: String,
    @Column(name = "BEHANDLER_ENHET")
    open var behandlerenhet: String? = null,
    @Column(name = "SAKSNR")
    open var saksnummer: String,
    @Column(name = "FTK_ST_BEH_OK")
    open var ftkStBehOk: Char = '0',
    @Column(name = "FTL_ST_SAMS_INNST")
    open var ftlStSamsInnst: Char = '0',
    @Column(name = "REF_SOKN_ID")
    open var referertSøknadsid: Long? = null,
    @Column(name = "GRUNNLAG_STATUS")
    open var grunnlagStatus: String = "IKKE_VURDERT",
    @Column(name = "BEHANDLING_ID")
    open var behandlingsid: String? = null,
    @Column(name = "LAGRET_I_BBM")
    open var lagretIBbm: Char = '0',
    @Column(name = "ref_vedtaksid")
    open var refVedtaksid: Int? = null,
)

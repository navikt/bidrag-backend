package no.nav.bidrag.sak.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import no.nav.bidrag.domene.enums.behandling.HendelseType
import no.nav.bidrag.domene.enums.behandling.SøknadGruppeKombinasjon
import no.nav.bidrag.sak.util.erVedtakstype
import java.time.LocalDateTime

@Entity(name = "T_HENDELSE")
open class Hendelse(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var hendelseId: Int? = null,
    @Column(name = "SAKSNR")
    open var saksnummer: String,
    @Column(name = "HEND_TYPE")
    @Convert(converter = HendelseTypeConverter::class)
    open var type: HendelseType?,
    @Column(name = "OPPRETTET_DATO")
    open var opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "sys_oppr_dato")
    open var systemOpprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "ENHET", columnDefinition = "CHAR(4)")
    open var enhet: String,
    @Column(name = "RESULTAT")
    open var resultat: String? = null,
    @Column(name = "OPPRETTET_AV")
    open var opprettetAv: String = "VL",
    @Column(name = "GR_KOMB_KODE")
    open var grKombKode: String? = null,
    /** BBM = BidragsBeregningsModulen
     *
     * BBM er et eldre mainframe-system (CICS/IBM) som utfører selve
     * beregningen av bidragsbeløp. Det er et separat system fra Bisys,
     * og Bisys kaller det via tjenestekall.
     *
     * fraBbm = true betyr at vedtaket ble fattet på grunnlag av en beregning utført i BBM —
     * altså at saksbehandler åpnet beregningsbildet, kjørte en
     * beregning der, og fattete vedtaket basert på det resultatet.
     * */
    @Convert(converter = BooleanConverter::class)
    open var fraBbm: Boolean = false,
    @ManyToOne(fetch = FetchType.EAGER, cascade = [CascadeType.PERSIST])
    @JoinColumn(name = "sokn_id")
    val søknad: Søknad?,
) {
    val erVedtak: Boolean
        get() {
            if (fraBbm && !resultat.isNullOrBlank()) return false
            return type.erVedtakstype()
        }

    val erBidrag: Boolean
        get() {
            val grKombKode =
                grKombKode
                    ?: return false

            val gruppekombinasjon = SøknadGruppeKombinasjon.fraKode(grKombKode)
            return when (gruppekombinasjon) {
                SøknadGruppeKombinasjon.BIDRAG,
                SøknadGruppeKombinasjon.BIDRAG_INNKREVING,
                SøknadGruppeKombinasjon.BIDRAG_TILLEGGSBIDRAG,
                SøknadGruppeKombinasjon.BIDRAG_TILLEGGSBIDRAG_INNKREVING,
                SøknadGruppeKombinasjon.BIDRAG_18_ÅR,
                SøknadGruppeKombinasjon.BIDRAG_18_ÅR_INNKREVING,
                SøknadGruppeKombinasjon.BIDRAG_18_ÅR_TILLEGGSBIDRAG,
                SøknadGruppeKombinasjon.BIDRAG_18_AAR_TILLEGGSBIDRAG_INNKREVING,
                SøknadGruppeKombinasjon.TILLEGGSBIDRAG,
                SøknadGruppeKombinasjon.TILLEGGSBIDRAG_INNKREVING,
                -> true

                else -> false
            }
        }

    val erForskudd: Boolean
        get() {
            val grKombKode =
                grKombKode
                    ?: return false

            val gruppekombinasjon = SøknadGruppeKombinasjon.fraKode(grKombKode)

            return gruppekombinasjon == SøknadGruppeKombinasjon.FORSKUDD
        }

    val erSærbidrag: Boolean
        get() {
            val grKombKode =
                grKombKode
                    ?: return false

            val gruppekombinasjon = SøknadGruppeKombinasjon.fraKode(grKombKode)

            return gruppekombinasjon == SøknadGruppeKombinasjon.SÆRBIDRAG ||
                gruppekombinasjon == SøknadGruppeKombinasjon.SÆRBIDRAG_INNKREVING // todo: SARTILSKUDD i Bisys (samme kode "ST")
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Hendelse

        if (hendelseId != other.hendelseId) return false
        if (saksnummer != other.saksnummer) return false
        if (type != other.type) return false
        if (opprettetTidspunkt != other.opprettetTidspunkt) return false
        if (enhet != other.enhet) return false
        if (resultat != other.resultat) return false
        if (opprettetAv != other.opprettetAv) return false
        if (grKombKode != other.grKombKode) return false
        if (fraBbm != other.fraBbm) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hendelseId ?: 0
        result = 31 * result + saksnummer.hashCode()
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + opprettetTidspunkt.hashCode()
        result = 31 * result + enhet.hashCode()
        result = 31 * result + (resultat?.hashCode() ?: 0)
        result = 31 * result + opprettetAv.hashCode()
        result = 31 * result + (grKombKode?.hashCode() ?: 0)
        result = 31 * result + fraBbm.hashCode()
        return result
    }

    override fun toString(): String = "Hendelse(hendelseId=$hendelseId, saksnummer='$saksnummer', " +
        "type=$type, opprettetTidspunkt=$opprettetTidspunkt, " +
        "enhet='$enhet', resultat=$resultat, opprettetAv='$opprettetAv', " +
        "grKombKode=$grKombKode, fraBbm=$fraBbm)"
}

package no.nav.bidrag.belopshistorikk.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.belopshistorikk.request.OpprettStønadsperiodeRequestDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadPeriodeDto
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties

@Entity
data class Periode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "periodeid")
    val periodeid: Int? = null,
    @Column(nullable = false, name = "fom")
    val fom: LocalDate = LocalDate.now(),
    @Column(nullable = true, name = "til")
    val til: LocalDate? = null,
    @ManyToOne
    @JoinColumn(name = "stønadsid")
    val stønad: Stønad = Stønad(),
    @Column(name = "vedtaksid")
    val vedtaksid: Int = 0,
    @Column(nullable = false, name = "gyldig_fra")
    val gyldigFra: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = true, name = "gyldig_til")
    val gyldigTil: LocalDateTime? = null,
    @Column(nullable = true, name = "periode_gjort_ugyldig_av_vedtaksid")
    val periodeGjortUgyldigAvVedtaksid: Int? = null,
    @Column(nullable = true, name = "beløp")
    val beløp: BigDecimal? = null,
    @Column(nullable = true, name = "valutakode")
    val valutakode: String? = null,
    @Column(nullable = false, name = "resultatkode")
    val resultatkode: String = "",
)

fun OpprettStønadsperiodeRequestDto.toPeriodeEntity(eksisterendeStønad: Stønad) = with(::Periode) {
    val propertiesByName = OpprettStønadsperiodeRequestDto::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                Periode::stønad.name -> eksisterendeStønad
                Periode::fom.name -> periode.toDatoperiode().fom
                Periode::til.name -> periode.toDatoperiode().til
                else -> propertiesByName[parameter.name]?.get(this@toPeriodeEntity)
            }
        },
    )
}

fun Periode.toStønadPeriodeDto() = with(::StønadPeriodeDto) {
    val propertiesByName = Periode::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                StønadPeriodeDto::stønadsid.name -> stønad.stønadsid
                StønadPeriodeDto::periode.name -> ÅrMånedsperiode(fom, til)
                else -> propertiesByName[parameter.name]?.get(this@toStønadPeriodeDto)
            }
        },
    )
}

package no.nav.bidrag.belopshistorikk.bo

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.belopshistorikk.persistence.entity.Periode
import no.nav.bidrag.belopshistorikk.persistence.entity.Stønad
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.belopshistorikk.request.OpprettStønadsperiodeRequestDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadPeriodeDto
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.reflect.full.memberProperties

data class PeriodeBo(
    @param:Schema(description = "Periodeid")
    val periodeid: Int? = null,
    @param:Schema(description = "Periode med fra-og-med-dato og til-dato med format ÅÅÅÅ-MM")
    val periode: ÅrMånedsperiode = ÅrMånedsperiode(YearMonth.now(), YearMonth.now()),
    @param:Schema(description = "Stønadsid")
    val stønadsid: Int? = null,
    @param:Schema(description = "Vedtaksid")
    val vedtaksid: Int = 0,
    @param:Schema(description = "Perioden er gyldig fra angitt tidspunkt (vedtakstidspunkt)")
    val gyldigFra: LocalDateTime = LocalDateTime.now(),
    @param:Schema(description = "Angir tidspunkt perioden eventuelt er ugyldig fra (tidspunkt for vedtak med periode som erstattet denne)")
    val gyldigTil: LocalDateTime? = null,
    @param:Schema(description = "Periode-gjort-ugyldig-av-vedtak-id")
    val periodeGjortUgyldigAvVedtaksid: Int? = 0,
    @param:Schema(description = "Beregnet stønadsbeløp")
    val beløp: BigDecimal? = BigDecimal.ZERO,
    @param:Schema(description = "Valutakoden tilhørende stønadsbeløpet")
    val valutakode: String? = "NOK",
    @param:Schema(description = "Resultatkode for stønaden")
    val resultatkode: String = "",
)

fun OpprettStønadsperiodeRequestDto.toPeriodeBo() = with(::PeriodeBo) {
    val propertiesByName = OpprettStønadsperiodeRequestDto::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                PeriodeBo::periodeid.name -> null
                PeriodeBo::stønadsid.name -> null
                else -> propertiesByName[parameter.name]?.get(this@toPeriodeBo)
            }
        },
    )
}

fun StønadPeriodeDto.toPeriodeBo() = with(::PeriodeBo) {
    val propertiesByName = StønadPeriodeDto::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                PeriodeBo::stønadsid.name -> stønadsid
                PeriodeBo::periodeid.name -> 0
                else -> propertiesByName[parameter.name]?.get(this@toPeriodeBo)
            }
        },
    )
}

fun PeriodeBo.toPeriodeEntity(eksisterendeStønad: Stønad) = with(::Periode) {
    val propertiesByName = PeriodeBo::class.memberProperties.associateBy { it.name }
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

fun PeriodeBo.toJustertPeriodeEntity(eksisterendeStønad: Stønad, vedtakstidspunkt: LocalDateTime) = with(::Periode) {
    val propertiesByName = PeriodeBo::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                Periode::stønad.name -> eksisterendeStønad
                Periode::fom.name -> periode.toDatoperiode().fom
                Periode::til.name -> periode.toDatoperiode().til
                Periode::gyldigFra.name -> vedtakstidspunkt
                else -> propertiesByName[parameter.name]?.get(this@toJustertPeriodeEntity)
            }
        },
    )
}

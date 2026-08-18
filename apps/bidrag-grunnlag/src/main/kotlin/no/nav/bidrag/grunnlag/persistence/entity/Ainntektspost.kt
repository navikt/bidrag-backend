package no.nav.bidrag.grunnlag.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import no.nav.bidrag.grunnlag.bo.AinntektspostBo
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.reflect.full.memberProperties

@Entity
data class Ainntektspost(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inntektspost_id")
    val inntektspostId: Int = 0,

    @Column(nullable = false, name = "inntekt_id")
    val inntektId: Int = 0,

    @Column(nullable = true, name = "utbetalingsperiode")
    val utbetalingsperiode: String? = "",

    @Column(nullable = true, name = "opptjeningsperiode_fra")
    val opptjeningsperiodeFra: LocalDate? = LocalDate.now(),

    @Column(nullable = true, name = "opptjeningsperiode_til")
    val opptjeningsperiodeTil: LocalDate? = LocalDate.now(),

    @Column(nullable = true, name = "opplysningspliktig_id")
    val opplysningspliktigId: String? = "",

    @Column(nullable = true, name = "virksomhet_id")
    val virksomhetId: String? = "",

    @Column(nullable = false, name = "inntekt_type")
    val inntektType: String = "",

    @Column(nullable = true, name = "fordel_type")
    val fordelType: String? = "",

    @Column(nullable = true, name = "beskrivelse")
    val beskrivelse: String? = "",

    @Column(nullable = false, name = "belop")
    val belop: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = true, name = "etterbetalingsperiode_fra")
    val etterbetalingsperiodeFra: LocalDate? = null,

    @Column(nullable = true, name = "etterbetalingsperiode_til")
    val etterbetalingsperiodeTil: LocalDate? = null,
)

fun Ainntektspost.toAinntektspostBo() = with(::AinntektspostBo) {
    val propertiesByName = Ainntektspost::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                else -> propertiesByName[parameter.name]?.get(this@toAinntektspostBo)
            }
        },
    )
}

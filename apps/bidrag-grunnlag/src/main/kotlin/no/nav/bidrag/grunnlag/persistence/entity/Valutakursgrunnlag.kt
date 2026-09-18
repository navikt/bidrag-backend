package no.nav.bidrag.grunnlag.persistence.entity

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.grunnlag.bo.ValutakursgrunnlagBo
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.collections.get
import kotlin.reflect.full.memberProperties

@Entity
data class Valutakursgrunnlag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "valutakursgrunnlag_id")
    val valutakursgrunnlagId: Int = 0,

    @Column(nullable = false, name = "aktiv")
    val aktiv: Boolean = true,

    @Column(nullable = false, name = "bruk_fra")
    val brukFra: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = true, name = "bruk_til")
    val brukTil: LocalDateTime? = null,

    @Column(nullable = false, name = "hentet_tidspunkt")
    val hentetTidspunkt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = true, name = "kurs")
    val kurs: BigDecimal? = null,

    @Column(nullable = true, name = "multiplikator")
    val multiplikator: Int? = null,

    @Column(nullable = false, name = "basisvaluta")
    @Enumerated(value = EnumType.STRING)
    val basisvaluta: Valutakode,

    @Column(nullable = false, name = "kvoteringsvaluta")
    @Enumerated(value = EnumType.STRING)
    val kvoteringsvaluta: Valutakode = Valutakode.NOK,

    @Column(nullable = false, name = "feilet_henting")
    val feiletHenting: Boolean = false,
)

fun Valutakursgrunnlag.toValutakursgrunnlagBo() = with(::ValutakursgrunnlagBo) {
    val propertiesByName = Valutakursgrunnlag::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                else -> propertiesByName[parameter.name]?.get(this@toValutakursgrunnlagBo)
            }
        },
    )
}

package no.nav.bidrag.grunnlag.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import no.nav.bidrag.grunnlag.bo.SkattegrunnlagspostBo
import java.math.BigDecimal
import kotlin.reflect.full.memberProperties

@Entity
data class Skattegrunnlagspost(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skattegrunnlagspost_id")
    val skattegrunnlagspostId: Int = 0,

    @Column(nullable = false, name = "skattegrunnlag_id")
    val skattegrunnlagId: Int = 0,

    @Column(nullable = false, name = "skattegrunnlag_type")
    val skattegrunnlagType: String = "",

    @Column(nullable = false, name = "inntekt_type")
    val inntektType: String = "",

    @Column(nullable = false, name = "belop")
    val belop: BigDecimal = BigDecimal.ZERO,
)

fun Skattegrunnlagspost.toSkattegrunnlagspostBo() = with(::SkattegrunnlagspostBo) {
    val propertiesByName = Skattegrunnlagspost::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                else -> propertiesByName[parameter.name]?.get(this@toSkattegrunnlagspostBo)
            }
        },
    )
}

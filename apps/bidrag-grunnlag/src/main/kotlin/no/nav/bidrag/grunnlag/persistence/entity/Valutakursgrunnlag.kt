package no.nav.bidrag.grunnlag.persistence.entity

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.grunnlag.bo.ValutakursgrunnlagBo
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.collections.get
import kotlin.reflect.full.memberProperties

@Entity
@Table(name = "valutakursgrunnlag")
data class Valutakursgrunnlag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "valutakursgrunnlag_id")
    val valutakursgrunnlagId: Int = 0,

    @Column(nullable = false, name = "aktiv")
    val aktiv: Boolean = true,

    @Column(nullable = false, name = "bruk_fra")
    val brukFra: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false, name = "bruk_til")
    val brukTil: LocalDateTime? = null,

    @Column(nullable = false, name = "hentet_tidspunkt")
    val hentetTidspunkt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = true, name = "kurs", precision = 38, scale = 16)
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

    @Column(name = "kilde", length = 20)
    @Enumerated(EnumType.STRING)
    val kilde: ValutakursgrunnlagKilde? = null,

    @Column(name = "observasjonsdato")
    val observasjonsdato: LocalDate? = null,
) {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status", length = 10)
    var status: ValutakursgrunnlagStatus = if (feiletHenting) ValutakursgrunnlagStatus.FEILET else ValutakursgrunnlagStatus.HENTET

    @Column(name = "oppdatert_tidspunkt", nullable = false)
    var oppdatertTidspunkt: LocalDateTime = LocalDateTime.now()

    @PreUpdate
    fun oppdaterTidspunkt() {
        oppdatertTidspunkt = LocalDateTime.now()
    }
}

enum class ValutakursgrunnlagStatus {
    FEILET,
    HENTET,
    OVERSTYRT,
}

enum class ValutakursgrunnlagKilde {
    ECB,
    NORGES_BANK,
    MANUELL,
}

fun Valutakursgrunnlag.toValutakursgrunnlagBo() = with(::ValutakursgrunnlagBo) {
    val propertiesByName = Valutakursgrunnlag::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.mapNotNull { parameter ->
            val name = parameter.name ?: return@mapNotNull null
            propertiesByName[name]?.let { parameter to it.get(this@toValutakursgrunnlagBo) }
        }.toMap(),
    )
}

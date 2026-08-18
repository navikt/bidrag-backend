package no.nav.bidrag.belopshistorikk.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.behandling.belopshistorikk.request.OpprettEngangsbeløpRequestDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.EngangsbeløpDto
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties

@Entity
data class Engangsbeløp(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "engangsbeløpsid")
    val engangsbeløpsid: Int? = null,
    @Column(nullable = false, name = "type")
    val type: String = "",
    @Column(nullable = false, name = "sak")
    val sak: String = "",
    @Column(nullable = false, name = "skyldner")
    val skyldner: String = "",
    @Column(nullable = false, name = "kravhaver")
    val kravhaver: String = "",
    @Column(nullable = false, name = "mottaker")
    val mottaker: String = "",
    @Column(name = "vedtaksid")
    val vedtaksid: Int = 0,
    @Column(nullable = false, name = "gyldig_fra")
    val gyldigFra: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = true, name = "gyldig_til")
    val gyldigTil: LocalDateTime? = null,
    @Column(nullable = true, name = "gjort_ugyldig_av_vedtaksid")
    val gjortUgyldigAvVedtaksid: Int? = null,
    @Column(nullable = true, name = "beløp")
    val beløp: BigDecimal? = BigDecimal.ZERO,
    @Column(nullable = true, name = "betalt_beløp")
    val betaltBeløp: BigDecimal? = null,
    @Column(nullable = true, name = "valutakode")
    val valutakode: String? = "",
    @Column(nullable = false, name = "resultatkode")
    val resultatkode: String = "",
    @Column(nullable = false, name = "innkreving")
    val innkreving: String = "",
    @Column(nullable = false, name = "referanse")
    val referanse: String = "",
    @Column(nullable = false, name = "opprettet_av")
    val opprettetAv: String = "",
    @Column(nullable = false, name = "opprettet_tidspunkt")
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = true, name = "endret_av")
    val endretAv: String? = null,
    @Column(nullable = true, name = "endret_tidspunkt")
    val endretTidspunkt: LocalDateTime? = null,
)

fun OpprettEngangsbeløpRequestDto.toEngangsbeløpEntity() = with(::Engangsbeløp) {
    val propertiesByName = OpprettEngangsbeløpRequestDto::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                Engangsbeløp::engangsbeløpsid.name -> null
                Engangsbeløp::type.name -> type.toString()
                Engangsbeløp::sak.name -> sak.toString()
                Engangsbeløp::skyldner.name -> skyldner.verdi
                Engangsbeløp::kravhaver.name -> kravhaver.verdi
                Engangsbeløp::mottaker.name -> mottaker.verdi
                Engangsbeløp::innkreving.name -> innkreving.toString()
                Engangsbeløp::opprettetTidspunkt.name -> LocalDateTime.now()
                else -> propertiesByName[parameter.name]?.get(this@toEngangsbeløpEntity)
            }
        },
    )
}

fun Engangsbeløp.toEngangsbeløpDto() = with(::EngangsbeløpDto) {
    val propertiesByName = Engangsbeløp::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                EngangsbeløpDto::type.name -> Engangsbeløptype.valueOf(type)
                EngangsbeløpDto::sak.name -> Saksnummer(sak)
                EngangsbeløpDto::skyldner.name -> Personident(skyldner)
                EngangsbeløpDto::kravhaver.name -> Personident(kravhaver)
                EngangsbeløpDto::mottaker.name -> Personident(mottaker)
                EngangsbeløpDto::innkreving.name -> Innkrevingstype.valueOf(innkreving)
                else -> propertiesByName[parameter.name]?.get(this@toEngangsbeløpDto)
            }
        },
    )
}

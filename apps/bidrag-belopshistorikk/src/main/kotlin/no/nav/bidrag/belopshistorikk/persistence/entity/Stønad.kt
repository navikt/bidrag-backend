package no.nav.bidrag.belopshistorikk.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.behandling.belopshistorikk.request.OpprettStønadRequestDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadPeriodeDto
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties

@Entity
data class Stønad(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stønadsid")
    val stønadsid: Int? = null,
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
    @Column(nullable = true, name = "neste_indeksreguleringsår")
    val nesteIndeksreguleringsår: Int? = 0,
    @Column(nullable = false, name = "innkreving")
    val innkreving: String = "",
    @Column(nullable = false, name = "opprettet_av")
    val opprettetAv: String = "",
    @Column(nullable = false, name = "opprettet_tidspunkt")
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = true, name = "endret_av")
    val endretAv: String? = null,
    @Column(nullable = true, name = "endret_tidspunkt")
    val endretTidspunkt: LocalDateTime? = null,
)

fun OpprettStønadRequestDto.toStønadEntity() = with(::Stønad) {
    val propertiesByName = OpprettStønadRequestDto::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                Stønad::stønadsid.name -> null
                Stønad::type.name -> type.toString()
                Stønad::sak.name -> sak.toString()
                Stønad::skyldner.name -> skyldner.verdi
                Stønad::kravhaver.name -> kravhaver.verdi
                Stønad::mottaker.name -> mottaker.verdi
                Stønad::innkreving.name -> innkreving.toString()
                Stønad::opprettetTidspunkt.name -> LocalDateTime.now()
                Stønad::endretAv.name -> opprettetAv
                else -> propertiesByName[parameter.name]?.get(this@toStønadEntity)
            }
        },
    )
}

fun Stønad.toStønadDto(stønadPeriodeDtoListe: List<StønadPeriodeDto>) = with(::StønadDto) {
    val propertiesByName = Stønad::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                StønadDto::type.name -> Stønadstype.valueOf(type)
                StønadDto::sak.name -> Saksnummer(sak)
                StønadDto::skyldner.name -> Personident(skyldner)
                StønadDto::kravhaver.name -> Personident(kravhaver)
                StønadDto::mottaker.name -> Personident(mottaker)
                StønadDto::innkreving.name -> Innkrevingstype.valueOf(innkreving)
                StønadDto::periodeListe.name -> stønadPeriodeDtoListe
                else -> propertiesByName[parameter.name]?.get(this@toStønadDto)
            }
        },
    )
}

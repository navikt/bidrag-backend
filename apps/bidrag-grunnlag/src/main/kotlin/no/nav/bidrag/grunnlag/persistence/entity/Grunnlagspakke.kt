package no.nav.bidrag.grunnlag.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.service.organisasjon.SaksbehandlernavnProvider
import no.nav.bidrag.domene.util.trimToNull
import no.nav.bidrag.grunnlag.bo.GrunnlagspakkeBo
import no.nav.bidrag.grunnlag.exception.custom.manglerOpprettetAv
import no.nav.bidrag.transport.behandling.grunnlag.request.OpprettGrunnlagspakkeRequestDto
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties

@Entity
data class Grunnlagspakke(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grunnlagspakke_id")
    val grunnlagspakkeId: Int = 0,

    val opprettetAv: String,
    val opprettetAvNavn: String? = null,
    val kildeapplikasjon: String,

    @Column(nullable = false, name = "opprettet_timestamp")
    val opprettetTimestamp: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = true, name = "endret_timestamp")
    val endretTimestamp: LocalDateTime? = null,

    @Column(nullable = true, name = "gyldig_til")
    val gyldigTil: LocalDate? = null,

    @Column(nullable = true, name = "formaal")
    val formaal: String? = null,
)

fun Grunnlagspakke.toGrunnlagspakkeBo() = with(::GrunnlagspakkeBo) {
    val propertiesByName = Grunnlagspakke::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                else -> propertiesByName[parameter.name]?.get(this@toGrunnlagspakkeBo)
            }
        },
    )
}

fun OpprettGrunnlagspakkeRequestDto.toGrunnlagspakkeEntity() = with(::Grunnlagspakke) {
    val propertiesByName = OpprettGrunnlagspakkeRequestDto::class.memberProperties.associateBy { it.name }
    val opprettetAvIdent = opprettetAv.trimToNull() ?: TokenUtils.hentSaksbehandlerIdent() ?: manglerOpprettetAv()
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                Grunnlagspakke::opprettetAv.name -> opprettetAvIdent
                Grunnlagspakke::opprettetAvNavn.name -> SaksbehandlernavnProvider.hentSaksbehandlernavn(opprettetAvIdent)
                Grunnlagspakke::kildeapplikasjon.name -> TokenUtils.hentApplikasjonsnavn() ?: "UKJENT"
                Grunnlagspakke::grunnlagspakkeId.name -> 0
                Grunnlagspakke::opprettetTimestamp.name -> LocalDateTime.now()
                Grunnlagspakke::formaal.name -> formaal.toString()
                else -> propertiesByName[parameter.name]?.get(this@toGrunnlagspakkeEntity)
            }
        },
    )
}

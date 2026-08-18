package no.nav.bidrag.grunnlag.bo

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.grunnlag.persistence.entity.RelatertPerson
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties

data class RelatertPersonBo(

    @Schema(description = "Grunnlagspakke-id")
    val grunnlagspakkeId: Int = 0,

    @Schema(description = "Personid til BM eller BP")
    var partPersonId: String?,

    @Schema(description = "Identen til den relaterte personen. Vil være barn eller husstandsmedlem")
    var relatertPersonPersonId: String?,

    @Schema(description = "Navn på den relaterte personen, format <Fornavn, mellomnavn, Etternavn")
    var navn: String?,

    @Schema(description = "Den relaterte personens fødselsdag")
    var fodselsdato: LocalDate?,

    @Schema(description = "Angir om den relaterte personen er barn av BM eller BM")
    var erBarnAvBmBp: Boolean,

    @Schema(
        description = "Den relaterte personen bor i husstanden fra- og med måned. Hvis periodeFra og periodeTil == null så er personen barn som " +
            "ikke bor sammen med BM/BP",
    )
    val husstandsmedlemPeriodeFra: LocalDate?,

    @Schema(description = "Den relaterte personen bor i husstanden til- og med måned")
    val husstandsmedlemPeriodeTil: LocalDate?,

    @Schema(description = "Angir om en sivilstand er aktiv")
    val aktiv: Boolean = true,

    @Schema(description = "Tidspunkt sivilstanden taes i bruk")
    val brukFra: LocalDateTime = LocalDateTime.now(),

    @Schema(description = "Tidspunkt sivilstanden ikke lenger er aktiv. Null betyr at sivilstanden er aktiv")
    val brukTil: LocalDateTime? = null,

    @Schema(description = "Opprettet tidspunkt")
    val hentetTidspunkt: LocalDateTime,
)

fun RelatertPersonBo.toRelatertPersonEntity() = with(::RelatertPerson) {
    val propertiesByName = RelatertPersonBo::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                RelatertPerson::relatertPersonId.name -> 0
                else -> propertiesByName[parameter.name]?.get(this@toRelatertPersonEntity)
            }
        },
    )
}

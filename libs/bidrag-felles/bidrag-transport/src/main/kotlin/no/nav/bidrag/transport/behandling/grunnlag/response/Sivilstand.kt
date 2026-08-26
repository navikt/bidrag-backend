package no.nav.bidrag.transport.behandling.grunnlag.response

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.person.SivilstandskodePDL
import java.time.LocalDate
import java.time.LocalDateTime

data class SivilstandDto(
    @Schema(description = "Id til personen sivilstanden er rapportert for")
    val personId: String?,
    @Schema(description = "Sivilstand gjelder fra- og med måned")
    val periodeFra: LocalDate?,
    @Schema(description = "Sivilstand gjelder til- og med måned")
    val periodeTil: LocalDate?,
    @Schema(description = "Personens sivilstand")
    val sivilstand: SivilstandskodePDL,
    @Schema(description = "Angir om en grunnlagsopplysning er aktiv")
    val aktiv: Boolean,
    @Schema(description = "Tidspunkt grunnlaget tas i bruk")
    val brukFra: LocalDateTime,
    @Schema(description = "Tidspunkt grunnlaget ikke lenger er aktivt. Null betyr at grunnlaget er aktivt")
    val brukTil: LocalDateTime?,
    @Schema(description = "Hentet tidspunkt")
    val hentetTidspunkt: LocalDateTime,
)

data class SivilstandGrunnlagDto(
    @Schema(description = "Id til personen sivilstanden er rapportert for")
    val personId: String?,
    @Schema(description = "Type sivilstand fra PDL")
    val type: SivilstandskodePDL?,
    @Schema(description = "Dato sivilstanden er gyldig fra")
    val gyldigFom: LocalDate?,
    @Schema(description = "Personid som kun er satt om personen er ektefelle eller separert ektefelle")
    val relatertVedSivilstand: String? = null,
    @Schema(description = "Dato NAV tidligst har fått bekreftet sivilstanden")
    val bekreftelsesdato: LocalDate?,
    @Schema(description = "Master for opplysningen om sivilstand (FREG eller PDL)")
    val master: String?,
    @Schema(description = "Tidspunktet sivilstanden er registrert")
    val registrert: LocalDateTime?,
    @Schema(description = "Angir om sivilstanden er historisk (true) eller aktiv (false)")
    val historisk: Boolean?,
)

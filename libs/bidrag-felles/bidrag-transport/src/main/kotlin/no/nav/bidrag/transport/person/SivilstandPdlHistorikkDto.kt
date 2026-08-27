package no.nav.bidrag.transport.person

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.person.SivilstandskodePDL
import java.time.LocalDate
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SivilstandPdlHistorikkDto(
    @Schema(description = "Liste over alle hentede forekomster av sivilstand fra bidrag-person")
    val sivilstandPdlDto: List<SivilstandPdlDto>,
)

data class SivilstandPdlDto(
    val type: SivilstandskodePDL?,
    val gyldigFom: LocalDate?,
    // Personid som kun er satt om personen er ektefelle eller separert ektefelle
    val relatertVedSivilstand: String? = null,
    val bekreftelsesdato: LocalDate?,
    val master: String?,
    val registrert: LocalDateTime?,
    val historisk: Boolean?,
)

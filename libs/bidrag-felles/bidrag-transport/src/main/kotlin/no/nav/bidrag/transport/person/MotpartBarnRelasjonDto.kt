package no.nav.bidrag.transport.person

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.person.Familierelasjon

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MotpartBarnRelasjonDto(
    @Schema(description = "Identen til personen")
    val person: PersonDto,
    @Schema(description = "Familieenheter til personen")
    val personensMotpartBarnRelasjon: List<MotpartBarnRelasjon> = emptyList(),
)

data class MotpartBarnRelasjon(
    val forelderrolleMotpart: Familierelasjon,
    val motpart: PersonDto?,
    val fellesBarn: List<PersonDto> = emptyList(),
)

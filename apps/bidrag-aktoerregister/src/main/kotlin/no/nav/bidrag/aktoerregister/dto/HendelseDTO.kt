package no.nav.bidrag.aktoerregister.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "En hendelse signaliserer at enten adresse eller kontonummer for en aktør er oppdatert. Hendelsen inneholder ikke selve oppdateringen.",
)
data class HendelseDTO(

    @Schema(description = "Hendelsens sekvensnummer. Sekvensnummeret vil alltid øke i nyere hendelser.")
    val sekvensnummer: Int = 0,

    @Schema(description = "Aktøren som er oppdatert.")
    val aktoerId: AktoerIdDTO,
)

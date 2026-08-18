package no.nav.bidrag.aktoerregister.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.aktoerregister.dto.enumer.Identtype

data class AktoerIdDTO(
    @param:Schema(
        description = "Identen for aktøren. " +
            "For personer vil dette være FNR eller DNR. " +
            "Ellers benyttes aktørnummer på elleve siffer hvor første siffer er 8 eller 9.",
    )
    val aktoerId: String,

    @param:Schema(
        description = "Angir hvilken type ident som er angitt. " +
            "For personer vil dette være FNR eller DNR, som angis med PERSONNUMMER. " +
            "Utover dette benyttes AKTOERNUMMER.",
    )
    val identtype: Identtype,
)

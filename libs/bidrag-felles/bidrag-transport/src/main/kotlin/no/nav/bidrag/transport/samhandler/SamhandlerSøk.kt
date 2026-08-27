package no.nav.bidrag.transport.samhandler

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Søkbare felter for opphenting av samhandlere.")
data class SamhandlerSøk(
    val ident: String? = null,
    val offentligId: String? = null,
    val navn: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val norskkontonr: String? = null,
    val iban: String? = null,
    val swift: String? = null,
    val banknavn: String? = null,
    val banklandkode: String? = null,
    val bankcode: String? = null,
)

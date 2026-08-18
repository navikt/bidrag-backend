package no.nav.bidrag.tilgangskontroll.model.graph

import com.fasterxml.jackson.annotation.JsonProperty

data class Brukerinformasjon(
    @param:JsonProperty("displayName")
    val fulltNavn: String?,
    @param:JsonProperty("givenName")
    val fornavnMellomnavn: String?,
    @param:JsonProperty("jobTitle")
    val jobbtittel: String?,
    @param:JsonProperty("mail")
    val epost: String?,
    @param:JsonProperty("officeLocation")
    val avdeling: String?,
    @param:JsonProperty("surname")
    val etternavn: String?,
    @param:JsonProperty("id")
    val id: String?,
    @param:JsonProperty("onPremisesSamAccountName")
    val onPremisesSamAccountName: String?,
)

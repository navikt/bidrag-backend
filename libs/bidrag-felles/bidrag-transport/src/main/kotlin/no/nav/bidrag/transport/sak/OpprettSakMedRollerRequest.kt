package no.nav.bidrag.transport.sak

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Konvensjon
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.domene.land.Landkode
import java.time.LocalDate

data class OpprettSakMedRollerRequest(
    val kategori: Sakskategori,
    val arbeidsfordeling: Arbeidsfordeling,
    @field:Valid
    @field:NotEmpty
    val roller: Set<SaksrolleRequest>,
    val levdeAdskilt: Boolean?,
    val konvensjon: Konvensjon?,
    val konvensjonsdato: LocalDate?,
    val ffuReferansenr: String?,
    val land: Landkode?,
)

data class OppdaterSaksrollerRequest(
    @field:Valid
    @field:NotEmpty
    val roller: Set<SaksrolleRequest>,
)

data class SaksrolleRequest(
    @field:JsonProperty("fodselsnummer")
    val fødselsnummer: Personident?,
    val type: Rolletype,
    @field:Valid
    val reellMottaker: ReellMottakerRequest?,
)

data class ReellMottakerRequest(
    val ident: ReellMottaker,
    val type: ReellMottakerType,
    @param:JsonProperty(required = true)
    @param:JsonSetter(nulls = Nulls.FAIL)
    val verge: Boolean,
)

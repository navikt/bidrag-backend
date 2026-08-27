package no.nav.bidrag.transport.sak

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Konvensjon
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.land.Landkode
import no.nav.bidrag.domene.sak.Saksnummer
import java.time.LocalDate

data class OppdaterRollerISakRequest(
    @field:Schema(description = "Saksnummeret til saken som rollene skal oppdateres for.")
    val saksnummer: Saksnummer,
    @field:Schema(description = "Nye eller oppdaterte roller som skal legges til eller endres i saken.")
    val roller: Set<RolleDto> = setOf(),
)

data class OppdaterSakRequest(
    val saksnummer: Saksnummer,
    val status: Bidragssakstatus? = null,
    val ansatt: Boolean? = null,
    val inhabilitet: Boolean? = null,
    val levdeAdskilt: Boolean? = null,
    val sanertDato: LocalDate? = null,
    val arbeidsfordeling: Arbeidsfordeling? = null,
    val kategorikode: Sakskategori? = null,
    val landkode: Landkode? = null,
    val konvensjonskode: Konvensjon? = null,
    val konvensjonsdato: LocalDate? = null,
    val ffuReferansenr: String? = null,
    val roller: Set<RolleDto> = setOf(),
)

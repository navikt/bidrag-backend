package no.nav.bidrag.transport.sak

import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Konvensjon
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.land.Landkode
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import java.time.LocalDate

data class OppdaterSakResponse(
    val saksnummer: Saksnummer,
    val eierfogd: Enhetsnummer,
    val kategorikode: Sakskategori,
    val status: Bidragssakstatus,
    val ansatt: Boolean,
    val inhabilitet: Boolean,
    val levdeAdskilt: Boolean,
    val sanertDato: LocalDate?,
    val arbeidsfordeling: Arbeidsfordeling = Arbeidsfordeling.EIERENHET,
    val landkode: Landkode?,
    val konvensjonskode: Konvensjon?,
    val konvensjonsdato: LocalDate?,
    val ffuReferansenr: String?,
    val roller: Collection<RolleDto> = setOf(),
)

package no.nav.bidrag.transport.sak

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Fogdårsak
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import java.time.LocalDate

@Schema(description = "Metadata for en bidragssak")
data class BidragssakDto(
    @Schema(description = "Eierfogd for bidragssaken")
    val eierfogd: Enhetsnummer,
    @Schema(description = "Saksnummeret til bidragssaken")
    val saksnummer: Saksnummer,
    @Schema(description = "Saksstatus til bidragssaken")
    val saksstatus: Bidragssakstatus,
    @Schema(description = "Kategorikode: 'N' eller 'U'")
    val kategori: Sakskategori,
    @Schema(description = "Om saken inneholder personer med diskresjonskode")
    val begrensetTilgang: Boolean = false,
    val opprettetDato: LocalDate,
    val levdeAdskilt: Boolean,
    @Schema(description = "Hvor vidt en av partene i saken er ukjent")
    val ukjentPart: Boolean,
    val vedtakssperre: Boolean = false,
    val avsluttet: Boolean = false,
    val arbeidsfordeling: Arbeidsfordeling = Arbeidsfordeling.EIERENHET,
    @Schema(description = "Rollene som saken inneholder")
    val roller: List<RolleDto> = emptyList(),
)

data class SamhandlerSakerDto(
    val antallSaker: Int,
    val saksnummere: List<String>,
)

data class SamhandlerSakerRequestDto(
    val samhandlerId: String,
)

data class FjernMidlertidligTilgangRequest(
    val saksnummer: String,
    val enhet: String,
    val årsak: Fogdårsak? = null,
)

data class OpprettMidlertidligTilgangRequest(
    val saksnummer: String,
    val enhet: String,
    val tilgangTilOgMedDato: LocalDate? = null,
    val årsak: Fogdårsak = Fogdårsak.MAKO,
)

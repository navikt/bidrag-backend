package no.nav.bidrag.transport.sak

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Konvensjon
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.land.Landkode
import java.time.LocalDate

class OpprettSakRequest(
    val kategori: Sakskategori = Sakskategori.NASJONAL,
    val levdeAdskilt: Boolean = false,
    @field:Schema(description = "Kovensjonskode tilsvarende kodene i T_KODE_KONVENSJON.")
    val konvensjon: Konvensjon? = null,
    val konvensjonsdato: LocalDate? = null,
    val ffuReferansenr: String? = null,
    val land: Landkode? = null,
    val arbeidsfordeling: Arbeidsfordeling = Arbeidsfordeling.EIERENHET,
    @field:Schema(description = "Rollene som skal opprettes i saken.")
    val roller: Set<RolleDto> = emptySet(),
)

package no.nav.bidrag.transport.sak

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Konvensjon
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.land.Landkode
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import java.time.LocalDate

class OpprettSakRequest(
    @field:Schema(description = "Sakens eierfogd (enhetsnummeret som får tilgang til saken).")
    val eierfogd: Enhetsnummer,
    val kategori: Sakskategori = Sakskategori.NASJONAL,
    val ansatt: Boolean = false,
    val inhabilitet: Boolean = false,
    val levdeAdskilt: Boolean = false,
    @field:Schema(description = "Kovensjonskode tilsvarende kodene i T_KODE_KONVENSJON.")
    val konvensjon: Konvensjon? = null,
    val konvensjonsdato: LocalDate? = null,
    val ffuReferansenr: String? = null,
    val land: Landkode? = null,
    val arbeidsfordeling: Arbeidsfordeling = Arbeidsfordeling.EIERENHET,
    @field:Schema(
        description =
            "Rollene som skal opprettes i saken. " +
                "Hvis BM mangler, må alle barn (BA) ha reell mottaker (RM).",
    )
    val roller: Set<RolleDto> = emptySet(),
)

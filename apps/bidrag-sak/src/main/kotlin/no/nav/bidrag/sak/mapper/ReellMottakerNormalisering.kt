package no.nav.bidrag.sak.mapper

import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.ReellMottakerDto
import no.nav.bidrag.transport.sak.RolleDto

/**
 * Legacy opprett sak i frontend sender RM i det utgåtte feltet `reellMottager`.
 * Verdien flyttes til `reellMottaker` ved inngangen, slik at resten av bidrag-sak bare leser `reellMottaker`.
 */
fun Set<RolleDto>.normaliserReellMottaker(): Set<RolleDto> = map { it.normaliserReellMottaker() }.toSet()

fun OpprettSakRequest.normaliserReellMottaker() = OpprettSakRequest(
    eierfogd = eierfogd,
    kategori = kategori,
    ansatt = ansatt,
    inhabilitet = inhabilitet,
    levdeAdskilt = levdeAdskilt,
    konvensjon = konvensjon,
    konvensjonsdato = konvensjonsdato,
    ffuReferansenr = ffuReferansenr,
    land = land,
    arbeidsfordeling = arbeidsfordeling,
    roller = roller.normaliserReellMottaker(),
)

@Suppress("DEPRECATION")
private fun RolleDto.normaliserReellMottaker(): RolleDto {
    val nytt = reellMottaker?.takeUnless { it.ident.verdi.isBlank() }
    val utgått = reellMottager?.takeUnless { it.verdi.isBlank() }
    require(nytt == null || utgått == null || nytt.ident == utgått) {
        "RM er oppgitt med ulik verdi i reellMottaker og reellMottager."
    }
    return copy(
        reellMottaker = nytt ?: utgått?.let { ReellMottakerDto(ident = it) },
        reellMottager = null,
    )
}

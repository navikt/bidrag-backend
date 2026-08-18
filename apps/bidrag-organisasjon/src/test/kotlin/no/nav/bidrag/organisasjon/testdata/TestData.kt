package no.nav.bidrag.organisasjon.testdata

import no.nav.bidrag.organisasjon.consumer.dto.EnhetKontakinformasjonResponse
import no.nav.bidrag.organisasjon.consumer.dto.EnhetPostadresseResponse

val ENHET_NR = "4806"
val ENHET_TLF = "123456789"

fun createEnhetKontaktinfoResponse(
    enhetNr: String? = ENHET_NR,
    poststed: String = "Drammen",
    postnummer: String = "3050",
    postboksnummer: String = "1583",
    postboksanlegg: String = "Drammen",
): EnhetKontakinformasjonResponse = EnhetKontakinformasjonResponse(
    enhetNr = enhetNr,
    telefonnummer = ENHET_TLF,
    postadresse =
    EnhetPostadresseResponse(
        poststed = poststed,
        postnummer = postnummer,
        postboksnummer = postboksnummer,
        postboksanlegg = postboksanlegg,
    ),
)

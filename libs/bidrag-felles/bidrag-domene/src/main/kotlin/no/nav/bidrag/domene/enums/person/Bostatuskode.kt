package no.nav.bidrag.domene.enums.person

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class Bostatuskode {
    MED_FORELDER,
    DOKUMENTERT_SKOLEGANG, // Hvis barnet er over 18 år, bor med forelder og går på skole
    IKKE_MED_FORELDER,
    DELT_BOSTED,
    REGNES_IKKE_SOM_BARN,

    // Følgende verdier brukes for voksne i husstand
    BOR_MED_ANDRE_VOKSNE,
    BOR_IKKE_MED_ANDRE_VOKSNE,

    // Følgende verdier brukes til unntakskoder i Bisys som påvirker resultatet av beregning
    UNNTAK_HOS_ANDRE,
    UNNTAK_ALENE,
    UNNTAK_ENSLIG_ASYLSØKER,

    MED_VERGE,
    ALENE,
}

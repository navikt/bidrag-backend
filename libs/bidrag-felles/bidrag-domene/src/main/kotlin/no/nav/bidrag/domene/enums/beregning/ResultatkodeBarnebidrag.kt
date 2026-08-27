package no.nav.bidrag.domene.enums.beregning

@Deprecated("", replaceWith = ReplaceWith("Resultatkode"))
enum class ResultatkodeBarnebidrag {
    BARNET_ER_SELVFORSØRGET, // Barnet er selvforsørget

    // Resultat av beregning av barnebidrag, angir at det må gjøres en forholdsmessig fordeling
    BEGRENSET_EVNE_FLERE_SAKER_UTFØR_FORHOLDSMESSIG_FORDELING,
    BEGRENSET_REVURDERING, // Beregnet bidrag er større enn forskuddsats, settes lik forskuddssats
    BIDRAG_IKKE_BEREGNET_DELT_BOSTED, // Barnet har delt bosted og BPs andel av U er under 50%, bidrag skal ikke beregnes
    BIDRAG_REDUSERT_AV_EVNE, // Bidrag redusert pga ikke full evne
    BIDRAG_REDUSERT_TIL_25_PROSENT_AV_INNTEKT, // Maks 25% av inntekt
    BIDRAG_SATT_TIL_BARNETILLEGG_BP, // BarnetilleggBP er høyere enn beregnet bidrag
    BIDRAG_SATT_TIL_BARNETILLEGG_FORSVARET, // Barnebidrag settes likt barnetillegg fra forsvaret
    BIDRAG_SATT_TIL_UNDERHOLDSKOSTNAD_MINUS_BARNETILLEGG_BM, // Beregnet bidrag er lavere enn underholdskostnad minus barnetilleggBM
    DELT_BOSTED, // Barnet bor like mye hos begge foreldre
    FORHOLDSMESSIG_FORDELING_BIDRAGSBELØP_ENDRET, // Beregning av forholdsmessig fordeling er utført og det er beregnet nytt bidragsbeløp
    FORHOLDSMESSIG_FORDELING_INGEN_ENDRING, // Beregning av forholdsmessig fordeling er utført og det er ingen endringer på bidragsbeløp
    INGEN_EVNE, // BP har 0.- i bidragsevne, bidrag satt til 0.-
    KOSTNADSBEREGNET_BIDRAG, // Kostnadsberegnet bidrag
}

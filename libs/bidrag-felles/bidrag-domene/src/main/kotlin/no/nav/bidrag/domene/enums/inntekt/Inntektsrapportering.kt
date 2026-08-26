package no.nav.bidrag.domene.enums.inntekt

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class Inntektsrapportering(
    val hentesAutomatisk: Boolean,
    val kanLeggesInnManuelt: Boolean,
    val inneholderInntektstypeListe: List<Inntektstype>,
    val legacyKode: String? = null,
) {
    // Rapporteringer fra bidrag-inntekt
    AINNTEKT(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.AAP,
                Inntektstype.DAGPENGER,
                Inntektstype.FORELDREPENGER,
                Inntektstype.INTRODUKSJONSSTØNAD,
                Inntektstype.KVALIFISERINGSSTØNAD,
                Inntektstype.LØNNSINNTEKT,
                Inntektstype.OVERGANGSSTØNAD,
                Inntektstype.PENSJON,
                Inntektstype.SYKEPENGER,
            ),
        legacyKode = "LTA",
    ),

    AINNTEKT_BEREGNET_3MND(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.AAP,
                Inntektstype.DAGPENGER,
                Inntektstype.FORELDREPENGER,
                Inntektstype.INTRODUKSJONSSTØNAD,
                Inntektstype.KVALIFISERINGSSTØNAD,
                Inntektstype.LØNNSINNTEKT,
                Inntektstype.OVERGANGSSTØNAD,
                Inntektstype.PENSJON,
                Inntektstype.SYKEPENGER,
            ),
    ),

    AINNTEKT_BEREGNET_12MND(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.AAP,
                Inntektstype.DAGPENGER,
                Inntektstype.FORELDREPENGER,
                Inntektstype.INTRODUKSJONSSTØNAD,
                Inntektstype.KVALIFISERINGSSTØNAD,
                Inntektstype.LØNNSINNTEKT,
                Inntektstype.OVERGANGSSTØNAD,
                Inntektstype.PENSJON,
                Inntektstype.SYKEPENGER,
            ),
    ),

    AINNTEKT_BEREGNET_LAVESTE_MÅNEDSINNTEKT_SISTE_3MND_GANGET_TOLV(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.AAP,
                Inntektstype.DAGPENGER,
                Inntektstype.FORELDREPENGER,
                Inntektstype.INTRODUKSJONSSTØNAD,
                Inntektstype.KVALIFISERINGSSTØNAD,
                Inntektstype.LØNNSINNTEKT,
                Inntektstype.OVERGANGSSTØNAD,
                Inntektstype.PENSJON,
                Inntektstype.SYKEPENGER,
            ),
    ),

    AINNTEKT_BEREGNET_3MND_FRA_OPPRINNELIG_VEDTAKSTIDSPUNKT(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.AAP,
                Inntektstype.DAGPENGER,
                Inntektstype.FORELDREPENGER,
                Inntektstype.INTRODUKSJONSSTØNAD,
                Inntektstype.KVALIFISERINGSSTØNAD,
                Inntektstype.LØNNSINNTEKT,
                Inntektstype.OVERGANGSSTØNAD,
                Inntektstype.PENSJON,
                Inntektstype.SYKEPENGER,
            ),
    ),

    AINNTEKT_BEREGNET_12MND_FRA_OPPRINNELIG_VEDTAKSTIDSPUNKT(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.AAP,
                Inntektstype.DAGPENGER,
                Inntektstype.FORELDREPENGER,
                Inntektstype.INTRODUKSJONSSTØNAD,
                Inntektstype.KVALIFISERINGSSTØNAD,
                Inntektstype.LØNNSINNTEKT,
                Inntektstype.OVERGANGSSTØNAD,
                Inntektstype.PENSJON,
                Inntektstype.SYKEPENGER,
            ),
    ),

    @Deprecated(
        "Bruk heller AINNTEKT_BEREGNET_3MND_FRA_OPPRINNELIG_VEDTAKSTIDSPUNKT",
        replaceWith = ReplaceWith("AINNTEKT_BEREGNET_3MND_FRA_OPPRINNELIG_VEDTAKSTIDSPUNKT"),
    )
    AINNTEKT_BEREGNET_3MND_FRA_OPPRINNELIG_VEDTAK(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.AAP,
                Inntektstype.DAGPENGER,
                Inntektstype.FORELDREPENGER,
                Inntektstype.INTRODUKSJONSSTØNAD,
                Inntektstype.KVALIFISERINGSSTØNAD,
                Inntektstype.LØNNSINNTEKT,
                Inntektstype.OVERGANGSSTØNAD,
                Inntektstype.PENSJON,
                Inntektstype.SYKEPENGER,
            ),
    ),

    @Deprecated(
        "Bruk heller AINNTEKT_BEREGNET_12MND_FRA_OPPRINNELIG_VEDTAKSTIDSPUNKT",
        replaceWith = ReplaceWith("AINNTEKT_BEREGNET_12MND_FRA_OPPRINNELIG_VEDTAKSTIDSPUNKT"),
    )
    AINNTEKT_BEREGNET_12MND_FRA_OPPRINNELIG_VEDTAK(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.AAP,
                Inntektstype.DAGPENGER,
                Inntektstype.FORELDREPENGER,
                Inntektstype.INTRODUKSJONSSTØNAD,
                Inntektstype.KVALIFISERINGSSTØNAD,
                Inntektstype.LØNNSINNTEKT,
                Inntektstype.OVERGANGSSTØNAD,
                Inntektstype.PENSJON,
                Inntektstype.SYKEPENGER,
            ),
    ),

    KAPITALINNTEKT(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.KAPITALINNTEKT,
            ),
        legacyKode = "KAPS",
    ),

    LIGNINGSINNTEKT(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.AAP,
                Inntektstype.DAGPENGER,
                Inntektstype.FORELDREPENGER,
                Inntektstype.INTRODUKSJONSSTØNAD,
                Inntektstype.KVALIFISERINGSSTØNAD,
                Inntektstype.LØNNSINNTEKT,
                Inntektstype.OVERGANGSSTØNAD,
                Inntektstype.PENSJON,
                Inntektstype.SYKEPENGER,
            ),
        legacyKode = "LIGS",
    ),

    KONTANTSTØTTE(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = true,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.KONTANTSTØTTE,
            ),
        legacyKode = "KONT",
    ),

    SMÅBARNSTILLEGG(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = true,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.SMÅBARNSTILLEGG,
            ),
        legacyKode = "ESBT",
    ),

    UTVIDET_BARNETRYGD(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = true,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.UTVIDET_BARNETRYGD,
            ),
        legacyKode = "UBAT",
    ),

    AAP(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.AAP,
            ),
        legacyKode = "AT",
    ),

    DAGPENGER(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.DAGPENGER,
            ),
        legacyKode = "AL",
    ),

    FORELDREPENGER(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.FORELDREPENGER,
            ),
    ),

    INTRODUKSJONSSTØNAD(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.INTRODUKSJONSSTØNAD,
            ),
    ),

    KVALIFISERINGSSTØNAD(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.KVALIFISERINGSSTØNAD,
            ),
    ),

    OVERGANGSSTØNAD(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.OVERGANGSSTØNAD,
            ),
        legacyKode = "EFOS",
    ),

    PENSJON(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.PENSJON,
            ),
        legacyKode = "PE",
    ),

    SYKEPENGER(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.SYKEPENGER,
            ),
        legacyKode = "SP",
    ),

    BARNETILLEGG(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = true,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.BARNETILLEGG_PENSJON,
                Inntektstype.BARNETILLEGG_UFØRETRYGD,
                Inntektstype.BARNETILLEGG_DAGPENGER,
                Inntektstype.BARNETILLEGG_KVALIFISERINGSSTØNAD,
                Inntektstype.BARNETILLEGG_AAP,
                Inntektstype.BARNETILLEGG_DNB,
                Inntektstype.BARNETILLEGG_NORDEA,
                Inntektstype.BARNETILLEGG_STOREBRAND,
                Inntektstype.BARNETILLEGG_KLP,
                Inntektstype.BARNETILLEGG_SPK,
                Inntektstype.BARNETILLEGG_TILTAKSPENGER,
            ),
    ),

    BARNETILSYN(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = true,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.BARNETILSYN,
            ),
    ),

    // Manuelt registrerte rapporteringer

    PERSONINNTEKT_EGNE_OPPLYSNINGER(
        hentesAutomatisk = false,
        kanLeggesInnManuelt = true,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.AAP,
                Inntektstype.DAGPENGER,
                Inntektstype.FORELDREPENGER,
                Inntektstype.INTRODUKSJONSSTØNAD,
                Inntektstype.KVALIFISERINGSSTØNAD,
                Inntektstype.LØNNSINNTEKT,
                Inntektstype.OVERGANGSSTØNAD,
                Inntektstype.PENSJON,
                Inntektstype.SYKEPENGER,
            ),
        legacyKode = "PIEO",
    ),

    KAPITALINNTEKT_EGNE_OPPLYSNINGER(
        hentesAutomatisk = false,
        kanLeggesInnManuelt = true,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.KAPITALINNTEKT,
            ),
        legacyKode = "KIEO",
    ),

    SAKSBEHANDLER_BEREGNET_INNTEKT(
        hentesAutomatisk = false,
        kanLeggesInnManuelt = true,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.AAP,
                Inntektstype.DAGPENGER,
                Inntektstype.FORELDREPENGER,
                Inntektstype.INTRODUKSJONSSTØNAD,
                Inntektstype.KVALIFISERINGSSTØNAD,
                Inntektstype.LØNNSINNTEKT,
                Inntektstype.OVERGANGSSTØNAD,
                Inntektstype.PENSJON,
                Inntektstype.SYKEPENGER,
            ),
        legacyKode = "SAK",
    ),

    LØNN_MANUELT_BEREGNET(
        hentesAutomatisk = false,
        kanLeggesInnManuelt = true,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.LØNNSINNTEKT,
            ),
    ),

    NÆRINGSINNTEKT_MANUELT_BEREGNET(
        hentesAutomatisk = false,
        kanLeggesInnManuelt = true,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.NÆRINGSINNTEKT,
            ),
    ),

    YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET(
        hentesAutomatisk = false,
        kanLeggesInnManuelt = true,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.AAP,
                Inntektstype.DAGPENGER,
                Inntektstype.FORELDREPENGER,
                Inntektstype.INTRODUKSJONSSTØNAD,
                Inntektstype.KVALIFISERINGSSTØNAD,
                Inntektstype.OVERGANGSSTØNAD,
                Inntektstype.PENSJON,
                Inntektstype.SYKEPENGER,
            ),
    ),

    // Rapporteringer brukt i Bisys/BBM

    AINNTEKT_KORRIGERT_FOR_BARNETILLEGG(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe = emptyList(),
        legacyKode = "LTAB",
    ),

    BARNETRYGD_MANUELL_VURDERING(
        hentesAutomatisk = false,
        kanLeggesInnManuelt = true,
        inneholderInntektstypeListe = emptyList(),
        legacyKode = "BAMV",
    ),

    BARNS_SYKDOM(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe = emptyList(),
        legacyKode = "BS",
    ),

    SKJØNN_MANGLER_DOKUMENTASJON(
        hentesAutomatisk = false,
        kanLeggesInnManuelt = true,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.AAP,
                Inntektstype.DAGPENGER,
                Inntektstype.FORELDREPENGER,
                Inntektstype.INTRODUKSJONSSTØNAD,
                Inntektstype.KVALIFISERINGSSTØNAD,
                Inntektstype.OVERGANGSSTØNAD,
                Inntektstype.LØNNSINNTEKT,
                Inntektstype.KAPITALINNTEKT,
                Inntektstype.NÆRINGSINNTEKT,
                Inntektstype.PENSJON,
                Inntektstype.SYKEPENGER,
            ),
        legacyKode = "MDOK",
    ),

    FORDEL_SÆRFRADRAG_ENSLIG_FORSØRGER(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = true,
        inneholderInntektstypeListe = emptyList(),
        legacyKode = "FSEF",
    ),

    FØDSEL_ADOPSJON(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe = emptyList(),
        legacyKode = "FA",
    ),

    INNTEKTSOPPLYSNINGER_FRA_ARBEIDSGIVER(
        hentesAutomatisk = false,
        kanLeggesInnManuelt = true,
        inneholderInntektstypeListe = emptyList(),
        legacyKode = "AG",
    ),

    LIGNINGSOPPLYSNINGER_MANGLER(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe = emptyList(),
        legacyKode = "ILOF",
    ),

    LIGNING_FRA_SKATTEETATEN(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe = emptyList(),
        legacyKode = "LIGN",
    ),

    LØNNSOPPGAVE_FRA_SKATTEETATEN(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe = emptyList(),
        legacyKode = "LTR",
    ),

    LØNNSOPPGAVE_FRA_SKATTEETATEN_KORRIGERT_FOR_BARNETILLEGG(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe = emptyList(),
        legacyKode = "LTRB",
    ),

    SKJØNN_MANGLENDE_BRUK_AV_EVNE(
        hentesAutomatisk = false,
        kanLeggesInnManuelt = true,
        inneholderInntektstypeListe =
            listOf(
                Inntektstype.AAP,
                Inntektstype.DAGPENGER,
                Inntektstype.FORELDREPENGER,
                Inntektstype.INTRODUKSJONSSTØNAD,
                Inntektstype.KVALIFISERINGSSTØNAD,
                Inntektstype.OVERGANGSSTØNAD,
                Inntektstype.LØNNSINNTEKT,
                Inntektstype.KAPITALINNTEKT,
                Inntektstype.NÆRINGSINNTEKT,
                Inntektstype.PENSJON,
                Inntektstype.SYKEPENGER,
            ),
        legacyKode = "EVNE",
    ),

    NETTO_KAPITALINNTEKT(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe = emptyList(),
        legacyKode = "KAP",
    ),

    PENSJON_KORRIGERT_FOR_BARNETILLEGG(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe = emptyList(),
        legacyKode = "PEB",
    ),

    REHABILITERINGSPENGER(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe = emptyList(),
        legacyKode = "RP",
    ),

    SKATTEGRUNNLAG_KORRIGERT_FOR_BARNETILLEGG(
        hentesAutomatisk = true,
        kanLeggesInnManuelt = false,
        inneholderInntektstypeListe = emptyList(),
        legacyKode = "LIGB",
    ),
    ;

    companion object {
        /**
         * Extension function som returnerer true hvis de to inntektrapporteringene som sammenlignes ikke inneholder noen like inntektstyper.
         * @param inntektsrapportering2 Verdi av type InntektRapportering som det skal sammenlignes med
         * @return true hvis inntektsrapporteringene som sammenlignes kan brukes samtidig
         */
        fun Inntektsrapportering.kanBrukesSammenMed(inntektsrapportering2: Inntektsrapportering) =
            !(this.inneholderInntektstypeListe.any { inntektsrapportering2.inneholderInntektstypeListe.contains(it) })

        fun fraLegacyKode(legacyKode: String): Inntektsrapportering? =
            try {
                enumValues<Inntektsrapportering>().find { it.legacyKode == legacyKode } ?: Inntektsrapportering.valueOf(legacyKode)
            } catch (e: Exception) {
                null
            }
    }
}

package no.nav.bidrag.domene.enum

import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering.Companion.kanBrukesSammenMed
import org.junit.jupiter.api.Test

class InntektsrapporteringTest {
    @Test
    fun `skal sjekke om to inntektsrapporteringer kan opptre samtidig`() {
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.AINNTEKT_BEREGNET_3MND) shouldBe false
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.AINNTEKT_BEREGNET_12MND) shouldBe false
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT) shouldBe true
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.LIGNINGSINNTEKT) shouldBe false
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.KONTANTSTØTTE) shouldBe true
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.SMÅBARNSTILLEGG) shouldBe true
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.UTVIDET_BARNETRYGD) shouldBe true
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.AAP) shouldBe false
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.DAGPENGER) shouldBe false
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.FORELDREPENGER) shouldBe false
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.INTRODUKSJONSSTØNAD) shouldBe false
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.KVALIFISERINGSSTØNAD) shouldBe false
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.OVERGANGSSTØNAD) shouldBe false
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.PENSJON) shouldBe false
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.SYKEPENGER) shouldBe false
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.LØNN_MANUELT_BEREGNET) shouldBe false
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER) shouldBe false
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT) shouldBe false
        Inntektsrapportering.AINNTEKT.kanBrukesSammenMed(Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET) shouldBe false

        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.AINNTEKT_BEREGNET_12MND) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT) shouldBe true
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.LIGNINGSINNTEKT) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.KONTANTSTØTTE) shouldBe true
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.SMÅBARNSTILLEGG) shouldBe true
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.UTVIDET_BARNETRYGD) shouldBe true
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.AAP) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.DAGPENGER) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.FORELDREPENGER) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.INTRODUKSJONSSTØNAD) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.KVALIFISERINGSSTØNAD) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.OVERGANGSSTØNAD) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.PENSJON) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.SYKEPENGER) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.LØNN_MANUELT_BEREGNET) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_3MND.kanBrukesSammenMed(
            Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET,
        ) shouldBe false

        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT) shouldBe true
        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(Inntektsrapportering.LIGNINGSINNTEKT) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(Inntektsrapportering.KONTANTSTØTTE) shouldBe true
        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(Inntektsrapportering.SMÅBARNSTILLEGG) shouldBe true
        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(Inntektsrapportering.UTVIDET_BARNETRYGD) shouldBe true
        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(Inntektsrapportering.AAP) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(Inntektsrapportering.DAGPENGER) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(Inntektsrapportering.FORELDREPENGER) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(Inntektsrapportering.INTRODUKSJONSSTØNAD) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(Inntektsrapportering.KVALIFISERINGSSTØNAD) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(Inntektsrapportering.OVERGANGSSTØNAD) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(Inntektsrapportering.PENSJON) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(Inntektsrapportering.SYKEPENGER) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(Inntektsrapportering.LØNN_MANUELT_BEREGNET) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT) shouldBe false
        Inntektsrapportering.AINNTEKT_BEREGNET_12MND.kanBrukesSammenMed(
            Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET,
        ) shouldBe false

        Inntektsrapportering.KAPITALINNTEKT.kanBrukesSammenMed(Inntektsrapportering.LIGNINGSINNTEKT) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT.kanBrukesSammenMed(Inntektsrapportering.KONTANTSTØTTE) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT.kanBrukesSammenMed(Inntektsrapportering.SMÅBARNSTILLEGG) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT.kanBrukesSammenMed(Inntektsrapportering.UTVIDET_BARNETRYGD) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT.kanBrukesSammenMed(Inntektsrapportering.AAP) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT.kanBrukesSammenMed(Inntektsrapportering.DAGPENGER) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT.kanBrukesSammenMed(Inntektsrapportering.FORELDREPENGER) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT.kanBrukesSammenMed(Inntektsrapportering.INTRODUKSJONSSTØNAD) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT.kanBrukesSammenMed(Inntektsrapportering.KVALIFISERINGSSTØNAD) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT.kanBrukesSammenMed(Inntektsrapportering.OVERGANGSSTØNAD) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT.kanBrukesSammenMed(Inntektsrapportering.PENSJON) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT.kanBrukesSammenMed(Inntektsrapportering.SYKEPENGER) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT.kanBrukesSammenMed(Inntektsrapportering.LØNN_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER) shouldBe false
        Inntektsrapportering.KAPITALINNTEKT.kanBrukesSammenMed(Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT.kanBrukesSammenMed(Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT.kanBrukesSammenMed(Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT.kanBrukesSammenMed(Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET) shouldBe true

        Inntektsrapportering.LIGNINGSINNTEKT.kanBrukesSammenMed(Inntektsrapportering.KONTANTSTØTTE) shouldBe true
        Inntektsrapportering.LIGNINGSINNTEKT.kanBrukesSammenMed(Inntektsrapportering.SMÅBARNSTILLEGG) shouldBe true
        Inntektsrapportering.LIGNINGSINNTEKT.kanBrukesSammenMed(Inntektsrapportering.UTVIDET_BARNETRYGD) shouldBe true
        Inntektsrapportering.LIGNINGSINNTEKT.kanBrukesSammenMed(Inntektsrapportering.AAP) shouldBe false
        Inntektsrapportering.LIGNINGSINNTEKT.kanBrukesSammenMed(Inntektsrapportering.DAGPENGER) shouldBe false
        Inntektsrapportering.LIGNINGSINNTEKT.kanBrukesSammenMed(Inntektsrapportering.FORELDREPENGER) shouldBe false
        Inntektsrapportering.LIGNINGSINNTEKT.kanBrukesSammenMed(Inntektsrapportering.INTRODUKSJONSSTØNAD) shouldBe false
        Inntektsrapportering.LIGNINGSINNTEKT.kanBrukesSammenMed(Inntektsrapportering.KVALIFISERINGSSTØNAD) shouldBe false
        Inntektsrapportering.LIGNINGSINNTEKT.kanBrukesSammenMed(Inntektsrapportering.OVERGANGSSTØNAD) shouldBe false
        Inntektsrapportering.LIGNINGSINNTEKT.kanBrukesSammenMed(Inntektsrapportering.PENSJON) shouldBe false
        Inntektsrapportering.LIGNINGSINNTEKT.kanBrukesSammenMed(Inntektsrapportering.SYKEPENGER) shouldBe false
        Inntektsrapportering.LIGNINGSINNTEKT.kanBrukesSammenMed(Inntektsrapportering.LØNN_MANUELT_BEREGNET) shouldBe false
        Inntektsrapportering.LIGNINGSINNTEKT.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.LIGNINGSINNTEKT.kanBrukesSammenMed(Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.LIGNINGSINNTEKT.kanBrukesSammenMed(Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER) shouldBe false
        Inntektsrapportering.LIGNINGSINNTEKT.kanBrukesSammenMed(Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT) shouldBe false
        Inntektsrapportering.LIGNINGSINNTEKT.kanBrukesSammenMed(Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET) shouldBe false

        Inntektsrapportering.KONTANTSTØTTE.kanBrukesSammenMed(Inntektsrapportering.SMÅBARNSTILLEGG) shouldBe true
        Inntektsrapportering.KONTANTSTØTTE.kanBrukesSammenMed(Inntektsrapportering.UTVIDET_BARNETRYGD) shouldBe true
        Inntektsrapportering.KONTANTSTØTTE.kanBrukesSammenMed(Inntektsrapportering.AAP) shouldBe true
        Inntektsrapportering.KONTANTSTØTTE.kanBrukesSammenMed(Inntektsrapportering.DAGPENGER) shouldBe true
        Inntektsrapportering.KONTANTSTØTTE.kanBrukesSammenMed(Inntektsrapportering.FORELDREPENGER) shouldBe true
        Inntektsrapportering.KONTANTSTØTTE.kanBrukesSammenMed(Inntektsrapportering.INTRODUKSJONSSTØNAD) shouldBe true
        Inntektsrapportering.KONTANTSTØTTE.kanBrukesSammenMed(Inntektsrapportering.KVALIFISERINGSSTØNAD) shouldBe true
        Inntektsrapportering.KONTANTSTØTTE.kanBrukesSammenMed(Inntektsrapportering.OVERGANGSSTØNAD) shouldBe true
        Inntektsrapportering.KONTANTSTØTTE.kanBrukesSammenMed(Inntektsrapportering.PENSJON) shouldBe true
        Inntektsrapportering.KONTANTSTØTTE.kanBrukesSammenMed(Inntektsrapportering.SYKEPENGER) shouldBe true
        Inntektsrapportering.KONTANTSTØTTE.kanBrukesSammenMed(Inntektsrapportering.LØNN_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.KONTANTSTØTTE.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.KONTANTSTØTTE.kanBrukesSammenMed(Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.KONTANTSTØTTE.kanBrukesSammenMed(Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.KONTANTSTØTTE.kanBrukesSammenMed(Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT) shouldBe true
        Inntektsrapportering.KONTANTSTØTTE.kanBrukesSammenMed(Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET) shouldBe true

        Inntektsrapportering.SMÅBARNSTILLEGG.kanBrukesSammenMed(Inntektsrapportering.UTVIDET_BARNETRYGD) shouldBe true
        Inntektsrapportering.SMÅBARNSTILLEGG.kanBrukesSammenMed(Inntektsrapportering.AAP) shouldBe true
        Inntektsrapportering.SMÅBARNSTILLEGG.kanBrukesSammenMed(Inntektsrapportering.DAGPENGER) shouldBe true
        Inntektsrapportering.SMÅBARNSTILLEGG.kanBrukesSammenMed(Inntektsrapportering.FORELDREPENGER) shouldBe true
        Inntektsrapportering.SMÅBARNSTILLEGG.kanBrukesSammenMed(Inntektsrapportering.INTRODUKSJONSSTØNAD) shouldBe true
        Inntektsrapportering.SMÅBARNSTILLEGG.kanBrukesSammenMed(Inntektsrapportering.KVALIFISERINGSSTØNAD) shouldBe true
        Inntektsrapportering.SMÅBARNSTILLEGG.kanBrukesSammenMed(Inntektsrapportering.OVERGANGSSTØNAD) shouldBe true
        Inntektsrapportering.SMÅBARNSTILLEGG.kanBrukesSammenMed(Inntektsrapportering.PENSJON) shouldBe true
        Inntektsrapportering.SMÅBARNSTILLEGG.kanBrukesSammenMed(Inntektsrapportering.SYKEPENGER) shouldBe true
        Inntektsrapportering.SMÅBARNSTILLEGG.kanBrukesSammenMed(Inntektsrapportering.LØNN_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.SMÅBARNSTILLEGG.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.SMÅBARNSTILLEGG.kanBrukesSammenMed(Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.SMÅBARNSTILLEGG.kanBrukesSammenMed(Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.SMÅBARNSTILLEGG.kanBrukesSammenMed(Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT) shouldBe true
        Inntektsrapportering.SMÅBARNSTILLEGG.kanBrukesSammenMed(Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET) shouldBe true

        Inntektsrapportering.UTVIDET_BARNETRYGD.kanBrukesSammenMed(Inntektsrapportering.AAP) shouldBe true
        Inntektsrapportering.UTVIDET_BARNETRYGD.kanBrukesSammenMed(Inntektsrapportering.DAGPENGER) shouldBe true
        Inntektsrapportering.UTVIDET_BARNETRYGD.kanBrukesSammenMed(Inntektsrapportering.FORELDREPENGER) shouldBe true
        Inntektsrapportering.UTVIDET_BARNETRYGD.kanBrukesSammenMed(Inntektsrapportering.INTRODUKSJONSSTØNAD) shouldBe true
        Inntektsrapportering.UTVIDET_BARNETRYGD.kanBrukesSammenMed(Inntektsrapportering.KVALIFISERINGSSTØNAD) shouldBe true
        Inntektsrapportering.UTVIDET_BARNETRYGD.kanBrukesSammenMed(Inntektsrapportering.OVERGANGSSTØNAD) shouldBe true
        Inntektsrapportering.UTVIDET_BARNETRYGD.kanBrukesSammenMed(Inntektsrapportering.PENSJON) shouldBe true
        Inntektsrapportering.UTVIDET_BARNETRYGD.kanBrukesSammenMed(Inntektsrapportering.SYKEPENGER) shouldBe true
        Inntektsrapportering.UTVIDET_BARNETRYGD.kanBrukesSammenMed(Inntektsrapportering.LØNN_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.UTVIDET_BARNETRYGD.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.UTVIDET_BARNETRYGD.kanBrukesSammenMed(Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.UTVIDET_BARNETRYGD.kanBrukesSammenMed(Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.UTVIDET_BARNETRYGD.kanBrukesSammenMed(Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT) shouldBe true
        Inntektsrapportering.UTVIDET_BARNETRYGD.kanBrukesSammenMed(Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET) shouldBe true

        Inntektsrapportering.AAP.kanBrukesSammenMed(Inntektsrapportering.DAGPENGER) shouldBe true
        Inntektsrapportering.AAP.kanBrukesSammenMed(Inntektsrapportering.FORELDREPENGER) shouldBe true
        Inntektsrapportering.AAP.kanBrukesSammenMed(Inntektsrapportering.INTRODUKSJONSSTØNAD) shouldBe true
        Inntektsrapportering.AAP.kanBrukesSammenMed(Inntektsrapportering.KVALIFISERINGSSTØNAD) shouldBe true
        Inntektsrapportering.AAP.kanBrukesSammenMed(Inntektsrapportering.OVERGANGSSTØNAD) shouldBe true
        Inntektsrapportering.AAP.kanBrukesSammenMed(Inntektsrapportering.PENSJON) shouldBe true
        Inntektsrapportering.AAP.kanBrukesSammenMed(Inntektsrapportering.SYKEPENGER) shouldBe true
        Inntektsrapportering.AAP.kanBrukesSammenMed(Inntektsrapportering.LØNN_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.AAP.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.AAP.kanBrukesSammenMed(Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.AAP.kanBrukesSammenMed(Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER) shouldBe false
        Inntektsrapportering.AAP.kanBrukesSammenMed(Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT) shouldBe false
        Inntektsrapportering.AAP.kanBrukesSammenMed(Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET) shouldBe false

        Inntektsrapportering.DAGPENGER.kanBrukesSammenMed(Inntektsrapportering.FORELDREPENGER) shouldBe true
        Inntektsrapportering.DAGPENGER.kanBrukesSammenMed(Inntektsrapportering.INTRODUKSJONSSTØNAD) shouldBe true
        Inntektsrapportering.DAGPENGER.kanBrukesSammenMed(Inntektsrapportering.KVALIFISERINGSSTØNAD) shouldBe true
        Inntektsrapportering.DAGPENGER.kanBrukesSammenMed(Inntektsrapportering.OVERGANGSSTØNAD) shouldBe true
        Inntektsrapportering.DAGPENGER.kanBrukesSammenMed(Inntektsrapportering.PENSJON) shouldBe true
        Inntektsrapportering.DAGPENGER.kanBrukesSammenMed(Inntektsrapportering.SYKEPENGER) shouldBe true
        Inntektsrapportering.DAGPENGER.kanBrukesSammenMed(Inntektsrapportering.LØNN_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.DAGPENGER.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.DAGPENGER.kanBrukesSammenMed(Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.DAGPENGER.kanBrukesSammenMed(Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER) shouldBe false
        Inntektsrapportering.DAGPENGER.kanBrukesSammenMed(Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT) shouldBe false
        Inntektsrapportering.DAGPENGER.kanBrukesSammenMed(Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET) shouldBe false

        Inntektsrapportering.FORELDREPENGER.kanBrukesSammenMed(Inntektsrapportering.INTRODUKSJONSSTØNAD) shouldBe true
        Inntektsrapportering.FORELDREPENGER.kanBrukesSammenMed(Inntektsrapportering.KVALIFISERINGSSTØNAD) shouldBe true
        Inntektsrapportering.FORELDREPENGER.kanBrukesSammenMed(Inntektsrapportering.OVERGANGSSTØNAD) shouldBe true
        Inntektsrapportering.FORELDREPENGER.kanBrukesSammenMed(Inntektsrapportering.PENSJON) shouldBe true
        Inntektsrapportering.FORELDREPENGER.kanBrukesSammenMed(Inntektsrapportering.SYKEPENGER) shouldBe true
        Inntektsrapportering.FORELDREPENGER.kanBrukesSammenMed(Inntektsrapportering.LØNN_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.FORELDREPENGER.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.FORELDREPENGER.kanBrukesSammenMed(Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.FORELDREPENGER.kanBrukesSammenMed(Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER) shouldBe false
        Inntektsrapportering.FORELDREPENGER.kanBrukesSammenMed(Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT) shouldBe false
        Inntektsrapportering.FORELDREPENGER.kanBrukesSammenMed(Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET) shouldBe false

        Inntektsrapportering.INTRODUKSJONSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.KVALIFISERINGSSTØNAD) shouldBe true
        Inntektsrapportering.INTRODUKSJONSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.OVERGANGSSTØNAD) shouldBe true
        Inntektsrapportering.INTRODUKSJONSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.PENSJON) shouldBe true
        Inntektsrapportering.INTRODUKSJONSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.SYKEPENGER) shouldBe true
        Inntektsrapportering.INTRODUKSJONSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.LØNN_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.INTRODUKSJONSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.INTRODUKSJONSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.INTRODUKSJONSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER) shouldBe false
        Inntektsrapportering.INTRODUKSJONSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT) shouldBe false
        Inntektsrapportering.INTRODUKSJONSSTØNAD.kanBrukesSammenMed(
            Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET,
        ) shouldBe false

        Inntektsrapportering.KVALIFISERINGSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.OVERGANGSSTØNAD) shouldBe true
        Inntektsrapportering.KVALIFISERINGSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.PENSJON) shouldBe true
        Inntektsrapportering.KVALIFISERINGSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.SYKEPENGER) shouldBe true
        Inntektsrapportering.KVALIFISERINGSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.LØNN_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.KVALIFISERINGSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.KVALIFISERINGSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.KVALIFISERINGSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER) shouldBe false
        Inntektsrapportering.KVALIFISERINGSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT) shouldBe false
        Inntektsrapportering.KVALIFISERINGSSTØNAD.kanBrukesSammenMed(
            Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET,
        ) shouldBe false

        Inntektsrapportering.OVERGANGSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.PENSJON) shouldBe true
        Inntektsrapportering.OVERGANGSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.SYKEPENGER) shouldBe true
        Inntektsrapportering.OVERGANGSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.LØNN_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.OVERGANGSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.OVERGANGSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.OVERGANGSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER) shouldBe false
        Inntektsrapportering.OVERGANGSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT) shouldBe false
        Inntektsrapportering.OVERGANGSSTØNAD.kanBrukesSammenMed(Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET) shouldBe false

        Inntektsrapportering.PENSJON.kanBrukesSammenMed(Inntektsrapportering.SYKEPENGER) shouldBe true
        Inntektsrapportering.PENSJON.kanBrukesSammenMed(Inntektsrapportering.LØNN_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.PENSJON.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.PENSJON.kanBrukesSammenMed(Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.PENSJON.kanBrukesSammenMed(Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER) shouldBe false
        Inntektsrapportering.PENSJON.kanBrukesSammenMed(Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT) shouldBe false
        Inntektsrapportering.PENSJON.kanBrukesSammenMed(Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET) shouldBe false

        Inntektsrapportering.SYKEPENGER.kanBrukesSammenMed(Inntektsrapportering.LØNN_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.SYKEPENGER.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.SYKEPENGER.kanBrukesSammenMed(Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.SYKEPENGER.kanBrukesSammenMed(Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER) shouldBe false
        Inntektsrapportering.SYKEPENGER.kanBrukesSammenMed(Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT) shouldBe false
        Inntektsrapportering.SYKEPENGER.kanBrukesSammenMed(Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET) shouldBe false

        Inntektsrapportering.LØNN_MANUELT_BEREGNET.kanBrukesSammenMed(Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER) shouldBe true
        Inntektsrapportering.LØNN_MANUELT_BEREGNET.kanBrukesSammenMed(Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET) shouldBe true
        Inntektsrapportering.LØNN_MANUELT_BEREGNET.kanBrukesSammenMed(Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER) shouldBe false
        Inntektsrapportering.LØNN_MANUELT_BEREGNET.kanBrukesSammenMed(Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT) shouldBe false
        Inntektsrapportering.LØNN_MANUELT_BEREGNET.kanBrukesSammenMed(
            Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET,
        ) shouldBe true

        Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER.kanBrukesSammenMed(
            Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET,
        ) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER.kanBrukesSammenMed(
            Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER,
        ) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER.kanBrukesSammenMed(
            Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT,
        ) shouldBe true
        Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER.kanBrukesSammenMed(
            Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET,
        ) shouldBe true

        Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET.kanBrukesSammenMed(
            Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER,
        ) shouldBe true
        Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET.kanBrukesSammenMed(
            Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT,
        ) shouldBe true
        Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET.kanBrukesSammenMed(
            Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET,
        ) shouldBe true

        Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER.kanBrukesSammenMed(
            Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT,
        ) shouldBe false
        Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER.kanBrukesSammenMed(
            Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET,
        ) shouldBe false

        Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT.kanBrukesSammenMed(
            Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET,
        ) shouldBe false
    }
}

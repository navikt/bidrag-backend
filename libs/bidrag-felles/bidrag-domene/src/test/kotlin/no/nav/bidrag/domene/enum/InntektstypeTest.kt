package no.nav.bidrag.domene.enum

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.enums.inntekt.Inntektstype.Companion.inngårIInntektRapporteringer
import org.junit.jupiter.api.Test

class InntektstypeTest {
    @Test
    fun `skal hente alle inntektRapporteringer som inneholder en gitt inntektType`() {
        Inntektstype.AAP.inngårIInntektRapporteringer() shouldContainAll
            listOf(
                Inntektsrapportering.AINNTEKT,
                Inntektsrapportering.AINNTEKT_BEREGNET_3MND,
                Inntektsrapportering.AINNTEKT_BEREGNET_12MND,
                Inntektsrapportering.AINNTEKT_BEREGNET_3MND_FRA_OPPRINNELIG_VEDTAK,
                Inntektsrapportering.AINNTEKT_BEREGNET_12MND_FRA_OPPRINNELIG_VEDTAK,
                Inntektsrapportering.LIGNINGSINNTEKT,
                Inntektsrapportering.AAP,
                Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER,
                Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT,
                Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET,
            )

        Inntektstype.DAGPENGER.inngårIInntektRapporteringer() shouldContainAll
            listOf(
                Inntektsrapportering.AINNTEKT,
                Inntektsrapportering.AINNTEKT_BEREGNET_3MND,
                Inntektsrapportering.AINNTEKT_BEREGNET_12MND,
                Inntektsrapportering.AINNTEKT_BEREGNET_3MND_FRA_OPPRINNELIG_VEDTAK,
                Inntektsrapportering.AINNTEKT_BEREGNET_12MND_FRA_OPPRINNELIG_VEDTAK,
                Inntektsrapportering.LIGNINGSINNTEKT,
                Inntektsrapportering.DAGPENGER,
                Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER,
                Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT,
                Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET,
            )

        Inntektstype.FORELDREPENGER.inngårIInntektRapporteringer() shouldContainAll
            listOf(
                Inntektsrapportering.AINNTEKT,
                Inntektsrapportering.AINNTEKT_BEREGNET_3MND,
                Inntektsrapportering.AINNTEKT_BEREGNET_12MND,
                Inntektsrapportering.AINNTEKT_BEREGNET_3MND_FRA_OPPRINNELIG_VEDTAK,
                Inntektsrapportering.AINNTEKT_BEREGNET_12MND_FRA_OPPRINNELIG_VEDTAK,
                Inntektsrapportering.LIGNINGSINNTEKT,
                Inntektsrapportering.FORELDREPENGER,
                Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER,
                Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT,
                Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET,
            )

        Inntektstype.INTRODUKSJONSSTØNAD.inngårIInntektRapporteringer() shouldContainAll
            listOf(
                Inntektsrapportering.AINNTEKT,
                Inntektsrapportering.AINNTEKT_BEREGNET_3MND,
                Inntektsrapportering.AINNTEKT_BEREGNET_12MND,
                Inntektsrapportering.AINNTEKT_BEREGNET_3MND_FRA_OPPRINNELIG_VEDTAK,
                Inntektsrapportering.AINNTEKT_BEREGNET_12MND_FRA_OPPRINNELIG_VEDTAK,
                Inntektsrapportering.LIGNINGSINNTEKT,
                Inntektsrapportering.INTRODUKSJONSSTØNAD,
                Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER,
                Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT,
                Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET,
            )

        Inntektstype.KVALIFISERINGSSTØNAD.inngårIInntektRapporteringer() shouldContainAll
            listOf(
                Inntektsrapportering.AINNTEKT,
                Inntektsrapportering.AINNTEKT_BEREGNET_3MND,
                Inntektsrapportering.AINNTEKT_BEREGNET_12MND,
                Inntektsrapportering.AINNTEKT_BEREGNET_3MND_FRA_OPPRINNELIG_VEDTAK,
                Inntektsrapportering.AINNTEKT_BEREGNET_12MND_FRA_OPPRINNELIG_VEDTAK,
                Inntektsrapportering.LIGNINGSINNTEKT,
                Inntektsrapportering.KVALIFISERINGSSTØNAD,
                Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER,
                Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT,
                Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET,
            )

        Inntektstype.OVERGANGSSTØNAD.inngårIInntektRapporteringer() shouldContainAll
            listOf(
                Inntektsrapportering.AINNTEKT,
                Inntektsrapportering.AINNTEKT_BEREGNET_3MND,
                Inntektsrapportering.AINNTEKT_BEREGNET_12MND,
                Inntektsrapportering.AINNTEKT_BEREGNET_3MND_FRA_OPPRINNELIG_VEDTAK,
                Inntektsrapportering.AINNTEKT_BEREGNET_12MND_FRA_OPPRINNELIG_VEDTAK,
                Inntektsrapportering.LIGNINGSINNTEKT,
                Inntektsrapportering.OVERGANGSSTØNAD,
                Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER,
                Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT,
                Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET,
            )

        Inntektstype.PENSJON.inngårIInntektRapporteringer() shouldContainAll
            listOf(
                Inntektsrapportering.AINNTEKT,
                Inntektsrapportering.AINNTEKT_BEREGNET_3MND,
                Inntektsrapportering.AINNTEKT_BEREGNET_12MND,
                Inntektsrapportering.AINNTEKT_BEREGNET_3MND_FRA_OPPRINNELIG_VEDTAK,
                Inntektsrapportering.AINNTEKT_BEREGNET_12MND_FRA_OPPRINNELIG_VEDTAK,
                Inntektsrapportering.LIGNINGSINNTEKT,
                Inntektsrapportering.PENSJON,
                Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER,
                Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT,
                Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET,
            )

        Inntektstype.SYKEPENGER.inngårIInntektRapporteringer() shouldContainAll
            listOf(
                Inntektsrapportering.AINNTEKT,
                Inntektsrapportering.AINNTEKT_BEREGNET_3MND,
                Inntektsrapportering.AINNTEKT_BEREGNET_12MND,
                Inntektsrapportering.AINNTEKT_BEREGNET_3MND_FRA_OPPRINNELIG_VEDTAK,
                Inntektsrapportering.AINNTEKT_BEREGNET_12MND_FRA_OPPRINNELIG_VEDTAK,
                Inntektsrapportering.LIGNINGSINNTEKT,
                Inntektsrapportering.SYKEPENGER,
                Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER,
                Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT,
                Inntektsrapportering.YTELSE_FRA_OFFENTLIG_MANUELT_BEREGNET,
            )

        Inntektstype.KONTANTSTØTTE.inngårIInntektRapporteringer() shouldBe
            listOf(
                Inntektsrapportering.KONTANTSTØTTE,
            )

        Inntektstype.SMÅBARNSTILLEGG.inngårIInntektRapporteringer() shouldBe
            listOf(
                Inntektsrapportering.SMÅBARNSTILLEGG,
            )

        Inntektstype.UTVIDET_BARNETRYGD.inngårIInntektRapporteringer() shouldBe
            listOf(
                Inntektsrapportering.UTVIDET_BARNETRYGD,
            )

        Inntektstype.KAPITALINNTEKT.inngårIInntektRapporteringer() shouldBe
            listOf(
                Inntektsrapportering.KAPITALINNTEKT,
                Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER,
                Inntektsrapportering.SKJØNN_MANGLER_DOKUMENTASJON,
                Inntektsrapportering.SKJØNN_MANGLENDE_BRUK_AV_EVNE,
            )

        Inntektstype.LØNNSINNTEKT.inngårIInntektRapporteringer() shouldContainAll
            listOf(
                Inntektsrapportering.AINNTEKT,
                Inntektsrapportering.AINNTEKT_BEREGNET_3MND,
                Inntektsrapportering.AINNTEKT_BEREGNET_12MND,
                Inntektsrapportering.AINNTEKT_BEREGNET_3MND_FRA_OPPRINNELIG_VEDTAK,
                Inntektsrapportering.AINNTEKT_BEREGNET_12MND_FRA_OPPRINNELIG_VEDTAK,
                Inntektsrapportering.LIGNINGSINNTEKT,
                Inntektsrapportering.PERSONINNTEKT_EGNE_OPPLYSNINGER,
                Inntektsrapportering.SAKSBEHANDLER_BEREGNET_INNTEKT,
                Inntektsrapportering.LØNN_MANUELT_BEREGNET,
            )

        Inntektstype.NÆRINGSINNTEKT.inngårIInntektRapporteringer() shouldBe
            listOf(
                Inntektsrapportering.NÆRINGSINNTEKT_MANUELT_BEREGNET,
                Inntektsrapportering.SKJØNN_MANGLER_DOKUMENTASJON,
                Inntektsrapportering.SKJØNN_MANGLENDE_BRUK_AV_EVNE,
            )
    }
}

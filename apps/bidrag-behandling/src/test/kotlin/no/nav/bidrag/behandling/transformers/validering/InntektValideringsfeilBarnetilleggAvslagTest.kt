package no.nav.bidrag.behandling.transformers.validering

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.transformers.behandling.hentInntekterValideringsfeil
import no.nav.bidrag.behandling.utils.testdata.opprettGyldigBehandlingForBeregningOgVedtak
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import org.junit.jupiter.api.Test
import java.time.YearMonth

class InntektValideringsfeilBarnetilleggAvslagTest {
    private fun behandlingMedUgyldigBarnetillegg(): Behandling {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(true, typeBehandling = TypeBehandling.BIDRAG)
        val barn = behandling.søknadsbarn.first()
        behandling.inntekter.clear()
        val fom = YearMonth.from(behandling.virkningstidspunkt)
        listOf(fom to null, fom.plusMonths(1) to null).forEach { (periodeFom, periodeTom) ->
            behandling.inntekter.add(
                opprettInntekt(
                    periodeFom,
                    periodeTom,
                    rolle = behandling.bidragsmottaker!!,
                    gjelderBarn = barn,
                    type = Inntektsrapportering.BARNETILLEGG,
                    behandling = behandling,
                ),
            )
        }
        return behandling
    }

    @Test
    fun `skal gi valideringsfeil for barnetillegg uten avslag`() {
        val behandling = behandlingMedUgyldigBarnetillegg()

        behandling.hentInntekterValideringsfeil(behandling.bidragsmottaker).barnetillegg.shouldNotBeNull().shouldNotBeEmpty()
    }

    @Test
    fun `skal ikke gi valideringsfeil for barnetillegg til barn med avslag`() {
        val behandling = behandlingMedUgyldigBarnetillegg()
        behandling.søknadsbarn.forEach { it.avslag = Resultatkode.AVSLAG }

        behandling.hentInntekterValideringsfeil().barnetillegg.shouldBeNull()
        behandling.hentInntekterValideringsfeil(behandling.bidragsmottaker).barnetillegg.shouldNotBeNull().shouldBeEmpty()
    }
}

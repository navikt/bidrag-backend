package no.nav.bidrag.behandling.transformers.validering

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.database.datamodell.PrivatAvtale
import no.nav.bidrag.behandling.database.datamodell.Rolle
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordeling
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordelingRolle
import no.nav.bidrag.behandling.dto.v2.samvær.mapValideringsfeil
import no.nav.bidrag.behandling.transformers.behandling.hentInntekterValideringsfeil
import no.nav.bidrag.behandling.transformers.beregning.ValiderBeregning
import no.nav.bidrag.behandling.transformers.løperBidragEtterEldsteVirkning
import no.nav.bidrag.behandling.transformers.underhold.valider
import no.nav.bidrag.behandling.utils.stubPersonConsumer
import no.nav.bidrag.behandling.utils.testdata.opprettGyldigBehandlingForBeregningOgVedtak
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth

class RevurderingsbarnKreverGrunnlagTest {
    @BeforeEach
    fun init() {
        stubPersonConsumer()
        mockkStatic("no.nav.bidrag.behandling.transformers.FellesKt")
        settLøperBidrag(false)
    }

    @AfterEach
    fun cleanup() {
        unmockkObject(AppContext)
        unmockkAll()
    }

    private fun settLøperBidrag(løper: Boolean) {
        every { any<Behandling>().løperBidragEtterEldsteVirkning(any()) } returns løper
    }

    private fun opprettBehandlingMedFF(erRevurdering: Boolean): Pair<Behandling, Rolle> {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(true, typeBehandling = TypeBehandling.BIDRAG)
        behandling.forholdsmessigFordeling = ForholdsmessigFordeling(erHovedbehandling = true)
        val barn = behandling.søknadsbarn.first()
        barn.forholdsmessigFordeling =
            ForholdsmessigFordelingRolle(
                tilhørerSak = behandling.saksnummer,
                behandlerenhet = behandling.behandlerEnhet,
                delAvOpprinneligBehandling = !erRevurdering,
                erRevurdering = erRevurdering,
                bidragsmottaker = behandling.bidragsmottaker!!.ident,
            )
        return behandling to barn
    }

    // kreverGrunnlagForBeregning

    @Test
    fun `revurderingsbarn uten avslag og uten løpende bidrag krever ikke grunnlag`() {
        val (_, barn) = opprettBehandlingMedFF(erRevurdering = true)
        barn.avslag.shouldBeNull()
        barn.erRevurderingsbarn.shouldBeTrue()
        barn.kreverGrunnlagForBeregning.shouldBeFalse()
    }

    @Test
    fun `revurderingsbarn med løpende bidrag krever grunnlag`() {
        val (_, barn) = opprettBehandlingMedFF(erRevurdering = true)
        settLøperBidrag(true)
        barn.kreverGrunnlagForBeregning.shouldBeTrue()
    }

    @Test
    fun `søknadsbarn uten avslag krever grunnlag`() {
        val (_, barn) = opprettBehandlingMedFF(erRevurdering = false)
        barn.kreverGrunnlagForBeregning.shouldBeTrue()
    }

    @Test
    fun `søknadsbarn med avslag krever ikke grunnlag med mindre bidrag løper i FF`() {
        val (_, barn) = opprettBehandlingMedFF(erRevurdering = false)
        barn.avslag = Resultatkode.AVSLAG
        barn.kreverGrunnlagForBeregning.shouldBeFalse()
        settLøperBidrag(true)
        barn.kreverGrunnlagForBeregning.shouldBeTrue()
    }

    @Test
    fun `gebyrsøknad gir ikke lenger krav om grunnlag for beregning, kun for gebyr`() {
        val (_, barn) = opprettBehandlingMedFF(erRevurdering = true)
        barn.harGebyrsøknad = true
        barn.kreverGrunnlagForBeregning.shouldBeFalse()
        barn.kreverGrunnlagForGebyrBeregning.shouldBeTrue()
    }

    @Test
    fun `bidragsmottaker og bidragspliktig krever alltid grunnlag`() {
        val (behandling, _) = opprettBehandlingMedFF(erRevurdering = true)
        behandling.bidragsmottaker!!.kreverGrunnlagForBeregning.shouldBeTrue()
        behandling.bidragspliktig!!.kreverGrunnlagForBeregning.shouldBeTrue()
    }

    // Samvær

    @Test
    fun `skal ikke gi samværsvalideringsfeil for revurderingsbarn som ikke krever grunnlag`() {
        val (behandling, barn) = opprettBehandlingMedFF(erRevurdering = true)
        val samvær = behandling.samvær.find { it.rolle.id == barn.id }.shouldNotBeNull()
        samvær.perioder.clear()

        samvær.mapValideringsfeil().harFeil.shouldBeFalse()
    }

    @Test
    fun `skal gi samværsvalideringsfeil for revurderingsbarn med løpende bidrag`() {
        val (behandling, barn) = opprettBehandlingMedFF(erRevurdering = true)
        settLøperBidrag(true)
        val samvær = behandling.samvær.find { it.rolle.id == barn.id }.shouldNotBeNull()
        samvær.perioder.clear()

        samvær.mapValideringsfeil().harFeil.shouldBeTrue()
    }

    @Test
    fun `skal gi samværsvalideringsfeil for avslått barn når bidrag løper i FF`() {
        val (behandling, barn) = opprettBehandlingMedFF(erRevurdering = false)
        barn.avslag = Resultatkode.AVSLAG
        settLøperBidrag(true)
        val samvær = behandling.samvær.find { it.rolle.id == barn.id }.shouldNotBeNull()
        samvær.perioder.clear()

        samvær.mapValideringsfeil().harFeil.shouldBeTrue()
    }

    // Underholdskostnad

    @Test
    fun `skal ikke gi underholdsvalideringsfeil for revurderingsbarn som ikke krever grunnlag`() {
        val (behandling, barn) = opprettBehandlingMedFF(erRevurdering = true)
        val underhold = behandling.underholdskostnader.find { it.rolle?.id == barn.id }.shouldNotBeNull()
        underhold.harTilsynsordning = true

        behandling.underholdskostnader.valider().shouldBeEmpty()

        settLøperBidrag(true)
        behandling.underholdskostnader.valider().shouldHaveSize(1)
    }

    // Barnetillegg (inntekter)

    private fun Behandling.leggTilUgyldigBarnetillegg(barn: Rolle) {
        inntekter.clear()
        inntekter.add(
            opprettInntekt(
                YearMonth.now().plusMonths(2),
                null,
                rolle = bidragsmottaker!!,
                gjelderBarn = barn,
                type = Inntektsrapportering.BARNETILLEGG,
                behandling = this,
            ),
        )
    }

    @Test
    fun `skal ikke gi barnetilleggsfeil for revurderingsbarn som ikke krever grunnlag`() {
        val (behandling, barn) = opprettBehandlingMedFF(erRevurdering = true)
        behandling.leggTilUgyldigBarnetillegg(barn)

        behandling.hentInntekterValideringsfeil().barnetillegg.shouldBeNull()
    }

    @Test
    fun `skal gi barnetilleggsfeil for revurderingsbarn med gebyrsøknad`() {
        val (behandling, barn) = opprettBehandlingMedFF(erRevurdering = true)
        barn.harGebyrsøknad = true
        behandling.leggTilUgyldigBarnetillegg(barn)

        behandling.hentInntekterValideringsfeil().barnetillegg.shouldNotBeNull()
    }
}

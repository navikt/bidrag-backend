package no.nav.bidrag.behandling.transformers

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.database.datamodell.PrivatAvtale
import no.nav.bidrag.behandling.database.datamodell.PrivatAvtalePeriode
import no.nav.bidrag.behandling.utils.stubPersonConsumer
import no.nav.bidrag.behandling.utils.testdata.opprettGyldigBehandlingForBeregningOgVedtak
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.privatavtale.PrivatAvtaleType
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class LøperBidragEtterEldsteVirkningTest {
    private lateinit var behandling: Behandling
    private lateinit var virkning: YearMonth

    @BeforeEach
    fun init() {
        stubPersonConsumer()
        behandling = opprettGyldigBehandlingForBeregningOgVedtak(true, typeBehandling = TypeBehandling.BIDRAG)
        virkning = YearMonth.from(behandling.eldsteVirkningstidspunkt)
        mockkStatic("no.nav.bidrag.behandling.transformers.FellesKt")
    }

    @AfterEach
    fun cleanup() {
        unmockkObject(AppContext)
        unmockkAll()
    }

    private fun løperBidrag(vararg perioder: ÅrMånedsperiode): Boolean {
        val barn = behandling.søknadsbarn.first()
        every { any<Behandling>().finnPerioderHvorDetLøperBidrag(any()) } returns perioder.toList()
        return behandling.løperBidragEtterEldsteVirkning(barn)
    }

    private fun leggTilPrivatAvtale(vararg perioder: Pair<LocalDate, LocalDate?>) {
        val barn = behandling.søknadsbarn.first()
        val privatAvtale =
            PrivatAvtale(
                behandling = behandling,
                rolle = barn,
                avtaleType = PrivatAvtaleType.PRIVAT_AVTALE,
                stønadstype = barn.stønadstype,
            )
        privatAvtale.perioder.addAll(
            perioder.map { (fom, tom) ->
                PrivatAvtalePeriode(privatAvtale = privatAvtale, fom = fom, tom = tom, beløp = BigDecimal(1000))
            },
        )
        behandling.privatAvtale.add(privatAvtale)
    }

    @Test
    fun `skal løpe når det kun finnes åpen privat avtale`() {
        leggTilPrivatAvtale(virkning.minusMonths(6).atDay(1) to null)
        løperBidrag().shouldBeTrue()
    }

    @Test
    fun `skal ikke løpe når privat avtale slutter måneden før eldste virkningstidspunkt`() {
        leggTilPrivatAvtale(virkning.minusMonths(6).atDay(1) to virkning.minusMonths(1).atEndOfMonth())
        løperBidrag().shouldBeFalse()
    }

    @Test
    fun `skal løpe når privat avtale slutter i måneden for eldste virkningstidspunkt`() {
        leggTilPrivatAvtale(virkning.minusMonths(6).atDay(1) to virkning.atEndOfMonth())
        løperBidrag().shouldBeTrue()
    }

    @Test
    fun `skal løpe når vedtak er åpent selv om privat avtale er avsluttet`() {
        leggTilPrivatAvtale(virkning.minusMonths(12).atDay(1) to virkning.minusMonths(6).atEndOfMonth())
        løperBidrag(ÅrMånedsperiode(virkning.minusMonths(6), null)).shouldBeTrue()
    }

    @Test
    fun `skal løpe når vedtak er avsluttet men privat avtale løper etter eldste virkningstidspunkt`() {
        leggTilPrivatAvtale(virkning.minusMonths(1).atDay(1) to null)
        løperBidrag(ÅrMånedsperiode(virkning.minusMonths(6), virkning.minusMonths(1))).shouldBeTrue()
    }

    @Test
    fun `skal ikke løpe når til-dato er lik eldste virkningstidspunkt fordi til er eksklusiv`() {
        virkning shouldBe YearMonth.of(2023, 2)
        løperBidrag(ÅrMånedsperiode(virkning.minusMonths(6), virkning)).shouldBeFalse()
    }

    @Test
    fun `skal løpe når til-dato er måneden etter eldste virkningstidspunkt`() {
        løperBidrag(ÅrMånedsperiode(virkning.minusMonths(6), virkning.plusMonths(1))).shouldBeTrue()
    }

    @Test
    fun `skal ikke løpe når til-dato er før eldste virkningstidspunkt`() {
        løperBidrag(ÅrMånedsperiode(virkning.minusMonths(6), virkning.minusMonths(1))).shouldBeFalse()
    }

    @Test
    fun `skal løpe når siste periode er åpen`() {
        løperBidrag(ÅrMånedsperiode(virkning.minusMonths(6), null)).shouldBeTrue()
    }

    @Test
    fun `skal ikke løpe når det ikke finnes perioder`() {
        løperBidrag().shouldBeFalse()
    }

    @Test
    fun `skal bruke til-dato fra siste periode`() {
        løperBidrag(
            ÅrMånedsperiode(virkning.minusMonths(12), virkning.minusMonths(8)),
            ÅrMånedsperiode(virkning.minusMonths(4), virkning),
        ).shouldBeFalse()
        løperBidrag(
            ÅrMånedsperiode(virkning.minusMonths(12), virkning.minusMonths(8)),
            ÅrMånedsperiode(virkning.minusMonths(4), virkning.plusMonths(2)),
        ).shouldBeTrue()
    }
}

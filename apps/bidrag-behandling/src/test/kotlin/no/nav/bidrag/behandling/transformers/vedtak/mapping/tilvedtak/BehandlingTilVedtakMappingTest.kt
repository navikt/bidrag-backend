package no.nav.bidrag.behandling.transformers.vedtak.mapping.tilvedtak

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.database.datamodell.Rolle
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordeling
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordelingRolle
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordelingSøknadBarn
import no.nav.bidrag.behandling.utils.testdata.oppretteBehandling
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.BehandlingsrefKilde
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettPeriodeRequestDto
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class BehandlingTilVedtakMappingTest {
    @Test
    fun `skal kun ta med soknadsreferanser for angitte soknadsbarn ved forholdsmessig fordeling`() {
        val behandling = oppretteBehandling(id = 10)
        val barn1 = opprettFFSøknadsbarn(behandling, ident = "20202020201", søknadsid = 111)
        val barn2 = opprettFFSøknadsbarn(behandling, ident = "20202020202", søknadsid = 222)
        behandling.roller = mutableSetOf(barn1, barn2)
        behandling.forholdsmessigFordeling = ForholdsmessigFordeling(null, true)

        val referanser = behandling.tilBehandlingreferanseListe(listOf(barn1))

        referanser
            .filter { it.kilde == BehandlingsrefKilde.BISYS_SØKNAD }
            .map { it.referanse } shouldContainExactlyInAnyOrder listOf("111")
    }

    private fun opprettFFSøknadsbarn(
        behandling: Behandling,
        ident: String,
        søknadsid: Long,
    ): Rolle =
        Rolle(
            ident = ident,
            rolletype = Rolletype.BARN,
            behandling = behandling,
            fødselsdato = LocalDate.of(2010, 3, 20),
            forholdsmessigFordeling =
                ForholdsmessigFordelingRolle(
                    tilhørerSak = "123",
                    behandlerenhet = "13",
                    delAvOpprinneligBehandling = true,
                    erRevurdering = false,
                    bidragsmottaker = "22222222222",
                    søknader =
                        mutableSetOf(
                            ForholdsmessigFordelingSøknadBarn(
                                mottattDato = LocalDate.of(2023, 3, 15),
                                søktAvType = SøktAvType.BIDRAGSMOTTAKER,
                                søknadsid = søknadsid,
                                behandlingstype = null,
                                behandlingstema = null,
                                saksnummer = "123",
                            ),
                        ),
                ),
        )

    @Test
    fun `skal legge til opphørsperiode når siste periode har sluttdato`() {
        val perioder =
            listOf(
                opprettPeriode(YearMonth.of(2025, 1), YearMonth.of(2025, 3)),
            )

        val resultat = perioder.fyllMellomromMedOpphørsperioder()

        resultat shouldBe
            listOf(
                opprettPeriode(YearMonth.of(2025, 1), YearMonth.of(2025, 3)),
                opprettOpphørsperiode(YearMonth.of(2025, 3), null),
            )
    }

    @Test
    fun `skal legge til opphørsperioder mellom perioder og etter siste periode med sluttdato`() {
        val perioder =
            listOf(
                opprettPeriode(YearMonth.of(2025, 1), YearMonth.of(2025, 3)),
                opprettPeriode(YearMonth.of(2025, 5), YearMonth.of(2025, 7)),
            )

        val resultat = perioder.fyllMellomromMedOpphørsperioder()

        resultat shouldBe
            listOf(
                opprettPeriode(YearMonth.of(2025, 1), YearMonth.of(2025, 3)),
                opprettOpphørsperiode(YearMonth.of(2025, 3), YearMonth.of(2025, 5)),
                opprettPeriode(YearMonth.of(2025, 5), YearMonth.of(2025, 7)),
                opprettOpphørsperiode(YearMonth.of(2025, 7), null),
            )
    }

    @Test
    fun `skal ikke legge til trailing opphørsperiode når siste periode er åpen`() {
        val perioder =
            listOf(
                opprettPeriode(YearMonth.of(2025, 1), YearMonth.of(2025, 3)),
                opprettPeriode(YearMonth.of(2025, 5), null),
            )

        val resultat = perioder.fyllMellomromMedOpphørsperioder()

        resultat shouldBe
            listOf(
                opprettPeriode(YearMonth.of(2025, 1), YearMonth.of(2025, 3)),
                opprettOpphørsperiode(YearMonth.of(2025, 3), YearMonth.of(2025, 5)),
                opprettPeriode(YearMonth.of(2025, 5), null),
            )
    }

    private fun opprettPeriode(
        fom: YearMonth,
        til: YearMonth?,
    ): OpprettPeriodeRequestDto = OpprettPeriodeRequestDto(
        periode = ÅrMånedsperiode(fom, til),
        beløp = BigDecimal.ONE,
        valutakode = VALUTAKODE,
        resultatkode = Resultatkode.BEREGNET_BIDRAG.name,
        grunnlagReferanseListe = GRUNNLAGSREFERANSE_LISTE,
    )

    private fun opprettOpphørsperiode(
        fom: YearMonth,
        til: YearMonth?,
    ): OpprettPeriodeRequestDto = OpprettPeriodeRequestDto(
        periode = ÅrMånedsperiode(fom, til),
        beløp = null,
        resultatkode = Resultatkode.OPPHØR.name,
        grunnlagReferanseListe = GRUNNLAGSREFERANSE_LISTE,
    )

    private companion object {
        const val VALUTAKODE = "NOK"
        val GRUNNLAGSREFERANSE_LISTE = listOf("grunnlag-1")
    }
}

package no.nav.bidrag.behandling.transformers.vedtak.mapping.fravedtak

import com.fasterxml.jackson.databind.node.POJONode
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.database.datamodell.Rolle
import no.nav.bidrag.behandling.dto.v2.behandling.Grunnlagsdatatype
import no.nav.bidrag.behandling.utils.testdata.TestDataPerson
import no.nav.bidrag.behandling.utils.testdata.opprettRolle
import no.nav.bidrag.behandling.utils.testdata.oppretteBehandling
import no.nav.bidrag.behandling.utils.testdata.testdataBM
import no.nav.bidrag.behandling.utils.testdata.testdataBP
import no.nav.bidrag.behandling.utils.testdata.testdataBarn1
import no.nav.bidrag.behandling.utils.testdata.testdataBarnBm
import no.nav.bidrag.behandling.utils.testdata.testdataBarnBm2
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.person.Familierelasjon
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.BeløpshistorikkGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.InnhentetAndreBarnTilBidragsmottaker
import no.nav.bidrag.transport.behandling.felles.grunnlag.InnhentetArbeidsforhold
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class VedtakTilBehandlingFlereRollerTest {
    private val referanseBm = "person_bm"
    private val referanseBm2 = "person_bm2"
    private val referanseBp = "person_bp"
    private val referanseBarnBidrag = "person_barn_bidrag"
    private val referanseBarn18År = "person_barn_18aar"
    private val referanseBarnTilBm = "person_barn_til_bm"
    private val referanseBarnTilBm2 = "person_barn_til_bm2"
    private val testdataBM2 = testdataBP.copy(rolletype = Rolletype.BIDRAGSMOTTAKER)
    private val hentetTidspunkt = LocalDateTime.of(2024, 2, 1, 0, 0)
    private val virkningstidspunkt = LocalDate.of(2024, 1, 1)

    @Test
    fun `skal kopiere ainntekt fra opprinnelig vedtakstidspunkt per rolle når samme barn har ulike stønadstyper`() {
        // Given
        val behandling = oppretteBehandling(stønadstype = Stønadstype.BIDRAG)
        val rolleBidrag = opprettBarnRolle(behandling, Stønadstype.BIDRAG, 2L)
        val rolle18År = opprettBarnRolle(behandling, Stønadstype.BIDRAG18AAR, 3L)
        behandling.roller = mutableSetOf(opprettRolle(behandling, testdataBM, 1L), rolleBidrag, rolle18År)
        val grunnlagsliste =
            listOf(
                personGrunnlag(referanseBm, Grunnlagstype.PERSON_BIDRAGSMOTTAKER, testdataBM),
                personGrunnlag(referanseBarnBidrag, Grunnlagstype.PERSON_SØKNADSBARN, testdataBarn1, Stønadstype.BIDRAG),
                personGrunnlag(referanseBarn18År, Grunnlagstype.PERSON_SØKNADSBARN, testdataBarn1, Stønadstype.BIDRAG18AAR),
                ainntektBeregnetGrunnlag(referanseBarnBidrag),
                ainntektBeregnetGrunnlag(referanseBarn18År),
            )

        // When
        val inntekter = grunnlagsliste.mapInntekter(behandling, lesemodus = false)

        // Then
        assertSoftly(inntekter.filter { it.type == Inntektsrapportering.AINNTEKT_BEREGNET_12MND_FRA_OPPRINNELIG_VEDTAKSTIDSPUNKT }) {
            shouldHaveSize(2)
            map { it.rolle } shouldContainExactlyInAnyOrder listOf(rolleBidrag, rolle18År)
            forEach { it.taMed shouldBe true }
        }
        assertSoftly(inntekter.filter { it.type == Inntektsrapportering.AINNTEKT_BEREGNET_12MND }) {
            shouldHaveSize(2)
            map { it.rolle } shouldContainExactlyInAnyOrder listOf(rolleBidrag, rolle18År)
            forEach { it.taMed shouldBe false }
        }
    }

    @Test
    fun `skal mappe beløpshistorikk for hver stønadstype til riktig rolle når samme barn har ulike stønadstyper`() {
        // Given
        val behandling = oppretteBehandling(stønadstype = Stønadstype.BIDRAG)
        val rolleBidrag = opprettBarnRolle(behandling, Stønadstype.BIDRAG, 3L)
        val rolle18År = opprettBarnRolle(behandling, Stønadstype.BIDRAG18AAR, 4L)
        behandling.roller =
            mutableSetOf(
                opprettRolle(behandling, testdataBM, 1L),
                opprettRolle(behandling, testdataBP, 2L),
                rolleBidrag,
                rolle18År,
            )
        val grunnlagsliste =
            listOf(
                personGrunnlag(referanseBm, Grunnlagstype.PERSON_BIDRAGSMOTTAKER, testdataBM),
                personGrunnlag(referanseBp, Grunnlagstype.PERSON_BIDRAGSPLIKTIG, testdataBP),
                personGrunnlag(referanseBarnBidrag, Grunnlagstype.PERSON_SØKNADSBARN, testdataBarn1, Stønadstype.BIDRAG),
                personGrunnlag(referanseBarn18År, Grunnlagstype.PERSON_SØKNADSBARN, testdataBarn1, Stønadstype.BIDRAG18AAR),
                beløpshistorikkGrunnlag(Grunnlagstype.BELØPSHISTORIKK_BIDRAG, referanseBarnBidrag),
                beløpshistorikkGrunnlag(Grunnlagstype.BELØPSHISTORIKK_BIDRAG_18_ÅR, referanseBarn18År),
            )

        // When
        val resultat = grunnlagsliste.mapGrunnlag(behandling, lesemodus = true)

        // Then
        assertSoftly(resultat.filter { it.type == Grunnlagsdatatype.BELØPSHISTORIKK_BIDRAG }) {
            shouldHaveSize(1)
            first().rolle shouldBe rolleBidrag
        }
        assertSoftly(resultat.filter { it.type == Grunnlagsdatatype.BELØPSHISTORIKK_BIDRAG_18_ÅR }) {
            shouldHaveSize(1)
            first().rolle shouldBe rolle18År
        }
    }

    @Test
    fun `skal mappe arbeidsforhold og andre barn for hver bidragsmottaker`() {
        // Given
        val behandling = oppretteBehandling(stønadstype = Stønadstype.BIDRAG)
        val rolleBm = opprettRolle(behandling, testdataBM, 1L)
        val rolleBm2 = opprettRolle(behandling, testdataBM2, 2L)
        behandling.roller = mutableSetOf(rolleBm, rolleBm2, opprettBarnRolle(behandling, Stønadstype.BIDRAG, 3L))
        val grunnlagsliste =
            listOf(
                personGrunnlag(referanseBm, Grunnlagstype.PERSON_BIDRAGSMOTTAKER, testdataBM),
                personGrunnlag(referanseBm2, Grunnlagstype.PERSON_BIDRAGSMOTTAKER, testdataBM2),
                personGrunnlag(referanseBarnBidrag, Grunnlagstype.PERSON_SØKNADSBARN, testdataBarn1, Stønadstype.BIDRAG),
                personGrunnlag(referanseBarnTilBm, Grunnlagstype.PERSON_BARN_BIDRAGSMOTTAKER, testdataBarnBm),
                personGrunnlag(referanseBarnTilBm2, Grunnlagstype.PERSON_BARN_BIDRAGSMOTTAKER, testdataBarnBm2),
                arbeidsforholdGrunnlag(referanseBm),
                arbeidsforholdGrunnlag(referanseBm2),
                andreBarnGrunnlag(referanseBm, referanseBarnTilBm, testdataBarnBm),
                andreBarnGrunnlag(referanseBm2, referanseBarnTilBm2, testdataBarnBm2),
            )

        // When
        val resultat = grunnlagsliste.mapGrunnlag(behandling, lesemodus = true)

        // Then
        assertSoftly(resultat.filter { it.type == Grunnlagsdatatype.ARBEIDSFORHOLD }) {
            shouldHaveSize(2)
            map { it.rolle } shouldContainExactlyInAnyOrder listOf(rolleBm, rolleBm2)
        }
        assertSoftly(resultat.filter { it.type == Grunnlagsdatatype.ANDRE_BARN }) {
            shouldHaveSize(2)
            map { it.rolle } shouldContainExactlyInAnyOrder listOf(rolleBm, rolleBm2)
            first { it.rolle == rolleBm2 }.data.contains(testdataBarnBm2.ident) shouldBe true
            first { it.rolle == rolleBm2 }.data.contains(testdataBarnBm.ident) shouldBe false
        }
    }

    @Test
    fun `skal knytte underholdskostnad for andre barn til riktig bidragsmottaker`() {
        // Given
        val behandling = oppretteBehandling(stønadstype = Stønadstype.BIDRAG)
        val rolleBm = opprettRolle(behandling, testdataBM, 1L)
        val rolleBm2 = opprettRolle(behandling, testdataBM2, 2L)
        behandling.roller = mutableSetOf(rolleBm, rolleBm2, opprettBarnRolle(behandling, Stønadstype.BIDRAG, 3L))
        val barnTilBm = testdataBarnBm.copy(fødselsdato = LocalDate.of(2020, 1, 1))
        val barnTilBm2 = testdataBarnBm2.copy(fødselsdato = LocalDate.of(2021, 1, 1))
        val grunnlagsliste =
            listOf(
                personGrunnlag(referanseBm, Grunnlagstype.PERSON_BIDRAGSMOTTAKER, testdataBM),
                personGrunnlag(referanseBm2, Grunnlagstype.PERSON_BIDRAGSMOTTAKER, testdataBM2),
                personGrunnlag(referanseBarnBidrag, Grunnlagstype.PERSON_SØKNADSBARN, testdataBarn1, Stønadstype.BIDRAG),
                personGrunnlag(referanseBarnTilBm, Grunnlagstype.PERSON_BARN_BIDRAGSMOTTAKER, barnTilBm),
                personGrunnlag(referanseBarnTilBm2, Grunnlagstype.PERSON_BARN_BIDRAGSMOTTAKER, barnTilBm2),
                andreBarnGrunnlag(referanseBm, referanseBarnTilBm, barnTilBm),
                andreBarnGrunnlag(referanseBm2, referanseBarnTilBm2, barnTilBm2),
            )
        val mapping = VedtakTilBehandlingMapping(mockk(), mockk(), mockk())

        // When
        val underholdskostnader =
            with(mapping) {
                grunnlagsliste.hentAndreBarnUnderholdskostnadPerioder(behandling, lesemodus = true, virkningstidspunkt)
            }

        // Then
        assertSoftly(underholdskostnader) {
            shouldHaveSize(2)
            first { it.person?.ident == barnTilBm.ident }.rolle shouldBe rolleBm
            first { it.person?.ident == barnTilBm2.ident }.rolle shouldBe rolleBm2
        }
    }

    private fun opprettBarnRolle(
        behandling: Behandling,
        stønadstype: Stønadstype,
        id: Long,
    ): Rolle = opprettRolle(behandling, testdataBarn1, id).also { it.stønadstype = stønadstype }

    private fun personGrunnlag(
        referanse: String,
        type: Grunnlagstype,
        person: TestDataPerson,
        stønadstype: Stønadstype? = null,
    ) = GrunnlagDto(
        referanse = referanse,
        type = type,
        innhold =
        POJONode(
            Person(
                ident = Personident(person.ident),
                navn = person.navn,
                fødselsdato = person.fødselsdato,
                stønadstype = stønadstype,
            ),
        ),
    )

    private fun ainntektBeregnetGrunnlag(gjelderReferanse: String) = GrunnlagDto(
        referanse = "inntekt_$gjelderReferanse",
        type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
        gjelderReferanse = gjelderReferanse,
        innhold =
        POJONode(
            InntektsrapporteringPeriode(
                periode = ÅrMånedsperiode(YearMonth.of(2024, 1), null),
                opprinneligPeriode = ÅrMånedsperiode(YearMonth.of(2023, 1), YearMonth.of(2024, 1)),
                manueltRegistrert = false,
                inntektsrapportering = Inntektsrapportering.AINNTEKT_BEREGNET_12MND,
                beløp = BigDecimal(10000),
                valgt = true,
            ),
        ),
    )

    private fun beløpshistorikkGrunnlag(
        type: Grunnlagstype,
        gjelderBarnReferanse: String,
    ) = GrunnlagDto(
        referanse = "${type}_$gjelderBarnReferanse",
        type = type,
        gjelderReferanse = referanseBp,
        gjelderBarnReferanse = gjelderBarnReferanse,
        innhold = POJONode(BeløpshistorikkGrunnlag(tidspunktInnhentet = hentetTidspunkt, beløpshistorikk = emptyList())),
    )

    private fun arbeidsforholdGrunnlag(gjelderReferanse: String) = GrunnlagDto(
        referanse = "arbeidsforhold_$gjelderReferanse",
        type = Grunnlagstype.INNHENTET_ARBEIDSFORHOLD,
        gjelderReferanse = gjelderReferanse,
        innhold =
        POJONode(
            InnhentetArbeidsforhold(
                hentetTidspunkt = hentetTidspunkt,
                grunnlag =
                listOf(
                    InnhentetArbeidsforhold.Arbeidsforhold(
                        startdato = LocalDate.of(2020, 1, 1),
                        sluttdato = null,
                        arbeidsgiverNavn = "Arbeidsgiver $gjelderReferanse",
                        arbeidsgiverOrgnummer = null,
                    ),
                ),
            ),
        ),
    )

    private fun andreBarnGrunnlag(
        bidragsmottakerReferanse: String,
        barnReferanse: String,
        barn: TestDataPerson,
    ) = GrunnlagDto(
        referanse = "andre_barn_$bidragsmottakerReferanse",
        type = Grunnlagstype.INNHENTET_ANDRE_BARN_TIL_BIDRAGSMOTTAKER,
        gjelderReferanse = bidragsmottakerReferanse,
        innhold =
        POJONode(
            InnhentetAndreBarnTilBidragsmottaker(
                hentetTidspunkt = hentetTidspunkt,
                grunnlag =
                listOf(
                    InnhentetAndreBarnTilBidragsmottaker.AndreBarnTilBidragsmottakerPDL(
                        gjelderPerson = barnReferanse,
                        relasjon = Familierelasjon.BARN,
                        navn = barn.navn,
                        fødselsdato = barn.fødselsdato,
                    ),
                ),
            ),
        ),
    )
}

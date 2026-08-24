package no.nav.bidrag.dokument.bestilling.bestilling

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.dokument.bestilling.bestilling.dto.beskrivelse
import no.nav.bidrag.dokument.bestilling.bestilling.dto.hentDokumentMal
import no.nav.bidrag.dokument.bestilling.consumer.BidragVedtakConsumer
import no.nav.bidrag.dokument.bestilling.consumer.SjablonConsumer
import no.nav.bidrag.dokument.bestilling.consumer.dto.SjablongerDto
import no.nav.bidrag.dokument.bestilling.model.SpråkKoder
import no.nav.bidrag.dokument.bestilling.model.tilLocalDateFom
import no.nav.bidrag.dokument.bestilling.model.tilLocalDateTil
import no.nav.bidrag.dokument.bestilling.model.typeRef
import no.nav.bidrag.dokument.bestilling.tjenester.PersonService
import no.nav.bidrag.dokument.bestilling.tjenester.SjablongService
import no.nav.bidrag.dokument.bestilling.tjenester.VedtakService
import no.nav.bidrag.dokument.bestilling.utils.BARN1
import no.nav.bidrag.dokument.bestilling.utils.BARN2
import no.nav.bidrag.dokument.bestilling.utils.BM1
import no.nav.bidrag.dokument.bestilling.utils.BP1
import no.nav.bidrag.dokument.bestilling.utils.FORSKUDDSATS_2024_2025
import no.nav.bidrag.dokument.bestilling.utils.FORSKUDD_INNTEKTGRENSE_2021_2022
import no.nav.bidrag.dokument.bestilling.utils.FORSKUDD_INNTEKTGRENSE_2022_2023
import no.nav.bidrag.dokument.bestilling.utils.FORSKUDD_INNTEKTGRENSE_2024_2025
import no.nav.bidrag.dokument.bestilling.utils.FORSKUDD_MAKS_INNTEKT_FORSKUDD_MOTTAKER_2021_2022
import no.nav.bidrag.dokument.bestilling.utils.FORSKUDD_MAKS_INNTEKT_FORSKUDD_MOTTAKER_2022_2023
import no.nav.bidrag.dokument.bestilling.utils.SAKSBEHANDLER_IDENT
import no.nav.bidrag.dokument.bestilling.utils.SAKSBEHANDLER_NAVN
import no.nav.bidrag.dokument.bestilling.utils.createPostAdresseResponse
import no.nav.bidrag.dokument.bestilling.utils.lagVedtaksdata
import no.nav.bidrag.dokument.bestilling.utils.readFile
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.domene.enums.person.Sivilstandskode
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.enums.vedtak.VirkningstidspunktÅrsakstype
import no.nav.bidrag.transport.dokumentmaler.InntektPeriode
import no.nav.bidrag.transport.dokumentmaler.PeriodeFraTom
import no.nav.bidrag.transport.dokumentmaler.VedtakBarnStonad
import no.nav.bidrag.transport.dokumentmaler.VedtakDetaljer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
@Disabled("Må tilpasse for ny DTO og grunnlagstruktur")
class VedtakServiceTest {
    @MockK
    lateinit var personService: PersonService

    @MockK
    lateinit var vedtakConsumer: BidragVedtakConsumer

    @MockK
    lateinit var sjablonConsumer: SjablonConsumer

    @InjectMockKs
    lateinit var sjablongService: SjablongService

    @InjectMockKs
    lateinit var vedtakService: VedtakService

    @BeforeEach
    fun initMocks() {
        val sjablonResponse = ObjectMapper().findAndRegisterModules().readValue(readFile("api/sjablon_all.json"), typeRef<SjablongerDto>())
        every { sjablonConsumer.hentSjablonger() } returns sjablonResponse
    }

    fun mockDefaultValues() {
        every { personService.hentPerson(BM1.ident.verdi, any()) } returns BM1
        every { personService.hentPerson(BP1.ident.verdi, any()) } returns BP1
        every { personService.hentPerson(BARN1.ident.verdi, any()) } returns BARN1
        every { personService.hentPerson(BARN2.ident.verdi, any()) } returns BARN2
        every { personService.hentSpråk(any()) } returns "NB"
        every { personService.hentPersonAdresse(any(), any()) } returns createPostAdresseResponse()
    }

    @Test
    fun `skal mappe vedtakdetaljer for vedtak forskudd`() {
        mockDefaultValues()
        val virkningDato = LocalDate.parse("2023-01-01")
        val vedtakResponse = lagVedtaksdata("vedtak/vedtak_forskudd_flere_ytelser.json")
        every { vedtakConsumer.hentVedtak(eq(108)) } returns vedtakResponse

        val vedtakDetaljer = vedtakService.hentVedtakDetaljer(108, hentDokumentMal("BI01S02")!!, SpråkKoder.BOKMAL, null)

        assertSoftly {
            vedtakDetaljer shouldNotBe null

            vedtakDetaljer.kilde shouldBe Vedtakskilde.MANUELT
            vedtakDetaljer.vedtakstype shouldBe Vedtakstype.FASTSETTELSE
            vedtakDetaljer.stønadstype shouldBe Stønadstype.FORSKUDD
            vedtakDetaljer.søknadFra shouldBe SøktAvType.BIDRAGSMOTTAKER
            vedtakDetaljer.årsakKode shouldBe VirkningstidspunktÅrsakstype.FRA_SØKNADSTIDSPUNKT
            vedtakDetaljer.virkningstidspunkt!! shouldBe virkningDato
            vedtakDetaljer.mottattDato!! shouldBe LocalDate.parse("2024-07-15")
            vedtakDetaljer.soktFraDato!! shouldBe LocalDate.parse("2023-01-01")
            vedtakDetaljer.vedtattDato!! shouldBe LocalDate.parse("2024-08-05")
            vedtakDetaljer.saksbehandlerInfo.ident shouldBe SAKSBEHANDLER_IDENT
            vedtakDetaljer.saksbehandlerInfo.navn shouldBe SAKSBEHANDLER_NAVN

            // Sivilstand
            vedtakDetaljer.sivilstandPerioder shouldHaveSize 2
            val sivilstandPeriode = vedtakDetaljer.hentSivilstandPeriodeForDato(PeriodeFraTom(virkningDato))!!
            sivilstandPeriode.periode.tilLocalDateFom() shouldBe LocalDate.parse("2023-01-01")
            sivilstandPeriode.periode.tilLocalDateTil() shouldBe LocalDate.parse("2024-01-31")
            sivilstandPeriode.sivilstand shouldBe Sivilstandskode.BOR_ALENE_MED_BARN

            // Bostatus perioder
            vedtakDetaljer.barnIHusstandPerioder shouldHaveSize 2
            val barnIHustandPeriode = vedtakDetaljer.hentBarnIHustandPeriodeForDato(virkningDato)!!
            barnIHustandPeriode.periode.tilLocalDateFom() shouldBe LocalDate.parse("2023-01-01")
            barnIHustandPeriode.periode.tilLocalDateTil() shouldBe LocalDate.parse("2023-10-31")
            barnIHustandPeriode.antall shouldBe 4.0

            vedtakDetaljer.vedtakBarn shouldHaveSize 2
            val barn1 = vedtakDetaljer.vedtakBarn[0]
            val barn1Bostatus = barn1.bostatusPerioder[0]
            barn1Bostatus.periode.tilLocalDateFom() shouldBe LocalDate.parse("2023-01-01")
            barn1Bostatus.periode.til shouldBe null
            barn1Bostatus.bostatus shouldBe Bostatuskode.MED_FORELDER

            val barn1StonadPeriode = barn1.stønadsendringer[0]
            barn1StonadPeriode.type shouldBe Stønadstype.FORSKUDD
            barn1StonadPeriode.vedtakPerioder shouldHaveSize 9
            val barnVedtakPeriode = barn1StonadPeriode.vedtakPerioder[0]
            barnVedtakPeriode.fomDato shouldBe LocalDate.parse("2023-01-01")
            barnVedtakPeriode.tomDato shouldBe LocalDate.parse("2023-03-31")
            barnVedtakPeriode.beløp shouldBe BigDecimal(1320)
            barnVedtakPeriode.resultatKode shouldBe "75"
            barnVedtakPeriode.inntektGrense shouldBe FORSKUDD_INNTEKTGRENSE_2022_2023
            barnVedtakPeriode.maksInntekt shouldBe FORSKUDD_MAKS_INNTEKT_FORSKUDD_MOTTAKER_2022_2023

            // Inntekt periode
            val barn1InntektPeriode1 = barnVedtakPeriode.inntekter[1]
            barn1InntektPeriode1.typer shouldHaveSize 2
            barn1InntektPeriode1.typer shouldContainAll listOf(Inntektsrapportering.OVERGANGSSTØNAD, Inntektsrapportering.FORELDREPENGER)
            barnVedtakPeriode.inntekter.all { it1 -> barnVedtakPeriode.inntekter.filter { it.periode == it1.periode && it.beskrivelse == it1.beskrivelse }.size == 1 }.shouldBeTrue()
        }
    }

    @Test
    fun `skal mappe vedtakdetaljer for vedtak særbidrag`() {
        mockDefaultValues()
        val virkningDato = LocalDate.parse("2024-08-01")
        val vedtakResponse = lagVedtaksdata("vedtak/vedtak_response-særbidrag.json")
        every { vedtakConsumer.hentVedtak(eq(108)) } returns vedtakResponse

        val vedtakDetaljer = vedtakService.hentVedtakDetaljer(108, hentDokumentMal("BI01S02")!!, SpråkKoder.BOKMAL, null)

        assertSoftly {
            vedtakDetaljer shouldNotBe null

            vedtakDetaljer.kilde shouldBe Vedtakskilde.MANUELT
            vedtakDetaljer.vedtakstype shouldBe Vedtakstype.FASTSETTELSE
            vedtakDetaljer.type shouldBe TypeBehandling.SÆRBIDRAG
            vedtakDetaljer.stønadstype.shouldBeNull()
            vedtakDetaljer.søknadFra shouldBe SøktAvType.BIDRAGSMOTTAKER
            vedtakDetaljer.engangsbeløptype shouldBe Engangsbeløptype.SÆRBIDRAG
            vedtakDetaljer.årsakKode.shouldBeNull()
            vedtakDetaljer.virkningstidspunkt!! shouldBe virkningDato
            vedtakDetaljer.mottattDato!! shouldBe LocalDate.parse("2024-01-01")
            vedtakDetaljer.soktFraDato!! shouldBe LocalDate.parse("2024-08-01")
            vedtakDetaljer.vedtattDato!! shouldBe LocalDate.parse("2024-08-06")
            vedtakDetaljer.saksbehandlerInfo.ident shouldBe SAKSBEHANDLER_IDENT
            vedtakDetaljer.saksbehandlerInfo.navn shouldBe SAKSBEHANDLER_NAVN

            // Sivilstand
            vedtakDetaljer.sivilstandPerioder shouldHaveSize 0

            // Bostatus perioder
            vedtakDetaljer.barnIHusstandPerioder shouldHaveSize 1
            val barnIHustandPeriode = vedtakDetaljer.hentBarnIHustandPeriodeForDato(virkningDato)!!
            barnIHustandPeriode.periode.tilLocalDateFom() shouldBe virkningDato
            barnIHustandPeriode.periode.tilLocalDateTil() shouldBe null
            barnIHustandPeriode.antall shouldBe 2.0

            vedtakDetaljer.vedtakBarn shouldHaveSize 1
            val barn1 = vedtakDetaljer.vedtakBarn[0]
            val barn1Bostatus = barn1.bostatusPerioder[0]
            barn1Bostatus.periode.tilLocalDateFom() shouldBe virkningDato
            barn1Bostatus.periode.til shouldBe null
            barn1Bostatus.bostatus shouldBe Bostatuskode.REGNES_IKKE_SOM_BARN

            barn1.stønadsendringer shouldHaveSize 0
            barn1.engangsbeløper shouldHaveSize 1

            val barnEngangsbeløp = barn1.engangsbeløper[0]
            barnEngangsbeløp.type shouldBe Engangsbeløptype.SÆRBIDRAG
            barnEngangsbeløp.periode.fom shouldBe virkningDato
            barnEngangsbeløp.periode.til shouldBe virkningDato.plusMonths(1).withDayOfMonth(1).minusDays(1)
            val inntekter = barnEngangsbeløp.inntekter
            inntekter shouldHaveSize 8

            assertSoftly(inntekter.filter { it.fødselsnummer == BARN2.ident.verdi }) {
                shouldHaveSize(2)
                this[0].typer shouldContainAll listOf(Inntektsrapportering.SKJØNN_MANGLER_DOKUMENTASJON)
                this[0].beskrivelse shouldBe "Skjønn - mangler dokumentasjon"
                this[0].beløp shouldBe BigDecimal(30000)

                val inntektTotal = this[1]
                inntektTotal.periodeTotalinntekt shouldBe true
                inntektTotal.beløp shouldBe BigDecimal(30000)
                inntektTotal.rolle shouldBe Rolletype.BARN
                inntektTotal.beskrivelse shouldBe "Personens beregningsgrunnlag i perioden"
            }
            assertSoftly(inntekter.filter { it.fødselsnummer == BM1.ident.verdi }) {
                shouldHaveSize(2)
                this[0].typer shouldContainAll listOf(Inntektsrapportering.AINNTEKT_BEREGNET_12MND)
                this[0].beskrivelse shouldBe "Opplysninger fra arbeidsgiver"

                val inntektTotal = this[1]
                inntektTotal.periodeTotalinntekt shouldBe true
                inntektTotal.beløp shouldBe BigDecimal(858000)
                inntektTotal.rolle shouldBe Rolletype.BIDRAGSMOTTAKER
                inntektTotal.beskrivelse shouldBe "Personens beregningsgrunnlag i perioden"
            }
            assertSoftly(inntekter.filter { it.fødselsnummer == BP1.ident.verdi }) {
                shouldHaveSize(4)
                val barn1InntektPeriode1 = this[0]
                barn1InntektPeriode1.typer shouldHaveSize 3
                barn1InntektPeriode1.beskrivelse shouldBe "Ytelse fra det offentlige"
                barn1InntektPeriode1.beløp shouldBe BigDecimal(94466)
                barn1InntektPeriode1.typer shouldContainAll listOf(Inntektsrapportering.OVERGANGSSTØNAD, Inntektsrapportering.FORELDREPENGER, Inntektsrapportering.SYKEPENGER)
                this.all { it1 -> this.filter { it.periode == it1.periode && it.beskrivelse == it1.beskrivelse }.size == 1 }.shouldBeTrue()

                val inntektTotal = this[3]
                inntektTotal.periodeTotalinntekt shouldBe true
                inntektTotal.beløp shouldBe BigDecimal(699466)
                inntektTotal.rolle shouldBe Rolletype.BIDRAGSPLIKTIG
                inntektTotal.beskrivelse shouldBe "Personens beregningsgrunnlag i perioden"
            }

            barnEngangsbeløp.medInnkreving shouldBe true
            assertSoftly(barnEngangsbeløp.sjablon) {
                it.forskuddSats shouldBe FORSKUDDSATS_2024_2025.toBigDecimal()
                it.inntektsgrense shouldBe FORSKUDD_INNTEKTGRENSE_2024_2025
            }
            assertSoftly(barnEngangsbeløp.særbidragBeregning!!) {
                it.andelProsent shouldBe 44.9.toBigDecimal()
                it.kravbeløp shouldBe BigDecimal(11000)
                it.godkjentbeløp shouldBe BigDecimal(7000)
                it.beløpDirekteBetaltAvBp shouldBe BigDecimal(1234)
                it.resultat shouldBe BigDecimal(1909)
                it.resultatKode shouldBe Resultatkode.SÆRBIDRAG_INNVILGET

                assertSoftly(it.inntekt) {
                    it.bmInntekt shouldBe BigDecimal(858000)
                    it.bpInntekt shouldBe BigDecimal(699466)
                    it.barnInntekt shouldBe BigDecimal(30000)
                    it.totalInntekt shouldBe BigDecimal(1587466)
                }
            }
            // Inntekt periode
        }
    }

    @Test
    fun `skal mappe vedtakdetaljer for vedtak særbidrag avslag`() {
        mockDefaultValues()
        val virkningDato = LocalDate.parse("2024-08-01")
        val vedtakResponse = lagVedtaksdata("vedtak/vedtak_response-særbidrag-avslag.json")
        every { vedtakConsumer.hentVedtak(eq(108)) } returns vedtakResponse

        val vedtakDetaljer = vedtakService.hentVedtakDetaljer(108, hentDokumentMal("BI01S02")!!, SpråkKoder.BOKMAL, null)

        assertSoftly {
            vedtakDetaljer shouldNotBe null

            vedtakDetaljer.kilde shouldBe Vedtakskilde.MANUELT
            vedtakDetaljer.vedtakstype shouldBe Vedtakstype.ENDRING
            vedtakDetaljer.stønadstype shouldBe null
            vedtakDetaljer.søknadFra shouldBe SøktAvType.BIDRAGSPLIKTIG
            vedtakDetaljer.årsakKode shouldBe null
            vedtakDetaljer.virkningstidspunkt!! shouldBe virkningDato
            vedtakDetaljer.mottattDato!! shouldBe LocalDate.parse("2024-01-15")
            vedtakDetaljer.soktFraDato!! shouldBe LocalDate.parse("2021-01-01")
            vedtakDetaljer.vedtattDato!! shouldBe LocalDate.parse("2024-08-09")
            vedtakDetaljer.saksbehandlerInfo.ident shouldBe SAKSBEHANDLER_IDENT
            vedtakDetaljer.saksbehandlerInfo.navn shouldBe SAKSBEHANDLER_NAVN

            // Sivilstand
            vedtakDetaljer.sivilstandPerioder shouldHaveSize 0

            // Bostatus perioder
            vedtakDetaljer.barnIHusstandPerioder shouldHaveSize 1
            val barnIHustandPeriode = vedtakDetaljer.hentBarnIHustandPeriodeForDato(virkningDato)!!
            barnIHustandPeriode.periode.tilLocalDateFom() shouldBe virkningDato
            barnIHustandPeriode.periode.tilLocalDateTil() shouldBe null
            barnIHustandPeriode.antall shouldBe 2.0

            vedtakDetaljer.vedtakBarn shouldHaveSize 1
            val barn1 = vedtakDetaljer.vedtakBarn[0]
            val barn1Bostatus = barn1.bostatusPerioder[0]
            barn1Bostatus.periode.tilLocalDateFom() shouldBe virkningDato
            barn1Bostatus.periode.til shouldBe null
            barn1Bostatus.bostatus shouldBe Bostatuskode.REGNES_IKKE_SOM_BARN

            val barnEngangsbeløp = barn1.engangsbeløper[0]
            barnEngangsbeløp.type shouldBe Engangsbeløptype.SÆRBIDRAG
            barnEngangsbeløp.periode.fom shouldBe virkningDato
            barnEngangsbeløp.periode.til shouldBe virkningDato.plusMonths(1).withDayOfMonth(1).minusDays(1)
            val inntekter = barnEngangsbeløp.inntekter

            barnEngangsbeløp.medInnkreving shouldBe true
            assertSoftly(barnEngangsbeløp.sjablon) {
                it.forskuddSats shouldBe FORSKUDDSATS_2024_2025.toBigDecimal().setScale(1)
                it.inntektsgrense shouldBe FORSKUDD_INNTEKTGRENSE_2024_2025
            }
            assertSoftly(barnEngangsbeløp.særbidragBeregning!!) {
                it.andelProsent shouldBe 0.toBigDecimal()
                it.kravbeløp shouldBe BigDecimal(27000)
                it.godkjentbeløp shouldBe BigDecimal(24000)
                it.beløpDirekteBetaltAvBp shouldBe BigDecimal(0)
                it.resultat shouldBe BigDecimal(0)
                it.resultatKode shouldBe no.nav.bidrag.domene.enums.beregning.Resultatkode.SÆRBIDRAG_IKKE_FULL_BIDRAGSEVNE

                assertSoftly(it.inntekt) {
                    it.bmInntekt shouldBe 625111.0.toBigDecimal()
                    it.bpInntekt shouldBe 421000.0.toBigDecimal()
                    it.barnInntekt shouldBe 20000.0.toBigDecimal()
                    it.totalInntekt shouldBe 1066111.0.toBigDecimal()
                }
            }
            inntekter shouldHaveSize 8

            inntekter.filter { it.fødselsnummer == BM1.ident.verdi } shouldHaveSize 3
            inntekter.filter { it.fødselsnummer == BP1.ident.verdi } shouldHaveSize 3
            inntekter.filter { it.fødselsnummer == BARN2.ident.verdi } shouldHaveSize 2
        }
    }

    fun List<InntektPeriode>.hentGrunnlag() = find { it.periodeTotalinntekt == true }

    fun List<InntektPeriode>.hentNettoKapital() = find { it.nettoKapitalInntekt == true }

    fun VedtakBarnStonad.vedtakPeriode(
        fomDato: LocalDate,
        tomDato: LocalDate? = null,
        beløp: BigDecimal,
        resultatKode: String,
    ) {
        val vedtakPeriode = hentVedtakPeriodeForDato(fomDato)!!
        vedtakPeriode.fomDato shouldBe fomDato
        vedtakPeriode.tomDato shouldBe tomDato
        vedtakPeriode.beløp shouldBe beløp
        vedtakPeriode.resultatKode shouldBe resultatKode
        vedtakPeriode.inntektGrense shouldBe if (tomDato == null) FORSKUDD_INNTEKTGRENSE_2022_2023 else FORSKUDD_INNTEKTGRENSE_2021_2022
        vedtakPeriode.maksInntekt shouldBe if (tomDato == null) FORSKUDD_MAKS_INNTEKT_FORSKUDD_MOTTAKER_2022_2023 else FORSKUDD_MAKS_INNTEKT_FORSKUDD_MOTTAKER_2021_2022
    }

    fun VedtakDetaljer.validerBostatus(
        fomDato: LocalDate,
        tomDato: LocalDate? = null,
        antallBarn: Int,
    ) {
        val barnIHustandPeriode = hentBarnIHustandPeriodeForDato(fomDato)!!
        barnIHustandPeriode.periode.tilLocalDateFom() shouldBe fomDato
        barnIHustandPeriode.periode.tilLocalDateTil() shouldBe tomDato
        barnIHustandPeriode.antall shouldBe antallBarn
    }

    fun VedtakDetaljer.validerSivilstandPeriode(
        periode: PeriodeFraTom,
        sivilstandKode: Sivilstandskode,
        beskrivelse: String,
    ) {
        val sivilstandPeriode = hentSivilstandPeriodeForDato(periode)!!
        sivilstandPeriode.periode.tilLocalDateFom() shouldBe periode.fraDato
        sivilstandPeriode.periode.tilLocalDateTil() shouldBe periode.tomDato
        sivilstandPeriode.sivilstand shouldBe sivilstandKode
    }

    fun VedtakBarnStonad.hentVedtakPeriodeForDato(dato: LocalDate) = this.vedtakPerioder.sortedByDescending { it.fomDato }.find { erInnenforPeriode(PeriodeFraTom(it.fomDato, it.tomDato), dato) }

    fun VedtakDetaljer.hentSivilstandPeriodeForDato(periode: PeriodeFraTom) = this.sivilstandPerioder.sortedByDescending { it.periode.fom }.find { it.periode.tilLocalDateFom() <= periode.fraDato && (it.periode.til == null || periode.tomDato == null || it.periode.tilLocalDateTil()!! >= periode.tomDato) }

    fun VedtakDetaljer.hentBarnIHustandPeriodeForDato(dato: LocalDate) = this.barnIHusstandPerioder.sortedByDescending { it.periode.tilLocalDateFom() }.find { it.periode.tilLocalDateFom() <= dato && (it.periode.tilLocalDateTil() == null || it.periode.tilLocalDateTil()!! > dato) }

    fun erInnenforPeriode(
        periode: PeriodeFraTom,
        dato: LocalDate,
    ) = periode.fraDato <= dato && (periode.tomDato == null || periode.tomDato!! > dato)
}

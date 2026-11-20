package no.nav.bidrag.beregn.core.service

import com.fasterxml.jackson.databind.node.POJONode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.beregn.core.consumer.ValutakursConsumer
import no.nav.bidrag.beregn.core.consumer.ValutakursgrunnlagResponse
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.LøpendeBidrag
import no.nav.bidrag.transport.behandling.felles.grunnlag.LøpendeBidragGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.LøpendeBidragPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.PrivatAvtalePeriodeGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.ValutakursGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate
import java.time.YearMonth

class ValutakursServiceTest {
    private val consumer = mockk<ValutakursConsumer>()
    private val service = ValutakursService(consumer)

    @ParameterizedTest
    @ValueSource(strings = ["2024-07", "2025-01"])
    fun `skal hente en kurs per halvår og valuta selv om flere barn bruker kursen`(halvårsstart: YearMonth) {
        val periode = ÅrMånedsperiode(halvårsstart.minusMonths(1), halvårsstart.plusMonths(1))
        val grunnlag = beregningsgrunnlag(periode, løpendeBidragPeriode(periode))
        every { consumer.hentValutakurs(Valutakode.EUR, any()) } answers {
            kursgrunnlag(secondArg(), kurs = if (secondArg<LocalDate>() == halvårsstart.atDay(1)) BigDecimal("12") else BigDecimal("11"))
        }

        val resultat = service.hentValutakursgrunnlag(listOf(grunnlag, grunnlag))

        resultat shouldHaveSize 2
        resultat.map { it.referanse }.distinct() shouldHaveSize 2
        val kurser = resultat.map { it.innholdTilObjekt<ValutakursGrunnlag>() }
        kurser.map { it.periode } shouldBe listOf(
            ÅrMånedsperiode(halvårsstart.minusMonths(6), halvårsstart),
            ÅrMånedsperiode(halvårsstart, halvårsstart.plusMonths(6)),
        )
        kurser.map { it.valutakursListe.first().valutakurs } shouldBe listOf(BigDecimal("11"), BigDecimal("12"))
        kurser.last().valutakursListe.last().valutakurs shouldBe BigDecimal.ONE.divide(BigDecimal("12"), MathContext.DECIMAL128)
        verify(exactly = 1) { consumer.hentValutakurs(Valutakode.EUR, halvårsstart.minusMonths(6).atDay(1)) }
        verify(exactly = 1) { consumer.hentValutakurs(Valutakode.EUR, halvårsstart.atDay(1)) }
        verify(exactly = 2) { consumer.hentValutakurs(any(), any()) }
    }

    @Test
    fun `skal bare hente kurs for valuta og perioder som brukes i beregningen`() {
        val periode = ÅrMånedsperiode("2024-06", "2024-07")
        val grunnlag = beregningsgrunnlag(
            periode,
            løpendeBidragPeriode(ÅrMånedsperiode("2024-07", "2025-01"), Valutakode.USD),
            løpendeBidragPeriode(ÅrMånedsperiode("2023-07", "2024-06"), Valutakode.GBP),
            løpendeBidragPeriode(periode, Valutakode.NOK),
            løpendeBidragPeriode(ÅrMånedsperiode(YearMonth.of(2024, 1), null)),
        )
        every { consumer.hentValutakurs(Valutakode.EUR, LocalDate.of(2024, 1, 1)) } returns kursgrunnlag(LocalDate.of(2024, 1, 1))

        val resultat = service.hentValutakursgrunnlag(listOf(grunnlag))

        resultat shouldHaveSize 1
        verify(exactly = 1) { consumer.hentValutakurs(any(), any()) }
    }

    @Test
    fun `skal ikke hente kurser for tomt grunnlag eller NOK`() {
        val periode = ÅrMånedsperiode("2024-07", "2025-01")

        service.hentValutakursgrunnlag(emptyList()) shouldBe emptyList()
        service.hentValutakursgrunnlag(listOf(beregningsgrunnlag(periode, løpendeBidragPeriode(periode, Valutakode.NOK)))) shouldBe emptyList()

        verify(exactly = 0) { consumer.hentValutakurs(any(), any()) }
    }

    @Test
    fun `skal lage separate kursgrunnlag for forskjellige valutaer i samme halvår`() {
        val periode = ÅrMånedsperiode("2024-07", "2024-08")
        val start = LocalDate.of(2024, 7, 1)
        val grunnlag = beregningsgrunnlag(
            periode,
            løpendeBidragPeriode(periode),
            løpendeBidragPeriode(periode, Valutakode.USD),
        )
        every { consumer.hentValutakurs(Valutakode.EUR, start) } returns kursgrunnlag(start)
        every { consumer.hentValutakurs(Valutakode.USD, start) } returns kursgrunnlag(start, BigDecimal("10")).copy(basisvaluta = Valutakode.USD)

        val resultat = service.hentValutakursgrunnlag(listOf(grunnlag))

        resultat.map { it.referanse }.distinct() shouldHaveSize 2
        resultat.map { it.innholdTilObjekt<ValutakursGrunnlag>().valutakursListe.first().valutakode1 } shouldBe listOf(Valutakode.EUR, Valutakode.USD)
        verify(exactly = 2) { consumer.hentValutakurs(any(), any()) }
    }

    @Test
    fun `skal hente kurs for privat avtale innenfor avtalens beregningsperiode`() {
        val periode = ÅrMånedsperiode("2024-12", "2025-01")
        val privatAvtale = GrunnlagDto(
            referanse = "privat_avtale",
            type = Grunnlagstype.PRIVAT_AVTALE_PERIODE_GRUNNLAG,
            innhold = POJONode(PrivatAvtalePeriodeGrunnlag(ÅrMånedsperiode("2024-01", "2026-01"), BigDecimal.TEN, valutakode = Valutakode.EUR)),
        )
        every { consumer.hentValutakurs(Valutakode.EUR, LocalDate.of(2024, 7, 1)) } returns kursgrunnlag(LocalDate.of(2024, 7, 1))

        service.hentValutakursgrunnlag(listOf(beregningsgrunnlag(periode, privatAvtale))) shouldHaveSize 1

        verify(exactly = 1) { consumer.hentValutakurs(any(), any()) }
    }

    @Test
    fun `skal hente kurser for løpende bidrag til særbidrag uten å endre originalbeløpet`() {
        val periode = ÅrMånedsperiode("2024-07", "2024-08")
        val bidrag = LøpendeBidrag(
            saksnummer = Saksnummer("1234567"),
            type = Stønadstype.BIDRAG,
            løpendeBeløp = BigDecimal("100"),
            valutakode = Valutakode.EUR,
            samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
            beregnetBeløp = BigDecimal.ZERO,
            faktiskBeløp = BigDecimal.ZERO,
            gjelderBarn = "barn",
        )
        val grunnlag = GrunnlagDto(
            referanse = "løpende_bidrag",
            type = Grunnlagstype.LØPENDE_BIDRAG,
            innhold = POJONode(LøpendeBidragGrunnlag(listOf(bidrag))),
        )
        every { consumer.hentValutakurs(Valutakode.EUR, LocalDate.of(2024, 7, 1)) } returns kursgrunnlag(LocalDate.of(2024, 7, 1))

        service.hentValutakursgrunnlag(listOf(beregningsgrunnlag(periode, grunnlag))) shouldHaveSize 1

        grunnlag.innholdTilObjekt<LøpendeBidragGrunnlag>().løpendeBidragListe.single() shouldBe bidrag
    }

    @Test
    fun `skal avvise ubrukelige kursgrunnlag uten reservekurs`() {
        val start = LocalDate.of(2024, 7, 1)
        val gyldig = kursgrunnlag(start)
        val ugyldige = listOf(
            gyldig.copy(kurs = null),
            gyldig.copy(kurs = BigDecimal.ZERO),
            gyldig.copy(kurs = BigDecimal("-1")),
            gyldig.copy(multiplikator = null),
            gyldig.copy(multiplikator = -2),
            gyldig.copy(feiletHenting = true),
            gyldig.copy(basisvaluta = Valutakode.USD),
            gyldig.copy(kvoteringsvaluta = Valutakode.EUR),
            gyldig.copy(brukFra = start.minusDays(1).atStartOfDay()),
            gyldig.copy(brukTil = start.plusMonths(6).minusDays(1).atStartOfDay()),
        )
        val periode = ÅrMånedsperiode("2024-07", "2024-08")
        for (respons in ugyldige) {
            every { consumer.hentValutakurs(Valutakode.EUR, start) } returns respons

            shouldThrow<IllegalArgumentException> {
                service.hentValutakursgrunnlag(listOf(beregningsgrunnlag(periode, løpendeBidragPeriode(periode))))
            }
        }
    }

    @Test
    fun `skal stoppe når kurs mangler også for fremtidige perioder`() {
        val periode = ÅrMånedsperiode("2099-01", "2099-02")
        every { consumer.hentValutakurs(Valutakode.EUR, LocalDate.of(2099, 1, 1)) } throws HttpClientErrorException(HttpStatus.NOT_FOUND)

        shouldThrow<HttpClientErrorException> {
            service.hentValutakursgrunnlag(listOf(beregningsgrunnlag(periode, løpendeBidragPeriode(periode))))
        }.statusCode shouldBe HttpStatus.NOT_FOUND
    }

    private fun beregningsgrunnlag(periode: ÅrMånedsperiode, vararg grunnlag: GrunnlagDto) = BeregnGrunnlag(
        periode = periode,
        søknadsbarnReferanse = "barn",
        stønadstype = Stønadstype.BIDRAG,
        grunnlagListe = grunnlag.toList(),
    )

    private fun løpendeBidragPeriode(periode: ÅrMånedsperiode, valuta: Valutakode = Valutakode.EUR) = GrunnlagDto(
        referanse = "løpende_$valuta",
        type = Grunnlagstype.LØPENDE_BIDRAG_PERIODE,
        innhold = POJONode(
            LøpendeBidragPeriode(
                periode = periode,
                saksnummer = Saksnummer("1234567"),
                stønadstype = Stønadstype.BIDRAG,
                løpendeBeløp = BigDecimal("100"),
                valutakode = valuta,
                beregnetBeløp = BigDecimal.ZERO,
                faktiskBeløp = BigDecimal.ZERO,
            ),
        ),
    )

    private fun kursgrunnlag(start: LocalDate, kurs: BigDecimal = BigDecimal("11.45")) = ValutakursgrunnlagResponse(
        brukFra = start.atStartOfDay(),
        brukTil = start.plusMonths(6).atStartOfDay(),
        kurs = kurs,
        multiplikator = 0,
        basisvaluta = Valutakode.EUR,
        kvoteringsvaluta = Valutakode.NOK,
        feiletHenting = false,
    )
}

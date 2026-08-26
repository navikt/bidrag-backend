package no.nav.bidrag.domene.tid

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.assertions.throwables.shouldThrowMessage
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

internal class ÅrMånedsperiodeTest {
    private val objectMapper =
        ObjectMapper()
            .registerKotlinModule()
            .registerModule(
                JavaTimeModule()
                    .addDeserializer(
                        YearMonth::class.java,
                        // Denne trengs for å parse år over 9999 riktig.
                        YearMonthDeserializer(DateTimeFormatter.ofPattern("u-MM")),
                    ),
            ).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    @Test
    fun `Månedsperiode serialiserer og deserialiserer riktig ved tidenes morgen`() {
        val årMånedsperiode = ÅrMånedsperiode(LocalDate.MIN, LocalDate.MIN)

        val writeValueAsString = objectMapper.writeValueAsString(årMånedsperiode)

        val deserialisert = objectMapper.readValue<ÅrMånedsperiode>(writeValueAsString)

        årMånedsperiode shouldBe deserialisert
    }

    @Test
    fun `Månedsperiode serialiserer og deserialiserer riktig ved tidenes ende`() {
        val årMånedsperiode = ÅrMånedsperiode(LocalDate.MAX, LocalDate.MAX)

        val writeValueAsString = objectMapper.writeValueAsString(årMånedsperiode)

        val deserialisert = objectMapper.readValue<ÅrMånedsperiode>(writeValueAsString)

        årMånedsperiode shouldBe deserialisert
    }

    @Test
    fun `Månedsperiode serialiserer og deserialiserer riktig ved år 0`() {
        val årMånedsperiode = ÅrMånedsperiode(YearMonth.of(0, 1), YearMonth.of(0, 1))

        val writeValueAsString = objectMapper.writeValueAsString(årMånedsperiode)

        val deserialisert = objectMapper.readValue<ÅrMånedsperiode>(writeValueAsString)

        årMånedsperiode shouldBe deserialisert
    }

    @Test
    fun `inneholder returnere true hvis måned er i perioden`() {
        val periode = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))

        val inneholder = periode.inneholder(YearMonth.of(2019, 1))

        inneholder shouldBe true
    }

    @Test
    fun `inneholder returnere true hvis måned ikke er i perioden`() {
        val periode = ÅrMånedsperiode(YearMonth.of(2019, 2), YearMonth.of(2019, 5))

        val inneholder = periode.inneholder(YearMonth.of(2019, 1))

        inneholder shouldBe false
    }

    @Test
    fun `snitt returnerer lik periode for like perioder`() {
        val periode1 = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))
        val periode2 = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))

        val snitt = periode1.snitt(periode2)

        snitt shouldBe periode1
    }

    @Test
    fun `snitt returnerer null for periode uten overlap`() {
        val periode1 = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))
        val periode2 = ÅrMånedsperiode(YearMonth.of(2018, 1), YearMonth.of(2018, 12))

        val snitt = periode1.snitt(periode2)

        snitt shouldBe null
    }

    @Test
    fun `snitt returnerer lik periode uansett hvilken periode som ligger til grunn`() {
        val periode1 = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))
        val periode2 = ÅrMånedsperiode(YearMonth.of(2019, 3), YearMonth.of(2019, 12))

        val snitt1til2 = periode1.snitt(periode2)
        val snitt2til1 = periode2.snitt(periode1)

        snitt1til2 shouldBe snitt2til1
        snitt1til2 shouldBe ÅrMånedsperiode(YearMonth.of(2019, 3), YearMonth.of(2019, 5))
    }

    @Test
    fun `snitt returnerer åpen periode med tom = null hvis begge periodene har tom = null`() {
        val periode1 = Datoperiode(YearMonth.of(2019, 1), null)
        val periode2 = Datoperiode(YearMonth.of(2019, 3), null)

        val snitt = periode1 snitt periode2

        snitt shouldBe Datoperiode(YearMonth.of(2019, 3), null)
    }

    @Test
    fun `snitt returnerer periode med tom lik verdien fra perioden som har verdi for tom hvis den ene er null`() {
        val periode1 = Datoperiode(YearMonth.of(2019, 1), null)
        val periode2 = Datoperiode(YearMonth.of(2019, 3), YearMonth.of(2019, 12))

        val snitt = periode1 snitt periode2

        snitt shouldBe Datoperiode(YearMonth.of(2019, 3), YearMonth.of(2019, 12))
    }

    @Test
    fun `union returnerer lik periode for like perioder`() {
        val periode1 = Datoperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))
        val periode2 = Datoperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))

        val union = periode1 union periode2

        union shouldBe periode1
    }

    @Test
    fun `union returnerer riktig periode for påfølgende perioder`() {
        val periode1 = Datoperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))
        val periode2 = Datoperiode(YearMonth.of(2018, 1), YearMonth.of(2019, 1))

        val union = periode1 union periode2

        union shouldBe Datoperiode(YearMonth.of(2018, 1), YearMonth.of(2019, 5))
    }

    @Test
    fun `union hvor en periode slutter med null retunere en åpen periode med tom = null`() {
        val periode1 = Datoperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))
        val periode2 = Datoperiode(YearMonth.of(2018, 1), null)

        val union = periode1 union periode2
        val union2 = periode2 union periode1

        union shouldBe union2
        union2 shouldBe Datoperiode(YearMonth.of(2018, 1), null)
    }

    @Test
    fun `union kaster exception for perioder som ikke følger hverandre eller overlapper`() {
        val periode1 = Datoperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))
        val periode2 = Datoperiode(YearMonth.of(2018, 1), YearMonth.of(2018, 11))

        shouldThrowMessage(
            "Kan ikke lage union av perioder som $periode1 og $periode2 som ikke overlapper eller direkte følger hverandre.",
        ) {
            periode1 union periode2
        }
    }

    @Test
    fun `union returnerer lik periode uansett hvilken periode som ligger til grunn`() {
        val periode1 = Datoperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))
        val periode2 = Datoperiode(YearMonth.of(2019, 3), YearMonth.of(2019, 12))

        val union1til2 = periode1 union periode2
        val union2til1 = periode2 union periode1

        union1til2 shouldBe union2til1
        union1til2 shouldBe Datoperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 12))
    }

    @Test
    fun `inneholder returnerer true for periode som helt inneholder innsendt periode`() {
        val periode1 = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))
        val periode2 = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 1))

        val inneholder = periode1.inneholder(periode2)

        inneholder shouldBe true
    }

    @Test
    fun `inneholder returnerer false for periode som stikker utenfor innsendt periode`() {
        val periode1 = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))
        val periode2 = ÅrMånedsperiode(YearMonth.of(2019, 2), YearMonth.of(2019, 4))

        val inneholder = periode1.inneholder(periode2)

        inneholder shouldBe false
    }

    @Test
    fun `omsluttesAv returnerer true for periode som helt omsluttes av innsendt periode`() {
        val periode1 = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 1))
        val periode2 = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))

        val inneholder = periode1.omsluttesAv(periode2)

        inneholder shouldBe true
    }

    @Test
    fun `omsluttesAv returnerer false for periode som nesten omsluttes av innsendt periode`() {
        val periode1 = ÅrMånedsperiode(YearMonth.of(2019, 2), YearMonth.of(2019, 4))
        val periode2 = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))

        val inneholder = periode1.omsluttesAv(periode2)

        inneholder shouldBe false
    }

    @Test
    fun `overlapperIStartenAv returnerer true hvis denne perioden overlapper i starten av perioden som sendes inn`() {
        val periodeSomOverlapperStarten = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 1))
        val periode = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))

        val overlapperIStartenAv = periodeSomOverlapperStarten.overlapperKunIStartenAv(periode)

        overlapperIStartenAv shouldBe true
    }

    @Test
    fun `overlapperIStartenAv returnerer false hvis denne perioden er lik den som sendes inn`() {
        val periodeSomErLik = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))
        val periode = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))

        val overlapperIStartenAv = periodeSomErLik.overlapperKunIStartenAv(periode)

        overlapperIStartenAv shouldBe false
    }

    @Test
    fun `overlapperIStartenAv returnerer false hvis denne perioden er før den som sendes inn`() {
        val periodeSomErFør = ÅrMånedsperiode(YearMonth.of(2018, 9), YearMonth.of(2018, 12))
        val periode = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))

        val overlapperIStartenAv = periodeSomErFør.overlapperKunIStartenAv(periode)

        overlapperIStartenAv shouldBe false
    }

    @Test
    fun `overlapperIStartenAv returnerer false hvis denne perioden starter etter den som sendes inn`() {
        val periodeSomErInneI = ÅrMånedsperiode(YearMonth.of(2018, 9), YearMonth.of(2018, 9))
        val periode = ÅrMånedsperiode(YearMonth.of(2018, 9), YearMonth.of(2018, 9))

        val overlapperIStartenAv = periodeSomErInneI.overlapperKunIStartenAv(periode)

        overlapperIStartenAv shouldBe false
    }

    @Test
    fun `overlapperISluttenAv returnerer true hvis denne perioden overlapper i slutten av perioden som sendes inn`() {
        val periodeSomOverlapperSlutten = ÅrMånedsperiode(YearMonth.of(2019, 3), YearMonth.of(2019, 3))
        val periode = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))

        val overlapperISluttenAv = periodeSomOverlapperSlutten.overlapperKunISluttenAv(periode)

        overlapperISluttenAv shouldBe true
    }

    @Test
    fun `overlapperISluttenAv returnerer false hvis denne perioden er lik den som sendes inn`() {
        val periodeSomErLik = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))
        val periode = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))

        val overlapperISluttenAv = periodeSomErLik.overlapperKunISluttenAv(periode)

        overlapperISluttenAv shouldBe false
    }

    @Test
    fun `overlapperISluttenAv returnerer false hvis denne perioden er etter den som sendes inn`() {
        val periodeSomErEtter = ÅrMånedsperiode(YearMonth.of(2019, 4), YearMonth.of(2019, 4))
        val periode = ÅrMånedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))

        val overlapperISluttenAv = periodeSomErEtter.overlapperKunISluttenAv(periode)

        overlapperISluttenAv shouldBe false
    }

    @Test
    fun `overlapperISluttenAv returnerer false hvis denne perioden slutter før den som sendes inn`() {
        val periodeSomErInneI = ÅrMånedsperiode(YearMonth.of(2018, 9), YearMonth.of(2018, 9))
        val periode = ÅrMånedsperiode(YearMonth.of(2018, 9), YearMonth.of(2018, 9))

        val overlapperISluttenAv = periodeSomErInneI.overlapperKunISluttenAv(periode)

        overlapperISluttenAv shouldBe false
    }

    @Test
    fun `lengdeIHeleMåneder returnerer korrekt antall måneder for lange perioder`() {
        val periode = ÅrMånedsperiode(YearMonth.of(2015, 9), YearMonth.of(2028, 3))

        val lengdeIHeleMåneder = periode.lengdeIHeleMåneder()

        lengdeIHeleMåneder shouldBe 151
    }

    @Test
    fun `lengdeIHeleMåneder returnerer korrekt antall måneder for korte perioder`() {
        val periode = ÅrMånedsperiode(YearMonth.of(2015, 9), YearMonth.of(2015, 12))

        val lengdeIHeleMåneder = periode.lengdeIHeleMåneder()

        lengdeIHeleMåneder shouldBe 4
    }
}

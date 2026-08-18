package no.nav.bidrag.grunnlag.comparator

import no.nav.bidrag.grunnlag.bo.AinntektBo
import no.nav.bidrag.grunnlag.bo.AinntektspostBo
import no.nav.bidrag.grunnlag.bo.SkattegrunnlagBo
import no.nav.bidrag.grunnlag.bo.SkattegrunnlagspostBo
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class PeriodComparatorTest {

    @Test
    fun `skal filtrere bort ainntekter som faller utenfor oppgitt periode`() {
        var newEntities = createPeriodEntities(Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 11, 1)))
        var existingEntities = createPeriodEntities(Period(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 12, 1)))

        val ainntektPeriodComparator = AinntektPeriodComparator()
        var ainntektPeriod = Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 11, 1))
        var comparatorResult = ainntektPeriodComparator.comparePeriodEntities(ainntektPeriod, newEntities, existingEntities)

        assertTrue(comparatorResult.expiredEntities.size == 2)
        assertTrue(comparatorResult.equalEntities.size == 3)
        assertTrue(comparatorResult.updatedEntities.isEmpty())

        newEntities = createPeriodEntities(Period(LocalDate.of(2021, 9, 1), LocalDate.of(2021, 11, 1)))

        ainntektPeriod = Period(LocalDate.of(2021, 9, 1), LocalDate.of(2021, 11, 1))
        comparatorResult = ainntektPeriodComparator.comparePeriodEntities(ainntektPeriod, newEntities, existingEntities)

        assertTrue(comparatorResult.expiredEntities.size == 3)
        assertTrue(comparatorResult.equalEntities.size == 2)
        assertTrue(comparatorResult.updatedEntities.isEmpty())

        // Et helt år med nye inntekter. Den nye perioden starter tidligere og slutter senere enn ekisiterede perioder.
        newEntities = createPeriodEntities(Period(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1)))
        existingEntities = createPeriodEntities(Period(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 12, 1)))

        ainntektPeriod = Period(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1))
        comparatorResult = ainntektPeriodComparator.comparePeriodEntities(ainntektPeriod, newEntities, existingEntities)

        assertTrue(comparatorResult.expiredEntities.isEmpty())
        assertTrue(comparatorResult.equalEntities.size == 10)
        assertTrue(comparatorResult.updatedEntities.size == 2)

        // Nye inntekter krysser fra ett år til et annet. Eksisterende inntekter finnes for en lengre periode i begge ender.
        newEntities = createPeriodEntities(Period(LocalDate.of(2020, 5, 1), LocalDate.of(2021, 7, 1)))
        existingEntities = createPeriodEntities(Period(LocalDate.of(2020, 3, 1), LocalDate.of(2021, 10, 1)))

        ainntektPeriod = Period(LocalDate.of(2020, 5, 1), LocalDate.of(2021, 7, 1))
        comparatorResult = ainntektPeriodComparator.comparePeriodEntities(ainntektPeriod, newEntities, existingEntities)

        assertTrue(comparatorResult.expiredEntities.size == 5)
        assertTrue(comparatorResult.equalEntities.size == 14)
        assertTrue(comparatorResult.updatedEntities.isEmpty())
    }

    @Test
    fun `skal sjekke om alle inntektsposter tilknyttet to inntekter er like`() {
        val ainntektPeriodComparator = AinntektPeriodComparator()
        var newEntities = createPeriodEntities(
            Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)),
            listOf(
                createAinntektspost(),
            ),
        )
        var existingEntities = createPeriodEntities(
            Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)),
            listOf(
                createAinntektspost(),
            ),
        )

        assertTrue(ainntektPeriodComparator.isEntitiesEqual(newEntities[0], existingEntities[0]))

        newEntities =
            createPeriodEntities(Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)), listOf(createAinntektspost(belop = BigDecimal(500))))
        existingEntities = createPeriodEntities(
            Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)),
            listOf(
                createAinntektspost(belop = BigDecimal(500.0)),
            ),
        )

        assertTrue(ainntektPeriodComparator.isEntitiesEqual(newEntities[0], existingEntities[0]))

        newEntities =
            createPeriodEntities(Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)), listOf(createAinntektspost(belop = BigDecimal(450))))

        assertFalse(ainntektPeriodComparator.isEntitiesEqual(newEntities[0], existingEntities[0]))

        newEntities =
            createPeriodEntities(
                Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)),
                listOf(createAinntektspost(beskrivelse = "Beskrivelse2")),
            )

        assertFalse(ainntektPeriodComparator.isEntitiesEqual(newEntities[0], existingEntities[0]))

        newEntities =
            createPeriodEntities(Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)), listOf(createAinntektspost(fordelType = "Fordeltype2")))

        assertFalse(ainntektPeriodComparator.isEntitiesEqual(newEntities[0], existingEntities[0]))

        newEntities =
            createPeriodEntities(
                Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)),
                listOf(createAinntektspost(inntektType = "Inntekttype2")),
            )

        assertFalse(ainntektPeriodComparator.isEntitiesEqual(newEntities[0], existingEntities[0]))

        newEntities =
            createPeriodEntities(
                Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)),
                listOf(createAinntektspost(opplysningspliktigId = "Opp2")),
            )

        assertFalse(ainntektPeriodComparator.isEntitiesEqual(newEntities[0], existingEntities[0]))

        newEntities = createPeriodEntities(
            Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)),
            listOf(createAinntektspost(opptjeningsperiodeFra = LocalDate.of(2021, 8, 2))),
        )

        assertFalse(ainntektPeriodComparator.isEntitiesEqual(newEntities[0], existingEntities[0]))

        newEntities = createPeriodEntities(
            Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)),
            listOf(createAinntektspost(opptjeningsperiodeTil = LocalDate.of(2021, 9, 2))),
        )

        assertFalse(ainntektPeriodComparator.isEntitiesEqual(newEntities[0], existingEntities[0]))

        newEntities = createPeriodEntities(
            Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)),
            listOf(createAinntektspost(virksomhetId = "Virk2")),
        )

        assertFalse(ainntektPeriodComparator.isEntitiesEqual(newEntities[0], existingEntities[0]))

        newEntities =
            createPeriodEntities(Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)), listOf(createAinntektspost(utbetalingsperiode = "Utb2")))

        assertFalse(ainntektPeriodComparator.isEntitiesEqual(newEntities[0], existingEntities[0]))

        newEntities =
            createPeriodEntities(
                Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)),
                listOf(createAinntektspost(etterbetalingsperiodeFom = LocalDate.of(2021, 10, 1))),
            )

        assertFalse(ainntektPeriodComparator.isEntitiesEqual(newEntities[0], existingEntities[0]))

        newEntities =
            createPeriodEntities(
                Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)),
                listOf(createAinntektspost(etterbetalingsperiodeTom = LocalDate.of(2021, 10, 1))),
            )

        assertFalse(ainntektPeriodComparator.isEntitiesEqual(newEntities[0], existingEntities[0]))
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `test på sorteringslogikk for ainntektsposter`() {
        val ainntektPeriodComparator = AinntektPeriodComparator()

        val existingEntities = createPeriodEntities(
            Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)),
            listOf(
                createAinntektspost(
                    utbetalingsperiode = "2022-07",
                    opptjeningsperiodeFra = LocalDate.of(2022, 6, 1),
                    opptjeningsperiodeTil = LocalDate.of(2022, 7, 1),
                    opplysningspliktigId = "opplysningspliktig1",
                    virksomhetId = "virksomhet1",
                    inntektType = "LOENNSINNTEKT",
                    fordelType = "kontantytelse",
                    beskrivelse = "fastloenn",
                    belop = BigDecimal(500.0),
                    etterbetalingsperiodeFom = LocalDate.of(2022, 2, 1),
                    etterbetalingsperiodeTom = LocalDate.of(2022, 3, 1),
                ),
                createAinntektspost(
                    utbetalingsperiode = "2022-07",
                    opptjeningsperiodeFra = LocalDate.of(2022, 6, 1),
                    opptjeningsperiodeTil = LocalDate.of(2022, 7, 1),
                    opplysningspliktigId = "opplysningspliktig1",
                    virksomhetId = "virksomhet1",
                    inntektType = "LOENNSINNTEKT",
                    fordelType = "kontantytelse",
                    beskrivelse = "fastloenn",
                    belop = BigDecimal(500.0),
                    etterbetalingsperiodeFom = LocalDate.of(2022, 3, 1),
                    etterbetalingsperiodeTom = LocalDate.of(2022, 4, 1),
                ),
            ),
        )

        val newEntities = createPeriodEntities(
            Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)),
            listOf(
                createAinntektspost(
                    utbetalingsperiode = "2022-07",
                    opptjeningsperiodeFra = LocalDate.of(2022, 6, 1),
                    opptjeningsperiodeTil = LocalDate.of(2022, 7, 1),
                    opplysningspliktigId = "opplysningspliktig1",
                    virksomhetId = "virksomhet1",
                    inntektType = "LOENNSINNTEKT",
                    fordelType = "kontantytelse",
                    beskrivelse = "fastloenn",
                    belop = BigDecimal(500.0),
                    etterbetalingsperiodeFom = LocalDate.of(2022, 3, 1),
                    etterbetalingsperiodeTom = LocalDate.of(2022, 4, 1),
                ),
                createAinntektspost(
                    utbetalingsperiode = "2022-07",
                    opptjeningsperiodeFra = LocalDate.of(2022, 6, 1),
                    opptjeningsperiodeTil = LocalDate.of(2022, 7, 1),
                    opplysningspliktigId = "opplysningspliktig1",
                    virksomhetId = "virksomhet1",
                    inntektType = "LOENNSINNTEKT",
                    fordelType = "kontantytelse",
                    beskrivelse = "fastloenn",
                    belop = BigDecimal(500.0),
                    etterbetalingsperiodeFom = LocalDate.of(2022, 2, 1),
                    etterbetalingsperiodeTom = LocalDate.of(2022, 3, 1),
                ),
            ),
        )
        assertTrue(ainntektPeriodComparator.isEntitiesEqual(newEntities[0], existingEntities[0]))
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `test på sorteringslogikk for skattegrunnlagsposter`() {
        val skattegrunnlagPeriodComparator = SkattegrunnlagPeriodComparator()

        // Tester sortreringslogikk på inntektsposter
        val existingEntities = createPeriodEntitiesSkattegrunnlag(
            Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)),
            listOf(
                createSkattegrunnlagspost(
                    skattegrunnlagType = "Svalbard",
                    inntektType = "LOENNSINNTEKT",
                    belop = BigDecimal(500.0),
                ),
                createSkattegrunnlagspost(
                    skattegrunnlagType = "Svalbard",
                    inntektType = "LOENNSINNTEKT",
                    belop = BigDecimal(600.0),
                ),
            ),
        )

        val newEntities = createPeriodEntitiesSkattegrunnlag(
            Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)),
            listOf(
                createSkattegrunnlagspost(
                    skattegrunnlagType = "Svalbard",
                    inntektType = "LOENNSINNTEKT",
                    belop = BigDecimal(600.0),
                ),
                createSkattegrunnlagspost(
                    skattegrunnlagType = "Svalbard",
                    inntektType = "LOENNSINNTEKT",
                    belop = BigDecimal(500.0),
                ),
            ),
        )
        assertTrue(skattegrunnlagPeriodComparator.isEntitiesEqual(newEntities[0], existingEntities[0]))
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal både filtrere bort ainntekter som faller utenfor ny periode og sjekke om nye inntekter er oppdatert eller ikke`() {
        val ainntektPeriodComparator = AinntektPeriodComparator()

        // Ingen inntekter er like og det finnes en eksisterende inntekt som ligger utenfor ny periode
        val newEntities = createPeriodEntities(
            Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 11, 1)),
            listOf(
                createAinntektspost(),
                createAinntektspost(beskrivelse = "Beskrivelse2", fordelType = "Fordeltype2"),
            ),
        )
        val existingEntities = createPeriodEntities(
            Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 12, 1)),
            listOf(
                createAinntektspost(),
                createAinntektspost(),
            ),
        )
        val ainntektPeriod = Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 11, 1))
        val comparatorResult = ainntektPeriodComparator.comparePeriodEntities(ainntektPeriod, newEntities, existingEntities)

        assertTrue(comparatorResult.expiredEntities.size == 4)
        assertTrue(comparatorResult.equalEntities.isEmpty())
        assertTrue(comparatorResult.updatedEntities.size == 3)
    }

    @Test
    fun `skal filtrere bort inntekter som ligger innenfor rett periode men som ikke lenger eksisterer i nye inntekter for perioden`() {
        val ainntektPeriodComparator = AinntektPeriodComparator()

        // Nye inntekter mangler inntekt for September, men den ligger i eksisterende inntekter.
        val newEntities = createPeriodEntities(
            Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1)),
            listOf(
                createAinntektspost(),
            ),
        )

        newEntities.addAll(
            createPeriodEntities(
                Period(LocalDate.of(2021, 10, 1), LocalDate.of(2021, 11, 1)),
                listOf(
                    createAinntektspost(),
                ),
            ),
        )
        val existingEntities = createPeriodEntities(
            Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 11, 1)),
            listOf(
                createAinntektspost(),
            ),
        )
        val ainntektPeriod = Period(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 11, 1))
        val comparatorResult = ainntektPeriodComparator.comparePeriodEntities(ainntektPeriod, newEntities, existingEntities)

        assertTrue(comparatorResult.expiredEntities.size == 1)
        assertTrue(comparatorResult.equalEntities.size == 2)
        assertTrue(comparatorResult.updatedEntities.isEmpty())
    }

    private fun createPeriodEntities(
        period: IPeriod,
        inntektsposter: List<AinntektspostBo> = emptyList(),
    ): MutableList<PeriodComparable<AinntektBo, AinntektspostBo>> {
        val existingEntities = mutableListOf<PeriodComparable<AinntektBo, AinntektspostBo>>()
        var currentStartDate = period.periodeFra
        while (currentStartDate.isBefore(period.periodeTil)) {
            existingEntities.add(
                PeriodComparable(
                    AinntektBo(periodeFra = currentStartDate, periodeTil = currentStartDate.plusMonths(1)),
                    inntektsposter,
                ),
            )
            currentStartDate = currentStartDate.plusMonths(1)
        }

        return existingEntities
    }

    private fun createAinntektspost(
        utbetalingsperiode: String = "Utb1",
        opptjeningsperiodeFra: LocalDate = LocalDate.of(2021, 8, 1),
        opptjeningsperiodeTil: LocalDate = LocalDate.of(2021, 9, 1),
        opplysningspliktigId: String = "Opp1",
        virksomhetId: String = "Virk1",
        inntektType: String = "Inntekttype1",
        fordelType: String = "Fordeltype1",
        beskrivelse: String = "Beskrivelse1",
        belop: BigDecimal = BigDecimal(1000),
        etterbetalingsperiodeFom: LocalDate? = null,
        etterbetalingsperiodeTom: LocalDate? = null,
    ): AinntektspostBo = AinntektspostBo(
        utbetalingsperiode = utbetalingsperiode,
        opptjeningsperiodeFra = opptjeningsperiodeFra,
        opptjeningsperiodeTil = opptjeningsperiodeTil,
        opplysningspliktigId = opplysningspliktigId,
        virksomhetId = virksomhetId,
        inntektType = inntektType,
        fordelType = fordelType,
        beskrivelse = beskrivelse,
        belop = belop,
        etterbetalingsperiodeFra = etterbetalingsperiodeFom,
        etterbetalingsperiodeTil = etterbetalingsperiodeTom,
    )

    private fun createPeriodEntitiesSkattegrunnlag(
        period: IPeriod,
        inntektsposter: List<SkattegrunnlagspostBo> = emptyList(),
    ): MutableList<PeriodComparable<SkattegrunnlagBo, SkattegrunnlagspostBo>> {
        val existingEntities = mutableListOf<PeriodComparable<SkattegrunnlagBo, SkattegrunnlagspostBo>>()
        var currentStartDate = period.periodeFra
        while (currentStartDate.isBefore(period.periodeTil)) {
            existingEntities.add(
                PeriodComparable(
                    SkattegrunnlagBo(periodeFra = currentStartDate, periodeTil = currentStartDate.plusMonths(1)),
                    inntektsposter,
                ),
            )
            currentStartDate = currentStartDate.plusMonths(1)
        }

        return existingEntities
    }

    private fun createSkattegrunnlagspost(
        skattegrunnlagType: String = "Svalbard",
        inntektType: String = "Lonnsinntekt",
        belop: BigDecimal = BigDecimal(1000),
    ): SkattegrunnlagspostBo = SkattegrunnlagspostBo(
        skattegrunnlagType = skattegrunnlagType,
        inntektType = inntektType,
        belop = belop,
    )
}

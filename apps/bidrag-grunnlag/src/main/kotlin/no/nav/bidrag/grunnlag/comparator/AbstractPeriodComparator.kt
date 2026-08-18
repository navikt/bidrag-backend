package no.nav.bidrag.grunnlag.comparator

import no.nav.bidrag.grunnlag.SECURE_LOGGER
import no.nav.bidrag.grunnlag.util.toJsonString
import java.math.BigDecimal
import java.time.LocalDate

abstract class AbstractPeriodComparator<T : PeriodComparable<*, *>> {

    private fun isInsidePeriod(requestedPeriod: IPeriod, existingPeriod: IPeriod): Boolean = existingPeriod.periodeFra.isAfterOrEqual(requestedPeriod.periodeFra) &&
        existingPeriod.periodeTil
            .isBeforeOrEqual(requestedPeriod.periodeTil)

    fun comparePeriodEntities(requestedPeriod: IPeriod, newEntities: List<T>, existingEntities: List<T>): ComparatorResult<T> {
        val expiredEntities = mutableListOf<T>()
        val updatedEntities = mutableListOf<T>()
        val equalEntities = mutableListOf<T>()

        val existingEntitiesWithinRequestedPeriod = filterEntitiesByPeriod(existingEntities, newEntities, requestedPeriod, expiredEntities)
        SECURE_LOGGER.debug(
            "${existingEntitiesWithinRequestedPeriod.size} eksisterende entiteter innenfor forespurt periode (${requestedPeriod.periodeFra} - " +
                "${requestedPeriod.periodeTil}).",
        )
        SECURE_LOGGER.debug("${expiredEntities.size} utløpte entiteter før sammenligning.")

        newEntities.forEach { newEntity ->
            val existingEntityWithEqualPeriod = findEntityWithEqualPeriod(newEntity, existingEntitiesWithinRequestedPeriod)
            if (existingEntityWithEqualPeriod != null) {
                if (isEntitiesEqual(newEntity, existingEntityWithEqualPeriod)) {
                    equalEntities.add(existingEntityWithEqualPeriod)
                } else {
                    SECURE_LOGGER.debug(
                        "Ny og eksisterende entitet er ulike. Ny: ${
                            toJsonString(newEntity)
                        }, Eksisterende: ${toJsonString(existingEntityWithEqualPeriod)}.",
                    )
                    expiredEntities.add(existingEntityWithEqualPeriod)
                    updatedEntities.add(newEntity)
                }
            } else {
                SECURE_LOGGER.debug(
                    "Kunne ikke finne eksisterede entitet for perioden (${newEntity.periodEntity.periodeFra} - " +
                        "${newEntity.periodEntity.periodeTil}).",
                )
                updatedEntities.add(newEntity)
            }
        }
        return ComparatorResult(expiredEntities, updatedEntities, equalEntities)
    }

    fun compareFields(newEntity: Any?, existingEntity: Any?, fieldName: String): Map<String, String> {
        val differences = mutableMapOf<String, String>()
        val entitiesNotEqual = when (newEntity) {
            is BigDecimal -> newEntity.compareTo(existingEntity as BigDecimal) != 0
            else -> newEntity != existingEntity
        }
        if (entitiesNotEqual) {
            differences[fieldName] = "$newEntity != $existingEntity"
        }
        return differences
    }

    private fun findEntityWithEqualPeriod(periodEntity: T, periodEntities: List<T>): T? = periodEntities.find { t ->
        t.periodEntity.periodeFra.isEqual(periodEntity.periodEntity.periodeFra) &&
            t.periodEntity.periodeTil.isEqual(
                periodEntity.periodEntity.periodeTil,
            )
    }

    private fun filterEntitiesByPeriod(
        existingEntities: List<T>,
        newEntities: List<T>,
        requestedPeriod: IPeriod,
        expiredEntities: MutableList<T>,
    ): List<T> {
        val filteredEntities = mutableListOf<T>()
        existingEntities.forEach { existingEntity ->
            if (isInsidePeriod(requestedPeriod, existingEntity.periodEntity) && periodEntityStillExists(existingEntity, newEntities)) {
                filteredEntities.add(existingEntity)
            } else {
                expiredEntities.add(existingEntity)
            }
        }
        return filteredEntities
    }

    private fun periodEntityStillExists(existingEntity: T, newEntities: List<T>): Boolean = findEntityWithEqualPeriod(existingEntity, newEntities) != null

    abstract fun isEntitiesEqual(newEntity: T, existingEntity: T): Boolean
}

interface IPeriod {
    val periodeFra: LocalDate
    val periodeTil: LocalDate
}

class Period(override val periodeFra: LocalDate, override val periodeTil: LocalDate) : IPeriod

open class PeriodComparable<PeriodEntity : IPeriod, Child>(val periodEntity: PeriodEntity, val children: List<Child>?)

class ComparatorResult<T : PeriodComparable<*, *>>(val expiredEntities: List<T>, val updatedEntities: List<T>, val equalEntities: List<T>)

fun LocalDate.isAfterOrEqual(startDate: LocalDate): Boolean = this >= startDate

fun LocalDate.isBeforeOrEqual(endDate: LocalDate): Boolean = this <= endDate

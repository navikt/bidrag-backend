package no.nav.bidrag.generer.testdata

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Random
import kotlin.math.abs

@Suppress("unused")
class RandomTestData(
    val random: Random,
) {
    fun dateBetween(
        fromInclusive: LocalDate,
        toExclusive: LocalDate?,
    ): LocalDate {
        val numberOfDays = ChronoUnit.DAYS.between(fromInclusive, toExclusive)
        return fromInclusive.plusDays(nextLong(numberOfDays))
    }

    fun nextLong(bound: Long): Long = abs(random.nextLong()) % bound

    companion object {
        private val lock = Any()
        private var instance: RandomTestData? = null

        fun random(): RandomTestData {
            if (instance == null) {
                synchronized(lock) {
                    if (instance == null) {
                        instance = RandomTestData(defaultRandom())
                    }
                }
            }
            return instance!!
        }

        private fun defaultRandom(): Random {
            try {
                val seedProperty = System.getProperty("testdata.randomseed")
                if (seedProperty != null) {
                    return Random(seedProperty.toLong())
                }
            } catch (_: Exception) {
                //
            }
            return Random()
        }

        fun <T> shuffle(list: MutableList<T?>): MutableList<T?> {
            val shuffledList: MutableList<T?> = ArrayList(list)
            shuffledList.shuffle(random().random)
            return shuffledList
        }
    }
}

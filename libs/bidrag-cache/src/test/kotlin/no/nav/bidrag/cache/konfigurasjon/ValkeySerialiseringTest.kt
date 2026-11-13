package no.nav.bidrag.cache.konfigurasjon

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.cache.nøkkelgenerering.CacheNøkkel
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.cache.Cache
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class ValkeySerialiseringTest {
    data class Person(
        val navn: String,
        val fødselsdato: LocalDate,
        val roller: List<Rolle>,
    )

    enum class Rolle { BM, BP }

    sealed interface Resultat {
        data class Treff(
            val verdi: Long,
        ) : Resultat

        data object Ingen : Resultat
    }

    // Felles lagring for to cache managere, slik at verdiene må gjennom bytes som på to ulike podder.
    private val lagring = mutableMapOf<String, ByteArray>()
    private val skriver =
        mockk<RedisCacheWriter>(relaxed = true) {
            every { get(any(), any()) } answers { lagring[secondArg<ByteArray>().decodeToString()] }
            every { get(any(), any(), any()) } answers { lagring[secondArg<ByteArray>().decodeToString()] }
            every { put(any(), any(), any(), any()) } answers { lagring[secondArg<ByteArray>().decodeToString()] = thirdArg() }
        }

    private fun valkeyCache(): Cache = RedisCacheManager
        .builder(skriver)
        .cacheDefaults(CacheConfig.Valkey().redisCacheConfig())
        .build()
        .getCache("test")
        .shouldNotBeNull()

    private fun rundtur(verdi: Any?): Any? {
        val nøkkel = CacheNøkkel("Test.hent", null, listOf("id"))
        valkeyCache().put(nøkkel, verdi)
        return valkeyCache().get(nøkkel).shouldNotBeNull().get()
    }

    @ParameterizedTest
    @MethodSource("støttedeVerdier")
    fun `skal lese verdien tilbake med samme verdi og type`(verdi: Any) {
        val lest = rundtur(verdi).shouldNotBeNull()

        lest shouldBe verdi
        lest.javaClass shouldBe verdi.javaClass
    }

    @Test
    fun `skal feile ved lesing av typer utenfor tillatte pakker`() {
        shouldThrowAny { rundtur(HttpStatus.OK) }
    }

    @Test
    fun `skal lese null tilbake som null`() {
        rundtur(null) shouldBe null
    }

    @Test
    fun `skal lese samlinger tilbake med samme innhold`() {
        rundtur(listOf("a", "b")).shouldBeInstanceOf<List<*>>() shouldBe listOf("a", "b")
        rundtur(emptyList<String>()).shouldBeInstanceOf<List<*>>() shouldBe emptyList<String>()
        rundtur(emptyMap<String, String>()).shouldBeInstanceOf<Map<*, *>>() shouldBe emptyMap<String, String>()
    }

    @Test
    fun `skal lese elementer i lister tilbake med riktig type`() {
        val lest = rundtur(listOf(Person("Ola", LocalDate.of(2020, 1, 1), listOf(Rolle.BM)))).shouldBeInstanceOf<List<*>>()

        lest.single().shouldBeInstanceOf<Person>().roller.single() shouldBe Rolle.BM
    }

    @Test
    fun `skal lese ByteArray tilbake med samme innhold`() {
        rundtur(byteArrayOf(1, 2, 3)).shouldBeInstanceOf<ByteArray>().toList() shouldBe listOf<Byte>(1, 2, 3)
    }

    companion object {
        @JvmStatic
        fun støttedeVerdier() = listOf(
            "tekst",
            42,
            42L,
            4.2,
            true,
            BigDecimal("1234.50"),
            LocalDate.of(2026, 1, 2),
            LocalDateTime.of(2026, 1, 2, 3, 4, 5),
            YearMonth.of(2026, 1),
            Rolle.BP,
            Person("Ola", LocalDate.of(2020, 1, 1), listOf(Rolle.BM, Rolle.BP)),
            Resultat.Treff(7),
            Resultat.Ingen,
            setOf(1L, 2L),
            hashMapOf("a" to 1),
            Pair("a", 1),
        )
    }
}

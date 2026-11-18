package no.nav.bidrag.cache.nøkkelgenerering

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.bidrag.cache.annotasjoner.KortCache
import no.nav.bidrag.cache.annotasjoner.LangCache
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.support.StaticListableBeanFactory
import java.time.Duration

class CacheNøkkelGeneratorTest {
    open class Tjeneste {
        @KortCache("felles", perBruker = false)
        open fun felles(id: String) = id

        @LangCache("perBruker", perBruker = true)
        open fun perBruker(id: String) = id

        @KortCache("levetid", perBruker = false, levetid = "5m")
        open fun medLevetid(id: String) = id

        @KortCache("ugyldig", perBruker = false, levetid = "tull")
        open fun ugyldigLevetid(id: String) = id

        @KortCache("negativ", perBruker = false, levetid = "-5m")
        open fun negativLevetid(id: String) = id
    }

    @LangCache("klasse", perBruker = false)
    class TjenesteMedKlasseannotasjon {
        fun hent(id: String) = id
    }

    private val tjeneste = Tjeneste()

    private fun generator(identifikator: CacheBrukerIdentifikator? = null): CacheNøkkelGenerator {
        val beanFactory = StaticListableBeanFactory()
        identifikator?.let { beanFactory.addBean("identifikator", it) }
        return CacheNøkkelGenerator(beanFactory.getBeanProvider(CacheBrukerIdentifikator::class.java))
    }

    private fun CacheNøkkelGenerator.nøkkel(
        metode: String,
        target: Any = tjeneste,
    ): CacheNøkkel = generate(target, target.javaClass.getMethod(metode, String::class.java), "id").shouldBeInstanceOf()

    @Test
    fun `skal ikke ha brukerident når perBruker er false`() {
        val nøkkel = generator().nøkkel("felles")

        nøkkel.metode shouldBe "Tjeneste.felles"
        nøkkel.brukerIdent shouldBe null
        nøkkel.levetid shouldBe null
    }

    @Test
    fun `skal ha brukerident når perBruker er true`() {
        generator { "Z123456" }.nøkkel("perBruker").brukerIdent shouldBe "Z123456"
    }

    @Test
    fun `skal feile når perBruker er true og brukeridentifikator mangler`() {
        shouldThrow<IllegalStateException> { generator().nøkkel("perBruker") }
    }

    @Test
    fun `skal feile når brukerident er tom`() {
        shouldThrow<IllegalStateException> { generator { " " }.nøkkel("perBruker") }
    }

    @Test
    fun `skal lese levetid fra annotasjonen`() {
        generator().nøkkel("medLevetid").levetid shouldBe Duration.ofMinutes(5)
    }

    @Test
    fun `skal feile når levetid er ugyldig`() {
        shouldThrow<IllegalArgumentException> { generator().nøkkel("ugyldigLevetid") }
    }

    @Test
    fun `skal feile når levetid er negativ`() {
        shouldThrow<IllegalArgumentException> { generator().nøkkel("negativLevetid") }
    }

    @Test
    fun `skal lese annotasjon på klassen`() {
        generator().nøkkel("hent", TjenesteMedKlasseannotasjon()).metode shouldBe "TjenesteMedKlasseannotasjon.hent"
    }
}

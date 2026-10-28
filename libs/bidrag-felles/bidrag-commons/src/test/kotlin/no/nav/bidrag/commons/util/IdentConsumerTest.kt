package no.nav.bidrag.commons.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.person.PersonidentDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestOperations
import java.net.URI

class IdentConsumerTest {
    private val restOperations: RestOperations = mockk()
    private lateinit var context: AnnotationConfigApplicationContext
    private lateinit var identConsumer: IdentConsumer

    @BeforeEach
    fun oppsett() {
        context = AnnotationConfigApplicationContext()
        context.beanFactory.registerSingleton("restOperations", restOperations)
        context.register(CacheTestConfig::class.java)
        context.refresh()
        identConsumer = context.getBean(IdentConsumer::class.java)
    }

    @AfterEach
    fun rydd() {
        context.close()
    }

    @ParameterizedTest
    @MethodSource("oppslag")
    fun `skal returnere og cache svaret`(oppslag: Oppslag) {
        stubSvar(oppslag.svar)

        oppslag.kall(identConsumer) shouldBe oppslag.forventet
        oppslag.kall(identConsumer) shouldBe oppslag.forventet

        verifiserAntallKall(1)
    }

    @ParameterizedTest
    @MethodSource("oppslag")
    fun `skal returnere null uten å cache når personen ikke finnes`(oppslag: Oppslag) {
        stubFeil(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "", HttpHeaders.EMPTY, ByteArray(0), null))

        oppslag.kall(identConsumer) shouldBe null
        oppslag.kall(identConsumer) shouldBe null

        verifiserAntallKall(2)
    }

    @ParameterizedTest
    @MethodSource("oppslag")
    fun `skal kaste feil videre uten å cache den`(oppslag: Oppslag) {
        stubFeil(HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "", HttpHeaders.EMPTY, ByteArray(0), null))
        shouldThrow<HttpServerErrorException> { oppslag.kall(identConsumer) }

        stubSvar(oppslag.svar)
        oppslag.kall(identConsumer) shouldBe oppslag.forventet
    }

    @Test
    fun `hentAlleIdenter skal returnere innsendt ident uten kall når den ikke er personident`() {
        identConsumer.hentAlleIdenter("123") shouldBe listOf("123")

        verifiserAntallKall(0)
    }

    private fun stubSvar(svar: Any?) {
        every { restOperations.exchange(any<URI>(), HttpMethod.POST, any(), any<ParameterizedTypeReference<Any>>()) } returns
            ResponseEntity.ok().body<Any>(svar)
    }

    private fun stubFeil(feil: Exception) {
        every { restOperations.exchange(any<URI>(), HttpMethod.POST, any(), any<ParameterizedTypeReference<Any>>()) } throws feil
    }

    private fun verifiserAntallKall(antall: Int) {
        verify(exactly = antall) { restOperations.exchange(any<URI>(), HttpMethod.POST, any(), any<ParameterizedTypeReference<Any>>()) }
    }

    class Oppslag(
        private val navn: String,
        val svar: Any,
        val forventet: Any,
        val kall: (IdentConsumer) -> Any?,
    ) {
        override fun toString() = navn
    }

    @Configuration
    @EnableCaching
    class CacheTestConfig {
        @Bean
        fun cacheManager(): CacheManager = ConcurrentMapCacheManager()

        @Bean
        fun identConsumer(restOperations: RestOperations) = IdentConsumer("http://bidrag-person", restOperations)
    }

    companion object {
        private val IDENT = genererFødselsnummer()
        private val HISTORISK_IDENT = genererFødselsnummer()
        private val PERSONIDENTER =
            listOf(
                PersonidentDto(IDENT, false, Identgruppe.FOLKEREGISTERIDENT),
                PersonidentDto(HISTORISK_IDENT, true, Identgruppe.FOLKEREGISTERIDENT),
            )
        private val PERSON = PersonDto(Personident(IDENT))

        @JvmStatic
        fun oppslag() = listOf(
            Oppslag("hentAlleIdenter", PERSONIDENTER, listOf(IDENT, HISTORISK_IDENT)) { it.hentAlleIdenter(IDENT) },
            Oppslag("sjekkIdent", PERSONIDENTER, IDENT) { it.sjekkIdent(HISTORISK_IDENT) },
            Oppslag("hentPersonInformasjon", PERSON, PERSON) { it.hentPersonInformasjon(Personident(IDENT)) },
        )
    }
}

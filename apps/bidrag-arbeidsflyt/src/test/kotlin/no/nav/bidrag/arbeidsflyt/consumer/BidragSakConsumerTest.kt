package no.nav.bidrag.arbeidsflyt.consumer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.arbeidsflyt.CacheConfig
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.sak.BidragssakDto
import org.junit.jupiter.api.Test
import org.springframework.aop.framework.ProxyFactory
import org.springframework.cache.annotation.AnnotationCacheOperationSource
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.cache.interceptor.CacheInterceptor
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

internal class BidragSakConsumerTest {
    private val operations = mockk<RestOperations>()
    private val sak = BidragssakDto(
        eierfogd = Enhetsnummer("4833"),
        saksnummer = Saksnummer("1234567"),
        saksstatus = Bidragssakstatus.IN,
        kategori = Sakskategori.NASJONAL,
        opprettetDato = LocalDate.parse("2026-01-01"),
        levdeAdskilt = false,
        ukjentPart = false,
    )
    private val consumer = ProxyFactory(
        BidragSakConsumer(URI.create("http://localhost/bidrag-sak"), operations, false),
    ).apply {
        addAdvice(
            CacheInterceptor().apply {
                setCacheManager(ConcurrentMapCacheManager(CacheConfig.SAK_CACHE))
                setCacheOperationSource(AnnotationCacheOperationSource())
                afterPropertiesSet()
                afterSingletonsInstantiated()
            },
        )
    }.proxy as BidragSakConsumer

    @Test
    fun `skal bruke cache ved vanlig saksoppslag`() {
        every {
            operations.exchange(any<URI>(), HttpMethod.GET, any(), any<ParameterizedTypeReference<BidragssakDto>>())
        } returns ResponseEntity.ok(sak)

        consumer.hentSak("1234567") shouldBe sak
        consumer.hentSak("1234567") shouldBe sak

        verify(exactly = 1) {
            operations.exchange(any<URI>(), HttpMethod.GET, any(), any<ParameterizedTypeReference<BidragssakDto>>())
        }
    }

    @Test
    fun `skal hente fersk sak uten å lese eller fylle cache`() {
        val nySak = sak.copy(eierfogd = Enhetsnummer("4806"))
        every {
            operations.exchange(any<URI>(), HttpMethod.GET, any(), any<ParameterizedTypeReference<BidragssakDto>>())
        } returns ResponseEntity.ok(sak) andThen ResponseEntity.ok(nySak)

        consumer.hentSak("1234567") shouldBe sak
        consumer.hentSakUtenCache("1234567") shouldBe nySak
        consumer.hentSakUtenCache("1234567") shouldBe nySak
        consumer.hentSak("1234567") shouldBe sak

        verify(exactly = 3) {
            operations.exchange(any<URI>(), HttpMethod.GET, any(), any<ParameterizedTypeReference<BidragssakDto>>())
        }
    }
}

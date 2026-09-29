package no.nav.bidrag.grunnlag.service

import jakarta.annotation.PostConstruct
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class HentHistoriskeValutakurserServiceTest {
    @Test
    fun `historisk kursinnhenting starter ikke ved oppstart`() {
        val innhenting = HentHistoriskeValutakurserService::class.java.getDeclaredMethod("hentHistoriskeValutakurser")

        assertFalse(innhenting.isAnnotationPresent(PostConstruct::class.java))
    }
}

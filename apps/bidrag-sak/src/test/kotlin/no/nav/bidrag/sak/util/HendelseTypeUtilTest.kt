package no.nav.bidrag.sak.util

import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.behandling.HendelseType
import org.junit.jupiter.api.Test

class HendelseTypeUtilTest {
    @Test
    fun `null type skal ikke være vedtakstype eller vedtak-eller-avvisning`() {
        val type: HendelseType? = null

        type.erVedtakstype() shouldBe false
        type.erVedtakEllerAvvisning() shouldBe false
    }
}

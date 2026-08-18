package no.nav.bidrag.sak.util

import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class VedtakstypeMapperTest {
    @ParameterizedTest(name = "kode ''{0}'' skal gi {1}")
    @CsvSource(
        "FA, FASTSETTELSE",
        "IG, INNKREVING",
        "PA, INNKREVING",
        "IR, INDEKSREGULERING",
        "KB, KLAGE",
        "KL, KLAGE",
        "KM, KLAGE",
        "RB, REVURDERING",
        "RF, REVURDERING",
        "FF, REVURDERING",
        "FK, REVURDERING",
        "OH, OPPHØR",
        "OA, ALDERSOPPHØR",
        "OF, ALDERSJUSTERING",
        "AJ, ALDERSJUSTERING",
        "EN, ENDRING",
    )
    fun `skal mappe sokn_type kode til riktig Vedtakstype`(
        kode: String,
        forventet: Vedtakstype,
    ) {
        kode.tilVedtakstype() shouldBe forventet
    }

    @Test
    fun `ukjent kode skal gi null`() {
        "XX".tilVedtakstype() shouldBe null
    }

    @Test
    fun `tom streng skal gi null`() {
        "".tilVedtakstype() shouldBe null
    }
}

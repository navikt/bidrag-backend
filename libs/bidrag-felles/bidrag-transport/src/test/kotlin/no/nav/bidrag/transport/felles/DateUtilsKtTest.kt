package no.nav.bidrag.transport.felles

import io.kotest.matchers.shouldBe
import java.time.LocalDateTime
import kotlin.test.Test

class DateUtilsKtTest {
    @Test
    fun `skal konvertere dato til string`() {
        val dato = LocalDateTime.of(2021, 1, 1, 2, 3, 4)
        dato.toCompactString() shouldBe "20210101020304"
    }
}

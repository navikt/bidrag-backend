package no.nav.bidrag.beregn.inntekt.util

import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VersionProviderTest {

    @Test
    @Disabled
    fun `Skal hente versjon fra fil`() {
        VersionProvider.APP_VERSJON shouldStartWith (LocalDate.now().year.toString())
    }
}

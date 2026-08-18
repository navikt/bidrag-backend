package no.nav.bidrag.dokument.journalpost.dto

import no.nav.bidrag.dokument.journalpost.model.BATCH_NAVN_JOARK_15
import no.nav.bidrag.transport.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class JournalpostInternTest {
    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal hente hente SKAN_BID som kilde når det ikke er et batchnavn`() {
        val jp = JournalpostIntern(batchNavn = null)

        assertThat(jp.kanal).isEqualTo(Kanal.SKAN_BID)
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal hente SKAN_BID som kilde når batchnavet ikke starter med BJOARK015`() {
        val jp = JournalpostIntern(batchNavn = "starter ikke med $BATCH_NAVN_JOARK_15")

        assertThat(jp.kanal).isEqualTo(Kanal.SKAN_BID)
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal hente NAV_NO_BID som kilde når batchnavet starter med BJOARK015`() {
        val jp = JournalpostIntern(batchNavn = "$BATCH_NAVN_JOARK_15 er starten på batchnavnet")

        assertThat(jp.kanal).isEqualTo(Kanal.NAV_NO_BID)
    }
}

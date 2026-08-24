package no.nav.bidrag.dokument.bestilling.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveMaxLength
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMalBrevserver
import no.nav.bidrag.dokument.bestilling.bestilling.dto.alleDokumentmaler
import no.nav.bidrag.dokument.bestilling.bestilling.dto.dokumentmalerBrevserver
import no.nav.bidrag.dokument.bestilling.bestilling.dto.dokumentmalerFarskap
import no.nav.bidrag.dokument.bestilling.bestilling.dto.dokumentmalerUtland
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

private val LOGGER = KotlinLogging.logger {}

class DokumentMalTest {
    @Test
    @Disabled
    fun `skal mappe dokumentmaler to json`() {
        val filter =
            SimpleFilterProvider()
                .addFilter(
                    "myFilter",
                    SimpleBeanPropertyFilter
                        .serializeAllExcept(
                            "filePath",
                            "innholdtype",
                            "tittel",
                            "bestillingSystem",
                            "folderName",
                            "tilhørerEnheter",
                        ),
                )

        LOGGER.info {
            ObjectMapper()
                .findAndRegisterModules()
                .writer(filter)
                .writeValueAsString(dokumentmalerBrevserver.filter { it is DokumentMalBrevserver })
        }

        val filter2 =
            SimpleFilterProvider()
                .addFilter(
                    "myFilter",
                    SimpleBeanPropertyFilter
                        .serializeAllExcept(
                            "filePath",
                            "innholdtype",
                            "bestillingSystem",
                            "folderName",
                            "batchbrev",
                            "enabled",
                            "redigerbar",
                            "kreverDataGrunnlag",
                            "tilhørerEnheter",
                            "dokumentType",
                        ),
                )

        LOGGER.info {
            ObjectMapper()
                .findAndRegisterModules()
                .writer(filter2)
                .writeValueAsString(dokumentmalerUtland)
        }
    }

    @Test
    fun `Skal ikke ha duplikat dokumentmal kode`() {
        alleDokumentmaler.forEach { mal ->
            val antall = alleDokumentmaler.filter { it.kode == mal.kode }.size
            withClue("Flere dokumentmaler har kode ${mal.kode}. Dokumentmal kode må være unik") {
                antall shouldBe 1
            }
        }
    }

    @Test
    fun `Skal ikke ha dokumentmal kode lengre enn 50 tegn`() {
        assertSoftly {
            alleDokumentmaler.forEach { mal ->
                withClue("Dokumentmal med kode ${mal.kode} er lengre enn 50 tegn") {
                    mal.kode shouldHaveMaxLength 49
                }
            }
        }
    }

    @Test
    fun `Skal hente dokumentmaler utland`() {
        dokumentmalerUtland.filter { it.kode.startsWith("UTLAND_") }.size shouldBe 17
    }

    @Test
    fun `Skal hente dokumentmaler farskap`() {
        dokumentmalerFarskap.filter { it.kode.startsWith("FARSKAP_") }.size shouldBe 35
        dokumentmalerFarskap.forEach {
            it.gruppeVisningsnavn shouldNotBe null
        }
    }
}

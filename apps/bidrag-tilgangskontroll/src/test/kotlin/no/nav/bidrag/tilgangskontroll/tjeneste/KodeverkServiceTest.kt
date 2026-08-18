package no.nav.bidrag.tilgangskontroll.tjeneste

import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KodeverkServiceTest {
    private lateinit var kodeverkService: KodeverkService

    @BeforeEach
    fun setup() {
        kodeverkService = KodeverkService()
        kodeverkService.init()
    }

    @Test
    fun `skal hente riktig GA-kode for behandlingstema BIDRAG`() {
        val adgruppe = kodeverkService.hentAdgruppe(Behandlingstema.BIDRAG)
        adgruppe shouldBe "0000-GA-Bisys-Bidrag"
    }

    @Test
    fun `skal hente riktig GA-kode for behandlingstema EKTEFELLEBIDRAG`() {
        val adgruppe = kodeverkService.hentAdgruppe(Behandlingstema.EKTEFELLEBIDRAG)
        adgruppe shouldBe "0000-GA-Bisys-Ektefellebidrag"
    }

    @Test
    fun `skal hente riktig GA-kode for behandlingstema FARSSKAP`() {
        val adgruppe = kodeverkService.hentAdgruppe(Behandlingstema.FARSSKAP)
        adgruppe shouldBe "0000-GA-TEMA_FAR"
    }

    @Test
    fun `skal hente riktig GA-kode for behandlingstema MOTREGNING`() {
        val adgruppe = kodeverkService.hentAdgruppe(Behandlingstema.MOTREGNING)
        adgruppe shouldBe "0000-GA-Bisys-Motregning"
    }

    @Test
    fun `skal hente riktig GA-kode for behandlingstema REISEKOSTNADER`() {
        val adgruppe = kodeverkService.hentAdgruppe(Behandlingstema.REISEKOSTNADER)
        adgruppe shouldBe "0000-GA-Bisys-Reisekostnader"
    }
}

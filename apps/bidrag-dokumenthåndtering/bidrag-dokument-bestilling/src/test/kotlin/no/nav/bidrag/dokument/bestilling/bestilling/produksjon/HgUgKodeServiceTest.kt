package no.nav.bidrag.dokument.bestilling.bestilling.produksjon

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.bidrag.dokument.bestilling.model.BehandlingType
import no.nav.bidrag.dokument.bestilling.model.SoknadType
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import org.junit.jupiter.api.Test

class HgUgKodeServiceTest {
    private val hgUgKodeService = HgUgKodeService()

    @Test
    fun `skal hente hg og ug for søknad fra bidragsmottaker`() {
        val hgUg = hgUgKodeService.findHgUg(SoknadType.SOKNAD, SøktAvType.BIDRAGSMOTTAKER, BehandlingType.BIDRAG)

        assertSoftly(hgUg.shouldNotBeNull()) {
            hg shouldBe "BB"
            ug shouldBe "S2"
        }
    }

    @Test
    fun `skal bruke hg og ug for NAV_BIDRAG når søknad er opprettet automatisk`() {
        val hgUg = hgUgKodeService.findHgUg(SoknadType.SOKNAD, SøktAvType.AUTOMATISK, BehandlingType.BIDRAG)
        val hgUgNavBidrag = hgUgKodeService.findHgUg(SoknadType.SOKNAD, SøktAvType.NAV_BIDRAG, BehandlingType.BIDRAG)

        assertSoftly(hgUg.shouldNotBeNull()) {
            hg shouldBe "BB"
            ug shouldBe "R"
            hg shouldBe hgUgNavBidrag?.hg
            ug shouldBe hgUgNavBidrag?.ug
        }
    }
}

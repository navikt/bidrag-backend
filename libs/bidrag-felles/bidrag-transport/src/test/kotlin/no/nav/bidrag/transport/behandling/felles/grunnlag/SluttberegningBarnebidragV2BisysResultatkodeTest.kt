package no.nav.bidrag.transport.behandling.felles.grunnlag

import com.fasterxml.jackson.databind.node.POJONode
import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class SluttberegningBarnebidragV2BisysResultatkodeTest {
    private val periode = ÅrMånedsperiode(YearMonth.parse("2026-05"), null)
    private val deltBosted = SluttberegningBarnebidragV2(periode, null, null, bidragIkkeBeregnetDeltBosted = true)
    private val forpleining = SluttberegningBarnebidragV2(periode, null, null, forpleiningDekkerUnderholdskostnaden = true)
    private val kostnadsberegnet = SluttberegningBarnebidragV2(periode, BigDecimal.TEN, BigDecimal.TEN)

    private fun SluttberegningBarnebidragV2.bisysResultatkode(
        vedtakstype: Vedtakstype? = null,
        løperBidrag: Boolean? = null,
    ) = listOf(
        GrunnlagDto(
            referanse = "sluttberegning",
            type = Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG,
            innhold = POJONode(this),
        ),
    ).tilBisysResultatkode(listOf("sluttberegning"), vedtakstype, løperBidrag)

    @Test
    fun `skal gi avslagskode for avslag`() {
        deltBosted.bisysResultatkode() shouldBe "ADN"
        forpleining.bisysResultatkode() shouldBe "AFU"
        deltBosted.bisysResultatkode(Vedtakstype.FASTSETTELSE, løperBidrag = false) shouldBe "ADN"
        forpleining.bisysResultatkode(Vedtakstype.FASTSETTELSE, løperBidrag = false) shouldBe "AFU"
    }

    @Test
    fun `skal gi opphørskode når bidraget løper eller vedtaket er opphør`() {
        deltBosted.bisysResultatkode(Vedtakstype.ENDRING, løperBidrag = true) shouldBe "ODN"
        forpleining.bisysResultatkode(Vedtakstype.ENDRING, løperBidrag = true) shouldBe "OFU"
        deltBosted.bisysResultatkode(Vedtakstype.OPPHØR) shouldBe "ODN"
        forpleining.bisysResultatkode(Vedtakstype.OPPHØR) shouldBe "OFU"
    }

    @Test
    fun `skal gi samme kode som før for resultat uten opphørskode`() {
        kostnadsberegnet.bisysResultatkode() shouldBe "KBB"
        kostnadsberegnet.bisysResultatkode(Vedtakstype.ENDRING, løperBidrag = true) shouldBe "KBB"
    }
}

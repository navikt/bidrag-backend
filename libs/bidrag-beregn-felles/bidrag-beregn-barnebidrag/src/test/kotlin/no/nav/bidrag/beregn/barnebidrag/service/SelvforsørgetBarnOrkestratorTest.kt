package no.nav.bidrag.beregn.barnebidrag.service

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.beregn.barnebidrag.felles.FellesTest
import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningPersonConsumer
import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningSakConsumer
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.BidragsberegningOrkestrator
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.HentLøpendeBidragService
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.OmgjøringOrkestrator
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.OmgjøringOrkestratorV2
import no.nav.bidrag.commons.web.mock.stubSjablonProvider
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BidragsberegningOrkestratorRequestV2
import no.nav.bidrag.transport.behandling.felles.grunnlag.resultatSluttberegning
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class SelvforsørgetBarnOrkestratorTest : FellesTest() {
    @MockK(relaxed = true)
    private lateinit var omgjøringOrkestrator: OmgjøringOrkestrator

    @MockK(relaxed = true)
    private lateinit var omgjøringOrkestratorV2: OmgjøringOrkestratorV2

    @MockK(relaxed = true)
    private lateinit var hentLøpendeBidragService: HentLøpendeBidragService

    @MockK(relaxed = true)
    private lateinit var personConsumer: BeregningPersonConsumer

    @MockK(relaxed = true)
    private lateinit var sakConsumer: BeregningSakConsumer

    @BeforeEach
    fun init() {
        stubSjablonProvider()
    }

    @Test
    fun `orkestrator skal gi resultat barnet er selvforsørget uten beregningsfeil når et barn er selvforsørget`() {
        val request = lesFilOgByggRequestGenerisk<BidragsberegningOrkestratorRequestV2>(
            "src/test/resources/testfiler/bidragsberegning_orkestrator/test01_v3_beregn_bidrag_selvforsorget_grunnlag.json",
        )

        val respons = orkestrator().utførBidragsberegningV3(request)

        val resultatkoder =
            respons.resultat.flatMap { barn ->
                barn.resultatVedtakListe.flatMap { vedtak ->
                    vedtak.periodeListe.map { respons.grunnlagListe.resultatSluttberegning(it.grunnlagsreferanseListe) }
                }
            }
        assertSoftly(respons) {
            resultat shouldHaveSize 2
            resultat.forEach { it.beregningsfeil shouldBe null }
            resultatkoder shouldContain Resultatkode.BARNET_ER_SELVFORSØRGET
        }
    }

    private fun orkestrator() = BidragsberegningOrkestrator(
        barnebidragApi = BeregnBarnebidragApi(),
        omgjøringOrkestrator = omgjøringOrkestrator,
        omgjøringOrkestratorV2 = omgjøringOrkestratorV2,
        hentLøpendeBidragService = hentLøpendeBidragService,
        personConsumer = personConsumer,
        sakConsumer = sakConsumer,
    )
}

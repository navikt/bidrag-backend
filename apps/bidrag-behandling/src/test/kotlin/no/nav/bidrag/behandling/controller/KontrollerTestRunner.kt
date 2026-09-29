package no.nav.bidrag.behandling.controller

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.MockkSpyBean
import io.getunleash.Unleash
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockkObject
import no.nav.bidrag.behandling.TestPostgres
import no.nav.bidrag.behandling.service.CommonTestRunner
import no.nav.bidrag.behandling.utils.StubUtils
import no.nav.bidrag.behandling.utils.stubPersonConsumer
import no.nav.bidrag.behandling.utils.stubVedtakConsumer
import no.nav.bidrag.behandling.utils.testdata.TestdataManager
import no.nav.bidrag.behandling.utils.testdata.opprettSakForBehandling
import no.nav.bidrag.behandling.utils.testdata.oppretteBehandling
import no.nav.bidrag.commons.service.organisasjon.SaksbehandlernavnProvider
import no.nav.bidrag.commons.service.sjablon.SjablonService
import no.nav.bidrag.commons.web.mock.stubKodeverkProvider
import no.nav.bidrag.commons.web.mock.stubSjablonProvider
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@ActiveProfiles(value = ["test", "testcontainer"])
abstract class KontrollerTestRunner : CommonTestRunner() {
    companion object {
        @Suppress("unused")
        @JvmStatic
        @DynamicPropertySource
        fun postgresqlProperties(registry: DynamicPropertyRegistry) {
            TestPostgres.registrerProperties(registry)
            registry.add("spring.datasource.hikari.connection-timeout") { 30000 }
        }
    }

    @LocalServerPort
    private val port = 0

    @Autowired
    lateinit var httpHeaderTestRestTemplate: TestRestTemplate

    @Autowired
    lateinit var httpHeaderTestRestTemplateNoJackson: TestRestTemplate

    @Autowired
    lateinit var testdataManager: TestdataManager

    @MockkBean
    lateinit var unleashInstance: Unleash

    @MockkSpyBean
    lateinit var sjablonService: SjablonService

    val stubUtils: StubUtils = StubUtils()

    protected fun rootUriV1(): String = "http://localhost:$port/api/v1"

    protected fun rootUriV2(): String = "http://localhost:$port/api/v2"

    @BeforeEach
    fun initMocks() {
        stubVedtakConsumer()
        clearMocks(unleashInstance, sjablonService)
        every { unleashInstance.isEnabled(any(), any<Boolean>()) } returns true
        every { unleashInstance.isEnabled(eq("vedtakssperre"), any<Boolean>()) } returns false
        mockkObject(SaksbehandlernavnProvider)
        every { SaksbehandlernavnProvider.hentSaksbehandlernavn(any()) } returns "Fornavn Etternavn"
        stubSjablonProvider()
        stubPersonConsumer()
        stubKodeverkProvider()
        stubUtils.stubUnleash()
        stubUtils.stubHentePersonInfoForTestpersoner()
        stubUtils.stubHentSaksbehandler()
        stubUtils.stubOpprettForsendelse()
        stubUtils.stubSlettForsendelse()
        stubUtils.stubHentForsendelserForSak()
        stubUtils.stubTilgangskontrollTema()
        stubUtils.stubHentePersoninfo(personident = "12345")
        stubUtils.stubKodeverkSkattegrunnlag()
        stubUtils.stubKodeverkLønnsbeskrivelse()
        stubUtils.stubKodeverkNaeringsinntektsbeskrivelser()
        stubUtils.stubKodeverkYtelsesbeskrivelser()
        stubUtils.stubKodeverkPensjonsbeskrivelser()
        stubUtils.stubKodeverkSpesifisertSummertSkattegrunnlag()
        stubUtils.stubTilgangskontrollSak()
        stubUtils.stubTilgangskontrollPerson()
        stubUtils.stubTilgangskontrollPersonISak()
        stubUtils.stubBidragBeløpshistorikkLøpendeSaker()
        stubUtils.stubHentSak(opprettSakForBehandling(oppretteBehandling()))
    }
}

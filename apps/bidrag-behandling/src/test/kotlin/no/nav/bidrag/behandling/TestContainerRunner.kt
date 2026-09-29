package no.nav.bidrag.behandling

import com.ninjasquad.springmockk.MockkSpyBean
import no.nav.bidrag.commons.service.sjablon.SjablonService
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@ActiveProfiles(value = ["test", "testcontainer"])
class TestContainerRunner : SpringTestRunner() {
    // Delt spy slik at tester kan stubbe sjabloner uten å opprette egen Spring-kontekst
    @MockkSpyBean
    lateinit var sjablonService: SjablonService

    companion object {
        @Suppress("unused")
        @JvmStatic
        @DynamicPropertySource
        fun postgresqlProperties(registry: DynamicPropertyRegistry) {
            TestPostgres.registrerProperties(registry, TestPostgres.testContainerRunnerDb)
        }
    }
}

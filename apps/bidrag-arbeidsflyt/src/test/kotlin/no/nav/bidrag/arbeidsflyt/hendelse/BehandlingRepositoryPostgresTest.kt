package no.nav.bidrag.arbeidsflyt.hendelse

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.bidrag.arbeidsflyt.persistence.repository.BehandlingRepository
import no.nav.bidrag.arbeidsflyt.service.BehandleBehandlingHendelseService
import no.nav.bidrag.arbeidsflyt.utils.opprettSakForBehandling
import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknadResponse
import no.nav.bidrag.transport.behandling.beregning.felles.PartISøknad
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelse
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelseBarn
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelseType
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import no.nav.bidrag.transport.dokument.Sporingsdata
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.LocalDate
import java.time.LocalDateTime

// Kjører mot ekte Postgres fordi spørringene bruker jsonb-operatorer som H2 ikke støtter.
internal class BehandlingRepositoryPostgresTest : AbstractBehandleHendelseTest() {
    companion object {
        private val postgres: PostgreSQLContainer by lazy {
            PostgreSQLContainer("postgres:15-alpine").apply {
                withDatabaseName("bidrag-arbeidsflyt")
                withUsername("cloudsqliamuser")
                withPassword("admin")
                start()
            }
        }

        @JvmStatic
        @DynamicPropertySource
        fun postgresProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jpa.database-platform") { "org.hibernate.dialect.PostgreSQLDialect" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
            registry.add("spring.sql.init.mode") { "never" }
            registry.add("spring.flyway.enabled") { true }
            registry.add("spring.flyway.locations") { "classpath:/db/migration" }
        }
    }

    @Autowired
    lateinit var behandleHendelseService: BehandleBehandlingHendelseService

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Test
    fun `skal ikke hente avsluttet behandling selv om barn fortsatt er under behandling`() {
        val behandlingsid = 123123L
        val hendelse = opprettHendelse(behandlingsid)
        stubHentSak(opprettSakForBehandling(hendelse.barn.first()))
        behandleHendelseService.behandleHendelse(hendelse)

        val behandling = behandlingRepository.finnForBehandlingId(behandlingsid)
        behandling.shouldNotBeNull()
        val cutoff = LocalDateTime.now().plusHours(1)
        behandlingRepository
            .finnBehandlingerMedSøknadUnderBehandlingStatusSjekketEldreEnn(cutoff)
            .map { it.id } shouldBe listOf(behandling.id)

        behandling.status = BehandlingStatusType.AVBRUTT
        behandlingRepository.save(behandling)

        behandlingRepository
            .finnBehandlingerMedSøknadUnderBehandlingStatusSjekketEldreEnn(cutoff) shouldBe emptyList()
    }

    @Test
    fun `skal oppdatere barnestatus fra BBM`() {
        val behandlingsid = 123124L
        val søknadsid = 123124L
        stubHentBBMSøknad(søknadsid, Behandlingstatus.VEDTAK_FATTET)

        behandleHendelseService.behandleHendelse(opprettHendelse(behandlingsid, søknadsid))

        val behandling = behandlingRepository.finnForBehandlingId(behandlingsid)
        behandling.shouldNotBeNull()
        behandling.barn!!.barn.first().status shouldBe Behandlingstatus.VEDTAK_FATTET
    }

    @Test
    fun `skal kunne prøve på nytt når BBM-kall feiler for avsluttet barn`() {
        val behandlingsid = 123125L
        val søknadsid = 123125L
        val underBehandlingHendelse = opprettHendelse(behandlingsid, søknadsid)
        val hendelse =
            underBehandlingHendelse.copy(
                type = BehandlingHendelseType.AVSLUTTET,
                status = BehandlingStatusType.AVBRUTT,
                barn = underBehandlingHendelse.barn.map { it.copy(status = Behandlingstatus.VEDTAK_FATTET) },
            )
        WireMock.stubFor(
            WireMock.post("/bbm/api/beregning/hentsoknad")
                .willReturn(WireMock.aResponse().withStatus(500)),
        )

        behandleHendelseService.behandleHendelse(hendelse)

        val behandling = behandlingRepository.finnForBehandlingId(behandlingsid)
        behandling.shouldNotBeNull()
        behandling.barn!!.barn.first().status shouldBe Behandlingstatus.UNDER_BEHANDLING
        behandlingRepository
            .finnBehandlingerMedSøknadUnderBehandlingStatusSjekketEldreEnn(LocalDateTime.now().plusHours(1))
            .map { it.id } shouldBe listOf(behandling.id)
    }

    private fun stubHentBBMSøknad(
        søknadsid: Long,
        barnStatus: Behandlingstatus,
    ) {
        val response =
            HentSøknadResponse(
                HentSøknad(
                    søknadsid = søknadsid,
                    søknadMottattDato = LocalDate.parse("2020-06-01"),
                    behandlingstema = Behandlingstema.BIDRAG,
                    saksnummer = "123456",
                    innkreving = true,
                    søktAvType = SøktAvType.BIDRAGSMOTTAKER,
                    behandlingStatusType = BehandlingStatusType.UNDER_BEHANDLING,
                    partISøknadListe =
                    listOf(
                        PartISøknad(
                            personident = "123213",
                            rolletype = Rolletype.BARN,
                            behandlingstatus = barnStatus,
                        ),
                    ),
                ),
            )
        WireMock.stubFor(
            WireMock.post("/bbm/api/beregning/hentsoknad")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(response)),
                ),
        )
    }

    private fun opprettHendelse(
        behandlingsid: Long,
        søknadsid: Long = 123,
    ) = BehandlingHendelse(
        type = BehandlingHendelseType.OPPRETTET,
        status = BehandlingStatusType.UNDER_BEHANDLING,
        vedtakstype = Vedtakstype.ENDRING,
        opprettetTidspunkt = LocalDateTime.now(),
        endretTidspunkt = LocalDateTime.now(),
        mottattDato = LocalDate.parse("2020-06-01"),
        behandlingsid = behandlingsid,
        behandlerEnhet = "4806",
        søknadsid = søknadsid,
        sporingsdata = Sporingsdata("test", "test", "test", "4806"),
        barn =
        listOf(
            BehandlingHendelseBarn(
                saksnummer = "123456",
                behandlingstype = Behandlingstype.ENDRING,
                behandlingstema = Behandlingstema.BIDRAG,
                status = Behandlingstatus.UNDER_BEHANDLING,
                stønadstype = Stønadstype.BIDRAG,
                medInnkreving = true,
                søktAv = SøktAvType.BIDRAGSMOTTAKER,
                søktFraDato = LocalDate.parse("2020-06-01"),
                ident = "123213",
                søknadsid = søknadsid,
                behandlerEnhet = "4806",
            ),
        ),
    )
}

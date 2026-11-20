package no.nav.bidrag.sak.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.sak.domain.Bidragssak
import no.nav.bidrag.sak.integration.kafka.KafkaProducer
import no.nav.bidrag.sak.repository.HendelseRepository
import no.nav.bidrag.transport.sak.SakHendelse
import no.nav.bidrag.transport.sak.SakKafkaHendelsestype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.transaction.IllegalTransactionStateException
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.support.TransactionTemplate
import java.sql.Connection
import javax.sql.DataSource

@SpringJUnitConfig(SakHendelsePubliseringTest.Konfigurasjon::class)
@ActiveProfiles("sak-hendelse-publisering-test")
internal class SakHendelsePubliseringTest {
    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    private lateinit var hendelseService: HendelseService

    @Autowired
    private lateinit var kafkaProducer: KafkaProducer

    private val sakshendelseSlot = slot<SakHendelse>()
    private val bidragssak = Bidragssak("1234567", "4833")

    @BeforeEach
    fun setUp() {
        clearMocks(kafkaProducer)
        jdbcTemplate.update("DELETE FROM sak_markor")
    }

    @Test
    fun `sender ikke hendelse før transaksjonen er committed`() {
        TransactionTemplate(transactionManager).executeWithoutResult {
            hendelseService.opprettKafkaHendelse(null, bidragssak)
            verify(exactly = 0) { kafkaProducer.sendSakshendelse(any()) }
        }
    }

    @Test
    fun `sender hendelse etter commit med uendret payload og committed markør`() {
        var markørerVedSending: Int? = null
        every { kafkaProducer.sendSakshendelse(capture(sakshendelseSlot)) } answers {
            markørerVedSending = lesMarkørerPåUavhengigTilkobling(dataSource)
        }

        TransactionTemplate(transactionManager).executeWithoutResult {
            jdbcTemplate.update("INSERT INTO sak_markor (id, verdi) VALUES (?, ?)", MARKØR_ID, MARKØR_VERDI)
            hendelseService.opprettKafkaHendelse(bidragssak, bidragssak)
        }

        verify(exactly = 1) { kafkaProducer.sendSakshendelse(any()) }
        markørerVedSending shouldBe 1
        sakshendelseSlot.captured shouldBe
            SakHendelse(
                saksnummer = Saksnummer("1234567"),
                hendelsestype = SakKafkaHendelsestype.ENDRING,
            )
    }

    @Test
    fun `sender ikke hendelse når transaksjonen rulles tilbake`() {
        shouldThrow<IllegalStateException> {
            TransactionTemplate(transactionManager).executeWithoutResult {
                jdbcTemplate.update("INSERT INTO sak_markor (id, verdi) VALUES (?, ?)", MARKØR_ID, MARKØR_VERDI)
                hendelseService.opprettKafkaHendelse(null, bidragssak)
                throw IllegalStateException("rollback")
            }
        }

        verify(exactly = 0) { kafkaProducer.sendSakshendelse(any()) }
        lesMarkørerPåUavhengigTilkobling(dataSource) shouldBe 0
    }

    @Test
    fun `krever transaksjon og sender ikke ved kall utenfor transaksjon`() {
        shouldThrow<IllegalTransactionStateException> {
            hendelseService.opprettKafkaHendelse(null, bidragssak)
        }

        verify(exactly = 0) { kafkaProducer.sendSakshendelse(any()) }
    }

    @TestConfiguration
    @Profile("sak-hendelse-publisering-test")
    @EnableTransactionManagement
    @Import(HendelseService::class, SakHendelsePubliseringLytter::class)
    class Konfigurasjon {
        @Bean
        fun dataSource(): DataSource = EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).generateUniqueName(true).build()

        @Bean
        fun jdbcTemplate(dataSource: DataSource): JdbcTemplate = JdbcTemplate(dataSource).also {
            it.execute("CREATE TABLE sak_markor (id INTEGER PRIMARY KEY, verdi VARCHAR(100) NOT NULL)")
        }

        @Bean
        fun transactionManager(dataSource: DataSource): PlatformTransactionManager = DataSourceTransactionManager(dataSource)

        @Bean
        fun hendelseRepository(): HendelseRepository = mockk(relaxed = true)

        @Bean
        fun kafkaProducer(): KafkaProducer = mockk(relaxed = true)
    }

    companion object {
        private const val MARKØR_ID = 1
        private const val MARKØR_VERDI = "commit marker"

        private fun lesMarkørerPåUavhengigTilkobling(dataSource: DataSource): Int = dataSource.connection.use { connection: Connection ->
            connection
                .prepareStatement("SELECT COUNT(*) FROM sak_markor WHERE id = ?")
                .use { statement ->
                    statement.setInt(1, MARKØR_ID)
                    statement.executeQuery().use { resultSet ->
                        resultSet.next()
                        resultSet.getInt(1)
                    }
                }
        }
    }
}

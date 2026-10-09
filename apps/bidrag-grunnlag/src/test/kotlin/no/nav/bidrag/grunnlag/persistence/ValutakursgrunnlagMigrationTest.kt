package no.nav.bidrag.grunnlag.persistence

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.math.BigDecimal
import java.time.LocalDateTime

@Testcontainers
class ValutakursgrunnlagMigrationTest {
    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("grunnlag")
            .withUsername("cloudsqliamuser")
            .withPassword("migration-test")

        private lateinit var jdbc: JdbcTemplate

        @BeforeAll
        @JvmStatic
        fun migrer() {
            val dataSource = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            val flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()

            flyway.migrate()
            assertTrue(flyway.info().applied().any { it.version?.version == "1.0.38" })
            assertEquals(0, flyway.migrate().migrationsExecuted)
            jdbc = JdbcTemplate(dataSource)
        }
    }

    @BeforeEach
    fun tømValutakursgrunnlag() {
        jdbc.execute("TRUNCATE TABLE valutakursgrunnlag RESTART IDENTITY")
    }

    @ParameterizedTest
    @CsvSource(
        "2025-01-01T00:00:00, 2025-07-01T00:00:00",
        "2025-07-01T00:00:00, 2026-01-01T00:00:00",
    )
    fun `hele halvår kan lagres med generert id og tidsstempler`(fra: LocalDateTime, til: LocalDateTime) {
        lagre(fra, til)

        assertEquals(1, jdbc.queryForObject("SELECT valutakursgrunnlag_id FROM valutakursgrunnlag", Int::class.java))
        assertEquals(
            1,
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM valutakursgrunnlag WHERE hentet_tidspunkt IS NOT NULL AND oppdatert_tidspunkt IS NOT NULL",
                Int::class.java,
            ),
        )
    }

    @Test
    fun `oppdateringstrigger erstatter oppdatert tidspunkt uten å endre hentet tidspunkt`() {
        lagre()
        val opprinnelig = LocalDateTime.of(2000, 1, 1, 0, 0)
        jdbc.update("UPDATE valutakursgrunnlag SET hentet_tidspunkt = ?, oppdatert_tidspunkt = ?, kurs = ?", opprinnelig, opprinnelig, BigDecimal("11"))

        val oppdatert = jdbc.queryForObject("SELECT oppdatert_tidspunkt FROM valutakursgrunnlag", java.sql.Timestamp::class.java)!!.toLocalDateTime()
        val hentet = jdbc.queryForObject("SELECT hentet_tidspunkt FROM valutakursgrunnlag", java.sql.Timestamp::class.java)!!.toLocalDateTime()
        assertTrue(oppdatert.isAfter(opprinnelig))
        assertEquals(opprinnelig, hentet)
        assertEquals(BigDecimal("11.0000000000000000"), jdbc.queryForObject("SELECT kurs FROM valutakursgrunnlag", BigDecimal::class.java))
    }

    @ParameterizedTest
    @CsvSource(
        "2025-02-01T00:00:00, 2025-08-01T00:00:00",
        "2025-01-02T00:00:00, 2025-07-02T00:00:00",
        "2025-01-01T00:00:01, 2025-07-01T00:00:01",
        "2025-01-01T00:00:00, 2025-06-01T00:00:00",
        "2025-07-01T00:00:00, 2025-01-01T00:00:00",
    )
    fun `halvårsconstraint avviser ugyldige perioder`(fra: LocalDateTime, til: LocalDateTime) {
        assertThrows<DataIntegrityViolationException> { lagre(fra, til) }
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM valutakursgrunnlag", Int::class.java))
    }

    @Test
    fun `samme valuta og halvår kan ikke lagres to ganger`() {
        lagre()

        assertThrows<DataIntegrityViolationException> { lagre() }
        lagre(valuta = "EUR")
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM valutakursgrunnlag", Int::class.java))
    }

    @ParameterizedTest
    @CsvSource(
        "HENTET, ECB",
        "HENTET, NORGES_BANK",
        "OVERSTYRT, MANUELL",
        "FEILET,",
    )
    fun `gyldige statuser og kilder kan lagres`(status: String, kilde: String?) {
        lagre(status = status, kilde = kilde, kurs = if (status == "FEILET") null else BigDecimal("10.125"))
        assertEquals(status, jdbc.queryForObject("SELECT status FROM valutakursgrunnlag", String::class.java))
        assertEquals(kilde, jdbc.queryForObject("SELECT kilde FROM valutakursgrunnlag", String::class.java))
    }

    @ParameterizedTest
    @CsvSource("UKJENT, ECB", "HENTET, UKJENT")
    fun `ukjent status eller kilde avvises`(status: String, kilde: String) {
        assertThrows<DataIntegrityViolationException> { lagre(status = status, kilde = kilde) }
    }

    @Test
    fun `numeric kolonnen lagrer maksimal presisjon og avviser overløp`() {
        val maksimalKurs = BigDecimal("9999999999999999999999.9999999999999999")
        lagre(kurs = maksimalKurs)

        assertEquals(maksimalKurs, jdbc.queryForObject("SELECT kurs FROM valutakursgrunnlag", BigDecimal::class.java))
        assertThrows<DataIntegrityViolationException> { lagre(valuta = "EUR", kurs = BigDecimal("10000000000000000000000")) }
    }

    private fun lagre(
        fra: LocalDateTime = LocalDateTime.of(2025, 1, 1, 0, 0),
        til: LocalDateTime = fra.plusMonths(6),
        valuta: String = "USD",
        status: String = "HENTET",
        kilde: String? = "ECB",
        kurs: BigDecimal? = BigDecimal("10.125"),
    ) {
        jdbc.update(
            """
            INSERT INTO valutakursgrunnlag (bruk_fra, bruk_til, basisvaluta, kvoteringsvaluta, status, kilde, kurs, multiplikator, feilet_henting)
            VALUES (?, ?, ?, 'NOK', ?, ?, ?, ?, ?)
            """.trimIndent(),
            fra,
            til,
            valuta,
            status,
            kilde,
            kurs,
            if (kurs == null) null else 0,
            status == "FEILET",
        )
    }
}

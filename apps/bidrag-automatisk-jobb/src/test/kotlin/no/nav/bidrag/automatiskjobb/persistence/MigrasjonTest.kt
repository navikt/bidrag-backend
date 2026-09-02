package no.nav.bidrag.automatiskjobb.persistence

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class MigrasjonTest {
    companion object {
        // Samme som produksjon.
        private const val POSTGRES_IMAGE = "postgres:17"

        @JvmStatic
        private val postgres =
            PostgreSQLContainer(POSTGRES_IMAGE).apply {
                withDatabaseName("bidrag-automatisk-jobb")
                withUsername("bidrag-automatisk-jobb")
                withPassword("admin")
                withInitScript("db/init.sql")
                start()
            }

        @JvmStatic
        private val migreringsresultat =
            Flyway
                .configure()
                .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .locations("classpath:db/migration")
                .load()
                .migrate()
    }

    @Test
    fun `alle migrasjoner skal kjøre uten feil`() {
        migreringsresultat.success shouldBe true
        migreringsresultat.migrationsExecuted shouldBeGreaterThan 0
    }

    @Test
    fun `sak-tabellen skal ha kolonnene entiteten forventer`() {
        kolonnerFor("sak") shouldContainAll
            listOf(
                "id",
                "saksnummer",
                "bidragspliktig",
                "bidragsmottaker",
                "opprettet_tidspunkt",
                "endret_tidspunkt",
            )
    }

    @Test
    fun `sak_barn-tabellen skal ha kolonnene entiteten forventer`() {
        kolonnerFor("sak_barn") shouldContainAll
            listOf("id", "sak_id", "kravhaver", "reell_mottaker", "endret_tidspunkt")
    }

    @Test
    fun `saksnummer skal være unikt`() {
        medTilkobling { connection ->
            connection.opprettSak("SAK-UNIK-1")

            assertThrows<SQLException> { connection.opprettSak("SAK-UNIK-1") }
        }
    }

    @Test
    fun `samme kravhaver skal ikke kunne registreres to ganger på samme sak`() {
        medTilkobling { connection ->
            val sakId = connection.opprettSak("SAK-DUPLIKAT-BARN")
            connection.opprettSakBarn(sakId, "12345678901")

            assertThrows<SQLException> { connection.opprettSakBarn(sakId, "12345678901") }
        }
    }

    @Test
    fun `sletting av sak skal kaskadere til sak_barn`() {
        medTilkobling { connection ->
            val sakId = connection.opprettSak("SAK-KASKADE")
            connection.opprettSakBarn(sakId, "12345678901")

            connection.createStatement().use { it.executeUpdate("DELETE FROM sak WHERE id = $sakId") }

            connection.antallSakBarnFor(sakId) shouldBe 0
        }
    }

    private fun kolonnerFor(tabell: String): List<String> = medTilkobling { connection ->
        connection
            .prepareStatement("SELECT column_name FROM information_schema.columns WHERE table_name = ?")
            .use { statement ->
                statement.setString(1, tabell)
                statement.executeQuery().use { rs ->
                    generateSequence { if (rs.next()) rs.getString("column_name") else null }.toList()
                }
            }
    }

    private fun Connection.opprettSak(saksnummer: String): Int = prepareStatement(
        "INSERT INTO sak (saksnummer) VALUES (?) RETURNING id",
    ).use { statement ->
        statement.setString(1, saksnummer)
        statement.executeQuery().use { rs ->
            rs.next()
            rs.getInt("id")
        }
    }

    private fun Connection.opprettSakBarn(
        sakId: Int,
        kravhaver: String,
    ) = prepareStatement("INSERT INTO sak_barn (sak_id, kravhaver) VALUES (?, ?)").use { statement ->
        statement.setInt(1, sakId)
        statement.setString(2, kravhaver)
        statement.executeUpdate()
    }

    private fun Connection.antallSakBarnFor(sakId: Int): Int = prepareStatement(
        "SELECT COUNT(*) FROM sak_barn WHERE sak_id = ?",
    ).use { statement ->
        statement.setInt(1, sakId)
        statement.executeQuery().use { rs ->
            rs.next()
            rs.getInt(1)
        }
    }

    private fun <T> medTilkobling(block: (Connection) -> T): T = DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use(block)
}

package no.nav.bidrag.aktoerregister.repository

import io.kotest.matchers.shouldBe
import no.nav.bidrag.aktoerregister.AktoerregisterApplicationTest
import no.nav.bidrag.aktoerregister.dto.enumer.Identtype
import no.nav.bidrag.aktoerregister.persistence.entities.Aktør
import no.nav.bidrag.aktoerregister.persistence.entities.Hendelse
import no.nav.bidrag.aktoerregister.persistence.repository.AktørRepository
import no.nav.bidrag.aktoerregister.persistence.repository.HendelseRepository
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.*

@SpringBootTest(classes = [AktoerregisterApplicationTest::class])
@Testcontainers
@EnableMockOAuth2Server
class JpaRepositoryTests {

    @Autowired
    private lateinit var hendelseRepository: HendelseRepository

    @Autowired
    private lateinit var aktørRepository: AktørRepository

    @BeforeEach
    fun setup() {
        aktørRepository.deleteAll()
    }

    @Test
    fun `Skal lagre og slette aktør med hendelser`() {
        val aktoerer = generateAktørListe()
        aktørRepository.saveAll(aktoerer)

        var hendelser = hendelseRepository.findAll()
        var lagdredeAktoerer = aktørRepository.findAll()

        hendelser.size shouldBe 40
        lagdredeAktoerer.size shouldBe 20

        aktørRepository.delete(lagdredeAktoerer[0])

        hendelser = hendelseRepository.findAll()
        lagdredeAktoerer = aktørRepository.findAll()

        hendelser.size shouldBe 38
        lagdredeAktoerer.size shouldBe 19
    }

    private fun generateAktørListe(): List<Aktør> {
        val aktørListe: MutableList<Aktør> = ArrayList()
        for (i in 0..19) {
            val aktør = Aktør(
                aktørIdent = UUID.randomUUID().toString(),
                aktørType = Identtype.PERSONNUMMER.name,
                land = "Norge",
                postnr = "0682",
                poststed = "Oslo",
                adresselinje1 = "Testgate $i",
                norskKontonr = i.toString(),
            )
            aktør.addHendelse(Hendelse(aktør = aktør, aktørIdent = aktør.aktørIdent))
            aktør.addHendelse(Hendelse(aktør = aktør, aktørIdent = aktør.aktørIdent))
            aktørListe.add(aktør)
        }
        return aktørListe
    }

    companion object {
        @Container
        var database: PostgreSQLContainer = PostgreSQLContainer("postgres").apply {
            withDatabaseName("test_db")
            withUsername("root")
            withPassword("root")
            withInitScript("db-setup.sql")
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(propertyRegistry: DynamicPropertyRegistry) {
            propertyRegistry.add("spring.datasource.url") { database.jdbcUrl }
        }
    }
}

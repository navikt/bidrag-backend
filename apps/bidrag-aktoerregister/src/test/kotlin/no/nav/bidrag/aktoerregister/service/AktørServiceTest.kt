package no.nav.bidrag.aktoerregister.service

import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
import no.nav.bidrag.aktoerregister.AktoerregisterApplicationTest
import no.nav.bidrag.aktoerregister.dto.enumer.Identtype
import no.nav.bidrag.aktoerregister.persistence.entities.Aktør
import no.nav.bidrag.aktoerregister.persistence.repository.AktørRepository
import no.nav.bidrag.aktoerregister.persistence.repository.HendelseRepository
import no.nav.bidrag.generer.testdata.konto.genererKontonummer
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.UUID

@SpringBootTest(classes = [AktoerregisterApplicationTest::class])
@Testcontainers
@EnableMockOAuth2Server
@Transactional
class AktørServiceTest {

    @Autowired
    private lateinit var aktørService: AktørService

    @Autowired
    private lateinit var aktørRepository: AktørRepository

    @Autowired
    private lateinit var hendelseRepository: HendelseRepository

    @Autowired
    private lateinit var entityManager: EntityManager

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

    @BeforeEach
    fun setup() {
        aktørRepository.deleteAll()
    }

    @Test
    fun skalTesteOpprettEllerOppdatertAktoerer() {
        val aktoerer = opprettAktoerListeMed20Aktører()
        for (aktør in aktoerer) {
            aktørService.lagreNyAktør(aktør)
        }
        var savedAktoerer = aktørRepository.findAll()
        var savedHendelser = hendelseRepository.findAll()

        savedAktoerer.size shouldBeExactly 20
        savedHendelser.size shouldBeExactly 20

        // Updating the same aktoerer to test that new hendelser are created
        for (aktør in aktoerer) {
            aktørService.oppdaterAktør(aktør, aktør, null)
        }
        savedAktoerer = aktørRepository.findAll()
        savedHendelser = hendelseRepository.findAll()

        savedAktoerer.size shouldBeExactly 20
        savedHendelser.size shouldBeExactly 40
    }

    @Test
    fun `skal oppdaterer aktør`() {
        val originalIdent = "originalIdent"
        val kontonummer = genererKontonummer().norskKontonummer(true).opprett()
        val kontonummer2 = genererKontonummer().norskKontonummer(true).opprett()
        val aktør = Aktør(
            aktørIdent = originalIdent,
            aktørType = Identtype.PERSONNUMMER.name,
            land = "Norge",
            postnr = "0682",
            poststed = "Oslo",
            adresselinje1 = "Testgate 1",
            norskKontonr = kontonummer.norskKontonummer,
        )
        val nyAktør = Aktør(
            aktørIdent = originalIdent,
            aktørType = Identtype.PERSONNUMMER.name,
            land = "Norge",
            postnr = "0682",
            poststed = "Oslo",
            adresselinje1 = "Testgate 2",
            norskKontonr = kontonummer2.norskKontonummer,
        )

        aktørRepository.save(aktør)

        aktørService.oppdaterAktør(aktør, nyAktør, originalIdent)

        val updatedAktør = aktørRepository.findByAktørIdent(originalIdent)
        updatedAktør?.adresselinje1 shouldBe "Testgate 2"
        updatedAktør?.norskKontonr shouldBe kontonummer2.norskKontonummer
    }

    private fun opprettAktoerListeMed20Aktører(): List<Aktør> {
        val aktørListe: MutableList<Aktør> = ArrayList()
        for (i in 0 until 20) {
            aktørListe.add(
                Aktør(
                    aktørIdent = UUID.randomUUID().toString(),
                    aktørType = Identtype.PERSONNUMMER.name,
                    land = "Norge",
                    postnr = "0682",
                    poststed = "Oslo",
                    adresselinje1 = "Testgate $i",
                    norskKontonr = i.toString(),
                ),
            )
        }
        return aktørListe
    }
}

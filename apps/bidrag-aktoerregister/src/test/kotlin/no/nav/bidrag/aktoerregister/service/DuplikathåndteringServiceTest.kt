package no.nav.bidrag.aktoerregister.service

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import jakarta.persistence.EntityManager
import no.nav.bidrag.aktoerregister.dto.enumer.Identtype
import no.nav.bidrag.aktoerregister.persistence.entities.Aktør
import no.nav.bidrag.aktoerregister.persistence.entities.Hendelse
import no.nav.bidrag.aktoerregister.persistence.entities.TidligereIdenter
import no.nav.bidrag.aktoerregister.persistence.repository.AktørRepository
import no.nav.bidrag.aktoerregister.persistence.repository.HendelseRepository
import no.nav.bidrag.aktoerregister.persistence.repository.TidligereIdenterRepository
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class DuplikathåndteringServiceTest {

    @MockK
    private lateinit var aktørRepository: AktørRepository

    @MockK
    private lateinit var hendelseRepository: HendelseRepository

    @MockK
    private lateinit var tidligereIdenterRepository: TidligereIdenterRepository

    @MockK
    private lateinit var entityManager: EntityManager

    @InjectMockKs
    private lateinit var duplikathåndteringService: DuplikathåndteringService

    @Nested
    inner class FinnAlleMatchendeAktører {

        @Test
        fun `skal returnere tom liste når ingen tidligere identer gis`() {
            val resultat = duplikathåndteringService.finnAlleMatchendeAktører(emptySet())

            resultat.shouldBeEmpty()
            verify(exactly = 0) { aktørRepository.findByAktørIdent(any()) }
        }

        @Test
        fun `skal returnere tom liste når ingen aktører matcher`() {
            every { tidligereIdenterRepository.findByTidligereAktoerIdent(any()) } returns emptyList()
            every { aktørRepository.findByAktørIdent(any()) } returns null

            val resultat = duplikathåndteringService.finnAlleMatchendeAktører(setOf("12345", "67890"))

            resultat.shouldBeEmpty()
            verify(exactly = 2) { aktørRepository.findByAktørIdent(any()) }
        }

        @Test
        fun `skal finne én aktør som matcher`() {
            every { tidligereIdenterRepository.findByTidligereAktoerIdent(any()) } returns emptyList()
            val aktør = opprettAktør(id = 1, ident = "12345")
            every { aktørRepository.findByAktørIdent("12345") } returns aktør
            every { aktørRepository.findByAktørIdent("67890") } returns null

            val resultat = duplikathåndteringService.finnAlleMatchendeAktører(setOf("12345", "67890"))

            resultat shouldHaveSize 1
            resultat[0] shouldBe aktør
        }

        @Test
        fun `skal finne flere aktører og returnere sortert etter id`() {
            every { tidligereIdenterRepository.findByTidligereAktoerIdent(any()) } returns emptyList()
            val aktør1 = opprettAktør(id = 3, ident = "12345")
            val aktør2 = opprettAktør(id = 1, ident = "67890")
            val aktør3 = opprettAktør(id = 2, ident = "11111")

            every { aktørRepository.findByAktørIdent("12345") } returns aktør1
            every { aktørRepository.findByAktørIdent("67890") } returns aktør2
            every { aktørRepository.findByAktørIdent("11111") } returns aktør3

            val resultat = duplikathåndteringService.finnAlleMatchendeAktører(setOf("12345", "67890", "11111"))

            resultat shouldHaveSize 3
            // Skal være sortert etter id (laveste først)
            resultat[0].id shouldBe 1
            resultat[1].id shouldBe 2
            resultat[2].id shouldBe 3
        }

        @Test
        fun `skal fjerne duplikater hvis samme aktør matcher flere identer`() {
            every { tidligereIdenterRepository.findByTidligereAktoerIdent(any()) } returns emptyList()
            val aktør = opprettAktør(id = 1, ident = "12345")
            every { aktørRepository.findByAktørIdent("12345") } returns aktør
            every { aktørRepository.findByAktørIdent("67890") } returns aktør

            val resultat = duplikathåndteringService.finnAlleMatchendeAktører(setOf("12345", "67890"))

            resultat shouldHaveSize 1
            resultat[0] shouldBe aktør
        }
    }

    @Nested
    inner class VelgPrimærAktør {

        @Test
        fun `skal velge aktør med laveste id`() {
            val aktør1 = opprettAktør(id = 5, ident = "12345")
            val aktør2 = opprettAktør(id = 2, ident = "67890")
            val aktør3 = opprettAktør(id = 8, ident = "11111")

            val resultat = duplikathåndteringService.velgPrimærAktør(listOf(aktør1, aktør2, aktør3))

            resultat shouldBe aktør2
            resultat.id shouldBe 2
        }

        @Test
        fun `skal velge første aktør når alle har samme id`() {
            val aktør1 = opprettAktør(id = 1, ident = "12345")
            val aktør2 = opprettAktør(id = 1, ident = "67890")

            val resultat = duplikathåndteringService.velgPrimærAktør(listOf(aktør1, aktør2))

            resultat shouldBe aktør1
        }

        @Test
        fun `skal velge eneste aktør i listen`() {
            val aktør = opprettAktør(id = 42, ident = "12345")

            val resultat = duplikathåndteringService.velgPrimærAktør(listOf(aktør))

            resultat shouldBe aktør
        }
    }

    @Nested
    inner class SlettDuplikater {

        @Test
        fun `skal ikke slette noe når det ikke er duplikater`() {
            val primærAktør = opprettAktør(id = 1, ident = "12345")

            duplikathåndteringService.slettDuplikater(primærAktør, listOf(primærAktør))

            verify(exactly = 0) { hendelseRepository.delete(any()) }
            verify(exactly = 0) { tidligereIdenterRepository.delete(any()) }
            verify(exactly = 0) { aktørRepository.delete(any()) }
            verify(exactly = 0) { entityManager.flush() }
        }

        @Test
        fun `skal slette én duplikat aktør med alle relaterte entiteter`() {
            val primærAktør = opprettAktør(id = 1, ident = "12345")
            val duplikatAktør = opprettAktørMedRelasjoner(
                id = 2,
                ident = "67890",
                antallHendelser = 2,
                antallTidligereIdenter = 1,
            )

            every { hendelseRepository.delete(any()) } just Runs
            every { tidligereIdenterRepository.delete(any()) } just Runs
            every { aktørRepository.delete(any()) } just Runs
            every { entityManager.flush() } just Runs

            duplikathåndteringService.slettDuplikater(primærAktør, listOf(primærAktør, duplikatAktør))

            // Verifiser at hendelser ble slettet
            verify(exactly = 2) { hendelseRepository.delete(any()) }

            // Verifiser at tidligere identer ble slettet
            verify(exactly = 1) { tidligereIdenterRepository.delete(any()) }

            // Verifiser at aktøren ble slettet
            verify(exactly = 1) { aktørRepository.delete(duplikatAktør) }

            // Verifiser at flush ble kalt
            verify(exactly = 1) { entityManager.flush() }
        }

        @Test
        fun `skal slette flere duplikater`() {
            val primærAktør = opprettAktør(id = 1, ident = "12345")
            val duplikat1 = opprettAktørMedRelasjoner(id = 2, ident = "67890", antallHendelser = 1, antallTidligereIdenter = 0)
            val duplikat2 = opprettAktørMedRelasjoner(id = 3, ident = "11111", antallHendelser = 3, antallTidligereIdenter = 2)

            every { hendelseRepository.delete(any()) } just Runs
            every { tidligereIdenterRepository.delete(any()) } just Runs
            every { aktørRepository.delete(any()) } just Runs
            every { entityManager.flush() } just Runs

            duplikathåndteringService.slettDuplikater(primærAktør, listOf(primærAktør, duplikat1, duplikat2))

            // Verifiser at alle hendelser ble slettet (1 + 3 = 4)
            verify(exactly = 4) { hendelseRepository.delete(any()) }

            // Verifiser at alle tidligere identer ble slettet (0 + 2 = 2)
            verify(exactly = 2) { tidligereIdenterRepository.delete(any()) }

            // Verifiser at begge duplikatene ble slettet
            verify(exactly = 1) { aktørRepository.delete(duplikat1) }
            verify(exactly = 1) { aktørRepository.delete(duplikat2) }
            verify(exactly = 0) { aktørRepository.delete(primærAktør) }

            // Verifiser at flush ble kalt én gang
            verify(exactly = 1) { entityManager.flush() }
        }

        @Test
        fun `skal håndtere duplikat uten hendelser eller tidligere identer`() {
            val primærAktør = opprettAktør(id = 1, ident = "12345")
            val duplikatAktør = opprettAktør(id = 2, ident = "67890")

            every { hendelseRepository.delete(any()) } just Runs
            every { tidligereIdenterRepository.delete(any()) } just Runs
            every { aktørRepository.delete(any()) } just Runs
            every { entityManager.flush() } just Runs

            duplikathåndteringService.slettDuplikater(primærAktør, listOf(primærAktør, duplikatAktør))

            // Ingen hendelser eller tidligere identer å slette
            verify(exactly = 0) { hendelseRepository.delete(any()) }
            verify(exactly = 0) { tidligereIdenterRepository.delete(any()) }

            // Men aktøren skal slettes
            verify(exactly = 1) { aktørRepository.delete(duplikatAktør) }
            verify(exactly = 1) { entityManager.flush() }
        }

        @Test
        fun `skal ikke slette primær aktør selv om den er i listen`() {
            val primærAktør = opprettAktørMedRelasjoner(id = 1, ident = "12345", antallHendelser = 5, antallTidligereIdenter = 3)

            every { hendelseRepository.delete(any()) } just Runs
            every { tidligereIdenterRepository.delete(any()) } just Runs
            every { aktørRepository.delete(any()) } just Runs
            every { entityManager.flush() } just Runs

            duplikathåndteringService.slettDuplikater(primærAktør, listOf(primærAktør))

            // Primær aktør skal IKKE slettes
            verify(exactly = 0) { hendelseRepository.delete(any()) }
            verify(exactly = 0) { tidligereIdenterRepository.delete(any()) }
            verify(exactly = 0) { aktørRepository.delete(any()) }
            verify(exactly = 0) { entityManager.flush() }
        }
    }

    @Nested
    inner class IntegrasjonScenarioer {

        @Test
        fun `skal finne og slette duplikater fra flerveis identoppdatering`() {
            val ident1 = genererFødselsnummer()
            val aktør1 = opprettAktør(id = 1, ident = ident1)
            val ident2 = genererFødselsnummer()
            val aktør2 = opprettAktør(id = 2, ident = ident2)

            every { aktørRepository.findByAktørIdent(genererFødselsnummer()) } returns null
            every { aktørRepository.findByAktørIdent(ident1) } returns aktør1
            every { aktørRepository.findByAktørIdent(ident2) } returns aktør2
            every { tidligereIdenterRepository.findByTidligereAktoerIdent(any()) } returns emptyList()
            every { hendelseRepository.delete(any()) } just Runs
            every { tidligereIdenterRepository.delete(any()) } just Runs
            every { aktørRepository.delete(any()) } just Runs
            every { entityManager.flush() } just Runs

            // 1. Finn alle matchende aktører
            val tidligereIdenter = setOf(ident2, ident1)
            val matchendeAktører = duplikathåndteringService.finnAlleMatchendeAktører(tidligereIdenter)

            matchendeAktører shouldHaveSize 2

            // 2. Velg primær aktør (eldste)
            val primærAktør = duplikathåndteringService.velgPrimærAktør(matchendeAktører)

            primærAktør.id shouldBe 1

            // 3. Slett duplikater
            duplikathåndteringService.slettDuplikater(primærAktør, matchendeAktører)

            verify(exactly = 1) { aktørRepository.delete(aktør2) }
            verify(exactly = 0) { aktørRepository.delete(aktør1) }
        }

        @Test
        fun `scenario - ingen duplikater funnet`() {
            every { tidligereIdenterRepository.findByTidligereAktoerIdent(any()) } returns emptyList()
            every { aktørRepository.findByAktørIdent(any()) } returns null

            val matchendeAktører = duplikathåndteringService.finnAlleMatchendeAktører(setOf("ident1", "ident2"))

            matchendeAktører.shouldBeEmpty()
        }

        @Test
        fun `scenario - alle tidligere identer peker på samme aktør`() {
            val aktør = opprettAktør(id = 1, ident = "current")
            every { tidligereIdenterRepository.findByTidligereAktoerIdent(any()) } returns emptyList()
            every { aktørRepository.findByAktørIdent("old1") } returns aktør
            every { aktørRepository.findByAktørIdent("old2") } returns aktør
            every { aktørRepository.findByAktørIdent("old3") } returns aktør

            val matchendeAktører = duplikathåndteringService.finnAlleMatchendeAktører(setOf("old1", "old2", "old3"))

            // Skal kun returnere én aktør selv om den matcher flere identer
            matchendeAktører shouldHaveSize 1
            matchendeAktører[0] shouldBe aktør
        }
    }

    // Helper functions
    private fun opprettAktør(id: Int, ident: String): Aktør = Aktør(
        aktørIdent = ident,
        aktørType = Identtype.PERSONNUMMER.name,
    ).apply {
        this.id = id
    }

    private fun opprettAktørMedRelasjoner(
        id: Int,
        ident: String,
        antallHendelser: Int,
        antallTidligereIdenter: Int,
    ): Aktør {
        val aktør = opprettAktør(id, ident)

        // Legg til hendelser
        repeat(antallHendelser) {
            aktør.hendelser.add(Hendelse(aktørIdent = ident, aktør = aktør))
        }

        // Legg til tidligere identer
        repeat(antallTidligereIdenter) { index ->
            aktør.tidligereIdenter.add(
                TidligereIdenter(
                    tidligereAktoerIdent = "tidligere_$index",
                    identtype = Identtype.PERSONNUMMER.name,
                    aktør = aktør,
                ),
            )
        }

        return aktør
    }
}

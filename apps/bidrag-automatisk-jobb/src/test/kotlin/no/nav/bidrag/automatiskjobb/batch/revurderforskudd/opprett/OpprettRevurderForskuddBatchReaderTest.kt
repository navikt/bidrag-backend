package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.opprett

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable
import java.time.LocalDate

class OpprettRevurderForskuddBatchReaderTest {
    private val forskuddFremTilDato: LocalDate = LocalDate.now()

    private fun barn(
        id: Int,
        saksnummer: String,
    ): Barn = mockk<Barn>().apply {
        every { this@apply.id } returns id
        every { this@apply.saksnummer } returns saksnummer
    }

    private fun opprettRepositoryMock(database: MutableList<Barn>): BarnRepository {
        val barnRepository = mockk<BarnRepository>()
        every {
            barnRepository.finnBarnSomSkalRevurdereForskuddEtter(any(), any(), any(), any())
        } answers {
            val sisteSaksnummer = secondArg<String>()
            val sisteId = thirdArg<Int>()
            val pageable = arg<Pageable>(3)
            database
                .filter { it.saksnummer > sisteSaksnummer || (it.saksnummer == sisteSaksnummer && it.id!! > sisteId) }
                .sortedWith(compareBy({ it.saksnummer }, { it.id }))
                .take(pageable.pageSize)
        }
        return barnRepository
    }

    @Test
    fun `skal gruppere barn med samme saksnummer på tvers av sider`() {
        val database =
            mutableListOf(
                barn(id = 1, saksnummer = "Sak1"),
                barn(id = 2, saksnummer = "Sak1"),
                barn(id = 3, saksnummer = "Sak2"),
            )
        val reader = OpprettRevurderForskuddBatchReader(opprettRepositoryMock(database), forskuddFremTilDato, pageSize = 2)

        val gruppe1 = reader.read()
        val gruppe2 = reader.read()
        val gruppe3 = reader.read()

        gruppe1?.map { it.id } shouldBe listOf(1, 2)
        gruppe2?.map { it.id } shouldBe listOf(3)
        gruppe3 shouldBe null
    }

    @Test
    fun `skal ikke returnere allerede leste barn på nytt selv om et barn med tidligere sorteringsnøkkel dukker opp mens jobben kjører`() {
        val database =
            mutableListOf(
                barn(id = 1, saksnummer = "Sak1"),
                barn(id = 2, saksnummer = "Sak1"),
                barn(id = 3, saksnummer = "Sak2"),
            )
        val reader = OpprettRevurderForskuddBatchReader(opprettRepositoryMock(database), forskuddFremTilDato, pageSize = 2)

        val gruppe1 = reader.read()
        gruppe1?.map { it.id } shouldBe listOf(1, 2)

        // Et "nytt" barn dukker opp med en sorteringsnøkkel før cursoren er allerede passert.
        database.add(barn(id = 0, saksnummer = "Sak0"))

        val gruppe2 = reader.read()
        val gruppe3 = reader.read()

        // S0/id=0 dukker aldri opp igjen i denne kjøringen, og ingen barn blir lest to ganger.
        gruppe2?.map { it.id } shouldBe listOf(3)
        gruppe3 shouldBe null
    }
}

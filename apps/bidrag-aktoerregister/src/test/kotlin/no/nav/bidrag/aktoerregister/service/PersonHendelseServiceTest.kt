package no.nav.bidrag.aktoerregister.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.aktoerregister.config.RestConfig
import no.nav.bidrag.aktoerregister.dto.enumer.Identtype
import no.nav.bidrag.aktoerregister.persistence.entities.Aktør
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.generer.testdata.person.Kjønn
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class PersonHendelseServiceTest {

    @MockK(relaxed = true)
    private lateinit var aktørService: AktørService

    @MockkBean
    private var objectMapper: ObjectMapper = RestConfig().objectMapper()

    @InjectMockKs
    private lateinit var personHendelseService: PersonHendelseService

    companion object {
        val personIdent1 = genererFødselsnummer(LocalDate.now().minusYears(30), Kjønn.MANN)
        val personIdent2 = genererFødselsnummer(LocalDate.now().minusYears(30), Kjønn.KVINNE)
        val aktør = Aktør(id = 0, aktørIdent = personIdent2, aktørType = Identtype.PERSONNUMMER.name)
        val aktørMedNyId = Aktør(id = 1, aktørIdent = personIdent1, aktørType = Identtype.PERSONNUMMER.name)
        val hendelse = "{" +
            "\"aktørid\":\"123456\"," +
            "\"personidenter\":[" +
            "\"$personIdent1\"," +
            "\"$personIdent2\"" +
            "]}"
    }

    @Test
    fun `skal behandle hendelse med ny ident`() {
        every { aktørService.hentAktørFraDatabase(Ident(personIdent1)) } returns Pair(null, false)
        every { aktørService.hentAktørFraDatabase(Ident(personIdent2)) } returns Pair(aktør, false)
        every { aktørService.hentAktørFraPerson(Ident(personIdent1)) } returns aktørMedNyId
        every { aktørService.hentAktørFraPerson(Ident(personIdent2)) } returns aktørMedNyId

        personHendelseService.behandleHendelse(hendelse)

        verify(exactly = 1) { aktørService.oppdaterAktør(aktør, aktørMedNyId, aktør.aktørIdent) }
    }

    @Test
    fun `skal behandle hendelse for ident som ikke finne si aktørregisteret`() {
        every { aktørService.hentAktørFraDatabase(Ident(personIdent1)) } returns Pair(null, false)
        every { aktørService.hentAktørFraDatabase(Ident(personIdent2)) } returns Pair(null, false)

        personHendelseService.behandleHendelse(hendelse)

        verify(exactly = 0) { aktørService.lagreNyAktør(aktør) }
    }
}

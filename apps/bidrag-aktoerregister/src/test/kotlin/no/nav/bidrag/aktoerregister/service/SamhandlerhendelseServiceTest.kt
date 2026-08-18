package no.nav.bidrag.aktoerregister.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.aktoerregister.dto.enumer.Identtype
import no.nav.bidrag.aktoerregister.persistence.entities.Aktør
import no.nav.bidrag.aktoerregister.persistence.repository.AktørRepository
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.generer.testdata.samhandler.genererSamhandler
import no.nav.bidrag.generer.testdata.samhandler.genererSamhandlerId
import no.nav.bidrag.transport.samhandler.SamhandlerKafkaHendelsestype
import no.nav.bidrag.transport.samhandler.Samhandlerhendelse
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test

@ExtendWith(MockKExtension::class)
class SamhandlerhendelseServiceTest {

    @RelaxedMockK
    lateinit var aktørService: AktørService

    @RelaxedMockK
    lateinit var aktørRepository: AktørRepository

    @InjectMockKs
    lateinit var samhandlerhendelseService: SamhandlerhendelseService

    @Test
    fun testOpprettNySamhandler() {
        val samhandlerhendelse = Samhandlerhendelse(
            samhandlerId = genererSamhandlerId().verdi,
            hendelsestype = SamhandlerKafkaHendelsestype.OPPRETTET,
            sporingId = "sporingId",
        )
        samhandlerhendelseService.behandleHendelse(samhandlerhendelse)

        verify(exactly = 0) { aktørRepository.findByAktørIdent(any()) }
        verify(exactly = 0) { aktørService.hentAktørFraSamhandler(any()) }
    }

    @Test
    fun testOppdaterSamhandler() {
        val samhandler = genererSamhandlerId().verdi
        every { aktørRepository.findByAktørIdent(any()) } returns Aktør(aktørIdent = samhandler, aktørType = Identtype.AKTOERNUMMER.name)
        every { aktørService.hentAktørFraSamhandler(any()) } returns Aktør(aktørIdent = samhandler, aktørType = Identtype.AKTOERNUMMER.name)
        val samhandlerhendelse = Samhandlerhendelse(
            samhandlerId = samhandler,
            hendelsestype = SamhandlerKafkaHendelsestype.OPPDATERT,
            sporingId = "sporingId",
        )
        samhandlerhendelseService.behandleHendelse(samhandlerhendelse)

        verify(exactly = 1) { aktørRepository.findByAktørIdent(any()) }
        verify(exactly = 1) { aktørService.oppdaterAktør(any(), any(), any()) }
        verify(exactly = 1) { aktørService.hentAktørFraSamhandler(any()) }
    }

    @Test
    fun testOpphørSamhandler() {
        val samhandlerhendelse = Samhandlerhendelse(
            samhandlerId = genererSamhandlerId().verdi,
            hendelsestype = SamhandlerKafkaHendelsestype.OPPHØRT,
            sporingId = "sporingId",
        )
        samhandlerhendelseService.behandleHendelse(samhandlerhendelse)

        verify(exactly = 0) { aktørRepository.findByAktørIdent(any()) }
        verify(exactly = 0) { aktørService.hentAktørFraSamhandler(any()) }
    }
}

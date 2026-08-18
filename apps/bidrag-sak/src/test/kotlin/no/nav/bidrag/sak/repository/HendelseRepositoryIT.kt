package no.nav.bidrag.sak.repository

import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.behandling.HendelseType
import no.nav.bidrag.sak.SpringTestRunner
import no.nav.bidrag.sak.domain.Hendelse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.temporal.ChronoUnit

class HendelseRepositoryIT : SpringTestRunner() {
    @Autowired
    private lateinit var hendelseRepository: HendelseRepository

    @Test
    fun `insert klarer å lagre en forekomst av Hendelse`() {
        val hendelse =
            Hendelse(
                saksnummer = "321321",
                type = HendelseType.BRUKERSTØTTE,
                enhet = "1701",
                resultat = "Ferdig",
                grKombKode = "852047",
                fraBbm = true,
                søknad = null,
            )

        val lagretHendelse = hendelseRepository.save(hendelse)

        val hentetHendelse = hendelseRepository.findByIdOrThrow(lagretHendelse.hendelseId!!)

        hentetHendelse.hendelseId shouldBe lagretHendelse.hendelseId
        hentetHendelse.saksnummer shouldBe hendelse.saksnummer
        hentetHendelse.type shouldBe hendelse.type
        hentetHendelse.opprettetTidspunkt shouldBe
            hendelse.opprettetTidspunkt.truncatedTo(ChronoUnit.MICROS)
        hentetHendelse.enhet shouldBe hendelse.enhet
        hentetHendelse.resultat shouldBe hendelse.resultat
        hentetHendelse.opprettetAv shouldBe hendelse.opprettetAv
        hentetHendelse.grKombKode shouldBe hendelse.grKombKode
        hentetHendelse.fraBbm shouldBe hendelse.fraBbm
    }
}

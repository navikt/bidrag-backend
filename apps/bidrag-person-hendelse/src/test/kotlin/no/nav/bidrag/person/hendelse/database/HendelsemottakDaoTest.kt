package no.nav.bidrag.person.hendelse.database

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import no.nav.bidrag.person.hendelse.Teststarter
import no.nav.bidrag.person.hendelse.domene.Endringstype
import no.nav.bidrag.person.hendelse.domene.Livshendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@SpringBootTest(
    classes = [Teststarter::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HendelsemottakDaoTest {
    @Autowired
    lateinit var aktorDao: AktorDao

    @Autowired
    lateinit var hendelsemottakDao: HendelsemottakDao

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    @Transactional
    fun skalLagreHendelse() {
        // gitt
        val personidenter = listOf("12345678910", "1234567891013")
        val aktørid = personidenter.first { it.length == 13 }
        val aktør = aktorDao.save(Aktor(aktørid))

        val hendelseid = "123"
        val opplysningstype = Livshendelse.Opplysningstype.SIVILSTAND_V1
        val endringstype = Endringstype.OPPRETTET

        val hendelsemottak = Hendelsemottak(hendelseid, opplysningstype, endringstype, personidenter.toString(), aktør)

        // hvis
        hendelsemottakDao.save(hendelsemottak)

        // så
        val eksisterer = hendelsemottakDao.existsByHendelseidAndOpplysningstype(hendelseid, opplysningstype)
        assertThat(eksisterer).isTrue
    }

    @Test
    @Transactional
    fun skalBegrenseAntallAktørerMedPubliseringsklareHendelser() {
        // gitt
        hendelsemottakDao.deleteAll()
        aktorDao.deleteAll()

        val aktørider = (1..3).map { "123456789101$it" }
        aktørider.forEachIndexed { indeks, aktørid ->
            val aktør = aktorDao.save(Aktor(aktørid))
            hendelsemottakDao.save(
                Hendelsemottak(
                    hendelseid = "hendelse-$indeks",
                    opplysningstype = Livshendelse.Opplysningstype.SIVILSTAND_V1,
                    endringstype = Endringstype.OPPRETTET,
                    personidenter = aktørid,
                    aktor = aktør,
                    status = Status.OVERFØRT,
                ),
            )
        }
        entityManager.flush()

        // hvis
        val begrensetUttrekk =
            hendelsemottakDao.henteIdTilAktørerMedPubliseringsklareHendelser(
                LocalDateTime.now(),
                PageRequest.of(0, 2),
            )

        // så
        assertThat(begrensetUttrekk).hasSize(2)

        val hendelser = hendelsemottakDao.hentePubliseringsklareOverførteHendelserForAktører(begrensetUttrekk)
        assertThat(hendelser).hasSize(2)
        assertThat(hendelser.map { it.aktor.id }).containsExactlyInAnyOrderElementsOf(begrensetUttrekk)
    }
}

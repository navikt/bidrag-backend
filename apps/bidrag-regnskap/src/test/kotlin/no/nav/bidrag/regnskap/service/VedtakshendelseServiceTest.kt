package no.nav.bidrag.regnskap.service

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.generer.testdata.sak.genererSaksnummer
import no.nav.bidrag.regnskap.UnleashFeatures
import no.nav.bidrag.regnskap.util.PåløpException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class VedtakshendelseServiceTest {

    @MockK(relaxed = true)
    private lateinit var oppdragService: OppdragService

    @MockK(relaxed = true)
    private lateinit var kravService: KravService

    @MockK(relaxed = true)
    private lateinit var persistenceService: PersistenceService

    @MockK(relaxed = true)
    private lateinit var oppdragsperiodeService: OppdragsperiodeService

    @MockK(relaxed = true)
    private lateinit var identUtils: IdentUtils

    @MockK(relaxed = true)
    private lateinit var driftsavvikService: DriftsavvikService

    @InjectMockKs
    private lateinit var vedtakshendelseService: VedtakshendelseService

    @BeforeEach
    fun setup() {
        mockkObject(UnleashFeaturesProvider)
        every { UnleashFeaturesProvider.isEnabled(UnleashFeatures.ENDRE_MOTTAKER.featureName, false, false) } returns true
        every { persistenceService.harAktivtDriftsavvik(false) } returns false
        every { kravService.erVedlikeholdsmodusPåslått() } returns false
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(UnleashFeaturesProvider)
    }

    @Test
    fun `skal mappe vedtakshendelse uten feil`() {
        val hendelse = opprettVedtakshendelse()

        val vedtakHendelse = vedtakshendelseService.mapVedtakHendelse(hendelse)

        vedtakHendelse shouldNotBe null
        vedtakHendelse.id shouldBe 123
        vedtakHendelse.engangsbeløpListe?.shouldHaveSize(1)
        vedtakHendelse.stønadsendringListe?.shouldHaveSize(1)
    }

    @Test
    fun `skal opprette oppdrag for stonadsendringer og engangsbeløp`() {
        val hendelse = opprettVedtakshendelse()

        every { oppdragService.lagreHendelse(any()) } returns 1

        vedtakshendelseService.behandleHendelse(hendelse)

        verify(exactly = 1) { oppdragService.lagreHendelse(any(), false) }
        verify(exactly = 1) { oppdragService.lagreHendelse(any(), true) }
    }

    @Test
    fun `Skal lese vedtakshendelse uten feil`() {
        assertDoesNotThrow {
            vedtakshendelseService.mapVedtakHendelse(
                """
        {
          "kilde":"MANUELT",
          "type":"INDEKSREGULERING",
          "id":"779",
          "vedtakstidspunkt":"2022-06-03T00:00:00.000000000",
          "enhetsnummer":"4812",
          "opprettetAv":"B101173",
          "kildeapplikasjon": "TEST",
          "opprettetTidspunkt":"2022-10-19T16:00:23.254988482",
          "stønadsendringListe":[
          ],
          "engangsbeløpListe":[
          ],
          "sporingsdata": {
            "correlationId": "12345"
          }
        }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `Skal lese vedtakshendelse med feil`() {
        assertThrows<InvalidFormatException> {
            vedtakshendelseService.mapVedtakHendelse(
                """
        {
          "type":"ÅRSAVGIFT",
          "vedtakTidspunkt":"2022-01-01T00:00:00.000000000",
          "id":"123",
          "enhetId":"enhetid",
          "stonadType":"BIDRAG",
          "sakId":"",
          "skyldnerId":"",
          "kravhaverId":"",
          "mottakerId":"",
          "opprettetAv":"",
          "opprettetTidspunkt":"2022-01-11T10:00:00.000001",
          "periodeListe":[],
          "sporingsdata: {
            "correlationId": "12345"
          }
        }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `Skal ikke lese hendelse om det finnes aktivt driftsavvik`() {
        every { driftsavvikService.harAktivtDriftsavvik(true) } returns true
        assertThrows<PåløpException> { vedtakshendelseService.behandleHendelse(opprettVedtakshendelse()) }
    }

    @Test
    fun `Skal ikke behandle hendelse om den allerede er behandlet`() {
        val hendelse = opprettVedtakshendelse()

        every { oppdragsperiodeService.hentAlleOppdragsperiodeMedVedtaksId(123) } returns listOf(mockk())

        val resultat = vedtakshendelseService.behandleHendelse(hendelse)

        resultat shouldHaveSize 0
        verify(exactly = 0) { oppdragService.lagreHendelse(any()) }
        verify(exactly = 0) { oppdragService.lagreHendelse(any(), any()) }
    }

    @Test
    fun `Skal ikke behandle endring av mottaker uten innkreving`() {
        val hendelse = opprettMottakerendringsHendelse("UTEN_INNKREVING")

        vedtakshendelseService.behandleHendelse(hendelse)

        verify(exactly = 0) { oppdragService.lagreHendelse(any(), any()) }
    }

    @Test
    fun `skal behandle mottakerendring uten perioder som annen stonadsendring`() {
        val hendelse = opprettMottakerendringsHendelse()
        every { oppdragService.lagreHendelse(any(), false) } returns null

        val oppdrag = vedtakshendelseService.behandleHendelse(hendelse)

        verify(exactly = 1) {
            oppdragService.lagreHendelse(match { it.vedtakId == 648462 && it.periodeListe.isEmpty() }, false)
        }
        oppdrag shouldBe emptyList()
    }

    @Test
    fun `skal behandle mottakerendring uten perioder ved ny levering`() {
        val hendelse = opprettMottakerendringsHendelse()

        vedtakshendelseService.behandleHendelse(hendelse)
        vedtakshendelseService.behandleHendelse(hendelse)

        verify(exactly = 2) { oppdragService.lagreHendelse(match { it.vedtakId == 648462 && it.periodeListe.isEmpty() }, false) }
    }

    @Test
    fun `skal behandle periodefri mottakerendring selv om Elin er deaktivert`() {
        every { UnleashFeaturesProvider.isEnabled(UnleashFeatures.ENDRE_MOTTAKER.featureName, false, false) } returns false

        vedtakshendelseService.behandleHendelse(opprettMottakerendringsHendelse())

        verify(exactly = 1) { oppdragService.lagreHendelse(match { it.periodeListe.isEmpty() }, false) }
    }

    @Test
    fun `skal behandle mottakerendring med perioder i vanlig oppdragsflyt`() {
        val hendelse = opprettVedtakshendelse(vedtakstype = "ENDRING_MOTTAKER")
        every { oppdragService.lagreHendelse(any(), any()) } returns 1

        val oppdrag = vedtakshendelseService.behandleHendelse(hendelse)

        oppdrag shouldBe listOf(1, 1)
        verify(exactly = 1) { oppdragService.lagreHendelse(match { it.periodeListe.isNotEmpty() }, false) }
    }

    private fun opprettMottakerendringsHendelse(
        innkrevingstype: String = "MED_INNKREVING",
    ): String = requireNotNull(javaClass.getResource("/testfiler/hendelse/endreRmOppdatering.json")).readText()
        .replace("\"BP\"", "\"${genererFødselsnummer()}\"")
        .replace("\"BARN1\"", "\"${genererFødselsnummer()}\"")
        .replace("\"BM\"", "\"${genererFødselsnummer()}\"")
        .replace("\"MED_INNKREVING\"", "\"$innkrevingstype\"")

    private fun opprettVedtakshendelse(
        vedtakstype: String = "INNKREVING",
        innkrevingstype: String = "MED_INNKREVING",
    ): String = """
      {
        "kilde":"MANUELT",
        "type":"$vedtakstype",
        "id":"123",
        "vedtakstidspunkt":"2022-06-01T00:00:00.000000000",
        "enhetsnummer":"4812",
        "opprettetAv":"B111111",
        "kildeapplikasjon":"TEST",
        "opprettetTidspunkt":"2022-01-01T16:00:00.000000000",
        "stønadsendringListe":[
          {
            "type":"BIDRAG",
            "sak":"${genererSaksnummer()}",
            "skyldner":"${genererFødselsnummer()}",
            "kravhaver":"${genererFødselsnummer()}",
            "mottaker":"${genererFødselsnummer()}",
            "innkreving":"$innkrevingstype",
            "beslutning":"ENDRING",
            "periodeListe":[
              {
                "periode": {
                    "fom":"2022-01",
                    "til":"2022-03"
                },
                "beløp":"2910",
                "valutakode":"NOK",
                "resultatkode":"KBB"
              },
              {
                "periode": {
                    "fom":"2022-03",
                    "til":null
                },
                "beløp":"2930",
                "valutakode":"NOK",
                "resultatkode":"KBB"
              }
            ]
          }
        ]
        ,
        "engangsbeløpListe":[
          {
            "type":"GEBYR_SKYLDNER",
            "sak":"${genererSaksnummer()}",
            "skyldner":"${genererFødselsnummer()}",
            "kravhaver":"${genererFødselsnummer()}",
            "mottaker":"${genererFødselsnummer()}",
            "belop":"1790",
            "valutakode":"NOK",
            "resultatkode":"GIGI",
            "innkreving":"$innkrevingstype",
            "referanse":"REFERANSE",
            "beslutning":"ENDRING"
          }
        ],
         "sporingsdata": {
            "correlationId": "12345"
          }
      }"""
}

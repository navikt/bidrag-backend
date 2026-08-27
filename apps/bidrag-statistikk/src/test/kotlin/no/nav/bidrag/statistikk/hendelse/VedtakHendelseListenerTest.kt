package no.nav.bidrag.statistikk.hendelse

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.enums.særbidrag.Særbidragskategori
import no.nav.bidrag.statistikk.BidragStatistikkTest
import no.nav.bidrag.statistikk.BidragStatistikkTest.Companion.TEST_PROFILE
import no.nav.bidrag.statistikk.TestUtil.Companion.byggVedtakDtoAldersjusteringBidrag
import no.nav.bidrag.statistikk.TestUtil.Companion.byggVedtakDtoBidrag
import no.nav.bidrag.statistikk.TestUtil.Companion.byggVedtakDtoBidragUtenGrunnlag
import no.nav.bidrag.statistikk.TestUtil.Companion.byggVedtakDtoForskudd
import no.nav.bidrag.statistikk.TestUtil.Companion.byggVedtakDtoUtenForskuddOgBidrag
import no.nav.bidrag.statistikk.TestUtil.Companion.byggVedtakDtoUtenGrunnlag
import no.nav.bidrag.statistikk.TestUtil.Companion.lesVedtakDtoFraFil
import no.nav.bidrag.statistikk.TestUtil.Companion.stubHenteVedtak
import no.nav.bidrag.transport.behandling.statistikk.BidragHendelse
import no.nav.bidrag.transport.behandling.statistikk.SærbidragHendelse
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture

@SpringBootTest(classes = [BidragStatistikkTest::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("VedtakHendelseListener (test av forretningslogikk)")
@ActiveProfiles(TEST_PROFILE)
@EnableMockOAuth2Server
@EnableAspectJAutoProxy
@EnableWireMock(
    ConfigureWireMock(name = "my-service", port = 0),
)
class VedtakHendelseListenerTest {
    @Autowired
    private lateinit var vedtakHendelseListener: VedtakHendelseListener

    @MockkBean(relaxed = true)
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @MockitoBean
    private lateinit var statistikkKafkaEventProducerMock: StatistikkKafkaEventProducer

    @BeforeEach
    fun init() {
        every { kafkaTemplate.send(any<ProducerRecord<String, String>>()) } returns CompletableFuture.completedFuture(io.mockk.mockk(relaxed = true))

        every { kafkaTemplate.afterSingletonsInstantiated() } returns Unit
    }

    @Disabled
    @Test
    fun `skal lese vedtakshendelse Forskudd uten feil`() {
        stubHenteVedtak(byggVedtakDtoForskudd())
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"MANUELT",
              "type":"ALDERSJUSTERING",
              "id":"999999999",
              "opprettetAv":"X123456",
              "kildeapplikasjon":"Bisys",              
              "vedtakstidspunkt":"2022-01-11T10:00:00.000001",              
              "enhetsnummer":"Enhet1",
              "opprettetTidspunkt":"2022-01-11T10:00:00.000001",    
              "stønadsendringListe": [
                {
                 "type": "FORSKUDD",
                 "sak": "",
                 "skyldner": "",
                 "kravhaver": "",
                 "mottaker": "",
                 "innkreving": "MED_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
                },
                {
                 "type": "FORSKUDD",
                 "sak": "",
                 "skyldner": "",
                 "kravhaver": "",
                 "mottaker": "",
                 "innkreving": "MED_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
              }                       
              ],
              "sporingsdata":
                {
                "correlationId":""            
                }
            }
            """.trimIndent(),
        )
        verify(statistikkKafkaEventProducerMock, times(2)).publishForskudd(anyOrNull())
    }

    @Disabled
    @Test
    fun `skal lese vedtakshendelse Forskudd uten grunnlag og sjekke at det fortsatt produseres hendelse`() {
        stubHenteVedtak(byggVedtakDtoUtenGrunnlag())
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"MANUELT",
              "type":"ALDERSJUSTERING",
              "id":"999999999",
              "opprettetAv":"X123456",
              "kildeapplikasjon":"Bisys",              
              "vedtakstidspunkt":"2022-01-11T10:00:00.000001",              
              "enhetsnummer":"Enhet1",
              "opprettetTidspunkt":"2022-01-11T10:00:00.000001",    
              "stønadsendringListe": [
                {
                 "type": "FORSKUDD",
                 "sak": "",
                 "skyldner": "",
                 "kravhaver": "",
                 "mottaker": "",
                 "innkreving": "MED_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
                },
                {
                 "type": "BIDRAG",
                 "sak": "",
                 "skyldner": "",
                 "kravhaver": "",
                 "mottaker": "",
                 "innkreving": "MED_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
                }                         
              ],
              "sporingsdata":
                {
                "correlationId":""            
                }
            }
            """.trimIndent(),
        )
        verify(statistikkKafkaEventProducerMock, times(1)).publishForskudd(anyOrNull())
    }

    @Disabled
    @Test
    fun `skal ikke behandle hendelse Forskudd hvis vedtak ikke inneholder forskudd eller bidrag`() {
        stubHenteVedtak(byggVedtakDtoUtenForskuddOgBidrag())
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"MANUELT",
              "type":"ALDERSJUSTERING",
              "id":"999999999",
              "opprettetAv":"X123456",
              "kildeapplikasjon":"Bisys",              
              "vedtakstidspunkt":"2022-01-11T10:00:00.000001",              
              "enhetsnummer":"Enhet1",
              "opprettetTidspunkt":"2022-01-11T10:00:00.000001",    
              "stønadsendringListe": [],
              "sporingsdata":
                {
                "correlationId":""            
                }
            }
            """.trimIndent(),
        )
        verifyNoInteractions(statistikkKafkaEventProducerMock)
    }

    @Test
    fun `skal lese vedtakshendelse Bidrag uten feil`() {
        val captor = argumentCaptor<BidragHendelse>()
        stubHenteVedtak(byggVedtakDtoBidrag("bidrag-behandling-q2"))
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"MANUELT",
              "type":"ENDRING",
              "id":"999999999",
              "opprettetAv":"ABCDEFG",
              "kildeapplikasjon":"bidrag-behandling",              
              "vedtakstidspunkt":"2020-01-01T23:34:55.869121094",              
              "enhetsnummer":"ABCD",
              "opprettetTidspunkt":"2020-01-01T23:34:55.869121094",    
              "stønadsendringListe": [
                {
                 "type": "BIDRAG",
                 "sak": "1234567",
                 "skyldner": "98765432109",
                 "kravhaver": "12345678901",
                 "mottaker": "16498311338",
                 "innkreving": "UTEN_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
                }
              ],
              "sporingsdata":
                {
                "correlationId":""            
                }
            }
            """.trimIndent(),
        )
        verify(statistikkKafkaEventProducerMock, times(1)).publishBidrag(captor.capture())

        val hendelser = captor.allValues
        assertThat(hendelser[0].vedtaksid).isEqualTo(999999999)
        assertThat(hendelser[0].vedtakstidspunkt).isEqualTo("2020-01-01T23:34:55.869121094")
        assertThat(hendelser[0].type).isEqualTo("ENDRING")
        assertThat(hendelser[0].saksnr).isEqualTo("1234567")
        assertThat(hendelser[0].skyldner).isEqualTo("98765432109")
        assertThat(hendelser[0].kravhaver).isEqualTo("12345678901")
        assertThat(hendelser[0].mottaker).isEqualTo("16498311338")
        assertThat(hendelser[0].historiskVedtak).isFalse
        assertThat(hendelser[0].bidragPeriodeListe.size == 2)

        assertThat(hendelser[0].bidragPeriodeListe[0].periodeFra).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(hendelser[0].bidragPeriodeListe[0].periodeTil).isEqualTo(LocalDate.of(2025, 3, 1))
        assertThat(hendelser[0].bidragPeriodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(100))
        assertThat(hendelser[0].bidragPeriodeListe[0].bidragsevne).isEqualTo(BigDecimal.valueOf(3500))
        assertThat(hendelser[0].bidragPeriodeListe[0].underholdskostnad).isEqualTo(BigDecimal.valueOf(500))
        assertThat(hendelser[0].bidragPeriodeListe[0].skyldnersAndelUnderholdskostnad).isEqualTo(BigDecimal.valueOf(400))
        assertThat(hendelser[0].bidragPeriodeListe[0].samværsfradrag).isEqualTo(BigDecimal.valueOf(150))
        assertThat(hendelser[0].bidragPeriodeListe[0].nettoBarnetilleggSkyldner).isEqualTo(BigDecimal.valueOf(500))
        assertThat(hendelser[0].bidragPeriodeListe[0].nettoBarnetilleggMottaker).isNull()
        assertThat(hendelser[0].bidragPeriodeListe[0].skyldnerBorMedAndreVoksne).isTrue
        assertThat(hendelser[0].bidragPeriodeListe[0].samværsklasse).isEqualTo(Samværsklasse.DELT_BOSTED)
        assertThat(hendelser[0].bidragPeriodeListe[0].skyldnerInntektListe?.first()?.beløp).isEqualTo(BigDecimal.valueOf(1000))
        assertThat(hendelser[0].bidragPeriodeListe[0].skyldnerInntektListe?.size == 1)
        assertThat(hendelser[0].bidragPeriodeListe[0].mottakerInntektListe?.size == 1)

        assertThat(hendelser[0].bidragPeriodeListe[1].periodeFra).isEqualTo(LocalDate.of(2025, 3, 1))
        assertThat(hendelser[0].bidragPeriodeListe[1].periodeTil).isNull()
        assertThat(hendelser[0].bidragPeriodeListe[1].beløp).isEqualTo(BigDecimal.valueOf(200))
        assertThat(hendelser[0].bidragPeriodeListe[1].bidragsevne).isEqualTo(BigDecimal.valueOf(3500))
        assertThat(hendelser[0].bidragPeriodeListe[1].underholdskostnad).isEqualTo(BigDecimal.valueOf(500))
        assertThat(hendelser[0].bidragPeriodeListe[1].skyldnersAndelUnderholdskostnad).isEqualTo(BigDecimal.valueOf(200))
        assertThat(hendelser[0].bidragPeriodeListe[1].samværsfradrag).isEqualTo(BigDecimal.valueOf(150))
        assertThat(hendelser[0].bidragPeriodeListe[1].nettoBarnetilleggSkyldner).isEqualTo(BigDecimal.valueOf(1200))
        assertThat(hendelser[0].bidragPeriodeListe[1].nettoBarnetilleggMottaker).isNull()
        assertThat(hendelser[0].bidragPeriodeListe[1].skyldnerBorMedAndreVoksne).isTrue
        assertThat(hendelser[0].bidragPeriodeListe[1].samværsklasse).isEqualTo(Samværsklasse.DELT_BOSTED)

        assertThat(hendelser[0].bidragPeriodeListe[1].skyldnerInntektListe?.first()?.beløp).isEqualTo(BigDecimal.valueOf(1700))
        assertThat(hendelser[0].bidragPeriodeListe[1].mottakerInntektListe?.first()?.beløp).isEqualTo(BigDecimal.valueOf(2500))
        assertThat(hendelser[0].bidragPeriodeListe[1].skyldnerInntektListe?.size == 1)
        assertThat(hendelser[0].bidragPeriodeListe[1].mottakerInntektListe?.size == 1)
    }

    @Test
    fun `skal behandle hendelse Bidrag uten grunnlag`() {
        val captor = argumentCaptor<BidragHendelse>()
        stubHenteVedtak(byggVedtakDtoBidragUtenGrunnlag())
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"MANUELT",
              "type":"ENDRING",
              "id":"999999999",
              "opprettetAv":"ABCDEFG",
              "kildeapplikasjon":"bidrag-behandling",              
              "vedtakstidspunkt":"2020-01-01T23:34:55.869121094",              
              "enhetsnummer":"ABCD",
              "opprettetTidspunkt":"2020-01-01T23:34:55.869121094",    
              "stønadsendringListe": [
                {
                 "type": "BIDRAG",
                 "sak": "1234567",
                 "skyldner": "98765432109",
                 "kravhaver": "12345678901",
                 "mottaker": "16498311338",
                 "innkreving": "MED_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
                }
              ],
              "sporingsdata":
                {
                "correlationId":""            
                }
            }
            """.trimIndent(),
        )
        verify(statistikkKafkaEventProducerMock, times(1)).publishBidrag(captor.capture())
        val hendelser = captor.allValues
    }

    @Test
    fun `skal lese vedtakshendelse for aldersjustering fra bidrag-automatisk-jobb uten feil`() {
        val captor = argumentCaptor<BidragHendelse>()
        stubHenteVedtak(byggVedtakDtoAldersjusteringBidrag())
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"AUTOMATISK",
              "type":"ALDERSJUSTERING",
              "id":"999999999",
              "opprettetAv":"ABCDEFG",
              "kildeapplikasjon":"bidrag-automatisk-jobb",              
              "vedtakstidspunkt":"2020-01-01T23:34:55.869121094",              
              "enhetsnummer":"ABCD",
              "opprettetTidspunkt":"2020-01-01T23:34:55.869121094",    
              "stønadsendringListe": [
                {
                 "type": "BIDRAG",
                 "sak": "1234567",
                 "skyldner": "98765432109",
                 "kravhaver": "12345678901",
                 "mottaker": "16498311338",
                 "innkreving": "MED_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
                }
              ],
              "sporingsdata":
                {
                "correlationId":""            
                }
            }
            """.trimIndent(),
        )
        verify(statistikkKafkaEventProducerMock, times(1)).publishBidrag(captor.capture())

        val hendelser = captor.allValues
        assertThat(hendelser[0].vedtaksid).isEqualTo(999999999)
        assertThat(hendelser[0].vedtakstidspunkt).isEqualTo("2020-01-01T23:34:55.869121094")
        assertThat(hendelser[0].type).isEqualTo("ALDERSJUSTERING")
        assertThat(hendelser[0].saksnr).isEqualTo("1234567")
        assertThat(hendelser[0].skyldner).isEqualTo("98765432109")
        assertThat(hendelser[0].kravhaver).isEqualTo("12345678901")
        assertThat(hendelser[0].mottaker).isEqualTo("16498311338")
        assertThat(hendelser[0].historiskVedtak).isFalse
        assertThat(hendelser[0].bidragPeriodeListe.first().bidragsevne).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().underholdskostnad).isEqualTo(BigDecimal.valueOf(500))
        assertThat(hendelser[0].bidragPeriodeListe.first().skyldnersAndelUnderholdskostnad).isEqualTo(BigDecimal.valueOf(3))
        assertThat(hendelser[0].bidragPeriodeListe.first().samværsfradrag).isEqualTo(BigDecimal.valueOf(150))
        assertThat(hendelser[0].bidragPeriodeListe.first().nettoBarnetilleggSkyldner).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().nettoBarnetilleggMottaker).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().skyldnerBorMedAndreVoksne).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().samværsklasse).isEqualTo(Samværsklasse.SAMVÆRSKLASSE_2)
    }

    @Test
    fun `skal lese vedtakshendelse for aldersjustering fra Bisys uten feil`() {
        val captor = argumentCaptor<BidragHendelse>()
        stubHenteVedtak(byggVedtakDtoBidrag("bisys"))
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"AUTOMATISK",
              "type":"ALDERSJUSTERING",
              "id":"999999999",
              "opprettetAv":"ABCDEFG",
              "kildeapplikasjon":"bisys",              
              "vedtakstidspunkt":"2020-01-01T23:34:55.869121094",              
              "enhetsnummer":"ABCD",
              "opprettetTidspunkt":"2020-01-01T23:34:55.869121094",    
              "stønadsendringListe": [
                {
                 "type": "BIDRAG",
                 "sak": "1234567",
                 "skyldner": "98765432109",
                 "kravhaver": "12345678901",
                 "mottaker": "16498311338",
                 "innkreving": "MED_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
                }
              ],
              "sporingsdata":
                {
                "correlationId":""            
                }
            }
            """.trimIndent(),
        )
        verify(statistikkKafkaEventProducerMock, times(1)).publishBidrag(captor.capture())

        val hendelser = captor.allValues
        assertThat(hendelser[0].vedtaksid).isEqualTo(999999999)
        assertThat(hendelser[0].vedtakstidspunkt).isEqualTo("2020-01-02T00:34:55.869121094")
        assertThat(hendelser[0].type).isEqualTo("ALDERSJUSTERING")
        assertThat(hendelser[0].saksnr).isEqualTo("1234567")
        assertThat(hendelser[0].skyldner).isEqualTo("98765432109")
        assertThat(hendelser[0].kravhaver).isEqualTo("12345678901")
        assertThat(hendelser[0].mottaker).isEqualTo("16498311338")
        assertThat(hendelser[0].historiskVedtak).isTrue
        assertThat(hendelser[0].bidragPeriodeListe.first().bidragsevne).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().underholdskostnad).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().skyldnersAndelUnderholdskostnad).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().samværsfradrag).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().nettoBarnetilleggSkyldner).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().nettoBarnetilleggMottaker).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().skyldnerBorMedAndreVoksne).isNull()
        assertThat(hendelser[0].bidragPeriodeListe.first().samværsklasse).isNull()
    }

    @Test
    fun `skal lese vedtakshendelse Bidrag med netto tilsynsutgift og faktisk utgift uten feil`() {
        val captor = argumentCaptor<BidragHendelse>()
        stubHenteVedtak(lesVedtakDtoFraFil("src/test/resources/fil/vedtakmednettotilsynsutgiftogfaktiskutgift.json"))
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"MANUELT",
              "type":"ENDRING",
              "id":"999999999",
              "opprettetAv":"ABCDEFG",
              "kildeapplikasjon":"bidrag-behandling",              
              "vedtakstidspunkt":"2025-08-27T11:00:00.000001",              
              "enhetsnummer":"ABCD",
              "opprettetTidspunkt":"2025-08-27T11:00:00.000001",    
              "stønadsendringListe": [
                {
                 "type": "BIDRAG",
                 "sak": "1234567",
                 "skyldner": "12345678901",
                 "kravhaver": "23456789012",
                 "mottaker": "34567890123",
                 "innkreving": "UTEN_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
                }
              ],
              "sporingsdata":
                {
                "correlationId":""            
                }
            }
            """.trimIndent(),
        )
        verify(statistikkKafkaEventProducerMock, times(1)).publishBidrag(captor.capture())

        val hendelser = captor.allValues
        assertThat(hendelser[0].vedtaksid).isEqualTo(999999999)
        assertThat(hendelser[0].vedtakstidspunkt).isEqualTo("2025-08-27T11:00:00.000001")
        assertThat(hendelser[0].type).isEqualTo("ENDRING")
        assertThat(hendelser[0].saksnr).isEqualTo("1234567")
        assertThat(hendelser[0].skyldner).isEqualTo("12345678901")
        assertThat(hendelser[0].kravhaver).isEqualTo("23456789012")
        assertThat(hendelser[0].mottaker).isEqualTo("34567890123")
        assertThat(hendelser[0].historiskVedtak).isFalse
        assertThat(hendelser[0].bidragPeriodeListe.size == 1)

        assertThat(hendelser[0].bidragPeriodeListe[0].periodeFra).isEqualTo(LocalDate.of(2025, 7, 1))
        assertThat(hendelser[0].bidragPeriodeListe[0].periodeTil).isNull()
        assertThat(hendelser[0].bidragPeriodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(2170))
        assertThat(hendelser[0].bidragPeriodeListe[0].bidragsevne).isEqualTo(BigDecimal.valueOf(7798.48))
        assertThat(hendelser[0].bidragPeriodeListe[0].underholdskostnad).isEqualTo(BigDecimal.valueOf(7925.31))
        assertThat(hendelser[0].bidragPeriodeListe[0].skyldnersAndelUnderholdskostnad).isEqualTo(BigDecimal.valueOf(3266.75))
        assertThat(hendelser[0].bidragPeriodeListe[0].samværsfradrag).isEqualTo(BigDecimal.valueOf(1099))
        assertThat(hendelser[0].bidragPeriodeListe[0].nettoBarnetilleggSkyldner).isEqualTo(BigDecimal.valueOf(613.58))
        assertThat(hendelser[0].bidragPeriodeListe[0].nettoBarnetilleggMottaker).isNull()
        assertThat(hendelser[0].bidragPeriodeListe[0].skyldnerBorMedAndreVoksne).isFalse
        assertThat(hendelser[0].bidragPeriodeListe[0].samværsklasse).isEqualTo(Samværsklasse.SAMVÆRSKLASSE_2)
        assertThat(hendelser[0].bidragPeriodeListe[0].skyldnerInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(496864))
        assertThat(hendelser[0].bidragPeriodeListe[0].mottakerInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(708553))
    }

    @Disabled
    @Test
    fun `skal lese vedtakshendelse Bidrag med netto tilsynsutgift og faktisk utgift uten feil2`() {
        val captor = argumentCaptor<BidragHendelse>()
        stubHenteVedtak(lesVedtakDtoFraFil("src/test/resources/fil/test.json"))
        vedtakHendelseListener.lesHendelse(
            """
            {
              "kilde":"MANUELT",
              "type":"INNKREVING",
              "id":"999999999",
              "opprettetAv":"ABCDEFG",
              "kildeapplikasjon":"bisys",              
              "vedtakstidspunkt":"2012-10-03T09:44:33.000619",              
              "enhetsnummer":"ABCD",
              "opprettetTidspunkt":"2012-10-03T09:44:33.000619",    
              "stønadsendringListe": [
                {
                 "type": "OPPFOSTRINGSBIDRAG",
                 "sak": "1210712",
                 "skyldner": "12345678901",
                 "kravhaver": "23456789012",
                 "mottaker": "34567890123",
                 "innkreving": "MED_INNKREVING",
                 "beslutning": "ENDRING",
                 "periodeListe": []             
                }
              ],
              "sporingsdata":
                {
                "correlationId":""            
                }
            }
            """.trimIndent(),
        )
        verify(statistikkKafkaEventProducerMock, times(2)).publishBidrag(captor.capture())

        val hendelser = captor.allValues
        /*        assertThat(hendelser[0].vedtaksid).isEqualTo(4796607)
                assertThat(hendelser[0].vedtakstidspunkt).isEqualTo("2025-08-27T11:00:00.000001")
                assertThat(hendelser[0].type).isEqualTo("ENDRING")
                assertThat(hendelser[0].saksnr).isEqualTo("1210712")
                assertThat(hendelser[0].skyldner).isEqualTo("12345678901")
                assertThat(hendelser[0].kravhaver).isEqualTo("23456789012")
                assertThat(hendelser[0].mottaker).isEqualTo("34567890123")
                assertThat(hendelser[0].historiskVedtak).isFalse
                assertThat(hendelser[0].bidragPeriodeListe.size == 1)

                assertThat(hendelser[0].bidragPeriodeListe[0].periodeFra).isEqualTo(LocalDate.of(2025, 7, 1))
                assertThat(hendelser[0].bidragPeriodeListe[0].periodeTil).isNull()
                assertThat(hendelser[0].bidragPeriodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(2170))
                assertThat(hendelser[0].bidragPeriodeListe[0].bidragsevne).isEqualTo(BigDecimal.valueOf(7798.48))
                assertThat(hendelser[0].bidragPeriodeListe[0].underholdskostnad).isEqualTo(BigDecimal.valueOf(7925.31))
                assertThat(hendelser[0].bidragPeriodeListe[0].skyldnersAndelUnderholdskostnad).isEqualTo(BigDecimal.valueOf(3266.75))
                assertThat(hendelser[0].bidragPeriodeListe[0].samværsfradrag).isEqualTo(BigDecimal.valueOf(1099))
                assertThat(hendelser[0].bidragPeriodeListe[0].nettoBarnetilleggSkyldner).isEqualTo(BigDecimal.valueOf(613.58))
                assertThat(hendelser[0].bidragPeriodeListe[0].nettoBarnetilleggMottaker).isNull()
                assertThat(hendelser[0].bidragPeriodeListe[0].skyldnerBorMedAndreVoksne).isFalse
                assertThat(hendelser[0].bidragPeriodeListe[0].samværsklasse).isEqualTo(Samværsklasse.SAMVÆRSKLASSE_2)
                assertThat(hendelser[0].bidragPeriodeListe[0].skyldnerInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(496864))
                assertThat(hendelser[0].bidragPeriodeListe[0].mottakerInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(708553))*/
    }

    @Test
    fun `skal lese vedtakshendelse for vedtak med nytt format SluttberegningBarnebidragV2`() {
        val captor = argumentCaptor<BidragHendelse>()
        stubHenteVedtak(lesVedtakDtoFraFil("src/test/resources/fil/vedtakMedSluttberegningBarnebidragV2.json"))
        vedtakHendelseListener.lesHendelse(
            """
      {
        "kilde": "MANUELT",
        "type": "FASTSETTELSE",
        "id": 99999999,
        "opprettetAv": "Z994977",
        "opprettetAvNavn": "F_Z994977 E_Z994977",
        "kildeapplikasjon": "bidrag-behandling-q2",
        "vedtakstidspunkt": "2025-12-03T13:41:00.788938207",
        "enhetsnummer": "4806",
        "innkrevingUtsattTilDato": "2025-12-06",
        "fastsattILand": null,
        "opprettetTidspunkt": "2025-12-03T13:41:01.514622789",
        "stønadsendringListe": [
          {
            "type": "BIDRAG",
            "sak": "2500288",
            "skyldner": "25439215782",
            "kravhaver": "21461495300",
            "mottaker": "14418321483",
            "førsteIndeksreguleringsår": 2026,
            "innkreving": "MED_INNKREVING",
            "beslutning": "ENDRING",
            "omgjørVedtakId": null,
            "eksternReferanse": null,
            "periodeListe": [
              {
                "periode": {
                  "fom": "2024-01",
                  "til": "2024-07"
                },
                "beløp": 5750,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              },
              {
                "periode": {
                  "fom": "2024-07",
                  "til": "2025-01"
                },
                "beløp": 5650,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              },
              {
                "periode": {
                  "fom": "2025-01",
                  "til": "2025-06"
                },
                "beløp": 5650,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              },
              {
                "periode": {
                  "fom": "2025-06",
                  "til": "2025-07"
                },
                "beløp": 2640,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              },
              {
                "periode": {
                  "fom": "2025-07",
                  "til": "2025-08"
                },
                "beløp": 2850,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              },
              {
                "periode": {
                  "fom": "2025-08",
                  "til": "2025-12"
                },
                "beløp": null,
                "valutakode": null,
                "resultatkode": "IKKE_OMSORG_FOR_BARNET",
                "delytelseId": null
              },
              {
                "periode": {
                  "fom": "2025-12",
                  "til": null
                },
                "beløp": 2850,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              }
            ]
          },
          {
            "type": "BIDRAG",
            "sak": "2500288",
            "skyldner": "25439215782",
            "kravhaver": "19460950603",
            "mottaker": "14418321483",
            "førsteIndeksreguleringsår": 2026,
            "innkreving": "MED_INNKREVING",
            "beslutning": "ENDRING",
            "omgjørVedtakId": null,
            "eksternReferanse": null,
            "periodeListe": [
              {
                "periode": {
                  "fom": "2024-01",
                  "til": "2024-07"
                },
                "beløp": 6240,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              },
              {
                "periode": {
                  "fom": "2024-07",
                  "til": "2025-01"
                },
                "beløp": 6550,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              },
              {
                "periode": {
                  "fom": "2025-01",
                  "til": "2025-06"
                },
                "beløp": 6550,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              },
              {
                "periode": {
                  "fom": "2025-06",
                  "til": "2025-07"
                },
                "beløp": 8160,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              },
              {
                "periode": {
                  "fom": "2025-07",
                  "til": "2025-08"
                },
                "beløp": 7560,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              },
              {
                "periode": {
                  "fom": "2025-08",
                  "til": "2025-12"
                },
                "beløp": 7560,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              },
              {
                "periode": {
                  "fom": "2025-12",
                  "til": null
                },
                "beløp": 7560,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              }
            ]
          },
          {
            "type": "BIDRAG",
            "sak": "2500288",
            "skyldner": "25439215782",
            "kravhaver": "09501077442",
            "mottaker": "14418321483",
            "førsteIndeksreguleringsår": 2026,
            "innkreving": "MED_INNKREVING",
            "beslutning": "ENDRING",
            "omgjørVedtakId": null,
            "eksternReferanse": null,
            "periodeListe": [
              {
                "periode": {
                  "fom": "2024-01",
                  "til": "2024-07"
                },
                "beløp": 6500,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              },
              {
                "periode": {
                  "fom": "2024-07",
                  "til": "2025-01"
                },
                "beløp": 6210,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              },
              {
                "periode": {
                  "fom": "2025-01",
                  "til": "2025-06"
                },
                "beløp": 6210,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              },
              {
                "periode": {
                  "fom": "2025-06",
                  "til": "2025-07"
                },
                "beløp": 7600,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              },
              {
                "periode": {
                  "fom": "2025-07",
                  "til": "2025-08"
                },
                "beløp": 7960,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              },
              {
                "periode": {
                  "fom": "2025-08",
                  "til": "2025-12"
                },
                "beløp": 7960,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              },
              {
                "periode": {
                  "fom": "2025-12",
                  "til": null
                },
                "beløp": 7960,
                "valutakode": "NOK",
                "resultatkode": "BEREGNET_BIDRAG",
                "delytelseId": null
              }
            ]
          }
        ],
        "engangsbeløpListe": [],
        "behandlingsreferanseListe": [
          {
            "kilde": "BEHANDLING_ID",
            "referanse": "1059"
          },
          {
            "kilde": "BISYS_SØKNAD",
            "referanse": "50010388"
          },
          {
            "kilde": "BISYS_SØKNAD",
            "referanse": "50010362"
          }
        ],
        "sporingsdata": {
          "correlationId": "6888c9d6591d4143942e976d352cf3bd-bidrag-behandling-q2",
          "brukerident": null,
          "opprettet": "2025-12-03T13:41:03.173297686",
          "opprettetAv": null
        }
      }
            """.trimIndent(),
        )

        verify(statistikkKafkaEventProducerMock, times(3)).publishBidrag(captor.capture())

        val hendelser = captor.allValues
        assertThat(hendelser[0].vedtaksid).isEqualTo(99999999)
        assertThat(hendelser[0].vedtakstidspunkt.truncatedTo(ChronoUnit.MICROS)).isEqualTo("2025-12-03T13:41:00.788938")
        assertThat(hendelser[0].type).isEqualTo("FASTSETTELSE")
        assertThat(hendelser[0].saksnr).isEqualTo("2500288")
        assertThat(hendelser[0].skyldner).isEqualTo("25439215782")
        assertThat(hendelser[0].kravhaver).isEqualTo("21461495300")
        assertThat(hendelser[0].mottaker).isEqualTo("14418321483")
        assertThat(hendelser[0].historiskVedtak).isFalse
        assertThat(hendelser[0].bidragPeriodeListe.size == 7)

        assertThat(hendelser[2].skyldner).isEqualTo("25439215782")
        assertThat(hendelser[2].kravhaver).isEqualTo("09501077442")
        assertThat(hendelser[2].mottaker).isEqualTo("14418321483")
        assertThat(hendelser[2].historiskVedtak).isFalse
        assertThat(hendelser[2].bidragPeriodeListe.size == 7)

        assertThat(hendelser[0].bidragPeriodeListe[0].periodeFra).isEqualTo(LocalDate.of(2024, 1, 1))
        assertThat(hendelser[0].bidragPeriodeListe[0].periodeTil).isEqualTo(LocalDate.of(2024, 7, 1))
        assertThat(hendelser[0].bidragPeriodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(5750))
        assertThat(hendelser[0].bidragPeriodeListe[0].bidragsevne).isEqualTo(BigDecimal.valueOf(28078.33))
        assertThat(hendelser[0].bidragPeriodeListe[0].underholdskostnad).isEqualTo(BigDecimal.valueOf(8778.69))
        assertThat(hendelser[0].bidragPeriodeListe[0].skyldnersAndelUnderholdskostnad).isEqualTo(BigDecimal.valueOf(7116.91))
        assertThat(hendelser[0].bidragPeriodeListe[0].nettoTilsynsutgift).isEqualTo(BigDecimal.valueOf(555.69))
        assertThat(hendelser[0].bidragPeriodeListe[0].faktiskUtgift).isEqualTo(BigDecimal.valueOf(1375))
        assertThat(hendelser[0].bidragPeriodeListe[0].samværsfradrag).isEqualTo(BigDecimal.ZERO)
        assertThat(hendelser[0].bidragPeriodeListe[0].nettoBarnetilleggSkyldner).isNull()
        assertThat(hendelser[0].bidragPeriodeListe[0].nettoBarnetilleggMottaker).isNull()
        assertThat(hendelser[0].bidragPeriodeListe[0].skyldnerBorMedAndreVoksne).isFalse
        assertThat(hendelser[0].bidragPeriodeListe[0].samværsklasse).isEqualTo(Samværsklasse.SAMVÆRSKLASSE_0)
        assertThat(hendelser[0].bidragPeriodeListe[0].skyldnerInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(913213))
        assertThat(hendelser[0].bidragPeriodeListe[0].mottakerInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(213233))

        assertThat(hendelser[2].bidragPeriodeListe[6].periodeFra).isEqualTo(LocalDate.of(2025, 12, 1))
        assertThat(hendelser[2].bidragPeriodeListe[6].periodeTil).isNull()
        assertThat(hendelser[2].bidragPeriodeListe[6].beløp).isEqualTo(BigDecimal.valueOf(7960))
        assertThat(hendelser[2].bidragPeriodeListe[6].bidragsevne).isEqualTo(BigDecimal.valueOf(24740.25))
        assertThat(hendelser[2].bidragPeriodeListe[6].underholdskostnad).isEqualTo(BigDecimal.valueOf(11084))
        assertThat(hendelser[2].bidragPeriodeListe[6].skyldnersAndelUnderholdskostnad).isEqualTo(BigDecimal.valueOf(8985.83))
        assertThat(hendelser[2].bidragPeriodeListe[6].nettoTilsynsutgift).isNull()
        assertThat(hendelser[2].bidragPeriodeListe[6].faktiskUtgift).isNull()
        assertThat(hendelser[2].bidragPeriodeListe[6].samværsfradrag).isEqualTo(BigDecimal.ZERO)
        assertThat(hendelser[2].bidragPeriodeListe[6].nettoBarnetilleggSkyldner).isNull()
        assertThat(hendelser[2].bidragPeriodeListe[6].nettoBarnetilleggMottaker).isNull()
        assertThat(hendelser[2].bidragPeriodeListe[6].skyldnerBorMedAndreVoksne).isFalse
        assertThat(hendelser[2].bidragPeriodeListe[6].samværsklasse).isEqualTo(Samværsklasse.SAMVÆRSKLASSE_0)
        assertThat(hendelser[2].bidragPeriodeListe[6].skyldnerInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(913213))
        assertThat(hendelser[2].bidragPeriodeListe[6].mottakerInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(213233))
    }

    @Test
    fun `skal lese vedtakshendelse for særbidragsvedtak`() {
        val captor = argumentCaptor<SærbidragHendelse>()
        stubHenteVedtak(lesVedtakDtoFraFil("src/test/resources/fil/vedtakMedSærbidrag.json"))
        vedtakHendelseListener.lesHendelse(
            """
    {
	"kilde": "MANUELT",
	"type": "FASTSETTELSE",
	"id": 1,
	"opprettetAv": "Z990313",
	"opprettetAvNavn": "F_Z990313 E_Z990313",
	"kildeapplikasjon": "bidrag-behandling-q2",
	"vedtakstidspunkt": "2026-04-30T08:33:02.88514208",
	"enhetsnummer": "4806",
	"innkrevingUtsattTilDato": null,
	"fastsattILand": null,
	"opprettetTidspunkt": "2026-04-30T08:33:04.68655846",
	"stønadsendringListe": [],
	"engangsbeløpListe": [
		{
			"type": "SÆRBIDRAG",
			"sak": "2500050",
			"skyldner": "1",
			"kravhaver": "3",
			"mottaker": "2",
			"beløp": 7887,
			"valutakode": "NOK",
			"resultatkode": "SÆRBIDRAG_INNVILGET",
			"innkreving": "MED_INNKREVING",
			"beslutning": "ENDRING",
			"omgjørVedtakId": null,
			"referanse": "behandling_1130_20260430082804_SÆRBIDRAG",
			"delytelseId": null,
			"eksternReferanse": null,
			"betaltBeløp": 9700
		}
	],
	"behandlingsreferanseListe": [
		{
			"kilde": "BEHANDLING_ID",
			"referanse": "1130"
		},
		{
			"kilde": "BISYS_SØKNAD",
			"referanse": "50010555"
		}
	],
	"sporingsdata": {
		"correlationId": "690012ce05e24b3f87f55819f11ea5d2-bidrag-behandling-q2",
		"brukerident": null,
		"opprettet": "2026-04-30T08:33:05.273650684",
		"opprettetAv": null
	}
}
            """.trimIndent(),
        )

        verify(statistikkKafkaEventProducerMock, times(1)).publishSærbidrag((captor.capture()))

        val hendelser = captor.allValues

        assertThat(hendelser[0].vedtaksid).isEqualTo(1)
        assertThat(hendelser[0].vedtakstidspunkt.truncatedTo(ChronoUnit.MICROS)).isEqualTo("2026-04-30T08:33:02.885142")
        assertThat(hendelser[0].type).isEqualTo("FASTSETTELSE")
        assertThat(hendelser[0].kategori).isEqualTo(Særbidragskategori.OPTIKK)
        assertThat(hendelser[0].saksnr).isEqualTo("2500050")
        assertThat(hendelser[0].skyldner).isEqualTo("1")
        assertThat(hendelser[0].kravhaver).isEqualTo("3")
        assertThat(hendelser[0].mottaker).isEqualTo("2")
        assertThat(hendelser[0].beløp).isEqualTo(BigDecimal.valueOf(7887))
        assertThat(hendelser[0].valutakode).isEqualTo("NOK")
        assertThat(hendelser[0].resultat).isEqualTo("SÆRBIDRAG_INNVILGET")
        assertThat(hendelser[0].innkreving).isTrue
        assertThat(hendelser[0].omgjørVedtakId).isNull()
        assertThat(hendelser[0].kravbeløp).isEqualTo(BigDecimal.valueOf(18000))
        assertThat(hendelser[0].godkjentBeløp).isEqualTo(BigDecimal.valueOf(14000))
        assertThat(hendelser[0].betaltBeløp).isEqualTo(BigDecimal.valueOf(9700))
        assertThat(hendelser[0].historiskVedtak).isFalse
        assertThat(hendelser[0].skyldnerInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(400000))
        assertThat(hendelser[0].mottakerInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(320000))
    }

    @Test
    fun `skal lese vedtakshendelse for historisk særbidragsvedtak`() {
        val captor = argumentCaptor<SærbidragHendelse>()
        stubHenteVedtak(lesVedtakDtoFraFil("src/test/resources/fil/vedtakMedSærbidragHistoriskVedtak.json"))
        vedtakHendelseListener.lesHendelse(
            """
    {
	"kilde": "MANUELT",
	"type": "FASTSETTELSE",
	"id": 1,
	"opprettetAv": "Z990313",
	"opprettetAvNavn": "F_Z990313 E_Z990313",
	"kildeapplikasjon": "bisys",
	"vedtakstidspunkt": "2026-04-30T08:33:02.88514208",
	"enhetsnummer": "4806",
	"innkrevingUtsattTilDato": null,
	"fastsattILand": null,
	"opprettetTidspunkt": "2026-04-30T08:33:04.68655846",
	"stønadsendringListe": [],
	"engangsbeløpListe": [
		{
			"type": "SÆRBIDRAG",
			"sak": "2500050",
			"skyldner": "1",
			"kravhaver": "3",
			"mottaker": "2",
			"beløp": 7887,
			"valutakode": "NOK",
			"resultatkode": "SÆRBIDRAG_INNVILGET",
			"innkreving": "MED_INNKREVING",
			"beslutning": "ENDRING",
			"omgjørVedtakId": null,
			"referanse": "behandling_1130_20260430082804_SÆRBIDRAG",
			"delytelseId": null,
			"eksternReferanse": null,
			"betaltBeløp": 9700
		}
	],
	"behandlingsreferanseListe": [
		{
			"kilde": "BEHANDLING_ID",
			"referanse": "1130"
		},
		{
			"kilde": "BISYS_SØKNAD",
			"referanse": "50010555"
		}
	],
	"sporingsdata": {
		"correlationId": "690012ce05e24b3f87f55819f11ea5d2-bidrag-behandling-q2",
		"brukerident": null,
		"opprettet": "2026-04-30T08:33:05.273650684",
		"opprettetAv": null
	}
}
            """.trimIndent(),
        )

        verify(statistikkKafkaEventProducerMock, times(1)).publishSærbidrag((captor.capture()))

        val hendelser = captor.allValues

        assertThat(hendelser[0].vedtaksid).isEqualTo(1)
        assertThat(hendelser[0].vedtakstidspunkt.truncatedTo(ChronoUnit.MICROS)).isEqualTo("2026-04-30T10:33:02.885142")
        assertThat(hendelser[0].type).isEqualTo("FASTSETTELSE")
        assertThat(hendelser[0].kategori).isNull()
        assertThat(hendelser[0].saksnr).isEqualTo("2500050")
        assertThat(hendelser[0].skyldner).isEqualTo("1")
        assertThat(hendelser[0].kravhaver).isEqualTo("3")
        assertThat(hendelser[0].mottaker).isEqualTo("2")
        assertThat(hendelser[0].beløp).isEqualTo(BigDecimal.valueOf(7887))
        assertThat(hendelser[0].valutakode).isEqualTo("NOK")
        assertThat(hendelser[0].resultat).isEqualTo("SÆRBIDRAG_INNVILGET")
        assertThat(hendelser[0].innkreving).isTrue
        assertThat(hendelser[0].omgjørVedtakId).isNull()
        assertThat(hendelser[0].kravbeløp).isNull()
        assertThat(hendelser[0].godkjentBeløp).isNull()
        assertThat(hendelser[0].betaltBeløp).isEqualTo(BigDecimal.valueOf(9700))
        assertThat(hendelser[0].historiskVedtak).isTrue
        assertThat(hendelser[0].skyldnerInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.ZERO)
        assertThat(hendelser[0].mottakerInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `skal lese vedtakshendelse for særbidragsvedtak med barnetillegg, vedtak fra Q2`() {
        val captor = argumentCaptor<SærbidragHendelse>()
        stubHenteVedtak(lesVedtakDtoFraFil("src/test/resources/fil/vedtakMedSærbidragMedBarnetilleggBeggeParter.json"))
        vedtakHendelseListener.lesHendelse(
            """
{
	"kilde": "MANUELT",
	"type": "FASTSETTELSE",
	"id": 1411,
	"opprettetAv": "Z990313",
	"opprettetAvNavn": "F_Z990313 E_Z990313",
	"kildeapplikasjon": "bidrag-behandling-q2",
	"vedtakstidspunkt": "2026-08-17T08:34:25.911971117",
	"enhetsnummer": "4806",
	"innkrevingUtsattTilDato": null,
	"fastsattILand": null,
	"opprettetTidspunkt": "2026-08-17T08:34:26.847325557",
	"stønadsendringListe": [],
	"engangsbeløpListe": [
		{
			"type": "SÆRBIDRAG",
			"sak": "2400150",
			"skyldner": "23418445645",
			"kravhaver": "02422177550",
			"mottaker": "30528732328",
			"beløp": null,
			"valutakode": "NOK",
			"resultatkode": "SÆRBIDRAG_IKKE_FULL_BIDRAGSEVNE",
			"innkreving": "MED_INNKREVING",
			"beslutning": "ENDRING",
			"omgjørVedtakId": null,
			"referanse": "behandling_1910_20260817082809_SÆRBIDRAG",
			"delytelseId": null,
			"eksternReferanse": null,
			"betaltBeløp": 0
		}
	],
	"behandlingsreferanseListe": [
		{
			"kilde": "BEHANDLING_ID",
			"referanse": "1910"
		},
		{
			"kilde": "BISYS_SØKNAD",
			"referanse": "50014156"
		}
	],
	"sporingsdata": {
		"correlationId": "f927120445574e269cba950c6f0cb1ee-bidrag-behandling-q2",
		"brukerident": null,
		"opprettet": "2026-08-17T08:34:27.591059991",
		"opprettetAv": null
	}
}
            """.trimIndent(),
        )

        verify(statistikkKafkaEventProducerMock, times(1)).publishSærbidrag((captor.capture()))

        val hendelser = captor.allValues

        val barnetilleggskyldnerInntektListe = hendelser[0].skyldnerInntektListe?.filter { it.type == Inntektsrapportering.BARNETILLEGG.name }
        val barnetilleggmottakerInntektListe = hendelser[0].mottakerInntektListe?.filter { it.type == Inntektsrapportering.BARNETILLEGG.name }

        assertThat(hendelser[0].vedtaksid).isEqualTo(1411)
        assertThat(hendelser[0].type).isEqualTo("FASTSETTELSE")
        assertThat(hendelser[0].kategori).isEqualTo(Særbidragskategori.TANNREGULERING)
        assertThat(hendelser[0].saksnr).isEqualTo("2400150")
        assertThat(hendelser[0].skyldner).isEqualTo("23418445645")
        assertThat(hendelser[0].kravhaver).isEqualTo("02422177550")
        assertThat(hendelser[0].mottaker).isEqualTo("30528732328")
        assertThat(hendelser[0].referanse).isEqualTo("behandling_1910_20260817082809_SÆRBIDRAG")
        assertThat(hendelser[0].beløp).isNull()
        assertThat(hendelser[0].valutakode).isEqualTo("NOK")
        assertThat(hendelser[0].resultat).isEqualTo("SÆRBIDRAG_IKKE_FULL_BIDRAGSEVNE")
        assertThat(hendelser[0].innkreving).isTrue
        assertThat(hendelser[0].omgjørVedtakId).isNull()
        assertThat(hendelser[0].kravbeløp).isEqualTo(BigDecimal.valueOf(19000))
        assertThat(hendelser[0].godkjentBeløp).isEqualTo(BigDecimal.valueOf(19000))
        assertThat(hendelser[0].betaltBeløp).isEqualTo(BigDecimal.valueOf(0))
        assertThat(hendelser[0].historiskVedtak).isFalse
        assertThat(hendelser[0].skyldnerInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(320400))
        assertThat(hendelser[0].mottakerInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(236000))

        assertThat(barnetilleggskyldnerInntektListe?.size).isEqualTo(2)
        assertThat(barnetilleggmottakerInntektListe?.size).isEqualTo(2)

        assertThat(
            barnetilleggskyldnerInntektListe?.filter { it.type == Inntektsrapportering.BARNETILLEGG.name }
                ?.all { it.gjelderKravhaver == "02422177550" },
        ).isTrue
        assertThat(
            barnetilleggmottakerInntektListe?.filter { it.type == Inntektsrapportering.BARNETILLEGG.name }
                ?.all { it.gjelderKravhaver == "02422177550" },
        ).isTrue

        assertThat(barnetilleggskyldnerInntektListe?.firstOrNull { it.beløp == BigDecimal.valueOf(14400) }?.inntektstype == Inntektstype.BARNETILLEGG_AAP.name)
        assertThat(barnetilleggskyldnerInntektListe?.firstOrNull { it.beløp == BigDecimal.valueOf(600) }?.inntektstype == Inntektstype.BARNETILLEGG_DAGPENGER.name)

        assertThat(barnetilleggmottakerInntektListe?.firstOrNull { it.beløp == BigDecimal.valueOf(12000) }?.inntektstype == Inntektstype.BARNETILLEGG_DAGPENGER.name)
        assertThat(barnetilleggmottakerInntektListe?.firstOrNull { it.beløp == BigDecimal.valueOf(24000) }?.inntektstype == Inntektstype.BARNETILLEGG_STOREBRAND.name)
    }

    @Test
    fun `skal lese vedtakshendelse uten grunnlagsobjekt for BM`() {
        val captor = argumentCaptor<SærbidragHendelse>()
        stubHenteVedtak(lesVedtakDtoFraFil("src/test/resources/fil/vedtakUtenGrunnlagsobjektBM.json"))
        vedtakHendelseListener.lesHendelse(
            """
    {
	"kilde": "MANUELT",
	"type": "FASTSETTELSE",
	"id": 1,
	"opprettetAv": "Z990313",
	"opprettetAvNavn": "F_Z990313 E_Z990313",
	"kildeapplikasjon": "bidrag-behandling-q2",
	"vedtakstidspunkt": "2026-04-30T08:33:02.88514208",
	"enhetsnummer": "4806",
	"innkrevingUtsattTilDato": null,
	"fastsattILand": null,
	"opprettetTidspunkt": "2026-04-30T08:33:04.68655846",
	"stønadsendringListe": [],
	"engangsbeløpListe": [
		{
			"type": "SÆRBIDRAG",
			"sak": "2500050",
			"skyldner": "1",
			"kravhaver": "3",
			"mottaker": "2",
			"beløp": 7887,
			"valutakode": "NOK",
			"resultatkode": "SÆRBIDRAG_INNVILGET",
			"innkreving": "MED_INNKREVING",
			"beslutning": "ENDRING",
			"omgjørVedtakId": null,
			"referanse": "behandling_1130_20260430082804_SÆRBIDRAG",
			"delytelseId": null,
			"eksternReferanse": null,
			"betaltBeløp": 9700
		}
	],
	"behandlingsreferanseListe": [
		{
			"kilde": "BEHANDLING_ID",
			"referanse": "1130"
		},
		{
			"kilde": "BISYS_SØKNAD",
			"referanse": "50010555"
		}
	],
	"sporingsdata": {
		"correlationId": "690012ce05e24b3f87f55819f11ea5d2-bidrag-behandling-q2",
		"brukerident": null,
		"opprettet": "2026-04-30T08:33:05.273650684",
		"opprettetAv": null
	}
}
            """.trimIndent(),
        )

        verify(statistikkKafkaEventProducerMock, times(1)).publishSærbidrag((captor.capture()))

        val hendelser = captor.allValues

        assertThat(hendelser[0].vedtaksid).isEqualTo(1)
        assertThat(hendelser[0].vedtakstidspunkt.truncatedTo(ChronoUnit.MICROS)).isEqualTo("2026-04-30T08:33:02.885142")
        assertThat(hendelser[0].type).isEqualTo("FASTSETTELSE")
        assertThat(hendelser[0].kategori).isEqualTo(Særbidragskategori.OPTIKK)
        assertThat(hendelser[0].saksnr).isEqualTo("2500050")
        assertThat(hendelser[0].skyldner).isEqualTo("1")
        assertThat(hendelser[0].kravhaver).isEqualTo("3")
        assertThat(hendelser[0].mottaker).isEqualTo("2")
        assertThat(hendelser[0].beløp).isEqualTo(BigDecimal.valueOf(7887))
        assertThat(hendelser[0].valutakode).isEqualTo("NOK")
        assertThat(hendelser[0].resultat).isEqualTo("SÆRBIDRAG_INNVILGET")
        assertThat(hendelser[0].innkreving).isTrue
        assertThat(hendelser[0].omgjørVedtakId).isNull()
        assertThat(hendelser[0].kravbeløp).isEqualTo(BigDecimal.valueOf(18000))
        assertThat(hendelser[0].godkjentBeløp).isEqualTo(BigDecimal.valueOf(14000))
        assertThat(hendelser[0].betaltBeløp).isEqualTo(BigDecimal.valueOf(9700))
        assertThat(hendelser[0].historiskVedtak).isFalse
        assertThat(hendelser[0].skyldnerInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(400000))
        assertThat(hendelser[0].mottakerInntektListe.isNullOrEmpty())
        assertThat(hendelser[0].kravhaverInntektListe.isNullOrEmpty())
    }

    @Test
    fun `skal lese vedtakshendelse med inntekt for flere søknadsbarn`() {
        val captor = argumentCaptor<BidragHendelse>()
        stubHenteVedtak(lesVedtakDtoFraFil("src/test/resources/fil/vedtakMedInntektFlereSøknadsbarn.json"))
        vedtakHendelseListener.lesHendelse(
            """
{
	"kilde": "MANUELT",
	"type": "ENDRING",
	"id": 99999999,
	"opprettetAv": "Z990313",
	"opprettetAvNavn": "F_Z990313 E_Z990313",
	"kildeapplikasjon": "bidrag-behandling-q2",
	"vedtakstidspunkt": "2026-08-25T14:25:37.945130823",
	"enhetsnummer": "4806",
	"innkrevingUtsattTilDato": "2026-08-28",
	"fastsattILand": null,
	"opprettetTidspunkt": "2026-08-25T14:25:38.189819882",
	"stønadsendringListe": [
		{
			"type": "BIDRAG",
			"sak": "2600125",
			"skyldner": "1",
			"kravhaver": "4",
			"mottaker": "2",
			"førsteIndeksreguleringsår": 2027,
			"innkreving": "MED_INNKREVING",
			"beslutning": "ENDRING",
			"omgjørVedtakId": null,
			"eksternReferanse": null,
			"periodeListe": [
				{
					"periode": {
						"fom": "2026-08",
						"til": null
					},
					"beløp": 1350,
					"valutakode": "NOK",
					"resultatkode": "BEREGNET_BIDRAG",
					"delytelseId": null
				}
			]
		},
		{
			"type": "BIDRAG",
			"sak": "2600125",
			"skyldner": "1",
			"kravhaver": "3",
			"mottaker": "2",
			"førsteIndeksreguleringsår": 2027,
			"innkreving": "MED_INNKREVING",
			"beslutning": "ENDRING",
			"omgjørVedtakId": null,
			"eksternReferanse": null,
			"periodeListe": [
				{
					"periode": {
						"fom": "2026-08",
						"til": null
					},
					"beløp": 1350,
					"valutakode": "NOK",
					"resultatkode": "BEREGNET_BIDRAG",
					"delytelseId": null
				}
			]
		}
	],
	"engangsbeløpListe": [
		{
			"type": "GEBYR_SKYLDNER",
			"sak": "2600125",
			"skyldner": "1",
			"kravhaver": "NAV",
			"mottaker": "NAV",
			"beløp": null,
			"valutakode": null,
			"resultatkode": "GEBYR_FRITATT",
			"innkreving": "MED_INNKREVING",
			"beslutning": "ENDRING",
			"omgjørVedtakId": null,
			"referanse": "Bisys_gebyr_BP_c6559d27-9123-40e9-b56a-3e6d82769cea",
			"delytelseId": null,
			"eksternReferanse": null,
			"betaltBeløp": null
		},
		{
			"type": "GEBYR_MOTTAKER",
			"sak": "2600125",
			"skyldner": "2",
			"kravhaver": "NAV",
			"mottaker": "NAV",
			"beløp": null,
			"valutakode": null,
			"resultatkode": "GEBYR_FRITATT",
			"innkreving": "MED_INNKREVING",
			"beslutning": "ENDRING",
			"omgjørVedtakId": null,
			"referanse": "Bisys_gebyr_BM_c294b22a-7177-4c21-98f2-e7b826c5f3ee",
			"delytelseId": null,
			"eksternReferanse": null,
			"betaltBeløp": null
		}
	],
	"behandlingsreferanseListe": [
		{
			"kilde": "BEHANDLING_ID",
			"referanse": "1971"
		},
		{
			"kilde": "BISYS_SØKNAD",
			"referanse": "50014413"
		}
	],
	"sporingsdata": {
		"correlationId": "c134742e089e40439227674666aed644-bidrag-behandling-q2",
		"brukerident": null,
		"opprettet": "2026-08-25T14:25:38.794429033",
		"opprettetAv": null
	}
}
            """.trimIndent(),
        )

        verify(statistikkKafkaEventProducerMock, times(2)).publishBidrag(captor.capture())
        val hendelser = captor.allValues
        assertThat(hendelser[0].bidragPeriodeListe[0].kravhaverInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(60000))
        assertThat(hendelser[1].bidragPeriodeListe[0].kravhaverInntektListe?.sumOf { it.beløp }).isEqualTo(BigDecimal.valueOf(50000))
    }
}

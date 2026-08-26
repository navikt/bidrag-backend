package no.nav.bidrag.statistikk.service

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.statistikk.BidragStatistikkTest
import no.nav.bidrag.statistikk.hendelse.StatistikkKafkaEventProducer
import no.nav.bidrag.transport.behandling.statistikk.BidragHendelse
import no.nav.bidrag.transport.behandling.statistikk.BidragPeriode
import no.nav.bidrag.transport.behandling.statistikk.ForskuddHendelse
import no.nav.bidrag.transport.behandling.statistikk.ForskuddPeriode
import no.nav.bidrag.transport.behandling.statistikk.Inntekt
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.anyOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import org.wiremock.spring.InjectWireMock
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("HendelserServiceTest")
@ActiveProfiles(BidragStatistikkTest.TEST_PROFILE)
@SpringBootTest(classes = [BidragStatistikkTest::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableMockOAuth2Server
@EnableWireMock(ConfigureWireMock(name = "my-service", port = 0))
class HendelserServiceTest {
    @InjectWireMock("my-service")
    var wireMock: WireMockServer? = null

    @Autowired
    private lateinit var hendelserService: HendelserService

    @MockitoBean
    private lateinit var statistikkKafkaEventProducerMock: StatistikkKafkaEventProducer

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal opprette forskuddshendelse`() {
        hendelserService.opprettForskuddshendelse(
            ForskuddHendelse(
                vedtaksid = 1,
                vedtakstidspunkt = LocalDateTime.now(),
                type = Vedtakstype.ENDRING.name,
                saksnr = "123",
                kravhaver = "12345",
                mottaker = "54321",
                historiskVedtak = false,
                forskuddPeriodeListe = listOf(
                    ForskuddPeriode(
                        periodeFra = LocalDate.of(2021, 1, 1),
                        periodeTil = LocalDate.of(2022, 1, 1),
                        beløp = BigDecimal(1000),
                        resultat = Beslutningstype.ENDRING.name,
                        barnetsAldersgruppe = "0-6",
                        antallBarnIEgenHusstand = 1.0,
                        sivilstand = "ENKE",
                        barnBorMedMottaker = true,
                        mottakerInntektListe = listOf(
                            Inntekt(
                                beløp = BigDecimal.valueOf(10000),
                                type = Inntektsrapportering.AINNTEKT_BEREGNET_12MND.name,
                            ),
                        ),
                        kravhaverInntektListe = emptyList(),
                    ),
                ),
            ),
        )

        verify(statistikkKafkaEventProducerMock).publishForskudd(anyOrNull())
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal opprette bidragshendelse`() {
        hendelserService.opprettBidragshendelse(
            BidragHendelse(
                vedtaksid = 1,
                vedtakstidspunkt = LocalDateTime.now(),
                stønadstype = Stønadstype.BIDRAG,
                type = Vedtakstype.ENDRING.name,
                saksnr = "123",
                skyldner = "23456",
                kravhaver = "12345",
                mottaker = "54321",
                historiskVedtak = false,
                innkreving = true,
                bidragPeriodeListe = listOf(
                    BidragPeriode(
                        periodeFra = LocalDate.of(2021, 1, 1),
                        periodeTil = LocalDate.of(2022, 1, 1),
                        beløp = BigDecimal(1000),
                        valutakode = "NOK",
                        resultat = Beslutningstype.ENDRING.name,
                        bidragsevne = BigDecimal(1000),
                        underholdskostnad = BigDecimal(1000),
                        skyldnersAndelUnderholdskostnad = BigDecimal(1000),
                        nettoTilsynsutgift = BigDecimal(1000),
                        faktiskUtgift = BigDecimal(1000),
                        samværsfradrag = BigDecimal(1000),
                        nettoBarnetilleggSkyldner = BigDecimal(1000),
                        nettoBarnetilleggMottaker = BigDecimal(1000),
                        skyldnerBorMedAndreVoksne = true,
                        samværsklasse = Samværsklasse.DELT_BOSTED,
                        skyldnerInntektListe = listOf(
                            Inntekt(
                                beløp = BigDecimal.valueOf(10000),
                                type = Inntektsrapportering.AINNTEKT_BEREGNET_12MND.name,
                            ),
                        ),
                        mottakerInntektListe = listOf(
                            Inntekt(
                                beløp = BigDecimal.valueOf(10000),
                                type = Inntektsrapportering.AINNTEKT_BEREGNET_12MND.name,
                            ),
                        ),
                        kravhaverInntektListe = emptyList(),
                    ),
                ),
            ),
        )

        verify(statistikkKafkaEventProducerMock).publishBidrag(anyOrNull())
    }
}

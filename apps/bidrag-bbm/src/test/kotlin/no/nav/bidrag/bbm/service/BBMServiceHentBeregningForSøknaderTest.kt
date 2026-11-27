package no.nav.bidrag.bbm.service

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.bidrag.bbm.CommonTestRunner
import no.nav.bidrag.bbm.utils.DATO_SØKNAD_SØKNAD_1
import no.nav.bidrag.bbm.utils.DATO_SØKNAD_SØKNAD_2
import no.nav.bidrag.bbm.utils.PERSONIDENT_BARN_1
import no.nav.bidrag.bbm.utils.PERSONIDENT_BARN_2
import no.nav.bidrag.bbm.utils.SAKSNUMMER_1
import no.nav.bidrag.bbm.utils.SAKSNUMMER_2
import no.nav.bidrag.bbm.utils.lageSøknadTestdata
import no.nav.bidrag.bbm.utils.opprettHendelse
import no.nav.bidrag.bbm.utils.opprettPeriodeBidrag
import no.nav.bidrag.domene.enums.behandling.HendelseType
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.HentBeregningForSøknad
import no.nav.bidrag.transport.behandling.beregning.felles.HentBeregningForSøknaderRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

private const val FRA_BBM = '1'
private const val IKKE_FRA_BBM = '0'
private const val SØKNADSID_SOM_IKKE_FINNES = 999_999L

class BBMServiceHentBeregningForSøknaderTest : CommonTestRunner() {
    @Autowired
    private lateinit var bbmService: BBMService

    private fun lagreSøknader(): Pair<Long, Long> {
        val søknader = testdataManager.lagreSøknadListe(lageSøknadTestdata())
        return søknader.first { it.saksnummer == SAKSNUMMER_1 }.søknadsid!! to
            søknader.first { it.saksnummer == SAKSNUMMER_2 }.søknadsid!!
    }

    private fun request(
        søknadsid: Long,
        saksnummer: String = SAKSNUMMER_1,
        personidentBarn: String = PERSONIDENT_BARN_1,
    ) = HentBeregningForSøknad(
        søknadsid = søknadsid,
        saksnummer = saksnummer,
        personidentBarn = Personident(personidentBarn),
        stønadstype = Stønadstype.BIDRAG,
    )

    @Test
    fun `skal hente beregninger som matcher søknadens mottatt dato og sette erVedtakMedBeregning når FRA_BBM er 1`() {
        val (søknadsid, _) = lagreSøknader()
        testdataManager.lagreHendelseListe(listOf(opprettHendelse(søknadsid = søknadsid, fraBbm = FRA_BBM)))
        // CommonTestRunner lagrer allerede periodeBidrag og samvær (klasse 1) for SAKSNUMMER_1 fra 2024-07-01 med datoSøknad 2024-01-01
        testdataManager.lagrePeriodeBidragListe(
            listOf(
                opprettPeriodeBidrag(
                    datoSøknad = DATO_SØKNAD_SØKNAD_1,
                    datoFom = LocalDate.parse("2024-02-01"),
                    faktiskBeløp = BigDecimal(2000),
                ),
                opprettPeriodeBidrag(datoSøknad = DATO_SØKNAD_SØKNAD_2, datoFom = LocalDate.parse("2024-08-01")),
            ),
        )

        val respons = bbmService.hentBeregningForSøknader(HentBeregningForSøknaderRequest(listOf(request(søknadsid))))

        respons.søknadListe shouldHaveSize 1
        assertSoftly(respons.søknadListe.first()) {
            this.søknadsid shouldBe søknadsid
            søknadMottattDato shouldBe DATO_SØKNAD_SØKNAD_1
            erVedtakMedBeregning shouldBe true
            hendelseFunnet shouldBe true
            beregningListe shouldHaveSize 2
            beregningListe.map { it.datoSøknad }.toSet() shouldBe setOf(DATO_SØKNAD_SØKNAD_1)
            beregningListe[0].periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-02-01"), LocalDate.parse("2024-07-01"))
            beregningListe[0].faktiskBeløp shouldBe BigDecimal("2000.00")
            beregningListe[1].periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null)
            beregningListe[1].faktiskBeløp shouldBe BigDecimal("1030.00")
            beregningListe[1].samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_1
        }
    }

    @Test
    fun `skal sette erVedtakMedBeregning til false når FRA_BBM er 0`() {
        val (søknadsid, _) = lagreSøknader()
        testdataManager.lagreHendelseListe(listOf(opprettHendelse(søknadsid = søknadsid, fraBbm = IKKE_FRA_BBM)))

        val respons = bbmService.hentBeregningForSøknader(HentBeregningForSøknaderRequest(listOf(request(søknadsid))))

        assertSoftly(respons.søknadListe.single()) {
            erVedtakMedBeregning shouldBe false
            hendelseFunnet shouldBe true
            søknadMottattDato shouldBe DATO_SØKNAD_SØKNAD_1
            beregningListe shouldHaveSize 1
        }
    }

    @Test
    fun `skal bruke FRA_BBM fra siste vedtakshendelse når søknaden har flere vedtakshendelser`() {
        val (søknadsid, _) = lagreSøknader()
        testdataManager.lagreHendelseListe(
            listOf(
                opprettHendelse(
                    søknadsid = søknadsid,
                    hendelsestype = HendelseType.VEDTAK,
                    fraBbm = FRA_BBM,
                    opprettetDato = LocalDateTime.parse("2024-06-01T10:00:00"),
                ),
                opprettHendelse(
                    søknadsid = søknadsid,
                    hendelsestype = HendelseType.KLAGEVEDTAK,
                    fraBbm = IKKE_FRA_BBM,
                    opprettetDato = LocalDateTime.parse("2024-07-01T10:00:00"),
                ),
            ),
        )

        val respons = bbmService.hentBeregningForSøknader(HentBeregningForSøknaderRequest(listOf(request(søknadsid))))

        respons.søknadListe.single().erVedtakMedBeregning shouldBe false
    }

    @Test
    fun `skal bruke høyeste hendelsesid når vedtakshendelser har samme opprettet dato`() {
        val (søknadsid, _) = lagreSøknader()
        val sammeTidspunkt = LocalDateTime.parse("2024-06-01T10:00:00")
        testdataManager.lagreHendelseListe(
            listOf(opprettHendelse(søknadsid = søknadsid, fraBbm = IKKE_FRA_BBM, opprettetDato = sammeTidspunkt)),
        )
        testdataManager.lagreHendelseListe(
            listOf(opprettHendelse(søknadsid = søknadsid, fraBbm = FRA_BBM, opprettetDato = sammeTidspunkt)),
        )

        val respons = bbmService.hentBeregningForSøknader(HentBeregningForSøknaderRequest(listOf(request(søknadsid))))

        respons.søknadListe.single().erVedtakMedBeregning shouldBe true
    }

    @Test
    fun `skal ignorere vedtaksforslag og andre hendelser som ikke er vedtak`() {
        val (søknadsid, _) = lagreSøknader()
        testdataManager.lagreHendelseListe(
            listOf(
                opprettHendelse(
                    søknadsid = søknadsid,
                    hendelsestype = HendelseType.VEDTAK,
                    fraBbm = IKKE_FRA_BBM,
                    opprettetDato = LocalDateTime.parse("2024-06-01T10:00:00"),
                ),
                opprettHendelse(
                    søknadsid = søknadsid,
                    hendelsestype = HendelseType.VEDTAKSFORSLAG,
                    fraBbm = FRA_BBM,
                    opprettetDato = LocalDateTime.parse("2024-07-01T10:00:00"),
                ),
                opprettHendelse(
                    søknadsid = søknadsid,
                    hendelsestype = HendelseType.SØKNAD_BM,
                    fraBbm = FRA_BBM,
                    opprettetDato = LocalDateTime.parse("2024-08-01T10:00:00"),
                ),
            ),
        )

        val respons = bbmService.hentBeregningForSøknader(HentBeregningForSøknaderRequest(listOf(request(søknadsid))))

        assertSoftly(respons.søknadListe.single()) {
            erVedtakMedBeregning shouldBe false
            hendelseFunnet shouldBe true
        }
    }

    @Test
    fun `skal returnere hendelseFunnet false uten å feile når søknaden ikke har vedtakshendelse`() {
        val (søknadsid, _) = lagreSøknader()
        testdataManager.lagreHendelseListe(
            listOf(opprettHendelse(søknadsid = søknadsid, hendelsestype = HendelseType.VEDTAKSFORSLAG, fraBbm = FRA_BBM)),
        )
        val respons = bbmService.hentBeregningForSøknader(HentBeregningForSøknaderRequest(listOf(request(søknadsid))))

        assertSoftly(respons.søknadListe.single()) {
            hendelseFunnet shouldBe false
            erVedtakMedBeregning shouldBe false
            søknadMottattDato shouldBe DATO_SØKNAD_SØKNAD_1
            beregningListe shouldHaveSize 1
        }
    }

    @Test
    fun `skal returnere hendelseFunnet false og tom beregningsliste uten å feile når søknaden ikke finnes`() {
        val respons =
            bbmService.hentBeregningForSøknader(
                HentBeregningForSøknaderRequest(listOf(request(SØKNADSID_SOM_IKKE_FINNES))),
            )

        assertSoftly(respons.søknadListe.single()) {
            søknadsid shouldBe SØKNADSID_SOM_IKKE_FINNES
            søknadMottattDato shouldBe null
            hendelseFunnet shouldBe false
            erVedtakMedBeregning shouldBe false
            beregningListe.shouldBeEmpty()
        }
    }

    @Test
    fun `skal returnere ett element per søknad i samme rekkefølge som forespørselen`() {
        val (søknadsid1, søknadsid2) = lagreSøknader()
        testdataManager.lagreHendelseListe(
            listOf(
                opprettHendelse(søknadsid = søknadsid1, fraBbm = FRA_BBM),
                opprettHendelse(søknadsid = søknadsid2, fraBbm = IKKE_FRA_BBM, saksnummer = SAKSNUMMER_2),
            ),
        )
        testdataManager.lagrePeriodeBidragListe(
            listOf(
                opprettPeriodeBidrag(saksnummer = SAKSNUMMER_2, barnId = PERSONIDENT_BARN_2, datoSøknad = DATO_SØKNAD_SØKNAD_2),
            ),
        )

        val respons =
            bbmService.hentBeregningForSøknader(
                HentBeregningForSøknaderRequest(
                    listOf(
                        request(søknadsid2, saksnummer = SAKSNUMMER_2, personidentBarn = PERSONIDENT_BARN_2),
                        request(SØKNADSID_SOM_IKKE_FINNES),
                        request(søknadsid1),
                    ),
                ),
            )

        respons.søknadListe.map { it.søknadsid } shouldBe listOf(søknadsid2, SØKNADSID_SOM_IKKE_FINNES, søknadsid1)
        assertSoftly(respons.søknadListe[0]) {
            erVedtakMedBeregning shouldBe false
            hendelseFunnet shouldBe true
            beregningListe.map { it.saksnummer to it.datoSøknad } shouldContainExactlyInAnyOrder
                listOf(SAKSNUMMER_2 to DATO_SØKNAD_SØKNAD_2)
        }
        respons.søknadListe[1].hendelseFunnet shouldBe false
        assertSoftly(respons.søknadListe[2]) {
            erVedtakMedBeregning shouldBe true
            hendelseFunnet shouldBe true
            beregningListe.map { it.saksnummer to it.datoSøknad } shouldContainExactlyInAnyOrder
                listOf(SAKSNUMMER_1 to DATO_SØKNAD_SØKNAD_1)
        }
    }
}

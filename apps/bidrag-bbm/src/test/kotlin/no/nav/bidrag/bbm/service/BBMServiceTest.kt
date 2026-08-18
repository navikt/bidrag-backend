package no.nav.bidrag.bbm.service

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.bidrag.bbm.CommonTestRunner
import no.nav.bidrag.bbm.utils.PERSONIDENT_BARN_2
import no.nav.bidrag.bbm.utils.SAKSNUMMER_1
import no.nav.bidrag.bbm.utils.SAKSNUMMER_3
import no.nav.bidrag.bbm.utils.lageSøknadTestdata
import no.nav.bidrag.bbm.utils.opprettPeriodeBidrag
import no.nav.bidrag.bbm.utils.opprettPeriodeBidragKomplett
import no.nav.bidrag.bbm.utils.opprettSamvær
import no.nav.bidrag.bbm.utils.opprettSamværKomplett
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.transport.behandling.beregning.felles.BidragBeregningRequestDto
import no.nav.bidrag.transport.behandling.beregning.felles.BidragBeregningResponsDto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class BBMServiceTest : CommonTestRunner() {
    @Autowired
    private lateinit var bbmService: BBMService

    private val personidentBarn = genererFødselsnummer()

    @Test
    fun `skal hente nyeste bidrag beregning for samme dato søknad`() {
        val søknader = testdataManager.lagreSøknadListe(lageSøknadTestdata())
        val søknadsid1 = søknader.first { it.saksnummer == SAKSNUMMER_1 }.søknadsid!!.toString()
        testdataManager.lagrePeriodeBidragListe(
            listOf(
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    søknadstype = Stønadstype.BIDRAG,
                ),
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    faktiskBeløp = BigDecimal(1000),
                    søknadstype = Stønadstype.BIDRAG,
                ),
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-08-01"),
                    faktiskBeløp = BigDecimal(1040),
                    søknadstype = Stønadstype.BIDRAG,
                ),
            ),
        )
        testdataManager.lagreSamværListe(
            listOf(
                opprettSamvær(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    søknadstype = Stønadstype.BIDRAG,
                ),
                opprettSamvær(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_2,
                    søknadstype = Stønadstype.BIDRAG,
                ),
                opprettSamvær(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-08-01"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_3,
                    søknadstype = Stønadstype.BIDRAG,
                ),
            ),
        )

        fun valider(respons: List<BidragBeregningResponsDto.BidragBeregning>) {
            respons shouldHaveSize 1
            assertSoftly(respons.first()) {
                saksnummer shouldBe SAKSNUMMER_1
                this.personidentBarn shouldBe Personident(PERSONIDENT_BARN_2)
                periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-08-01"), null)
                beregnetBeløp shouldBe BigDecimal("10024.00")
                faktiskBeløp shouldBe BigDecimal("1040.00")
                beløpSamvær shouldBe BigDecimal("100.00")
                samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_3
            }
        }

        valider(
            bbmService
                .hentSisteBidragOgSamvær(
                    BidragBeregningRequestDto(
                        listOf(
                            BidragBeregningRequestDto.HentBidragBeregning(
                                saksnummer = SAKSNUMMER_1,
                                personidentBarn = Personident(PERSONIDENT_BARN_2),
                                datoSøknad = LocalDate.parse("2024-01-01"),
                                stønadstype = Stønadstype.BIDRAG,
                            ),
                        ),
                    ),
                ).beregningListe,
        )

        valider(
            bbmService
                .hentSisteBidragOgSamvær(
                    BidragBeregningRequestDto(
                        listOf(
                            BidragBeregningRequestDto.HentBidragBeregning(
                                saksnummer = SAKSNUMMER_1,
                                personidentBarn = Personident(PERSONIDENT_BARN_2),
                                søknadsid = søknadsid1,
                                stønadstype = Stønadstype.BIDRAG,
                            ),
                        ),
                    ),
                ).beregningListe,
        )
    }

    @Test
    fun `skal hente bidrag for søknadstype bidrag 18 år`() {
        val søknader = testdataManager.lagreSøknadListe(lageSøknadTestdata())
        val søknadsid1 = søknader[0].søknadsid!!.toString()
        testdataManager.lagrePeriodeBidragListe(
            listOf(
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    søknadstype = Stønadstype.BIDRAG18AAR,
                ),
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-05-02"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    faktiskBeløp = BigDecimal(1000),
                    søknadstype = Stønadstype.BIDRAG18AAR,
                ),
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-06-02"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    faktiskBeløp = BigDecimal(1040),
                ),
            ),
        )
        testdataManager.lagreSamværListe(
            listOf(
                opprettSamvær(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    søknadstype = Stønadstype.BIDRAG18AAR,
                ),
                opprettSamvær(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-05-02"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_2,
                    søknadstype = Stønadstype.BIDRAG18AAR,
                ),
                opprettSamvær(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-04-02"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_3,
                ),
            ),
        )

        val respons =
            bbmService.hentSisteBidragOgSamvær(
                BidragBeregningRequestDto(
                    listOf(
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = SAKSNUMMER_1,
                            personidentBarn = Personident(PERSONIDENT_BARN_2),
                            søknadsid = søknadsid1,
                            stønadstype = Stønadstype.BIDRAG18AAR,
                        ),
                    ),
                ),
            )

        respons.beregningListe shouldHaveSize 1
        assertSoftly(respons.beregningListe[0]) {
            saksnummer shouldBe SAKSNUMMER_1
            this.personidentBarn shouldBe Personident(PERSONIDENT_BARN_2)
            periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null)
            beregnetBeløp shouldBe BigDecimal("10024.00")
            faktiskBeløp shouldBe BigDecimal("1030.00")
            beløpSamvær shouldBe BigDecimal("100.00")
            samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_1
        }
    }

    @Test
    fun `skal ikke hente bidrag for søknadstype bidrag 18 år hvis ikke finnes`() {
        val søknader = testdataManager.lagreSøknadListe(lageSøknadTestdata())
        val søknadsid1 = søknader.first { it.saksnummer == SAKSNUMMER_1 }.søknadsid!!.toString()
        testdataManager.lagrePeriodeBidragListe(
            listOf(
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    søknadstype = Stønadstype.BIDRAG,
                ),
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-05-02"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    faktiskBeløp = BigDecimal(1000),
                    søknadstype = Stønadstype.BIDRAG,
                ),
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-06-02"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    faktiskBeløp = BigDecimal(1040),
                    søknadstype = Stønadstype.BIDRAG,
                ),
            ),
        )

        bbmService
            .hentSisteBidragOgSamvær(
                BidragBeregningRequestDto(
                    listOf(
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = SAKSNUMMER_1,
                            personidentBarn = Personident(PERSONIDENT_BARN_2),
                            søknadsid = søknadsid1,
                            stønadstype = Stønadstype.BIDRAG18AAR,
                        ),
                    ),
                ),
            ).beregningListe shouldHaveSize 0

        bbmService
            .hentSisteBidragOgSamvær(
                BidragBeregningRequestDto(
                    listOf(
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = SAKSNUMMER_1,
                            personidentBarn = Personident(PERSONIDENT_BARN_2),
                            søknadsid = søknadsid1,
                            stønadstype = Stønadstype.BIDRAG,
                        ),
                    ),
                ),
            ).beregningListe shouldHaveSize 1
    }

    @Test
    fun `skal returnere tom liste når ingen data finnes for saksnummer`() {
        val respons = bbmService.hentAllePeriodeBidragOgSamværsklasseForSaksnummer(listOf("8888888"))

        respons.beregningListe shouldHaveSize 0
    }

    @Test
    fun `skal returnere beregning når matchende samvær og periodeBidrag finnes`() {
        testdataManager.lagrePeriodeBidragListe(
            listOf(
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                ),
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-08-01"),
                ),
            ),
        )
        testdataManager.lagreSamværListe(
            listOf(
                opprettSamvær(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_1,
                ),
                opprettSamvær(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-08-01"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_2,
                ),
                opprettSamvær(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-10-01"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_2,
                ),
            ),
        )

        val respons = bbmService.hentAllePeriodeBidragOgSamværsklasseForSaksnummer(listOf(SAKSNUMMER_3))

        respons.beregningListe shouldHaveSize 2
        assertSoftly(respons.beregningListe[0]) {
            saksnummer shouldBe SAKSNUMMER_3
            this.personidentBarn shouldBe Personident(PERSONIDENT_BARN_2)
            periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null)
            samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_1
        }
        assertSoftly(respons.beregningListe[1]) {
            saksnummer shouldBe SAKSNUMMER_3
            this.personidentBarn shouldBe Personident(PERSONIDENT_BARN_2)
            periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-08-01"), null)
            samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_2
        }
    }

    @Test
    fun `skal returnere null i samværsklasse hvis det ikke finnes matchende samvær`() {
        testdataManager.lagrePeriodeBidragListe(
            listOf(
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                ),
            ),
        )

        val respons = bbmService.hentAllePeriodeBidragOgSamværsklasseForSaksnummer(listOf(SAKSNUMMER_3))

        assertSoftly(respons.beregningListe[0]) {
            periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null)
            saksnummer shouldBe SAKSNUMMER_3
            this.personidentBarn shouldBe Personident(PERSONIDENT_BARN_2)
            samværsklasse shouldBe null
        }
    }

    @Test
    fun `skal håndtere flere saksnummer med blandet data`() {
        testdataManager.lagrePeriodeBidragListe(
            listOf(
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                ),
                opprettPeriodeBidrag(
                    saksnummer = "4567890",
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                ),
            ),
        )
        testdataManager.lagreSamværListe(
            listOf(
                opprettSamvær(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                ),
            ),
        )

        val respons = bbmService.hentAllePeriodeBidragOgSamværsklasseForSaksnummer(listOf(SAKSNUMMER_3, "4567890"))

        respons.beregningListe shouldHaveSize 2
        respons.beregningListe[0].saksnummer shouldBe SAKSNUMMER_3
    }

    @Test
    fun `skal returnere tom liste hvis ingen periodeBidrag finnes for vedtak`() {
        testdataManager.lagreSøknadListe(lageSøknadTestdata())

        val respons =
            bbmService.hentAlleBeregningerOgSamværForVedtak(
                BidragBeregningRequestDto(
                    listOf(
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = "1234567",
                            personidentBarn = Personident(personidentBarn),
                            datoSøknad = LocalDate.parse("2024-01-01"),
                            stønadstype = Stønadstype.BIDRAG,
                        ),
                    ),
                ),
            )

        respons.beregningListe shouldHaveSize 0
    }

    @Test
    fun `skal returnere beregning med null samværsklasse hvis ingen matchende samvær finnes for vedtak`() {
        testdataManager.lagreSøknadListe(lageSøknadTestdata())
        testdataManager.lagrePeriodeBidragListe(
            listOf(
                opprettPeriodeBidrag(
                    saksnummer = "1234567",
                    barnId = personidentBarn,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                ),
            ),
        )

        val respons =
            bbmService.hentAlleBeregningerOgSamværForVedtak(
                BidragBeregningRequestDto(
                    listOf(
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = "1234567",
                            personidentBarn = Personident(personidentBarn),
                            datoSøknad = LocalDate.parse("2024-01-01"),
                            stønadstype = Stønadstype.BIDRAG,
                        ),
                    ),
                ),
            )

        assertSoftly {
            respons.beregningListe.size shouldBe 1
            respons.beregningListe[0].periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null)
            respons.beregningListe[0].samværsklasse shouldBe null
        }
    }

    @Test
    fun `skal returnere beregning med korrekt samværsklasse hvis matchende samvær finnes for vedtak`() {
        testdataManager.lagreSøknadListe(lageSøknadTestdata())
        testdataManager.lagrePeriodeBidragListe(
            listOf(
                opprettPeriodeBidrag(
                    saksnummer = "1234567",
                    barnId = personidentBarn,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                ),
            ),
        )
        testdataManager.lagreSamværListe(
            listOf(
                opprettSamvær(
                    saksnummer = "1234567",
                    barnId = personidentBarn,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_2,
                ),
            ),
        )

        val respons =
            bbmService.hentAlleBeregningerOgSamværForVedtak(
                BidragBeregningRequestDto(
                    listOf(
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = "1234567",
                            personidentBarn = Personident(personidentBarn),
                            datoSøknad = LocalDate.parse("2024-01-01"),
                            stønadstype = Stønadstype.BIDRAG,
                        ),
                    ),
                ),
            )

        assertSoftly {
            respons.beregningListe.size shouldBe 1
            respons.beregningListe[0].periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null)
            respons.beregningListe[0].samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_2
        }
    }

    @Test
    fun `skal håndtere flere perioder med ulik samværsklasse for vedtak`() {
        testdataManager.lagreSøknadListe(lageSøknadTestdata())
        testdataManager.lagrePeriodeBidragListe(
            listOf(
                opprettPeriodeBidragKomplett(
                    saksnummer = "1234567",
                    barnId = personidentBarn,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    beregnetBeløp = BigDecimal.valueOf(1700.01),
                    faktiskBeløp = BigDecimal.valueOf(1500.01),
                    beløpSamvær = BigDecimal.valueOf(100.01),
                ),
                opprettPeriodeBidragKomplett(
                    saksnummer = "1234567",
                    barnId = personidentBarn,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-08-01"),
                    beregnetBeløp = BigDecimal.valueOf(2000.01),
                    faktiskBeløp = BigDecimal.valueOf(1900.01),
                    beløpSamvær = BigDecimal.valueOf(200.01),
                ),
                opprettPeriodeBidragKomplett(
                    saksnummer = "1234567",
                    barnId = personidentBarn,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-09-01"),
                    beregnetBeløp = BigDecimal.valueOf(2200.01),
                    faktiskBeløp = BigDecimal.valueOf(2100.01),
                    beløpSamvær = BigDecimal.valueOf(200.01),
                ),
                // annen periode for barnet, skal ikke plukkes med
                opprettPeriodeBidragKomplett(
                    saksnummer = "1234567",
                    barnId = "99887766554",
                    datoSøknad = LocalDate.parse("2024-01-02"),
                    datoFom = LocalDate.parse("2024-08-01"),
                    beregnetBeløp = BigDecimal.valueOf(5500),
                    faktiskBeløp = BigDecimal.valueOf(150),
                    beløpSamvær = BigDecimal.valueOf(350),
                ),
                // periode for annet barn
                opprettPeriodeBidragKomplett(
                    saksnummer = "1234567",
                    barnId = "99887766554",
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-08-01"),
                    beregnetBeløp = BigDecimal.valueOf(4400),
                    faktiskBeløp = BigDecimal.valueOf(120),
                    beløpSamvær = BigDecimal.valueOf(300),
                ),
            ),
        )
        testdataManager.lagreSamværListe(
            listOf(
                opprettSamværKomplett(
                    saksnummer = "1234567",
                    barnId = personidentBarn,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    datoTom = LocalDate.parse("2024-07-31"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_1,
                ),
                opprettSamværKomplett(
                    saksnummer = "1234567",
                    barnId = personidentBarn,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-08-01"),
                    datoTom = null,
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_2,
                ),
                // periode for annet barn
                opprettSamværKomplett(
                    saksnummer = "1234567",
                    barnId = "99887766554",
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-10-01"),
                    datoTom = null,
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_3,
                ),
            ),
        )

        val respons =
            bbmService.hentAlleBeregningerOgSamværForVedtak(
                BidragBeregningRequestDto(
                    listOf(
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = "1234567",
                            personidentBarn = Personident(personidentBarn),
                            datoSøknad = LocalDate.parse("2024-01-01"),
                            stønadstype = Stønadstype.BIDRAG,
                        ),
                    ),
                ),
            )

        respons.beregningListe shouldHaveSize 3
        assertSoftly(respons.beregningListe[0]) {
            periode shouldBe ÅrMånedsperiode(YearMonth.parse("2024-07"), YearMonth.parse("2024-08"))
            this.personidentBarn shouldBe Personident(this@BBMServiceTest.personidentBarn)
            beregnetBeløp shouldBe BigDecimal.valueOf(1700.01)
            faktiskBeløp shouldBe BigDecimal.valueOf(1500.01)
            beløpSamvær shouldBe BigDecimal.valueOf(100.01)
            samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_1
        }
        assertSoftly(respons.beregningListe[1]) {
            periode shouldBe ÅrMånedsperiode(YearMonth.parse("2024-08"), YearMonth.parse("2024-09"))
            this.personidentBarn shouldBe Personident(this@BBMServiceTest.personidentBarn)
            beregnetBeløp shouldBe BigDecimal.valueOf(2000.01)
            faktiskBeløp shouldBe BigDecimal.valueOf(1900.01)
            beløpSamvær shouldBe BigDecimal.valueOf(200.01)
            samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_2
        }
        assertSoftly(respons.beregningListe[2]) {
            periode shouldBe ÅrMånedsperiode(YearMonth.parse("2024-09"), null)
            this.personidentBarn shouldBe Personident(this@BBMServiceTest.personidentBarn)
            beregnetBeløp shouldBe BigDecimal.valueOf(2200.01)
            faktiskBeløp shouldBe BigDecimal.valueOf(2100.01)
            beløpSamvær shouldBe BigDecimal.valueOf(200.01)
            samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_2
        }
    }
}

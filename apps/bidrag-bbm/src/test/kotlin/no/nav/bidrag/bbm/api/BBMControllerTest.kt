package no.nav.bidrag.bbm.api

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.bidrag.bbm.CommonTestRunner
import no.nav.bidrag.bbm.model.BidragBeregningSaksnummerRequest
import no.nav.bidrag.bbm.utils.DATO_SØKNAD_SØKNAD_1
import no.nav.bidrag.bbm.utils.DATO_SØKNAD_SØKNAD_2
import no.nav.bidrag.bbm.utils.PERSONIDENT_BARN_1
import no.nav.bidrag.bbm.utils.PERSONIDENT_BARN_2
import no.nav.bidrag.bbm.utils.PERSONIDENT_BARN_4
import no.nav.bidrag.bbm.utils.PERSONIDENT_BM_1
import no.nav.bidrag.bbm.utils.PERSONIDENT_BM_2
import no.nav.bidrag.bbm.utils.PERSONIDENT_BP_1
import no.nav.bidrag.bbm.utils.SAKSNUMMER_1
import no.nav.bidrag.bbm.utils.SAKSNUMMER_2
import no.nav.bidrag.bbm.utils.SAKSNUMMER_3
import no.nav.bidrag.bbm.utils.lageSøknadTestdata
import no.nav.bidrag.bbm.utils.opprettBlankett
import no.nav.bidrag.bbm.utils.opprettKodeSøknadStatus
import no.nav.bidrag.bbm.utils.opprettPeriodeBidrag
import no.nav.bidrag.bbm.utils.opprettPeriodeBidragKomplett
import no.nav.bidrag.bbm.utils.opprettRolle
import no.nav.bidrag.bbm.utils.opprettSamvær
import no.nav.bidrag.bbm.utils.opprettSamværKomplett
import no.nav.bidrag.bbm.utils.opprettSøknad
import no.nav.bidrag.bbm.utils.opprettSøknadslinje
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.BidragBeregningRequestDto
import no.nav.bidrag.transport.behandling.beregning.felles.BidragBeregningResponsDto
import no.nav.bidrag.transport.behandling.beregning.felles.HentBPsÅpneSøknaderRequest
import no.nav.bidrag.transport.behandling.beregning.felles.HentBPsÅpneSøknaderResponse
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.collections.get

class BBMControllerTest : CommonTestRunner() {
    @Test
    fun `Skal returnere tom liste hvis ikke funnet for dato søknad`() {
        val httpEntity =
            HttpEntity(
                BidragBeregningRequestDto(
                    listOf(
                        BidragBeregningRequestDto.HentBidragBeregning(
                            SAKSNUMMER_1,
                            Personident(PERSONIDENT_BARN_1),
                            datoSøknad = LocalDate.parse("2024-01-02"),
                            null,
                            Stønadstype.BIDRAG,
                        ),
                    ),
                ),
            )

        val response =
            httpHeaderTestRestTemplate.postForEntity(
                "$rootUri/api/beregning",
                httpEntity,
                BidragBeregningResponsDto::class.java,
            )

        response.body shouldNotBe null
        response.body!!.beregningListe.shouldBeEmpty()
    }

    @Test
    fun `Skal returnere tom liste hvis ikke funnet for motttat dato i søknad`() {
        val søknader = testdataManager.lagreSøknadListe(lageSøknadTestdata())
        val søknadsid1 = søknader.first { it.saksnummer == SAKSNUMMER_1 }.søknadsid!!.toString()
        val httpEntity =
            HttpEntity(
                BidragBeregningRequestDto(
                    listOf(
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = SAKSNUMMER_1,
                            personidentBarn = Personident(PERSONIDENT_BARN_1),
                            søknadsid = søknadsid1,
                            stønadstype = Stønadstype.BIDRAG,
                        ),
                    ),
                ),
            )

        testdataManager.lagreSøknadListe(
            listOf(opprettSøknad(søknadsid = søknadsid1.toLong(), blankettid = 1L, søknadMottattDato = LocalDate.parse("2024-01-02"))),
        )

        val response =
            httpHeaderTestRestTemplate.postForEntity(
                "$rootUri/api/beregning",
                httpEntity,
                BidragBeregningResponsDto::class.java,
            )

        response.body shouldNotBe null
        response.body!!.beregningListe.shouldBeEmpty()
    }

    @Test
    fun `Skal hente nyeste bidrag beregning uten samvær med søknadsid`() {
        val søknader = testdataManager.lagreSøknadListe(lageSøknadTestdata())
        val søknadsid1 = søknader.first { it.saksnummer == SAKSNUMMER_1 }.søknadsid!!.toString()
        val httpEntity =
            HttpEntity(
                BidragBeregningRequestDto(
                    listOf(
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = SAKSNUMMER_1,
                            personidentBarn = Personident(PERSONIDENT_BARN_2),
                            søknadsid = søknadsid1,
                            stønadstype = Stønadstype.BIDRAG,
                        ),
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = SAKSNUMMER_2,
                            personidentBarn = Personident(PERSONIDENT_BARN_2),
                            søknadsid = søknadsid1,
                            stønadstype = Stønadstype.BIDRAG,
                        ),
                    ),
                ),
            )

        testdataManager.lagrePeriodeBidragListe(
            listOf(
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoFom = LocalDate.parse("2024-07-01"),
                ),
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-05-02"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    faktiskBeløp = BigDecimal(1000),
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

        val response =
            httpHeaderTestRestTemplate.postForEntity(
                "$rootUri/api/beregning",
                httpEntity,
                BidragBeregningResponsDto::class.java,
            )

        response.body shouldNotBe null
        response.body!!.beregningListe.shouldHaveSize(2)
        assertSoftly(response.body!!.beregningListe[0]) {
            periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null)
            saksnummer shouldBe SAKSNUMMER_1
            personidentBarn shouldBe Personident(PERSONIDENT_BARN_2)
            beregnetBeløp shouldBe BigDecimal("10024.00")
            faktiskBeløp shouldBe BigDecimal("1030.00")
            beløpSamvær shouldBe BigDecimal("100.00")
            samværsklasse shouldBe null
        }
        assertSoftly(response.body!!.beregningListe[1]) {
            periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null)
            saksnummer shouldBe SAKSNUMMER_2
            personidentBarn shouldBe Personident(PERSONIDENT_BARN_2)
            beregnetBeløp shouldBe BigDecimal("10024.00")
            faktiskBeløp shouldBe BigDecimal("1030.00")
            beløpSamvær shouldBe BigDecimal("100.00")
            samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_1
        }
    }

    @Test
    fun `Skal hente bidrag beregning med samværsklasse`() {
        val søknader = testdataManager.lagreSøknadListe(lageSøknadTestdata())
        val søknadsid1 = søknader.first { it.saksnummer == SAKSNUMMER_1 }.søknadsid!!.toString()
        val httpEntity =
            HttpEntity(
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
            )

        testdataManager.lagrePeriodeBidragListe(
            listOf(
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoFom = LocalDate.parse("2024-07-01"),
                ),
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-05-02"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    faktiskBeløp = BigDecimal(1000),
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
                    datoFom = LocalDate.parse("2024-07-01"),
                ),
                opprettSamvær(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-05-02"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_2,
                ),
                opprettSamvær(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-04-02"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_3,
                ),
            ),
        )

        val response =
            httpHeaderTestRestTemplate.postForEntity(
                "$rootUri/api/beregning",
                httpEntity,
                BidragBeregningResponsDto::class.java,
            )

        response.body shouldNotBe null
        response.body!!.beregningListe.shouldHaveSize(1)
        assertSoftly(response.body!!.beregningListe[0]) {
            periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null)
            saksnummer shouldBe SAKSNUMMER_1
            personidentBarn shouldBe Personident(PERSONIDENT_BARN_2)
            beregnetBeløp shouldBe BigDecimal("10024.00")
            faktiskBeløp shouldBe BigDecimal("1030.00")
            beløpSamvær shouldBe BigDecimal("100.00")
            samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_1
        }
    }

    @Test
    fun `Skal hente bidrag beregning for flere barn`() {
        val søknader = testdataManager.lagreSøknadListe(lageSøknadTestdata())
        val søknadsid1 = søknader.first { it.saksnummer == SAKSNUMMER_1 }.søknadsid!!.toString()
        val søknadsid2 = søknader.first { it.saksnummer == SAKSNUMMER_2 }.søknadsid!!.toString()
        val httpEntity =
            HttpEntity(
                BidragBeregningRequestDto(
                    listOf(
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = SAKSNUMMER_1,
                            personidentBarn = Personident(PERSONIDENT_BARN_1),
                            søknadsid = søknadsid1,
                            stønadstype = Stønadstype.BIDRAG,
                        ),
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = SAKSNUMMER_2,
                            personidentBarn = Personident(PERSONIDENT_BARN_2),
                            søknadsid = søknadsid2,
                            stønadstype = Stønadstype.BIDRAG,
                        ),
                    ),
                ),
            )

        val perioderBarn1 =
            listOf(
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_1,
                    datoSøknad = DATO_SØKNAD_SØKNAD_1,
                    datoFom = LocalDate.parse("2024-07-01"),
                ),
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_1,
                    datoSøknad = DATO_SØKNAD_SØKNAD_1,
                    datoFom = LocalDate.parse("2024-10-01"),
                    faktiskBeløp = BigDecimal(1000),
                ),
            )
        val samværBarn1 =
            listOf(
                opprettSamvær(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_1,
                    datoSøknad = DATO_SØKNAD_SØKNAD_1,
                    datoFom = LocalDate.parse("2024-07-01"),
                ),
                opprettSamvær(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_1,
                    datoSøknad = LocalDate.parse("2024-05-02"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_2,
                ),
            )
        val perioderBarn2 =
            listOf(
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_2,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = DATO_SØKNAD_SØKNAD_2,
                    datoFom = LocalDate.parse("2024-07-01"),
                ),
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_2,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = DATO_SØKNAD_SØKNAD_2,
                    datoFom = LocalDate.parse("2024-08-01"),
                    faktiskBeløp = BigDecimal(1000),
                ),
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_2,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-06-02"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    faktiskBeløp = BigDecimal(1040),
                ),
            )

        val samværBarn2 =
            listOf(
                opprettSamvær(
                    saksnummer = SAKSNUMMER_2,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = DATO_SØKNAD_SØKNAD_2,
                    datoFom = LocalDate.parse("2024-07-01"),
                ),
                opprettSamvær(
                    saksnummer = SAKSNUMMER_2,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-08-02"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_2,
                ),
                opprettSamvær(
                    saksnummer = SAKSNUMMER_2,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-04-02"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_3,
                ),
            )
        testdataManager.lagreSamværListe(
            samværBarn1 + samværBarn2,
        )
        testdataManager.lagrePeriodeBidragListe(
            perioderBarn1 + perioderBarn2,
        )

        val response =
            httpHeaderTestRestTemplate.postForEntity(
                "$rootUri/api/beregning",
                httpEntity,
                BidragBeregningResponsDto::class.java,
            )

        response.body shouldNotBe null
        response.body!!.beregningListe.shouldHaveSize(2)
        assertSoftly(response.body!!.beregningListe[0]) {
            periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-10-01"), null)
            saksnummer shouldBe SAKSNUMMER_1
            personidentBarn shouldBe Personident(PERSONIDENT_BARN_1)
            datoSøknad shouldBe DATO_SØKNAD_SØKNAD_1
            beregnetBeløp shouldBe BigDecimal("10024.00")
            faktiskBeløp shouldBe BigDecimal("1000.00")
            beløpSamvær shouldBe BigDecimal("100.00")
            samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_1
        }
        assertSoftly(response.body!!.beregningListe[1]) {
            periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-08-01"), null)
            saksnummer shouldBe SAKSNUMMER_2
            personidentBarn shouldBe Personident(PERSONIDENT_BARN_2)
            datoSøknad shouldBe DATO_SØKNAD_SØKNAD_2
            beregnetBeløp shouldBe BigDecimal("10024.00")
            faktiskBeløp shouldBe BigDecimal("1000.00")
            beløpSamvær shouldBe BigDecimal("100.00")
            samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_1
        }
    }

    @Test
    fun `skal ikke hente bidrag for søknadstype hvis ikke finnes`() {
        val søknader = testdataManager.lagreSøknadListe(lageSøknadTestdata())
        val søknadsid1 = søknader.first { it.saksnummer == SAKSNUMMER_1 }.søknadsid!!.toString()
        testdataManager.lagrePeriodeBidragListe(
            listOf(
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = DATO_SØKNAD_SØKNAD_1,
                    datoFom = LocalDate.parse("2024-07-01"),
                    søknadstype = Stønadstype.BIDRAG18AAR,
                ),
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-05-02"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    faktiskBeløp = BigDecimal(1000),
                    søknadstype = Stønadstype.BIDRAG18AAR,
                ),
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-06-02"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    faktiskBeløp = BigDecimal(1040),
                    søknadstype = Stønadstype.BIDRAG18AAR,
                ),
            ),
        )

        httpHeaderTestRestTemplate
            .postForEntity(
                "$rootUri/api/beregning",
                HttpEntity(
                    BidragBeregningRequestDto(
                        listOf(
                            BidragBeregningRequestDto.HentBidragBeregning(
                                saksnummer = SAKSNUMMER_3,
                                personidentBarn = Personident(PERSONIDENT_BARN_2),
                                søknadsid = søknadsid1,
                                stønadstype = Stønadstype.BIDRAG,
                            ),
                        ),
                    ),
                ),
                BidragBeregningResponsDto::class.java,
            ).body!!
            .beregningListe shouldHaveSize 0

        httpHeaderTestRestTemplate
            .postForEntity(
                "$rootUri/api/beregning",
                HttpEntity(
                    BidragBeregningRequestDto(
                        listOf(
                            BidragBeregningRequestDto.HentBidragBeregning(
                                saksnummer = SAKSNUMMER_3,
                                personidentBarn = Personident(PERSONIDENT_BARN_2),
                                søknadsid = søknadsid1,
                                stønadstype = Stønadstype.BIDRAG18AAR,
                            ),
                        ),
                    ),
                ),
                BidragBeregningResponsDto::class.java,
            ).body!!
            .beregningListe shouldHaveSize 1
    }

    @Test
    fun `Skal matche samvær og perioder`() {
        val httpEntity =
            HttpEntity(
                BidragBeregningSaksnummerRequest(
                    listOf(
                        BidragBeregningSaksnummerRequest.Saksnummer(SAKSNUMMER_3),
                    ),
                ),
            )

        testdataManager.lagrePeriodeBidragListe(
            listOf(
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_1,
                    datoSøknad = LocalDate.parse("2024-04-02"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    faktiskBeløp = BigDecimal(1010),
                ),
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-04-02"),
                    datoFom = LocalDate.parse("2024-08-01"),
                    faktiskBeløp = BigDecimal(1020),
                ),
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-04-02"),
                    datoFom = LocalDate.parse("2024-09-01"),
                    faktiskBeløp = BigDecimal(1030),
                ),
            ),
        )
        testdataManager.lagreSamværListe(
            listOf(
                opprettSamvær(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_1,
                    datoSøknad = LocalDate.parse("2024-04-02"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_1,
                ),
                opprettSamvær(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-04-02"),
                    datoFom = LocalDate.parse("2024-08-01"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_2,
                ),
                opprettSamvær(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-04-02"),
                    datoFom = LocalDate.parse("2024-09-01"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_3,
                ),
            ),
        )

        val response =
            httpHeaderTestRestTemplate.postForEntity(
                "$rootUri/api/beregning/saksnummer",
                httpEntity,
                BidragBeregningResponsDto::class.java,
            )

        response.body shouldNotBe null
        response.body!!.beregningListe.shouldHaveSize(3)
        response.body!!.beregningListe[0].periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null)
        response.body!!.beregningListe[0].faktiskBeløp shouldBe BigDecimal("1010.00")
        response.body!!.beregningListe[0].personidentBarn shouldBe Personident(PERSONIDENT_BARN_1)
        response.body!!.beregningListe[0].datoSøknad shouldBe LocalDate.parse("2024-04-02")
        response.body!!.beregningListe[0].samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_1

        response.body!!.beregningListe[1].periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-08-01"), null)
        response.body!!.beregningListe[1].faktiskBeløp shouldBe BigDecimal("1020.00")
        response.body!!.beregningListe[1].personidentBarn shouldBe Personident(PERSONIDENT_BARN_2)
        response.body!!.beregningListe[1].datoSøknad shouldBe LocalDate.parse("2024-04-02")
        response.body!!.beregningListe[1].samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_2

        response.body!!.beregningListe[2].periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-09-01"), null)
        response.body!!.beregningListe[2].faktiskBeløp shouldBe BigDecimal("1030.00")
        response.body!!.beregningListe[2].personidentBarn shouldBe Personident(PERSONIDENT_BARN_2)
        response.body!!.beregningListe[2].datoSøknad shouldBe LocalDate.parse("2024-04-02")
        response.body!!.beregningListe[2].samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_3
    }

    @Test
    fun `Skal finne alle samvær og perioder`() {
        val httpEntity =
            HttpEntity(
                BidragBeregningSaksnummerRequest(
                    listOf(
                        BidragBeregningSaksnummerRequest.Saksnummer(SAKSNUMMER_3),
                    ),
                ),
            )

        testdataManager.lagrePeriodeBidragListe(
            listOf(
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_1,
                    datoSøknad = LocalDate.parse("2024-04-02"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    faktiskBeløp = BigDecimal(1010),
                ),
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-05-02"),
                    datoFom = LocalDate.parse("2024-08-01"),
                    faktiskBeløp = BigDecimal(1020),
                ),
                opprettPeriodeBidrag(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-06-02"),
                    datoFom = LocalDate.parse("2024-09-01"),
                    faktiskBeløp = BigDecimal(1030),
                ),
            ),
        )
        testdataManager.lagreSamværListe(
            listOf(
                opprettSamvær(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_1,
                    datoSøknad = LocalDate.parse("2024-04-02"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_1,
                ),
                opprettSamvær(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-05-02"),
                    datoFom = LocalDate.parse("2024-08-01"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_2,
                ),
                opprettSamvær(
                    saksnummer = SAKSNUMMER_3,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-06-02"),
                    datoFom = LocalDate.parse("2024-09-01"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_3,
                ),
            ),
        )

        val response =
            httpHeaderTestRestTemplate.postForEntity(
                "$rootUri/api/beregning/saksnummer",
                httpEntity,
                BidragBeregningResponsDto::class.java,
            )

        response.body shouldNotBe null
        response.body!!.beregningListe.shouldHaveSize(3)
        response.body!!.beregningListe[0].datoSøknad shouldBe LocalDate.parse("2024-04-02")
        response.body!!.beregningListe[0].periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null)
        response.body!!.beregningListe[0].faktiskBeløp shouldBe BigDecimal("1010.00")
        response.body!!.beregningListe[0].personidentBarn shouldBe Personident(PERSONIDENT_BARN_1)
        response.body!!.beregningListe[0].samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_1

        response.body!!.beregningListe[1].datoSøknad shouldBe LocalDate.parse("2024-05-02")
        response.body!!.beregningListe[1].periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-08-01"), null)
        response.body!!.beregningListe[1].faktiskBeløp shouldBe BigDecimal("1020.00")
        response.body!!.beregningListe[1].personidentBarn shouldBe Personident(PERSONIDENT_BARN_2)
        response.body!!.beregningListe[1].samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_2

        response.body!!.beregningListe[2].datoSøknad shouldBe LocalDate.parse("2024-06-02")
        response.body!!.beregningListe[2].periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-09-01"), null)
        response.body!!.beregningListe[2].faktiskBeløp shouldBe BigDecimal("1030.00")
        response.body!!.beregningListe[2].personidentBarn shouldBe Personident(PERSONIDENT_BARN_2)
        response.body!!.beregningListe[2].samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_3
    }

    @Test
    fun `Skal returnere tom liste hvis det ikke finnes åpne søknader for BP`() {
        val httpEntity =
            HttpEntity(
                HentBPsÅpneSøknaderRequest(PERSONIDENT_BP_1),
            )

        val response =
            httpHeaderTestRestTemplate.exchange(
                "$rootUri/api/beregning/apnesoknader",
                HttpMethod.POST,
                httpEntity,
                object : ParameterizedTypeReference<HentBPsÅpneSøknaderResponse>() {},
            )

        response.body shouldNotBe null
        response.body!!.åpneSøknader.shouldBeEmpty()
    }

    @Test
    fun `Skal finne åpne søknader om bidrag for BP`() {
        val httpEntity =
            HttpEntity(
                HentBPsÅpneSøknaderRequest(PERSONIDENT_BP_1),
            )

        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(
                    kode = "UB",
                    lukketStatus = "0",
                ),
                opprettKodeSøknadStatus(
                    kode = "VF",
                    lukketStatus = "1",
                ),
                opprettKodeSøknadStatus(
                    kode = "TR",
                    lukketStatus = "1",
                ),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(
                        saksnummer = SAKSNUMMER_1,
                        fnr = PERSONIDENT_BP_1,
                        rolletype = "BP",
                    ),
                    opprettRolle(
                        saksnummer = SAKSNUMMER_1,
                        fnr = PERSONIDENT_BM_1,
                        rolletype = "BM",
                    ),
                    opprettRolle(
                        saksnummer = SAKSNUMMER_1,
                        fnr = PERSONIDENT_BARN_1,
                        rolletype = "BA",
                    ),
                    opprettRolle(
                        saksnummer = SAKSNUMMER_2,
                        fnr = PERSONIDENT_BP_1,
                        rolletype = "BP",
                    ),
                    opprettRolle(
                        saksnummer = SAKSNUMMER_2,
                        fnr = PERSONIDENT_BM_2,
                        rolletype = "BM",
                    ),
                    opprettRolle(
                        saksnummer = SAKSNUMMER_2,
                        fnr = PERSONIDENT_BARN_4,
                        rolletype = "BA",
                    ),
                ),
            )

        val blanketter =
            testdataManager.lagreBlankettListe(
                listOf(
                    opprettBlankett(
                        saksnummer = SAKSNUMMER_1,
                        søknadFraKode = "MO",
                        søknadstype = "FA",
                    ),
                    opprettBlankett(
                        saksnummer = SAKSNUMMER_2,
                        søknadFraKode = "MO",
                        søknadstype = "FA",
                    ),
                ),
            )

        val søknader =
            testdataManager.lagreSøknadListe(
                listOf(
                    opprettSøknad(
                        blankettid = blanketter[0].blankettid!!,
                        saksnummer = SAKSNUMMER_1,
                        søknadMottattDato = LocalDate.parse("2024-01-02"),
                        søknadsgruppekode = "BI",
                        behandlingsid = "1",
                    ),
                    opprettSøknad(
                        blankettid = blanketter[0].blankettid!!,
                        saksnummer = SAKSNUMMER_2,
                        søknadMottattDato = LocalDate.parse("2024-01-02"),
                        søknadsgruppekode = "BI",
                        behandlingsid = "2",
                    ),
                ),
            )

        val søknadslinjer =
            testdataManager.lagreSøknadslinjeListe(
                listOf(
                    opprettSøknadslinje(
                        søknadsid = søknader[0].søknadsid!!,
                        rolleid = roller[2].rolleid!!,
                        innbetaltBeløp = BigDecimal.valueOf(100),
                        søknadsstatuskode = "UB",
                        gruppeKombinasjonskode = "BI",
                        saksnummer = SAKSNUMMER_1,
                    ),
                    opprettSøknadslinje(
                        søknadsid = søknader[1].søknadsid!!,
                        rolleid = roller[3].rolleid!!,
                        innbetaltBeløp = BigDecimal.valueOf(250),
                        søknadsstatuskode = "UB",
                        gruppeKombinasjonskode = "BI",
                        saksnummer = SAKSNUMMER_1,
                    ),
                ),
            )

        val response =
            httpHeaderTestRestTemplate.exchange(
                "$rootUri/api/beregning/apnesoknader",
                HttpMethod.POST,
                httpEntity,
                object : ParameterizedTypeReference<HentBPsÅpneSøknaderResponse>() {},
            )

        response.body
            ?.åpneSøknader
            ?.size
            .shouldBe(2)
    }

    @Test
    fun `skal returnere tom liste når ingen data finnes for hentAlleBeregninger`() {
        val httpEntity =
            HttpEntity(
                BidragBeregningRequestDto(
                    listOf(
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = "9999999",
                            personidentBarn = Personident("99999999999"),
                            datoSøknad = LocalDate.parse("2024-01-01"),
                            stønadstype = Stønadstype.BIDRAG,
                        ),
                    ),
                ),
            )

        val response =
            httpHeaderTestRestTemplate.postForEntity(
                "$rootUri/api/beregning/alleberegningerogsamvar",
                httpEntity,
                BidragBeregningResponsDto::class.java,
            )

        response.body shouldNotBe null
        response.body!!.beregningListe.shouldBeEmpty()
    }

    @Test
    fun `skal returnere alle perioder med matchende samværsklasser`() {
        val søknader = testdataManager.lagreSøknadListe(lageSøknadTestdata())
        testdataManager.lagrePeriodeBidragListe(
            listOf(
                opprettPeriodeBidragKomplett(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    faktiskBeløp = BigDecimal(1010),
                ),
                opprettPeriodeBidragKomplett(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-08-01"),
                    faktiskBeløp = BigDecimal(1020),
                ),
                opprettPeriodeBidragKomplett(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-09-01"),
                    faktiskBeløp = BigDecimal(1030),
                ),
            ),
        )
        testdataManager.lagreSamværListe(
            listOf(
                opprettSamværKomplett(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    datoTom = LocalDate.parse("2024-07-31"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_1,
                ),
                opprettSamværKomplett(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-08-01"),
                    datoTom = LocalDate.parse("2024-08-31"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_2,
                ),
                opprettSamværKomplett(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-09-01"),
                    datoTom = LocalDate.parse("2024-09-30"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_3,
                ),
            ),
        )

        val httpEntity =
            HttpEntity(
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
            )

        val response =
            httpHeaderTestRestTemplate.postForEntity(
                "$rootUri/api/beregning/alleberegningerogsamvar",
                httpEntity,
                BidragBeregningResponsDto::class.java,
            )

        response.body shouldNotBe null
        response.body!!.beregningListe shouldHaveSize 3
        assertSoftly(response.body!!.beregningListe[0]) {
            periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-07-01"), LocalDate.parse("2024-08-01"))
            faktiskBeløp shouldBe BigDecimal("1010.00")
            samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_1
        }
        assertSoftly(response.body!!.beregningListe[1]) {
            periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-08-01"), LocalDate.parse("2024-09-01"))
            faktiskBeløp shouldBe BigDecimal("1020.00")
            samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_2
        }
        assertSoftly(response.body!!.beregningListe[2]) {
            periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-09-01"), null)
            faktiskBeløp shouldBe BigDecimal("1030.00")
            samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_3
        }
    }

    @Test
    fun `skal håndtere flere barn med ulike perioder`() {
        val søknader = testdataManager.lagreSøknadListe(lageSøknadTestdata())
        testdataManager.lagrePeriodeBidragListe(
            listOf(
                opprettPeriodeBidragKomplett(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_1,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    faktiskBeløp = BigDecimal(1010),
                ),
                opprettPeriodeBidragKomplett(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-08-01"),
                    faktiskBeløp = BigDecimal(1020),
                ),
            ),
        )
        testdataManager.lagreSamværListe(
            listOf(
                opprettSamværKomplett(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_1,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-07-01"),
                    datoTom = LocalDate.parse("2024-07-31"),
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_1,
                ),
                opprettSamværKomplett(
                    saksnummer = SAKSNUMMER_1,
                    barnId = PERSONIDENT_BARN_2,
                    datoSøknad = LocalDate.parse("2024-01-01"),
                    datoFom = LocalDate.parse("2024-08-01"),
                    datoTom = null,
                    samværskode = Samværsklasse.SAMVÆRSKLASSE_2,
                ),
            ),
        )

        val httpEntity =
            HttpEntity(
                BidragBeregningRequestDto(
                    listOf(
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = SAKSNUMMER_1,
                            personidentBarn = Personident(PERSONIDENT_BARN_1),
                            datoSøknad = LocalDate.parse("2024-01-01"),
                            stønadstype = Stønadstype.BIDRAG,
                        ),
                        BidragBeregningRequestDto.HentBidragBeregning(
                            saksnummer = SAKSNUMMER_1,
                            personidentBarn = Personident(PERSONIDENT_BARN_2),
                            datoSøknad = LocalDate.parse("2024-01-01"),
                            stønadstype = Stønadstype.BIDRAG,
                        ),
                    ),
                ),
            )

        val response =
            httpHeaderTestRestTemplate.postForEntity(
                "$rootUri/api/beregning/alleberegningerogsamvar",
                httpEntity,
                BidragBeregningResponsDto::class.java,
            )

        response.body shouldNotBe null
        response.body!!.beregningListe shouldHaveSize 2
        assertSoftly(response.body!!.beregningListe[0]) {
            periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null)
            personidentBarn shouldBe Personident(PERSONIDENT_BARN_1)
            faktiskBeløp shouldBe BigDecimal("1010.00")
            samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_1
        }
        assertSoftly(response.body!!.beregningListe[1]) {
            periode shouldBe ÅrMånedsperiode(LocalDate.parse("2024-08-01"), null)
            personidentBarn shouldBe Personident(PERSONIDENT_BARN_2)
            faktiskBeløp shouldBe BigDecimal("1020.00")
            samværsklasse shouldBe Samværsklasse.SAMVÆRSKLASSE_2
        }
    }
}

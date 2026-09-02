package no.nav.bidrag.sak.controller

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Konvensjon
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.domene.land.Landkode
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.sak.SpringTestRunner
import no.nav.bidrag.sak.config.BidragOrganisasjonTestConfig
import no.nav.bidrag.sak.domain.BidragssakTest.Companion.createBidragssak
import no.nav.bidrag.sak.domain.BidragssakTest.Companion.createRolle
import no.nav.bidrag.sak.dto.NySakCommandDto
import no.nav.bidrag.sak.dto.NySakResponseDto
import no.nav.bidrag.sak.integration.BidragBBMConsumer
import no.nav.bidrag.sak.integration.kodeverk.CachedKodeverkService
import no.nav.bidrag.sak.repository.BidragssakRepository
import no.nav.bidrag.sak.repository.HendelseRepository
import no.nav.bidrag.sak.repository.RolleRepository
import no.nav.bidrag.sak.repository.VedtakOverføringRepository
import no.nav.bidrag.sak.repository.findByIdOrThrow
import no.nav.bidrag.sak.service.ArbeidsfordelingService
import no.nav.bidrag.sak.service.BidragSakService
import no.nav.bidrag.sak.service.HendelseService
import no.nav.bidrag.sak.service.RolleService
import no.nav.bidrag.sak.service.RollehistorikkService
import no.nav.bidrag.sak.service.SaksnummerSerie.hentMinimumsgrenseForAarstall
import no.nav.bidrag.sak.service.Tilgangskontroll
import no.nav.bidrag.sak.util.FnrGenerator
import no.nav.bidrag.sak.validering.OpprettSakValidator
import no.nav.bidrag.transport.sak.OppdaterSakRequest
import no.nav.bidrag.transport.sak.OppdaterSakResponse
import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.OpprettSakResponse
import no.nav.bidrag.transport.sak.RolleDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
internal class BidragSakControllerIT : SpringTestRunner() {
    @Autowired
    private lateinit var tilgangClientMock: Tilgangskontroll

    @Autowired
    private lateinit var bidragssakRepository: BidragssakRepository

    @Autowired
    private lateinit var bidragssakService: BidragSakService
    private val opprettSakValidator: OpprettSakValidator = mockk()
    private val bbmConsumerMock: BidragBBMConsumer = mockk()

    private fun makeFullContextPath(): String = "http://localhost:$port"

    private fun fullUrlForNySak(): String = UriComponentsBuilder
        .fromUriString("${makeFullContextPath()}/bidrag-sak${BidragSakController.SAK_NY}")
        .toUriString()

    private fun fullUrlForSokSak(): String = UriComponentsBuilder
        .fromUriString("${makeFullContextPath()}/bidrag-sak${BidragSakController.SAK_SOK}/1234567")
        .toUriString()

    @BeforeEach
    fun setUp() {
        every { identConsumer.hentAlleIdenter(any()) }.answers { listOf(firstArg()) }
        every { identConsumer.hentPersonInformasjon(any()) }.answers { null }
        MDC.put("callId", "NavCallId")
    }

    @Nested
    inner class FinnFor {
        private lateinit var saksnummer: Saksnummer

        val fnrBM = Personident(FnrGenerator.generer(2006, 4, 17))
        val fnrBP = Personident(FnrGenerator.generer(2007, 5, 18))

        @BeforeEach
        fun setUp() {
            saksnummer =
                bidragssakService
                    .opprettSak(
                        OpprettSakRequest(
                            Enhetsnummer("1701"),
                            roller =
                            setOf(
                                RolleDto(fnrBM, Rolletype.BIDRAGSMOTTAKER),
                                RolleDto(fnrBP, Rolletype.BIDRAGSPLIKTIG),
                            ),
                        ),
                    ).saksnummer
        }

        private fun fullUrlForSøkPåFødselsnummer(): String = UriComponentsBuilder
            .fromUriString("${makeFullContextPath()}/bidrag-sak${BidragSakController.PERSON_SAK}/${fnrBM.verdi}")
            .toUriString()

        private fun fullUrlForSøkMedPost(): String = UriComponentsBuilder
            .fromUriString("${makeFullContextPath()}${BidragSakController.PERSON_SAK}")
            .toUriString()

        @Test
        fun finnForFødselsnummerReturnereRiktigSak() {
            val respons =
                testRestTemplate.exchange(
                    fullUrlForSøkPåFødselsnummer(),
                    HttpMethod.GET,
                    null,
                    Any::class.java,
                )
            respons.statusCode.is2xxSuccessful shouldBe true
        }

        @Test
        fun finnForPostFødselsnummerReturnereRiktigSak() {
            val respons =
                testRestTemplate.postForEntity(
                    fullUrlForSøkMedPost(),
                    initHttpEntity<Any>(fnrBM),
                    Any::class.java,
                )
            respons.statusCode.is2xxSuccessful shouldBe true
        }
    }

    @Nested
    inner class OppdaterSak {
        private lateinit var saksnummer: Saksnummer

        val fnrBM = FnrGenerator.generer(2006, 4, 17)
        val fnrBP = FnrGenerator.generer(2007, 5, 18)
        val fnrBA1 = FnrGenerator.generer(2008, 6, 19)
        val fnrBA2 = FnrGenerator.generer(2009, 7, 20)
        val fnrBA3 = FnrGenerator.generer(2010, 8, 21)

        @BeforeEach
        fun setUp() {
            MDC.put("callId", "NavCallId")
            saksnummer =
                bidragssakService
                    .opprettSak(
                        OpprettSakRequest(
                            Enhetsnummer("1701"),
                            roller =
                            setOf(
                                RolleDto(Personident(fnrBM), Rolletype.BIDRAGSMOTTAKER),
                                RolleDto(Personident(fnrBP), Rolletype.BIDRAGSPLIKTIG),
                                RolleDto(Personident(fnrBA1), Rolletype.BARN, reellMottager = ReellMottaker(fnrBA1)),
                                RolleDto(Personident(fnrBA2), Rolletype.BARN, reellMottager = ReellMottaker(fnrBA2)),
                                RolleDto(Personident(fnrBA3), Rolletype.BARN),
                            ),
                        ),
                    ).saksnummer
        }

        @Test
        fun `skal oppdatere bidragssak med alle felter fra dto`() {
            testRestTemplate.postForEntity(
                urlOppdaterSak(),
                initHttpEntity(
                    OppdaterSakRequest(
                        saksnummer = saksnummer,
                        status = Bidragssakstatus.AK,
                        ansatt = true,
                        inhabilitet = true,
                        levdeAdskilt = true,
                        sanertDato = LocalDate.now(),
                        arbeidsfordeling = Arbeidsfordeling.BARNEBORTFØRING,
                        kategorikode = Sakskategori.UTLAND,
                        landkode = Landkode("SWE"),
                        konvensjonskode = Konvensjon.H73,
                        konvensjonsdato = LocalDate.now(),
                        ffuReferansenr = "Foo",
                    ),
                ),
                OppdaterSakResponse::class.java,
            )

            val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)
            oppdatertSak.status shouldBe Bidragssakstatus.AK
            oppdatertSak.ansatt shouldBe true
            oppdatertSak.inhabilitet shouldBe true
            oppdatertSak.levdeAdskilt shouldBe true
            oppdatertSak.sanertDato shouldBe LocalDate.now()
            oppdatertSak.arbeidsfordeling shouldBe Arbeidsfordeling.BARNEBORTFØRING
            oppdatertSak.kategori shouldBe Sakskategori.UTLAND
            oppdatertSak.land shouldBe "SWE"
            oppdatertSak.konvensjon shouldBe Konvensjon.H73
            oppdatertSak.konvensjonsdato shouldBe LocalDate.now()
            oppdatertSak.ffuReferansenr shouldBe "Foo"
            oppdatertSak.roller shouldHaveSize 7
        }

        @Test
        fun `skal lagre nytt barn med reell mottager`() {
            val nyttBarnFnr = FnrGenerator.generer()
            val rmFnr = FnrGenerator.generer()
            testRestTemplate.postForEntity(
                urlOppdaterSak(),
                initHttpEntity(
                    OppdaterSakRequest(
                        saksnummer = saksnummer,
                        roller = setOf(RolleDto(Personident(nyttBarnFnr), Rolletype.BARN, reellMottager = ReellMottaker(rmFnr))),
                    ),
                ),
                OppdaterSakResponse::class.java,
            )

            val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)
            oppdatertSak.roller shouldHaveSize 9
            oppdatertSak.roller
                .first { it.fødselsnummer == fnrBA1 && it.rolleType == Rolletype.BARN }
                .rolleId shouldBe
                oppdatertSak.roller.first { it.fødselsnummer == fnrBA1 && it.rolleType == Rolletype.REELMOTTAKER }.rolleId
            oppdatertSak.roller
                .first { it.fødselsnummer == fnrBA2 && it.rolleType == Rolletype.BARN }
                .rolleId shouldBe
                oppdatertSak.roller.first { it.fødselsnummer == fnrBA2 && it.rolleType == Rolletype.REELMOTTAKER }.rolleId
            oppdatertSak.roller
                .first { it.fødselsnummer == nyttBarnFnr && it.rolleType == Rolletype.BARN }
                .rolleId shouldBe
                oppdatertSak.roller.first { it.fødselsnummer == rmFnr && it.rolleType == Rolletype.REELMOTTAKER }.rolleId
            oppdatertSak.roller
                .first { it.fødselsnummer == nyttBarnFnr && it.rolleType == Rolletype.BARN }
                .objektnummer
                .shouldBe("06")
            oppdatertSak.roller.first { it.fødselsnummer == fnrBA3 && it.rolleType == Rolletype.BARN } shouldNotBe null
            oppdatertSak.roller.first { it.fødselsnummer == fnrBM && it.rolleType == Rolletype.BIDRAGSMOTTAKER } shouldNotBe null
            oppdatertSak.roller.first { it.fødselsnummer == fnrBP && it.rolleType == Rolletype.BIDRAGSPLIKTIG } shouldNotBe null
        }

        @Test
        fun `skal lagre ny reell mottager`() {
            val rmFnr = FnrGenerator.generer()
            testRestTemplate.postForEntity(
                urlOppdaterSak(),
                initHttpEntity(
                    OppdaterSakRequest(
                        saksnummer = saksnummer,
                        roller = setOf(RolleDto(Personident(fnrBA3), Rolletype.BARN, reellMottager = ReellMottaker(rmFnr))),
                    ),
                ),
                OppdaterSakResponse::class.java,
            )

            val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)
            oppdatertSak.roller shouldHaveSize 8
            oppdatertSak.roller
                .first { it.fødselsnummer == fnrBA1 && it.rolleType == Rolletype.BARN }
                .rolleId shouldBe
                oppdatertSak.roller.first { it.fødselsnummer == fnrBA1 && it.rolleType == Rolletype.REELMOTTAKER }.rolleId
            oppdatertSak.roller
                .first { it.fødselsnummer == fnrBA3 && it.rolleType == Rolletype.BARN }
                .rolleId shouldBe
                oppdatertSak.roller.first { it.fødselsnummer == rmFnr && it.rolleType == Rolletype.REELMOTTAKER }.rolleId
            oppdatertSak.roller
                .first { it.fødselsnummer == fnrBA2 && it.rolleType == Rolletype.BARN }
                .rolleId shouldBe
                oppdatertSak.roller.first { it.fødselsnummer == fnrBA2 && it.rolleType == Rolletype.REELMOTTAKER }.rolleId
            oppdatertSak.roller.first { it.fødselsnummer == fnrBM && it.rolleType == Rolletype.BIDRAGSMOTTAKER } shouldNotBe null
            oppdatertSak.roller.first { it.fødselsnummer == fnrBP && it.rolleType == Rolletype.BIDRAGSPLIKTIG } shouldNotBe null
        }

        @Test
        fun `skal lagre endring av reell mottager`() {
            val rmFnr = FnrGenerator.generer()
            testRestTemplate.postForEntity(
                urlOppdaterSak(),
                initHttpEntity(
                    OppdaterSakRequest(
                        saksnummer = saksnummer,
                        roller = setOf(RolleDto(Personident(fnrBA1), Rolletype.BARN, reellMottager = ReellMottaker(rmFnr))),
                    ),
                ),
                OppdaterSakResponse::class.java,
            )

            val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)
            oppdatertSak.roller shouldHaveSize 8
            oppdatertSak.roller
                .first { it.fødselsnummer == fnrBA2 && it.rolleType == Rolletype.BARN }
                .rolleId shouldBe
                oppdatertSak.roller.first { it.fødselsnummer == fnrBA2 && it.rolleType == Rolletype.REELMOTTAKER }.rolleId
            oppdatertSak.roller
                .first { it.fødselsnummer == fnrBA1 && it.rolleType == Rolletype.BARN }
                .rolleId shouldBe
                oppdatertSak.roller.first { it.fødselsnummer == rmFnr && it.rolleType == Rolletype.REELMOTTAKER }.rolleId
            oppdatertSak.roller
                .first { it.fødselsnummer == fnrBA3 && it.rolleType == Rolletype.BARN }
                .rolleId shouldBe null
            oppdatertSak.roller.first { it.fødselsnummer == fnrBP && it.rolleType == Rolletype.BIDRAGSPLIKTIG } shouldNotBe null
            oppdatertSak.roller.first { it.fødselsnummer == fnrBM && it.rolleType == Rolletype.BIDRAGSMOTTAKER } shouldNotBe null
        }

        @Test
        fun `skal lagre fjerning av reell mottager`() {
            testRestTemplate.postForEntity(
                urlOppdaterSak(),
                initHttpEntity(
                    OppdaterSakRequest(
                        saksnummer = saksnummer,
                        roller = setOf(RolleDto(Personident(fnrBA1), Rolletype.BARN)),
                    ),
                ),
                OppdaterSakResponse::class.java,
            )

            val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)
            oppdatertSak.roller shouldHaveSize 7
            oppdatertSak.roller
                .first { it.fødselsnummer == fnrBA2 && it.rolleType == Rolletype.BARN }
                .rolleId shouldBe
                oppdatertSak.roller.first { it.fødselsnummer == fnrBA2 && it.rolleType == Rolletype.REELMOTTAKER }.rolleId
            oppdatertSak.roller
                .first { it.fødselsnummer == fnrBA1 && it.rolleType == Rolletype.BARN }
                .rolleId shouldBe null
            oppdatertSak.roller
                .first { it.fødselsnummer == fnrBA3 && it.rolleType == Rolletype.BARN }
                .rolleId shouldBe null
            oppdatertSak.roller.first { it.fødselsnummer == fnrBM && it.rolleType == Rolletype.BIDRAGSMOTTAKER } shouldNotBe null
            oppdatertSak.roller.first { it.fødselsnummer == fnrBP && it.rolleType == Rolletype.BIDRAGSPLIKTIG } shouldNotBe null
        }

        @Test
        fun `skal takle internt bytte av reell mottager`() {
            testRestTemplate.postForEntity(
                urlOppdaterSak(),
                initHttpEntity(
                    OppdaterSakRequest(
                        saksnummer = saksnummer,
                        roller =
                        setOf(
                            RolleDto(Personident(fnrBA1), Rolletype.BARN, reellMottager = ReellMottaker(fnrBA2)),
                            RolleDto(Personident(fnrBA2), Rolletype.BARN, reellMottager = ReellMottaker(fnrBA1)),
                        ),
                    ),
                ),
                OppdaterSakResponse::class.java,
            )

            val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)
            oppdatertSak.roller shouldHaveSize 9

            oppdatertSak.roller
                .filter {
                    it.fødselsnummer == fnrBA2 && it.rolleType == Rolletype.REELMOTTAKER
                }.map { it.rolleId } shouldContain
                oppdatertSak.roller
                    .first {
                        it.fødselsnummer == fnrBA1 && it.rolleType == Rolletype.BARN
                    }.rolleId

            oppdatertSak.roller
                .filter {
                    it.fødselsnummer == fnrBA1 && it.rolleType == Rolletype.REELMOTTAKER
                }.map { it.rolleId } shouldContain
                oppdatertSak.roller
                    .first { it.fødselsnummer == fnrBA2 && it.rolleType == Rolletype.BARN }
                    .rolleId

            oppdatertSak.roller
                .first { it.fødselsnummer == fnrBA3 && it.rolleType == Rolletype.BARN }
                .rolleId shouldBe null
            oppdatertSak.roller.first { it.fødselsnummer == fnrBM && it.rolleType == Rolletype.BIDRAGSMOTTAKER } shouldNotBe null
            oppdatertSak.roller.first { it.fødselsnummer == fnrBP && it.rolleType == Rolletype.BIDRAGSPLIKTIG } shouldNotBe null
        }

        @Test
        fun `skal sette eierfogd til det som returneres fra hentEnhetForArbeidsfordelingGeografiskTilknytning`() {
            testRestTemplate.restTemplate.postForEntity(
                urlOppdaterSak(),
                initHttpEntity(
                    OppdaterSakRequest(
                        saksnummer = saksnummer,
                        kategorikode = Sakskategori.UTLAND,
                        landkode = Landkode("SWE"),
                        konvensjonskode = Konvensjon.H73,
                        konvensjonsdato = LocalDate.now(),
                        ffuReferansenr = "Foo",
                    ),
                ),
                OppdaterSakResponse::class.java,
            )

            val oppdatertSak = bidragssakRepository.findByIdOrThrow(saksnummer.verdi)

            val sisteEierfogd = oppdatertSak.tilganger.filter { it.tilgangTomDato == null }
            sisteEierfogd.size shouldBe 1
            sisteEierfogd.first().enhetsnummer shouldBe BidragOrganisasjonTestConfig.EIERFOGD_UTLAND
            oppdatertSak.eierfogd shouldBe BidragOrganisasjonTestConfig.EIERFOGD_UTLAND
        }

        private fun urlOppdaterSak() = UriComponentsBuilder
            .fromUriString(makeFullContextPath())
            .pathSegment("sak", "oppdater")
            .toUriString()
    }

    @Nested
    inner class OpprettSak {
        @Test
        fun `oppretter sak basert på OpprettSakRequest med tre roller`() {
            val barn = Personident(FnrGenerator.generer(2006, 4, 17))
            val bm = Personident(FnrGenerator.generer(1986, 4, 17))
            val bp = Personident(FnrGenerator.generer(1985, 4, 17))
            val opprettSakRequest =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("0"),
                    kategori = Sakskategori.NASJONAL,
                    ansatt = true,
                    inhabilitet = true,
                    levdeAdskilt = true,
                    konvensjon = Konvensjon.INGEN,
                    konvensjonsdato = LocalDate.of(2024, 10, 31),
                    land = Landkode("SWE"),
                    roller =
                    setOf(
                        RolleDto(
                            fødselsnummer = barn,
                            type = Rolletype.BARN,
                            objektnummer = "01",
                            reellMottager = ReellMottaker(bm.verdi),
                        ),
                        RolleDto(
                            fødselsnummer = bm,
                            type = Rolletype.BIDRAGSMOTTAKER,
                            objektnummer = "02",
                        ),
                        RolleDto(
                            fødselsnummer = bp,
                            type = Rolletype.BIDRAGSPLIKTIG,
                            objektnummer = "03",
                        ),
                    ),
                )

            val opprettSak =
                testRestTemplate.postForEntity(
                    localhost("sak"),
                    initHttpEntity(opprettSakRequest),
                    OpprettSakResponse::class.java,
                )
            opprettSak.body shouldNotBe null
        }

        @Test
        fun `oppretter sak basert på OpprettSkaRequest med korrekte felter`() {
            val fnr1 = FnrGenerator.generer()
            val fnr2 = FnrGenerator.generer()

            val opprettSak =
                testRestTemplate.postForEntity(
                    localhost("sak"),
                    initHttpEntity(
                        OpprettSakRequest(
                            eierfogd = Enhetsnummer("1701"),
                            kategori = Sakskategori.NASJONAL,
                            konvensjon = Konvensjon.NI,
                            roller =
                            setOf(
                                RolleDto(Personident(fnr1), Rolletype.BIDRAGSMOTTAKER),
                                RolleDto(Personident(fnr2), Rolletype.BIDRAGSPLIKTIG),
                            ),
                        ),
                    ),
                    OpprettSakResponse::class.java,
                )
            opprettSak.body shouldNotBe null

            val nySakFraBase = bidragssakRepository.findByIdOrThrow(opprettSak.body!!.saksnummer.verdi)
            nySakFraBase.eierfogd shouldBe "1701"
            nySakFraBase.kategori shouldBe Sakskategori.NASJONAL
            nySakFraBase.ansatt shouldBe false
            nySakFraBase.inhabilitet shouldBe false
            nySakFraBase.levdeAdskilt shouldBe false
            nySakFraBase.konvensjon shouldBe Konvensjon.NI
            nySakFraBase.konvensjonsdato shouldBe null
            nySakFraBase.ffuReferansenr shouldBe null
            nySakFraBase.land shouldBe null
            nySakFraBase.roller shouldExist {
                it.fødselsnummer == fnr1 && it.rolleType == Rolletype.BIDRAGSMOTTAKER
            }
            nySakFraBase.roller shouldExist {
                it.fødselsnummer == fnr2 && it.rolleType == Rolletype.BIDRAGSPLIKTIG
            }
        }

        @Test
        fun `oppretter sak med flere reelle mottagere oppretter sak med korrekte kobling i db og returnere korrekte koblinger`() {
            val fnrBarn1 = FnrGenerator.generer()
            val fnrBarn2 = FnrGenerator.generer()

            val opprettSak =
                testRestTemplate.postForEntity(
                    localhost("sak"),
                    initHttpEntity(
                        OpprettSakRequest(
                            eierfogd = Enhetsnummer("1701"),
                            kategori = Sakskategori.NASJONAL,
                            konvensjon = Konvensjon.NI,
                            roller =
                            setOf(
                                RolleDto(fødselsnummer = Personident(FnrGenerator.generer()), type = Rolletype.BIDRAGSMOTTAKER),
                                RolleDto(fødselsnummer = Personident(FnrGenerator.generer()), type = Rolletype.BIDRAGSPLIKTIG),
                                RolleDto(
                                    fødselsnummer = Personident(fnrBarn1),
                                    type = Rolletype.BARN,
                                    reellMottager = ReellMottaker(fnrBarn1),
                                ),
                                RolleDto(
                                    fødselsnummer = Personident(fnrBarn2),
                                    type = Rolletype.BARN,
                                    reellMottager = ReellMottaker(fnrBarn2),
                                ),
                            ),
                        ),
                    ),
                    OpprettSakResponse::class.java,
                )

            opprettSak.body shouldNotBe null
            val nySakFraBase = bidragssakRepository.findByIdOrThrow(opprettSak.body!!.saksnummer.verdi)
            val rmForBarn1 = nySakFraBase.roller.first { it.fødselsnummer == fnrBarn1 && it.rolleType == Rolletype.REELMOTTAKER }
            val baForBarn1 = nySakFraBase.roller.first { it.fødselsnummer == fnrBarn1 && it.rolleType == Rolletype.BARN }
            baForBarn1.rmRolleId shouldBe rmForBarn1.rolleId
            val rmForBarn2 = nySakFraBase.roller.first { it.fødselsnummer == fnrBarn2 && it.rolleType == Rolletype.REELMOTTAKER }
            val baForBarn2 = nySakFraBase.roller.first { it.fødselsnummer == fnrBarn2 && it.rolleType == Rolletype.BARN }
            baForBarn2.rmRolleId shouldBe rmForBarn2.rolleId
        }
    }

    @Nested
    internal inner class FunctionalTests {
        @Test
        fun `skal opprette bidragssak med tilgang til eierfogd`() {
            val response: ResponseEntity<NySakResponseDto> =
                testRestTemplate.exchange(
                    fullUrlForNySak(),
                    HttpMethod.POST,
                    initHttpEntity(NySakCommandDto(Enhetsnummer("1001"))),
                    NySakResponseDto::class.java,
                )
            response shouldNotBe null
            response.statusCode shouldBe HttpStatus.CREATED
            response.body!!.saksnummer shouldBeGreaterThanOrEqualTo Saksnummer(hentMinimumsgrenseForAarstall().toString())
        }

        @Test
        fun `skal opprette to bidragssaker med etterfølgende saksnummer`() {
            val responseSakEn =
                testRestTemplate.exchange(
                    fullUrlForNySak(),
                    HttpMethod.POST,
                    initHttpEntity(NySakCommandDto(Enhetsnummer("1001"))),
                    NySakResponseDto::class.java,
                )
            val responseSakTo =
                testRestTemplate.exchange(
                    fullUrlForNySak(),
                    HttpMethod.POST,
                    initHttpEntity(NySakCommandDto(Enhetsnummer("1001"))),
                    NySakResponseDto::class.java,
                )

            responseSakEn.body shouldNotBe null
            responseSakTo.body shouldNotBe null
            val saksnummerEn = requireNotNull(responseSakEn.body!!.saksnummer).verdi.toLong()
            val saksnummerTo = requireNotNull(responseSakTo.body!!.saksnummer).verdi.toLong()
            saksnummerTo shouldBe saksnummerEn + 1
        }

        @Test
        fun skalGiNotFoundDersomSakIkkeFinnes() {
            val sakSomIkkeFinnes = Saksnummer("1234567")
            every { tilgangClientMock.harTilgangSaksnummer(sakSomIkkeFinnes) }
                .throws(
                    HttpClientErrorException(HttpStatus.NOT_FOUND),
                )

            val respons =
                testRestTemplate.exchange(
                    fullUrlForSokSak(),
                    HttpMethod.GET,
                    null,
                    Any::class.java,
                )
            respons.statusCode shouldBe HttpStatus.NOT_FOUND
        }
    }

    @Nested
    @DisplayName("Tests with mocked repositories")
    internal inner class MockedRepositories {
        private val bidragspliktigIdent = genererFødselsnummer()
        private val bidragsmottakerIdent = genererFødselsnummer()
        private val reelmottakerIdent = genererFødselsnummer()
        private val reelmottakerIdent2 = genererFødselsnummer()
        private val ikkeFunnetIdent = genererFødselsnummer()

        private val bidragssakRepositoryMock: BidragssakRepository = mockk()
        private val hendelseRepositoryMock: HendelseRepository = mockk()
        private val rolleRepositoryMock: RolleRepository = mockk()
        private val vedtakOverføringRepositoryMock: VedtakOverføringRepository = mockk()

        private val cachedKodeverkService: CachedKodeverkService = mockk()

        private val arbeidsfordelingService: ArbeidsfordelingService = mockk(relaxed = true)

        private val rolleService: RolleService = mockk(relaxed = true)

        private val rollehistorikkService: RollehistorikkService = mockk(relaxed = true)

        private val hendelseService: HendelseService = mockk()

        private var bidragSakController: BidragSakController =
            BidragSakController(
                BidragSakService(
                    bidragssakRepository = bidragssakRepositoryMock,
                    hendelseRepository = hendelseRepositoryMock,
                    rolleRepository = rolleRepositoryMock,
                    vedtakOverføringRepository = vedtakOverføringRepositoryMock,
                    tilgangClient = tilgangClientMock,
                    cachedKodeverkService = cachedKodeverkService,
                    arbeidsfordelingService = arbeidsfordelingService,
                    rolleService = rolleService,
                    rollehistorikkService = rollehistorikkService,
                    hendelseService = hendelseService,
                    identConsumer = identConsumer,
                    opprettSakValidator = opprettSakValidator,
                    bbmConsumer = bbmConsumerMock,
                ),
            )

        @Test
        fun `skal finne metadata til gitt bidragsak`() {
            every { bidragssakRepositoryMock.findBySaksnummer("101") }
                .returns(
                    createBidragssak(
                        "101",
                        "1701",
                        Bidragssakstatus.IN,
                        mutableSetOf(
                            createRolle(bidragspliktigIdent, Rolletype.BIDRAGSPLIKTIG),
                            createRolle(bidragsmottakerIdent, Rolletype.BIDRAGSMOTTAKER),
                            createRolle(reelmottakerIdent, Rolletype.REELMOTTAKER),
                        ),
                    ),
                )
            every { bidragssakRepositoryMock.findBySaksnummer("102") } returns null

            val bidragSakDtoResponse = bidragSakController.findMetadataForSak(Saksnummer("101"), false)
            val bidragSakDtoNoResponse = bidragSakController.findMetadataForSak(Saksnummer("102"), false)

            bidragSakDtoResponse.statusCode shouldBe HttpStatus.OK
            bidragSakDtoNoResponse.statusCode shouldBe HttpStatus.NOT_FOUND
            bidragSakDtoResponse.body?.saksnummer shouldBe Saksnummer("101")
            bidragSakDtoResponse.body?.eierfogd shouldBe Enhetsnummer("1701")
            bidragSakDtoResponse.body?.saksstatus shouldBe Bidragssakstatus.IN
        }

        @Test
        fun `skal returnere metadata med rolleliste uten fnr for sak uten nødvendige rettigheter`() {
            every { tilgangClientMock.harTilgangSaksnummer(Saksnummer("101")) }
                .returns(false)

            every { bidragssakRepositoryMock.findBySaksnummer("101") }
                .returns(
                    createBidragssak(
                        "101",
                        "1701",
                        Bidragssakstatus.IN,
                        mutableSetOf(
                            createRolle(bidragspliktigIdent, Rolletype.BIDRAGSPLIKTIG),
                            createRolle(bidragsmottakerIdent, Rolletype.BIDRAGSMOTTAKER),
                            createRolle(reelmottakerIdent, Rolletype.REELMOTTAKER),
                        ),
                    ),
                )
            val bidragSakDtoResponse = bidragSakController.findMetadataForSak(Saksnummer("101"), false)
            bidragSakDtoResponse.body!!.roller.forEach { it.fødselsnummer shouldBe "" }
            bidragSakDtoResponse.statusCode shouldBe HttpStatus.OK
            bidragSakDtoResponse.body!!.begrensetTilgang shouldBe true
            bidragSakDtoResponse.body!!.saksnummer shouldBe Saksnummer("101")
            bidragSakDtoResponse.body!!.eierfogd shouldBe Enhetsnummer("1701")
            bidragSakDtoResponse.body!!.saksstatus shouldBe Bidragssakstatus.IN
        }

        @Test
        fun `skal finne bidragssak for gitt person`() {
            every { bidragssakRepositoryMock.findByRoller(listOf(reelmottakerIdent)) }
                .returns(
                    listOf(
                        createBidragssak(
                            "101",
                            "1701",
                            Bidragssakstatus.IN,
                            mutableSetOf(
                                createRolle(bidragspliktigIdent, Rolletype.BIDRAGSPLIKTIG),
                                createRolle(bidragsmottakerIdent, Rolletype.BIDRAGSMOTTAKER),
                                createRolle(reelmottakerIdent, Rolletype.REELMOTTAKER),
                            ),
                        ),
                    ),
                )
            every { bidragssakRepositoryMock.findByRoller(listOf(ikkeFunnetIdent)) } returns listOf()
            val bidragSakDtoResponse = bidragSakController.finnForFødselsnummer(Personident(reelmottakerIdent))
            val bidragSakDtoNoResponse = bidragSakController.finnForFødselsnummer(Personident(ikkeFunnetIdent))
            bidragSakDtoResponse.statusCode shouldBe HttpStatus.OK
            bidragSakDtoNoResponse.statusCode shouldBe HttpStatus.NOT_FOUND
            bidragSakDtoResponse.body!!.size shouldBe 1
            bidragSakDtoResponse.body!!.map { it.saksnummer } shouldContain Saksnummer("101")
            bidragSakDtoResponse.body!!.map { it.eierfogd } shouldContain Enhetsnummer("1701")
            bidragSakDtoResponse.body!!.map { it.saksstatus } shouldContain Bidragssakstatus.IN
        }

        @Test
        fun `skal skjerme fodselsnummer i roller ved manglende rettigheter`() {
            every { identConsumer.hentAlleIdenter(any()) }.answers { listOf(firstArg()) }
            every { tilgangClientMock.harTilgangSaksnummer(Saksnummer("101")) }
                .returns(false)
            val foedselsnummer = reelmottakerIdent
            every { bidragssakRepositoryMock.findByRoller(listOf(foedselsnummer)) }
                .returns(
                    listOf(
                        createBidragssak(
                            "101",
                            "1701",
                            Bidragssakstatus.IN,
                            mutableSetOf(
                                createRolle(bidragspliktigIdent, Rolletype.BIDRAGSPLIKTIG),
                                createRolle(bidragsmottakerIdent, Rolletype.BIDRAGSMOTTAKER),
                                createRolle(reelmottakerIdent, Rolletype.REELMOTTAKER),
                            ),
                        ),
                    ),
                )
            val bidragSakDtoResponse = bidragSakController.finnForFødselsnummer(Personident(reelmottakerIdent))
            bidragSakDtoResponse.statusCode shouldBe HttpStatus.OK
            bidragSakDtoResponse.body!!.size shouldBe 1
            bidragSakDtoResponse.body!!.map { it.saksnummer } shouldContain Saksnummer("101")
            bidragSakDtoResponse.body!!.map { it.eierfogd } shouldContain Enhetsnummer("1701")
            bidragSakDtoResponse.body!![0].roller.forEach { (foedselsnummer1, _) ->
                if (foedselsnummer != foedselsnummer1?.verdi) {
                    foedselsnummer1?.verdi shouldHaveLength 0
                }
            }
        }

        @Test
        fun `skal finne bidragssaker for gitt person`() {
            every { identConsumer.hentAlleIdenter(any()) }.answers { listOf(firstArg()) }
            every { bidragssakRepositoryMock.findByRoller(listOf(bidragspliktigIdent)) }
                .returns(
                    listOf(
                        createBidragssak(
                            "101",
                            mutableSetOf(
                                createRolle(bidragspliktigIdent, Rolletype.BIDRAGSPLIKTIG),
                                createRolle(bidragsmottakerIdent, Rolletype.BIDRAGSMOTTAKER),
                                createRolle(reelmottakerIdent, Rolletype.REELMOTTAKER),
                            ),
                        ),
                        createBidragssak(
                            "102",
                            mutableSetOf(
                                createRolle(bidragspliktigIdent, Rolletype.BIDRAGSPLIKTIG),
                                createRolle(bidragsmottakerIdent, Rolletype.BIDRAGSMOTTAKER),
                                createRolle(reelmottakerIdent2, Rolletype.REELMOTTAKER),
                            ),
                        ),
                    ),
                )
            val bidragSakDtoResponse = bidragSakController.finnForFødselsnummer(Personident(bidragspliktigIdent))
            bidragSakDtoResponse.statusCode shouldBe HttpStatus.OK
            bidragSakDtoResponse.body!! shouldHaveSize 2
            bidragSakDtoResponse.body!!.map { it.saksnummer } shouldBe listOf(Saksnummer("101"), Saksnummer("102"))
        }
    }

    fun <T : Any> initHttpEntity(body: T): HttpEntity<T> {
        val headers = HttpHeaders()
        headers.add(EnhetFilter.X_ENHET_HEADER, "1001")
        headers.add(CorrelationId.CORRELATION_ID_HEADER, "Correlateion_xxx_bidrag_sak")
        headers.setBearerAuth(lokalTestToken())
        return HttpEntity(body, headers)
    }
}

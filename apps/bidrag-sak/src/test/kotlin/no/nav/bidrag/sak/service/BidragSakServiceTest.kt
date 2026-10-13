package no.nav.bidrag.sak.service

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.util.IdentConsumer
import no.nav.bidrag.domene.enums.behandling.HendelseType
import no.nav.bidrag.domene.enums.behandling.SøknadGruppeKombinasjon
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.Fogdårsak
import no.nav.bidrag.domene.enums.sak.Konvensjon
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.sak.Tilgangstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.domene.land.Landkode
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.generer.testdata.person.genererPersonident
import no.nav.bidrag.sak.domain.Bidragssak
import no.nav.bidrag.sak.domain.Hendelse
import no.nav.bidrag.sak.domain.Søknad
import no.nav.bidrag.sak.domain.Søknadslinje
import no.nav.bidrag.sak.domain.Tilgang
import no.nav.bidrag.sak.dto.NySakCommandDto
import no.nav.bidrag.sak.integration.BidragBBMConsumer
import no.nav.bidrag.sak.integration.kodeverk.CachedKodeverkService
import no.nav.bidrag.sak.repository.BidragssakRepository
import no.nav.bidrag.sak.repository.HendelseRepository
import no.nav.bidrag.sak.repository.RolleRepository
import no.nav.bidrag.sak.repository.VedtakOverføringRepository
import no.nav.bidrag.sak.util.FnrGenerator
import no.nav.bidrag.sak.validering.OpprettSakValidator
import no.nav.bidrag.transport.sak.OppdaterRollerISakRequest
import no.nav.bidrag.transport.sak.OpprettMidlertidligTilgangRequest
import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.ReellMottakerDto
import no.nav.bidrag.transport.sak.RolleDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.time.LocalDateTime

internal class BidragSakServiceTest {
    private val bidragssakRepositoryMock: BidragssakRepository = mockk(relaxed = true)
    private val hendelseRepositoryMock: HendelseRepository = mockk()
    private val rolleRepositoryMock: RolleRepository = mockk()
    private val vedtakOverføringRepositoryMock: VedtakOverføringRepository = mockk()

    private val tilgangClientMock: Tilgangskontroll = mockk(relaxed = true)

    private val cachedKodeverkService: CachedKodeverkService = mockk()

    private val rolleService: RolleService = mockk(relaxed = true)
    private val bbmConsumerMock: BidragBBMConsumer = mockk(relaxed = true)

    private val rollehistorikkService: RollehistorikkService = mockk(relaxed = true)

    private val arbeidsfordelingService: ArbeidsfordelingService = mockk(relaxed = true)

    private val hendelseService: HendelseService = mockk(relaxed = true)
    private val identConsumer: IdentConsumer = mockk(relaxed = false)
    private val opprettSakValidator: OpprettSakValidator = mockk(relaxed = true)

    private lateinit var bidragSakService: BidragSakService

    private lateinit var saveSakSlot: CapturingSlot<Bidragssak>

    @BeforeEach
    fun initKlasseMedMockedRepo() {
        saveSakSlot = slot()
        every { bidragssakRepositoryMock.save(capture(saveSakSlot)) }.answers { saveSakSlot.captured }
        every { cachedKodeverkService.hentLandkoder() } returns mapOf(Landkode("NOR") to "Norge")
        every { identConsumer.hentAlleIdenter(any()) }.answers { listOf(firstArg()) }

        bidragSakService =
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
            )
    }

    @Nested
    inner class OpprettSak {
        @Test
        fun `skal opprette sak med gyldig landkode om request kommer med det`() {
            val opprettSakRequest =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    land = Landkode("NOR"),
                    roller =
                    setOf(
                        RolleDto(Personident(FnrGenerator.generer()), Rolletype.BIDRAGSMOTTAKER),
                        RolleDto(Personident(FnrGenerator.generer()), Rolletype.BIDRAGSPLIKTIG),
                    ),
                )

            bidragSakService.opprettSak(opprettSakRequest)

            saveSakSlot.captured.land shouldBe "NOR"
        }

        @Test
        fun `skal opprette sak med roller og tilgang basert på verdier fra request`() {
            every { rollehistorikkService.oppdaterRollehistorikk(any(), any(), any()) } answers {
                val bidragssak = thirdArg<Bidragssak>()
                bidragssak.roller
            }

            val opprettSakRequest =
                OpprettSakRequest(
                    eierfogd = Enhetsnummer("1701"),
                    kategori = Sakskategori.NASJONAL,
                    ansatt = true,
                    inhabilitet = false,
                    levdeAdskilt = true,
                    konvensjon = Konvensjon.US,
                    konvensjonsdato = LocalDate.now(),
                    ffuReferansenr = "ffu",
                    land = Landkode("NOR"),
                    roller =
                    setOf(
                        RolleDto(
                            type = Rolletype.BIDRAGSPLIKTIG,
                            mottagerErVerge = true,
                        ),
                        RolleDto(
                            type = Rolletype.BIDRAGSMOTTAKER,
                            mottagerErVerge = true,
                        ),
                    ),
                )

            bidragSakService.opprettSak(opprettSakRequest)

            saveSakSlot.captured.eierfogd shouldBe opprettSakRequest.eierfogd.verdi
            saveSakSlot.captured.kategori shouldBe opprettSakRequest.kategori
            saveSakSlot.captured.ansatt shouldBe opprettSakRequest.ansatt
            saveSakSlot.captured.inhabilitet shouldBe opprettSakRequest.inhabilitet
            saveSakSlot.captured.levdeAdskilt shouldBe opprettSakRequest.levdeAdskilt
            saveSakSlot.captured.konvensjon shouldBe opprettSakRequest.konvensjon
            saveSakSlot.captured.konvensjonsdato shouldBe opprettSakRequest.konvensjonsdato
            saveSakSlot.captured.ffuReferansenr shouldBe opprettSakRequest.ffuReferansenr
            saveSakSlot.captured.land shouldBe opprettSakRequest.land?.verdi

            val lagretRolle =
                saveSakSlot.captured.roller
                    .sortedBy { it.objektnummer }
                    .first()
            val requestRolle = opprettSakRequest.roller.first()
            lagretRolle.fødselsnummer shouldBe requestRolle.fødselsnummer
            lagretRolle.rolleType shouldBe requestRolle.type
            lagretRolle.mottagerErVerge shouldBe requestRolle.mottagerErVerge
            saveSakSlot.captured.tilganger
                .first()
                .enhetsnummer shouldBe "1701"
        }
    }

    @Nested
    inner class OppdaterRollerISak {
        private val saksnr = "2025-000001"
        private val barnUnder18 = genererPersonident(LocalDate.now().minusYears(10))

        private fun baMedFnr(fnr: Personident = genererPersonident()) = RolleDto(
            fødselsnummer = fnr,
            type = Rolletype.BARN,
        )

        private fun baMedRM(
            rm: String = "85000000074",
            verge: Boolean = false,
        ) = RolleDto(
            type = Rolletype.BARN,
            reellMottaker =
            ReellMottakerDto(
                ident = ReellMottaker(rm),
                verge = verge,
            ),
        )

        private fun bm(fnr: Personident = genererPersonident()) = RolleDto(
            fødselsnummer = fnr,
            type = Rolletype.BIDRAGSMOTTAKER,
        )

        private fun bp(fnr: Personident = genererPersonident()) = RolleDto(
            fødselsnummer = fnr,
            type = Rolletype.BIDRAGSPLIKTIG,
        )

        @BeforeEach
        fun stubSakFinnes() {
            // Sørg for at findByIdOrThrow (top-level ext) finner noe via findByIdOrNull
            every { bidragssakRepositoryMock.findByIdOrNull(saksnr) } returns
                Bidragssak(
                    saksnummer = saksnr,
                    eierfogd = "1701",
                ).also {
                    // legg på et minimum av eksisterende roller hvis ønskelig
                    it.tilganger.add(Tilgang(enhetsnummer = "1701", bidragssak = it))
                }

            every { bidragssakRepositoryMock.save(capture(saveSakSlot)) } answers { saveSakSlot.captured }
        }

        @Test
        fun `skal oppdatere roller og sende hendelse`() {
            val req =
                OppdaterRollerISakRequest(
                    saksnummer = Saksnummer(saksnr),
                    roller =
                    setOf(
                        bm(),
                        bp(),
                        baMedFnr(barnUnder18),
                    ),
                )

            every { rolleService.oppdaterRoller(any(), req.roller) } answers {
                // returnér eksisterende + noen "oppdaterte" roller
                val sakArg = firstArg<Bidragssak>()
                sakArg.roller + setOf()
            }

            // og kobling av RM etter lagring
            every { rolleService.oppdaterRollerMedReelleMottager(any(), any()) } answers { firstArg() }

            val resp = bidragSakService.oppdaterRollerISak(req)

            assertThat(resp).isNotNull
            // repository saves: en gang rett etter apply + en gang etter oppdatering av RM
            verify(exactly = 2) { bidragssakRepositoryMock.save(any()) }
            // oppdaterRoller kalt med sak + roller fra request
            verify(exactly = 1) { rolleService.oppdaterRoller(any(), req.roller) }
            // hendelse sendt
            verify(exactly = 1) { hendelseService.opprettKafkaHendelse(any(), any()) }
        }

        @Test
        fun `skal kaste feil hvis validering feiler for rolle (RM kun tillatt på BA)`() {
            every { opprettSakValidator.validerRolle(any()) } throws
                IllegalArgumentException("Reell mottaker (RM) kan kun registreres på barn (BA).")

            // RM på BP -> skal trigge require i RolleDto.valider()
            val req =
                OppdaterRollerISakRequest(
                    saksnummer = Saksnummer(saksnr),
                    roller =
                    setOf(
                        RolleDto(
                            type = Rolletype.BIDRAGSPLIKTIG,
                            reellMottaker =
                            ReellMottakerDto(
                                ident = ReellMottaker("85000000083"),
                                verge = false,
                            ),
                        ),
                    ),
                )

            assertThatThrownBy {
                bidragSakService.oppdaterRollerISak(req)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Reell mottaker (RM) kan kun registreres på barn (BA)")
        }

        @Test
        fun `skal koble reell mottaker for barn etter lagring`() {
            // oppdatering med ett barn som har RM
            val req =
                OppdaterRollerISakRequest(
                    saksnummer = Saksnummer(saksnr),
                    roller =
                    setOf(
                        baMedFnr(barnUnder18),
                        baMedRM("85000000083", verge = true),
                    ),
                )

            every { rolleService.oppdaterRoller(any(), req.roller) } answers {
                val sakArg = firstArg<Bidragssak>()
                sakArg.roller
            }

            // verifiser at vi kalles med liste som inneholder KUN roller med RM (size 1)
            every { rolleService.oppdaterRollerMedReelleMottager(any(), match { it.size == 1 && it.first().harRM() }) } answers {
                firstArg()
            }

            bidragSakService.oppdaterRollerISak(req)

            verify { rolleService.oppdaterRollerMedReelleMottager(any(), match { it.size == 1 && it.first().harRM() }) }
            verify { bidragssakRepositoryMock.save(any()) }
        }

        @Test
        fun `skal håndtere tomt roller-sett uten å feile`() {
            val req =
                OppdaterRollerISakRequest(
                    saksnummer = Saksnummer(saksnr),
                    roller = emptySet(),
                )

            every { rolleService.oppdaterRoller(any(), req.roller) } answers {
                val sakArg = firstArg<Bidragssak>()
                sakArg.roller // ingen endring
            }
            every { rolleService.oppdaterRollerMedReelleMottager(any(), any()) } answers { firstArg() }

            assertThatCode {
                bidragSakService.oppdaterRollerISak(req)
            }.doesNotThrowAnyException()
        }
    }

    @Nested
    inner class OpprettEllerUtvidMidlertidligTilgangSak {
        private val saksnr = "2025-000001"

        @BeforeEach
        fun stubSakFinnes() {
            every { bidragssakRepositoryMock.findByIdOrNull(saksnr) } returns
                Bidragssak(
                    saksnummer = saksnr,
                    eierfogd = "1701",
                ).also {
                    it.tilganger.add(Tilgang(enhetsnummer = "1701", bidragssak = it))
                }
        }

        @Test
        fun `skal ikke lagre når enhet er eierfogd`() {
            val req =
                OpprettMidlertidligTilgangRequest(
                    saksnummer = saksnr,
                    enhet = "1701",
                    tilgangTilOgMedDato = LocalDate.now().plusMonths(3),
                )

            bidragSakService.opprettEllerUtvidMidlertidligTilgangSak(req)

            verify(exactly = 0) { bidragssakRepositoryMock.save(any()) }
        }

        @Test
        fun `skal ikke lagre når aktiv midlertidig tilgang uten tom-dato allerede finnes`() {
            every { bidragssakRepositoryMock.findByIdOrNull(saksnr) } returns
                Bidragssak(saksnummer = saksnr, eierfogd = "1701").also { sak ->
                    sak.tilganger.addAll(
                        listOf(
                            Tilgang(enhetsnummer = "1701", bidragssak = sak),
                            Tilgang(
                                enhetsnummer = "4806",
                                tilgangTomDato = null,
                                årsak = Fogdårsak.MAKO,
                                type = Tilgangstype.MIDL,
                                bidragssak = sak,
                            ),
                        ),
                    )
                }

            val req =
                OpprettMidlertidligTilgangRequest(
                    saksnummer = saksnr,
                    enhet = "4806",
                    tilgangTilOgMedDato = LocalDate.now().plusMonths(3),
                )

            bidragSakService.opprettEllerUtvidMidlertidligTilgangSak(req)

            verify(exactly = 0) { bidragssakRepositoryMock.save(any()) }
        }

        @Test
        fun `skal ikke lagre når aktiv midlertidig tilgang med fremtidig tom-dato allerede finnes`() {
            every { bidragssakRepositoryMock.findByIdOrNull(saksnr) } returns
                Bidragssak(saksnummer = saksnr, eierfogd = "1701").also { sak ->
                    sak.tilganger.addAll(
                        listOf(
                            Tilgang(enhetsnummer = "1701", bidragssak = sak),
                            Tilgang(
                                enhetsnummer = "4806",
                                tilgangTomDato = LocalDate.now().plusMonths(1),
                                årsak = Fogdårsak.MAKO,
                                type = Tilgangstype.MIDL,
                                bidragssak = sak,
                            ),
                        ),
                    )
                }

            val req =
                OpprettMidlertidligTilgangRequest(
                    saksnummer = saksnr,
                    enhet = "4806",
                    tilgangTilOgMedDato = LocalDate.now().plusMonths(3),
                )

            bidragSakService.opprettEllerUtvidMidlertidligTilgangSak(req)

            verify(exactly = 0) { bidragssakRepositoryMock.save(any()) }
        }

        @Test
        fun `skal utvide utløpt midlertidig tilgang og lagre`() {
            val nyTomDato = LocalDate.now().plusMonths(6)
            val utløptTilgang =
                Tilgang(
                    enhetsnummer = "4806",
                    tilgangTomDato = LocalDate.now().minusDays(1),
                    årsak = Fogdårsak.MAKO,
                    type = Tilgangstype.MIDL,
                )

            every { bidragssakRepositoryMock.findByIdOrNull(saksnr) } returns
                Bidragssak(saksnummer = saksnr, eierfogd = "1701").also { sak ->
                    utløptTilgang.bidragssak = sak
                    sak.tilganger.addAll(
                        listOf(
                            Tilgang(enhetsnummer = "1701", bidragssak = sak),
                            utløptTilgang,
                        ),
                    )
                }

            val req =
                OpprettMidlertidligTilgangRequest(
                    saksnummer = saksnr,
                    enhet = "4806",
                    tilgangTilOgMedDato = nyTomDato,
                )

            bidragSakService.opprettEllerUtvidMidlertidligTilgangSak(req)

            utløptTilgang.tilgangTomDato shouldBe nyTomDato
            verify(exactly = 1) { bidragssakRepositoryMock.save(any()) }
        }

        @Test
        fun `skal opprette ny midlertidig tilgang med MAKO-årsak og MIDL-type`() {
            mockkStatic(TokenUtils::class)
            every { TokenUtils.hentSaksbehandlerIdent() } returns "Z999999"

            try {
                val tomDato = LocalDate.now().plusMonths(3)
                val req =
                    OpprettMidlertidligTilgangRequest(
                        saksnummer = saksnr,
                        enhet = "4806",
                        tilgangTilOgMedDato = tomDato,
                    )

                bidragSakService.opprettEllerUtvidMidlertidligTilgangSak(req)

                verify(exactly = 1) { bidragssakRepositoryMock.save(any()) }
                val midlTilgang = saveSakSlot.captured.tilganger.find { it.type == Tilgangstype.MIDL }
                assertThat(midlTilgang).isNotNull
                midlTilgang!!.enhetsnummer shouldBe "4806"
                midlTilgang.tilgangTomDato shouldBe tomDato
                midlTilgang.årsak shouldBe Fogdårsak.MAKO
                midlTilgang.opprettetAv shouldBe "Z999999"
            } finally {
                unmockkStatic(TokenUtils::class)
            }
        }
    }

    @Nested
    inner class HentMaxLoepenummerSomIkkeOverskrider {
        @Test
        fun `skal hente maks nummer for gitt årstall fra saksnummer for å generere neste saksnummer`() {
            every { bidragssakRepositoryMock.hentMaxLoepenummerSomIkkeOverskrider(any()) }
                .returns(AARSTALL_MINIMUMSGRENSE + 666)

            val nySakResponseDto = bidragSakService.nySak(NySakCommandDto(Enhetsnummer("101")))

            nySakResponseDto.saksnummer shouldBe Saksnummer((AARSTALL_MINIMUMSGRENSE + 667).toString())
            verify {
                bidragssakRepositoryMock.hentMaxLoepenummerSomIkkeOverskrider(AARSTALL_MAKSIMUMSGRENSE)
            }
        }

        @Test
        fun `skal starte ny nummerserie for årstall når tall fra db er mindre enn minimum for årstall`() {
            every { bidragssakRepositoryMock.hentMaxLoepenummerSomIkkeOverskrider(any()) }
                .returns(AARSTALL_MINIMUMSGRENSE - 666)

            val nySakResponseDto = bidragSakService.nySak(NySakCommandDto(Enhetsnummer("101")))

            nySakResponseDto.saksnummer shouldBe Saksnummer(AARSTALL_MINIMUMSGRENSE.toString())
            verify {
                bidragssakRepositoryMock.hentMaxLoepenummerSomIkkeOverskrider(AARSTALL_MAKSIMUMSGRENSE)
            }
        }
    }

    @Nested
    inner class NySak {
        @Test
        fun `skal opprette en ny bidragssak med tilgang`() {
            val nySakCommandDto = NySakCommandDto(Enhetsnummer("101"))
            bidragSakService.nySak(nySakCommandDto)
            verify {
                bidragssakRepositoryMock.save(any())
            }

            saveSakSlot.captured.saksnummer shouldBe AARSTALL_MINIMUMSGRENSE.toString()
            saveSakSlot.captured.eierfogd shouldBe "101"
            saveSakSlot.captured.tilganger
                .first()
                .enhetsnummer shouldBe "101"
            saveSakSlot.captured.tilganger
                .first()
                .tilgangFomDato shouldBe LocalDate.now()
        }
    }

    @Nested
    inner class FinnHendelserForSak {
        private val saksnummer = Saksnummer("2026-000001")

        @BeforeEach
        fun setup() {
            every { tilgangClientMock.harTilgangSaksnummer(saksnummer) } returns true
            every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns emptyList()
            every { vedtakOverføringRepositoryMock.countVedtakGrunnlagOverførtForSak(any(), any()) } returns 0
            every { vedtakOverføringRepositoryMock.finnVedtakIdBidragVedtakForSak(any(), any()) } returns emptyList()
            every { rolleRepositoryMock.findByBySaksnummerAndRolleType(saksnummer.verdi, Rolletype.BARN) } returns emptyList()
        }

        @Test
        fun `skal returnere tom liste og ikke kalle repository ved begrenset tilgang`() {
            every { tilgangClientMock.harTilgangSaksnummer(saksnummer) } returns false

            val result = bidragSakService.finnHendelserForSak(saksnummer)

            result shouldBe emptyList()
            verify(exactly = 0) { hendelseRepositoryMock.findBySaksnummer(any()) }
        }

        @Test
        fun `skal kalle repository og returnere mappede hendelser ved full tilgang`() {
            every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                listOf(Hendelse(saksnummer = saksnummer.verdi, type = HendelseType.BRUKERSTØTTE, enhet = "1701", søknad = null))

            val result = bidragSakService.finnHendelserForSak(saksnummer)

            result shouldHaveSize 1
            verify(exactly = 1) { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) }
        }

        @Test
        fun `vedtak-hendelse skal få link VEDTAK`() {
            every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                listOf(Hendelse(saksnummer = saksnummer.verdi, type = HendelseType.VEDTAK, enhet = "1701", søknad = null))

            val result = bidragSakService.finnHendelserForSak(saksnummer)

            result.first().link shouldBe "VEDTAK"
        }

        @Test
        fun `vedtak-hendelse fra BBM med resultat skal ikke få link VEDTAK`() {
            every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                listOf(
                    Hendelse(
                        saksnummer = saksnummer.verdi,
                        type = HendelseType.VEDTAK,
                        enhet = "1701",
                        resultat = "INNVILGET",
                        fraBbm = true,
                        søknad = null,
                    ),
                )

            val result = bidragSakService.finnHendelserForSak(saksnummer)

            result.first().link shouldBe null
        }

        @Test
        fun `forskudd-hendelse skal få riktig link og søknadsgruppe`() {
            every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                listOf(
                    Hendelse(
                        saksnummer = saksnummer.verdi,
                        type = HendelseType.BRUKERSTØTTE,
                        enhet = "1701",
                        grKombKode = SøknadGruppeKombinasjon.FORSKUDD.kode,
                        søknad = null,
                    ),
                )

            val result = bidragSakService.finnHendelserForSak(saksnummer)

            result.first().link shouldBe SøknadGruppeKombinasjon.FORSKUDD.kode
            result.first().søknadsgruppe shouldBe SøknadGruppeKombinasjon.FORSKUDD
        }

        @Test
        fun `særbidrag-hendelse skal få riktig link og søknadsgruppe`() {
            every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                listOf(
                    Hendelse(
                        saksnummer = saksnummer.verdi,
                        type = HendelseType.BRUKERSTØTTE,
                        enhet = "1701",
                        grKombKode = SøknadGruppeKombinasjon.SÆRBIDRAG.kode,
                        søknad = null,
                    ),
                )

            val result = bidragSakService.finnHendelserForSak(saksnummer)

            result.first().link shouldBe SøknadGruppeKombinasjon.SÆRBIDRAG.kode
            result.first().søknadsgruppe shouldBe SøknadGruppeKombinasjon.SÆRBIDRAG
        }

        @Test
        fun `bidrag-hendelse skal få riktig link og søknadsgruppe`() {
            every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                listOf(
                    Hendelse(
                        saksnummer = saksnummer.verdi,
                        type = HendelseType.BRUKERSTØTTE,
                        enhet = "1701",
                        grKombKode = SøknadGruppeKombinasjon.BIDRAG.kode,
                        søknad = null,
                    ),
                )

            val result = bidragSakService.finnHendelserForSak(saksnummer)

            result.first().link shouldBe SøknadGruppeKombinasjon.BIDRAG.kode
            result.first().søknadsgruppe shouldBe SøknadGruppeKombinasjon.BIDRAG
        }

        @Test
        fun `hendelse uten kategorisering skal få link null og ingen søknadsgruppe`() {
            every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                listOf(Hendelse(saksnummer = saksnummer.verdi, type = HendelseType.BRUKERSTØTTE, enhet = "1701", søknad = null))

            val result = bidragSakService.finnHendelserForSak(saksnummer)

            result.first().link shouldBe null
            result.first().søknadsgruppe shouldBe null
        }

        @Test
        fun `skal mappe alle felter korrekt fra hendelse til dto`() {
            val tidspunkt = LocalDateTime.of(2026, 6, 1, 10, 0)
            every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                listOf(
                    Hendelse(
                        hendelseId = 123,
                        saksnummer = saksnummer.verdi,
                        type = HendelseType.BRUKERSTØTTE,
                        enhet = "1701",
                        opprettetTidspunkt = tidspunkt,
                        resultat = "INNVILGET",
                        grKombKode = SøknadGruppeKombinasjon.FORSKUDD.kode,
                        søknad = null,
                    ),
                )

            val result = bidragSakService.finnHendelserForSak(saksnummer)

            result.first().hendelseId shouldBe "123"
            result.first().opprettetTidspunkt shouldBe tidspunkt
            result.first().enhet shouldBe Enhetsnummer("1701")
            result.first().type shouldBe HendelseType.BRUKERSTØTTE
            result.first().resultat shouldBe "INNVILGET"
        }

        @Nested
        inner class ErLukket {
            private fun hendelseMedSøknad(søknad: Søknad?) = Hendelse(saksnummer = saksnummer.verdi, type = HendelseType.BRUKERSTØTTE, enhet = "1701", søknad = søknad)

            private fun søknadMedLinjer(vararg statuser: String): Søknad {
                val søknad = Søknad(søknadslinjer = mutableListOf(), hendelser = mutableListOf())
                statuser.forEach { søknad.søknadslinjer.add(Søknadslinje(søknad = søknad, statusKode = it)) }
                return søknad
            }

            @Test
            fun `uten søknad skal erLukket være true`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelseMedSøknad(null))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().erLukket shouldBe true
            }

            @Test
            fun `med tom søknadslinjeliste skal erLukket være true`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelseMedSøknad(søknadMedLinjer()))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().erLukket shouldBe true
            }

            @Test
            fun `med kun lukkede søknadslinjer skal erLukket være true`() {
                // VF = VEDTAK_FATTET (lukket), DM = DMT_AVSLUTTET (lukket)
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelseMedSøknad(søknadMedLinjer("VF", "DM")))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().erLukket shouldBe true
            }

            @Test
            fun `med en ikke-lukket søknadslinje skal erLukket være false`() {
                // UB = UNDER_BEHANDLING (ikke lukket)
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelseMedSøknad(søknadMedLinjer("UB")))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().erLukket shouldBe false
            }

            @Test
            fun `med blanding av lukkede og ikke-lukkede søknadslinjer skal erLukket være false`() {
                // VF = lukket, UB = ikke lukket → én åpen linje gjør at erLukket er false
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelseMedSøknad(søknadMedLinjer("VF", "UB", "DM")))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().erLukket shouldBe false
            }

            @Test
            fun `med ukjent statusKode skal den ignoreres og erLukket bli true`() {
                // mapNotNull fjerner ukjente koder → fold over tom liste → true
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelseMedSøknad(søknadMedLinjer("UKJENT_KODE")))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().erLukket shouldBe true
            }

            @Test
            fun `alle ikke-lukkede statuser gir erLukket false`() {
                // IH = INGEN_HENDELSE, SA = SENDT_NAV_FARSKAPSENHET – begge ikke-lukket
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelseMedSøknad(søknadMedLinjer("IH", "SA")))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().erLukket shouldBe false
            }
        }

        @Nested
        inner class ResultatIBisys {
            private fun hendelse(
                resultat: String?,
                fraBbm: Boolean,
                type: HendelseType = HendelseType.BRUKERSTØTTE,
                grKombKode: String? = null,
                behandlingId: String? = null,
            ) = Hendelse(
                saksnummer = saksnummer.verdi,
                type = type,
                enhet = "1701",
                resultat = resultat,
                fraBbm = fraBbm,
                grKombKode = grKombKode,
                søknad = behandlingId?.let { Søknad(id = null, søknadslinjer = mutableListOf(), hendelser = mutableListOf(), behandlingId = it) },
            )

            @Test
            fun `er false når resultat er null`() {
                // Ikke-vedtak (BRUS) med søknadsgruppelenke, slik at vi isolerer BBM-grenen
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelse(resultat = null, fraBbm = true, grKombKode = SøknadGruppeKombinasjon.FORSKUDD.kode))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().resultatIBisys shouldBe false
            }

            @Test
            fun `er false når resultat er blank`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelse(resultat = "  ", fraBbm = true, grKombKode = SøknadGruppeKombinasjon.FORSKUDD.kode))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().resultatIBisys shouldBe false
            }

            @Test
            fun `er false når fraBbm er false`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelse(resultat = "INNVILGET", fraBbm = false, grKombKode = SøknadGruppeKombinasjon.FORSKUDD.kode))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().resultatIBisys shouldBe false
            }

            @Test
            fun `er false når link er null (ingen vedtak og ingen søknadsgruppe)`() {
                // BRUS uten grKombKode → link = null
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelse(resultat = "INNVILGET", fraBbm = true, type = HendelseType.BRUKERSTØTTE))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().resultatIBisys shouldBe false
            }

            @Test
            fun `er true når resultat er satt, fraBbm er true og link er VEDTAK`() {
                // NB: erVedtak=false når fraBbm=true+resultat!=null, så vi bruker grKombKode for link
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(
                        hendelse(
                            resultat = "INNVILGET",
                            fraBbm = true,
                            grKombKode = SøknadGruppeKombinasjon.FORSKUDD.kode,
                        ),
                    )

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().resultatIBisys shouldBe true
                result.first().link shouldBe SøknadGruppeKombinasjon.FORSKUDD.kode
            }

            @Test
            fun `er true når resultat er satt, fraBbm er true og link er en søknadsgruppekode`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(
                        hendelse(
                            resultat = "INNVILGET",
                            fraBbm = true,
                            grKombKode = SøknadGruppeKombinasjon.FORSKUDD.kode,
                        ),
                    )

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().resultatIBisys shouldBe true
                result.first().link shouldBe SøknadGruppeKombinasjon.FORSKUDD.kode
            }

            @Test
            fun `er true for vedtakslenke og vedtaks-hendelsestype selv uten BBM-resultat`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelse(resultat = null, fraBbm = false, type = HendelseType.VEDTAK))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().link shouldBe "VEDTAK"
                result.first().resultatIBisys shouldBe true
            }

            @Test
            fun `er true for alle vedtaks-hendelsestyper med vedtakslenke`() {
                val vedtakstyper =
                    listOf(
                        HendelseType.VEDTAK,
                        HendelseType.VEDTAK_BARN_OVER_18,
                        HendelseType.VEDTAK_BP,
                        HendelseType.VEDTAK_BM,
                        HendelseType.VEDTAK_VERGE,
                        HendelseType.VEDTAK_FYLKESNEMDA,
                        HendelseType.VEDTAK_KOMMUNE,
                        HendelseType.VEDTAK_FRA_BOST,
                        HendelseType.VEDTAK_FTK,
                        HendelseType.VEDTAK_UTENLANDSKE_MYNDIGHETER,
                        HendelseType.VEDTAK_MIDLERTIDIG,
                        HendelseType.INDEKSREGULERT,
                    )
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    vedtakstyper.map { hendelse(resultat = null, fraBbm = false, type = it) }

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.forEach { it.resultatIBisys shouldBe true }
            }

            @Test
            fun `er false for vedtakstype fattet i ny løsning (behandlingId satt)`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelse(resultat = null, fraBbm = false, type = HendelseType.VEDTAK, behandlingId = "behandling-123"))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().resultatIBisys shouldBe false
            }
        }

        @Nested
        inner class ErBisysVedtakOgErOverført {
            private fun søknadMedId(
                id: Int?,
                behandlingId: String? = null,
            ) = Søknad(id = id, søknadslinjer = mutableListOf(), hendelser = mutableListOf(), behandlingId = behandlingId)

            private fun hendelseMedSøknad(søknad: Søknad?) = Hendelse(saksnummer = saksnummer.verdi, type = HendelseType.BRUKERSTØTTE, enhet = "1701", søknad = søknad)

            @Test
            fun `er false når søknad er null`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelseMedSøknad(null))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().erBisysVedtakOgErOverført shouldBe false
            }

            @Test
            fun `er false når søknad har behandlingId (ny løsning)`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelseMedSøknad(søknadMedId(id = 42, behandlingId = "behandling-123")))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().erBisysVedtakOgErOverført shouldBe false
            }

            @Test
            fun `er false når søknad-id er null og behandlingId er null`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelseMedSøknad(søknadMedId(id = null, behandlingId = null)))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().erBisysVedtakOgErOverført shouldBe false
            }

            @Test
            fun `er false når ingen vedtak er overført for sak og søknad`() {
                every {
                    vedtakOverføringRepositoryMock.countVedtakGrunnlagOverførtForSak(saksnummer, 42)
                } returns 0
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelseMedSøknad(søknadMedId(id = 42)))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().erBisysVedtakOgErOverført shouldBe false
            }

            @Test
            fun `er true når minst ett vedtak er overført for sak og søknad`() {
                every {
                    vedtakOverføringRepositoryMock.countVedtakGrunnlagOverførtForSak(saksnummer, 42)
                } returns 1
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelseMedSøknad(søknadMedId(id = 42)))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().erBisysVedtakOgErOverført shouldBe true
            }
        }

        @Nested
        inner class VedtaksidOgBehandlingsid {
            private fun søknadMedId(
                id: Int?,
                behandlingId: String? = null,
            ) = Søknad(id = id, søknadslinjer = mutableListOf(), hendelser = mutableListOf(), behandlingId = behandlingId)

            private fun hendelseMedSøknad(søknad: Søknad?) = Hendelse(saksnummer = saksnummer.verdi, type = HendelseType.BRUKERSTØTTE, enhet = "1701", søknad = søknad)

            @Test
            fun `behandlingsid settes fra søknad`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelseMedSøknad(søknadMedId(id = 42, behandlingId = "behandling-123")))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().behandlingsid shouldBe "behandling-123"
            }

            @Test
            fun `behandlingsid er null når søknad mangler`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelseMedSøknad(null))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().behandlingsid shouldBe null
            }

            @Test
            fun `vedtaksid settes fra vedtakoverføring for ny løsning`() {
                every { vedtakOverføringRepositoryMock.finnVedtakIdBidragVedtakForSak(saksnummer, 42) } returns listOf(9999)
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelseMedSøknad(søknadMedId(id = 42)))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().vedtaksid shouldBe "9999"
            }

            @Test
            fun `vedtaksid er null når ingen overføring finnes`() {
                every { vedtakOverføringRepositoryMock.finnVedtakIdBidragVedtakForSak(saksnummer, 42) } returns emptyList()
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelseMedSøknad(søknadMedId(id = 42)))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().vedtaksid shouldBe null
            }

            @Test
            fun `vedtaksid er null og repository kalles ikke når søknad mangler`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelseMedSøknad(null))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().vedtaksid shouldBe null
                verify(exactly = 0) { vedtakOverføringRepositoryMock.finnVedtakIdBidragVedtakForSak(any(), any()) }
            }
        }

        @Nested
        inner class VisKlage {
            private fun hendelse(
                type: HendelseType,
                grKombKode: String? = null,
            ) = Hendelse(
                saksnummer = saksnummer.verdi,
                type = type,
                enhet = "1701",
                grKombKode = grKombKode,
                søknad = null,
            )

            @Test
            fun `er true for vedtakstype uten refusjon- eller innkrevingskode`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelse(type = HendelseType.VEDTAK, grKombKode = SøknadGruppeKombinasjon.BIDRAG.kode))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().erKlageberettigetVedtak shouldBe true
            }

            @Test
            fun `er true for avvisning (AV)`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelse(type = HendelseType.AVVIST, grKombKode = SøknadGruppeKombinasjon.BIDRAG.kode))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().erKlageberettigetVedtak shouldBe true
            }

            @Test
            fun `er true for vedtakstype når grKombKode er null`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelse(type = HendelseType.VEDTAK, grKombKode = null))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().erKlageberettigetVedtak shouldBe true
            }

            @Test
            fun `er true for alle vedtaks- og avvisningstyper`() {
                val gyldigeTyper =
                    listOf(
                        HendelseType.VEDTAK,
                        HendelseType.VEDTAK_BARN_OVER_18,
                        HendelseType.VEDTAK_BM,
                        HendelseType.VEDTAK_BP,
                        HendelseType.VEDTAK_FYLKESNEMDA,
                        HendelseType.VEDTAK_KOMMUNE,
                        HendelseType.VEDTAK_UTENLANDSKE_MYNDIGHETER,
                        HendelseType.VEDTAK_VERGE,
                        HendelseType.VEDTAK_FRA_BOST,
                        HendelseType.VEDTAK_FTK,
                        HendelseType.AVVIST,
                    )
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    gyldigeTyper.map { hendelse(type = it) }

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.forEach { it.erKlageberettigetVedtak shouldBe true }
            }

            @Test
            fun `er false for hendelsestype som ikke er vedtak eller avvisning`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelse(type = HendelseType.BRUKERSTØTTE))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().erKlageberettigetVedtak shouldBe false
            }

            @Test
            fun `er false når grKombKode er refusjon bidrag (RB)`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelse(type = HendelseType.VEDTAK, grKombKode = SøknadGruppeKombinasjon.REFUSJON_BIDRAG.kode))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().erKlageberettigetVedtak shouldBe false
            }

            @Test
            fun `er false når grKombKode er innkreving (IK)`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(hendelse(type = HendelseType.VEDTAK, grKombKode = SøknadGruppeKombinasjon.INNKREVING.kode))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().erKlageberettigetVedtak shouldBe false
            }
        }

        @Nested
        inner class BarnObjektNumre {
            @Test
            fun `skal returnere objektnummer for alle barn tilknyttet saken`() {
                val barn1 = mockk<no.nav.bidrag.sak.domain.Rolle> { every { objektnummer } returns "01" }
                val barn2 = mockk<no.nav.bidrag.sak.domain.Rolle> { every { objektnummer } returns "02" }
                every { rolleRepositoryMock.findByBySaksnummerAndRolleType(saksnummer.verdi, Rolletype.BARN) } returns listOf(barn1, barn2)
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(Hendelse(saksnummer = saksnummer.verdi, type = HendelseType.BRUKERSTØTTE, enhet = "1701", søknad = null))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().barnObjektNumre shouldBe listOf("01", "02")
            }

            @Test
            fun `skal returnere tom liste når ingen barn er tilknyttet saken`() {
                every { rolleRepositoryMock.findByBySaksnummerAndRolleType(saksnummer.verdi, Rolletype.BARN) } returns emptyList()
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(Hendelse(saksnummer = saksnummer.verdi, type = HendelseType.BRUKERSTØTTE, enhet = "1701", søknad = null))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().barnObjektNumre shouldBe emptyList()
            }
        }

        @Nested
        inner class SoknadFraOgVedtakType {
            private fun søknadMedBlankett(
                soknFraKode: String?,
                soknType: String?,
            ) = Søknad(
                id = null,
                søknadslinjer = mutableListOf(),
                hendelser = mutableListOf(),
                blankett =
                no.nav.bidrag.sak.domain
                    .Blankett(blankettId = 1, soknFraKode = soknFraKode, soknType = soknType),
            )

            @Test
            fun `skal sette søktAv fra blankett soknFraKode`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(
                        Hendelse(
                            saksnummer = saksnummer.verdi,
                            type = HendelseType.BRUKERSTØTTE,
                            enhet = "1701",
                            søknad = søknadMedBlankett(soknFraKode = "MO", soknType = null),
                        ),
                    )

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().søktAv shouldBe no.nav.bidrag.domene.enums.rolle.SøktAvType.BIDRAGSMOTTAKER
            }

            @Test
            fun `skal returnere null for søktAv når blankett mangler`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(Hendelse(saksnummer = saksnummer.verdi, type = HendelseType.BRUKERSTØTTE, enhet = "1701", søknad = null))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().søktAv shouldBe null
            }

            @Test
            fun `skal sette vedtakType FA til FASTSETTELSE`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(
                        Hendelse(
                            saksnummer = saksnummer.verdi,
                            type = HendelseType.VEDTAK,
                            enhet = "1701",
                            søknad = søknadMedBlankett(soknFraKode = null, soknType = "FA"),
                        ),
                    )

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().vedtakType shouldBe no.nav.bidrag.domene.enums.vedtak.Vedtakstype.FASTSETTELSE
            }

            @Test
            fun `skal returnere null for vedtakType når blankett mangler`() {
                every { hendelseRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                    listOf(Hendelse(saksnummer = saksnummer.verdi, type = HendelseType.BRUKERSTØTTE, enhet = "1701", søknad = null))

                val result = bidragSakService.finnHendelserForSak(saksnummer)

                result.first().vedtakType shouldBe null
            }
        }
    }

    @Nested
    inner class HarSkrivetilgang {
        private val saksnummer = Saksnummer("1234567")
        private val enhet = "4806"

        private fun sak(vararg tilganger: Tilgang) = Bidragssak(saksnummer = saksnummer.verdi, eierfogd = enhet).also {
            it.tilganger.addAll(tilganger)
        }

        private fun tilgang(
            enhetsnummer: String = enhet,
            fomDato: LocalDate = LocalDate.now(),
            tomDato: LocalDate? = null,
        ) = Tilgang(enhetsnummer = enhetsnummer, tilgangFomDato = fomDato, tilgangTomDato = tomDato)

        @BeforeEach
        fun setup() {
            every { bidragssakRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns sak()
        }

        @Test
        fun `er true for tilgang uten tom-dato`() {
            every { bidragssakRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                sak(tilgang(fomDato = LocalDate.now().minusDays(1)))

            bidragSakService.harSkrivetilgang(saksnummer, enhet) shouldBe true
        }

        @Test
        fun `er true for tilgang med fremtidig tom-dato`() {
            every { bidragssakRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                sak(tilgang(tomDato = LocalDate.now().plusDays(1)))

            bidragSakService.harSkrivetilgang(saksnummer, enhet) shouldBe true
        }

        @Test
        fun `er false når fom-dato er i fremtiden`() {
            every { bidragssakRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                sak(tilgang(fomDato = LocalDate.now().plusDays(1)))

            bidragSakService.harSkrivetilgang(saksnummer, enhet) shouldBe false
        }

        @Test
        fun `er false når tom-dato er passert`() {
            every { bidragssakRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                sak(tilgang(tomDato = LocalDate.now().minusDays(1)))

            bidragSakService.harSkrivetilgang(saksnummer, enhet) shouldBe false
        }

        @Test
        fun `er false når enhetsnummer ikke matcher`() {
            every { bidragssakRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns
                sak(tilgang(enhetsnummer = "9999"))

            bidragSakService.harSkrivetilgang(saksnummer, enhet) shouldBe false
        }

        @Test
        fun `er false når sak ikke finnes`() {
            every { bidragssakRepositoryMock.findBySaksnummer(saksnummer.verdi) } returns null

            bidragSakService.harSkrivetilgang(saksnummer, enhet) shouldBe false
        }
    }

    companion object {
        private val AARSTALL_MINIMUMSGRENSE = hentMinimumsgrenseForAarstall()
        private val AARSTALL_MAKSIMUMSGRENSE = hentMaksimumsgrenseForAarstall()

        private fun hentMinimumsgrenseForAarstall(): Int = hentNaaverendeAarstall() * 100000

        private fun hentMaksimumsgrenseForAarstall(): Int = (hentNaaverendeAarstall() + 1) * 100000

        private fun hentNaaverendeAarstall(): Int = LocalDate.now().year % 100
    }
}

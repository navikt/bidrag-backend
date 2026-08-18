package no.nav.bidrag.bbm.service

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.bidrag.bbm.CommonTestRunner
import no.nav.bidrag.bbm.bo.FinnSammenknytningerHovedsøknadRequest
import no.nav.bidrag.bbm.bo.SammenknyttSøknaderRequest
import no.nav.bidrag.bbm.bo.SlettHovedsøknadRequest
import no.nav.bidrag.bbm.bo.SlettSammenknytningForSøknadRequest
import no.nav.bidrag.bbm.bo.SøknadsknytningStatus
import no.nav.bidrag.bbm.utils.PERSONIDENT_BARN_1
import no.nav.bidrag.bbm.utils.PERSONIDENT_BARN_2
import no.nav.bidrag.bbm.utils.PERSONIDENT_BARN_3
import no.nav.bidrag.bbm.utils.PERSONIDENT_BARN_4
import no.nav.bidrag.bbm.utils.PERSONIDENT_BM_1
import no.nav.bidrag.bbm.utils.PERSONIDENT_BM_2
import no.nav.bidrag.bbm.utils.PERSONIDENT_BM_3
import no.nav.bidrag.bbm.utils.PERSONIDENT_BP_1
import no.nav.bidrag.bbm.utils.SAKSNUMMER_1
import no.nav.bidrag.bbm.utils.SAKSNUMMER_2
import no.nav.bidrag.bbm.utils.SAKSNUMMER_3
import no.nav.bidrag.bbm.utils.TestdataManager
import no.nav.bidrag.bbm.utils.opprettBlankett
import no.nav.bidrag.bbm.utils.opprettKodeSøknadStatus
import no.nav.bidrag.bbm.utils.opprettRolle
import no.nav.bidrag.bbm.utils.opprettSøknad
import no.nav.bidrag.bbm.utils.opprettSøknadslinje
import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.transport.behandling.beregning.felles.Barn
import no.nav.bidrag.transport.behandling.beregning.felles.FeilregistrerSøknadRequest
import no.nav.bidrag.transport.behandling.beregning.felles.FeilregistrerSøknadsBarnRequest
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknadRequest
import no.nav.bidrag.transport.behandling.beregning.felles.LeggTilBarnIFFSøknadRequest
import no.nav.bidrag.transport.behandling.beregning.felles.OppdaterBehandlerenhetRequest
import no.nav.bidrag.transport.behandling.beregning.felles.OppdaterBehandlingsidRequest
import no.nav.bidrag.transport.behandling.beregning.felles.OppdaterReferanseGebyrRequest
import no.nav.bidrag.transport.behandling.beregning.felles.OpprettSøknadRequest
import no.nav.bidrag.transport.behandling.beregning.felles.PartISøknad
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.toString

class BisysServiceTest(
    @param:Autowired override var testdataManager: TestdataManager,
) : CommonTestRunner() {
    @Autowired
    private lateinit var bisysService: BisysService

    @BeforeEach
    fun setup() {
        testdataManager.rydd()
    }

    @Test
    fun `skal hente åpne søknader i saker tilknyttet angitt BP`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
                opprettKodeSøknadStatus(kode = "VF", lukketStatus = "1"),
                opprettKodeSøknadStatus(kode = "TR", lukketStatus = "1"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_3, rolletype = "BA"),
                    opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BM_2, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BARN_4, rolletype = "BA"),
                ),
            )

        val blanketter =
            testdataManager.lagreBlankettListe(
                listOf(
                    opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "MO", søknadstype = "FA"),
                    opprettBlankett(saksnummer = SAKSNUMMER_2, søknadFraKode = "MO", søknadstype = "FA"),
                ),
            )

        val søknader =
            testdataManager.lagreSøknadListe(
                listOf(
                    opprettSøknad(
                        saksnummer = SAKSNUMMER_1,
                        blankettid = blanketter[0].blankettid!!,
                        søknadMottattDato = LocalDate.parse("2024-02-01"),
                        søknadFomDato = LocalDate.parse("2024-01-01"),
                        søknadsgruppekode = "BI",
                        behandlingsid = "1",
                    ),
                    opprettSøknad(
                        saksnummer = SAKSNUMMER_1,
                        blankettid = blanketter[0].blankettid!!,
                        søknadMottattDato = LocalDate.parse("2024-03-01"),
                        søknadFomDato = LocalDate.parse("2024-02-01"),
                        søknadsgruppekode = "FO",
                        behandlingsid = "2",
                    ),
                    opprettSøknad(
                        saksnummer = SAKSNUMMER_1,
                        blankettid = blanketter[0].blankettid!!,
                        søknadsgruppekode = "GB",
                        behandlingsid = "3",
                    ),
                    opprettSøknad(
                        saksnummer = SAKSNUMMER_2,
                        blankettid = blanketter[1].blankettid!!,
                        søknadMottattDato = LocalDate.parse("2024-03-01"),
                        søknadFomDato = LocalDate.parse("2024-02-01"),
                        søknadsgruppekode = "BI",
                        behandlingsid = "4",
                    ),
                ),
            )

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknader[0].søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    innbetaltBeløp = BigDecimal.valueOf(100.01),
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "BI",
                    saksnummer = SAKSNUMMER_1,
                ),
                opprettSøknadslinje(
                    søknadsid = søknader[0].søknadsid!!,
                    rolleid = roller[3].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "TR",
                    gruppeKombinasjonskode = "BI",
                    saksnummer = SAKSNUMMER_1,
                ),
                opprettSøknadslinje(
                    søknadsid = søknader[0].søknadsid!!,
                    rolleid = roller[4].rolleid!!,
                    innbetaltBeløp = BigDecimal.valueOf(250.01),
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "BI",
                    saksnummer = SAKSNUMMER_1,
                ),
                opprettSøknadslinje(
                    søknadsid = søknader[1].søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "FO",
                    saksnummer = SAKSNUMMER_1,
                ),
                opprettSøknadslinje(
                    søknadsid = søknader[2].søknadsid!!,
                    rolleid = roller[0].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "GB",
                    saksnummer = SAKSNUMMER_1,
                    referanseGebyr = "referanse gebyr 1",
                ),
                opprettSøknadslinje(
                    søknadsid = søknader[2].søknadsid!!,
                    rolleid = roller[1].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "GB",
                    saksnummer = SAKSNUMMER_1,
                ),
                opprettSøknadslinje(
                    søknadsid = søknader[3].søknadsid!!,
                    rolleid = roller[7].rolleid!!,
                    innbetaltBeløp = BigDecimal.valueOf(500.01),
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "BI",
                    saksnummer = SAKSNUMMER_2,
                ),
            ),
        )

        fun valider(respons: List<HentSøknad>) {
            respons.shouldHaveSize(2)
            assertSoftly(respons[0]) {
                saksnummer shouldBe SAKSNUMMER_1
                søknadsid shouldBe søknader[0].søknadsid
                behandlingstema shouldBe Behandlingstema.BIDRAG
                behandlingsid shouldBe 1L
                søknadMottattDato shouldBe LocalDate.parse("2024-02-01")
                søknadFomDato shouldBe LocalDate.parse("2024-01-01")
                behandlerenhet shouldBe "4608"
                søktAvType shouldBe SøktAvType.BIDRAGSMOTTAKER
                partISøknadListe shouldBe
                    listOf(
                        PartISøknad(
                            personident = PERSONIDENT_BP_1,
                            rolletype = Rolletype.BIDRAGSPLIKTIG,
                            behandlingstatus = null,
                            innbetaltBeløp = null,
                            gebyr = true,
                            referanseGebyr = "referanse gebyr 1",
                        ),
                        PartISøknad(
                            personident = PERSONIDENT_BM_1,
                            rolletype = Rolletype.BIDRAGSMOTTAKER,
                            behandlingstatus = null,
                            innbetaltBeløp = null,
                            gebyr = true,
                            referanseGebyr = null,
                        ),
                        PartISøknad(
                            personident = PERSONIDENT_BARN_1,
                            rolletype = Rolletype.BARN,
                            behandlingstatus = Behandlingstatus.UNDER_BEHANDLING,
                            innbetaltBeløp = BigDecimal.valueOf(100.01),
                            gebyr = false,
                            referanseGebyr = null,
                        ),
                        PartISøknad(
                            personident = PERSONIDENT_BARN_2,
                            rolletype = Rolletype.BARN,
                            behandlingstatus = Behandlingstatus.TRUKKET,
                            innbetaltBeløp = null,
                            gebyr = false,
                            referanseGebyr = null,
                        ),
                        PartISøknad(
                            personident = PERSONIDENT_BARN_3,
                            rolletype = Rolletype.BARN,
                            behandlingstatus = Behandlingstatus.UNDER_BEHANDLING,
                            innbetaltBeløp = BigDecimal.valueOf(250.01),
                            gebyr = false,
                            referanseGebyr = null,
                        ),
                    )
            }

            assertSoftly(respons[1]) {
                saksnummer shouldBe SAKSNUMMER_2
                søknadsid shouldBe søknader[3].søknadsid
                behandlingstema shouldBe Behandlingstema.BIDRAG
                behandlingsid shouldBe 4L
                søknadMottattDato shouldBe LocalDate.parse("2024-03-01")
                søknadFomDato shouldBe LocalDate.parse("2024-02-01")
                søktAvType shouldBe SøktAvType.BIDRAGSMOTTAKER
                partISøknadListe shouldBe
                    listOf(
                        PartISøknad(
                            personident = PERSONIDENT_BP_1,
                            rolletype = Rolletype.BIDRAGSPLIKTIG,
                            behandlingstatus = null,
                            innbetaltBeløp = null,
                            gebyr = false,
                            referanseGebyr = null,
                        ),
                        PartISøknad(
                            personident = PERSONIDENT_BM_2,
                            rolletype = Rolletype.BIDRAGSMOTTAKER,
                            behandlingstatus = null,
                            innbetaltBeløp = null,
                            gebyr = false,
                            referanseGebyr = null,
                        ),
                        PartISøknad(
                            personident = PERSONIDENT_BARN_4,
                            rolletype = Rolletype.BARN,
                            behandlingstatus = Behandlingstatus.UNDER_BEHANDLING,
                            innbetaltBeløp = BigDecimal.valueOf(500.01),
                            gebyr = false,
                            referanseGebyr = null,
                        ),
                    )
            }
        }

        valider(
            bisysService
                .hentÅpneSøknaderForPerson(
                    personident = PERSONIDENT_BP_1,
                ).åpneSøknader
                .sortedWith(compareBy({ it.saksnummer }, { it.søknadsid })),
        )
    }

    @Test
    fun `oppretter blankett, søknad og hendelse for gyldig bidrag`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
                opprettKodeSøknadStatus(kode = "VF", lukketStatus = "1"),
                opprettKodeSøknadStatus(kode = "TR", lukketStatus = "1"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                ),
            )

        val request =
            OpprettSøknadRequest(
                saksnummer = SAKSNUMMER_1,
                behandlingsid = 1L,
                behandlerenhet = "enhet1",
                behandlingstema = Behandlingstema.BIDRAG,
                søknadFomDato = LocalDate.now(),
                innkreving = true,
                barnListe =
                listOf(
                    Barn(
                        personident = PERSONIDENT_BARN_1,
                    ),
                ),
                behandlingstype = null,
            )

        val response = bisysService.opprettSøknader(request)

        val opprettetSøknad = testdataManager.hentSøknadMedId(response.søknadsid)
        val opprettedeSøknadslinjer = testdataManager.hentSøknadslinjerForSøknadMedId(response.søknadsid)
        val hendelse = testdataManager.hentHendelserForSøknadMedId(response.søknadsid)

        val åpneSøknader = bisysService.hentÅpneSøknaderForPerson(PERSONIDENT_BP_1).åpneSøknader

        assertSoftly {
            opprettetSøknad!!.apply {
                søknadsid shouldBe response.søknadsid
                søknadMottattDato shouldBe LocalDate.now()
                søknadFomDato shouldBe request.søknadFomDato
                søknadsgruppekode shouldBe "BI"
                behandlerenhet shouldBe request.behandlerenhet
                saksnummer shouldBe request.saksnummer
                behandlingsid shouldBe request.behandlingsid.toString()
            }

            opprettedeSøknadslinjer!!.first().apply {
                søknadsid shouldBe response.søknadsid
                rolleid shouldBe roller[2].rolleid
                innbetaltBeløp shouldBe null
                søknadStatuskode shouldBe "UB"
                gruppeKombinasjonskode shouldBe "BI"
                saksnummer shouldBe request.saksnummer
            }

            hendelse!!.first().apply {
                saksnummer shouldBe request.saksnummer
                søknadsid shouldBe response.søknadsid
                hendelsestype shouldBe "FFET"
                enhet shouldBe request.behandlerenhet
                søknadstype shouldBe "FF"
                gruppeKombinasjonskode shouldBe "BI"
                systemOpprettetDato.toLocalDate() shouldBe LocalDate.now()
            }

            åpneSøknader.shouldHaveSize(1)
            åpneSøknader.first().apply {
                saksnummer shouldBe request.saksnummer
                behandlingstema shouldBe Behandlingstema.BIDRAG
                behandlingsid shouldBe request.behandlingsid
                søknadFomDato shouldBe request.søknadFomDato
            }
        }
    }

    @Test
    fun `kaster feil for ugyldig stønadstype`() {
        val request =
            OpprettSøknadRequest(
                saksnummer = SAKSNUMMER_1,
                behandlingsid = 1L,
                behandlerenhet = "enhet1",
                behandlingstema = Behandlingstema.EKTEFELLEBIDRAG,
                søknadFomDato = LocalDate.now(),
                innkreving = true,
                barnListe =
                listOf(
                    Barn(
                        personident = PERSONIDENT_BARN_1,
                    ),
                ),
                behandlingstype = null,
            )

        val exception =
            shouldThrow<HttpClientErrorException> {
                bisysService.opprettSøknader(request)
            }

        exception.message shouldBe "400 Ugyldig behandlingstema angitt: EKTEFELLEBIDRAG"
    }

    @Test
    fun `oppretter søknad med innkreving satt til false`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
                opprettKodeSøknadStatus(kode = "VF", lukketStatus = "1"),
                opprettKodeSøknadStatus(kode = "TR", lukketStatus = "1"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                ),
            )

        val request =
            OpprettSøknadRequest(
                saksnummer = SAKSNUMMER_1,
                behandlingsid = 1L,
                behandlerenhet = "enhet1",
                behandlingstema = Behandlingstema.BIDRAG,
                søknadFomDato = LocalDate.now(),
                innkreving = false,
                barnListe =
                listOf(
                    Barn(
                        personident = PERSONIDENT_BARN_1,
                    ),
                ),
                behandlingstype = null,
            )

        val response = bisysService.opprettSøknader(request)

        val opprettetSøknad = testdataManager.hentSøknadMedId(response.søknadsid)

        val opprettedeSøknadslinjer = testdataManager.hentSøknadslinjerForSøknadMedId(response.søknadsid)

        val åpneSøknader = bisysService.hentÅpneSøknaderForPerson(PERSONIDENT_BP_1).åpneSøknader

        assertSoftly {
            opprettetSøknad!!.apply {
                søknadsid shouldBe response.søknadsid
                søknadMottattDato shouldBe LocalDate.now()
                søknadFomDato shouldBe request.søknadFomDato
                søknadsgruppekode shouldBe "BI"
                behandlerenhet shouldBe request.behandlerenhet
                saksnummer shouldBe request.saksnummer
                behandlingsid shouldBe request.behandlingsid.toString()
            }

            opprettedeSøknadslinjer!!.first().apply {
                søknadsid shouldBe response.søknadsid
                rolleid shouldBe roller[2].rolleid
                innbetaltBeløp shouldBe null
                søknadStatuskode shouldBe "UB"
                gruppeKombinasjonskode shouldBe "B"
                saksnummer shouldBe request.saksnummer
            }

            åpneSøknader.shouldHaveSize(1)
            åpneSøknader.first().apply {
                saksnummer shouldBe SAKSNUMMER_1
                behandlingstema shouldBe Behandlingstema.BIDRAG
                behandlingsid shouldBe 1L
                søknadFomDato shouldBe request.søknadFomDato
            }
        }
    }

    @Test
    fun `oppretter søknad for bidrag 18 år`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
                opprettKodeSøknadStatus(kode = "VF", lukketStatus = "1"),
                opprettKodeSøknadStatus(kode = "TR", lukketStatus = "1"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
                ),
            )

        val request =
            OpprettSøknadRequest(
                saksnummer = SAKSNUMMER_1,
                behandlingsid = 1L,
                behandlerenhet = "enhet1",
                behandlingstema = Behandlingstema.BIDRAG_18_ÅR,
                søknadFomDato = LocalDate.now(),
                innkreving = true,
                barnListe =
                listOf(
                    Barn(
                        personident = PERSONIDENT_BARN_1,
                    ),
                    Barn(
                        personident = PERSONIDENT_BARN_2,
                    ),
                ),
                behandlingstype = null,
            )

        val response = bisysService.opprettSøknader(request)

        val opprettetSøknad = testdataManager.hentSøknadMedId(response.søknadsid)

        val opprettedeSøknadslinjer = testdataManager.hentSøknadslinjerForSøknadMedId(response.søknadsid)

        val åpneSøknader = bisysService.hentÅpneSøknaderForPerson(PERSONIDENT_BP_1).åpneSøknader

        assertSoftly {
            opprettetSøknad!!.apply {
                søknadsid shouldBe response.søknadsid
                søknadMottattDato shouldBe LocalDate.now()
                søknadFomDato shouldBe request.søknadFomDato
                søknadsgruppekode shouldBe "18"
                behandlerenhet shouldBe request.behandlerenhet
                saksnummer shouldBe request.saksnummer
                behandlingsid shouldBe request.behandlingsid.toString()
            }

            opprettedeSøknadslinjer!!.first().apply {
                søknadsid shouldBe response.søknadsid
                rolleid shouldBe roller[2].rolleid
                innbetaltBeløp shouldBe null
                søknadStatuskode shouldBe "UB"
                gruppeKombinasjonskode shouldBe "II"
                saksnummer shouldBe request.saksnummer
            }

            opprettedeSøknadslinjer[1].apply {
                søknadsid shouldBe response.søknadsid
                rolleid shouldBe roller[3].rolleid
                innbetaltBeløp shouldBe null
                søknadStatuskode shouldBe "UB"
                gruppeKombinasjonskode shouldBe "II"
                saksnummer shouldBe request.saksnummer
            }

            åpneSøknader.shouldHaveSize(1)
            åpneSøknader.first().apply {
                saksnummer shouldBe SAKSNUMMER_1
                behandlingstema shouldBe Behandlingstema.BIDRAG_18_ÅR
                behandlingsid shouldBe 1L
                søknadFomDato shouldBe request.søknadFomDato
            }
        }
    }

    @Test
    fun `skal lagre behandlingsid når søknad eksisterer og behandlingsid er null`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "RF"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankett.blankettid!!,
                            behandlingsid = null,
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "BI",
                    saksnummer = SAKSNUMMER_1,
                ),
            ),
        )

        val request = OppdaterBehandlingsidRequest(søknadsid = søknad.søknadsid!!, nyBehandlingsid = 1L)
        bisysService.oppdaterBehandlingsid(request)

        val åpneSøknader = bisysService.hentÅpneSøknaderForPerson(PERSONIDENT_BP_1).åpneSøknader
        åpneSøknader.first().behandlingsid shouldBe 1L
    }

    @Test
    fun `skal oppdatere behandlerenhet når søknad eksisterer`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "RF"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankett.blankettid!!,
                            behandlingsid = null,
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "BI",
                    saksnummer = SAKSNUMMER_1,
                ),
            ),
        )

        val request = OppdaterBehandlerenhetRequest(søknadsid = søknad.søknadsid!!, behandlerenhet = "1234")
        bisysService.oppdaterBehandlerenhet(request)

        val oppdatertSøknad = testdataManager.hentSøknadMedId(søknad.søknadsid!!)
        oppdatertSøknad!!.behandlerenhet shouldBe "1234"
    }

    @Test
    fun `skal kaste HttpClientErrorException når søknad ikke eksisterer`() {
        val request = OppdaterBehandlingsidRequest(søknadsid = 999L, nyBehandlingsid = 1L)

        val exception =
            shouldThrow<HttpClientErrorException> {
                bisysService.oppdaterBehandlingsid(request)
            }

        exception.statusCode shouldBe HttpStatus.NOT_FOUND
        exception.statusText shouldBe "Ingen søknad med angitt søknadsid funnet"
    }

    @Test
    fun `skal kaste HttpClientErrorException når eksisterende behandlingsid ikke matcher`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        testdataManager.lagreRoller(
            listOf(
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
            ),
        )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "RF"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankett.blankettid!!,
                            behandlingsid = "1",
                        ),
                    ),
                ).first()

        val request =
            OppdaterBehandlingsidRequest(
                søknadsid = søknad.søknadsid!!,
                eksisterendeBehandlingsid = 2L,
                nyBehandlingsid = 3L,
            )

        val exception =
            shouldThrow<HttpClientErrorException> {
                bisysService.oppdaterBehandlingsid(request)
            }

        exception.statusCode shouldBe HttpStatus.BAD_REQUEST
        exception.statusText shouldBe "Angitt eksisterende behandlingsid stemmer ikke med lagret behandlingsid"
    }

    @Test
    fun `skal feilregistrere alle søknadslinjer for søknad som skal feilregistreres når søknad eksisterer`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
                opprettKodeSøknadStatus(kode = "FR", lukketStatus = "1"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "BM", søknadstype = "FF"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            blankettid = blankett.blankettid!!,
                            søknadMottattDato = LocalDate.parse("2024-02-01"),
                            søknadFomDato = LocalDate.parse("2024-01-01"),
                            søknadsgruppekode = "BI",
                            saksnummer = SAKSNUMMER_1,
                            behandlingsid = "1",
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "BI",
                    saksnummer = SAKSNUMMER_1,
                ),
            ),
        )

        // Test at tilhørende innkrevingssøknad også feilregistreres
        val innkrevingssøknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            blankettid = blankett.blankettid!!,
                            søknadMottattDato = LocalDate.parse("2024-02-01"),
                            søknadFomDato = LocalDate.parse("2024-01-01"),
                            søknadsgruppekode = "IK",
                            saksnummer = SAKSNUMMER_1,
                            behandlingsid = "1",
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = innkrevingssøknad.søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "IK",
                    saksnummer = SAKSNUMMER_1,
                ),
            ),
        )

        val request = FeilregistrerSøknadRequest(søknadsid = søknad.søknadsid!!)
        bisysService.feilregistrerSøknad(request)

        val oppdaterteSøknadslinjer = testdataManager.hentSøknadslinjerForSøknadMedId(søknad.søknadsid!!)
        oppdaterteSøknadslinjer!!.first().søknadStatuskode shouldBe "FR"

        val oppdaterteInnkrevingssøknadslinjer = testdataManager.hentSøknadslinjerForSøknadMedId(innkrevingssøknad.søknadsid!!)
        oppdaterteInnkrevingssøknadslinjer!!.first().søknadStatuskode shouldBe "FR"
    }

    @Test
    fun `skal kaste HttpClientErrorException når man forsøker å feilregistrere søknad som ikke eksisterer`() {
        val søknadsid = 999L
        val request = FeilregistrerSøknadRequest(søknadsid = søknadsid)

        val exception =
            shouldThrow<HttpClientErrorException> {
                bisysService.feilregistrerSøknad(request)
            }

        exception.message shouldBe "400 Fant ikke søknad med id: $søknadsid"
    }

    @Test
    fun `skal håndtere tom liste med søknadslinjer`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        testdataManager.lagreRoller(
            listOf(
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
            ),
        )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "BM", søknadstype = "FF"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            blankettid = blankett.blankettid!!,
                            søknadMottattDato = LocalDate.parse("2024-02-01"),
                            søknadFomDato = LocalDate.parse("2024-01-01"),
                            søknadsgruppekode = "BI",
                            saksnummer = SAKSNUMMER_1,
                        ),
                    ),
                ).first()

        val request = FeilregistrerSøknadRequest(søknadsid = søknad.søknadsid!!)
        bisysService.feilregistrerSøknad(request)

        val søknadslinjer = testdataManager.hentSøknadslinjerForSøknadMedId(søknad.søknadsid!!)
        søknadslinjer shouldBe emptyList()
    }

    @Test
    fun `skal legge til barn i FF-søknad med innkreving når det ligger innkreving i søknaden fra før`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankett.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "BI",
                    saksnummer = SAKSNUMMER_1,
                ),
            ),
        )

        val request =
            LeggTilBarnIFFSøknadRequest(søknadsid = søknad.søknadsid!!, personidentBarn = PERSONIDENT_BARN_2)
        bisysService.leggTilBarnIFFSøknad(request)

        val søknadslinjer = testdataManager.hentSøknadslinjerForSøknadMedId(søknad.søknadsid!!)

        assertSoftly {
            søknadslinjer!!.shouldHaveSize(2)
            søknadslinjer.first().apply {
                rolleid shouldBe roller[2].rolleid
                gruppeKombinasjonskode shouldBe "BI"
                søknadStatuskode shouldBe "UB"
            }
            søknadslinjer[1].apply {
                rolleid shouldBe roller[3].rolleid
                gruppeKombinasjonskode shouldBe "BI"
                søknadStatuskode shouldBe "UB"
            }
        }
    }

    @Test
    fun `skal legge til barn i FF-søknad for bidrag 18 år med innkreving`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankett.blankettid!!,
                            søknadsgruppekode = "II",
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "II",
                    saksnummer = SAKSNUMMER_1,
                ),
            ),
        )

        val request =
            LeggTilBarnIFFSøknadRequest(søknadsid = søknad.søknadsid!!, personidentBarn = PERSONIDENT_BARN_2)
        bisysService.leggTilBarnIFFSøknad(request)

        val søknadslinjer = testdataManager.hentSøknadslinjerForSøknadMedId(søknad.søknadsid!!)

        søknadslinjer!!.first().gruppeKombinasjonskode shouldBe "II"
        søknadslinjer[1].gruppeKombinasjonskode shouldBe "II"
    }

    @Test
    fun `skal resette søknadslinje til under behandling for barn i FF-søknad som ligger som feilregistrert i søknad fra før`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankett.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "FR",
                    gruppeKombinasjonskode = "BI",
                    saksnummer = SAKSNUMMER_1,
                ),
            ),
        )

        val request =
            LeggTilBarnIFFSøknadRequest(søknadsid = søknad.søknadsid!!, personidentBarn = PERSONIDENT_BARN_1)
        bisysService.leggTilBarnIFFSøknad(request)

        val søknadslinjer = testdataManager.hentSøknadslinjerForSøknadMedId(søknad.søknadsid!!)

        søknadslinjer!!.size shouldBe 1
        søknadslinjer[0].gruppeKombinasjonskode shouldBe "BI"
    }

    @Test
    fun `skal kaste HttpClientErrorException når søknad ikke finnes for legg til barn`() {
        val request = LeggTilBarnIFFSøknadRequest(søknadsid = 999L, personidentBarn = PERSONIDENT_BARN_1)

        val exception =
            shouldThrow<HttpClientErrorException> {
                bisysService.leggTilBarnIFFSøknad(request)
            }

        exception.message shouldBe "400 Fant ikke søknad med id: 999"
    }

    @Test
    fun `skal kaste HttpClientErrorException når søknadstype ikke er FF for legg til barn`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "BI"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankett.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "BI",
                    saksnummer = SAKSNUMMER_1,
                ),
            ),
        )

        val request =
            LeggTilBarnIFFSøknadRequest(søknadsid = søknad.søknadsid!!, personidentBarn = PERSONIDENT_BARN_2)

        val exception =
            shouldThrow<HttpClientErrorException> {
                bisysService.leggTilBarnIFFSøknad(request)
            }

        exception.message shouldBe "400 Søknad med id: ${søknad.søknadsid} er ikke en FF-søknad"
    }

    @Test
    fun `skal kaste HttpClientErrorException når man forsøker å legge til et barn som ikke har en rolle i saken`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        testdataManager.lagreRoller(
            listOf(
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
            ),
        )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankett.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()

        val request =
            LeggTilBarnIFFSøknadRequest(søknadsid = søknad.søknadsid!!, personidentBarn = PERSONIDENT_BARN_1)

        val exception =
            shouldThrow<HttpClientErrorException> {
                bisysService.leggTilBarnIFFSøknad(request)
            }

        exception.message shouldBe "400 Fant ikke rolle for barn med personident: $PERSONIDENT_BARN_1"
    }

    @Test
    fun `hent søknad - skal returnere VEDTAK_FATTET når alle søknadslinjer har lukketStatus = true og minst én har status VF`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "VF", lukketStatus = "1"),
                opprettKodeSøknadStatus(kode = "TR", lukketStatus = "1"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadstype = "FF", søknadFraKode = "ET"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            blankettid = blankett.blankettid!!,
                            søknadsgruppekode = "BI",
                            saksnummer = SAKSNUMMER_1,
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    søknadsstatuskode = "VF",
                    saksnummer = SAKSNUMMER_1,
                    innbetaltBeløp = null,
                    gruppeKombinasjonskode = "BI",
                ),
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[3].rolleid!!,
                    søknadsstatuskode = "TR",
                    saksnummer = SAKSNUMMER_1,
                    innbetaltBeløp = null,
                    gruppeKombinasjonskode = "BI",
                ),
            ),
        )

        val response = bisysService.hentSøknad(HentSøknadRequest(søknadsid = søknad.søknadsid!!))

        response.søknad.behandlingStatusType shouldBe BehandlingStatusType.VEDTAK_FATTET
    }

    @Test
    fun `hent søknad - skal returnere AVBRUTT når alle søknadslinjer har lukketStatus = true og minst én har status FR`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "FR", lukketStatus = "1"),
                opprettKodeSøknadStatus(kode = "TR", lukketStatus = "1"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadstype = "FF", søknadFraKode = "ET"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            blankettid = blankett.blankettid!!,
                            søknadsgruppekode = "BI",
                            saksnummer = SAKSNUMMER_1,
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    søknadsstatuskode = "FR",
                    saksnummer = SAKSNUMMER_1,
                    innbetaltBeløp = null,
                    gruppeKombinasjonskode = "BI",
                ),
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[3].rolleid!!,
                    søknadsstatuskode = "TR",
                    saksnummer = SAKSNUMMER_1,
                    innbetaltBeløp = null,
                    gruppeKombinasjonskode = "BI",
                ),
            ),
        )

        val response = bisysService.hentSøknad(HentSøknadRequest(søknadsid = søknad.søknadsid!!))

        response.søknad.behandlingStatusType shouldBe BehandlingStatusType.AVBRUTT
    }

    @Test
    fun `hent søknad - skal returnere UNDER_BEHANDLING når minst én søknadslinje har lukketStatus = false`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
                opprettKodeSøknadStatus(kode = "VF", lukketStatus = "1"),
                opprettKodeSøknadStatus(kode = "FR", lukketStatus = "1"),
                opprettKodeSøknadStatus(kode = "TR", lukketStatus = "1"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadstype = "FF", søknadFraKode = "ET"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            blankettid = blankett.blankettid!!,
                            søknadsgruppekode = "BI",
                            saksnummer = SAKSNUMMER_1,
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    søknadsstatuskode = "UB",
                    saksnummer = SAKSNUMMER_1,
                    innbetaltBeløp = null,
                    gruppeKombinasjonskode = "BI",
                ),
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[3].rolleid!!,
                    søknadsstatuskode = "VF",
                    saksnummer = SAKSNUMMER_1,
                    innbetaltBeløp = null,
                    gruppeKombinasjonskode = "BI",
                ),
            ),
        )

        val response = bisysService.hentSøknad(HentSøknadRequest(søknadsid = søknad.søknadsid!!))

        response.søknad.behandlingStatusType shouldBe BehandlingStatusType.UNDER_BEHANDLING
    }

    @Test
    fun `hent søknad - skal returnere UNDER_BEHANDLING når alle søknadslinjer har lukketStatus = true og ingen har VF eller FR-status`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
                opprettKodeSøknadStatus(kode = "VF", lukketStatus = "1"),
                opprettKodeSøknadStatus(kode = "FR", lukketStatus = "1"),
                opprettKodeSøknadStatus(kode = "TR", lukketStatus = "1"),
                opprettKodeSøknadStatus(kode = "ER", lukketStatus = "1"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadstype = "FF", søknadFraKode = "ET"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            blankettid = blankett.blankettid!!,
                            søknadsgruppekode = "BI",
                            saksnummer = SAKSNUMMER_1,
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    søknadsstatuskode = "TR",
                    saksnummer = SAKSNUMMER_1,
                    innbetaltBeløp = null,
                    gruppeKombinasjonskode = "BI",
                ),
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[3].rolleid!!,
                    søknadsstatuskode = "ER",
                    saksnummer = SAKSNUMMER_1,
                    innbetaltBeløp = null,
                    gruppeKombinasjonskode = "BI",
                ),
            ),
        )

        val response = bisysService.hentSøknad(HentSøknadRequest(søknadsid = søknad.søknadsid!!))

        response.søknad.behandlingStatusType shouldBe BehandlingStatusType.UNDER_BEHANDLING
    }

    @Test
    fun `hent søknad som ikke er bidragssøknad skal ha BehandlingStatusType Under behandling og tom liste med søknadslinjer`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadstype = "FA", søknadFraKode = "ET"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            blankettid = blankett.blankettid!!,
                            søknadsgruppekode = "FA",
                            saksnummer = SAKSNUMMER_1,
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[1].rolleid!!,
                    søknadsstatuskode = "UB",
                    saksnummer = SAKSNUMMER_1,
                    innbetaltBeløp = null,
                    gruppeKombinasjonskode = "FA",
                ),
            ),
        )

        val response = bisysService.hentSøknad(HentSøknadRequest(søknadsid = søknad.søknadsid!!))

        response.søknad.behandlingStatusType shouldBe BehandlingStatusType.UNDER_BEHANDLING
        response.søknad.behandlingstema shouldBe Behandlingstema.FARSSKAP
        response.søknad.partISøknadListe.size shouldBe 3
    }

    @Test
    fun `hent søknad - gebyr, test at referansen kommer i responsen`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
                opprettKodeSøknadStatus(kode = "VF", lukketStatus = "1"),
                opprettKodeSøknadStatus(kode = "FR", lukketStatus = "1"),
                opprettKodeSøknadStatus(kode = "TR", lukketStatus = "1"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadstype = "FF", søknadFraKode = "ET"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            blankettid = blankett.blankettid!!,
                            søknadsgruppekode = "GB",
                            saksnummer = SAKSNUMMER_1,
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[0].rolleid!!,
                    søknadsstatuskode = "UB",
                    saksnummer = SAKSNUMMER_1,
                    innbetaltBeløp = null,
                    gruppeKombinasjonskode = "GB",
                    referanseGebyr = "Referanse1",
                ),
            ),
        )

        val response = bisysService.hentSøknad(HentSøknadRequest(søknadsid = søknad.søknadsid!!))

        response.søknad.behandlingStatusType shouldBe BehandlingStatusType.UNDER_BEHANDLING
        response.søknad.partISøknadListe
            .first()
            .referanseGebyr shouldBe "Referanse1"
    }

    @Test
    fun `skal feilregistrere søknadslinje for barn og lage hendelse når alle linjer er feilregistrert`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
                opprettKodeSøknadStatus(kode = "FR", lukketStatus = "1"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "BM", søknadstype = "FF"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            blankettid = blankett.blankettid!!,
                            søknadMottattDato = LocalDate.parse("2024-02-01"),
                            søknadFomDato = LocalDate.parse("2024-01-01"),
                            søknadsgruppekode = "BI",
                            saksnummer = SAKSNUMMER_1,
                            behandlingsid = "1",
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "BI",
                    saksnummer = SAKSNUMMER_1,
                ),
            ),
        )

        val request = FeilregistrerSøknadsBarnRequest(søknadsid = søknad.søknadsid!!, personidentBarn = PERSONIDENT_BARN_1)
        bisysService.feilregistrerSøknadsbarn(request)

        val oppdaterteSøknadslinjer = testdataManager.hentSøknadslinjerForSøknadMedId(søknad.søknadsid!!)
        val hendelser = testdataManager.hentHendelserForSøknadMedId(søknad.søknadsid!!)

        assertSoftly {
            oppdaterteSøknadslinjer!!.first().søknadStatuskode shouldBe "FR"
            hendelser!!.shouldHaveSize(1)
            hendelser.first().hendelsestype shouldBe "FR"
        }
    }

    @Test
    fun `skal feilregistrere kun søknadslinje for barn uten å lage hendelse når andre linjer ikke er feilregistrert`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
                opprettKodeSøknadStatus(kode = "FR", lukketStatus = "1"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "BM", søknadstype = "FF"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            blankettid = blankett.blankettid!!,
                            søknadMottattDato = LocalDate.parse("2024-02-01"),
                            søknadFomDato = LocalDate.parse("2024-01-01"),
                            søknadsgruppekode = "BI",
                            saksnummer = SAKSNUMMER_1,
                            behandlingsid = "1",
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "BI",
                    saksnummer = SAKSNUMMER_1,
                ),
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[3].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "BI",
                    saksnummer = SAKSNUMMER_1,
                ),
            ),
        )

        val request = FeilregistrerSøknadsBarnRequest(søknadsid = søknad.søknadsid!!, personidentBarn = PERSONIDENT_BARN_1)
        bisysService.feilregistrerSøknadsbarn(request)

        val oppdaterteSøknadslinjer = testdataManager.hentSøknadslinjerForSøknadMedId(søknad.søknadsid!!)
        val hendelser = testdataManager.hentHendelserForSøknadMedId(søknad.søknadsid!!)

        assertSoftly {
            oppdaterteSøknadslinjer!!.first { it.rolleid == roller[2].rolleid }.søknadStatuskode shouldBe "FR"
            oppdaterteSøknadslinjer.first { it.rolleid == roller[3].rolleid }.søknadStatuskode shouldBe "UB"
            hendelser shouldBe emptyList()
        }
    }

    @Test
    fun `skal feilregistrere tilhørende innkrevingssøknad når alle søknadslinjer i begge søknader er feilregistrert`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
                opprettKodeSøknadStatus(kode = "FR", lukketStatus = "1"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "BM", søknadstype = "FF"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            blankettid = blankett.blankettid!!,
                            søknadMottattDato = LocalDate.parse("2024-02-01"),
                            søknadFomDato = LocalDate.parse("2024-01-01"),
                            søknadsgruppekode = "BI",
                            saksnummer = SAKSNUMMER_1,
                            behandlingsid = "1",
                        ),
                    ),
                ).first()

        val innkrevingssøknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            blankettid = blankett.blankettid!!,
                            søknadMottattDato = LocalDate.parse("2024-02-01"),
                            søknadFomDato = LocalDate.parse("2024-01-01"),
                            søknadsgruppekode = "IK",
                            saksnummer = SAKSNUMMER_1,
                            behandlingsid = "1",
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "BI",
                    saksnummer = SAKSNUMMER_1,
                ),
                opprettSøknadslinje(
                    søknadsid = innkrevingssøknad.søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "IK",
                    saksnummer = SAKSNUMMER_1,
                ),
            ),
        )

        val request = FeilregistrerSøknadsBarnRequest(søknadsid = søknad.søknadsid!!, personidentBarn = PERSONIDENT_BARN_1)
        bisysService.feilregistrerSøknadsbarn(request)

        val oppdaterteSøknadslinjer = testdataManager.hentSøknadslinjerForSøknadMedId(søknad.søknadsid!!)
        val oppdaterteInnkrevingssøknadslinjer = testdataManager.hentSøknadslinjerForSøknadMedId(innkrevingssøknad.søknadsid!!)

        assertSoftly {
            oppdaterteSøknadslinjer!!.first().søknadStatuskode shouldBe "FR"
            oppdaterteInnkrevingssøknadslinjer!!.first().søknadStatuskode shouldBe "FR"
        }
    }

    @Test
    fun `skal kaste HttpClientErrorException når søknad ikke finnes for feilregistrering av søknadsbarn`() {
        val request = FeilregistrerSøknadsBarnRequest(søknadsid = 999L, personidentBarn = PERSONIDENT_BARN_1)

        val exception =
            shouldThrow<HttpClientErrorException> {
                bisysService.feilregistrerSøknadsbarn(request)
            }

        exception.message shouldBe "400 Fant ikke søknad med id: 999"
    }

    @Test
    fun `skal kaste HttpClientErrorException når rolle for barn ikke finnes for feilregistrering av søknadsbarn`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        testdataManager.lagreRoller(
            listOf(
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
            ),
        )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "BM", søknadstype = "FF"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            blankettid = blankett.blankettid!!,
                            søknadsgruppekode = "BI",
                            saksnummer = SAKSNUMMER_1,
                        ),
                    ),
                ).first()

        val request = FeilregistrerSøknadsBarnRequest(søknadsid = søknad.søknadsid!!, personidentBarn = PERSONIDENT_BARN_1)

        val exception =
            shouldThrow<HttpClientErrorException> {
                bisysService.feilregistrerSøknadsbarn(request)
            }

        exception.message shouldBe "400 Fant ikke rolle for barn med personident: $PERSONIDENT_BARN_1"
    }

    @Test
    fun `skal oppdatere referanse på gebyrsøknad når søknad eksisterer`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankett.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()

        val gebyrsøknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankett.blankettid!!,
                            søknadsgruppekode = "GB",
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "BI",
                    saksnummer = SAKSNUMMER_1,
                ),
            ),
        )

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = gebyrsøknad.søknadsid!!,
                    rolleid = roller[0].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "GB",
                    saksnummer = SAKSNUMMER_1,
                ),
            ),
        )

        val request =
            OppdaterReferanseGebyrRequest(
                søknadsid = søknad.søknadsid!!,
                personident = PERSONIDENT_BP_1,
                referanse = "NY_REFERANSE_123",
            )

        bisysService.oppdaterReferanseGebyr(request)

        val oppdatertSøknadslinje =
            testdataManager
                .hentSøknadslinjerForSøknadMedId(gebyrsøknad.søknadsid!!)!!
                .first()

        oppdatertSøknadslinje.engangsbeløpReferanse shouldBe "NY_REFERANSE_123"
    }

    @Test
    fun `skal kaste HttpClientErrorException når søknad ikke finnes for oppdatering av referanse gebyr`() {
        val request =
            OppdaterReferanseGebyrRequest(
                søknadsid = 999L,
                personident = PERSONIDENT_BP_1,
                referanse = "REFERANSE",
            )

        val exception =
            shouldThrow<HttpClientErrorException> {
                bisysService.oppdaterReferanseGebyr(request)
            }

        exception.message shouldBe "400 Fant ikke søknad med id: 999"
    }

    @Test
    fun `skal kaste HttpClientErrorException når søknad er lukket ved oppdatering av referanse gebyr`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "VF", lukketStatus = "1"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankett.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[0].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "VF",
                    gruppeKombinasjonskode = "BI",
                    saksnummer = SAKSNUMMER_1,
                ),
            ),
        )

        val request =
            OppdaterReferanseGebyrRequest(
                søknadsid = søknad.søknadsid!!,
                personident = PERSONIDENT_BP_1,
                referanse = "REFERANSE",
            )

        val exception =
            shouldThrow<HttpClientErrorException> {
                bisysService.oppdaterReferanseGebyr(request)
            }

        exception.message shouldBe
            "400 Referanse på gebyr kan ikke oppdateres hvis søknaden er lukket. " +
            "søknadsid: ${søknad.søknadsid} personident: $PERSONIDENT_BP_1"
    }

    @Test
    fun `skal kaste HttpClientErrorException når gebyrsøknad ikke finnes ved oppdatering av referanse`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankett.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[0].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "BI",
                    saksnummer = SAKSNUMMER_1,
                ),
            ),
        )

        val request =
            OppdaterReferanseGebyrRequest(
                søknadsid = søknad.søknadsid!!,
                personident = PERSONIDENT_BP_1,
                referanse = "REFERANSE",
            )

        val exception =
            shouldThrow<HttpClientErrorException> {
                bisysService.oppdaterReferanseGebyr(request)
            }

        exception.message shouldBe "400 Fant ikke gebyrsøknad tilknyttet søknad med id: ${søknad.søknadsid}"
    }

    @Test
    fun `skal kaste HttpClientErrorException når søknadslinje ikke finnes for personident i gebyrsøknad`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        val roller =
            testdataManager.lagreRoller(
                listOf(
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                    opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                ),
            )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val søknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankett.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()

        val gebyrsøknad =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankett.blankettid!!,
                            søknadsgruppekode = "GB",
                        ),
                    ),
                ).first()

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = søknad.søknadsid!!,
                    rolleid = roller[2].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "BI",
                    saksnummer = SAKSNUMMER_1,
                ),
            ),
        )

        testdataManager.lagreSøknadslinjeListe(
            listOf(
                opprettSøknadslinje(
                    søknadsid = gebyrsøknad.søknadsid!!,
                    rolleid = roller[0].rolleid!!,
                    innbetaltBeløp = null,
                    søknadsstatuskode = "UB",
                    gruppeKombinasjonskode = "GB",
                    saksnummer = SAKSNUMMER_1,
                ),
            ),
        )

        val request =
            OppdaterReferanseGebyrRequest(
                søknadsid = søknad.søknadsid!!,
                personident = PERSONIDENT_BM_1,
                referanse = "REFERANSE",
            )

        val exception =
            shouldThrow<HttpClientErrorException> {
                bisysService.oppdaterReferanseGebyr(request)
            }

        exception.message shouldBe
            "400 Fant ikke søknadslinje for personident: $PERSONIDENT_BM_1 i gebyrsøknad med id: ${gebyrsøknad.søknadsid}"
    }

    @Test
    fun `oppretter ikke ny søknad når matchende søknad finnes fra før`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
                opprettKodeSøknadStatus(kode = "VF", lukketStatus = "1"),
                opprettKodeSøknadStatus(kode = "TR", lukketStatus = "1"),
            ),
        )

        testdataManager.lagreRoller(
            listOf(
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
            ),
        )

        val request1 =
            OpprettSøknadRequest(
                saksnummer = SAKSNUMMER_1,
                behandlingsid = 1L,
                behandlerenhet = "enhet1",
                behandlingstema = Behandlingstema.BIDRAG,
                søknadFomDato = LocalDate.now(),
                innkreving = true,
                barnListe =
                listOf(
                    Barn(
                        personident = PERSONIDENT_BARN_1,
                    ),
                ),
                behandlingstype = null,
            )

        val request2 =
            OpprettSøknadRequest(
                saksnummer = SAKSNUMMER_1,
                behandlingsid = 1L,
                behandlerenhet = "enhet1",
                behandlingstema = Behandlingstema.BIDRAG,
                søknadFomDato = LocalDate.now(),
                innkreving = true,
                barnListe =
                listOf(
                    Barn(
                        personident = PERSONIDENT_BARN_1,
                    ),
                    Barn(
                        personident = PERSONIDENT_BARN_2,
                    ),
                ),
                behandlingstype = null,
            )

        val søknad1 = bisysService.opprettSøknader(request1)
        val søknad2 = bisysService.opprettSøknader(request2)

        søknad2.søknadsid shouldBe søknad1.søknadsid
    }

    @Test
    fun `oppretter ny søknad når matchende søknad er feilregistrert`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
                opprettKodeSøknadStatus(kode = "VF", lukketStatus = "1"),
                opprettKodeSøknadStatus(kode = "TR", lukketStatus = "1"),
            ),
        )

        testdataManager.lagreRoller(
            listOf(
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
            ),
        )

        val request1 =
            OpprettSøknadRequest(
                saksnummer = SAKSNUMMER_1,
                behandlingsid = 1L,
                behandlerenhet = "enhet1",
                behandlingstema = Behandlingstema.BIDRAG,
                søknadFomDato = LocalDate.now(),
                innkreving = true,
                barnListe =
                listOf(
                    Barn(
                        personident = PERSONIDENT_BARN_1,
                    ),
                ),
                behandlingstype = null,
            )

        val request2 =
            OpprettSøknadRequest(
                saksnummer = SAKSNUMMER_1,
                behandlingsid = 1L,
                behandlerenhet = "enhet1",
                behandlingstema = Behandlingstema.BIDRAG,
                søknadFomDato = LocalDate.now(),
                innkreving = true,
                barnListe =
                listOf(
                    Barn(
                        personident = PERSONIDENT_BARN_1,
                    ),
                    Barn(
                        personident = PERSONIDENT_BARN_2,
                    ),
                ),
                behandlingstype = null,
            )

        val søknad1 = bisysService.opprettSøknader(request1)
        bisysService.feilregistrerSøknad(FeilregistrerSøknadRequest(søknadsid = søknad1.søknadsid))
        val søknad2 = bisysService.opprettSøknader(request2)

        søknad2.søknadsid shouldNotBe søknad1.søknadsid
    }

    @Test
    fun `oppretter ny søknad når request inneholder nytt barn`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
                opprettKodeSøknadStatus(kode = "VF", lukketStatus = "1"),
                opprettKodeSøknadStatus(kode = "TR", lukketStatus = "1"),
            ),
        )

        testdataManager.lagreRoller(
            listOf(
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
            ),
        )

        val request1 =
            OpprettSøknadRequest(
                saksnummer = SAKSNUMMER_1,
                behandlingsid = 1L,
                behandlerenhet = "enhet1",
                behandlingstema = Behandlingstema.BIDRAG,
                søknadFomDato = LocalDate.now(),
                innkreving = true,
                barnListe =
                listOf(
                    Barn(
                        personident = PERSONIDENT_BARN_1,
                    ),
                ),
                behandlingstype = null,
            )

        val request2 =
            OpprettSøknadRequest(
                saksnummer = SAKSNUMMER_1,
                behandlingsid = 1L,
                behandlerenhet = "enhet1",
                behandlingstema = Behandlingstema.BIDRAG,
                søknadFomDato = LocalDate.now(),
                innkreving = true,
                barnListe =
                listOf(
                    Barn(
                        personident = PERSONIDENT_BARN_2,
                    ),
                ),
                behandlingstype = null,
            )

        val søknad1 = bisysService.opprettSøknader(request1)
        val søknad2 = bisysService.opprettSøknader(request2)

        søknad1.søknadsid shouldNotBe søknad2.søknadsid
    }

    @Test
    fun `oppretter ny søknad når matchende åpen søknad finnes, men med annen verdi i innkreving`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
                opprettKodeSøknadStatus(kode = "VF", lukketStatus = "1"),
                opprettKodeSøknadStatus(kode = "TR", lukketStatus = "1"),
            ),
        )

        testdataManager.lagreRoller(
            listOf(
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
            ),
        )

        val request1 =
            OpprettSøknadRequest(
                saksnummer = SAKSNUMMER_1,
                behandlingsid = 1L,
                behandlerenhet = "enhet1",
                behandlingstema = Behandlingstema.BIDRAG,
                søknadFomDato = LocalDate.now(),
                innkreving = true,
                barnListe =
                listOf(
                    Barn(
                        personident = PERSONIDENT_BARN_1,
                    ),
                ),
                behandlingstype = null,
            )

        val request2 =
            OpprettSøknadRequest(
                saksnummer = SAKSNUMMER_1,
                behandlingsid = 1L,
                behandlerenhet = "enhet1",
                behandlingstema = Behandlingstema.BIDRAG,
                søknadFomDato = LocalDate.now(),
                innkreving = false,
                barnListe =
                listOf(
                    Barn(
                        personident = PERSONIDENT_BARN_1,
                    ),
                ),
                behandlingstype = null,
            )

        val søknad1 = bisysService.opprettSøknader(request1)
        val søknad2 = bisysService.opprettSøknader(request2)

        // Id til eksisterende søknad returneres siden matchende søknad allerede finnes
        søknad1.søknadsid shouldNotBe søknad2.søknadsid
    }

    @Test
    fun `oppretter ny søknad når matchende søknad finnes, men med annen verdi i innkreving`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
                opprettKodeSøknadStatus(kode = "VF", lukketStatus = "1"),
                opprettKodeSøknadStatus(kode = "TR", lukketStatus = "1"),
            ),
        )

        testdataManager.lagreRoller(
            listOf(
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
            ),
        )

        val request1 =
            OpprettSøknadRequest(
                saksnummer = SAKSNUMMER_1,
                behandlingsid = 1L,
                behandlerenhet = "enhet1",
                behandlingstema = Behandlingstema.BIDRAG,
                behandlingstype = Behandlingstype.FORHOLDSMESSIG_FORDELING,
                søknadFomDato = LocalDate.now(),
                innkreving = false,
                barnListe =
                listOf(
                    Barn(
                        personident = PERSONIDENT_BARN_1,
                    ),
                ),
            )

        val request2 =
            OpprettSøknadRequest(
                saksnummer = SAKSNUMMER_1,
                behandlingsid = 1L,
                behandlerenhet = "enhet1",
                behandlingstema = Behandlingstema.BIDRAG,
                behandlingstype = Behandlingstype.FORHOLDSMESSIG_FORDELING,
                søknadFomDato = LocalDate.now(),
                innkreving = true,
                barnListe =
                listOf(
                    Barn(
                        personident = PERSONIDENT_BARN_1,
                    ),
                ),
            )

        val søknad1 = bisysService.opprettSøknader(request1)

        val søknad2 = bisysService.opprettSøknader(request2)

        søknad2.søknadsid shouldNotBe søknad1.søknadsid
    }

    @Test
    fun `skal sammenknytte søknader når de ikke er sammenknyttet fra før`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        testdataManager.lagreRoller(
            listOf(
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BM_2, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
                opprettRolle(saksnummer = SAKSNUMMER_3, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_3, fnr = PERSONIDENT_BM_3, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_3, fnr = PERSONIDENT_BARN_3, rolletype = "BA"),
            ),
        )

        val blankettHovedsøknad =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val blankettReferertSøknad1 =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_2, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val blankettReferertSøknad2 =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_3, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val hovedsøknadsid =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankettHovedsøknad.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        val referertSøknadsid1 =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_2,
                            blankettid = blankettReferertSøknad1.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        val request1 =
            SammenknyttSøknaderRequest(
                hovedsøknadsid = hovedsøknadsid!!,
                referertSøknadsid = referertSøknadsid1!!,
            )

        bisysService.sammenknyttSøknader(request1)

        val referertSøknadsid2 =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_3,
                            blankettid = blankettReferertSøknad2.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        val request2 =
            SammenknyttSøknaderRequest(
                hovedsøknadsid = hovedsøknadsid,
                referertSøknadsid = referertSøknadsid2!!,
            )

        bisysService.sammenknyttSøknader(request2)

        val sammenknytningerHovedsøknad =
            testdataManager.hentSøknadsknytningerHovedsøknad(hovedsøknadsid = hovedsøknadsid).sortedBy { it.referertSøknadsid }
        val sammenknytningerReferertSøknad1 = testdataManager.hentSøknadsknytningReferertSøknad(referertSøknadsid = referertSøknadsid1)
        val sammenknytningerReferertSøknad2 = testdataManager.hentSøknadsknytningReferertSøknad(referertSøknadsid = referertSøknadsid2)

        assertSoftly {
            sammenknytningerHovedsøknad.shouldHaveSize(2)
            sammenknytningerHovedsøknad[0].hovedsøknadsid shouldBe hovedsøknadsid
            sammenknytningerHovedsøknad[0].referertSøknadsid shouldBe referertSøknadsid1
            sammenknytningerHovedsøknad[0].status shouldBe SøknadsknytningStatus.Aktiv.name

            sammenknytningerHovedsøknad[1].hovedsøknadsid shouldBe hovedsøknadsid
            sammenknytningerHovedsøknad[1].referertSøknadsid shouldBe referertSøknadsid2
            sammenknytningerHovedsøknad[1].status shouldBe SøknadsknytningStatus.Aktiv.name

            sammenknytningerReferertSøknad1.shouldHaveSize(1)
            sammenknytningerReferertSøknad1.first().hovedsøknadsid shouldBe hovedsøknadsid
            sammenknytningerReferertSøknad1.first().referertSøknadsid shouldBe referertSøknadsid1
            sammenknytningerReferertSøknad1.first().status shouldBe SøknadsknytningStatus.Aktiv.name

            sammenknytningerReferertSøknad2.shouldHaveSize(1)
            sammenknytningerReferertSøknad2.first().hovedsøknadsid shouldBe hovedsøknadsid
            sammenknytningerReferertSøknad2.first().referertSøknadsid shouldBe referertSøknadsid2
            sammenknytningerReferertSøknad2.first().status shouldBe SøknadsknytningStatus.Aktiv.name
        }
    }

    @Test
    fun `skal ikke opprette ny sammenknytning når søknader allerede er sammenknyttet`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        testdataManager.lagreRoller(
            listOf(
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
            ),
        )

        val blankett =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val søknader =
            testdataManager.lagreSøknadListe(
                listOf(
                    opprettSøknad(
                        saksnummer = SAKSNUMMER_1,
                        blankettid = blankett.blankettid!!,
                        søknadsgruppekode = "BI",
                    ),
                    opprettSøknad(
                        saksnummer = SAKSNUMMER_1,
                        blankettid = blankett.blankettid!!,
                        søknadsgruppekode = "BI",
                    ),
                ),
            )

        val request =
            SammenknyttSøknaderRequest(
                hovedsøknadsid = søknader[0].søknadsid!!,
                referertSøknadsid = søknader[1].søknadsid!!,
            )

        bisysService.sammenknyttSøknader(request)
        bisysService.sammenknyttSøknader(request)

        val sammenknytninger = testdataManager.hentSøknadsknytningerHovedsøknad(søknader[0].søknadsid!!)

        sammenknytninger.shouldHaveSize(1)
    }

    @Test
    fun `skal slette sammenknytning for referert søknad1`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        testdataManager.lagreRoller(
            listOf(
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BM_2, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
                opprettRolle(saksnummer = SAKSNUMMER_3, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_3, fnr = PERSONIDENT_BM_3, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_3, fnr = PERSONIDENT_BARN_3, rolletype = "BA"),
            ),
        )

        val blankettHovedsøknad =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val blankettReferertSøknad1 =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val blankettReferertSøknad2 =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val hovedsøknadsid =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankettHovedsøknad.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        val referertSøknadsid1 =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_2,
                            blankettid = blankettReferertSøknad1.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        val referertSøknadsid2 =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_3,
                            blankettid = blankettReferertSøknad2.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        // Knytt mot første søknad
        val request1 =
            SammenknyttSøknaderRequest(
                hovedsøknadsid = hovedsøknadsid!!,
                referertSøknadsid = referertSøknadsid1!!,
            )

        bisysService.sammenknyttSøknader(request1)

        // Knytt mot andre søknad
        val request2 =
            SammenknyttSøknaderRequest(
                hovedsøknadsid = hovedsøknadsid,
                referertSøknadsid = referertSøknadsid2!!,
            )

        bisysService.sammenknyttSøknader(request2)

        bisysService.slettSammenknytningReferertSøknad(SlettSammenknytningForSøknadRequest(søknadsid = referertSøknadsid1))

        val sammenknytninger = testdataManager.hentSøknadsknytningerHovedsøknad(hovedsøknadsid)

        assertSoftly {
            sammenknytninger.shouldHaveSize(1)
            sammenknytninger.count { it.status == SøknadsknytningStatus.Slettet.name } shouldBe 0
            sammenknytninger.count { it.status == SøknadsknytningStatus.Aktiv.name } shouldBe 1
        }
    }

    @Test
    fun `skal slette sammenknytninger for hovedsøknad og opprette sammenknytninger mot ny hovedsøknad`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        testdataManager.lagreRoller(
            listOf(
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BM_2, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
                opprettRolle(saksnummer = SAKSNUMMER_3, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_3, fnr = PERSONIDENT_BM_3, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_3, fnr = PERSONIDENT_BARN_3, rolletype = "BA"),
            ),
        )

        val blankettHovedsøknad =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val blankettNyHovedsøknad =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_2, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val blankettReferertSøknad1 =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val blankettReferertSøknad2 =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val hovedsøknadsid =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankettHovedsøknad.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        val nyHovedsøknadsid =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_2,
                            blankettid = blankettNyHovedsøknad.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        val referertSøknadsid1 =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_2,
                            blankettid = blankettReferertSøknad1.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        val referertSøknadsid2 =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_3,
                            blankettid = blankettReferertSøknad2.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        // Knytt mot første søknad
        val request1 =
            SammenknyttSøknaderRequest(
                hovedsøknadsid = hovedsøknadsid!!,
                referertSøknadsid = referertSøknadsid1!!,
            )

        bisysService.sammenknyttSøknader(request1)

        // Knytt mot andre søknad
        val request2 =
            SammenknyttSøknaderRequest(
                hovedsøknadsid = hovedsøknadsid,
                referertSøknadsid = referertSøknadsid2!!,
            )

        bisysService.sammenknyttSøknader(request2)

        // Sletter knytninger mot gammel hovedsøknad og oppretter knytninger mot ny hovedsøknad
        bisysService.slettSammenknytningerHovedsøknad(
            SlettHovedsøknadRequest(eksisterendeHovedsøknadsid = hovedsøknadsid, nyHovedsøknadsid = nyHovedsøknadsid),
        )

        val sammenknytningerSlettetHovedsøknad = testdataManager.hentSøknadsknytningerHovedsøknad(hovedsøknadsid)

        val sammenknytningerNyHovedsøknad =
            testdataManager
                .hentSøknadsknytningerHovedsøknad(
                    nyHovedsøknadsid!!,
                ).sortedBy { it.referertSøknadsid }

        assertSoftly {
            sammenknytningerSlettetHovedsøknad.shouldHaveSize(0)

            sammenknytningerNyHovedsøknad.shouldHaveSize(2)

            sammenknytningerNyHovedsøknad[0].hovedsøknadsid shouldBe nyHovedsøknadsid
            sammenknytningerNyHovedsøknad[0].referertSøknadsid shouldBe referertSøknadsid1
            sammenknytningerNyHovedsøknad[0].status shouldBe SøknadsknytningStatus.Aktiv.name

            sammenknytningerNyHovedsøknad[1].hovedsøknadsid shouldBe nyHovedsøknadsid
            sammenknytningerNyHovedsøknad[1].referertSøknadsid shouldBe referertSøknadsid2
            sammenknytningerNyHovedsøknad[1].status shouldBe SøknadsknytningStatus.Aktiv.name
        }
    }

    @Test
    fun `skal slette sammenknytning for referert søknad`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        testdataManager.lagreRoller(
            listOf(
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BM_2, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
                opprettRolle(saksnummer = SAKSNUMMER_3, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_3, fnr = PERSONIDENT_BM_3, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_3, fnr = PERSONIDENT_BARN_3, rolletype = "BA"),
            ),
        )

        val blankettHovedsøknad =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val blankettReferertSøknad1 =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val blankettReferertSøknad2 =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val hovedsøknadsid =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankettHovedsøknad.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        val referertSøknadsid1 =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_2,
                            blankettid = blankettReferertSøknad1.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        val referertSøknadsid2 =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_3,
                            blankettid = blankettReferertSøknad2.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        // Knytt mot første søknad
        val request1 =
            SammenknyttSøknaderRequest(
                hovedsøknadsid = hovedsøknadsid!!,
                referertSøknadsid = referertSøknadsid1!!,
            )

        bisysService.sammenknyttSøknader(request1)

        // Knytt mot andre søknad
        val request2 =
            SammenknyttSøknaderRequest(
                hovedsøknadsid = hovedsøknadsid,
                referertSøknadsid = referertSøknadsid2!!,
            )

        bisysService.sammenknyttSøknader(request2)

        bisysService.slettSammenknytningReferertSøknad(SlettSammenknytningForSøknadRequest(søknadsid = referertSøknadsid1))

        val sammenknytningerHovedsøknad = testdataManager.hentSøknadsknytningerHovedsøknad(hovedsøknadsid)
        val sammenknytningerReferertSøknad = testdataManager.hentSøknadsknytningReferertSøknad(referertSøknadsid1)

        assertSoftly {
            sammenknytningerHovedsøknad.shouldHaveSize(1)
            sammenknytningerHovedsøknad.count { it.status == SøknadsknytningStatus.Aktiv.name } shouldBe 1

            sammenknytningerReferertSøknad.shouldHaveSize(0)
        }
    }

    @Test
    fun `skal endre sammenknytning for referert søknad`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        testdataManager.lagreRoller(
            listOf(
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BM_2, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
                opprettRolle(saksnummer = SAKSNUMMER_3, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_3, fnr = PERSONIDENT_BM_3, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_3, fnr = PERSONIDENT_BARN_3, rolletype = "BA"),
            ),
        )

        val blankettHovedsøknad1 =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val blankettReferertSøknad1 =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_2, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val blankettHovedsøknad2 =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_3, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val hovedsøknadsid1 =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankettHovedsøknad1.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        val referertSøknadsid1 =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_2,
                            blankettid = blankettReferertSøknad1.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        val hovedsøknadsid2 =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_3,
                            blankettid = blankettHovedsøknad2.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        val request1 =
            SammenknyttSøknaderRequest(
                hovedsøknadsid = hovedsøknadsid1!!,
                referertSøknadsid = referertSøknadsid1!!,
            )

        bisysService.sammenknyttSøknader(request1)

        bisysService.endreSammenknytningSøknad(
            SammenknyttSøknaderRequest(hovedsøknadsid = hovedsøknadsid2!!, referertSøknadsid = referertSøknadsid1),
        )

        val sammenknytningerHovedsøknad1 = testdataManager.hentSøknadsknytningerHovedsøknad(hovedsøknadsid1)
        val sammenknytningerHovedsøknad2 = testdataManager.hentSøknadsknytningerHovedsøknad(hovedsøknadsid2)
        val slettetSammenknytningerReferertSøknad = testdataManager.hentSøknadsknytningReferertSøknad(referertSøknadsid1, "Slettet")
        val sammenknytningerReferertSøknad = testdataManager.hentSøknadsknytningReferertSøknad(referertSøknadsid1)

        assertSoftly {
            sammenknytningerHovedsøknad1.shouldHaveSize(0)

            slettetSammenknytningerReferertSøknad.shouldHaveSize(1)
            slettetSammenknytningerReferertSøknad.count { it.status == SøknadsknytningStatus.Slettet.name } shouldBe 1
            slettetSammenknytningerReferertSøknad.count { it.status == SøknadsknytningStatus.Aktiv.name } shouldBe 0

            sammenknytningerReferertSøknad.shouldHaveSize(1)
            sammenknytningerReferertSøknad.count { it.status == SøknadsknytningStatus.Slettet.name } shouldBe 0
            sammenknytningerReferertSøknad.count { it.status == SøknadsknytningStatus.Aktiv.name } shouldBe 1

            sammenknytningerHovedsøknad2.count { it.status == SøknadsknytningStatus.Slettet.name } shouldBe 0
            sammenknytningerHovedsøknad2.count { it.status == SøknadsknytningStatus.Aktiv.name } shouldBe 1
        }
    }

    @Test
    fun `skal finne sammenknytninger for hovedsøknad`() {
        testdataManager.lagreKodeSøknadsstatus(
            listOf(
                opprettKodeSøknadStatus(kode = "UB", lukketStatus = "0"),
            ),
        )

        testdataManager.lagreRoller(
            listOf(
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BM_1, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_1, fnr = PERSONIDENT_BARN_1, rolletype = "BA"),
                opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BM_2, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_2, fnr = PERSONIDENT_BARN_2, rolletype = "BA"),
                opprettRolle(saksnummer = SAKSNUMMER_3, fnr = PERSONIDENT_BP_1, rolletype = "BP"),
                opprettRolle(saksnummer = SAKSNUMMER_3, fnr = PERSONIDENT_BM_3, rolletype = "BM"),
                opprettRolle(saksnummer = SAKSNUMMER_3, fnr = PERSONIDENT_BARN_3, rolletype = "BA"),
            ),
        )

        val blankettHovedsøknad =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val blankettReferertSøknad1 =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val blankettReferertSøknad2 =
            testdataManager
                .lagreBlankettListe(
                    listOf(
                        opprettBlankett(saksnummer = SAKSNUMMER_1, søknadFraKode = "ET", søknadstype = "FF"),
                    ),
                ).first()

        val hovedsøknadsid =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_1,
                            blankettid = blankettHovedsøknad.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        val referertSøknadsid1 =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_2,
                            blankettid = blankettReferertSøknad1.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        val referertSøknadsid2 =
            testdataManager
                .lagreSøknadListe(
                    listOf(
                        opprettSøknad(
                            saksnummer = SAKSNUMMER_3,
                            blankettid = blankettReferertSøknad2.blankettid!!,
                            søknadsgruppekode = "BI",
                        ),
                    ),
                ).first()
                .søknadsid

        // Knytt mot første søknad
        val request1 =
            SammenknyttSøknaderRequest(
                hovedsøknadsid = hovedsøknadsid!!,
                referertSøknadsid = referertSøknadsid1!!,
            )

        bisysService.sammenknyttSøknader(request1)

        // Knytt mot andre søknad
        val request2 =
            SammenknyttSøknaderRequest(
                hovedsøknadsid = hovedsøknadsid,
                referertSøknadsid = referertSøknadsid2!!,
            )

        bisysService.sammenknyttSøknader(request2)

        val sammenknytninger =
            bisysService
                .finnSammenknytningerHovedsøknad(
                    FinnSammenknytningerHovedsøknadRequest(søknadsid = hovedsøknadsid),
                ).søknader
                .sortedBy { it.søknadsid }
//
//        val sammenknytninger = testdataManager.hentSøknadsknytningerHovedsøknad(hovedsøknadsid)

        assertSoftly {
            sammenknytninger.shouldHaveSize(2)
            sammenknytninger[0].søknadsid shouldBe referertSøknadsid1
            sammenknytninger[1].søknadsid shouldBe referertSøknadsid2
        }
    }
}

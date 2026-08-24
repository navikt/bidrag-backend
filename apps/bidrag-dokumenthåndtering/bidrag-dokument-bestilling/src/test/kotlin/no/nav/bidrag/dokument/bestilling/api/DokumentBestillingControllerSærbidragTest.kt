package no.nav.bidrag.dokument.bestilling.api

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.bidrag.dokument.bestilling.api.dto.DokumentBestillingForespørsel
import no.nav.bidrag.dokument.bestilling.api.dto.DokumentBestillingResponse
import no.nav.bidrag.dokument.bestilling.bestilling.dto.hentDokumentMal
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto.BrevBestilling
import no.nav.bidrag.dokument.bestilling.consumer.dto.fornavnEtternavn
import no.nav.bidrag.dokument.bestilling.model.tilBisysResultatkodeForBrev
import no.nav.bidrag.dokument.bestilling.utils.ANNEN_MOTTAKER
import no.nav.bidrag.dokument.bestilling.utils.BARN1
import no.nav.bidrag.dokument.bestilling.utils.BARN2
import no.nav.bidrag.dokument.bestilling.utils.BM1
import no.nav.bidrag.dokument.bestilling.utils.BP1
import no.nav.bidrag.dokument.bestilling.utils.FASTSETTELSE_GEBYR_2024
import no.nav.bidrag.dokument.bestilling.utils.FORSKUDDSATS_2024_2025
import no.nav.bidrag.dokument.bestilling.utils.FORSKUDD_INNTEKTGRENSE_2024_2025
import no.nav.bidrag.dokument.bestilling.utils.MULTIPLIKATOR_MAKS_INNTEKT_FORSKUDD_MOTTAKER_2024
import no.nav.bidrag.dokument.bestilling.utils.SAK_OPPRETTET_DATO
import no.nav.bidrag.dokument.bestilling.utils.createEnhetKontaktInformasjon
import no.nav.bidrag.dokument.bestilling.utils.createPostAdresseResponse
import no.nav.bidrag.dokument.bestilling.utils.opprettBehandlingDetaljer
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.transport.dokumentmaler.PeriodeFraTom
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate

class DokumentBestillingControllerSærbidragTest : AbstractControllerTest() {
    @Test
    fun `skal produsere XML for varselbrev særbidrag`() {
        stubDefaultValues()
        stubUtils.stubHentPerson("16451299577", ANNEN_MOTTAKER)
        stubUtils.stubHentPerson("25451755601", ANNEN_MOTTAKER)
        val behandlingResponse =
            opprettBehandlingDetaljer()
        stubUtils.stubHentBehandling(behandlingResponse)
        val enhetKontaktInfo = createEnhetKontaktInformasjon()
        val bmAdresse = createPostAdresseResponse()

        val dokumentMal = hentDokumentMal("BI01S04")!!
        val tittel = "Tittel på dokument"
        val saksnummer = "123213"
        val mottakerId = BM1.ident
        val gjelderId = BP1.ident

        stubUtils.stubHentAdresse(postAdresse = bmAdresse)
        stubUtils.stubEnhetKontaktInfo(enhetKontaktInfo)

        val request =
            DokumentBestillingForespørsel(
                mottakerId = mottakerId.verdi,
                gjelderId = gjelderId.verdi,
                saksnummer = saksnummer,
                tittel = tittel,
                behandlingId = 12312,
                enhet = "4806",
                spraak = "NB",
                dokumentreferanse = "BIF1234567",
            )

        jmsTestConsumer.withOnlinebrev {
            val response =
                httpHeaderTestRestTemplate.exchange(
                    "${rootUri()}/bestill/${dokumentMal.kode}",
                    HttpMethod.POST,
                    HttpEntity(request),
                    DokumentBestillingResponse::class.java,
                )

            response.statusCode shouldBe HttpStatus.OK

            val message: BrevBestilling = this.getMessageAsObject(BrevBestilling::class.java)!!
            assertSoftly {
                verifyBrevbestillingHeaders(message, dokumentMal)
                message.validateKontaktInformasjon(enhetKontaktInfo, BM1, BP1, bmAdresse)

                message.brev?.parter?.bmkravkfremav shouldBe ""
                message.brev?.parter?.bmgebyr shouldBe null
                message.brev?.parter?.bmlandkode shouldBe ""
                message.brev?.parter?.bpkravfremav shouldBe ""
                message.brev?.parter?.bpgebyr shouldBe null
                message.brev?.parter?.bplandkode shouldBe ""
                message.brev?.parter?.bmdatodod shouldBe null
                message.brev?.parter?.bpdatodod shouldBe null

                message.brev?.barnISak?.shouldHaveSize(1)

                val barnISak1 = message.brev?.barnISak!!.first()
                barnISak1.fDato shouldBe BARN1.fødselsdato
                barnISak1.fnr shouldBe BARN1.ident.verdi
                barnISak1.navn shouldBe BARN1.fornavnEtternavn()
                barnISak1.personIdRm shouldBe ""
                barnISak1.belopGebyrRm shouldBe ""
                barnISak1.belForskudd shouldBe null
                barnISak1.belBidrag shouldBe null

                message.brev?.soknadBost?.saksnr shouldBe saksnummer
                message.brev?.soknadBost?.sakstype shouldBe "E"
                message.brev?.soknadBost?.hgKode shouldBe "SB"
                message.brev?.soknadBost?.ugKode shouldBe "E"
                message.brev?.soknadBost?.resKode shouldBe ""
                message.brev?.soknadBost?.rmISak shouldBe false
                message.brev?.soknadBost?.gebyrsats shouldBe FASTSETTELSE_GEBYR_2024.toBigDecimal()
                message.brev?.soknadBost?.sendtDato shouldBe LocalDate.now()

                message.brev?.saksbehandler?.navn shouldBe "Saksbehandler Mellomnavn Saksbehandlersen"

                // Valider forskudd vedtak resultater
                val soknadDato = LocalDate.parse("2024-07-15")

                val soknad = message.brev?.soknad!!
                soknad.soknDato shouldBe soknadDato
                soknad.type shouldBe "SB"
                soknad.aarsakKd shouldBe ""
                soknad.undergrp shouldBe "E"
                soknad.saksnr shouldBe saksnummer
                soknad.sendtDato shouldBe null
                soknad.vedtattDato shouldBe null
                soknad.virkningDato shouldBe null

                val soknadBost = message.brev?.soknadBost!!
                soknadBost.hgKode shouldBe "SB"
                soknadBost.ugKode shouldBe "E"
                soknadBost.datoSakReg shouldBe SAK_OPPRETTET_DATO
                soknadBost.gebyrsats shouldBe FASTSETTELSE_GEBYR_2024.toBigDecimal().setScale(1)
                soknadBost.virkningsDato shouldBe null
                soknadBost.sendtDato shouldBe LocalDate.now()
                soknadBost.mottatDato shouldBe soknadDato
                soknadBost.soknGrKode shouldBe "ST"
                soknadBost.resKode shouldBe ""
                soknadBost.soknFraKode shouldBe "MO"
                soknadBost.soknType shouldBe "EN"

                message.brev?.vedtak!! shouldHaveSize 0
                message.brev?.bidragBarn!! shouldHaveSize 0
                message.brev?.inntektGrunnlagForskuddPerioder!! shouldHaveSize 8

                assertSoftly(message.brev!!.sjablon!!) {
                    val forskuddSats = FORSKUDDSATS_2024_2025.toBigDecimal()
                    forskuddSats shouldBe forskuddSats
                    multiplikatorInntekstgrenseForskudd shouldBe MULTIPLIKATOR_MAKS_INNTEKT_FORSKUDD_MOTTAKER_2024.toBigDecimal()
                    maksForskuddsgrense shouldBe forskuddSats * MULTIPLIKATOR_MAKS_INNTEKT_FORSKUDD_MOTTAKER_2024.toBigDecimal()

                    inntektTillegsbidrag!! shouldBeGreaterThan BigDecimal.ZERO
                    maksProsentInntektBp!! shouldBeGreaterThan BigDecimal.ZERO
                    multiplikatorHøyInntektBp!! shouldBeGreaterThan BigDecimal.ZERO
                    multiplikatorMaksBidrag!! shouldBeGreaterThan BigDecimal.ZERO
                    multiplikatorMaksInntekBarn!! shouldBeGreaterThan BigDecimal.ZERO
                    nedreInntekstgrenseGebyr!! shouldBeGreaterThan BigDecimal.ZERO
                    maksgrenseHøyInntekt!! shouldBeGreaterThan BigDecimal.ZERO
                    maksBidragsgrense!! shouldBeGreaterThan BigDecimal.ZERO
                    maksInntektsgrense!! shouldBeGreaterThan BigDecimal.ZERO
                    maksInntektsgebyr!! shouldBeGreaterThan BigDecimal.ZERO
                    prosentTillegsgebyr!! shouldBe BigDecimal.ZERO
                }

                stubUtils.Verify().verifyHentEnhetKontaktInfoCalledWith()
                stubUtils.Verify().verifyBehandlingKalt()
                stubUtils.Verify().verifyHentPersonCalled(BM1.ident.verdi)
                stubUtils.Verify().verifyHentPersonCalled(BP1.ident.verdi)
                stubUtils.Verify().verifyHentPersonCalled(BARN1.ident.verdi)
            }
        }
    }

    @Test
    fun `skal produsere XML for særbidrag vedtakbrev med flere perioder`() {
        stubDefaultValues()
        stubUtils.stubHentPerson("16451299577", ANNEN_MOTTAKER)
        stubUtils.stubHentPerson("25451755601", ANNEN_MOTTAKER)
        stubUtils.stubHentVedtak("vedtak_response-særbidrag.json")
        val enhetKontaktInfo = createEnhetKontaktInformasjon()
        val bmAdresse = createPostAdresseResponse()
        val dokumentMal = hentDokumentMal("BI01E01")!!
        val tittel = "Tittel på dokument"
        val saksnummer = "123213"
        val mottakerId = BM1.ident
        val gjelderId = BP1.ident

        stubUtils.stubHentAdresse(postAdresse = bmAdresse)
        stubUtils.stubEnhetKontaktInfo(enhetKontaktInfo)

        val request =
            DokumentBestillingForespørsel(
                mottakerId = mottakerId.verdi,
                gjelderId = gjelderId.verdi,
                saksnummer = saksnummer,
                tittel = tittel,
                vedtakId = 12312,
                enhet = "4806",
                spraak = "NB",
                dokumentreferanse = "BIF1234567",
            )

        jmsTestConsumer.withOnlinebrev {
            val response =
                httpHeaderTestRestTemplate.exchange(
                    "${rootUri()}/bestill/${dokumentMal.kode}",
                    HttpMethod.POST,
                    HttpEntity(request),
                    DokumentBestillingResponse::class.java,
                )

            response.statusCode shouldBe HttpStatus.OK

            val særbidragVerdi = BigDecimal(1909)
            val message: BrevBestilling = this.getMessageAsObject(BrevBestilling::class.java)!!
            assertSoftly {
                verifyBrevbestillingHeaders(message, dokumentMal)
                message.validateKontaktInformasjon(enhetKontaktInfo, BM1, BP1, bmAdresse)

                message.brev?.parter?.bmkravkfremav shouldBe ""
                message.brev?.parter?.bmgebyr shouldBe null
                message.brev?.parter?.bmlandkode shouldBe ""
                message.brev?.parter?.bpkravfremav shouldBe ""
                message.brev?.parter?.bpgebyr shouldBe null
                message.brev?.parter?.bplandkode shouldBe ""
                message.brev?.parter?.bmdatodod shouldBe null
                message.brev?.parter?.bpdatodod shouldBe null

                message.brev?.barnISak?.shouldHaveSize(1)

                val barnISak1 = message.brev?.barnISak!!.first()
                barnISak1.fDato shouldBe BARN2.fødselsdato
                barnISak1.fnr shouldBe BARN2.ident.verdi
                barnISak1.navn shouldBe BARN2.fornavnEtternavn()
                barnISak1.personIdRm shouldBe ""
                barnISak1.belopGebyrRm shouldBe ""
                barnISak1.belForskudd shouldBe null
                barnISak1.belBidrag shouldBe null

                message.brev?.soknadBost?.saksnr shouldBe saksnummer
                message.brev?.soknadBost?.sakstype shouldBe "E"
                message.brev?.soknadBost?.hgKode shouldBe "SB"
                message.brev?.soknadBost?.ugKode shouldBe "S"
                message.brev?.soknadBost?.resKode shouldBe Resultatkode.SÆRBIDRAG_INNVILGET.legacyKode
                message.brev?.soknadBost?.rmISak shouldBe false
                message.brev?.soknadBost?.gebyrsats shouldBe FASTSETTELSE_GEBYR_2024.toBigDecimal()
                message.brev?.soknadBost?.sendtDato shouldBe LocalDate.now()

                message.brev?.saksbehandler?.navn shouldBe "Saksbehandler Mellomnavn Saksbehandlersen"

                val virkningDato = LocalDate.parse("2024-08-01")
                val periode1 = PeriodeFraTom(virkningDato, LocalDate.parse("2024-08-31"))

                // Valider forskudd vedtak resultater
                val soknadDato = LocalDate.parse("2024-01-01")

                val soknad = message.brev?.soknad!!
                soknad.soknDato shouldBe soknadDato
                soknad.type shouldBe "SB"
                soknad.aarsakKd shouldBe ""
                soknad.undergrp shouldBe "S"
                soknad.saksnr shouldBe saksnummer
                soknad.sendtDato shouldBe LocalDate.parse("2024-08-06")
                soknad.vedtattDato shouldBe LocalDate.parse("2024-08-06")
                soknad.virkningDato shouldBe virkningDato

                val soknadBost = message.brev?.soknadBost!!
                soknadBost.hgKode shouldBe "SB"
                soknadBost.ugKode shouldBe "S"
                soknadBost.datoSakReg shouldBe SAK_OPPRETTET_DATO
                soknadBost.gebyrsats shouldBe FASTSETTELSE_GEBYR_2024.toBigDecimal().setScale(1)
                soknadBost.virkningsDato shouldBe virkningDato
                soknadBost.mottatDato shouldBe soknadDato
                soknadBost.soknGrKode shouldBe "ST"
                soknadBost.resKode shouldBe Resultatkode.SÆRBIDRAG_INNVILGET.legacyKode
                soknadBost.soknFraKode shouldBe "MO"
                soknadBost.soknType shouldBe "FA"

                message.brev?.vedtak!! shouldHaveSize 1
                message.brev?.bidragBarn!! shouldHaveSize 1
                val barn1 = message?.brev?.bidragBarn!![0]
                barn1.barn!!.fnr shouldBe BARN2.ident.verdi
                barn1.barn!!.saksnr shouldBe saksnummer

                assertSoftly(message.brev!!.vedtak.filter { it.fnr == BARN2.ident.verdi }) {
                    shouldHaveSize(1)
                    val vedtakPeriode1 = this[0]
                    vedtakPeriode1.belopBidrag shouldBe særbidragVerdi.setScale(2)
                    vedtakPeriode1.fomDato shouldBe periode1.fraDato
                    vedtakPeriode1.tomDato shouldBe periode1.tomDato
                    vedtakPeriode1.fnr shouldBe BARN2.ident.verdi
                    vedtakPeriode1.erInnkreving shouldBe true
                    vedtakPeriode1.resultatKode shouldBe Resultatkode.SÆRBIDRAG_INNVILGET.legacyKode
                }

                message.brev?.forskuddVedtakPeriode!!.size shouldBe 0

                barn1.inntektPerioder shouldHaveSize 8
                val inntekterPeriode1 = barn1.inntektPerioder
                assertSoftly(inntekterPeriode1.filter { it.rolle == "02" }) {
                    shouldHaveSize(2)
                    this[0].inntektGrense shouldBe FORSKUDD_INNTEKTGRENSE_2024_2025
                    this[0].belopType shouldBe "AINNTEKT_BEREGNET_12MND"
                    this[0].beskrivelse shouldBe "Opplysninger fra arbeidsgiver"
                    this[0].belopÅrsinntekt shouldBe BigDecimal(858000)
                    this[0].rolle shouldBe "02"

                    this[1].inntektGrense shouldBe FORSKUDD_INNTEKTGRENSE_2024_2025
                    this[1].belopType shouldBe "XINN"
                    this[1].beskrivelse shouldBe "Personens beregningsgrunnlag i perioden"
                    this[1].belopÅrsinntekt shouldBe BigDecimal(858000)
                    this[1].rolle shouldBe "02"
                }
                assertSoftly(inntekterPeriode1.filter { it.rolle == "01" }) {
                    shouldHaveSize(4)
                    this[0].fomDato shouldBe periode1.fraDato
                    this[0].tomDato shouldBe periode1.tomDato
                    this[0].inntektGrense shouldBe FORSKUDD_INNTEKTGRENSE_2024_2025
                    this[0].belopType shouldBe "SP"
                    this[0].beskrivelse shouldBe "Ytelse fra det offentlige"
                    this[0].belopÅrsinntekt shouldBe BigDecimal(94466)
                    this[0].rolle shouldBe "01"

                    this[3].fomDato shouldBe periode1.fraDato
                    this[3].tomDato shouldBe periode1.tomDato
                    this[3].inntektGrense shouldBe FORSKUDD_INNTEKTGRENSE_2024_2025
                    this[3].belopType shouldBe "XINN"
                    this[3].beskrivelse shouldBe "Personens beregningsgrunnlag i perioden"
                    this[3].belopÅrsinntekt shouldBe BigDecimal(699466)
                    this[3].rolle shouldBe "01"
                }
                assertSoftly(inntekterPeriode1.filter { it.rolle == "04" }) {
                    shouldHaveSize(2)
                    this[0].fomDato shouldBe periode1.fraDato
                    this[0].tomDato shouldBe periode1.tomDato
                    this[0].inntektGrense shouldBe FORSKUDD_INNTEKTGRENSE_2024_2025
                    this[0].belopType shouldBe "MDOK"
                    this[0].beskrivelse shouldBe "SkjÃ¸nn - mangler dokumentasjon"
                    this[0].belopÅrsinntekt shouldBe BigDecimal(30000)
                    this[0].rolle shouldBe "04"

                    this[1].fomDato shouldBe periode1.fraDato
                    this[1].tomDato shouldBe periode1.tomDato
                    this[1].inntektGrense shouldBe FORSKUDD_INNTEKTGRENSE_2024_2025
                    this[1].belopType shouldBe "XINN"
                    this[1].beskrivelse shouldBe "Personens beregningsgrunnlag i perioden"
                    this[1].belopÅrsinntekt shouldBe BigDecimal(30000)
                    this[1].rolle shouldBe "04"
                }
                barn1.forskuddSivilstandPerioder shouldHaveSize 0
                barn1.forskuddBarnPerioder shouldHaveSize 0
                barn1.særbidrag.shouldNotBeNull()

                assertSoftly(barn1.særbidrag!!) {
                    antTermin shouldBe 1
                    beløpSøkt shouldBe BigDecimal(11000)
                    beløpGodkjent shouldBe BigDecimal(7000)
                    fratrekk shouldBe BigDecimal(1234)
                    beløpSærbidrag shouldBe særbidragVerdi
                    beløpForskudd shouldBe BigDecimal(1970)
                    beløpInntektsgrense shouldBe BigDecimal(59100)
                    bpInntekt shouldBe BigDecimal(699466)
                    bmInntekt shouldBe BigDecimal(858000)
                    bbInntekt shouldBe BigDecimal(30000)
                    sumInntekt shouldBe BigDecimal(1587466)
                    fordNokkel shouldBe BigDecimal("449.1")
                }

                assertSoftly(barn1.særbidragPeriode) {
                    this.shouldHaveSize(1)
                    this[0].fomDato shouldBe periode1.fraDato
                    this[0].tomDato shouldBe periode1.tomDato
                    this[0].beløp shouldBe særbidragVerdi
                }

                barn1.inntektGrunnlagForskuddPerioder shouldHaveSize 0 // 4 inntektgrenser for enslig, 4 for gift/samboer. Inntekgrenser vises for alle 3 perioder = total 8*3

                stubUtils.Verify().verifyHentEnhetKontaktInfoCalledWith()
                stubUtils.Verify().verifyHentPersonCalled(BM1.ident.verdi)
                stubUtils.Verify().verifyHentPersonCalled(BP1.ident.verdi)
                stubUtils.Verify().verifyHentPersonCalled(BARN2.ident.verdi)
            }
        }
    }

    @Test
    fun `skal produsere XML for særbidrag vedtakbrev for direkte avslag`() {
        stubDefaultValues()
        stubUtils.stubHentPerson("16451299577", ANNEN_MOTTAKER)
        stubUtils.stubHentPerson("25451755601", ANNEN_MOTTAKER)
        stubUtils.stubHentVedtak("vedtak_response-særbidrag-direkte_avslag.json")
        val enhetKontaktInfo = createEnhetKontaktInformasjon()
        val bmAdresse = createPostAdresseResponse()
        val dokumentMal = hentDokumentMal("BI01E01")!!
        val tittel = "Tittel på dokument"
        val saksnummer = "123213"
        val mottakerId = BM1.ident
        val gjelderId = BP1.ident

        stubUtils.stubHentAdresse(postAdresse = bmAdresse)
        stubUtils.stubEnhetKontaktInfo(enhetKontaktInfo)

        val request =
            DokumentBestillingForespørsel(
                mottakerId = mottakerId.verdi,
                gjelderId = gjelderId.verdi,
                saksnummer = saksnummer,
                tittel = tittel,
                vedtakId = 12312,
                enhet = "4806",
                spraak = "NB",
                dokumentreferanse = "BIF1234567",
            )

        jmsTestConsumer.withOnlinebrev {
            val response =
                httpHeaderTestRestTemplate.exchange(
                    "${rootUri()}/bestill/${dokumentMal.kode}",
                    HttpMethod.POST,
                    HttpEntity(request),
                    DokumentBestillingResponse::class.java,
                )

            response.statusCode shouldBe HttpStatus.OK

            val message: BrevBestilling = this.getMessageAsObject(BrevBestilling::class.java)!!
            assertSoftly {
                verifyBrevbestillingHeaders(message, dokumentMal)
                message.validateKontaktInformasjon(enhetKontaktInfo, BM1, BP1, bmAdresse)

                message.brev?.parter?.bmkravkfremav shouldBe ""
                message.brev?.parter?.bmgebyr shouldBe null
                message.brev?.parter?.bmlandkode shouldBe ""
                message.brev?.parter?.bpkravfremav shouldBe ""
                message.brev?.parter?.bpgebyr shouldBe null
                message.brev?.parter?.bplandkode shouldBe ""
                message.brev?.parter?.bmdatodod shouldBe null
                message.brev?.parter?.bpdatodod shouldBe null

                message.brev?.barnISak?.shouldHaveSize(1)

                val barnISak1 = message.brev?.barnISak!!.first()
                barnISak1.fDato shouldBe BARN2.fødselsdato
                barnISak1.fnr shouldBe BARN2.ident.verdi
                barnISak1.navn shouldBe BARN2.fornavnEtternavn()
                barnISak1.personIdRm shouldBe ""
                barnISak1.belopGebyrRm shouldBe ""
                barnISak1.belForskudd shouldBe null
                barnISak1.belBidrag shouldBe null

                message.brev?.soknadBost?.saksnr shouldBe saksnummer
                message.brev?.soknadBost?.resKode shouldBe Resultatkode.AVSLAG_PRIVAT_AVTALE_OM_SÆRBIDRAG.tilBisysResultatkodeForBrev(Vedtakstype.FASTSETTELSE)
                message.brev?.soknadBost?.rmISak shouldBe false
                message.brev?.soknadBost?.gebyrsats shouldBe FASTSETTELSE_GEBYR_2024.toBigDecimal()
                message.brev?.soknadBost?.sendtDato shouldBe LocalDate.now()

                message.brev?.saksbehandler?.navn shouldBe "Saksbehandler Mellomnavn Saksbehandlersen"

                val virkningDato = LocalDate.parse("2024-08-01")

                // Valider forskudd vedtak resultater
                val soknadDato = LocalDate.parse("2024-01-15")

                val soknad = message.brev?.soknad!!
                soknad.soknDato shouldBe soknadDato
                soknad.type shouldBe "SB"
                soknad.aarsakKd shouldBe Resultatkode.PRIVAT_AVTALE.legacyKode
                soknad.undergrp shouldBe "S"
                soknad.saksnr shouldBe saksnummer
                soknad.sendtDato shouldBe LocalDate.parse("2024-08-27")
                soknad.vedtattDato shouldBe LocalDate.parse("2024-08-27")
                soknad.virkningDato shouldBe virkningDato

                val soknadBost = message.brev?.soknadBost!!
                soknadBost.hgKode shouldBe "SB"
                soknadBost.ugKode shouldBe "S"
                soknadBost.datoSakReg shouldBe SAK_OPPRETTET_DATO
                soknadBost.gebyrsats shouldBe FASTSETTELSE_GEBYR_2024.toBigDecimal().setScale(1)
                soknadBost.virkningsDato shouldBe virkningDato
                soknadBost.mottatDato shouldBe soknadDato
                soknadBost.soknGrKode shouldBe "ST"
                soknadBost.resKode shouldBe Resultatkode.AVSLAG_PRIVAT_AVTALE_OM_SÆRBIDRAG.tilBisysResultatkodeForBrev(Vedtakstype.FASTSETTELSE)
                soknadBost.soknFraKode shouldBe "PL"
                soknadBost.soknType shouldBe "FA"

                message.brev?.vedtak!! shouldHaveSize 0
                message.brev?.bidragBarn!! shouldHaveSize 1
                val barn1 = message?.brev?.bidragBarn!![0]
                barn1.barn!!.fnr shouldBe BARN2.ident.verdi
                barn1.barn!!.saksnr shouldBe saksnummer
                message.brev!!.vedtak shouldHaveSize 0
                message.brev?.forskuddVedtakPeriode!!.size shouldBe 0
                barn1.inntektPerioder shouldHaveSize 0
                barn1.forskuddSivilstandPerioder shouldHaveSize 0
                barn1.forskuddBarnPerioder shouldHaveSize 0
                barn1.særbidrag.shouldNotBeNull()
                barn1.særbidragPeriode shouldHaveSize 0
                assertSoftly(barn1.særbidrag!!) {
                    antTermin shouldBe null
                }

                barn1.inntektGrunnlagForskuddPerioder shouldHaveSize 0

                stubUtils.Verify().verifyHentEnhetKontaktInfoCalledWith()
                stubUtils.Verify().verifyHentPersonCalled(BM1.ident.verdi)
                stubUtils.Verify().verifyHentPersonCalled(BP1.ident.verdi)
                stubUtils.Verify().verifyHentPersonCalled(BARN2.ident.verdi)
            }
        }
    }

    @Test
    fun `skal produsere XML for særbidrag vedtakbrev for avslag alle utgifter foreldet`() {
        stubDefaultValues()
        stubUtils.stubHentPerson("16451299577", ANNEN_MOTTAKER)
        stubUtils.stubHentPerson("25451755601", ANNEN_MOTTAKER)
        stubUtils.stubHentVedtak("vedtak_response-særbidrag-utgifter_foreldet.json")
        val enhetKontaktInfo = createEnhetKontaktInformasjon()
        val bmAdresse = createPostAdresseResponse()
        val dokumentMal = hentDokumentMal("BI01E01")!!
        val tittel = "Tittel på dokument"
        val saksnummer = "123213"
        val mottakerId = BM1.ident
        val gjelderId = BP1.ident

        stubUtils.stubHentAdresse(postAdresse = bmAdresse)
        stubUtils.stubEnhetKontaktInfo(enhetKontaktInfo)

        val request =
            DokumentBestillingForespørsel(
                mottakerId = mottakerId.verdi,
                gjelderId = gjelderId.verdi,
                saksnummer = saksnummer,
                tittel = tittel,
                vedtakId = 12312,
                enhet = "4806",
                spraak = "NB",
                dokumentreferanse = "BIF1234567",
            )

        jmsTestConsumer.withOnlinebrev {
            val response =
                httpHeaderTestRestTemplate.exchange(
                    "${rootUri()}/bestill/${dokumentMal.kode}",
                    HttpMethod.POST,
                    HttpEntity(request),
                    DokumentBestillingResponse::class.java,
                )

            response.statusCode shouldBe HttpStatus.OK

            val message: BrevBestilling = this.getMessageAsObject(BrevBestilling::class.java)!!
            assertSoftly {
                verifyBrevbestillingHeaders(message, dokumentMal)
                message.validateKontaktInformasjon(enhetKontaktInfo, BM1, BP1, bmAdresse)

                message.brev?.parter?.bmkravkfremav shouldBe ""
                message.brev?.parter?.bmgebyr shouldBe null
                message.brev?.parter?.bmlandkode shouldBe ""
                message.brev?.parter?.bpkravfremav shouldBe ""
                message.brev?.parter?.bpgebyr shouldBe null
                message.brev?.parter?.bplandkode shouldBe ""
                message.brev?.parter?.bmdatodod shouldBe null
                message.brev?.parter?.bpdatodod shouldBe null

                message.brev?.barnISak?.shouldHaveSize(1)

                val barnISak1 = message.brev?.barnISak!!.first()
                barnISak1.fDato shouldBe BARN2.fødselsdato
                barnISak1.fnr shouldBe BARN2.ident.verdi
                barnISak1.navn shouldBe BARN2.fornavnEtternavn()
                barnISak1.personIdRm shouldBe ""
                barnISak1.belopGebyrRm shouldBe ""
                barnISak1.belForskudd shouldBe null
                barnISak1.belBidrag shouldBe null

                message.brev?.soknadBost?.saksnr shouldBe saksnummer
                message.brev?.soknadBost?.resKode shouldBe Resultatkode.ALLE_UTGIFTER_ER_FORELDET.legacyKode
                message.brev?.soknadBost?.rmISak shouldBe false
                message.brev?.soknadBost?.gebyrsats shouldBe FASTSETTELSE_GEBYR_2024.toBigDecimal()
                message.brev?.soknadBost?.sendtDato shouldBe LocalDate.now()

                message.brev?.saksbehandler?.navn shouldBe "Saksbehandler Mellomnavn Saksbehandlersen"

                val virkningDato = LocalDate.parse("2024-08-01")
                val periode1 = PeriodeFraTom(virkningDato, LocalDate.parse("2024-08-31"))
                // Valider forskudd vedtak resultater
                val soknadDato = LocalDate.parse("2024-07-13")

                val soknad = message.brev?.soknad!!
                soknad.soknDato shouldBe soknadDato
                soknad.type shouldBe "SB"
                soknad.aarsakKd shouldBe Resultatkode.ALLE_UTGIFTER_ER_FORELDET.legacyKode
                soknad.undergrp shouldBe "S"
                soknad.saksnr shouldBe saksnummer
                soknad.sendtDato shouldBe LocalDate.parse("2024-08-09")
                soknad.vedtattDato shouldBe LocalDate.parse("2024-08-09")
                soknad.virkningDato shouldBe virkningDato

                val soknadBost = message.brev?.soknadBost!!
                soknadBost.hgKode shouldBe "SB"
                soknadBost.ugKode shouldBe "S"
                soknadBost.datoSakReg shouldBe SAK_OPPRETTET_DATO
                soknadBost.gebyrsats shouldBe FASTSETTELSE_GEBYR_2024.toBigDecimal().setScale(1)
                soknadBost.virkningsDato shouldBe virkningDato
                soknadBost.mottatDato shouldBe soknadDato
                soknadBost.soknGrKode shouldBe "ST"
                soknadBost.resKode shouldBe Resultatkode.ALLE_UTGIFTER_ER_FORELDET.legacyKode
                soknadBost.soknFraKode shouldBe "MO"
                soknadBost.soknType shouldBe "FA"

                message.brev?.vedtak!! shouldHaveSize 1
                message.brev?.bidragBarn!! shouldHaveSize 1
                val barn1 = message?.brev?.bidragBarn!![0]
                barn1.barn!!.fnr shouldBe BARN2.ident.verdi
                barn1.barn!!.saksnr shouldBe saksnummer
                message.brev!!.vedtak shouldHaveSize 1
                message.brev?.forskuddVedtakPeriode!!.size shouldBe 0
                barn1.inntektPerioder shouldHaveSize 0
                barn1.forskuddSivilstandPerioder shouldHaveSize 0
                barn1.forskuddBarnPerioder shouldHaveSize 0
                barn1.særbidrag.shouldNotBeNull()
                barn1.særbidragPeriode shouldHaveSize 1
                assertSoftly(barn1.særbidrag!!) {
                    antTermin shouldBe 1
                    beløpSøkt shouldBe BigDecimal(13000)
                    beløpGodkjent shouldBe BigDecimal(0)
                    beløpSærbidrag shouldBe BigDecimal(0)
                    beløpForskudd shouldBe BigDecimal(1970)
                    beløpInntektsgrense shouldBe BigDecimal(59100)
                    bpInntekt shouldBe BigDecimal(0)
                    bmInntekt shouldBe BigDecimal(0)
                    bbInntekt shouldBe BigDecimal(0)
                    sumInntekt shouldBe BigDecimal(0)
                    fordNokkel shouldBe BigDecimal("0.0")
                }
                assertSoftly(barn1.særbidragPeriode) {
                    this.shouldHaveSize(1)
                    this[0].fomDato shouldBe periode1.fraDato
                    this[0].tomDato shouldBe periode1.tomDato
                    this[0].beløp shouldBe BigDecimal(0)
                }

                barn1.inntektGrunnlagForskuddPerioder shouldHaveSize 0

                stubUtils.Verify().verifyHentEnhetKontaktInfoCalledWith()
                stubUtils.Verify().verifyHentPersonCalled(BM1.ident.verdi)
                stubUtils.Verify().verifyHentPersonCalled(BP1.ident.verdi)
                stubUtils.Verify().verifyHentPersonCalled(BARN2.ident.verdi)
            }
        }
    }

    @Test
    fun `skal produsere XML for særbidrag vedtakbrev for avslag lavere enn forskuddssats`() {
        stubDefaultValues()
        stubUtils.stubHentPerson("16451299577", ANNEN_MOTTAKER)
        stubUtils.stubHentPerson("25451755601", ANNEN_MOTTAKER)
        stubUtils.stubHentVedtak("vedtak_response-særbidrag-avslag_lavere_enn_forskudd.json")
        val enhetKontaktInfo = createEnhetKontaktInformasjon()
        val bmAdresse = createPostAdresseResponse()
        val dokumentMal = hentDokumentMal("BI01E01")!!
        val tittel = "Tittel på dokument"
        val saksnummer = "123213"
        val mottakerId = BM1.ident
        val gjelderId = BP1.ident

        stubUtils.stubHentAdresse(postAdresse = bmAdresse)
        stubUtils.stubEnhetKontaktInfo(enhetKontaktInfo)

        val request =
            DokumentBestillingForespørsel(
                mottakerId = mottakerId.verdi,
                gjelderId = gjelderId.verdi,
                saksnummer = saksnummer,
                tittel = tittel,
                vedtakId = 12312,
                enhet = "4806",
                spraak = "NB",
                dokumentreferanse = "BIF1234567",
            )

        jmsTestConsumer.withOnlinebrev {
            val response =
                httpHeaderTestRestTemplate.exchange(
                    "${rootUri()}/bestill/${dokumentMal.kode}",
                    HttpMethod.POST,
                    HttpEntity(request),
                    DokumentBestillingResponse::class.java,
                )

            response.statusCode shouldBe HttpStatus.OK

            val message: BrevBestilling = this.getMessageAsObject(BrevBestilling::class.java)!!
            assertSoftly {
                verifyBrevbestillingHeaders(message, dokumentMal)
                message.validateKontaktInformasjon(enhetKontaktInfo, BM1, BP1, bmAdresse)

                message.brev?.parter?.bmkravkfremav shouldBe ""
                message.brev?.parter?.bmgebyr shouldBe null
                message.brev?.parter?.bmlandkode shouldBe ""
                message.brev?.parter?.bpkravfremav shouldBe ""
                message.brev?.parter?.bpgebyr shouldBe null
                message.brev?.parter?.bplandkode shouldBe ""
                message.brev?.parter?.bmdatodod shouldBe null
                message.brev?.parter?.bpdatodod shouldBe null

                message.brev?.barnISak?.shouldHaveSize(1)

                val barnISak1 = message.brev?.barnISak!!.first()
                barnISak1.fDato shouldBe BARN2.fødselsdato
                barnISak1.fnr shouldBe BARN2.ident.verdi
                barnISak1.navn shouldBe BARN2.fornavnEtternavn()
                barnISak1.personIdRm shouldBe ""
                barnISak1.belopGebyrRm shouldBe ""
                barnISak1.belForskudd shouldBe null
                barnISak1.belBidrag shouldBe null

                message.brev?.soknadBost?.saksnr shouldBe saksnummer
                message.brev?.soknadBost?.resKode shouldBe Resultatkode.GODKJENT_BELØP_ER_LAVERE_ENN_FORSKUDDSSATS.legacyKode
                message.brev?.soknadBost?.rmISak shouldBe false
                message.brev?.soknadBost?.gebyrsats shouldBe FASTSETTELSE_GEBYR_2024.toBigDecimal()
                message.brev?.soknadBost?.sendtDato shouldBe LocalDate.now()

                message.brev?.saksbehandler?.navn shouldBe "Saksbehandler Mellomnavn Saksbehandlersen"

                val virkningDato = LocalDate.parse("2024-08-01")
                val periode1 = PeriodeFraTom(virkningDato, LocalDate.parse("2024-08-31"))

                // Valider forskudd vedtak resultater
                val soknadDato = LocalDate.parse("2024-07-04")

                val soknad = message.brev?.soknad!!
                soknad.soknDato shouldBe soknadDato
                soknad.type shouldBe "SB"
                soknad.aarsakKd shouldBe ""
                soknad.undergrp shouldBe "E"
                soknad.saksnr shouldBe saksnummer
                soknad.sendtDato shouldBe LocalDate.parse("2024-08-09")
                soknad.vedtattDato shouldBe LocalDate.parse("2024-08-09")
                soknad.virkningDato shouldBe virkningDato

                val soknadBost = message.brev?.soknadBost!!
                soknadBost.hgKode shouldBe "SB"
                soknadBost.ugKode shouldBe "E"
                soknadBost.datoSakReg shouldBe SAK_OPPRETTET_DATO
                soknadBost.gebyrsats shouldBe FASTSETTELSE_GEBYR_2024.toBigDecimal().setScale(1)
                soknadBost.virkningsDato shouldBe virkningDato
                soknadBost.mottatDato shouldBe soknadDato
                soknadBost.soknGrKode shouldBe "ST"
                soknadBost.resKode shouldBe Resultatkode.GODKJENT_BELØP_ER_LAVERE_ENN_FORSKUDDSSATS.legacyKode
                soknadBost.soknFraKode shouldBe "MO"
                soknadBost.soknType shouldBe "EN"

                message.brev?.vedtak!! shouldHaveSize 1
                assertSoftly(message.brev?.vedtak!!) {
                    this[0].belopBidrag shouldBe BigDecimal(0).setScale(2)
                    this[0].fomDato shouldBe LocalDate.parse("2024-08-01")
                    this[0].tomDato shouldBe LocalDate.parse("2024-08-31")
                    this[0].fnr shouldBe BARN2.ident.verdi
                    this[0].erInnkreving shouldBe true
                    this[0].resultatKode shouldBe Resultatkode.GODKJENT_BELØP_ER_LAVERE_ENN_FORSKUDDSSATS.legacyKode
                }
                message.brev?.bidragBarn!! shouldHaveSize 1
                val barn1 = message?.brev?.bidragBarn!![0]
                barn1.barn!!.fnr shouldBe BARN2.ident.verdi
                barn1.barn!!.saksnr shouldBe saksnummer
                message.brev?.forskuddVedtakPeriode!!.size shouldBe 0
                barn1.inntektPerioder shouldHaveSize 0
                barn1.forskuddSivilstandPerioder shouldHaveSize 0
                barn1.forskuddBarnPerioder shouldHaveSize 0
                barn1.særbidrag.shouldNotBeNull()
                assertSoftly(barn1.særbidrag!!) {
                    antTermin shouldBe 1
                    beløpSøkt shouldBe BigDecimal(30000)
                    beløpGodkjent shouldBe BigDecimal(300)
                    beløpSærbidrag shouldBe BigDecimal(0)
                    beløpForskudd shouldBe BigDecimal(1240)
                    beløpInntektsgrense shouldBe BigDecimal(59100)
                    bpInntekt shouldBe BigDecimal(0)
                    bmInntekt shouldBe BigDecimal(0)
                    bbInntekt shouldBe BigDecimal(0)
                    sumInntekt shouldBe BigDecimal(0)
                    fordNokkel shouldBe BigDecimal("0.0")
                }
                assertSoftly(barn1.særbidragPeriode) {
                    this.shouldHaveSize(1)
                    this[0].fomDato shouldBe periode1.fraDato
                    this[0].tomDato shouldBe periode1.tomDato
                    this[0].beløp shouldBe BigDecimal(0)
                }
                barn1.inntektGrunnlagForskuddPerioder shouldHaveSize 0

                stubUtils.Verify().verifyHentEnhetKontaktInfoCalledWith()
                stubUtils.Verify().verifyHentPersonCalled(BM1.ident.verdi)
                stubUtils.Verify().verifyHentPersonCalled(BP1.ident.verdi)
                stubUtils.Verify().verifyHentPersonCalled(BARN2.ident.verdi)
            }
        }
    }
}

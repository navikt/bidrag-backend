package no.nav.bidrag.dokument.bestilling.api

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import no.nav.bidrag.dokument.bestilling.api.dto.DokumentBestillingForespørsel
import no.nav.bidrag.dokument.bestilling.api.dto.DokumentBestillingResponse
import no.nav.bidrag.dokument.bestilling.bestilling.dto.hentDokumentMal
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto.BrevBestilling
import no.nav.bidrag.dokument.bestilling.consumer.dto.fornavnEtternavn
import no.nav.bidrag.dokument.bestilling.model.MAX_DATE
import no.nav.bidrag.dokument.bestilling.utils.ANNEN_MOTTAKER
import no.nav.bidrag.dokument.bestilling.utils.BARN1
import no.nav.bidrag.dokument.bestilling.utils.BARN2
import no.nav.bidrag.dokument.bestilling.utils.BM1
import no.nav.bidrag.dokument.bestilling.utils.BP1
import no.nav.bidrag.dokument.bestilling.utils.FASTSETTELSE_GEBYR_2024
import no.nav.bidrag.dokument.bestilling.utils.FORSKUDDSATS_2024_2025
import no.nav.bidrag.dokument.bestilling.utils.FORSKUDD_INNTEKTGRENSE_2022_2023
import no.nav.bidrag.dokument.bestilling.utils.FORSKUDD_INNTEKTGRENSE_2023_2024
import no.nav.bidrag.dokument.bestilling.utils.FORSKUDD_MAKS_INNTEKT_FORSKUDD_MOTTAKER_2022_2023
import no.nav.bidrag.dokument.bestilling.utils.FORSKUDD_MAKS_INNTEKT_FORSKUDD_MOTTAKER_2024_2025
import no.nav.bidrag.dokument.bestilling.utils.MULTIPLIKATOR_MAKS_INNTEKT_FORSKUDD_MOTTAKER_2024
import no.nav.bidrag.dokument.bestilling.utils.SAK_OPPRETTET_DATO
import no.nav.bidrag.dokument.bestilling.utils.createEnhetKontaktInformasjon
import no.nav.bidrag.dokument.bestilling.utils.createPostAdresseResponse
import no.nav.bidrag.dokument.bestilling.utils.opprettBehandlingDetaljer
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.VirkningstidspunktÅrsakstype
import no.nav.bidrag.transport.dokumentmaler.PeriodeFraTom
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate

class DokumentBestillingControllerForskuddTest : AbstractControllerTest() {
    @Test
    fun `skal produsere XML for varselbrev forskudd`() {
        stubDefaultValues()
        stubUtils.stubHentPerson("16451299577", ANNEN_MOTTAKER)
        stubUtils.stubHentPerson("25451755601", ANNEN_MOTTAKER)
        val behandlingResponse =
            opprettBehandlingDetaljer().copy(
                type = TypeBehandling.FORSKUDD,
                engangsbeløptype = null,
                stønadstype = Stønadstype.FORSKUDD,
                årsak = VirkningstidspunktÅrsakstype.FRA_SØKNADSTIDSPUNKT,
                roller =
                setOf(
                    no.nav.bidrag.dokument.bestilling.consumer.dto.RolleDto(
                        rolletype = Rolletype.BIDRAGSMOTTAKER,
                        ident = BM1.ident.verdi,
                        navn = BM1.navn,
                        fødselsdato = BM1.fødselsdato,
                    ),
                    no.nav.bidrag.dokument.bestilling.consumer.dto.RolleDto(
                        rolletype = Rolletype.BARN,
                        ident = BARN1.ident.verdi,
                        navn = BARN1.navn,
                        fødselsdato = BARN1.fødselsdato,
                    ),
                    no.nav.bidrag.dokument.bestilling.consumer.dto.RolleDto(
                        rolletype = Rolletype.BARN,
                        ident = BARN2.ident.verdi,
                        navn = BARN2.navn,
                        fødselsdato = BARN2.fødselsdato,
                    ),
                ),
            )
        stubUtils.stubHentBehandling(behandlingResponse)
        val enhetKontaktInfo = createEnhetKontaktInformasjon()
        val bmAdresse = createPostAdresseResponse()

        val dokumentMal = hentDokumentMal("BI01S08")!!
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

                message.brev?.barnISak?.shouldHaveSize(2)

                val barnISak1 = message.brev?.barnISak!!.find { it.fnr == BARN1.ident.verdi }!!
                barnISak1.fDato shouldBe BARN1.fødselsdato
                barnISak1.fnr shouldBe BARN1.ident.verdi
                barnISak1.navn shouldBe BARN1.fornavnEtternavn()
                barnISak1.personIdRm shouldBe ""
                barnISak1.belopGebyrRm shouldBe ""
                barnISak1.belForskudd shouldBe null
                barnISak1.belBidrag shouldBe null

                message.brev?.soknadBost?.saksnr shouldBe saksnummer
                message.brev?.soknadBost?.sakstype shouldBe "E"
                message.brev?.soknadBost?.hgKode shouldBe "FO"
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
                soknad.type shouldBe "FO"
                soknad.aarsakKd shouldBe VirkningstidspunktÅrsakstype.FRA_SØKNADSTIDSPUNKT.legacyKode.firstOrNull()
                soknad.undergrp shouldBe "E"
                soknad.saksnr shouldBe saksnummer
                soknad.sendtDato shouldBe null
                soknad.vedtattDato shouldBe null
                soknad.virkningDato shouldBe null

                val soknadBost = message.brev?.soknadBost!!
                soknadBost.hgKode shouldBe "FO"
                soknadBost.ugKode shouldBe "E"
                soknadBost.datoSakReg shouldBe SAK_OPPRETTET_DATO
                soknadBost.gebyrsats shouldBe FASTSETTELSE_GEBYR_2024.toBigDecimal().setScale(1)
                soknadBost.virkningsDato shouldBe null
                soknadBost.sendtDato shouldBe LocalDate.now()
                soknadBost.mottatDato shouldBe soknadDato
                soknadBost.soknGrKode shouldBe "FO"
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
    fun `skal produsere XML for forskudd vedtakbrev med flere perioder`() {
        stubDefaultValues()
        stubUtils.stubHentPerson("16451299577", ANNEN_MOTTAKER)
        stubUtils.stubHentPerson("25451755601", ANNEN_MOTTAKER)
        stubUtils.stubHentVedtak("vedtak_response_forskudd.json")
        val enhetKontaktInfo = createEnhetKontaktInformasjon()
        val bmAdresse = createPostAdresseResponse()
        val dokumentMal = hentDokumentMal("BI01A01")!!
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

                message.brev?.barnISak?.shouldHaveSize(2)

                val barnISak1 = message.brev?.barnISak!!.find { it.fnr == BARN2.ident.verdi }!!
                barnISak1.fDato shouldBe BARN2.fødselsdato
                barnISak1.fnr shouldBe BARN2.ident.verdi
                barnISak1.navn shouldBe BARN2.fornavnEtternavn()
                barnISak1.personIdRm shouldBe ""
                barnISak1.belopGebyrRm shouldBe ""
                barnISak1.belForskudd shouldBe BigDecimal(1480).setScale(2)
                barnISak1.belBidrag shouldBe null

                message.brev?.soknadBost?.saksnr shouldBe saksnummer
                message.brev?.soknadBost?.sakstype shouldBe "E"
                message.brev?.soknadBost?.rmISak shouldBe false
                message.brev?.soknadBost?.sendtDato shouldBe LocalDate.now()

                message.brev?.saksbehandler?.navn shouldBe "Saksbehandler Mellomnavn Saksbehandlersen"

                val virkningDato = LocalDate.parse("2023-01-01")
                val periode1 = PeriodeFraTom(virkningDato, LocalDate.parse("2023-02-28"))
                val periode2 =
                    PeriodeFraTom(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-06-30"))
                val periode4 = PeriodeFraTom(LocalDate.parse("2024-01-01"), LocalDate.parse("2024-01-31"))
                val sistePeriode =
                    PeriodeFraTom(LocalDate.parse("2024-07-01"), MAX_DATE)

                // Valider forskudd vedtak resultater
                val soknadDato = LocalDate.parse("2024-07-15")

                val soknad = message.brev?.soknad!!
                soknad.soknDato shouldBe soknadDato
                soknad.type shouldBe "FO"
                soknad.aarsakKd shouldBe VirkningstidspunktÅrsakstype.FRA_SØKNADSTIDSPUNKT.legacyKode.firstOrNull()
                soknad.undergrp shouldBe "S"
                soknad.saksnr shouldBe saksnummer
                soknad.sendtDato shouldBe LocalDate.parse("2024-08-04")
                soknad.vedtattDato shouldBe LocalDate.parse("2024-08-04")
                soknad.virkningDato shouldBe virkningDato

                val soknadBost = message.brev?.soknadBost!!
                soknadBost.hgKode shouldBe "FO"
                soknadBost.ugKode shouldBe "S"
                soknadBost.datoSakReg shouldBe SAK_OPPRETTET_DATO
                soknadBost.gebyrsats shouldBe FASTSETTELSE_GEBYR_2024.toBigDecimal().setScale(1)
                soknadBost.virkningsDato shouldBe virkningDato
                soknadBost.mottatDato shouldBe soknadDato
                soknadBost.soknGrKode shouldBe "FO"
                soknadBost.soknFraKode shouldBe "MO"
                soknadBost.soknType shouldBe "FA"

                message.brev?.vedtak!! shouldHaveSize 17
                message.brev?.bidragBarn!! shouldHaveSize 2
                val barn1 = message?.brev?.bidragBarn!![0]
                barn1.barn!!.fnr shouldBe BARN2.ident.verdi
                barn1.barn!!.saksnr shouldBe saksnummer

                val barn2 = message?.brev?.bidragBarn!![1]
                barn2.barn!!.fnr shouldBe BARN1.ident.verdi
                barn2.barn!!.saksnr shouldBe saksnummer

                assertSoftly(message.brev!!.vedtak.filter { it.fnr == BARN1.ident.verdi }) {
                    shouldHaveSize(8)
                    val vedtakPeriode1 = this[0]
                    vedtakPeriode1.belopBidrag shouldBe BigDecimal(2200).setScale(2)
                    vedtakPeriode1.fomDato shouldBe periode1.fraDato
                    vedtakPeriode1.tomDato shouldBe periode1.tomDato
                    vedtakPeriode1.fnr shouldBe BARN1.ident.verdi
                    vedtakPeriode1.resultatKode shouldBe Resultatkode.FORHØYET_FORSKUDD_11_ÅR_125_PROSENT.legacyKode

                    val vedtakPeriode2 = this[1]
                    vedtakPeriode2.belopBidrag shouldBe BigDecimal(2200).setScale(2)
                    vedtakPeriode2.fomDato shouldBe periode2.fraDato
                    vedtakPeriode2.tomDato shouldBe periode2.tomDato
                    vedtakPeriode2.fnr shouldBe BARN1.ident.verdi
                    vedtakPeriode2.resultatKode shouldBe Resultatkode.FORHØYET_FORSKUDD_11_ÅR_125_PROSENT.legacyKode

                    val vedtakPeriodeSiste = this[7]
                    vedtakPeriodeSiste.belopBidrag shouldBe BigDecimal("0.00")
                    vedtakPeriodeSiste.fomDato shouldBe sistePeriode.fraDato
                    vedtakPeriodeSiste.tomDato shouldBe sistePeriode.tomDato
                    vedtakPeriodeSiste.fnr shouldBe BARN1.ident.verdi
                    vedtakPeriodeSiste.resultatKode shouldBe Resultatkode.AVSLAG_OVER_18_ÅR.legacyKode
                }

                assertSoftly(message.brev!!.vedtak.filter { it.fnr == BARN2.ident.verdi }) {
                    shouldHaveSize(9)
                    val vedtakPeriode1 = this[0]
                    vedtakPeriode1.belopBidrag shouldBe BigDecimal(1760).setScale(2)
                    vedtakPeriode1.fomDato shouldBe periode1.fraDato
                    vedtakPeriode1.tomDato shouldBe periode1.tomDato
                    vedtakPeriode1.fnr shouldBe BARN2.ident.verdi
                    vedtakPeriode1.resultatKode shouldBe Resultatkode.FORHØYET_FORSKUDD_100_PROSENT.legacyKode

                    val vedtakPeriode2 = this[1]
                    vedtakPeriode2.belopBidrag shouldBe BigDecimal(1760).setScale(2)
                    vedtakPeriode2.fomDato shouldBe periode2.fraDato
                    vedtakPeriode2.tomDato shouldBe periode2.tomDato
                    vedtakPeriode2.fnr shouldBe BARN2.ident.verdi
                    vedtakPeriode2.resultatKode shouldBe Resultatkode.FORHØYET_FORSKUDD_100_PROSENT.legacyKode

                    val vedtakPeriodeSiste = this[8]
                    vedtakPeriodeSiste.belopBidrag shouldBe BigDecimal(1480).setScale(2)
                    vedtakPeriodeSiste.fomDato shouldBe sistePeriode.fraDato
                    vedtakPeriodeSiste.tomDato shouldBe sistePeriode.tomDato
                    vedtakPeriodeSiste.fnr shouldBe BARN2.ident.verdi
                    vedtakPeriodeSiste.resultatKode shouldBe Resultatkode.ORDINÆRT_FORSKUDD_75_PROSENT.legacyKode
                }

                message.brev?.forskuddVedtakPeriode!!.size shouldBe 17
                assertSoftly(message.brev?.forskuddVedtakPeriode!!.filter { it.fnr == BARN2.ident.verdi }) {
                    shouldHaveSize(9)
                    val forskuddVedtakPeriode1 = this[0]
                    forskuddVedtakPeriode1.fomDato shouldBe periode1.fraDato
                    forskuddVedtakPeriode1.tomDato shouldBe periode1.tomDato
                    forskuddVedtakPeriode1.beløp shouldBe BigDecimal(1760).setScale(2)
                    forskuddVedtakPeriode1.fnr shouldBe BARN2.ident.verdi
                    forskuddVedtakPeriode1.resultatKode shouldBe Resultatkode.FORHØYET_FORSKUDD_100_PROSENT.legacyKode
                    forskuddVedtakPeriode1.prosent shouldBe "100"
                    forskuddVedtakPeriode1.maksInntekt shouldBe FORSKUDD_MAKS_INNTEKT_FORSKUDD_MOTTAKER_2022_2023

                    val forskuddVedtakPeriode4 = this[8]
                    forskuddVedtakPeriode4.fomDato shouldBe sistePeriode.fraDato
                    forskuddVedtakPeriode4.tomDato shouldBe sistePeriode.tomDato
                    forskuddVedtakPeriode4.beløp shouldBe BigDecimal(1480).setScale(2)
                    forskuddVedtakPeriode4.fnr shouldBe BARN2.ident.verdi
                    forskuddVedtakPeriode4.resultatKode shouldBe Resultatkode.ORDINÆRT_FORSKUDD_75_PROSENT.legacyKode
                    forskuddVedtakPeriode4.forskKode shouldBe ""
                    forskuddVedtakPeriode4.prosent shouldBe "075"
                    forskuddVedtakPeriode4.maksInntekt shouldBe FORSKUDD_MAKS_INNTEKT_FORSKUDD_MOTTAKER_2024_2025

                    // Skal være samme som message.brev.forskuddVedtakPeriode[0]
                    barn1.forskuddVedtakPerioder shouldHaveSize 9
                    val barnForskuddVedtakPeriode1 = barn1.forskuddVedtakPerioder[0]
                    barnForskuddVedtakPeriode1.fomDato shouldBe forskuddVedtakPeriode1.fomDato
                    barnForskuddVedtakPeriode1.tomDato shouldBe forskuddVedtakPeriode1.tomDato
                    barnForskuddVedtakPeriode1.beløp shouldBe forskuddVedtakPeriode1.beløp
                    barnForskuddVedtakPeriode1.fnr shouldBe forskuddVedtakPeriode1.fnr
                    barnForskuddVedtakPeriode1.resultatKode shouldBe forskuddVedtakPeriode1.resultatKode
                    barnForskuddVedtakPeriode1.prosent shouldBe forskuddVedtakPeriode1.prosent
                    barnForskuddVedtakPeriode1.maksInntekt shouldBe forskuddVedtakPeriode1.maksInntekt
                }

                assertSoftly(message.brev?.forskuddVedtakPeriode!!.filter { it.fnr == BARN1.ident.verdi }) {
                    shouldHaveSize(8)
                    val forskuddVedtakPeriode1 = this[0]
                    forskuddVedtakPeriode1.fomDato shouldBe periode1.fraDato
                    forskuddVedtakPeriode1.tomDato shouldBe periode1.tomDato
                    forskuddVedtakPeriode1.beløp shouldBe BigDecimal(2200).setScale(2)
                    forskuddVedtakPeriode1.fnr shouldBe BARN1.ident.verdi
                    forskuddVedtakPeriode1.resultatKode shouldBe Resultatkode.FORHØYET_FORSKUDD_11_ÅR_125_PROSENT.legacyKode
                    forskuddVedtakPeriode1.prosent shouldBe "125"
                    forskuddVedtakPeriode1.maksInntekt shouldBe FORSKUDD_MAKS_INNTEKT_FORSKUDD_MOTTAKER_2022_2023

                    val forskuddVedtakPeriode4 = this[7]
                    forskuddVedtakPeriode4.fomDato shouldBe sistePeriode.fraDato
                    forskuddVedtakPeriode4.tomDato shouldBe sistePeriode.tomDato
                    forskuddVedtakPeriode4.beløp shouldBe BigDecimal("0.00")
                    forskuddVedtakPeriode4.fnr shouldBe BARN1.ident.verdi
                    forskuddVedtakPeriode4.resultatKode shouldBe Resultatkode.AVSLAG_OVER_18_ÅR.legacyKode
                    forskuddVedtakPeriode4.forskKode shouldBe "BOA"
                    forskuddVedtakPeriode4.prosent shouldBe "000"
                    forskuddVedtakPeriode4.maksInntekt shouldBe FORSKUDD_MAKS_INNTEKT_FORSKUDD_MOTTAKER_2024_2025
                }

                barn1.inntektPerioder shouldHaveSize 37
                val inntekterPeriode1 = barn1.hentInntektPerioder(periode1)
                inntekterPeriode1 shouldHaveSize 3
                inntekterPeriode1[0].inntektGrense shouldBe FORSKUDD_INNTEKTGRENSE_2022_2023
                inntekterPeriode1[0].belopType shouldBe "UBAT"
                inntekterPeriode1[0].belopÅrsinntekt shouldBe BigDecimal(12648)
                inntekterPeriode1[1].belopType shouldBe "XKAP"
                inntekterPeriode1[1].belopÅrsinntekt shouldBe BigDecimal(145000)
                inntekterPeriode1[2].belopType shouldBe "XINN"
                inntekterPeriode1[2].belopÅrsinntekt shouldBe BigDecimal(157648)

                val inntekterPeriode2 = barn1.hentInntektPerioder(periode2)
                inntekterPeriode2 shouldHaveSize 3
                inntekterPeriode2[0].inntektGrense shouldBe FORSKUDD_INNTEKTGRENSE_2022_2023
                inntekterPeriode2[0].fnr shouldBe BM1.ident.verdi
                inntekterPeriode2[0].rolle shouldBe "02"
                inntekterPeriode2[0].belopType shouldBe "UBAT"
                inntekterPeriode2[0].beskrivelse shouldBe "Utvidet barnetrygd"
                inntekterPeriode2[0].belopÅrsinntekt shouldBe BigDecimal(29868)
                inntekterPeriode2[1].belopType shouldBe "XKAP"
                inntekterPeriode2[1].beskrivelse shouldBe "Netto positive kapitalinntekter"
                inntekterPeriode2[1].belopÅrsinntekt shouldBe BigDecimal(145000)
                inntekterPeriode2[2].belopType shouldBe "XINN"
                inntekterPeriode2[2].beskrivelse shouldBe "Personens beregningsgrunnlag i perioden"
                inntekterPeriode2[2].belopÅrsinntekt shouldBe BigDecimal(174868)

                val inntekterPeriode3 = barn1.hentInntektPerioder(periode4)
                inntekterPeriode3 shouldHaveSize 4
                inntekterPeriode3[0].inntektGrense shouldBe FORSKUDD_INNTEKTGRENSE_2023_2024
                inntekterPeriode3[0].belopType shouldBe "UBAT"
                inntekterPeriode3[0].belopÅrsinntekt shouldBe BigDecimal(30192)
                inntekterPeriode3[1].belopType shouldBe "KONT"
                inntekterPeriode3[1].beskrivelse shouldBe "KontantstÃ¸tte"
                inntekterPeriode3[1].belopÅrsinntekt shouldBe BigDecimal(90000)
                inntekterPeriode3[2].belopType shouldBe "XKAP"
                inntekterPeriode3[2].belopÅrsinntekt shouldBe BigDecimal(145000)
                inntekterPeriode3[3].belopType shouldBe "XINN"
                inntekterPeriode3[3].belopÅrsinntekt shouldBe BigDecimal(265192)

                barn1.forskuddSivilstandPerioder shouldHaveSize 2
                val sivilstandPeriode1 = barn1.forskuddSivilstandPerioder[0]
                sivilstandPeriode1.fomDato shouldBe virkningDato
                sivilstandPeriode1.tomDato shouldBe LocalDate.parse("2023-12-31")
                sivilstandPeriode1.kode shouldBe "SEPA"
                sivilstandPeriode1.beskrivelse shouldBe "Enslig"

                val sivilstandPeriode2 = barn1.forskuddSivilstandPerioder[1]
                sivilstandPeriode2.fomDato shouldBe LocalDate.parse("2024-01-01")
                sivilstandPeriode2.tomDato shouldBe MAX_DATE
                sivilstandPeriode2.kode shouldBe "GIFT"
                sivilstandPeriode2.beskrivelse shouldBe "Gift/samboer"

                barn1.forskuddBarnPerioder shouldHaveSize 3
                barn1.forskuddBarnPerioder[0].antallBarn shouldBe 4
                barn1.forskuddBarnPerioder[1].antallBarn shouldBe 3
                barn1.forskuddBarnPerioder[1].antallBarn shouldBe 3

                barn1.inntektGrunnlagForskuddPerioder shouldHaveSize 8 * 3 // 4 inntektgrenser for enslig, 4 for gift/samboer. Inntekgrenser vises for alle 3 perioder = total 8*3

                stubUtils.Verify().verifyHentEnhetKontaktInfoCalledWith()
                stubUtils.Verify().verifyHentPersonCalled(BM1.ident.verdi)
                stubUtils.Verify().verifyHentPersonCalled(BP1.ident.verdi)
                stubUtils.Verify().verifyHentPersonCalled(BARN1.ident.verdi)
            }
        }
    }

    @Test
    fun `skal produsere XML for forskudd vedtakbrev direkte avslag`() {
        stubDefaultValues()
        stubUtils.stubHentPerson("16451299577", ANNEN_MOTTAKER)
        stubUtils.stubHentPerson("25451755601", ANNEN_MOTTAKER)
        stubUtils.stubHentVedtak("vedtak_response-forskudd-direkte-avslag.json")
        val enhetKontaktInfo = createEnhetKontaktInformasjon()
        val bmAdresse = createPostAdresseResponse()
        val dokumentMal = hentDokumentMal("BI01A01")!!
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
            purge()
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

                message.brev?.barnISak?.shouldHaveSize(2)

                val barnISak1 = message.brev?.barnISak!!.find { it.fnr == BARN2.ident.verdi }!!
                barnISak1.fDato shouldBe BARN2.fødselsdato
                barnISak1.fnr shouldBe BARN2.ident.verdi
                barnISak1.navn shouldBe BARN2.fornavnEtternavn()
                barnISak1.personIdRm shouldBe ""
                barnISak1.belopGebyrRm shouldBe ""
                barnISak1.belForskudd shouldBe BigDecimal(0).setScale(2)
                barnISak1.belBidrag shouldBe null

                message.brev?.soknadBost?.saksnr shouldBe saksnummer
                message.brev?.soknadBost?.sakstype shouldBe "E"
                message.brev?.soknadBost?.rmISak shouldBe false
                message.brev?.soknadBost?.sendtDato shouldBe LocalDate.now()

                message.brev?.saksbehandler?.navn shouldBe "Saksbehandler Mellomnavn Saksbehandlersen"

                val virkningDato = LocalDate.parse("2023-01-01")
                val periode1 = PeriodeFraTom(virkningDato, MAX_DATE)

                // Valider forskudd vedtak resultater
                val soknadDato = LocalDate.parse("2024-01-28")

                val soknad = message.brev?.soknad!!
                soknad.soknDato shouldBe soknadDato
                soknad.type shouldBe "FO"
                soknad.aarsakKd shouldBe Resultatkode.IKKE_OMSORG.legacyKode
                soknad.undergrp shouldBe "E"
                soknad.saksnr shouldBe saksnummer
                soknad.sendtDato shouldBe LocalDate.parse("2024-08-12")
                soknad.vedtattDato shouldBe LocalDate.parse("2024-08-12")
                soknad.virkningDato shouldBe virkningDato

                val soknadBost = message.brev?.soknadBost!!
                soknadBost.hgKode shouldBe "FO"
                soknadBost.ugKode shouldBe "E"
                soknadBost.datoSakReg shouldBe SAK_OPPRETTET_DATO
                soknadBost.gebyrsats shouldBe FASTSETTELSE_GEBYR_2024.toBigDecimal().setScale(1)
                soknadBost.virkningsDato shouldBe virkningDato
                soknadBost.mottatDato shouldBe soknadDato
                soknadBost.soknGrKode shouldBe "FO"
                soknadBost.soknFraKode shouldBe "MO"
                soknadBost.soknType shouldBe "EN"

                message.brev?.vedtak!! shouldHaveSize 2
                message.brev?.bidragBarn!! shouldHaveSize 2
                val barn1 = message?.brev?.bidragBarn!![0]
                barn1.barn!!.fnr shouldBe BARN2.ident.verdi
                barn1.barn!!.saksnr shouldBe saksnummer

                val barn2 = message?.brev?.bidragBarn!![1]
                barn2.barn!!.fnr shouldBe BARN1.ident.verdi
                barn2.barn!!.saksnr shouldBe saksnummer

                assertSoftly(message.brev!!.vedtak.filter { it.fnr == BARN1.ident.verdi }) {
                    shouldHaveSize(1)
                    val vedtakPeriode1 = this[0]
                    vedtakPeriode1.belopBidrag shouldBe BigDecimal("0.00")
                    vedtakPeriode1.fomDato shouldBe periode1.fraDato
                    vedtakPeriode1.tomDato shouldBe MAX_DATE
                    vedtakPeriode1.fnr shouldBe BARN1.ident.verdi
                    vedtakPeriode1.resultatKode shouldBe Resultatkode.IKKE_OMSORG.legacyKode
                }

                assertSoftly(message.brev!!.vedtak.filter { it.fnr == BARN2.ident.verdi }) {
                    shouldHaveSize(1)
                    val vedtakPeriode1 = this[0]
                    vedtakPeriode1.belopBidrag shouldBe BigDecimal("0.00")
                    vedtakPeriode1.fomDato shouldBe periode1.fraDato
                    vedtakPeriode1.tomDato shouldBe MAX_DATE
                    vedtakPeriode1.fnr shouldBe BARN2.ident.verdi
                    vedtakPeriode1.resultatKode shouldBe Resultatkode.IKKE_OMSORG.legacyKode
                }

                message.brev?.forskuddVedtakPeriode!!.size shouldBe 2
                assertSoftly(message.brev?.forskuddVedtakPeriode!!.filter { it.fnr == BARN2.ident.verdi }) {
                    shouldHaveSize(1)
                    val forskuddVedtakPeriode1 = this[0]
                    forskuddVedtakPeriode1.fomDato shouldBe periode1.fraDato
                    forskuddVedtakPeriode1.tomDato shouldBe MAX_DATE
                    forskuddVedtakPeriode1.beløp shouldBe BigDecimal("0.00")
                    forskuddVedtakPeriode1.fnr shouldBe BARN2.ident.verdi
                    forskuddVedtakPeriode1.resultatKode shouldBe Resultatkode.IKKE_OMSORG.legacyKode
                    forskuddVedtakPeriode1.prosent shouldBe "000"
                    forskuddVedtakPeriode1.maksInntekt shouldBe FORSKUDD_MAKS_INNTEKT_FORSKUDD_MOTTAKER_2024_2025

                    // Skal være samme som message.brev.forskuddVedtakPeriode[0]
                    barn1.forskuddVedtakPerioder shouldHaveSize 1
                    val barnForskuddVedtakPeriode1 = barn1.forskuddVedtakPerioder[0]
                    barnForskuddVedtakPeriode1.fomDato shouldBe forskuddVedtakPeriode1.fomDato
                    barnForskuddVedtakPeriode1.tomDato shouldBe MAX_DATE
                    barnForskuddVedtakPeriode1.beløp shouldBe forskuddVedtakPeriode1.beløp?.setScale(2)
                    barnForskuddVedtakPeriode1.fnr shouldBe forskuddVedtakPeriode1.fnr
                    barnForskuddVedtakPeriode1.resultatKode shouldBe forskuddVedtakPeriode1.resultatKode
                    barnForskuddVedtakPeriode1.prosent shouldBe forskuddVedtakPeriode1.prosent
                    barnForskuddVedtakPeriode1.maksInntekt shouldBe forskuddVedtakPeriode1.maksInntekt
                }

                assertSoftly(message.brev?.forskuddVedtakPeriode!!.filter { it.fnr == BARN1.ident.verdi }) {
                    shouldHaveSize(1)
                    val forskuddVedtakPeriode1 = this[0]
                    forskuddVedtakPeriode1.fomDato shouldBe periode1.fraDato
                    forskuddVedtakPeriode1.tomDato shouldBe periode1.tomDato
                    forskuddVedtakPeriode1.beløp shouldBe BigDecimal("0.00")
                    forskuddVedtakPeriode1.fnr shouldBe BARN1.ident.verdi
                    forskuddVedtakPeriode1.resultatKode shouldBe Resultatkode.IKKE_OMSORG.legacyKode
                    forskuddVedtakPeriode1.prosent shouldBe "000"
                    forskuddVedtakPeriode1.maksInntekt shouldBe FORSKUDD_MAKS_INNTEKT_FORSKUDD_MOTTAKER_2024_2025
                }

                barn1.inntektPerioder shouldHaveSize 0
                barn1.forskuddSivilstandPerioder shouldHaveSize 0
                barn1.forskuddBarnPerioder shouldHaveSize 0

                barn1.inntektGrunnlagForskuddPerioder shouldHaveSize 8 * 3 // 4 inntektgrenser for enslig, 4 for gift/samboer. Inntekgrenser vises for alle 3 perioder = total 8*3

                stubUtils.Verify().verifyHentEnhetKontaktInfoCalledWith()
                stubUtils.Verify().verifyHentPersonCalled(BM1.ident.verdi)
                stubUtils.Verify().verifyHentPersonCalled(BP1.ident.verdi)
                stubUtils.Verify().verifyHentPersonCalled(BARN1.ident.verdi)
            }
        }
    }
}

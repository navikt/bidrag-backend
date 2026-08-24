package no.nav.bidrag.dokument.bestilling.bestilling.produksjon

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.dokument.bestilling.bestilling.dto.BestillingSystem
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DataGrunnlag
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentBestillingResult
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMal
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMalType
import no.nav.bidrag.dokument.bestilling.bestilling.dto.andelUnderholdPerioder
import no.nav.bidrag.dokument.bestilling.bestilling.dto.beløpKode
import no.nav.bidrag.dokument.bestilling.bestilling.dto.beskrivelse
import no.nav.bidrag.dokument.bestilling.bestilling.dto.inntektsperioder
import no.nav.bidrag.dokument.bestilling.bestilling.dto.samværsperioder
import no.nav.bidrag.dokument.bestilling.bestilling.dto.underholdskostnadperioder
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto.Brev
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto.BrevBestilling
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto.BrevKontaktinfo
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto.BrevMottaker
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto.behandlingType
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto.brevbestilling
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto.soknadType
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto.toKode
import no.nav.bidrag.dokument.bestilling.consumer.BidragDokumentConsumer
import no.nav.bidrag.dokument.bestilling.model.BehandlingType
import no.nav.bidrag.dokument.bestilling.model.MAX_DATE
import no.nav.bidrag.dokument.bestilling.model.ResultatKoder
import no.nav.bidrag.dokument.bestilling.model.brevkodeVedtakForskudd
import no.nav.bidrag.dokument.bestilling.model.tilBisysResultatkodeForBrev
import no.nav.bidrag.dokument.bestilling.model.tilLocalDateFom
import no.nav.bidrag.dokument.bestilling.model.tilLocalDateTil
import no.nav.bidrag.dokument.bestilling.tjenester.erBidrag
import no.nav.bidrag.domene.enums.barnetilsyn.Skolealder
import no.nav.bidrag.domene.enums.barnetilsyn.Tilsynstype
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.behandling.tilBehandlingstema
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.person.Sivilstandskode
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.util.avrundetMedNullDesimaler
import no.nav.bidrag.domene.util.visningsnavn
import no.nav.bidrag.domene.util.årsbeløpTilMåndesbeløp
import no.nav.bidrag.transport.dokument.AvsenderMottakerDto
import no.nav.bidrag.transport.dokument.JournalpostType
import no.nav.bidrag.transport.dokument.OpprettDokumentDto
import no.nav.bidrag.transport.dokument.OpprettJournalpostRequest
import no.nav.bidrag.transport.dokument.isNumeric
import no.nav.bidrag.transport.dokumentmaler.DokumentBestilling
import no.nav.bidrag.transport.dokumentmaler.EnhetKontaktInfo
import no.nav.bidrag.transport.dokumentmaler.ForskuddInntektgrensePeriode
import no.nav.bidrag.transport.dokumentmaler.Mottaker
import no.nav.bidrag.transport.dokumentmaler.fraVerdi
import no.nav.bidrag.transport.dokumentmaler.tilVerdi
import org.springframework.beans.factory.annotation.Value
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Component
import java.math.BigDecimal

val bidragStønader = listOf(Stønadstype.BIDRAG, Stønadstype.BIDRAG18AAR)
private val log = KotlinLogging.logger {}

@Component(BestillingSystem.BREVSERVER)
class BrevserverProducer(
    val onlinebrevTemplate: JmsTemplate,
    val batchbrevTemplate: JmsTemplate,
    val bidragDokumentConsumer: BidragDokumentConsumer,
    val hgUgKodeService: HgUgKodeService,
    @Value($$"${BREVSERVER_PASSORD}") val brevPassord: String,
) : DokumentProducer {
    override fun produser(
        dokumentBestilling: DokumentBestilling,
        dokumentMal: DokumentMal,
    ): DokumentBestillingResult {
        val journalpostId = opprettJournalpost(dokumentBestilling, dokumentMal)

        if (dokumentBestilling.bestillBatch || dokumentMal.batchbrev) {
            log.info { "Sender melding til batch kø" }
            val melding =
                mapToBrevserverMessage(
                    dokumentBestilling,
                    dokumentMal,
                )
            melding.skrivertype = "SENTRAL"
            melding.direkteutskrift = "JA"
            batchbrevTemplate.convertAndSend(melding)
        } else {
            onlinebrevTemplate.convertAndSend(
                mapToBrevserverMessage(
                    dokumentBestilling,
                    dokumentMal,
                ),
            )
        }

        // TODO: Error handling
        return DokumentBestillingResult(
            dokumentReferanse = dokumentBestilling.dokumentreferanse!!,
            journalpostId = journalpostId,
            bestillingSystem = BestillingSystem.BREVSERVER,
        )
    }

    fun opprettJournalpost(
        dokumentBestilling: DokumentBestilling,
        dokumentMal: DokumentMal,
    ): String {
        if (dokumentBestilling.dokumentreferanse.isNullOrEmpty()) {
            val tittel = dokumentBestilling.tittel ?: dokumentMal.beskrivelse
            val response =
                bidragDokumentConsumer.opprettJournalpost(
                    OpprettJournalpostRequest(
                        tittel = tittel,
                        journalførendeEnhet = dokumentBestilling.enhet,
                        tilknyttSaker = listOf(dokumentBestilling.saksnummer!!),
                        dokumenter =
                        listOf(
                            OpprettDokumentDto(
                                tittel = tittel,
                                brevkode = dokumentMal.kode,
                            ),
                        ),
                        gjelderIdent = dokumentBestilling.gjelder?.fodselsnummer!!,
                        avsenderMottaker =
                        AvsenderMottakerDto(
                            dokumentBestilling.mottaker?.navn,
                            dokumentBestilling.mottaker?.fodselsnummer!!,
                        ),
                        journalposttype =
                        when (dokumentMal.type) {
                            DokumentMalType.NOTAT -> JournalpostType.NOTAT
                            else -> JournalpostType.UTGÅENDE
                        },
                        saksbehandlerIdent = dokumentBestilling.saksbehandler?.ident,
                    ),
                )

            dokumentBestilling.dokumentreferanse = response?.dokumenter?.get(0)?.dokumentreferanse
            return response?.journalpostId!!
        }
        return ""
    }

    private fun mapToBrevserverMessage(
        dokumentBestilling: DokumentBestilling,
        dokumentMal: DokumentMal,
    ): BrevBestilling {
        val dokumentSpraak = dokumentBestilling.spraak ?: "NB"
        val saksbehandlerNavn = dokumentBestilling.saksbehandler?.navn
        val vedtakInfo = dokumentBestilling.vedtakDetaljer
        return brevbestilling {
            val roller = dokumentBestilling.roller
            val bp = roller.bidragspliktig
            val bm = roller.bidragsmottaker
            val hgUgDto =
                vedtakInfo?.let {
                    hgUgKodeService.findHgUg(
                        it.soknadType,
                        it.søknadFra,
                        it.behandlingType,
                    )
                }
            malpakke = "BI01.${dokumentMal.kode}"
            passord = brevPassord
            saksbehandler = dokumentBestilling.saksbehandler?.ident!!
            brev {
                brevref = dokumentBestilling.dokumentreferanse!!
                spraak = dokumentSpraak
                tknr = dokumentBestilling.enhet!!
                mottaker = dokumentBestilling.mottaker?.let { mapBrevmottaker(this, it) }
                kontaktInfo = mapKontaktInfo(this, dokumentBestilling.kontaktInfo)
                dokumentBestilling.sjablonDetaljer.let {
                    if (dokumentMal.inneholderDatagrunnlag(DataGrunnlag.SJABLON)) {
                        mapInnteksgrenseSjabloner(it.forskuddInntektgrensePerioder)
                        sjablon {
                            forskuddSats = it.forskuddSats
                            inntektTillegsbidrag = it.inntektsintervallTillegsbidrag
                            maksProsentInntektBp = it.maksProsentAvInntektBp
                            multiplikatorHøyInntektBp = it.multiplikatorHøyInntektBp
                            multiplikatorMaksBidrag = it.multiplikatorMaksBidrag
                            multiplikatorMaksInntekBarn = it.multiplikatorMaksInntekBarn
                            multiplikatorInntekstgrenseForskudd = it.multiplikatorInntekstgrenseForskudd
                            nedreInntekstgrenseGebyr = it.nedreInntekstgrenseGebyr
                            prosentTillegsgebyr = it.prosentsatsTilleggsbidrag
                            maksgrenseHøyInntekt = it.maksgrenseHøyInntekt
                            maksBidragsgrense = it.maksBidragsgrense
                            maksInntektsgrense = it.maksInntektsgrense
                            maksForskuddsgrense = it.maksForskuddsgrense
                            maksInntektsgebyr = it.maksInntektsgebyr
                        }
                    }
                }
                soknadBost {
                    saksnr = dokumentBestilling.saksnummer
                    rmISak = dokumentBestilling.rmISak
                    datoSakReg = dokumentBestilling.datoSakOpprettet
                    hgKode = hgUgDto?.hg
                    ugKode = if (dokumentBestilling.vedtakDetaljer?.erDirekteAvslagForAlleBarn == true && dokumentBestilling.vedtakDetaljer!!.stønadstype?.erBidrag == true) "OH" else hgUgDto?.ug
                    sakstype =
                        if (dokumentBestilling.sakDetaljer.harUkjentPart) {
                            "X"
                        } else if (dokumentBestilling.sakDetaljer.levdeAdskilt) {
                            "U"
                        } else {
                            "E"
                        }
                    gebyrsats = dokumentBestilling.sjablonDetaljer.fastsettelseGebyr
                    vedtakInfo?.let {
                        soknGrKode = it.behandlingType?.kode
                        soknFraKode = it.søknadFra?.kode
                        soknType = it.soknadType?.kode
                        virkningsDato = it.virkningstidspunkt
                        mottatDato = it.mottattDato
                        it.vedtakBarn.sumOf { it.sumAvregning }.takeIf { it > BigDecimal.ZERO }?.let { sumAvregning ->
                            secureLogger.info { "Skriv sum avregning $sumAvregning for brev ${dokumentBestilling.tittel} i sak ${dokumentBestilling.saksnummer}" }
                            b4Kode = "1"
                            b4Belop = sumAvregning
                        }
                    }
                    forskUtBet = vedtakInfo != null
                    // Kode fra beslutningårsak i Bisys.
                    val vedtakPerioder =
                        vedtakInfo
                            ?.vedtakBarn
                            ?.flatMap { it.stønadsendringer }
                            ?.flatMap { it.vedtakPerioder } ?: emptyList()
                    val antallVedtakPerioder = vedtakPerioder.size
                    resKode =
                        if (antallVedtakPerioder > 1) {
                            ResultatKoder.FLERE_BESLUTNING_LINJER
                        } else if (antallVedtakPerioder == 1) {
                            val resultatKode = vedtakPerioder[0].resultatKode
                            val erInnkrevingPrivatAvtale =
                                resultatKode == ResultatKoder.PRIVAT_AVTALE && "BI01G01" == dokumentMal.kode
                            val erInnkreving =
                                listOf(
                                    ResultatKoder.INNVILGET_VEDTAK,
                                    ResultatKoder.UTENLANDSK_AVGJØRELSE,
                                ).contains(resultatKode)
                            if (erInnkreving || erInnkrevingPrivatAvtale) ResultatKoder.VEDTAK_VANLIG_INNKREVING else resultatKode
                        } else if (vedtakInfo?.type == TypeBehandling.SÆRBIDRAG) {
                            vedtakInfo.vedtakBarn
                                .firstOrNull()
                                ?.engangsbeløper
                                ?.firstOrNull()
                                ?.særbidragBeregning
                                ?.resultatKode
                                ?.tilBisysResultatkodeForBrev(dokumentBestilling.vedtakDetaljer!!.vedtakstype)
                        } else {
                            null
                        }
                }
                parter {
                    bpfnr = bp?.fodselsnummer
                    bpnavn = bp?.navn
                    bpfodselsdato = bp?.fodselsdato
                    bmfnr = bm?.fodselsnummer
                    bmnavn = bm?.navn
                    bmfodselsdato = bm?.fodselsdato
                    bmlandkode = if (dokumentMal.kode == brevkodeVedtakForskudd) null else bm?.landkode3
                    bplandkode = if (dokumentMal.kode == brevkodeVedtakForskudd) null else bp?.landkode3
                    bpdatodod = bp?.doedsdato
                    bmdatodod = bm?.doedsdato
                    bmgebyr = vedtakInfo?.gebyr?.bmGebyr
                    bpgebyr = vedtakInfo?.gebyr?.bpGebyr
                }
                brevSaksbehandler {
                    navn = saksbehandlerNavn
                }
                vedtakInfo?.let {
                    soknad {
                        aarsakKd = it.årsakKode?.legacyKode?.firstOrNull() ?: it.avslagsKode?.tilBisysResultatkodeForBrev(dokumentBestilling.vedtakDetaljer!!.vedtakstype) // TODO: Oversett til riktig kode
                        undergrp = hgUgDto?.ug
                        type = it.stønadstype?.tilBehandlingstema()?.bisysKode ?: hgUgDto?.hg

                        vedtattDato = it.vedtattDato
                        virkningDato = it.virkningstidspunkt
                        soknDato = it.mottattDato
                        sendtDato = it.vedtattDato // TODO: Er dette riktig?
                        saksnr = dokumentBestilling.saksnummer
                    }
                }

                vedtakInfo?.vedtakBarn?.forEach { vedtakBarn ->
                    vedtakBarn.stønadsendringer.forEach { detaljer ->
                        detaljer.vedtakPerioder.forEach { vedtakPeriode ->
                            vedtak {
                                fomDato = vedtakPeriode.fomDato
                                tomDato = vedtakPeriode.tomDato ?: MAX_DATE
                                fnr = vedtakBarn.fødselsnummer
                                belopBidrag = vedtakPeriode.beløp
                                resultatKode = vedtakPeriode.resultatKode
                                søktTilleggsbidrag = false // TODO
                                erInnkreving = detaljer.innkreving
                            }
                        }
                    }
                    if (vedtakInfo.type == TypeBehandling.BIDRAG && vedtakBarn.erDirekteAvslag) return@forEach

                    bidragBarn {
                        barn {
                            navn = vedtakBarn.navn
                            fnr = vedtakBarn.fødselsnummer
                            saksnr = dokumentBestilling.saksnummer
                        }
                        vedtakBarn.engangsbeløper.forEach { engangsbeløp ->
                            if (engangsbeløp.type == Engangsbeløptype.SÆRBIDRAG && !engangsbeløp.erDirekteAvslag && !dokumentMal.avslagsbrev) {
                                val beregning = engangsbeløp.særbidragBeregning!!
                                vedtak {
                                    fomDato = engangsbeløp.periode.fom
                                    tomDato = engangsbeløp.periode.til?.plusDays(1)
                                    fnr = vedtakBarn.fødselsnummer
                                    erInnkreving = engangsbeløp.medInnkreving
                                    belopBidrag = beregning.resultat
                                    resultatKode = beregning.resultatKode.tilBisysResultatkodeForBrev(dokumentBestilling.vedtakDetaljer!!.vedtakstype)
                                }
                                særbidrag {
                                    antTermin = 1
                                    bidrEvneSiVt = true
                                    beløpSøkt = beregning.kravbeløp
                                    beløpGodkjent = beregning.godkjentbeløp
                                    fratrekk = beregning.beløpDirekteBetaltAvBp
                                    beløpSærbidrag = beregning.resultat
                                    beløpForskudd = engangsbeløp.sjablon.forskuddSats
                                    beløpInntektsgrense = engangsbeløp.sjablon.inntektsgrense
                                    fordNokkel = beregning.andelProsent

                                    val inntekt = beregning.inntekt
                                    bmInntekt = inntekt.bmInntekt
                                    bpInntekt = inntekt.bpInntekt
                                    bbInntekt = inntekt.barnInntekt
                                    sumInntekt = inntekt.totalInntekt
                                }

                                særbidragPeriode {
                                    fomDato = engangsbeløp.periode.fom
                                    tomDato = engangsbeløp.periode.til!!
                                    beløp = beregning.resultat
                                }
                                engangsbeløp.inntekter.forEach {
                                    inntektPeriode {
                                        fomDato = it.periode?.tilLocalDateFom()
                                        tomDato = it.periode.tilLocalDateTil()?.plusDays(1) ?: MAX_DATE
                                        belopType = it.beløpKode
                                        belopÅrsinntekt = it.beløp
                                        beskrivelse = it.beskrivelse
                                        rolle = it.rolle.toKode()
                                        fnr = it.fødselsnummer
                                        inntektGrense = engangsbeløp.sjablon.inntektsgrense
                                    }
                                }
                            }
                        }

                        if (vedtakInfo.type == TypeBehandling.FORSKUDD) {
                            vedtakInfo.barnIHusstandPerioder.forEach {
                                forskuddBarnPeriode {
                                    fomDato = it.periode.tilLocalDateFom()
                                    tomDato = it.periode.tilLocalDateTil() ?: MAX_DATE
                                    antallBarn = it.antall.toInt()
                                }
                            }

                            vedtakInfo.sivilstandPerioder.forEach { sivilstand ->
                                forskuddSivilstandPeriode {
                                    fomDato = sivilstand.periode.tilLocalDateFom()
                                    tomDato = sivilstand.periode.tilLocalDateTil() ?: MAX_DATE
                                    kode = sivilstand.sivilstand.toKode()
                                    beskrivelse = sivilstand.sivilstand.visningsnavn.bruker[Språk.NB]
                                }
                            }
                            vedtakBarn.stønadsendringer.forEach { detaljer ->
                                detaljer.forskuddInntektgrensePerioder.forEach {
                                    inntektGrunnlagForskuddPeriode {
                                        fomDato = it.fomDato
                                        tomDato = it.tomDato ?: MAX_DATE
                                        antallBarn = it.antallBarn
                                        forsorgerKode =
                                            when (it.forsorgerType) {
                                                Sivilstandskode.ENSLIG -> "EN"
                                                Sivilstandskode.GIFT_SAMBOER -> "GS"
                                                else -> ""
                                            }
                                        belop50fra = it.beløp50Prosent.fraVerdi()
                                        belop50til = it.beløp50Prosent.tilVerdi()
                                        belop75fra = it.beløp75Prosent.fraVerdi()
                                        belop75til = it.beløp75Prosent.tilVerdi()
                                    }
                                }
                                detaljer.vedtakPerioder.forEach { vedtakPeriode ->
                                    forskuddVedtak {
                                        fomDato = vedtakPeriode.fomDato
                                        tomDato = vedtakPeriode.tomDato ?: MAX_DATE
                                        fnr = vedtakBarn.fødselsnummer
                                        resultatKode = vedtakPeriode.resultatKode
                                        forskKode = Resultatkode.fraKode(vedtakPeriode.resultatKode)?.tilForskuddKode()
                                        beløp = vedtakPeriode.beløp
                                        prosent = if (vedtakPeriode.resultatKode.isNumeric) vedtakPeriode.resultatKode.padStart(3, '0') else "000"
                                        maksInntekt = vedtakPeriode.maksInntekt
                                    }

                                    forskuddVedtakPeriode {
                                        fomDato = vedtakPeriode.fomDato
                                        tomDato = vedtakPeriode.tomDato ?: MAX_DATE
                                        fnr = vedtakBarn.fødselsnummer
                                        resultatKode = vedtakPeriode.resultatKode
                                        forskKode = Resultatkode.fraKode(vedtakPeriode.resultatKode)?.tilForskuddKode()
                                        beløp = vedtakPeriode.beløp
                                        prosent = if (vedtakPeriode.resultatKode.isNumeric) vedtakPeriode.resultatKode.padStart(3, '0') else "000"
                                        maksInntekt = vedtakPeriode.maksInntekt
                                    }

                                    vedtakPeriode.inntekter.forEach {
                                        inntektPeriode {
                                            fomDato = it.periode?.tilLocalDateFom()
                                            tomDato = it.periode.tilLocalDateTil() ?: MAX_DATE
                                            belopType = it.beløpKode
                                            belopÅrsinntekt = it.beløp
                                            beskrivelse = it.beskrivelse
                                            rolle = it.rolle.toKode()
                                            fnr = it.fødselsnummer
                                            inntektGrense = vedtakPeriode.inntektGrense
                                        }
                                    }
                                }
                            }
                        }

                        if (vedtakInfo.type == TypeBehandling.BIDRAG) {
                            vedtakBarn.stønadsendringer.forEach { stønadsendring ->
                                stønadsendring.forskuddInntektgrensePerioder.forEach {
                                    inntektGrunnlagForskuddPeriode {
                                        fomDato = it.fomDato
                                        tomDato = it.tomDato ?: MAX_DATE
                                        antallBarn = it.antallBarn
                                        forsorgerKode =
                                            when (it.forsorgerType) {
                                                Sivilstandskode.ENSLIG -> "EN"
                                                Sivilstandskode.GIFT_SAMBOER -> "GS"
                                                else -> ""
                                            }
                                        belop50fra = it.beløp50Prosent.fraVerdi()
                                        belop50til = it.beløp50Prosent.tilVerdi()
                                        belop75fra = it.beløp75Prosent.fraVerdi()
                                        belop75til = it.beløp75Prosent.tilVerdi()
                                    }
                                }

                                vedtakBarn.inntektsperioder.forEach {
                                    inntektPeriode {
                                        fomDato = it.periode?.tilLocalDateFom()
                                        tomDato = it.periode.tilLocalDateTil() ?: MAX_DATE
                                        belopType = it.beløpKode
                                        belopÅrsinntekt = it.beløp
                                        beskrivelse = it.beskrivelse
                                        rolle = it.rolle.toKode()
                                        fnr = it.fødselsnummer
                                        inntektGrense = it.innteksgrense
                                    }
                                }

                                stønadsendring.vedtakPerioder.forEach { vedtakPeriode ->
                                    vedtakBarn {
                                        fomDato = vedtakPeriode.fomDato
                                        tomDato = vedtakPeriode.tomDato ?: MAX_DATE
                                        fnr = vedtakBarn.fødselsnummer
                                        belopBidrag = vedtakPeriode.beløp
                                        resultatKode = vedtakPeriode.resultatKode
                                        søktTilleggsbidrag = false // TODO
                                        erInnkreving = stønadsendring.innkreving
                                    }
                                    vedtakPeriode.bidragsevne?.let {
                                        bidragEvnePeriode {
                                            fomDato = it.periode.fom.atDay(1)
                                            tomDato = it.periode.til?.atEndOfMonth() ?: MAX_DATE
//                                        skatteklasse = ??
                                            antallBarn =
                                                it.underholdEgneBarnIHusstand.antallBarnIHusstanden
                                                    .toBigDecimal()
                                                    .avrundetMedNullDesimaler
                                                    .toInt()
                                            antallBarnDelt = it.underholdEgneBarnIHusstand.antallBarnDeltBossted
                                            bostatus = if (it.borMedAndreVoksne) "1" else "0"
                                            flBarnSakJN = vedtakInfo.vedtakBarn.size > 1
                                            fullBiEvneJN = it.harFullEvne
                                            biEvneBeskr =
                                                when {
                                                    it.harFullEvne -> "F"
                                                    it.harDelvisEvne -> "D"
                                                    else -> "I"
                                                }
                                            belInntGrlag = it.inntektBP
                                            belTrygdeAvg = it.skatt.trygdeavgift
                                            belSkatt = it.skatt.sumSkatt
                                            belMinFradrg = it.sjabloner.beløpMinstefradrag
                                            belPerFradrg = it.sjabloner.beløpKlassfradrag
                                            belBoutgift = it.underholdEgneBarnIHusstand.sjablon
                                            belEgetUhold = it.sjabloner.underholdBeløp
                                            belUholdBhus = it.sjabloner.beløpUnderholdEgneBarnIHusstanden
                                            belAarEvne = it.bidragsevne
                                            belMndEvne = it.bidragsevne.årsbeløpTilMåndesbeløp()
                                            belSumBidrag = it.beløpBidrag // TODO
                                            belBerBidrag = it.beløpBidrag // TODO
                                            belJustBidr = it.beløpBidrag // TODO
                                        }
                                    }
                                }
                            }
                            vedtakBarn.andelUnderholdPerioder.forEach {
                                andelUnderholdPeriode {
                                    fomDato = it.periode.tilLocalDateFom()
                                    tomDato = it.periode.tilLocalDateTil() ?: MAX_DATE
                                    belopInntektBp = it.inntektBP
                                    belopInntektBm = it.inntektBM
                                    belopInntektBarn = it.inntektBarn
                                    belopInntektSum = it.totalEndeligInntekt
                                    fordNokkel = it.andelFaktor
                                    belopUnderholdKostnad = it.beløpUnderholdskostnad
                                    belopBp = it.beløpBpsAndel
                                }
                            }
                            vedtakBarn.samværsperioder.forEach {
                                samværPeriode {
                                    fomDato = it.periode.fom.atDay(1)
                                    tomDato = it.periode.til?.atEndOfMonth() ?: MAX_DATE
                                    samvarKode = it.samværsklasse.bisysKode
                                    aldersGruppe = it.aldersgruppe?.let { "${it.first} - ${it.second ?: ""}" }
                                    belSamvFradr = it.samværsfradragBeløp
                                    samvBeskr =
                                        when (it.samværsklasse) {
                                            Samværsklasse.SAMVÆRSKLASSE_0 -> "0-1 netter pr mnd"
                                            Samværsklasse.SAMVÆRSKLASSE_1 -> "2-3 netter pr mnd eller minst 2 dager pr mnd"
                                            Samværsklasse.SAMVÆRSKLASSE_2 -> "4-8 netter pr mnd"
                                            Samværsklasse.SAMVÆRSKLASSE_3 -> "9-13 netter pr mnd"
                                            Samværsklasse.SAMVÆRSKLASSE_4 -> "14-15 netter pr mnd"
                                            Samværsklasse.DELT_BOSTED -> "Delt samvær"
                                        }
                                    fodselsnummer = vedtakBarn.fødselsnummer
                                }
                            }
                            vedtakBarn.underholdskostnadperioder.forEach {
                                underholdKostnadPeriode {
                                    fomDato = it.periode.tilLocalDateFom()
                                    tomDato = it.periode.tilLocalDateTil() ?: MAX_DATE
                                    belopFbrKost = it.delberegning.forbruksutgift
                                    belopBoutg = it.delberegning.boutgift
                                    belGkjBTils = it.delberegning.nettoTilsynsutgift
                                    belFaktBTils = it.delberegning.nettoTilsynsutgift
                                    belopBTrygd = it.delberegning.barnetrygd
                                    belopSmaBTil = BigDecimal.ZERO
                                    belBerSumU = it.delberegning.underholdskostnad
                                    belJustSumU = it.delberegning.underholdskostnad
                                    fodselsnummer = it.gjelderIdent
                                    rolle = it.rolletype?.toKode()
                                    skolealderTp =
                                        when (it.skolealder) {
                                            Skolealder.OVER -> "O"
                                            Skolealder.UNDER -> "U"
                                            else -> null
                                        }
                                    tilsyntypKd =
                                        when {
                                            it.tilsynstype == Tilsynstype.HELTID && it.skolealder == Skolealder.OVER -> "HO"
                                            it.tilsynstype == Tilsynstype.HELTID && it.skolealder == Skolealder.UNDER -> "HU"
                                            it.tilsynstype == Tilsynstype.DELTID && it.skolealder == Skolealder.OVER -> "DO"
                                            it.tilsynstype == Tilsynstype.DELTID && it.skolealder == Skolealder.UNDER -> "DU"
                                            else -> null
                                        }
                                }
                            }
                        }
                    }
                }
                dokumentBestilling.roller.barn.forEach { rolle ->
                    val vedtakBarn = vedtakInfo?.vedtakBarn?.find { it.fødselsnummer == rolle.fodselsnummer }
                    barnISak {
                        fnr = rolle.fodselsnummer
                        navn = rolle.navn
                        fDato = rolle.fodselsdato
                        fornavn = rolle.fornavn
                        belBidrag = vedtakBarn?.løpendeBidrag
                        belForskudd =
                            rolle.fodselsnummer?.let { it1 -> vedtakInfo?.hentForskuddBarn(it1) }
                    }
                }
            }
        }
    }

    fun Brev.mapInnteksgrenseSjabloner(forskuddInntektgrensePeriode: List<ForskuddInntektgrensePeriode>) = forskuddInntektgrensePeriode.map {
        inntektGrunnlagForskuddPeriode {
            fomDato = it.fomDato
            tomDato = it.tomDato ?: MAX_DATE
            antallBarn = it.antallBarn
            forsorgerKode =
                when (it.forsorgerType) {
                    Sivilstandskode.ENSLIG -> "EN"
                    Sivilstandskode.GIFT_SAMBOER -> "GS"
                    else -> ""
                }
            belop50fra = it.beløp50Prosent.fraVerdi()
            belop50til = it.beløp50Prosent.tilVerdi()
            belop75fra = it.beløp75Prosent.fraVerdi()
            belop75til = it.beløp75Prosent.tilVerdi()
        }
    }

    fun mapKontaktInfo(
        brev: Brev,
        _kontaktInfo: EnhetKontaktInfo?,
    ): BrevKontaktinfo? {
        val kontaktInfo = _kontaktInfo ?: return null
        return brev.brevKontaktinfo {
            returOgPostadresse {
                enhet = kontaktInfo.enhetId
                navn = kontaktInfo.navn
                adresselinje1 = kontaktInfo.postadresse.adresselinje1
                adresselinje2 = kontaktInfo.postadresse.adresselinje2
                postnummer = kontaktInfo.postadresse.postnummer
                poststed = kontaktInfo.postadresse.poststed
                land = kontaktInfo.postadresse.land
                telefon = kontaktInfo.telefonnummer
            }
            avsender =
                avsender {
                    navn = kontaktInfo.navn
                }
            tlfAvsender =
                tlfAvsender {
                    telefonnummer = kontaktInfo.telefonnummer
                }
        }
    }

    fun mapBrevmottaker(
        brev: Brev,
        mottaker: Mottaker,
    ): BrevMottaker = brev.brevmottaker {
        navn = mottaker.navn
        spraak = mottaker.spraak
        fodselsnummer = mottaker.fodselsnummer
        rolle = mottaker.rolle?.toKode()
        fodselsdato = mottaker.fodselsdato

        val adresse = mottaker.adresse
        if (adresse != null) {
            adresselinje1 = adresse.adresselinje1
            adresselinje2 = adresse.adresselinje2
            adresselinje3 = adresse.adresselinje3
            adresselinje4 = adresse.adresselinje4
            boligNr = adresse.bruksenhetsnummer
            postnummer = adresse.postnummer ?: ""
//            landkode = adresse.landkode3
        }
    }
}

fun Resultatkode.tilForskuddKode() = when (this) {
    Resultatkode.AVSLAG_OVER_18_ÅR -> "BOA"
    Resultatkode.AVSLAG_IKKE_REGISTRERT_PÅ_ADRESSE -> "BAF"
    else -> null
}

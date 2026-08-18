package no.nav.bidrag.regnskap.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.regnskap.Søknadstype
import no.nav.bidrag.domene.enums.regnskap.Transaksjonskode
import no.nav.bidrag.domene.enums.regnskap.Type
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.transport.regnskap.krav.Krav
import no.nav.bidrag.transport.regnskap.krav.KravResponse
import no.nav.bidrag.transport.regnskap.krav.Kravkontering
import no.nav.bidrag.transport.regnskap.krav.Kravliste
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import java.time.LocalDate
import java.time.LocalDateTime

private val LOGGER = KotlinLogging.logger { }
private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

@Service
class KravService(
    private val skattConsumer: SkattConsumer,
    private val persistenceService: PersistenceService,
    private val behandlingsstatusService: BehandlingsstatusService,
) {

    @Transactional(
        noRollbackFor = [HttpClientErrorException::class, HttpServerErrorException::class, JwtTokenUnauthorizedException::class],
        propagation = Propagation.REQUIRES_NEW,
    )
    fun sendKrav(oppdragIdListe: List<Int>) {
        val oppdragListe = hentGyldigeOppdrag(oppdragIdListe)

        if (oppdragListe.isEmpty()) {
            LOGGER.info { "Det finnes ingen oppdrag med angitte oppdragsIder: $oppdragIdListe som skal oversendes." }
            return
        }

        // Om det finnes ikke godkjente overføringer som er forsøkt overført tidligere så skal det forsøkes å overføres en gang til og om det feiler avbrytes oversending
        if (validerOppdragForFeiledeOverføringer(oppdragListe)) return

        val oppdragsperioderMedIkkeOverførteKonteringerListe =
            oppdragListe.flatMap { hentOppdragsperioderMedIkkeOverførteKonteringer(it) }

        if (oppdragsperioderMedIkkeOverførteKonteringerListe.isEmpty()) {
            LOGGER.info { "Alle konteringer er allerede overført for alle oppdrag $oppdragIdListe." }
            return
        }

        prosesserOgSendKravTilSkatt(oppdragsperioderMedIkkeOverførteKonteringerListe, oppdragListe)
    }

    private fun prosesserOgSendKravTilSkatt(
        oppdragsperioderMedIkkeOverførteKonteringerListe: List<Oppdragsperiode>,
        oppdragListe: List<Oppdrag>,
    ) {
        LOGGER.debug { "Oppretter kravlister for oppdragsperioder: $oppdragsperioderMedIkkeOverførteKonteringerListe." }
        val kravlister = opprettKravlister(oppdragsperioderMedIkkeOverførteKonteringerListe)
        secureLogger.debug { "Sender kravlister til skatt: $kravlister." }
        kravlister.forEach { kravliste ->
            val skattResponse = skattConsumer.sendKrav(kravliste.first)
            lagreOverføringAvKrav(skattResponse, kravliste.second, oppdragListe)
        }
    }

    @Transactional
    fun validerOppdragForFeiledeOverføringer(oppdragListe: List<Oppdrag>): Boolean {
        oppdragListe.forEach { oppdrag ->
            if (harOppdragFeiledeOverføringer(oppdrag)) {
                val feiledeOverføringer =
                    behandlingsstatusService.hentBehandlingsstatusForIkkeGodkjenteKonteringerForReferansekode(hentSisteReferansekoder(oppdrag))

                if (feiledeOverføringer.isNotEmpty()) {
                    val feilmeldingSammenslått = feiledeOverføringer.entries.joinToString("\n") { it.value }
                    secureLogger.error { "Det har oppstått feil ved overføring av krav for oppdrag ${oppdrag.oppdragId} på følgende batchUider med følgende feilmelding:\n $feilmeldingSammenslått" }
                    return true
                }
            }
        }
        return false
    }

    private fun hentGyldigeOppdrag(oppdragIdListe: List<Int>): List<Oppdrag> = oppdragIdListe.mapNotNull { persistenceService.hentOppdrag(it) }
        .filterNot { oppdrag ->
            val skalUtsettes = oppdrag.utsattTilDato?.isAfter(LocalDate.now()) == true
            if (skalUtsettes) {
                LOGGER.info { "Oppdrag ${oppdrag.oppdragId} skal ikke oversendes før ${oppdrag.utsattTilDato}. Avventer oversending av krav." }
            }
            skalUtsettes
        }

    private fun hentSisteReferansekoder(oppdrag: Oppdrag) = oppdrag.oppdragsperioder.flatMap { oppdragsperiode ->
        oppdragsperiode.konteringer.flatMap { kontering ->
            listOfNotNull(kontering.sisteReferansekode)
        }
    }.distinct()

    private fun harOppdragFeiledeOverføringer(oppdrag: Oppdrag) = oppdrag.oppdragsperioder.any { oppdragsperiode ->
        oppdragsperiode.konteringer.any { kontering ->
            kontering.behandlingsstatusOkTidspunkt == null && kontering.sisteReferansekode != null
        }
    }

    fun opprettKravlister(oppdragsperioderMedIkkeOverførteKonteringerListe: List<Oppdragsperiode>): List<Pair<Kravliste, List<Kontering>>> {
        // Gruperer alle oppdragene på vedtakId for å sende over oppdrag knyttet til en vedtakId om gangen,
        // sorterer på vedtakId slik at tidligste vedtak kommer først
        // mapper så til kontering for å opprette en KravKontering per kontering
        // mapper deretter kravKonteringene per vedtak i hver sin kravliste og sender til skatt per kravliste
        secureLogger.debug { "Konteringer: ${oppdragsperioderMedIkkeOverførteKonteringerListe.flatMap { it.konteringer }}" }
        val ikkeOverførteKonteringer = finnAlleIkkeOverførteKonteringer(oppdragsperioderMedIkkeOverførteKonteringerListe)
        secureLogger.debug { "Alle ikke overførte konteringer: $ikkeOverførteKonteringer" }
        val konteringerGruppertPåVedtakId = ikkeOverførteKonteringer.groupBy { it.vedtakId }
        secureLogger.debug { "Konteringer gruppert på vedtakId: $konteringerGruppertPåVedtakId" }
        val sorterteVedtakIdTilKonteringerMap =
            konteringerGruppertPåVedtakId.mapValues { entry ->
                entry.value.sortedBy { kontering -> kontering.vedtakId }
            }.toSortedMap()
        secureLogger.debug { "Sorterte vedtakId til konteringer map: $sorterteVedtakIdTilKonteringerMap" }
        val kravlisteForKonteringer = sorterteVedtakIdTilKonteringerMap.map { (_, konteringer) ->
            Pair(Kravliste(listOf(opprettKravKonteringListe(konteringer))), konteringer)
        }
        secureLogger.debug { "Kravliste for konteringer: $kravlisteForKonteringer" }

        return kravlisteForKonteringer
    }

    private fun lagreOverføringAvKrav(skattResponse: ResponseEntity<String>, konteringerFraOverførtKrav: List<Kontering>, oppdrag: List<Oppdrag>) {
        try {
            when (skattResponse.statusCode) {
                HttpStatus.ACCEPTED -> {
                    hånterVellykketKravResponse(oppdrag, skattResponse, konteringerFraOverførtKrav)
                }

                HttpStatus.BAD_REQUEST -> {
                    secureLogger.error { "En eller flere konteringer har ikke gått gjennom validering. \nSaksnummer: ${oppdrag.first().sakId} \nFeilmelding${skattResponse.body}" }
                }

                HttpStatus.SERVICE_UNAVAILABLE -> {
                    secureLogger.error { "Skatt svarte med uventet statuskode: ${skattResponse.statusCode}. Tjenesten hos skatt er slått av. Dette kan skje enten ved innlesing av påløpsfil eller ved andre uventede feil. \nSaksnummer: ${oppdrag.first().sakId} \nFeilmelding: ${skattResponse.body}" }
                }

                HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN -> {
                    secureLogger.error { "Skatt svarte med uventet statuskode: ${skattResponse.statusCode} Bidrag-Regnskap er ikke autorisert eller mangler rettigheter for kallet mot skatt. \nSaksnummer: ${oppdrag.first().sakId} \nFeilmelding: $skattResponse" }
                }

                else -> {
                    secureLogger.error { "Skatt svarte med uventet statuskode: ${skattResponse.statusCode}. \nSaksnummer: ${oppdrag.first().sakId} \nFeilmelding: ${skattResponse.body}" }
                }
            }
        } catch (e: Exception) {
            secureLogger.error(e) { "Tolkningen av svaret fra skatt feilet på noe uventet! \nSaksnummer: ${oppdrag.first().sakId} \nFeil: ${e.message}" }
        }
    }

    private fun hånterVellykketKravResponse(
        oppdrag: List<Oppdrag>,
        skattResponse: ResponseEntity<String>,
        konteringerFraOverførtKrav: List<Kontering>,
    ) {
        secureLogger.info { "Mottok svar fra skatt for sak ${oppdrag.first().sakId}: \n$skattResponse" }
        val kravResponse = objectMapper.readValue(skattResponse.body, KravResponse::class.java)
        lagreVellykketOverføringAvKrav(konteringerFraOverførtKrav, kravResponse, oppdrag)
    }

    fun erVedlikeholdsmodusPåslått(): Boolean = skattConsumer.hentStatusPåVedlikeholdsmodus().statusCode == HttpStatus.SERVICE_UNAVAILABLE

    private fun lagreVellykketOverføringAvKrav(alleIkkeOverforteKonteringer: List<Kontering>, kravResponse: KravResponse, oppdrag: List<Oppdrag>) {
        alleIkkeOverforteKonteringer.forEach { kontering ->
            val now = LocalDateTime.now()
            kontering.overføringstidspunkt = now
            kontering.sisteReferansekode = kravResponse.batchUid
        }
        persistenceService.lagreOppdrag(oppdrag)
    }

    fun opprettKravKonteringListe(konteringerListe: List<Kontering>): Krav = Krav(
        konteringerListe.map { kontering ->
            mapKonteringTilKravkontering(kontering)
        },
    )

    private fun mapKonteringTilKravkontering(kontering: Kontering): Kravkontering {
        val transaksjonskode = Transaksjonskode.valueOf(kontering.transaksjonskode)

        val oppdrag = kontering.oppdragsperiode!!.oppdrag!!
        val beløp = if (transaksjonskode.negativtBeløp) {
            kontering.oppdragsperiode!!.beløp.negate()
        } else {
            kontering.oppdragsperiode!!.beløp
        }

        return Kravkontering(
            transaksjonskode = transaksjonskode,
            type = Type.valueOf(kontering.type),
            soknadType = Søknadstype.valueOf(kontering.søknadType),
            gjelderIdent = oppdrag.gjelderIdent,
            kravhaverIdent = oppdrag.kravhaverIdent,
            mottakerIdent = oppdrag.mottakerIdent,
            skyldnerIdent = oppdrag.skyldnerIdent,
            belop = beløp,
            valuta = kontering.oppdragsperiode!!.valuta,
            periode = kontering.overføringsperiode,
            vedtaksdato = kontering.oppdragsperiode!!.vedtaksdato.toString(),
            kjoredato = LocalDate.now().toString(),
            saksbehandlerId = kontering.oppdragsperiode!!.opprettetAv,
            attestantId = kontering.oppdragsperiode!!.opprettetAv,
            tekst = kontering.oppdragsperiode!!.eksternReferanse,
            fagsystemId = oppdrag.sakId,
            delytelsesId = kontering.oppdragsperiode!!.delytelseId.toString(),
        )
    }

    fun hentOppdragsperioderMedIkkeOverførteKonteringer(oppdrag: Oppdrag): List<Oppdragsperiode> = oppdrag.oppdragsperioder.filter { finnesDetIkkeOverførteKonteringer(it) }

    private fun finnesDetIkkeOverførteKonteringer(oppdragsperiode: Oppdragsperiode): Boolean = oppdragsperiode.konteringer.any { it.overføringstidspunkt == null }

    private fun finnAlleIkkeOverførteKonteringer(oppdragsperioder: List<Oppdragsperiode>): List<Kontering> = oppdragsperioder.flatMap { oppdragsperiode ->
        oppdragsperiode.konteringer.filter { kontering ->
            kontering.overføringstidspunkt == null
        }
    }
}

package no.nav.bidrag.regnskap.service

import no.nav.bidrag.domene.enums.regnskap.Søknadstype
import no.nav.bidrag.domene.enums.regnskap.Transaksjonskode
import no.nav.bidrag.domene.enums.regnskap.Type
import no.nav.bidrag.domene.enums.regnskap.behandlingsstatus.Batchstatus
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.dto.oppdrag.KonteringResponse
import no.nav.bidrag.regnskap.dto.oppdrag.OppdragResponse
import no.nav.bidrag.regnskap.dto.oppdrag.OppdragsperiodeResponse
import no.nav.bidrag.regnskap.dto.oppdrag.OppslagAvOppdragPåSakIdResponse
import no.nav.bidrag.regnskap.dto.vedtak.FeiledeVedtak
import no.nav.bidrag.regnskap.dto.vedtak.IkkeOversendteVedtak
import no.nav.bidrag.regnskap.dto.vedtak.UtsatteOgFeiledeVedtak
import no.nav.bidrag.regnskap.dto.vedtak.UtsatteOppdrag
import no.nav.bidrag.regnskap.dto.vedtak.UtsatteOppdragResponse
import no.nav.bidrag.regnskap.dto.vedtak.UtsatteVedtak
import no.nav.bidrag.regnskap.dto.vedtak.Vedtak
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.util.EnumUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class OppslagService(
    private val persistenceService: PersistenceService,
    private val skattConsumer: SkattConsumer,
) {

    @Transactional
    fun hentPåSakId(sakId: String): OppslagAvOppdragPåSakIdResponse? {
        val oppdrag = persistenceService.hentAlleOppdragPåSakId(sakId)

        if (oppdrag.isEmpty()) return null

        return OppslagAvOppdragPåSakIdResponse(oppdrag.map { hentOppdragResponse(it) })
    }

    @Transactional
    fun hentOppdrag(oppdragId: Int): OppdragResponse? {
        val oppdrag = persistenceService.hentOppdrag(oppdragId) ?: return null

        return hentOppdragResponse(oppdrag)
    }

    @Transactional
    fun hentUtsatteOgFeiledeVedtakForSak(saksnummer: Saksnummer): UtsatteOgFeiledeVedtak {
        val oppdrag = persistenceService.hentAlleOppdragPåSakId(saksnummer.verdi)

        val utsatteVedtak = hentUtsatteVedtak(oppdrag)
        val ikkeOversendteVedtak = hentIkkeOversendteVedtak(oppdrag, utsatteVedtak.map { it.vedtakId })
        val feiledeVedtak = hentFeiledeVedtak(oppdrag)

        return UtsatteOgFeiledeVedtak(utsatteVedtak, ikkeOversendteVedtak, feiledeVedtak)
    }

    private fun hentUtsatteVedtak(oppdrag: List<Oppdrag>): List<UtsatteVedtak> {
        val now = LocalDate.now()
        return oppdrag
            .filter { it.utsattTilDato != null && it.utsattTilDato!!.isAfter(now) }
            .flatMap { it.oppdragsperioder }
            .distinctBy { it.vedtakId }
            .map { UtsatteVedtak(it.vedtakId, it.oppdrag!!.utsattTilDato!!) }
    }

    private fun hentIkkeOversendteVedtak(oppdrag: List<Oppdrag>, utsatteVedtak: List<Int>): List<IkkeOversendteVedtak> = oppdrag
        .flatMap { it.oppdragsperioder }
        .flatMap { it.konteringer }
        .filter { it.overføringstidspunkt == null }
        .map { it.oppdragsperiode!! }
        .distinctBy { it.vedtakId }
        .filterNot { utsatteVedtak.contains(it.vedtakId) }
        .map { IkkeOversendteVedtak(it.vedtakId) }

    private fun hentFeiledeVedtak(oppdrag: List<Oppdrag>): List<FeiledeVedtak> = oppdrag
        .flatMap { it.oppdragsperioder }
        .filter { oppdragsperiode -> oppdragsperiode.konteringer.any { it.behandlingsstatusOkTidspunkt == null } }
        .flatMap { it.konteringer }
        .filter { it.sisteReferansekode != null }
        .groupBy { it.sisteReferansekode!! }
        .mapNotNull { (sisteReferansekode, konteringerForBatch) ->
            val sjekkAvBehandlingsstatusResponse = skattConsumer.sjekkBehandlingsstatus(sisteReferansekode).body
            var feilmelding: String? = null
            if (sjekkAvBehandlingsstatusResponse?.batchStatus == Batchstatus.Done) {
                val now = LocalDateTime.now()
                konteringerForBatch.forEach { it.behandlingsstatusOkTidspunkt = now }
            } else {
                feilmelding = sjekkAvBehandlingsstatusResponse?.konteringFeil?.firstOrNull()?.feilmelding
            }
            feilmelding?.let { FeiledeVedtak(konteringerForBatch.first().oppdragsperiode!!.vedtakId, it) }
        }

    fun hentOppdragResponse(oppdrag: Oppdrag): OppdragResponse = OppdragResponse(
        oppdragId = oppdrag.oppdragId,
        type = oppdrag.stønadType,
        sakId = oppdrag.sakId,
        kravhaverIdent = oppdrag.kravhaverIdent,
        skyldnerIdent = oppdrag.skyldnerIdent,
        gjelderIdent = oppdrag.gjelderIdent,
        mottakerIdent = oppdrag.mottakerIdent,
        utsattTilTidspunkt = oppdrag.utsattTilDato.toString(),
        endretTidspunkt = oppdrag.endretTidspunkt.toString(),
        oppdragsperioder = hentOppdragsperioderMedKonteringer(oppdrag),
    )

    fun hentOppdragsperioderMedKonteringer(oppdrag: Oppdrag): List<OppdragsperiodeResponse> = (oppdrag.oppdragsperioder).map {
        OppdragsperiodeResponse(
            oppdragsperiodeId = it.oppdragsperiodeId,
            oppdragId = it.oppdrag?.oppdragId,
            vedtakId = it.vedtakId,
            referanse = it.referanse,
            belop = it.beløp,
            valuta = it.valuta,
            periodeFra = it.periodeFra.toString(),
            periodeTil = it.periodeTil.toString(),
            vedtaksdato = it.vedtaksdato.toString(),
            opprettetAv = it.opprettetAv,
            delytelseId = it.delytelseId,
            eksternReferanse = it.eksternReferanse,
            aktivTil = it.aktivTil.toString(),
            konteringer = hentKonteringer(it),
            opphørendeOppdragsperiode = it.opphørendeOppdragsperiode,
        )
    }

    fun hentKonteringer(oppdragsperiode: Oppdragsperiode): List<KonteringResponse> = oppdragsperiode.konteringer.map {
        KonteringResponse(
            konteringId = it.konteringId,
            oppdragsperiodeId = it.oppdragsperiode?.oppdragsperiodeId,
            transaksjonskode = Transaksjonskode.valueOf(it.transaksjonskode),
            overforingsperiode = it.overføringsperiode,
            overforingstidspunkt = it.overføringstidspunkt.toString(),
            behandlingsstatusOkTidspunkt = it.behandlingsstatusOkTidspunkt.toString(),
            type = Type.valueOf(it.type),
            soknadType = Søknadstype.valueOf(it.søknadType),
            sendtIPalopsperiode = it.sendtIPåløpsperiode,
            sisteReferansekode = it.sisteReferansekode,
            opprettetTidspunkt = it.opprettetTidspunkt.toString(),
            vedtakId = it.vedtakId,
        )
    }

    @Transactional
    fun hentAlleUtsatteOppdrag(): UtsatteOppdragResponse {
        val alleUtsatteOppdrag = persistenceService.hentAlleUtsatteOppdrag()

        fun evaluerStønadstype(oppdrag: Oppdrag): Stønadstype? = if (EnumUtils.erAvEnumType<Stønadstype>(oppdrag.stønadType)) Stønadstype.valueOf(oppdrag.stønadType) else null

        fun evaluerEngangsbeløptype(oppdrag: Oppdrag): Engangsbeløptype? = if (EnumUtils.erAvEnumType<Engangsbeløptype>(oppdrag.stønadType)) Engangsbeløptype.valueOf(oppdrag.stønadType) else null

        fun opprettVedtak(oppdrag: Oppdrag): List<Vedtak> = oppdrag.oppdragsperioder.map { oppdragsperiode ->
            Vedtak(
                oppdragsperiode.vedtakId,
                oppdragsperiode.vedtaksdato,
                oppdragsperiode.enhetsnummer,
            )
        }

        return UtsatteOppdragResponse(
            alleUtsatteOppdrag.map { oppdrag ->
                UtsatteOppdrag(
                    oppdragsid = oppdrag.oppdragId,
                    saksnummer = Saksnummer(oppdrag.sakId),
                    stønadstype = evaluerStønadstype(oppdrag),
                    engangsbeløptype = evaluerEngangsbeløptype(oppdrag),
                    utsattTilDato = oppdrag.utsattTilDato!!,
                    vedtak = opprettVedtak(oppdrag),
                )
            },
        )
    }
}

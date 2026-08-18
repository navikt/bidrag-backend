package no.nav.bidrag.regnskap.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.regnskap.consumer.BidragVedtakConsumer
import no.nav.bidrag.regnskap.dto.patch.OppdaterReferanseRequest
import no.nav.bidrag.regnskap.dto.patch.ReferanseForVedtakResponse
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.persistence.repository.OppdragRepository
import no.nav.bidrag.regnskap.persistence.repository.OppdragsperiodeRepository
import org.springframework.stereotype.Service

private val LOGGER = KotlinLogging.logger {}

@Service
class PatchService(
    private val oppdragsperiodeRepository: OppdragsperiodeRepository,
    private val oppdragRepository: OppdragRepository,
    private val bidragVedtakConsumer: BidragVedtakConsumer,
    private val identUtils: IdentUtils,
) {

    fun hentReferanseForVedtak(vedtakId: Int): List<ReferanseForVedtakResponse> = oppdragsperiodeRepository.findAllByVedtakIdAndReferanseIsNotNull(vedtakId).map { oppdragsperiode ->
        opprettReferanseForVedtakResponse(oppdragsperiode)
    }

    fun hentReferanseForSak(saksnummer: String): List<ReferanseForVedtakResponse> = oppdragRepository.findAllBySakId(saksnummer)
        .flatMap { it.oppdragsperioder }
        .filter { oppdragsperiode -> oppdragsperiode.referanse != null }
        .map { oppdragsperiode ->
            opprettReferanseForVedtakResponse(oppdragsperiode)
        }

    fun hentAlleTommeReferanser(): List<Int> = oppdragsperiodeRepository.findAllByReferanse("").map { it.vedtakId }

    fun oppdaterReferanseForOppdragsperiode(oppdaterReferanseRequest: OppdaterReferanseRequest) {
        val oppdragsperiode = oppdragsperiodeRepository.findById(oppdaterReferanseRequest.oppdragsperiodeId)
            .orElseThrow { IllegalArgumentException("Oppdragsperiode med id ${oppdaterReferanseRequest.oppdragsperiodeId} finnes ikke") }
        oppdragsperiode.referanse = oppdaterReferanseRequest.referanse
        oppdragsperiodeRepository.save(oppdragsperiode)
    }

    /**
     * Denne metoden går igjennom og sjekker alle som har tom string som referanse. Dette kom av en bug i vedtaksoverføring i ny løsning.
     */
    fun patchTommeReferanser() {
        val oppdragsperioderMedTommeRefernaser = oppdragsperiodeRepository.findAllByReferanse("")

        oppdragsperioderMedTommeRefernaser.forEach { oppdragsperiode ->
            val vedtakResponse = bidragVedtakConsumer.hentVedtak(oppdragsperiode.vedtakId)
            val matcheneEngangsbeløp = vedtakResponse?.engangsbeløpListe
                ?.filter { it.type.name == oppdragsperiode.oppdrag!!.stønadType }
                ?.filter { erSammePerson(it.skyldner, oppdragsperiode.oppdrag!!.skyldnerIdent) }
                ?.filter { erSammePerson(it.kravhaver, oppdragsperiode.oppdrag!!.kravhaverIdent!!) }

            if (matcheneEngangsbeløp.isNullOrEmpty()) {
                LOGGER.error { "Fant ingen matchende engangsbeløp for vedtak: ${oppdragsperiode.vedtakId} ved oppslag på oppdragsperiode: ${oppdragsperiode.oppdragsperiodeId}." }
                return@forEach
            }
            if (matcheneEngangsbeløp.size > 1) {
                LOGGER.error { "Fant flere matchene engangsbeløp for vedtak: ${oppdragsperiode.vedtakId} ved oppslag på oppdragsperiode: ${oppdragsperiode.oppdragsperiodeId}." }
                return@forEach
            }

            oppdragsperiode.referanse = matcheneEngangsbeløp.first().referanse
            oppdragsperiodeRepository.save(oppdragsperiode)
        }
    }

    private fun erSammePerson(
        personFraVedtak: Personident,
        personFraOppdragsperiode: String,
    ): Boolean = identUtils.hentNyesteIdent(personFraVedtak).verdi == personFraOppdragsperiode

    private fun opprettReferanseForVedtakResponse(oppdragsperiode: Oppdragsperiode): ReferanseForVedtakResponse = ReferanseForVedtakResponse(
        referanse = oppdragsperiode.referanse,
        vedtakId = oppdragsperiode.vedtakId,
        oppdragId = oppdragsperiode.oppdrag!!.oppdragId!!,
        oppdragsperiodeId = oppdragsperiode.oppdragsperiodeId!!,
        sakId = oppdragsperiode.oppdrag!!.sakId,
        stønadstype = oppdragsperiode.oppdrag!!.stønadType,
        vedtakstype = Vedtakstype.valueOf(oppdragsperiode.vedtakType),
    )
}

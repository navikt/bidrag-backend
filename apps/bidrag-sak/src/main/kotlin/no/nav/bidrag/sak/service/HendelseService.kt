package no.nav.bidrag.sak.service

import no.nav.bidrag.domene.enums.behandling.HendelseType
import no.nav.bidrag.domene.felles.Verdiobjekt
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.sak.domain.Bidragssak
import no.nav.bidrag.sak.domain.Hendelse
import no.nav.bidrag.sak.integration.kafka.KafkaProducer
import no.nav.bidrag.sak.mapper.personidentBidragsmottaker
import no.nav.bidrag.sak.mapper.personidentBidragspliktig
import no.nav.bidrag.sak.mapper.toBarnISak
import no.nav.bidrag.sak.repository.HendelseRepository
import no.nav.bidrag.transport.sak.OppdaterSakRequest
import no.nav.bidrag.transport.sak.SakHendelse
import no.nav.bidrag.transport.sak.SakKafkaHendelsestype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class HendelseService(
    private val hendelseRepository: HendelseRepository,
    private val kafkaProducer: KafkaProducer,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun opprettHendelser(
        sak: Bidragssak,
        oppdaterSakRequest: OppdaterSakRequest,
    ) {
        opprettHendelse(
            sak.kategori,
            oppdaterSakRequest.kategorikode,
            sak,
            HendelseType.GJELDER_ENDRET,
        )
        opprettHendelse(
            sak.konvensjon,
            oppdaterSakRequest.konvensjonskode,
            sak,
            HendelseType.KONVENSJONSKOДЕ_REGISTRERT,
        )
        opprettHendelse(
            sak.ffuReferansenr,
            oppdaterSakRequest.ffuReferansenr,
            sak,
            HendelseType.REFERANSENUMMER_REGISTRERT,
        )
    }

    fun opprettKafkaHendelse(
        sak: Bidragssak?,
        oppdatertSak: Bidragssak,
    ) {
        val sakshendelse =
            SakHendelse(
                saksnummer = Saksnummer(oppdatertSak.saksnummer),
                hendelsestype = if (sak == null) SakKafkaHendelsestype.OPPRETTELSE else SakKafkaHendelsestype.ENDRING,
                bidragspliktig = oppdatertSak.roller.personidentBidragspliktig(),
                bidragsmottaker = oppdatertSak.roller.personidentBidragsmottaker(),
                barn = oppdatertSak.roller.toBarnISak(),
            )
        try {
            kafkaProducer.sendSakshendelse(sakshendelse)
        } catch (e: Exception) {
            logger.warn("Sending av sak hendelse feilet", e)
        }
    }

    private fun opprettHendelse(
        gammelVerdi: Any?,
        nyVerdi: Any?,
        sak: Bidragssak,
        hendelsestype: HendelseType,
    ) {
        if (gammelVerdi == nyVerdi) {
            return
        }

        val gammelUtskriftsverdi = if (gammelVerdi is Verdiobjekt<*>) gammelVerdi.verdi else gammelVerdi
        val nyUtskriftsverdi = if (nyVerdi is Verdiobjekt<*>) nyVerdi.verdi else nyVerdi

        val resultat = finnResultat(gammelUtskriftsverdi, hendelsestype, nyUtskriftsverdi, nyVerdi)

        val hendelse =
            Hendelse(
                saksnummer = sak.saksnummer,
                type = hendelsestype,
                resultat = resultat,
                enhet = sak.eierfogd,
                søknad = null,
            )

        hendelseRepository.save(hendelse)
    }

    private fun finnResultat(
        gammelUtskriftsverdi: Any?,
        hendelsestype: HendelseType,
        nyUtskriftsverdi: Any?,
        nyVerdi: Any?,
    ): String {
        val navn = hendelsestype.beskrivelse
        return if (gammelUtskriftsverdi == null) {
            "$navn - $nyUtskriftsverdi"
        } else if (nyVerdi == null) {
            "$navn verdi fjernet"
        } else {
            "$navn fra $gammelUtskriftsverdi til $nyUtskriftsverdi"
        }
    }
}

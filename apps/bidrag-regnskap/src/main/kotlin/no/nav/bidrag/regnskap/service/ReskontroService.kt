package no.nav.bidrag.regnskap.service

import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.regnskap.Transaksjonskode
import no.nav.bidrag.regnskap.consumer.BidragReskontroConsumer
import no.nav.bidrag.regnskap.persistence.entity.Kontering
import no.nav.bidrag.transport.reskontro.response.transaksjoner.TransaksjonDto
import no.nav.bidrag.transport.reskontro.response.transaksjoner.TransaksjonerDto
import org.springframework.stereotype.Service

@Service
class ReskontroService(
    private val bidragReskontroConsumer: BidragReskontroConsumer,
) {

    /**
     * Denne metoden sammenligner konteringer med hva som finnes i reskontro. Den tar input med konteringer.
     * Metoden tar deretter å oppretter en liste med feilmelding om det finnes avvik mellom oversending og reskontro.
     * Dette er ikke ment som en erstatning av sjekkAvBehandlingstatus, men heller som et suplement.
     */
    fun sammenlignOversendteKonteringerMedReskontro(oversendteKonteringer: Map<String, Set<Kontering>>): HashMap<String, MutableSet<String>> {
        val feilmeldinger: HashMap<String, MutableSet<String>> = hashMapOf()
        oversendteKonteringer.forEach { (sisteReferansekode, konteringer) ->
            // Siden alle konteringer oversendt for en referansekode alltid er knyttet til samme sak og vedtak kan vi hente saksnummer fra første kontering
            val saksnummer = konteringer.first().oppdragsperiode!!.oppdrag!!.sakId

            val transaksjoner: TransaksjonerDto?
            try {
                transaksjoner = bidragReskontroConsumer.hentTransaksjonerForSak(saksnummer)
            } catch (e: Exception) {
                secureLogger.error(e) { "Klarte ikke hente transaksjoner fra reskontro for sak: $saksnummer for konteringer: $konteringer" }
                feilmeldinger.putIfAbsent(sisteReferansekode, mutableSetOf())
                feilmeldinger[sisteReferansekode]?.add("Klarte ikke hente transaksjoner fra reskontro for sak: $saksnummer, vedtak: ${konteringer.first().vedtakId}.\n")
                return@forEach
            }
            if (transaksjoner == null || transaksjoner.transaksjoner.isEmpty()) {
                if (erAlleForskudd(konteringer)) {
                    secureLogger.info { "Fant ingen transaksjoner i reskontro for sak: $saksnummer for konteringer: $konteringer, men alle er forskudd som ikke finnes i reskontro før om en uke." }
                } else {
                    secureLogger.warn { "Fant ingen transaksjoner i reskontro for sak: $saksnummer for konteringer: $konteringer" }
                    feilmeldinger.putIfAbsent(sisteReferansekode, mutableSetOf())
                    feilmeldinger[sisteReferansekode]?.add("Det finnes ingen transaksjoner i reskontro for sak: $saksnummer, vedtak: ${konteringer.first().vedtakId}.\n")
                }
            }

            konteringer.forEach { kontering ->
                if (erAlleForskudd(setOf(kontering))) {
                    return@forEach
                }
                val transaksjonSomMatcherKontering = finnMatcheneTransaksjon(transaksjoner, kontering)
                if (transaksjonSomMatcherKontering == null) {
                    feilmeldinger.putIfAbsent(sisteReferansekode, mutableSetOf())
                    feilmeldinger[sisteReferansekode]?.add("Fant ikke transaksjon i reskontro for sak: $saksnummer, vedtak: ${kontering.vedtakId}, transaksjonskode: ${kontering.transaksjonskode}, periode: ${kontering.overføringsperiode} som matcher oversendt kontering: ${kontering.konteringId}.\n")
                }
            }
        }
        return feilmeldinger
    }

    private fun erAlleForskudd(konteringer: Set<Kontering>): Boolean = konteringer.all { it.transaksjonskode == Transaksjonskode.A1.name || it.transaksjonskode == Transaksjonskode.A3.name }

    private fun finnMatcheneTransaksjon(
        transaksjoner: TransaksjonerDto?,
        kontering: Kontering,
    ): TransaksjonDto? = transaksjoner?.transaksjoner?.find { transaksjon ->
        val sammeDelytelsesId = transaksjon.delytelsesid == kontering.oppdragsperiode!!.delytelseId.toString()
        val sammeTransaksjonskode = transaksjon.transaksjonskode == kontering.transaksjonskode
        val sammePeriode = transaksjon.periode?.toMånedsperiode()?.fom.toString() == kontering.overføringsperiode
        sammeDelytelsesId &&
            sammeTransaksjonskode &&
            sammePeriode
    }
}

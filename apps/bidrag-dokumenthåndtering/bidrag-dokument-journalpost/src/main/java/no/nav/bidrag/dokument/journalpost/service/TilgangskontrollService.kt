package no.nav.bidrag.dokument.journalpost.service

import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.dokument.journalpost.consumer.BidragTilgangskontrollConsumer
import no.nav.bidrag.dokument.journalpost.exception.ingenTilgang
import org.springframework.stereotype.Service

@Service
class TilgangskontrollService(
    private val bidragTilgangskontrollConsumer: BidragTilgangskontrollConsumer,
) {
    fun sjekkTilgangSak(saksnummer: String) {
        if (SikkerhetsKontekst.erIApplikasjonKontekst()) return
        if (!bidragTilgangskontrollConsumer.sjekkTilgangSak(saksnummer)) ingenTilgang("Ingen tilgang til saksnummer $saksnummer")
    }

    fun harTilgangTilTema(tema: String): Boolean {
        if (SikkerhetsKontekst.erIApplikasjonKontekst()) return true
        return bidragTilgangskontrollConsumer.sjekkTilgangTema(tema)
    }

    fun sjekkTilgangPerson(personnummer: String) {
        if (SikkerhetsKontekst.erIApplikasjonKontekst()) return
        if (!bidragTilgangskontrollConsumer.sjekkTilgangPerson(personnummer)) ingenTilgang("Ingen tilgang til personnummer $personnummer")
    }
}

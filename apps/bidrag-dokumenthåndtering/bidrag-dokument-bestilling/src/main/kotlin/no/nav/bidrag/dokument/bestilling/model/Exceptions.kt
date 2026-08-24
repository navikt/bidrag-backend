package no.nav.bidrag.dokument.bestilling.model

import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMal
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

class ProduksjonAvDokumentStottesIkke(
    dokumentMal: DokumentMal,
) : RuntimeException("Produksjon av dokument med brevkode=${dokumentMal.kode} støttes ikke")

class FantIkkePersonException(
    msg: String,
) : RuntimeException(msg)

class FantIkkeEnhetException(
    msg: String,
) : RuntimeException(msg)

class FantIkkeSakException(
    msg: String,
) : RuntimeException(msg)

class HentSakFeiletException(
    msg: String,
    throwable: Throwable,
) : RuntimeException(msg, throwable)

class HentVedtakFeiletException(
    msg: String,
    throwable: Throwable,
) : RuntimeException(msg, throwable)

class HentPersonFeiletException(
    msg: String,
    throwable: Throwable,
) : RuntimeException(msg, throwable)

class SamhandlerManglerKontaktinformasjon(
    msg: String,
) : RuntimeException(msg)

class ManglerGjelderException(
    msg: String,
) : RuntimeException(msg)

class BestillingManglerMottaker : RuntimeException("Bestilling mangler mottaker")

fun fantIkkeVedtak(vedtakId: Int): Nothing = throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "Fant ikke vedtak med id $vedtakId")

fun manglerBehandlingId(): Nothing =
    throw HttpClientErrorException(
        HttpStatus.BAD_REQUEST,
        "Forespørsel for opprettelse av varselbrev mangler behandlingId",
    )

fun manglerVedtakId(): Nothing =
    throw HttpClientErrorException(
        HttpStatus.BAD_REQUEST,
        "Forespørsel for opprettelse av vedtaksbrev mangler vedtakId",
    )

fun fantIkkeSak(saksnummer: String): Nothing = throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "Fant ikke sak med id $saksnummer")

fun dokumentMalEksistererIkke(dokumentmalKode: String): Nothing =
    throw HttpClientErrorException(
        HttpStatus.BAD_REQUEST,
        "Dokumentmal $dokumentmalKode eksisterer ikke",
    )

fun kanIkkeBestilleDokumentMal(kode: String): Nothing =
    throw HttpClientErrorException(
        HttpStatus.BAD_REQUEST,
        "Kan ikke bestille dokumentmal $kode.",
    )

fun manglerDataGrunnlag(dokumentMal: DokumentMal): Nothing =
    throw HttpClientErrorException(
        HttpStatus.BAD_REQUEST,
        "Forespørsel mangler informasjon for å opprette dokumentmal ${dokumentMal.kode}.",
    )

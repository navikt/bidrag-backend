package no.nav.bidrag.automatiskjobb.service

import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentAllePersoner
import no.nav.bidrag.transport.behandling.felles.grunnlag.personIdent
import no.nav.bidrag.transport.behandling.felles.grunnlag.stønadstype
import no.nav.bidrag.transport.person.PersonDto
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpStatusCodeException

fun hentPerson(ident: String?): PersonDto? = try {
    ident.takeIfNotNullOrEmpty {
        AppContext.getBean(BidragPersonConsumer::class.java).hentPerson(Personident(it))
    }
} catch (e: HttpStatusCodeException) {
    if (e.statusCode == HttpStatus.NOT_FOUND) {
        null
    } else {
        throw e
    }
}

fun hentNyesteIdent(ident: String?) = ident?.let { hentPerson(ident)?.ident ?: Personident(ident) }

fun Collection<GrunnlagDto>.hentPersonNyesteIdent(
    ident: String?,
    stønadstype: Stønadstype? = null,
) = hentAllePersoner()
    .find {
        (hentNyesteIdent(it.personIdent)?.verdi == hentNyesteIdent(ident)?.verdi || it.personIdent == ident) &&
            (stønadstype == null || it.stønadstype == null || stønadstype == it.stønadstype)
    }

fun <T, R> T?.takeIfNotNullOrEmpty(block: (T) -> R): R? = if (this == null ||
    (
        this is String &&
            this.trim().isEmpty()
        ) ||
    (this is List<*> && this.isEmpty())
) {
    null
} else {
    block(this)
}

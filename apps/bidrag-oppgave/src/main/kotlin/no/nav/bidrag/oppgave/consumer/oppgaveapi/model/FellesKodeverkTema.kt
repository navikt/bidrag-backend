package no.nav.bidrag.oppgave.consumer.oppgaveapi.model

/**
 * Utvalg av tema fra Navs felles kodeverk som brukes ved søk i oppgave-apiet.
 *
 * Wrapper for det som tidligere lå i `no.nav.common.consts.FellesKodeverkTema`.
 * Enum-navnet er identisk med tema-koden, slik at det brukes direkte som query-parameter.
 *
 * Første iterasjon inneholder kun temaene team Bidrag har behov for. Utvid listen
 * ved behov – se felles kodeverk for fullstendig oversikt.
 */
enum class FellesKodeverkTema {
    /** Bidrag */
    BID,

    /** Helsetjenester og ortopediske hjelpemidler */
    HEL,

    /** Hjelpemidler */
    HJE,
}

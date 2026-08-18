package no.nav.bidrag.bbm.persistence.bisys.repository

import no.nav.bidrag.bbm.bo.Gebyrsøknad
import no.nav.bidrag.bbm.bo.Innkrevingssøknad
import no.nav.bidrag.bbm.bo.ÅpenSøknad
import no.nav.bidrag.bbm.persistence.bisys.entity.Søknad
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.time.LocalDate

interface SøknadRepository : CrudRepository<Søknad, String> {
    @Query(
        "select s from Søknad s where s.søknadsid = :søknadsid",
    )
    fun finnSøknad(søknadsid: Long): Søknad?

    @Query(
        "select new no.nav.bidrag.bbm.bo.ÅpenSøknad(s.behandlerenhet, s.saksnummer, " +
            "s.søknadsid, s.refVedtaksid, s.referertSøknadsid, s.blankettid, s.søknadMottattDato, " +
            "s.søknadFomDato, s.søknadsgruppekode, " +
            "s.behandlingsid, sl.søknadslinjeid, r.fnr, sl.innbetaltBeløp, sl.søknadStatuskode, sl.gruppeKombinasjonskode, " +
            "sl.engangsbeløpReferanse) " +
            "from Søknad s join Søknadslinje sl on s.søknadsid = sl.søknadsid join KodeSøknadStatus kss " +
            "on sl.søknadStatuskode = kss.kode " +
            "join Rolle r on sl.rolleid = r.rolleid " +
            "where s.saksnummer in :saksnummerListe and s.søknadsgruppekode in ('BI', 'OB', '18') and kss.lukketStatus = '0'",
    )
    fun finnÅpneSøknader(saksnummerListe: List<String>): List<ÅpenSøknad>

    @Query(
        "select new no.nav.bidrag.bbm.bo.Gebyrsøknad(s.søknadsid, sl.rolleid, sl.engangsbeløpReferanse)" +
            " from Søknad s join Søknadslinje sl on s.søknadsid = sl.søknadsid " +
            "where s.blankettid = :blankettid and s.søknadsgruppekode = 'GB'",
    )
    fun finnTilhørendeGebyrsøknader(blankettid: Long): List<Gebyrsøknad>

    @Query(
        "select new no.nav.bidrag.bbm.bo.Innkrevingssøknad(s.søknadsid, sl.rolleid)" +
            " from Søknad s join Søknadslinje sl on s.søknadsid = sl.søknadsid " +
            "where s.blankettid = :blankettid and s.søknadsgruppekode = 'IK'",
    )
    fun finnTilhørendeInnkrevingssøknaderOgSøknadslinjer(blankettid: Long): List<Innkrevingssøknad>

    @Query(
        "select s from Søknad s where s.blankettid = :blankettid and s.søknadsgruppekode = 'IK' ",
    )
    fun finnTilhørendeInnkrevingsssøknad(blankettid: Long): Søknad?

    @Query(
        "select s from Søknad s where s.blankettid = :blankettid ",
    )
    fun finnAlleTilhørendeSøknader(blankettid: Long): List<Søknad>

    // Må splitte i to metoder fordi null = null er false og da fungerer ikke s.refVedtaksid = :refVedtaksid hvis begge er null
    @Query(
        "select s from Søknad s " +
            "where s.saksnummer = :saksnummer and s.behandlingsid = :behandlingsid " +
            "and s.søknadsgruppekode = :søknadsgruppekode and s.behandlerenhet = :behandlerenhet " +
            "and s.søknadFomDato = :søknadFomDato and s.refVedtaksid = :refVedtaksid " +
            "order by s.søknadsid desc limit 1",
    )
    fun finnEksisterendeIkkeFeilregistrertSøknadMedRefVedtaksid(
        saksnummer: String,
        behandlingsid: Int?,
        refVedtaksid: Int,
        behandlerenhet: String?,
        søknadFomDato: LocalDate,
        søknadsgruppekode: String,
    ): Søknad?

    @Query(
        "select s from Søknad s " +
            "where s.saksnummer = :saksnummer and s.behandlingsid = :behandlingsid " +
            "and s.søknadsgruppekode = :søknadsgruppekode and s.behandlerenhet = :behandlerenhet " +
            "and s.søknadFomDato = :søknadFomDato and s.refVedtaksid is null " +
            "order by s.søknadsid desc limit 1",
    )
    fun finnEksisterendeIkkeFeilregistrertSøknadUtenRefVedtaksid(
        saksnummer: String,
        behandlingsid: Int?,
        behandlerenhet: String?,
        søknadFomDato: LocalDate,
        søknadsgruppekode: String,
    ): Søknad?

    fun finnEksisterendeIkkeFeilregistrertSøknad(
        saksnummer: String,
        behandlingsid: Int?,
        refVedtaksid: Int?,
        behandlerenhet: String?,
        søknadFomDato: LocalDate,
        søknadsgruppekode: String,
    ): Søknad? = if (refVedtaksid == null) {
        finnEksisterendeIkkeFeilregistrertSøknadUtenRefVedtaksid(
            saksnummer = saksnummer,
            behandlingsid = behandlingsid,
            behandlerenhet = behandlerenhet,
            søknadFomDato = søknadFomDato,
            søknadsgruppekode = søknadsgruppekode,
        )
    } else {
        finnEksisterendeIkkeFeilregistrertSøknadMedRefVedtaksid(
            saksnummer = saksnummer,
            behandlingsid = behandlingsid,
            refVedtaksid = refVedtaksid,
            behandlerenhet = behandlerenhet,
            søknadFomDato = søknadFomDato,
            søknadsgruppekode = søknadsgruppekode,
        )
    }
}

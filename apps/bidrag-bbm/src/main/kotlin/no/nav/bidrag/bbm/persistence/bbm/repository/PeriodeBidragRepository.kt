package no.nav.bidrag.bbm.persistence.bbm.repository

import no.nav.bidrag.bbm.persistence.bbm.entity.PeriodeBidrag
import no.nav.bidrag.bbm.persistence.bbm.entity.PeriodeBidragId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.time.LocalDate

interface PeriodeBidragRepository : CrudRepository<PeriodeBidrag, PeriodeBidragId> {
    @Query(
        "select pb from PeriodeBidrag pb where pb.saksnummer = :saksnummer and pb.personidentBarn = :personidentBarn " +
            "and pb.datoSøknad = :datoSøknad and pb.soknadstype in :søknadstyper order by pb.datoFom desc fetch first 1 row only",
    )
    fun finnSisteBidragPeriode(
        saksnummer: String,
        personidentBarn: String,
        datoSøknad: LocalDate,
        søknadstyper: List<String>,
    ): PeriodeBidrag?

    @Query(
        "select pb from PeriodeBidrag pb where pb.saksnummer = :saksnummer order by pb.datoFom asc",
    )
    fun finnAlleBidragPeriodeForSaksnummer(saksnummer: String): List<PeriodeBidrag>

    @Query(
        "select pb from PeriodeBidrag pb where pb.saksnummer = :saksnummer and pb.personidentBarn = :personidentBarn " +
            "and pb.datoSøknad = :datoSøknad and pb.soknadstype in :søknadstyper order by pb.datoFom",
    )
    fun finnAlleBidragPeriode(
        saksnummer: String,
        personidentBarn: String,
        datoSøknad: LocalDate,
        søknadstyper: List<String>,
    ): List<PeriodeBidrag>
}

package no.nav.bidrag.bbm.utils

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.transaction.Transactional
import no.nav.bidrag.bbm.persistence.bbm.entity.PeriodeBidrag
import no.nav.bidrag.bbm.persistence.bbm.entity.Samvær
import no.nav.bidrag.bbm.persistence.bbm.repository.PeriodeBidragRepository
import no.nav.bidrag.bbm.persistence.bbm.repository.SamværRepository
import no.nav.bidrag.bbm.persistence.bisys.entity.Blankett
import no.nav.bidrag.bbm.persistence.bisys.entity.Hendelse
import no.nav.bidrag.bbm.persistence.bisys.entity.KodeSøknadStatus
import no.nav.bidrag.bbm.persistence.bisys.entity.Rolle
import no.nav.bidrag.bbm.persistence.bisys.entity.Søknad
import no.nav.bidrag.bbm.persistence.bisys.entity.Søknadsknytning
import no.nav.bidrag.bbm.persistence.bisys.entity.Søknadslinje
import no.nav.bidrag.bbm.persistence.bisys.repository.BlankettRepository
import no.nav.bidrag.bbm.persistence.bisys.repository.HendelseRepository
import no.nav.bidrag.bbm.persistence.bisys.repository.KodeSøknadStatusRepository
import no.nav.bidrag.bbm.persistence.bisys.repository.RolleRepository
import no.nav.bidrag.bbm.persistence.bisys.repository.SøknadRepository
import no.nav.bidrag.bbm.persistence.bisys.repository.SøknadsknytningRepository
import no.nav.bidrag.bbm.persistence.bisys.repository.SøknadslinjeRepository
import no.nav.bidrag.domene.enums.behandling.SøknadsknytningStatus
import org.springframework.stereotype.Component

@Component
class TestdataManager(
    private val periodeBidragRepository: PeriodeBidragRepository,
    private val samværRepository: SamværRepository,
    private val søknadRepository: SøknadRepository,
    private val søknadslinjeRepository: SøknadslinjeRepository,
    private val rolleRepository: RolleRepository,
    private val kodeSøknadStatusRepository: KodeSøknadStatusRepository,
    private val entityManager: EntityManager,
    private val blankettRepository: BlankettRepository,
    private val hendelseRepository: HendelseRepository,
    private val søknadsknytningRepository: SøknadsknytningRepository,
) {
    @PersistenceContext(unitName = "bisys")
    private lateinit var bisysEntityManager: EntityManager

    @Transactional
    fun lagrePeriodeBidragListe(periodeBidrag: List<PeriodeBidrag>): List<PeriodeBidrag> = periodeBidragRepository.saveAll(periodeBidrag).toList()

    @Transactional
    fun lagreSamværListe(samvær: List<Samvær>): List<Samvær> = samværRepository.saveAll(samvær).toList()

    @Transactional
    fun lagreSøknadListe(søknad: List<Søknad>): List<Søknad> = søknadRepository.saveAll<Søknad>(søknad).toList()

    @Transactional
    fun lagreBlankettListe(blankett: List<Blankett>): List<Blankett> = blankettRepository.saveAll<Blankett>(blankett).toList()

    @Transactional
    fun lagreRoller(rolle: List<Rolle>): List<Rolle> = rolleRepository.saveAll<Rolle>(rolle).toList()

    @Transactional
    fun lagreSøknadslinjeListe(søknadslinje: List<Søknadslinje>): List<Søknadslinje> = søknadslinjeRepository.saveAll<Søknadslinje>(søknadslinje).toList()

    @Transactional
    fun lagreKodeSøknadsstatus(kss: List<KodeSøknadStatus>): List<KodeSøknadStatus> = kodeSøknadStatusRepository.saveAll(kss).toList()

    @Transactional
    fun hentSøknadMedId(søknadsid: Long): Søknad? = søknadRepository.finnSøknad(søknadsid)

    @Transactional
    fun hentSøknadslinjerForSøknadMedId(søknadsid: Long): List<Søknadslinje>? = søknadslinjeRepository.finnSøknadslinjerForSøknad(søknadsid)

    @Transactional
    fun hentHendelserForSøknadMedId(søknadsid: Long): List<Hendelse>? = hendelseRepository.finnHendelserForSøknad(søknadsid)

    @Transactional
    fun lagreSøknadsknytninger(søknadsknytninger: List<Søknadsknytning>): List<Søknadsknytning> = søknadsknytningRepository.saveAll<Søknadsknytning>(søknadsknytninger).toList()

    @Transactional
    fun hentSøknadsknytningerHovedsøknad(hovedsøknadsid: Long): List<Søknadsknytning> = søknadsknytningRepository.finnSøknadsknytningerHovedsøknad(
        hovedsøknadsid = hovedsøknadsid,
        statuser = listOf(SøknadsknytningStatus.Aktiv.name),
    )

    @Transactional
    fun hentSøknadsknytningReferertSøknad(
        referertSøknadsid: Long,
        status: String? = null,
    ): List<Søknadsknytning> = søknadsknytningRepository.finnSøknadsknytningReferertSøknad(
        referertSøknadsid = referertSøknadsid,
        statuser = listOf(status ?: SøknadsknytningStatus.Aktiv.name),
    )

    fun rydd() {
        entityManager.clear()
        bisysEntityManager.clear()
        periodeBidragRepository.deleteAll()
        samværRepository.deleteAll()
        søknadslinjeRepository.deleteAll()
        søknadRepository.deleteAll()
        kodeSøknadStatusRepository.deleteAll()
        rolleRepository.deleteAll()
        blankettRepository.deleteAll()
        søknadsknytningRepository.deleteAll()
    }
}

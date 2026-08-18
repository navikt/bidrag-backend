package no.nav.bidrag.bbm.persistence.bisys.repository

import no.nav.bidrag.bbm.persistence.bisys.entity.KodeSøknadStatus
import org.springframework.data.repository.CrudRepository

interface KodeSøknadStatusRepository : CrudRepository<KodeSøknadStatus, String>

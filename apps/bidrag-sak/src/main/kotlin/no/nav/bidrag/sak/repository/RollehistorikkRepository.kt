package no.nav.bidrag.sak.repository

import no.nav.bidrag.sak.domain.Rollehistorikk
import org.springframework.data.jpa.repository.JpaRepository

interface RollehistorikkRepository : JpaRepository<Rollehistorikk, String>

package no.nav.bidrag.bbm.persistence.bisys.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "T_SOKNAD_KNYT")
@Suppress("unused")
open class Søknadsknytning(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    open var id: Long? = null,
    @Column(name = "HOVED_SOKNAD_ID")
    open var hovedsøknadsid: Long? = null,
    @Column(name = "REF_SOKNAD_ID")
    open var referertSøknadsid: Long? = null,
    @Column(name = "STATUS")
    open var status: String? = null,
    @Column(name = "SOKNAD_KNYTNINGSTYPE")
    open var søknadKnytningstype: String? = null,
    @Column(name = "OPPRETT_DATO")
    open var opprettetTidspunkt: LocalDateTime? = null,
)

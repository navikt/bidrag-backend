package no.nav.bidrag.sak.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity(name = "t_soknad_linje")
class Søknadslinje(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "sokn_linje_id", nullable = false, columnDefinition = "integer")
    private var soknLinjeId: Int? = null,
    @ManyToOne(fetch = FetchType.EAGER, cascade = [CascadeType.PERSIST])
    @JoinColumn(name = "sokn_id", nullable = false)
    val søknad: Søknad,
    @Column(name = "sokn_stat_kode", length = 20, nullable = false)
    val statusKode: String,
)

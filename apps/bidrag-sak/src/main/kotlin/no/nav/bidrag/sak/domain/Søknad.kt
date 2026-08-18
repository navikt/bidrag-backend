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
import jakarta.persistence.OneToMany

@Entity(name = "T_SOKNAD")
class Søknad(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "sokn_id", nullable = false, columnDefinition = "integer")
    val id: Int? = null,
    @OneToMany(mappedBy = "søknad", fetch = FetchType.EAGER)
    val søknadslinjer: MutableList<Søknadslinje>,
    @OneToMany(mappedBy = "søknad", cascade = [CascadeType.PERSIST])
    val hendelser: MutableList<Hendelse>,
    @Column(name = "behandling_id", nullable = true)
    val behandlingId: String? = null,
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "blankett_id", nullable = true)
    val blankett: Blankett? = null,
)

package no.nav.bidrag.grunnlag.consumer.ecb.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.LocalDate

@Entity(name = "EcbValutakursCache")
@Table(
    name = "ECBVALUTAKURSCACHE",
    uniqueConstraints = [UniqueConstraint(name = "ecbvalutakurscache_valutakode_dato_unique", columnNames = ["valutakode", "valutakursdato"])],
)
class ECBValutakursCache(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ecbvalutakurscache_seq_generator")
    @SequenceGenerator(name = "ecbvalutakurscache_seq_generator", sequenceName = "ecbvalutakurscache_seq", allocationSize = 50)
    val id: Long = 0,
    @Column(name = "valutakursdato", columnDefinition = "DATE", nullable = false)
    val valutakursdato: LocalDate,
    @Column(name = "valutakode", nullable = false)
    val valutakode: String,
    @Column(name = "kurs", nullable = false, precision = 38, scale = 16)
    val kurs: BigDecimal,
)

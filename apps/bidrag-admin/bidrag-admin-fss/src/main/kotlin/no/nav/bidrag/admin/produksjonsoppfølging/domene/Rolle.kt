package no.nav.bidrag.admin.produksjonsoppfølging.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "T_ROLLE")
class Rolle {
    @Id
    @Column(name = "rolle_id")
    val id: Long = 0

    @Column(name = "saksnr")
    val saksnr: String? = null

    @Column(name = "fnr")
    val fnr: String? = null

    @Column(name = "objektnr")
    val objektnr: String? = null

    @Column(name = "rolle_type")
    @Enumerated(EnumType.STRING)
    val type: no.nav.bidrag.admin.produksjonsoppfølging.domene.Rolletype? = null

    @Column(name = "ukj_part_kode")
    val ukjentPartKode: String? = null

    @Column(name = "rm_rolle_id")
    val rmRolleId: Int? = null

    @Column(name = "fodt_dato")
    val fodtDato: String? = null

    @Column(name = "opprettet_dato")
    val opprettetDato: String? = null

    fun objektnrSomInt(): Int {
        if (objektnr == null) {
            return 0
        }
        return objektnr!!.replaceFirst("^0+(?!$)".toRegex(), "").toInt()
    }

    fun erBidragspliktig(): Boolean = no.nav.bidrag.admin.produksjonsoppfølging.domene.Rolletype.BP == type

    fun erBidragsmottaker(): Boolean = no.nav.bidrag.admin.produksjonsoppfølging.domene.Rolletype.BM == type

    fun erBarn(): Boolean = no.nav.bidrag.admin.produksjonsoppfølging.domene.Rolletype.BA == type

    fun erReellMotaker(): Boolean = no.nav.bidrag.admin.produksjonsoppfølging.domene.Rolletype.RM == type
}

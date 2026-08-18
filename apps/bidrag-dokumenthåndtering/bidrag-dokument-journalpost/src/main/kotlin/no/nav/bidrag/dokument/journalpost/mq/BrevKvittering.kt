package no.nav.bidrag.dokument.journalpost.mq

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement
import no.nav.bidrag.dokument.journalpost.model.toStringByReflection

@XmlRootElement(name = "rtv-brevkvitt")
@XmlAccessorType(XmlAccessType.NONE)
class BrevKvittering {
    @XmlElement(name = "brevref")
    var brevRef: String? = null

    @XmlElement(name = "sysid")
    var sysId: String? = null

    @XmlElement(name = "type")
    var type: String? = null

    @XmlElement(name = "status")
    var status: BrevStatus? = null

    @XmlElement(name = "feilkode")
    var feilkode: String? = null

    constructor()
    constructor(brevRef: String, status: BrevStatus, sysId: String) {
        this.brevRef = brevRef
        this.status = status
        this.sysId = sysId
    }

    override fun toString(): String = this.toStringByReflection()
}

enum class BrevStatus {
    FERDIG,
    FEIL,
    LAGRET,
    AVBRUTT,
}

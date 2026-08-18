package no.nav.bidrag.dokument.journalpost.dto

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlRootElement

class Dokumentbestilling private constructor(
    val brevreferanse: String? = null,
    val systemId: String? = null,
    val token: String? = null,
) {
    data class Builder(
        var brevreferanse: String? = null,
        var systemId: String? = null,
        var token: String? = null,
    ) {
        fun brevreferanse(brevreferanse: String?) = apply { this.brevreferanse = brevreferanse }

        fun systemId(systemId: String?) = apply { this.systemId = systemId }

        fun token(token: String?) = apply { this.token = token }

        fun build() = Dokumentbestilling(brevreferanse, systemId, token)
    }
}

@XmlRootElement(name = Dokumenttilgang.ROOT_ELEMENT_NAME)
@XmlAccessorType(XmlAccessType.FIELD)
class Dokumenttilgang constructor(
    @XmlAttribute
    val saksbehandler: String? = null,
    @XmlAttribute
    val passord: String? = null,
    @XmlAttribute
    val klientToken: String? = null,
    @XmlAttribute
    val sysid: String? = null,
    @XmlAttribute
    val modus: String? = null,
    val brev: Brev? = null,
) {
    companion object {
        const val ROOT_ELEMENT_NAME = "rtv-brev"
    }
}

@XmlRootElement(name = "brev")
@XmlAccessorType(XmlAccessType.FIELD)
data class Brev(
    @XmlAttribute
    val brevref: String? = null,
) {
    constructor() : this(null)
}

package no.nav.bidrag.dokument.journalpost.configuration.jms

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "mq")
data class MQProperties(
    var hostName: String? = null,
    var port: Int = 0,
    var name: String? = null,
    var appname: String? = null,
    var username: String = "",
    var password: String = "",
    var channel: String = "",
    var brevserverQueue: String? = null,
    var backOffInitialInterval: Long = 2000,
    var backOffMaxInterval: Long = 300000,
)

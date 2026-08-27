package no.nav.bidrag.transport.felles

fun <T> Boolean?.ifTrue(block: (Boolean) -> T?): T? = if (this == true) block(this) else null

fun <T> Boolean?.ifFalse(block: (Boolean) -> T?): T? = if (this == false) block(this) else null

package no.nav.bidrag.mdc

import java.lang.Long
import java.util.UUID
import kotlin.String

internal class IdUtils {
    fun generateId(): String {
        val uuid = UUID.randomUUID()
        return "${Long.toHexString(uuid.mostSignificantBits)}${Long.toHexString(uuid.leastSignificantBits)}"
    }
}

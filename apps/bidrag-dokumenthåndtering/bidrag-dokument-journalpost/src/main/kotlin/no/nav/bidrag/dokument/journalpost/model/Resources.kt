package no.nav.bidrag.dokument.journalpost.model

import no.nav.bidrag.dokument.journalpost.exception.ResourceDiscriminatorException
import kotlin.reflect.full.memberProperties

class ResourceByDiscriminator<T>(
    private val resources: Map<Discriminator, T>,
) {
    init {
        if (resources.isEmpty()) {
            throw ResourceDiscriminatorException("Minst en ressurs må ligge i ressurs-mappen")
        }
    }

    fun get(discriminator: Discriminator) = resources[discriminator] ?: throw ResourceDiscriminatorException(
        "Ingen ressurs for $discriminator",
    )

    override fun toString() = "${resources.size} ${resources.firstNotNullOf { it.javaClass.simpleName }} med ${resources.keys}"
}

enum class Discriminator {
    REGULAR_USER,
    SERVICE_USER,
}

fun Any.toStringByReflection(
    exclude: List<String> = listOf(),
    mask: List<String> = listOf(),
): String {
    val propsString =
        this::class
            .memberProperties
            .filter { exclude.isEmpty() || !exclude.contains(it.name) }
            .joinToString(", ") {
                val value = if (mask.isNotEmpty() && mask.contains(it.name)) "****" else it.getter.call(this).toString()
                "${it.name}=$value"
            }

    return "${this::class.simpleName}[$propsString]"
}

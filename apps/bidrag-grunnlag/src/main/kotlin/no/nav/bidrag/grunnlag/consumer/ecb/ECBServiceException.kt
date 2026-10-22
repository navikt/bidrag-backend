package no.nav.bidrag.grunnlag.consumer.ecb

class ECBServiceException(
    override val message: String,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)

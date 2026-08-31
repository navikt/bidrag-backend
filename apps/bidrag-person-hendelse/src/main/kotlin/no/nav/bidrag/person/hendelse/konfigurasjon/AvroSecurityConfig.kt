package no.nav.bidrag.person.hendelse.konfigurasjon

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class AvroSecurityConfig(
    // TODO: Tillater midlertidig alle pakker/klasser ("*"). Bør strammes inn til kun de pakkene
    // vi faktisk deserialiserer, f.eks. "no.nav.person.pdl.leesah".
    @Value($$"${AVRO_SERIALIZABLE_PACKAGES:*}") private val avroSerializablePackages: String,
) {
    @PostConstruct
    fun konfigurerAvroSerializablePackages() {
        System.setProperty(AVRO_SERIALIZABLE_PACKAGES_PROPERTY, avroSerializablePackages)
    }

    companion object {
        const val AVRO_SERIALIZABLE_PACKAGES_PROPERTY = "org.apache.avro.SERIALIZABLE_PACKAGES"
    }
}

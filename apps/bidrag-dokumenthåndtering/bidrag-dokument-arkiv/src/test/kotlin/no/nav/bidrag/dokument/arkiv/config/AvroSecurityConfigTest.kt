package no.nav.bidrag.dokument.arkiv.config

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.bidrag.dokument.arkiv.hendelser.createHendelseRecord
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Verifiserer at Kafka faktisk klarer å deserialisere en spesifikk Avro-record
    * når pakken er tillatt av AvroSecurityConfig.
    */
@DisplayName("Kafka Avro-deserialisering av JournalfoeringHendelseRecord")
internal class AvroSecurityConfigTest {

    @Test
    @DisplayName("skal serialisere og deserialisere JournalfoeringHendelseRecord korrekt via ekte Kafka Avro (de)serializer når pakken er tillatt")
    fun `skal serialisere og deserialisere hendelse korrekt naar pakken er tillatt av AvroSecurityConfig`() {
        AvroSecurityConfig("no.nav.joarkjournalfoeringhendelser").konfigurerAvroSerializablePackages()
        val original = createHendelseRecord(123213L)

        val deserialisert = deserialiser(serialiser(original))

        deserialisert.shouldBeInstanceOf<JournalfoeringHendelseRecord>()
        deserialisert.hendelsesId shouldBe original.hendelsesId
        deserialisert.journalpostId shouldBe original.journalpostId
        deserialisert.journalpostStatus shouldBe original.journalpostStatus
        deserialisert.temaNytt shouldBe original.temaNytt
        deserialisert.mottaksKanal shouldBe original.mottaksKanal
    }

    private fun serialiser(record: JournalfoeringHendelseRecord): ByteArray = KafkaAvroSerializer().use {
        it.configure(mapOf(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl), false)
        it.serialize(TOPIC, record)
    }

    private fun deserialiser(bytes: ByteArray): JournalfoeringHendelseRecord = KafkaAvroDeserializer().use {
        it.configure(
            mapOf(
                KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl,
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to "true",
            ),
            false,
        )
        it.deserialize(TOPIC, bytes) as JournalfoeringHendelseRecord
    }

    companion object {
        private const val TOPIC = "avro-security-config-test-topic"

        // Unik testklasse-instansiering slik at ikke testen deler skjemaregister-tilstand med andre tester.
        private val schemaRegistryUrl = "mock://${UUID.randomUUID()}"
    }
}
